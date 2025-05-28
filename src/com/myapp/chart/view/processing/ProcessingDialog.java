package com.myapp.chart.view.processing;

import com.myapp.chart.controller.ChartController;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;
import com.myapp.chart.view.ChartFrame;
import com.myapp.chart.view.processing.operation.OperationFactory;
import com.myapp.chart.view.processing.operation.ProcessingOperation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

/**
 * 通道处理对话框：选择通道和操作，设置参数后应用处理并刷新主界面
 */
public class ProcessingDialog extends JDialog {

    // 常量配置
    private static final String DIALOG_TITLE = "通道处理";
    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_HEIGHT = 260;
    private static final int GRID_GAP = 5;
    private static final int LABEL_COLUMN = 0;
    private static final int FIELD_COLUMN = 1;
    private static final String LABEL_CHANNEL = "通道:";
    private static final String LABEL_OPERATION = "操作:";
    private static final String BUTTON_OK = "确定";
    private static final String BUTTON_CANCEL = "取消";
    private static final int BTN_PANE_GAP = 5;

    private final JComboBox<ChannelData> channelCombo;
    private final JComboBox<String> opCombo;
    private final ParamPanel paramPanel;
    private final MarkTypePanel markPanel;
    private final DataModel model;

    /**
     * 构造函数：初始化对话框组件和事件
     */
    public ProcessingDialog(Frame owner, DataModel model) {
        super(owner, DIALOG_TITLE, true);
        this.model = model;

        initDialog();
        channelCombo = createChannelCombo();
        opCombo = createOperationCombo();
        paramPanel = new ParamPanel();
        markPanel = new MarkTypePanel();

        layoutComponents();
        addEventListeners();
    }

    /**
     * 初始化对话框基本属性
     */
    private void initDialog() {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setLayout(new GridBagLayout());
        setLocationRelativeTo(getOwner());
    }

    /**
     * 创建通道下拉框
     */
    private JComboBox<ChannelData> createChannelCombo() {
        JComboBox<ChannelData> combo = new JComboBox<>(
                model.getChannels().toArray(new ChannelData[0])
        );
        return combo;
    }

    /**
     * 创建操作下拉框
     */
    private JComboBox<String> createOperationCombo() {
        JComboBox<String> combo = new JComboBox<>(
                OperationFactory.getOperationNames()
        );
        return combo;
    }

    /**
     * 布局所有组件
     */
    private void layoutComponents() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP);
        c.fill = GridBagConstraints.HORIZONTAL;

        // 第1行：通道选择
        c.gridx = LABEL_COLUMN; c.gridy = 0;
        add(new JLabel(LABEL_CHANNEL), c);
        c.gridx = FIELD_COLUMN;
        add(channelCombo, c);

        // 第2行：操作选择
        c.gridx = LABEL_COLUMN; c.gridy = 1;
        add(new JLabel(LABEL_OPERATION), c);
        c.gridx = FIELD_COLUMN;
        add(opCombo, c);

        // 第3行：参数面板
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        add(paramPanel, c);

        // 第4行：标记类型面板
        c.gridy = 3;
        add(markPanel, c);

        // 第5行：按钮面板
        c.gridy = 4;
        add(createButtonPane(), c);

        // 初始时根据操作刷新UI
        updateUIForOperation((String) opCombo.getSelectedItem());
    }

    /**
     * 创建确定/取消按钮面板
     */
    private JPanel createButtonPane() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, BTN_PANE_GAP, 0));
        JButton okBtn = new JButton(BUTTON_OK);
        JButton cancelBtn = new JButton(BUTTON_CANCEL);
        okBtn.addActionListener(this::onProcess);
        cancelBtn.addActionListener(e -> dispose());
        panel.add(okBtn);
        panel.add(cancelBtn);
        return panel;
    }

    /**
     * 添加操作选择监听，切换时更新参数区和标记类型区
     */
    private void addEventListeners() {
        opCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateUIForOperation((String) e.getItem());
            }
        });
    }

    /**
     * 根据操作名称更新参数面板和标记类型面板可见性
     */
    private void updateUIForOperation(String opName) {
        ProcessingOperation op = OperationFactory.create(opName);
        paramPanel.updateFor(op);
        markPanel.updateFor(op);
    }

    /**
     * 处理按钮事件：执行选中操作并刷新图表
     */
    private void onProcess(ActionEvent e) {
        ChannelData source = (ChannelData) channelCombo.getSelectedItem();
        String opName = (String) opCombo.getSelectedItem();
        ProcessingOperation op = OperationFactory.create(opName);

        double paramValue = 0;
        int windowSize = 0;
        boolean ge = markPanel.isGreaterOrEqual();
        try {
            if (op.needsParam()) {
                paramValue = paramPanel.getParam();
            }
            if (op.needsWindowSize()) {
                windowSize = paramPanel.getWindowSize();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "请输入合法的数字参数。", "参数错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ChannelData result = op.process(source, paramValue, windowSize, ge);
        model.getChannels().add(result);
        ((ChartFrame) getOwner()).loadData(model);
        dispose();
    }
}