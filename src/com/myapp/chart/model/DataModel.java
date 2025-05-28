package com.myapp.chart.model;

import com.biorecorder.edflib.HeaderConfig;

import java.util.List;

/**
 * 保存全局数据：文件名、所有通道、当前视窗 offset + windowLength 等。
 * 同时可选地保存 EDF 文件头（HeaderConfig），非 EDF 文件该字段为 null。
 */
public class DataModel {

    private final String fileName;
    private final List<ChannelData> channels;
    private final HeaderConfig edfHeader;    // 如果是 EDF 文件，则非 null

    private int currentOffset = 0;       // 当前视窗起点（样本索引）
    private int windowLength;            // 当前视窗长度（样本点数）

    /**
     * 非 EDF 文件时使用此构造器，edfHeader 自动设为 null。
     */
    public DataModel(String fileName, List<ChannelData> channels) {
        this(fileName, channels, null);
    }

    /**
     * EDF 文件时使用此构造器，将 HeaderConfig 一并保存。
     */
    public DataModel(String fileName,
                     List<ChannelData> channels,
                     HeaderConfig edfHeader) {
        this.fileName     = fileName;
        this.channels     = channels;
        this.edfHeader    = edfHeader;
        // 默认窗口：显示全文件长度
        this.windowLength = totalSamples();
    }

    /* ------------ getters / setters ----------- */

    /** 原始文件名（含扩展名） */
    public String getFileName() {
        return fileName;
    }

    /** 所有通道列表 */
    public List<ChannelData> getChannels() {
        return channels;
    }

    /** 如果是 EDF 文件，该方法返回非 null 的 HeaderConfig；否则返回 null */
    public HeaderConfig getEdfHeader() {
        return edfHeader;
    }

    /** 当前视窗起点 */
    public int getCurrentOffset() {
        return currentOffset;
    }

    /**
     * 设置视窗起点，自动做上下界校验。
     */
    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = Math.max(0, currentOffset);
        // 避免越界
        int total = totalSamples();
        if (this.currentOffset + windowLength > total) {
            this.currentOffset = Math.max(0, total - windowLength);
        }
    }

    /** 当前视窗长度 */
    public int getWindowLength() {
        return windowLength;
    }

    /**
     * 直接设置窗口长度（样本点数），并做上下界校验。
     * 通常用于“自动缩放”到某一固定长度。
     */
    public void setWindowLength(int windowLength) {
        this.windowLength = Math.max(1, windowLength);
        int total = totalSamples();
        if (this.windowLength > total) {
            this.windowLength = total;
        }
        if (currentOffset + this.windowLength > total) {
            this.currentOffset = Math.max(0, total - this.windowLength);
        }
    }

    /**
     * 按比例缩放当前窗口长度。
     * @param factor <1 放大（窗口变长），>1 缩小（窗口变短）
     */
    public void zoom(double factor) {
        int newLen = (int) (windowLength * factor);
        windowLength = Math.max(1, newLen);
        int total = totalSamples();
        if (currentOffset + windowLength > total) {
            currentOffset = Math.max(0, total - windowLength);
        }
    }

    /**
     * 以第一个通道的长度为准，全局样本总数。
     */
    public int totalSamples() {
        return channels.isEmpty() ? 0 : channels.get(0).getData().length;
    }
}
