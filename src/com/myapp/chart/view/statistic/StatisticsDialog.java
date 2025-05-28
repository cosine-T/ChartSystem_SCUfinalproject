package com.myapp.chart.view.statistic;

import com.myapp.chart.model.ChannelData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * 统计对话框：显示选择通道并计算其最大值、最小值、均值和方差
 */
public class StatisticsDialog extends JDialog {

    // 常量配置
    private static final String DIALOG_TITLE = "通道统计";
    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_HEIGHT = 300;
    private static final int FLOW_GAP = 5;
    private static final String BUTTON_STAT = "统计";
    private static final String BUTTON_CANCEL = "取消";
    private static final String MSG_SELECT_WARNING = "请至少选择一个通道。";
    private static final String MSG_WARNING_TITLE = "提示";
    private static final String MSG_RESULTS_TITLE = "统计结果";
    private static final String DECIMAL_PATTERN = "0.000";

    private final JList<ChannelData> channelList;
    private final DecimalFormat df = new DecimalFormat(DECIMAL_PATTERN);

    /**
     * 构造函数：初始化对话框与组件
     */
    public StatisticsDialog(Frame owner, List<ChannelData> channels) {
        super(owner, DIALOG_TITLE, true);
        channelList = new JList<>(channels.toArray(new ChannelData[0]));
        initializeDialog(owner);
        add(createListScrollPane(), BorderLayout.CENTER);
        add(createButtonPane(), BorderLayout.SOUTH);
    }

    /**
     * 初始化对话框属性
     */
    private void initializeDialog(Frame owner) {
        setLayout(new BorderLayout(FLOW_GAP, FLOW_GAP));
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(owner);
        channelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    /**
     * 创建通道列表滚动面板
     */
    private JScrollPane createListScrollPane() {
        return new JScrollPane(channelList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * 创建底部按钮面板
     */
    private JPanel createButtonPane() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, FLOW_GAP, FLOW_GAP));
        JButton statBtn = new JButton(BUTTON_STAT);
        JButton cancelBtn = new JButton(BUTTON_CANCEL);
        statBtn.addActionListener(this::onStat);
        cancelBtn.addActionListener(e -> dispose());
        panel.add(statBtn);
        panel.add(cancelBtn);
        return panel;
    }

    /**
     * 统计按钮事件：计算并显示选中通道的统计指标
     */
    private void onStat(ActionEvent e) {
        List<ChannelData> selected = channelList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, MSG_SELECT_WARNING,
                    MSG_WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }
        String resultText = buildStatisticsText(selected);
        showResults(resultText);
        dispose();
    }

    /**
     * 构建统计结果文本：最大、最小、均值、方差
     */
    private String buildStatisticsText(List<ChannelData> channels) {
        StringBuilder sb = new StringBuilder();
        for (ChannelData ch : channels) {
            double[] data = ch.getData();
            double[] clean = DoubleStream.of(data)
                    .filter(d -> !Double.isNaN(d))
                    .toArray();
            double min = DoubleStream.of(clean).min().orElse(Double.NaN);
            double max = DoubleStream.of(clean).max().orElse(Double.NaN);
            double mean = DoubleStream.of(clean).average().orElse(Double.NaN);
            double variance = computeVariance(clean, mean);

            sb.append(ch.getName()).append("\n")
                    .append(String.format("  最大: %s, 最小: %s\n", df.format(max), df.format(min)))
                    .append(String.format("  均值: %s, 方差: %s\n\n", df.format(mean), df.format(variance)));
        }
        return sb.toString();
    }

    /**
     * 计算方差
     */
    private double computeVariance(double[] data, double mean) {
        return DoubleStream.of(data)
                .map(d -> (d - mean) * (d - mean))
                .average().orElse(Double.NaN);
    }

    /**
     * 弹出结果对话框显示统计文本
     */
    private void showResults(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setEditable(false);
        JScrollPane scroll = new JScrollPane(ta,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JOptionPane.showMessageDialog(this, scroll,
                MSG_RESULTS_TITLE, JOptionPane.INFORMATION_MESSAGE);
    }
}
