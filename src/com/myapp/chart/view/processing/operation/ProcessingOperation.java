package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public interface ProcessingOperation {
    /** 执行运算，返回新的 ChannelData（可能带高亮） */
    ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual);

    /** 用于 UI 下拉列表显示 */
    String getName();

    /** 是否需要数字参数（如放大倍数、阈值） */
    boolean needsParam();

    /** 是否需要窗口大小参数（仅滑动平均） */
    boolean needsWindowSize();

    /** 是否需要标记类型选择（>=/<=） */
    boolean needsMarkType();
}
