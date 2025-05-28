package com.myapp.chart.controller;

import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

/**
 * 主控制器：负责应用启动、菜单构建以及各子控制器委派
 */
public class ChartController {

    // 常量配置
    private static final String APP_TITLE      = "Chart System";
    private static final String MENU_FILE      = "文件";
    private static final String MENU_VIEW      = "视图";
    private static final String MENU_TOOL      = "工具";
    private static final String ITEM_OPEN_BIN  = "打开二进制...";
    private static final String ITEM_OPEN_EDF  = "打开 EDF...";
    private static final String ITEM_EXIT      = "退出";
    private static final String ITEM_MONITOR   = "启动监护仪模拟";
    private static final String ITEM_STATS     = "统计";
    private static final String ITEM_PROCESS   = "处理";

    // 子控制器
    private final FileController    fileController;
    private final ViewController    viewController;
    private final ToolsController   toolsController;
    private final ChannelController channelController;

    private ChartFrame frame;
    private DataModel  model;

    /**
     * 构造函数：初始化各子控制器
     */
    public ChartController() {
        this.fileController    = new FileController(this);
        this.viewController    = new ViewController(this);
        this.toolsController   = new ToolsController(this);
        this.channelController = new ChannelController(this);
    }

    /**
     * 启动应用：创建主窗口并显示
     */
    public void init() {
        frame = new ChartFrame(this);
        frame.setTitle(APP_TITLE);
        frame.setVisible(true);
    }

    /**
     * 构建主菜单栏
     */
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createToolMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu(MENU_FILE);
        menu.add(createMenuItem(ITEM_OPEN_BIN, e -> fileController.openBinary()));
        menu.add(createMenuItem(ITEM_OPEN_EDF, e -> fileController.openEdf()));
        menu.addSeparator();
        menu.add(createMenuItem(ITEM_EXIT, e -> System.exit(0)));
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu(MENU_VIEW);
        menu.add(createMenuItem(ITEM_MONITOR, e -> viewController.showMonitor()));
        return menu;
    }

    private JMenu createToolMenu() {
        JMenu menu = new JMenu(MENU_TOOL);
        menu.add(createMenuItem(ITEM_STATS,   e -> toolsController.showStatistics()));
        menu.add(createMenuItem(ITEM_PROCESS, e -> toolsController.showProcessing()));
        return menu;
    }

    /**
     * 通用菜单项创建
     */
    private JMenuItem createMenuItem(String title, ActionListener listener) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(listener);
        return item;
    }

    /*======== 滚动与缩放操作 ========*/
    public void onHScroll(int value)               { viewController.onHScroll(value); }
    public void zoomHorizontally(double factor)    { viewController.zoomHorizontally(factor); frame.centerYAll(); }
    public void autoZoomHorizontal()               { viewController.autoZoomHorizontal(); frame.centerYAll(); }
    public void zoomToTimeWindow(int seconds)      { viewController.zoomToTimeWindow(seconds); }

    /*======== 通道操作 ========*/
    public void exportChannel(ChannelData ch)      { channelController.exportChannel(ch); }
    public void closeChannel(ChannelData ch)       { channelController.closeChannel(ch); }

    /**
     * 在模型中移动通道顺序：direction = -1 上移，+1 下移
     */
    public void moveChannel(ChannelData ch, int direction) {
        List<ChannelData> channels = model.getChannels();
        int idx = channels.indexOf(ch);
        int newIdx = idx + direction;
        if (idx >= 0 && newIdx >= 0 && newIdx < channels.size()) {
            Collections.swap(channels, idx, newIdx);
        }
    }

    /**
     * 重新加载数据并刷新界面
     */
    public void reloadData()    { frame.loadData(model); }

    /**
     * 刷新当前视图（仅缩放/平移时调用）
     */
    public void refreshView()   { frame.updateView(); }

    /*======== Getter / Setter ========*/
    public ChartFrame getFrame()            { return frame; }
    public DataModel  getModel()            { return model; }
    public void       setModel(DataModel m) { this.model = m; }
    public DataModel  getDataModel()        { return model; }
}
