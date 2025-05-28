package com.myapp.chart.controller;

import com.myapp.chart.file.FileWriter;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;

import javax.swing.*;
import java.io.File;

/**
 * 通道控制器：负责单通道的导出与关闭操作
 */
public class ChannelController {

    // 默认导出文件扩展名
    private static final String EXT_TXT = "txt";
    private static final String EXT_BIN = "bin";
    private static final String DOT = ".";
    private static final String MSG_CONFIRM_TITLE = "导出通道";

    private final ChartController parentController;

    /**
     * 构造函数：注入主控制器
     */
    public ChannelController(ChartController parentController) {
        this.parentController = parentController;
    }

    /**
     * 导出指定通道到文件（支持 txt 与 bin 格式）
     */
    public void exportChannel(ChannelData channel) {
        if (channel == null) {
            return;
        }
        JFileChooser chooser = createFileChooser(channel);
        int result = chooser.showSaveDialog(parentController.getFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        String ext = getFileExtension(file.getName()).toLowerCase();
        if (EXT_BIN.equals(ext)) {
            FileWriter.exportBin(channel, file);
        } else {
            FileWriter.exportTxt(channel, file);
        }
    }

    /**
     * 关闭指定通道，并刷新主界面
     */
    public void closeChannel(ChannelData channel) {
        DataModel model = parentController.getModel();
        if (model == null) {
            return;
        }
        model.getChannels().remove(channel);
        refreshChart(model);
    }

    /**
     * 创建文件保存对话框，并设置默认文件名与过滤器
     */
    private JFileChooser createFileChooser(ChannelData channel) {
        JFileChooser chooser = new JFileChooser();
        String defaultName = channel.getName() + DOT + EXT_TXT;
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "文本文件 (*." + EXT_TXT + ")", EXT_TXT));
        return chooser;
    }

    /**
     * 从文件名获取扩展名
     */
    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf(DOT);
        if (idx > 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1);
        }
        return "";
    }

    /**
     * 刷新主界面数据
     */
    private void refreshChart(DataModel model) {
        ChartFrame frame = parentController.getFrame();
        frame.loadData(model);
    }
}