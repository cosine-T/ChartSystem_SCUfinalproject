package com.myapp.chart;

import com.myapp.chart.controller.ChartController;

import javax.swing.*;

/**
 * 程序入口。
 */
public class ChartApp {

    public static void main(String[] args) {
        /* 建议在 EDT 线程启动 Swing UI */
        SwingUtilities.invokeLater(() -> {
            try {
                // 使用系统默认外观
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new ChartController().init();
        });
    }
}
