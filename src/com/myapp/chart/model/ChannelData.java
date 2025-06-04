package com.myapp.chart.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 单通道数据及其元信息。高亮区段基于时间（秒）。
 */
public class ChannelData {

    // —— 常量定义 —— //
    private static final double DEFAULT_Y_SCALE = 1.0;

    private static final int COLOR_BASE  = 50;    // RGB 最小值
    private static final int COLOR_RANGE = 206;   // RGB 可变范围（256 - 50）

    private static final int HASH_R_FACTOR = 31;
    private static final int HASH_G_FACTOR = 17;
    private static final int HASH_B_FACTOR = 47;

    // —— 成员变量 —— //
    private final String name;
    private final double[] data;
    private final float sampleRate;
    private double yScale = DEFAULT_Y_SCALE;
    private boolean visible = true;
    private final Color color;

    /** 高亮区段列表：每个元素为 [startTimeSec, endTimeSec] */
    private final List<double[]> highlightTimeRanges = new ArrayList<>();

    public ChannelData(String name, double[] data, float sampleRate) {
        this.name       = name;
        this.data       = data;
        this.sampleRate = sampleRate;
        this.color      = genColor(name.hashCode());
    }

    // —— Getter / Setter —— //
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
        int r = COLOR_BASE + Math.abs(h * HASH_R_FACTOR) % COLOR_RANGE;
        int g = COLOR_BASE + Math.abs(h * HASH_G_FACTOR) % COLOR_RANGE;
        int b = COLOR_BASE + Math.abs(h * HASH_B_FACTOR) % COLOR_RANGE;
        return new Color(r, g, b);
    }

    @Override
    public String toString() {
        return name;
    }
}
