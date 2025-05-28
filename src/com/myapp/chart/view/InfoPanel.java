package com.myapp.chart.view;

import com.biorecorder.edflib.HeaderConfig;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 信息面板：右侧元数据信息展示，可折叠/展开（简化为固定宽度）
 */
public class InfoPanel extends JPanel {

    // 常量配置
    private static final int PANEL_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String NEWLINE = "\n";

    private final JTextArea textArea = new JTextArea();

    public InfoPanel() {
        initializePanel();
    }

    /**
     * 初始化面板布局和文本区域
     */
    private void initializePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, DEFAULT_HEIGHT));
        setLayout(new BorderLayout());
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    /**
     * 填充文件及通道元数据信息；若为 EDF 文件，则追加 HeaderConfig 信息
     */
    public void setContent(DataModel model) {
        if (model == null || model.getChannels().isEmpty()) {
            clearContent();
            return;
        }

        StringBuilder sb = new StringBuilder();
        appendFileInfo(sb, model);
        appendChannelOverview(sb, model);
        appendEdfHeader(sb, model.getEdfHeader());

        textArea.setText(sb.toString());
    }

    /**
     * 清空面板内容
     */
    public void clearContent() {
        textArea.setText("");
    }

    /**
     * 构建文件基本信息
     */
    private void appendFileInfo(StringBuilder sb, DataModel model) {
        sb.append("文件: ").append(model.getFileName()).append(NEWLINE);
        sb.append("通道数: ").append(model.getChannels().size()).append(NEWLINE);
        long totalSamples = model.totalSamples();
        double durationSec = totalSamples / model.getChannels().get(0).getSampleRate();
        sb.append("总时长: ").append(durationSec).append(" 秒").append(NEWLINE).append(NEWLINE);
    }

    /**
     * 构建通道一览信息
     */
    private void appendChannelOverview(StringBuilder sb, DataModel model) {
        sb.append("--- 通道概览 ---").append(NEWLINE);
        for (ChannelData ch : model.getChannels()) {
            sb.append("- ")
                    .append(ch.getName())
                    .append(" (")
                    .append(ch.getSampleRate())
                    .append(" Hz)")
                    .append(NEWLINE);
        }
        sb.append(NEWLINE);
    }

    /**
     * 构建 EDF Header 信息（若存在）
     */
    private void appendEdfHeader(StringBuilder sb, HeaderConfig hdr) {
        if (hdr == null) {
            return;
        }
        sb.append("--- EDF Header ---").append(NEWLINE);
        sb.append("病人信息: ").append(hdr.getPatientIdentification()).append(NEWLINE);
        sb.append("记录信息: ").append(hdr.getRecordingIdentification()).append(NEWLINE);
        sb.append("开始时间: ")
                .append(formatDate(hdr.getRecordingStartDateTimeMs()))
                .append(NEWLINE);
        sb.append("数据记录数: ").append(hdr.getNumberOfDataRecords()).append(NEWLINE);
        sb.append("单记录时长: ").append(hdr.getDurationOfDataRecord()).append(" 秒");
    }

    /**
     * 格式化时间戳为指定格式字符串
     */
    private String formatDate(long timestampMs) {
        DateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
        return df.format(new Date(timestampMs));
    }
}
