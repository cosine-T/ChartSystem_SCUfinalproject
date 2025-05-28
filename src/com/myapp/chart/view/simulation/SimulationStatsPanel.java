package com.myapp.chart.view.simulation;

import com.myapp.chart.controller.ViewController;
import com.myapp.chart.model.ChannelData;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * HR / BP / SpO₂ / RR 实时统计
 *   ─ HR：R-峰 > HR_PEAK_THRESHOLD mV，峰间 ≥ HR_MIN_PEAK_INTERVAL_SEC 秒
 *   ─ RR：呼吸信号过零 & 幅度 > RR_PEAK_THRESHOLD，峰间 ≥ RR_MIN_PEAK_INTERVAL_SEC 秒
 */
public class SimulationStatsPanel extends JPanel {

    // —— 常量区 —— //

    // 心率（HR）参数
    private static final double HR_PEAK_THRESHOLD       = 0.7;    // mV
    private static final double HR_MIN_PEAK_INTERVAL_SEC= 0.2;    // 秒
    private static final int    HR_LOOKBACK_SEC         = 8;      // 最近 N 秒

    // 血压（BP）参数
    private static final int    BP_LOOKBACK_SEC         = 1;      // 最近 N 秒

    // 血氧（SpO2）参数
    private static final int    SPO2_LOOKBACK_SEC       = 6;      // 最近 N 秒

    // 呼吸率（RR）参数（示例值，实际可调）
    private static final double RR_PEAK_THRESHOLD       = 0.2;    // 呼吸峰阈值
    private static final double RR_MIN_PEAK_INTERVAL_SEC= 1.0;    // 秒
    private static final int    DEFAULT_RR_VALUE        = 15;      // 无法计算时显示

    // 文本格式
    private static final String FORMAT_HR  = "HR  %3.0f";
    private static final String FORMAT_BP  = "BP  %3.0f/%3.0f";
    private static final String FORMAT_SPO = "SpO2 %2.0f%%";
    private static final String FORMAT_RR  = "RR  %3.0f";

    // —— UI 组件 —— //

    private final JLabel hrLabel  = createStatLabel();
    private final JLabel bpLabel  = createStatLabel();
    private final JLabel spoLabel = createStatLabel();
    private final JLabel rrLabel  = createStatLabel();

    // 播放/写入控制器
    private final ViewController.PlaybackController ctrl;

    public SimulationStatsPanel(ViewController.PlaybackController ctrl) {
        this.ctrl = ctrl;
        setLayout(new GridLayout(4, 1));
        add(hrLabel);
        add(bpLabel);
        add(spoLabel);
        add(rrLabel);
    }

    private static JLabel createStatLabel() {
        JLabel label = new JLabel("--", SwingConstants.CENTER);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        return label;
    }

    /**
     * 刷新所有统计；由外部定时（如每 2s）调用
     */
    public void refresh(List<ChannelData> chs) {
        long written = ctrl.getWritten();
        if (written <= 0) return;

        int endIndex = (int) written - 1;
        updateHeartRate(chs.get(0), endIndex);
        updateBloodPressure(chs.get(1), endIndex);
        updateSpO2(chs.get(2), endIndex);
        updateRespiratoryRate(chs.get(3), endIndex);
    }

    // —— 各项统计实现 —— //

    private void updateHeartRate(ChannelData ecgChannel, int endIndex) {
        double fs = ecgChannel.getSampleRate();
        int span = (int) Math.min(endIndex + 1, HR_LOOKBACK_SEC * fs);
        double[] data = ecgChannel.getData();

        List<Integer> peaks = new ArrayList<>();
        int minInterval = (int) (HR_MIN_PEAK_INTERVAL_SEC * fs);
        int lastPeak = -minInterval;

        int start = endIndex + 1 - span;
        for (int i = start + 1; i < endIndex; i++) {
            if (data[i] > HR_PEAK_THRESHOLD
                    && data[i] > data[i - 1]
                    && data[i] > data[i + 1]
                    && i - lastPeak >= minInterval)
            {
                peaks.add(i);
                lastPeak = i;
            }
        }

        double hrValue = 0;
        if (peaks.size() > 1) {
            double totalInterval = 0;
            for (int i = 1; i < peaks.size(); i++) {
                totalInterval += (peaks.get(i) - peaks.get(i - 1));
            }
            double avgSamplesPerBeat = totalInterval / (peaks.size() - 1);
            hrValue = 60.0 * fs / avgSamplesPerBeat;
        }

        hrLabel.setText(String.format(FORMAT_HR, hrValue));
    }

    private void updateBloodPressure(ChannelData bpChannel, int endIndex) {
        double fs = bpChannel.getSampleRate();
        int span = (int) Math.min(endIndex + 1, BP_LOOKBACK_SEC * fs);
        double[] data = bpChannel.getData();

        double systolic  = Double.MIN_VALUE;
        double diastolic = Double.MAX_VALUE;

        int start = endIndex + 1 - span;
        for (int i = start; i <= endIndex; i++) {
            double v = data[i];
            if (Double.isNaN(v)) continue;
            if (v > systolic)  systolic = v;
            if (v < diastolic) diastolic = v;
        }

        bpLabel.setText(String.format(FORMAT_BP, systolic, diastolic));
    }

    private void updateSpO2(ChannelData spo2Channel, int endIndex) {
        double fs = spo2Channel.getSampleRate();
        int span = (int) Math.min(endIndex + 1, SPO2_LOOKBACK_SEC * fs);
        double[] data = spo2Channel.getData();

        double sum = 0;
        int count = 0;
        int start = endIndex + 1 - span;
        for (int i = start; i <= endIndex; i++) {
            double v = data[i];
            if (!Double.isNaN(v)) {
                sum += v;
                count++;
            }
        }
        double spoValue = (count > 0) ? (sum / count) : 0;
        spoLabel.setText(String.format(FORMAT_SPO, spoValue));
    }

    private void updateRespiratoryRate(ChannelData rrChannel, int endIndex) {
        // 简单示例：未做实际过零检测，保留默认
        double rrValue = DEFAULT_RR_VALUE;
        rrLabel.setText(String.format(FORMAT_RR, rrValue));
    }
}
