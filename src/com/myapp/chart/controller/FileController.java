package com.myapp.chart.controller;

import com.myapp.chart.file.BinaryReader;
import com.myapp.chart.file.EDFReader;
import com.myapp.chart.file.FileReader;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;

import javax.swing.*;
import java.awt.Component;
import java.io.File;

/**
 * 文件控制器：负责打开二进制和 EDF 文件，并将数据加载到模型中
 */
public class FileController {

    // 常量配置
    private static final String DIALOG_TITLE = "加载失败";
    private static final String FILE_CHOOSER_TITLE = "选择文件";

    private final ChartController chartController;

    /**
     * 构造函数：注入主控制器
     */
    public FileController(ChartController chartController) {
        this.chartController = chartController;
    }

    /**
     * 打开并读取自定义二进制文件
     */
    public void openBinary() {
        openWithReader(new BinaryReader());
    }

    /**
     * 打开并读取 EDF 文件
     */
    public void openEdf() {
        openWithReader(new EDFReader());
    }

    /**
     * 通用打开流程：显示文件对话框，读取并合并到数据模型，刷新视图
     */
    private void openWithReader(FileReader reader) {
        ChartFrame frame = chartController.getFrame();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(FILE_CHOOSER_TITLE);
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            DataModel newModel = reader.read(file);
            mergeModel(newModel);
            frame.loadData(chartController.getModel());
        } catch (Exception ex) {
            showError(frame, ex.getMessage());
        }
    }

    /**
     * 将新模型合并到主控制器的数据模型中
     */
    private void mergeModel(DataModel newModel) {
        DataModel model = chartController.getModel();
        if (model == null) {
            chartController.setModel(newModel);
        } else {
            for (ChannelData ch : newModel.getChannels()) {
                model.getChannels().add(ch);
            }
        }
    }

    /**
     * 显示错误对话框
     */
    private void showError(Component owner, String message) {
        JOptionPane.showMessageDialog(owner,
                message,
                DIALOG_TITLE,
                JOptionPane.ERROR_MESSAGE);
    }
}
