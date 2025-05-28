package com.myapp.chart.view.simulation;

import com.myapp.chart.controller.ViewController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.file.FileWriter;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;
import java.util.Arrays;

public class ExportSimulation {

    // 导出范围枚举，避免使用魔法数字
    private enum ExportScope {
        CURRENT_WINDOW(0),
        ALL_RECORDS(1);

        private final int index;

        ExportScope(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static ExportScope fromIndex(int idx) {
            for (ExportScope scope : values()) {
                if (scope.getIndex() == idx) {
                    return scope;
                }
            }
            throw new IllegalArgumentException("未知的导出范围索引: " + idx);
        }
    }

    // 常量定义
    private static final String DIALOG_TITLE = "导出";
    private static final String LABEL_CHANNEL = "选择通道：";
    private static final String LABEL_SCOPE = "导出范围：";
    private static final String[] SCOPE_OPTIONS = {"仅当前窗口", "全部已录"};
    private static final String FILE_FILTER_DESCRIPTION = "文本文件 (*.txt)";
    private static final String FILE_EXTENSION = "txt";
    private static final String DEFAULT_FILE_NAME_SUFFIX = ".txt";
    private static final String ERROR_TITLE = "Error";
    private static final String EXPORT_FAIL_MESSAGE = "导出失败: ";

    /**
     * 弹出导出对话框
     *
     * @param parent 所属窗口
     * @param chs    通道列表
     * @param ctrl   PlaybackController（提供 offset / windowLen / written）
     * @param names  通道显示名称
     */
    public static void show(JFrame parent,
                            List<ChannelData> chs,
                            ViewController.PlaybackController ctrl,
                            String[] names) {
        JComboBox<String> cbChan = new JComboBox<>(names);
        JComboBox<String> cbScope = new JComboBox<>(SCOPE_OPTIONS);

        Object[] message = {
                LABEL_CHANNEL, cbChan,
                LABEL_SCOPE, cbScope
        };

        int option = JOptionPane.showConfirmDialog(
                parent, message, DIALOG_TITLE,
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        int chanIndex = cbChan.getSelectedIndex();
        ChannelData source = chs.get(chanIndex);

        ExportScope scope = ExportScope.fromIndex(cbScope.getSelectedIndex());

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                FILE_FILTER_DESCRIPTION, FILE_EXTENSION));
        fileChooser.setSelectedFile(new File(names[chanIndex] + DEFAULT_FILE_NAME_SUFFIX));

        if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        try {
            int startIndex;
            int endIndex;
            long written = ctrl.getWritten();

            if (scope == ExportScope.CURRENT_WINDOW) {
                startIndex = ctrl.getOffset();
                endIndex = (int) Math.min(startIndex + ctrl.getWindowLength(), written);
            } else {
                startIndex = 0;
                endIndex = (int) written;
            }

            double[] slice = Arrays.copyOfRange(
                    source.getData(), startIndex, endIndex);
            ChannelData exportData = new ChannelData(
                    source.getName() + "_export",
                    slice,
                    source.getSampleRate());

            FileWriter.exportTxt(exportData, file);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    EXPORT_FAIL_MESSAGE + ex.getMessage(),
                    ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
