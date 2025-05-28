package com.myapp.chart.view.processing.operation;

import java.util.LinkedHashMap;
import java.util.Map;

public class OperationFactory {
    private static final Map<String, ProcessingOperation> ops = new LinkedHashMap<>();

    static {
        register(new CopyOperation());
        register(new DiffOperation());
        register(new IntegrateOperation());
        register(new AmplifyOperation());
        register(new MovingAvgOperation());
        register(new MarkOperation());
    }

    private static void register(ProcessingOperation op) {
        ops.put(op.getName(), op);
    }

    /** 给 JComboBox 用：所有操作名称（保持注册顺序） */
    public static String[] getOperationNames() {
        return ops.keySet().toArray(new String[0]);
    }

    /** 根据名称返回对应运算实例 */
    public static ProcessingOperation create(String name) {
        return ops.get(name);
    }
}
