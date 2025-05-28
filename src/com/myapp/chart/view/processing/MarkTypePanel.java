package com.myapp.chart.view.processing;

import com.myapp.chart.view.processing.operation.ProcessingOperation;

import javax.swing.*;
import java.awt.*;

public class MarkTypePanel extends JPanel {
    private final JRadioButton geRb;
    private final JRadioButton leRb;

    public MarkTypePanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        geRb = new JRadioButton("大于等于");
        leRb = new JRadioButton("小于等于");
        ButtonGroup group = new ButtonGroup();
        group.add(geRb);
        group.add(leRb);
        geRb.setSelected(true);
        add(geRb);
        add(leRb);
    }

    /** 根据当前操作决定是否显示这一整块 */
    public void updateFor(ProcessingOperation op) {
        setVisible(op.needsMarkType());
    }

    /** 当前选的是“>=”还是“<=” */
    public boolean isGreaterOrEqual() {
        return geRb.isSelected();
    }
}
