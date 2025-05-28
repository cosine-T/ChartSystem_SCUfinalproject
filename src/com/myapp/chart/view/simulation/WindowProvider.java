package com.myapp.chart.view.simulation;

import java.awt.Dimension;

/**
 * 简单接口：提供当前窗口（波形区）的 offset 与 length，
 * 以及对话框建议大小等通用窗口参数。
 */
public interface WindowProvider {
    /** 波形区当前的样本起点偏移 */
    int getOffset();

    /** 波形区当前的可视样本数（窗口长度） */
    int getWindowLength();
}