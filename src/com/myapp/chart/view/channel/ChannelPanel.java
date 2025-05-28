package com.myapp.chart.view.channel;

import com.myapp.chart.controller.ChartController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.simulation.WindowProvider;

import javax.swing.*;
import java.awt.*;

/**
 * 单通道波形面板：负责 UI 结构，绘图逻辑由 ChannelRenderer 处理
 */
public class ChannelPanel extends JPanel implements ChannelConstants {

    // 常量配置
    private static final Dimension PREF_SIZE = new Dimension(400, PREF_H);
    private static final int SCROLL_ORIENTATION = JScrollBar.VERTICAL;
    private static final int SCROLL_INITIAL = 50;
    private static final int SCROLL_MIN = 0;
    private static final int SCROLL_MAX = 100;

    // 控制器与数据
    private final ChartController controller;
    private final ChannelData channelData;
    private final DataModel dataModel;
    private final WindowProvider windowProvider;

    // 滚动条与状态
    private final JScrollBar yScroll;
    private double scrollPos = 0.0; // 范围 -1 ~ 1

    /**
     * 构造函数：支持在线模式与批量模式
     */
    public ChannelPanel(ChartController controller,
                        ChannelData channelData,
                        DataModel dataModel,
                        WindowProvider windowProvider) {
        this.controller = controller;
        this.channelData = channelData;
        this.dataModel = dataModel;
        this.windowProvider = windowProvider;

        // 面板尺寸与布局
        setPreferredSize(PREF_SIZE);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, PREF_H));
        setLayout(new BorderLayout());

        // 垂直滚动条，用于 Y 轴偏移控制
        yScroll = createYScrollBar();
        add(createScrollContainer(), BorderLayout.EAST);
    }

    /**
     * 构造函数：纯回放模式
     */
    public ChannelPanel(ChartController controller,
                        ChannelData channelData,
                        DataModel dataModel) {
        this(controller, channelData, dataModel, null);
    }

    /** 创建垂直滚动条 */
    private JScrollBar createYScrollBar() {
        JScrollBar scroll = new JScrollBar(
                SCROLL_ORIENTATION,
                SCROLL_INITIAL,
                SCROLL_MIN,
                SCROLL_MIN,
                SCROLL_MAX
        );
        scroll.addAdjustmentListener(e -> {
            scrollPos = (SCROLL_INITIAL - scroll.getValue()) / (double) SCROLL_INITIAL;
            repaint();
        });
        // 在线模式锁定滚动条
        if (dataModel == null && windowProvider != null) {
            scroll.setEnabled(false);
            scroll.setVisible(false);
        }
        return scroll;
    }

    /** 创建滚动条容器 */
    private JPanel createScrollContainer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(CTRL_W, PREF_H));
        panel.add(yScroll, BorderLayout.CENTER);
        return panel;
    }

    /** 重置 Y 轴滚动到中心 */
    public void resetYScroll() {
        scrollPos = 0.0;
        yScroll.setValue(SCROLL_INITIAL);
    }

    /** 获取通道数据，用于渲染 */
    public ChannelData getChannelData() {
        return channelData;
    }

    /** 获取滚动位置，用于渲染偏移 */
    public double getScrollPos() {
        return scrollPos;
    }

    /** 获取当前偏移（样本索引） */
    int curOffset() {
        return dataModel != null
                ? dataModel.getCurrentOffset()
                : windowProvider.getOffset();
    }

    /** 获取窗口长度（样本数） */
    int winLen() {
        return dataModel != null
                ? dataModel.getWindowLength()
                : windowProvider.getWindowLength();
    }

    /** 获取首个通道采样率或默认值 */
    double firstSampleRateOr(double defaultRate) {
        return dataModel != null && !dataModel.getChannels().isEmpty()
                ? dataModel.getChannels().get(0).getSampleRate()
                : defaultRate;
    }

    /** 绘制入口：委托给 ChannelRenderer */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ChannelRenderer.paint((Graphics2D) g.create(), this);
    }
}
