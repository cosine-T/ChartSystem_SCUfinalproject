package com.myapp.chart.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 单通道数据及其元信息。高亮区段基于时间（秒）。
 */
public class ChannelData {

    private final String name;
    private final double[] data;
    private final float sampleRate;
    private double yScale = 1.0;               // 纵向缩放
    private boolean visible = true;            // 是否在图中显示
    private final Color color;                 // 绘制颜色

    /** 高亮区段列表：每个元素为 [startTimeSec, endTimeSec] */
    private final List<double[]> highlightTimeRanges = new ArrayList<>();

    public ChannelData(String name, double[] data, float sampleRate) {
        this.name       = name;
        this.data       = data;
        this.sampleRate = sampleRate;
        this.color      = genColor(name.hashCode());
    }

    /* ----------- getters / setters ----------- */

    public String getName()              { return name; }
    public double[] getData()            { return data; }
    public float getSampleRate()         { return sampleRate; }
    public double getyScale()            { return yScale; }
    public void   setyScale(double ys)   { this.yScale = ys; }
    public boolean isVisible()           { return visible; }
    public void    setVisible(boolean v) { this.visible = v; }
    public Color   getColor()            { return color; }

    /** 返回所有高亮区段（时间秒为单位） */
    public List<double[]> getHighlightTimeRanges() {
        return highlightTimeRanges;
    }

    /** 添加一个高亮区段，参数为开始/结束时间（秒） */
    public void addHighlightTimeRange(double startSec, double endSec) {
        highlightTimeRanges.add(new double[]{startSec, endSec});
    }

    /** 清空所有高亮区段 */
    public void clearHighlightTimeRanges() {
        highlightTimeRanges.clear();
    }

    /**
     * 为兼容旧代码，保留基于索引的接口：
     * 将样本索引转换为时间后存储
     */
    @Deprecated
    public void addHighlightRange(int startIdx, int endIdx) {
        double startSec = startIdx / sampleRate;
        double endSec   = endIdx   / sampleRate;
        addHighlightTimeRange(startSec, endSec);
    }

    /** 根据通道名哈希生成一种可区分颜色 */
    private static Color genColor(int h) {
        int r = 50 + Math.abs(h * 31) % 206;
        int g = 50 + Math.abs(h * 17) % 206;
        int b = 50 + Math.abs(h * 47) % 206;
        return new Color(r, g, b);
    }

    @Override
    public String toString() {
        return name;
    }
}
