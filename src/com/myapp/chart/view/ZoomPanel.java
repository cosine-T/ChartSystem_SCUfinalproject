package com.myapp.chart.view;

import com.myapp.chart.controller.ChartController;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * 缩放面板：全局 X 轴缩放包括自动缩放、一键时长按钮及对数滑块 (1%–1000%)
 */
public class ZoomPanel extends JPanel {

    // 常量配置
    private static final int SLIDER_MIN = 10;
    private static final int SLIDER_MAX = 1000;
    private static final int SLIDER_INITIAL = 100;
    private static final int FLOW_HGAP = 4;
    private static final int FLOW_VGAP = 4;
    private static final int BUTTON_MARGIN = 4;
    private static final int MAJOR_TICK = 25;
    private static final int MINOR_TICK = 5;
    private static final int LABEL_FIRST = 100;
    private static final int LABEL_SECOND = 500;
    private static final int LABEL_THIRD = 1000;

    private final ChartController controller;
    private final JSlider zoomSlider;
    private final JButton autoBtn;
    private int lastValue = SLIDER_INITIAL;  // 记录上次滑块值

    public ZoomPanel(ChartController controller) {
        this.controller = controller;
        initializePanel();
        this.autoBtn = createAutoButton();
        this.zoomSlider = createZoomSlider();
        addComponents();
        bindEvents();
    }

    /** 初始化面板布局 */
    private void initializePanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, FLOW_HGAP, FLOW_VGAP));
    }

    /** 创建自动缩放按钮 */
    private JButton createAutoButton() {
        JButton btn = new JButton("自动X");
        btn.setToolTipText("自动缩放到适合最长通道");
        return btn;
    }

    /** 构造时长按钮，点击时缩放到指定秒数窗口 */
    private JButton createDurationButton(String text, int seconds) {
        JButton btn = new JButton(text);
        btn.setToolTipText("窗口时长 " + text);
        btn.addActionListener(e -> {
            controller.zoomToTimeWindow(seconds);
            resetSlider();
        });
        return btn;
    }

    /** 创建水平对数缩放滑块 */
    private JSlider createZoomSlider() {
        JSlider slider = new JSlider(SLIDER_MIN, SLIDER_MAX, SLIDER_INITIAL);
        slider.setToolTipText("水平缩放 (%)");
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(MAJOR_TICK);
        slider.setMinorTickSpacing(MINOR_TICK);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(LABEL_FIRST, new JLabel("100%"));
        labels.put(LABEL_SECOND, new JLabel("500%"));
        labels.put(LABEL_THIRD, new JLabel("1000%"));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        return slider;
    }

    /** 将组件添加到面板 */
    private void addComponents() {
        add(autoBtn);
        add(createDurationButton("8h", 8 * 3600));
        add(createDurationButton("1h", 3600));
        add(createDurationButton("30m", 30 * 60));
        add(createDurationButton("10m", 10 * 60));
        add(createDurationButton("1m", 60));
        add(zoomSlider);
    }

    /** 绑定事件监听 */
    private void bindEvents() {
        autoBtn.addActionListener(e -> {
            controller.autoZoomHorizontal();
            resetSlider();
        });
        zoomSlider.addChangeListener(e -> {
            int v = zoomSlider.getValue();
            double factor = lastValue / (double) v;
            controller.zoomHorizontally(factor);
            lastValue = v;
        });
    }

    /** 重置滑块到初始值 */
    private void resetSlider() {
        lastValue = SLIDER_INITIAL;
        zoomSlider.setValue(SLIDER_INITIAL);
    }
}
