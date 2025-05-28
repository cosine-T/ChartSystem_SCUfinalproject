package com.myapp.chart.view;

import com.myapp.chart.controller.ChartController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.channel.ChannelPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * 控制面板：显示各通道的可见性、导出/关闭、上下移动及纵向缩放控制。
 */
public class ControlPanel extends JPanel {

    // 常量配置
    private static final int PANEL_WIDTH = 170;
    private static final int DEFAULT_PANEL_HEIGHT = 100;
    private static final int SLIDER_MIN = -100;
    private static final int SLIDER_MAX = 100;
    private static final int SLIDER_INITIAL = 0;
    private static final int SLIDER_PREF_WIDTH = 20;
    private static final int SLIDER_PREF_HEIGHT = 60;
    private static final int TICK_SPACING = 50;
    private static final int LABEL_OFFSET = 2;
    private static final int ITEM_H = ChannelPanel.PREF_H;
    private static final int ITEM_WIDTH = PANEL_WIDTH - 10;
    private static final int BUTTON_SPACING = 2;

    private final ChartController controller;
    private final JPanel listPanel = new JPanel();
    private final JScrollPane listScroll;

    public ControlPanel(ChartController controller) {
        this.controller = controller;
        // 设置面板大小和布局
        setPreferredSize(new Dimension(PANEL_WIDTH, DEFAULT_PANEL_HEIGHT));
        setLayout(new BorderLayout());

        // 列表容器
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listScroll = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(listScroll, BorderLayout.CENTER);
    }

    /**
     * 绑定垂直滚动条模型，使控制面板与通道列表同步滚动
     */
    public void bindVerticalScrollModel(BoundedRangeModel model) {
        listScroll.getVerticalScrollBar().setModel(model);
    }

    /**
     * 刷新面板内容：根据数据模型创建通道控制项
     */
    public void refresh(DataModel model) {
        listPanel.removeAll();
        for (ChannelData ch : model.getChannels()) {
            listPanel.add(new ChannelControlItem(ch));
        }
        revalidate();
        repaint();
    }

    /**
     * 刷新为空状态：清空列表
     */
    public void refreshEmpty() {
        listPanel.removeAll();
        revalidate();
        repaint();
    }

    /**
     * 通道控制项：包含可见开关、操作按钮及纵向缩放滑块
     */
    private class ChannelControlItem extends JPanel {
        private final double baseScale;
        private final JSlider slider;

        ChannelControlItem(ChannelData ch) {
            this.baseScale = ch.getyScale();

            // 初始化纵向缩放滑块（对数刻度 0.1×–10×）
            slider = new JSlider(JSlider.VERTICAL, SLIDER_MIN, SLIDER_MAX, SLIDER_INITIAL);
            slider.setPreferredSize(new Dimension(SLIDER_PREF_WIDTH, SLIDER_PREF_HEIGHT));
            slider.setToolTipText("纵向缩放：0.1× – 10× (对数刻度)");
            slider.setMajorTickSpacing(TICK_SPACING);
            slider.setPaintTicks(true);
            Hashtable<Integer, JLabel> labels = new Hashtable<>();
            labels.put(SLIDER_MIN, new JLabel("0.1×"));
            labels.put(SLIDER_INITIAL, new JLabel("1×"));
            labels.put(SLIDER_MAX, new JLabel("10×"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setAlignmentX(Component.LEFT_ALIGNMENT);
            slider.addChangeListener(e -> {
                double factor = Math.pow(10, slider.getValue() / 100.0);
                ch.setyScale(baseScale * factor);
                controller.refreshView();
            });

            // 布局和边距
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(LABEL_OFFSET, LABEL_OFFSET, LABEL_OFFSET, LABEL_OFFSET));

            // 第一行：可见性复选框
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, LABEL_OFFSET, 0));
            JCheckBox cb = new JCheckBox(ch.getName(), ch.isVisible());
            cb.addActionListener(e -> {
                ch.setVisible(cb.isSelected());
                controller.refreshView();
            });
            row1.add(cb);
            add(row1);

            // 第二行：按钮列与滑块
            JPanel row2 = new JPanel(new BorderLayout());

            // 按钮列：导出、关闭、移动
            JPanel btnCol = new JPanel();
            btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));
            JButton expBtn = new JButton("导出");
            JButton closeBtn = new JButton("关闭");
            JButton upBtn = new JButton("↑");
            JButton downBtn = new JButton("↓");
            for (JButton btn : new JButton[]{expBtn, closeBtn, upBtn, downBtn}) {
                btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            expBtn.addActionListener(e -> controller.exportChannel(ch));
            closeBtn.addActionListener(e -> controller.closeChannel(ch));
            upBtn.setToolTipText("上移通道");
            upBtn.addActionListener(e -> moveChannel(ch, -1));
            downBtn.setToolTipText("下移通道");
            downBtn.addActionListener(e -> moveChannel(ch, 1));

            btnCol.add(expBtn);
            btnCol.add(Box.createVerticalStrut(BUTTON_SPACING));
            btnCol.add(closeBtn);
            btnCol.add(Box.createVerticalStrut(BUTTON_SPACING));
            btnCol.add(Box.createVerticalGlue());
            btnCol.add(upBtn);
            btnCol.add(Box.createVerticalStrut(BUTTON_SPACING));
            btnCol.add(downBtn);
            row2.add(btnCol, BorderLayout.WEST);

            // 滑块列：自动纵向缩放按钮 + 滑块
            JPanel sliderCol = new JPanel();
            sliderCol.setLayout(new BoxLayout(sliderCol, BoxLayout.Y_AXIS));
            sliderCol.setBorder(BorderFactory.createEmptyBorder(0, LABEL_OFFSET * 5, 0, 0));
            JButton autoVBtn = new JButton("自动V");
            autoVBtn.setToolTipText("恢复自动纵向缩放");
            autoVBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            autoVBtn.addActionListener(e -> {
                slider.setValue(SLIDER_INITIAL);
                ch.setyScale(baseScale);
                controller.getFrame().centerYAndRefresh(ch);
            });
            sliderCol.add(autoVBtn);
            sliderCol.add(Box.createVerticalStrut(BUTTON_SPACING));
            sliderCol.add(slider);
            row2.add(sliderCol, BorderLayout.CENTER);

            add(row2);

            // 固定高度，保持列表一致
            setPreferredSize(new Dimension(ITEM_WIDTH, ITEM_H));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_H));
            setMinimumSize(new Dimension(ITEM_WIDTH, ITEM_H));
        }

        /**
         * 调整通道顺序并重载数据
         */
        private void moveChannel(ChannelData ch, int delta) {
            // 重置缩放并居中
            slider.setValue(SLIDER_INITIAL);
            ch.setyScale(baseScale);
            controller.getFrame().centerY(ch);
            // 调整顺序与刷新
            controller.moveChannel(ch, delta);
            controller.reloadData();
        }
    }
}