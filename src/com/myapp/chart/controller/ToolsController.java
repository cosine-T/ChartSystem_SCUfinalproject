package com.myapp.chart.controller;

import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;
import com.myapp.chart.view.processing.ProcessingDialog;
import com.myapp.chart.view.statistic.StatisticsDialog;

import javax.swing.*;

/**
 * 工具控制器：负责统计与处理对话框展示
 */
public class ToolsController {

    // 常量配置
    private static final String TITLE_WARNING         = "提示";
    private static final String MESSAGE_NO_CHANNELS  = "无可%s的通道";
    private static final String OPERATION_STATS      = "统计";
    private static final String OPERATION_PROCESS    = "处理";
    private static final int    MESSAGE_TYPE_WARNING = JOptionPane.WARNING_MESSAGE;

    private final ChartController chartController;

    /**
     * 构造函数：注入主控制器
     */
    public ToolsController(ChartController chartController) {
        this.chartController = chartController;
    }

    /**
     * 显示统计对话框；若无通道则弹警告
     */
    public void showStatistics() {
        ChartFrame frame = chartController.getFrame();
        DataModel model = chartController.getModel();
        if (model == null || model.getChannels().isEmpty()) {
            String message = String.format(MESSAGE_NO_CHANNELS, OPERATION_STATS);
            JOptionPane.showMessageDialog(frame, message, TITLE_WARNING, MESSAGE_TYPE_WARNING);
            return;
        }
        new StatisticsDialog(frame, model.getChannels()).setVisible(true);
    }

    /**
     * 显示处理对话框；若无通道则弹警告
     */
    public void showProcessing() {
        ChartFrame frame = chartController.getFrame();
        DataModel model = chartController.getModel();
        if (model == null || model.getChannels().isEmpty()) {
            String message = String.format(MESSAGE_NO_CHANNELS, OPERATION_PROCESS);
            JOptionPane.showMessageDialog(frame, message, TITLE_WARNING, MESSAGE_TYPE_WARNING);
            return;
        }
        new ProcessingDialog(frame, model).setVisible(true);
    }
}
