package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class IntegrateOperation implements ProcessingOperation {
    @Override
    public String getName() {
        return "积分";
    }

    @Override
    public boolean needsParam() {
        return false;
    }

    @Override
    public boolean needsWindowSize() {
        return false;
    }

    @Override
    public boolean needsMarkType() {
        return false;
    }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        // 如果 src.getSampleRate() 本身就是 float，就直接接收：
        float sr = src.getSampleRate();
        // 如果它返回 double，则改为： float sr = (float) src.getSampleRate();

        double[] d   = src.getData();
        double[] out = new double[d.length];

        // 积分运算：累加 data[i] / sr
        out[0] = d[0] / sr;
        for (int i = 1; i < d.length; i++) {
            out[i] = out[i - 1] + d[i] / sr;
        }

        // 传入的第三个参数已是 float，就不会再有 lossy conversion
        return new ChannelData(src.getName() + "_积分", out, sr);
    }
}
