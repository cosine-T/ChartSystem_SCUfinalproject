package com.myapp.chart.view.simulation;

import com.myapp.chart.controller.ViewController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.view.channel.ChannelPanel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * ECG 监护仪窗口：无限记录 + 统计 + 导出（统一时间轴，按全局采样率 FS 对齐）
 */
public class MonitorFrame extends JFrame implements WindowProvider {

    // —— 常量区 —— //

    // 全局采样率与初始容量
    private static final int FS = 250;                    // 采样率 (Hz)
    private static final int INIT_CAPACITY = 10_000;      // 初始样本容量 (~40s)

    // 界面尺寸
    private static final int WINDOW_WIDTH  = 1100;
    private static final int WINDOW_HEIGHT = 660;

    // 播放控制按钮符号
    private static final String ICON_PLAY_PAUSE = "⏸";
    private static final String ICON_PLAY       = "▶";

    // 缩放滑块
    private static final int ZOOM_MIN       = 500;
    private static final int ZOOM_MAX       = 5_000;

    // 统计刷新与模拟线程休眠间隔
    private static final int STATS_REFRESH_INTERVAL_MS = 2_000;
    private static final int PAUSE_SLEEP_MS            = 20;
    private static final int SIM_SLEEP_MS              = 4;

    // 心率更新周期、RESP 周期与 SPO2 采样间隔
    private static final int BPM_UPDATE_INTERVAL = FS * 8;    // 每 8 秒更新一次 bpm
    private static final int RESP_SAMPLE_PERIOD  = 10;
    private static final int SPO2_SAMPLE_PERIOD  = 5;
    private static final int SPO2_LOW_INTERVAL   = FS * 40;   // 每 40 秒可能进入低值
    private static final int SPO2_LOW_DURATION   = FS * 4;    // 维持 4 秒低值

    // 血压波形长度
    private static final int BP_BEAT_LENGTH = 125;

    // —— ECG 模拟常量 —— //
    private static final double ECG_NOISE_STD = 0.02;
    private static final double[] ECG_THETA = {-70, -15, 0, 15, 100};
    private static final double[] ECG_SIGMA = {15, 5, 3, 7, 20};
    private static final double[] ECG_AMP   = {0.2, -0.15, 1.2, -0.25, 0.35};

    // —— BP 模拟常量 —— //
    private static final double BP_NOISE_STD      = 0.6;
    private static final double BP_BEAT_RISE_THRESHOLD = 0.25;
    private static final double BP_RISE_BASE      = 80;
    private static final double BP_RISE_AMPLITUDE = 45;
    private static final double BP_DECAY_BASE     = 75;
    private static final double BP_DECAY_AMPLITUDE = 45;
    private static final double BP_DECAY_WIDTH     = 0.6;

    // —— RESP 模拟常量 —— //
    private static final double RESP_AMPLITUDE = 1.2;
    private static final double RESP_NOISE_STD = 0.05;

    // —— SpO2 模拟常量 —— //
    private static final double SPO2_LOW_BASE    = 88.0;
    private static final double SPO2_NORMAL_BASE = 98.0;
    private static final double SPO2_NOISE_STD   = 0.3;

    // —— 其他模拟常量 —— //
    private static final int CAPACITY_GROWTH_FACTOR = 2;



    // GUI 文本
    private static final String LABEL_TIME_ZOOM = "Time Zoom";
    private static final String BTN_EXPORT_TXT  = "导出 TXT";
    private static final String FRAME_TITLE     = "Vital Signs Monitor";

    // —— 成员变量 —— //

    private final ViewController.PlaybackController ctrl =
            new ViewController.PlaybackController(INIT_CAPACITY);
    private volatile boolean playing = true;

    private final List<ChannelData> channels = new ArrayList<>();
    private final List<ChannelPanel> panels  = new ArrayList<>();

    private final JScrollBar hScroll;
    private final JSlider    zoom;
    private final JToggleButton btnPlay;
    private final SimulationStatsPanel stats;

    private VitalSim simThread;
    private final Timer statsTimer;

    // —— 构造函数 —— //

    public MonitorFrame() {
        super(FRAME_TITLE);
        this.stats = new SimulationStatsPanel(ctrl);

        initChannels();
        JPanel waveCol = createWaveformColumn();

        this.btnPlay = createPlayToggle();
        this.zoom    = createZoomSlider();

        JPanel top = createTopPanel();
        this.hScroll = createScrollBar();

        JButton btnExport = createExportButton();
        JPanel east = new JPanel(new BorderLayout());
        east.add(stats,   BorderLayout.CENTER);
        east.add(btnExport, BorderLayout.SOUTH);

        setupLayout(top, waveCol, east);
        setupWindow();

        // 启动后台模拟与统计刷新
        simThread = new VitalSim();
        simThread.start();
        statsTimer = new Timer(STATS_REFRESH_INTERVAL_MS, e -> stats.refresh(channels));
        statsTimer.start();
    }

    @Override
    public void dispose() {
        simThread.stopSim();
        statsTimer.stop();
        super.dispose();
    }

    // WindowProvider 接口
    @Override public int getOffset()       { return ctrl.getOffset(); }
    @Override public int getWindowLength() { return ctrl.getWindowLength(); }

    // —— 私有方法 —— //

    /** 初始化四个通道的数据缓冲 */
    private void initChannels() {
        channels.add(new ChannelData("ECG",  createArray(INIT_CAPACITY), FS));
        channels.add(new ChannelData("BP",   createArray(INIT_CAPACITY), FS));
        channels.add(new ChannelData("SpO2", createArray(INIT_CAPACITY), FS));
        channels.add(new ChannelData("RESP", createArray(INIT_CAPACITY), FS));
    }

    /** 创建存放波形的纵列面板 */
    private JPanel createWaveformColumn() {
        JPanel panel = new JPanel(new GridLayout(channels.size(), 1));
        for (ChannelData ch : channels) {
            ChannelPanel p = new ChannelPanel(null, ch, null, this);
            panels.add(p);
            panel.add(p);
        }
        return panel;
    }

    /** 播放/暂停切换按钮 */
    private JToggleButton createPlayToggle() {
        JToggleButton btn = new JToggleButton(ICON_PLAY_PAUSE, true);
        btn.addActionListener(e -> {
            playing = btn.isSelected();
            btn.setText(playing ? ICON_PLAY_PAUSE : ICON_PLAY);
            ctrl.setAutoFollow(playing);
        });
        return btn;
    }

    /** 时间缩放滑块 */
    private JSlider createZoomSlider() {
        JSlider slider = new JSlider(ZOOM_MIN, ZOOM_MAX, ctrl.getWindowLength());
        slider.addChangeListener(e -> {
            ctrl.setWindowLen(slider.getValue());
            syncScroll();
            repaintWaves();
        });
        return slider;
    }

    /** 顶部控制面板 */
    private JPanel createTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnPlay);
        top.add(new JLabel(LABEL_TIME_ZOOM));
        top.add(zoom);
        return top;
    }

    /** 底部水平滚动条 */
    private JScrollBar createScrollBar() {
        JScrollBar bar = new JScrollBar(
                JScrollBar.HORIZONTAL,
                0, ctrl.getWindowLength(), 0, INIT_CAPACITY);
        bar.addAdjustmentListener(e -> {
            if (!playing) {
                ctrl.setOffset(e.getValue());
                repaintWaves();
            }
        });
        return bar;
    }

    /** 导出按钮 */
    private JButton createExportButton() {
        JButton btn = new JButton(BTN_EXPORT_TXT);
        btn.addActionListener(e ->
                ExportSimulation.show(
                        this,
                        channels,
                        ctrl,
                        new String[]{"ECG", "BP", "SpO2", "RESP"}
                )
        );
        return btn;
    }

    /** 布局组装 */
    private void setupLayout(JPanel top, JPanel center, JPanel east) {
        setLayout(new BorderLayout());
        add(top,     BorderLayout.NORTH);
        add(center,  BorderLayout.CENTER);
        add(east,    BorderLayout.EAST);
        add(hScroll, BorderLayout.SOUTH);
    }

    /** 窗口基础属性 */
    private void setupWindow() {
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /** 创建并用 NaN 填充数组 */
    private double[] createArray(int size) {
        double[] a = new double[size];
        Arrays.fill(a, Double.NaN);
        return a;
    }

    private void repaintWaves() {
        SwingUtilities.invokeLater(() -> panels.forEach(JComponent::repaint));
    }

    private void syncScroll() {
        hScroll.setVisibleAmount(ctrl.getWindowLength());
        hScroll.setMaximum((int) Math.max(ctrl.getWritten(), ctrl.getWindowLength()));
        if (playing) {
            hScroll.setValue(ctrl.getOffset());
        }
    }

    // —— 内部模拟线程 —— //

    private class VitalSim extends Thread {
        private volatile boolean running = true;
        private final Random rnd = new Random();

        private long sampleIdx = 0;
        private double bpm = 75;
        private int spo2LowCnt = 0;

        private final double[] theta = ECG_THETA;
        private final double[] sigma = ECG_SIGMA;
        private final double[] amp = ECG_AMP;

        private final double[] bpBeat = buildBpBeat();
        private int bpPtr = 0;
        private int respPtr = 0;

        @Override
        public void run() {
            while (running) {
                if (!playing) {
                    sleepSilently(PAUSE_SLEEP_MS);
                    continue;
                }
                if (sampleIdx % BPM_UPDATE_INTERVAL == 0) {
                    bpm = 60 + rnd.nextDouble() * 30;
                }

                ensureCapacity((int) sampleIdx + 1);
                int idx = (int) sampleIdx;

                // ECG
                double phase = (sampleIdx % FS) * 360.0 / FS;
                double ecg = 0;
                for (int i = 0; i < theta.length; i++) {
                    double d = angleDiff(phase, theta[i]);
                    ecg += amp[i] * Math.exp(-0.5 * d * d / (sigma[i] * sigma[i]));
                }
                ecg += rnd.nextGaussian() * ECG_NOISE_STD;
                channels.get(0).getData()[idx] = ecg;

                // BP
                double[] bpArr = channels.get(1).getData();
                bpArr[idx] = (sampleIdx % 2 == 0)
                        ? bpBeat[bpPtr++] + rnd.nextGaussian() * BP_NOISE_STD
                        : bpArr[idx - 1];
                bpPtr %= bpBeat.length;

                // RESP
                double[] rsp = channels.get(3).getData();
                rsp[idx] = (sampleIdx % RESP_SAMPLE_PERIOD == 0)
                        ? RESP_AMPLITUDE * Math.sin(2 * Math.PI * respPtr++ / BP_BEAT_LENGTH)
                        + rnd.nextGaussian() * RESP_NOISE_STD
                        : rsp[idx - 1];
                respPtr %= BP_BEAT_LENGTH;

                // SpO2
                if (spo2LowCnt > 0) {
                    spo2LowCnt--;
                } else if (sampleIdx % SPO2_LOW_INTERVAL == 0) {
                    spo2LowCnt = SPO2_LOW_DURATION;
                }
                double base = (spo2LowCnt > 0) ? SPO2_LOW_BASE : SPO2_NORMAL_BASE;
                double[] spo = channels.get(2).getData();
                spo[idx] = (sampleIdx % SPO2_SAMPLE_PERIOD == 0)
                        ? base + rnd.nextGaussian() * SPO2_NOISE_STD
                        : spo[idx - 1];

                sampleIdx++;
                ctrl.onWrite(1);
                syncScroll();
                repaintWaves();
                sleepSilently(SIM_SLEEP_MS);
            }
        }

        void stopSim() {
            running = false;
        }

        private double angleDiff(double a, double b) {
            double d = a - b;
            if (d > 180) d -= 360;
            if (d < -180) d += 360;
            return d;
        }

        private void ensureCapacity(int need) {
            int cap = channels.get(0).getData().length;
            if (need <= cap) return;

            int newCap = cap;
            while (newCap < need) {
                newCap *= CAPACITY_GROWTH_FACTOR;
            }
            for (ChannelData ch : channels) {
                double[] neo = Arrays.copyOf(ch.getData(), newCap);
                Arrays.fill(neo, ch.getData().length, newCap, Double.NaN);
                try {
                    Field f = ChannelData.class.getDeclaredField("data");
                    f.setAccessible(true);
                    f.set(ch, neo);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            ctrl.expandCapacity(newCap);
            hScroll.setMaximum(newCap);
        }

        private void sleepSilently(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {}
        }

        private double[] buildBpBeat() {
            double[] w = new double[BP_BEAT_LENGTH];
            for (int i = 0; i < BP_BEAT_LENGTH; i++) {
                double t = i / (double) BP_BEAT_LENGTH;
                w[i] = (t < BP_BEAT_RISE_THRESHOLD)
                        ? BP_RISE_BASE + BP_RISE_AMPLITUDE * Math.sin(Math.PI * t / BP_BEAT_RISE_THRESHOLD)
                        : BP_DECAY_BASE + BP_DECAY_AMPLITUDE * Math.exp(-(t - BP_BEAT_RISE_THRESHOLD) / BP_DECAY_WIDTH);
            }
            return w;
        }
    }
}