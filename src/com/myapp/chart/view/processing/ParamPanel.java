package com.myapp.chart.view.processing;

import com.myapp.chart.view.processing.operation.ProcessingOperation;

import javax.swing.*;
import java.awt.*;

public class ParamPanel extends JPanel {
    private final JLabel label;
    private final JTextField field;

    public ParamPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        label = new JLabel("参数：");
        field = new JTextField(10);
        add(label);
        add(field);
    }

    /** 根据当前操作调整 label 文案及可见性 */
    public void updateFor(ProcessingOperation op) {
        if (op.needsWindowSize()) {
            label.setText("窗口大小：");
            field.setText("3");
            setVisible(true);
        } else if (op.needsParam()) {
            label.setText("参数：");
            field.setText("1.0");
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    /** 供需要“参数”的操作调用 */
    public double getParam() throws NumberFormatException {
        return Double.parseDouble(field.getText().trim());
    }

    /** 供需要“窗口大小”的操作调用 */
    public int getWindowSize() throws NumberFormatException {
        return (int) Math.round(getParam());
    }
}
