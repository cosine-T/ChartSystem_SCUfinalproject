package com.myapp.chart.view;

import com.myapp.chart.controller.ChartController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.channel.ChannelPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 主框架：包含菜单、缩放面板、控制面板、通道面板、信息面板和状态栏。
 */
public class ChartFrame extends JFrame {
    // 窗口尺寸
    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 700;
    // 拆分面板设置
    private static final int CONTROL_PANEL_WIDTH = 170;
    private static final int INFO_PANEL_WIDTH = 200;
    private static final int DEFAULT_RIGHT_GROUP_WIDTH = 1000;
    private static final int DIVIDER_SIZE = 12;
    // 滚动条设置
    private static final int SCROLL_ORIENTATION = Adjustable.HORIZONTAL;
    private static final int SCROLL_INITIAL_VALUE = 0;
    private static final int SCROLL_INITIAL_EXTENT = 100;
    private static final int SCROLL_MIN = 0;
    private static final int SCROLL_MAX = 1000;
    private static final int BLOCK_INCREMENT_DIVISOR = 2;
    private static final int UNIT_INCREMENT_DIVISOR = 10;

    private final ChartController controller;
    private final ControlPanel controlPanel;
    private final JPanel channelContainer;
    private final InfoPanel infoPanel;
    private final JScrollBar hScroll;
    private final List<ChannelPanel> channelPanels = new ArrayList<>();

    public ChartFrame(ChartController controller) {
        super("Chart System");
        this.controller = controller;

        initializeFrame();
        this.controlPanel = createControlPanel();
        this.channelContainer = createChannelContainer();
        JScrollPane centerScroll = createCenterScrollPane(channelContainer);
        this.infoPanel = createInfoPanel();
        JSplitPane contentSplit = createContentSplitPane(controlPanel, centerScroll, infoPanel);
        add(contentSplit, BorderLayout.CENTER);
        this.hScroll = createHorizontalScrollBar();
        add(createStatusBar(hScroll), BorderLayout.SOUTH);

        // 将中心滚动面板的垂直滚动模型绑定到控制面板
        controlPanel.bindVerticalScrollModel(centerScroll.getVerticalScrollBar().getModel());
    }

    /**
     * 初始化主窗口
     */
    private void initializeFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setJMenuBar(controller.createMenuBar());
        add(new ZoomPanel(controller), BorderLayout.NORTH);
    }

    /**
     * 创建控制面板
     */
    private ControlPanel createControlPanel() {
        return new ControlPanel(controller);
    }

    /**
     * 创建通道容器面板
     */
    private JPanel createChannelContainer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * 创建中央滚动面板
     */
    private JScrollPane createCenterScrollPane(JPanel container) {
        return new JScrollPane(container,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * 创建信息面板
     */
    private InfoPanel createInfoPanel() {
        return new InfoPanel();
    }

    /**
     * 创建内容拆分面板
     */
    private JSplitPane createContentSplitPane(JComponent left, JScrollPane center, JComponent right) {
        // 左右拆分：左侧为控制面板，右侧暂留位置供下一步拆分
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, null);
        leftSplit.setOneTouchExpandable(true);
        leftSplit.setDividerSize(DIVIDER_SIZE);
        leftSplit.setContinuousLayout(true);
        leftSplit.setDividerLocation(CONTROL_PANEL_WIDTH);

        // 右侧拆分：中间为通道列表，右侧为信息面板
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, center, right);
        rightSplit.setOneTouchExpandable(true);
        rightSplit.setDividerSize(DIVIDER_SIZE);
        rightSplit.setContinuousLayout(true);
        rightSplit.setDividerLocation(DEFAULT_RIGHT_GROUP_WIDTH - INFO_PANEL_WIDTH);

        leftSplit.setRightComponent(rightSplit);
        return leftSplit;
    }

    /**
     * 创建水平滚动条
     */
    private JScrollBar createHorizontalScrollBar() {
        JScrollBar scrollBar = new JScrollBar(SCROLL_ORIENTATION,
                SCROLL_INITIAL_VALUE, SCROLL_INITIAL_EXTENT,
                SCROLL_MIN, SCROLL_MAX);
        scrollBar.addAdjustmentListener(e -> controller.onHScroll(e.getValue()));
        return scrollBar;
    }

    /**
     * 创建状态栏并添加滚动条
     */
    private JPanel createStatusBar(JScrollBar scrollBar) {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.add(scrollBar, BorderLayout.SOUTH);
        return statusBar;
    }

    /**
     * 加载或刷新数据（首次打开、关闭或新增通道时调用）
     */
    public void loadData(DataModel model) {
        // 首次打开时，将窗口长度设置为全样本长度，并偏移归零
        if (model != null && model.getWindowLength() < model.totalSamples()) {
            model.setWindowLength(model.totalSamples());
            model.setCurrentOffset(0);
        }

        channelContainer.removeAll();
        channelPanels.clear();

        if (model == null || model.getChannels().isEmpty()) {
            // 无数据时，显示空状态
            controlPanel.refreshEmpty();
            infoPanel.clearContent();
            hScroll.setEnabled(false);
        } else {
            // 有数据时，创建并添加各通道面板
            for (ChannelData ch : model.getChannels()) {
                ChannelPanel panel = new ChannelPanel(controller, ch, model);
                channelPanels.add(panel);
                channelContainer.add(panel);
            }
            controlPanel.refresh(model);
            infoPanel.setContent(model);

            int total = model.totalSamples();
            int view = model.getWindowLength();
            hScroll.setEnabled(true);
            hScroll.setMaximum(Math.max(total, view));
            hScroll.setVisibleAmount(view);
            hScroll.setBlockIncrement(Math.max(view / BLOCK_INCREMENT_DIVISOR, 1));
            hScroll.setUnitIncrement(Math.max(view / UNIT_INCREMENT_DIVISOR, 1));
        }

        // 重新布局并居中Y轴
        revalidate();
        repaint();
        centerYAll();
    }

    /**
     * 更新视图（仅在缩放/滚动但通道未变更时调用）
     */
    public void updateView() {
        channelPanels.forEach(JComponent::repaint);
        DataModel model = controller.getDataModel();
        if (model != null && !model.getChannels().isEmpty()) {
            hScroll.setEnabled(true);
            hScroll.setValue(model.getCurrentOffset());
            hScroll.setVisibleAmount(model.getWindowLength());
        } else {
            hScroll.setEnabled(false);
        }
    }

    /**
     * 让指定通道 Y 轴居中
     */
    public void centerY(ChannelData ch) {
        channelPanels.stream()
                .filter(p -> p.getChannelData() == ch)
                .findFirst()
                .ifPresent(ChannelPanel::resetYScroll);
    }

    /**
     * 让所有通道 Y 轴居中
     */
    public void centerYAll() {
        channelPanels.forEach(ChannelPanel::resetYScroll);
    }

    /**
     * 居中单通道并刷新视图
     */
    public void centerYAndRefresh(ChannelData ch) {
        centerY(ch);
        updateView();
    }
}
