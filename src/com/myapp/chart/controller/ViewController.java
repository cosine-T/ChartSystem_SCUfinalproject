package com.myapp.chart.controller;

import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;
import com.myapp.chart.view.simulation.MonitorFrame;

import javax.swing.*;

public class ViewController {
    private final ChartController chartC;

    public ViewController(ChartController chartC) {
        this.chartC = chartC;
    }

    public void showMonitor() {
        // 直接启动模拟窗口
        SwingUtilities.invokeLater(() -> new MonitorFrame().setVisible(true));
    }

    public void onHScroll(int value) {
        DataModel model = chartC.getModel();
        if (model == null) return;
        model.setCurrentOffset(value);
        chartC.refreshView();
    }

    public void zoomHorizontally(double factor) {
        DataModel model = chartC.getModel();
        if (model == null) return;
        model.zoom(factor);
        chartC.refreshView();
    }

    public void autoZoomHorizontal() {
        DataModel model = chartC.getModel();
        ChartFrame frame = chartC.getFrame();
        if (model == null || model.getChannels().isEmpty()) return;

        // 找最长
        int maxSamples = model.getChannels().stream()
                .mapToInt(ch -> ch.getData().length)
                .max().orElse(0);

        model.setWindowLength(maxSamples);
        model.setCurrentOffset(0);
        frame.updateView();
    }

    public void zoomToTimeWindow(int seconds) {
        DataModel model = chartC.getModel();
        ChartFrame frame = chartC.getFrame();
        if (model == null || model.getChannels().isEmpty()) return;

        float fs = model.getChannels().get(0).getSampleRate();
        int samples = (int)(seconds * fs);
        model.setWindowLength(samples);
        model.setCurrentOffset(0);
        frame.updateView();
    }

    /**
     * 统一时间轴控制器：保存“已写入样本数 / 可视窗口长度 / 当前窗口偏移 / 自动跟随”。
     * 线程安全：所有 public 方法均 synchronized。
     */
    public static class PlaybackController {

        private int bufferCap;     // 当前通道数组容量（可增长）
        private long written = 0;  // 已写入样本数（无限递增）
        private int  windowLen;    // 可视窗口（默认 2500 ≈10 s）
        private int  offset   = 0; // 当前窗口起点
        private boolean autoFollow = true;

        public PlaybackController(int initCap) {
            this.bufferCap = initCap;
            this.windowLen = 2500;                   // 初始 10 s
        }

        /* 写入 n 个新样本后调用 */
        public synchronized void onWrite(int n) {
            written += n;
            if (autoFollow) {
                offset = Math.max(0, (int) written - windowLen);
            }
        }

        /* ========== getters ========== */
        public synchronized int  getOffset()       { return offset;        }
        public synchronized int  getWindowLength() { return windowLen;     }
        public synchronized long getWritten()      { return written;       }
        public synchronized int  getCapacity()     { return bufferCap;     }
        public synchronized boolean isAutoFollow() { return autoFollow;    }

        /* ========== setters ========== */
        public synchronized void setWindowLen(int len) {
            this.windowLen = len;
            offset = Math.min(offset, Math.max(0, (int) written - windowLen));
        }
        public synchronized void setOffset(int off) {
            offset = Math.max(0, Math.min(off, Math.max(0, (int) written - windowLen)));
        }
        public synchronized void setAutoFollow(boolean b) { autoFollow = b; }

        /* 缓冲区动态扩容时调用 */
        public synchronized void expandCapacity(int newCap) { bufferCap = newCap; }
    }
}
