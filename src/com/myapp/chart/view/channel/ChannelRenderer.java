package com.myapp.chart.view.channel;

import com.myapp.chart.model.ChannelData;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * 负责波形和高亮绘制，ChannelPanel 提供上下文
 */
final class ChannelRenderer {

    // 常量配置
    private static final int AXIS_OFFSET = 60;            // 左侧 Y 轴宽度
    private static final int CONTROL_WIDTH = 24;          // 右侧控件宽度
    private static final int GRID_X_COUNT = 10;           // X 轴网格数
    private static final int GRID_Y_COUNT = 4;            // Y 轴网格数
    private static final double DEFAULT_FS = 250.0;       // 默认采样率
    private static final int TOP_PADDING = 20;            // 顶部留白
    private static final Color GRID_COLOR = new Color(235, 235, 235);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 0, 80);
    private static final DecimalFormat Y_LABEL_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat X_LABEL_FORMAT = new DecimalFormat("0.##");
    private static final double HIGHLIGHT_ALPHA = 0.32;    // 高亮透明度 (80/255 ≈ 0.32)

    private ChannelRenderer() {}

    static void paint(Graphics2D g2, ChannelPanel ctx) {
        ChannelData ch = ctx.getChannelData();
        if (!ch.isVisible()) {
            return;
        }

        int width = ctx.getWidth();
        int height = ctx.getHeight() - 1;
        int plotX = AXIS_OFFSET;
        int plotWidth = width - AXIS_OFFSET - CONTROL_WIDTH;
        int plotHeight = height - TOP_PADDING;
        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        // 时间窗口参数
        int offset = ctx.curOffset();
        int window = ctx.winLen();
        double fsGlobal = ctx.firstSampleRateOr(DEFAULT_FS);
        double t0 = offset / fsGlobal;
        double tSpan = window / fsGlobal;

        // 背景
        g2.setClip(plotX, 0, plotWidth, plotHeight);
        g2.setColor(Color.WHITE);
        g2.fillRect(plotX, 0, plotWidth, plotHeight);

        // 高亮区域
        g2.setColor(HIGHLIGHT_COLOR);
        for (double[] range : ch.getHighlightTimeRanges()) {
            double start = range[0], end = range[1];
            if (end < t0 || start > t0 + tSpan) {
                continue;
            }
            double sRel = Math.max(0, start - t0);
            double eRel = Math.min(tSpan, end - t0);
            int x1 = plotX + (int) (sRel / tSpan * plotWidth);
            int x2 = plotX + (int) (eRel / tSpan * plotWidth);
            g2.fillRect(x1, 0, x2 - x1 + 1, plotHeight);
        }

        // 数据范围
        double[] data = ch.getData();
        double dMin = Double.POSITIVE_INFINITY;
        double dMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < window; i++) {
            int idx = (int) ((offset + i) * ch.getSampleRate() / fsGlobal);
            if (idx >= data.length) break;
            double v = data[idx];
            if (Double.isNaN(v)) continue;
            dMin = Math.min(dMin, v);
            dMax = Math.max(dMax, v);
        }
        if (!Double.isFinite(dMin)) {
            return;
        }

        double fullRange = dMax - dMin;
        double scale = ch.getyScale();
        double pad = fullRange * 0.2;
        double visibleRange = fullRange / scale + 2 * pad;
        double mid = (dMin + dMax) / 2;
        double y0 = mid - visibleRange / 2 + ctx.getScrollPos() * (fullRange - pad);

        double yStep = AxisUtil.niceStep(visibleRange, GRID_Y_COUNT);
        double yBase = Math.floor(y0 / yStep) * yStep;
        double yRange = yStep * GRID_Y_COUNT;

        // 绘制网格
        g2.setColor(GRID_COLOR);
        double xStep = AxisUtil.niceStep(tSpan, GRID_X_COUNT);
        double xStart = Math.floor(t0 / xStep) * xStep;
        for (double t = xStart; t <= t0 + tSpan + 1e-9; t += xStep) {
            int x = plotX + (int) ((t - t0) / tSpan * plotWidth);
            if (x >= plotX && x <= plotX + plotWidth) {
                g2.drawLine(x, 0, x, plotHeight);
            }
        }
        for (int i = 0; i <= GRID_Y_COUNT; i++) {
            double yVal = yBase + i * yStep;
            int y = AxisUtil.mapY(yVal, yBase, yRange, plotHeight);
            g2.drawLine(plotX, y, plotX + plotWidth, y);
        }

        // 绘制波形
        g2.setColor(ch.getColor());
        int liPrev = (int) (offset * ch.getSampleRate() / fsGlobal);
        int xPrev = plotX;
        int yPrev = AxisUtil.mapY(data[liPrev], yBase, yRange, plotHeight);
        for (int i = 1; i < window; i++) {
            int li = (int) ((offset + i) * ch.getSampleRate() / fsGlobal);
            if (li >= data.length) break;
            double v = data[li];
            if (Double.isNaN(v)) continue;
            int x = plotX + (int) (i / (double) window * plotWidth);
            int y = AxisUtil.mapY(v, yBase, yRange, plotHeight);
            g2.drawLine(xPrev, yPrev, x, y);
            xPrev = x; yPrev = y;
        }

        // 坐标轴与刻度
        g2.setClip(null);
        g2.setColor(Color.BLACK);
        // Y 轴
        g2.drawLine(plotX, 0, plotX, plotHeight);
        int exponent = (int) Math.floor(Math.log10(Math.max(Math.abs(yBase), Math.abs(yBase + yRange))));
        double scaleDiv = Math.pow(10, exponent);
        for (int i = 0; i <= GRID_Y_COUNT; i++) {
            double yVal = yBase + i * yStep;
            int y = AxisUtil.mapY(yVal, yBase, yRange, plotHeight);
            g2.drawLine(plotX - 3, y, plotX, y);
            String label = Y_LABEL_FORMAT.format(yVal / scaleDiv);
            int textWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, plotX - 5 - textWidth, y + 4);
        }
        if (exponent != 0) {
            g2.drawString("×10^" + exponent, plotX + 2, 10);
        }
        // X 轴
        g2.drawLine(plotX, plotHeight, plotX + plotWidth, plotHeight);
        boolean useHMS = tSpan > 60;
        for (double t = xStart; t <= t0 + tSpan + 1e-9; t += xStep) {
            int x = plotX + (int) ((t - t0) / tSpan * plotWidth);
            if (x >= plotX && x <= plotX + plotWidth) {
                g2.drawLine(x, plotHeight, x, plotHeight + 3);
                String txt = useHMS ? String.format("%02d:%02d:%02d",
                        (int) t / 3600, ((int) t % 3600) / 60, (int) t % 60)
                        : X_LABEL_FORMAT.format(t);
                int tw = g2.getFontMetrics().stringWidth(txt);
                g2.drawString(txt, x - tw/2, plotHeight + 15);
            }
        }

        // 通道名称
        g2.drawString(ch.getName(), plotX + plotWidth/2 - g2.getFontMetrics().stringWidth(ch.getName())/2, 14);
    }
}
