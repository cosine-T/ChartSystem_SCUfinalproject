package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class MovingAvgOperation implements ProcessingOperation {
    @Override public String getName() { return "滑动平均滤波"; }
    @Override public boolean needsParam()      { return false; }
    @Override public boolean needsWindowSize() { return true; }
    @Override public boolean needsMarkType()   { return false; }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        double[] d = src.getData();
        double[] out = new double[d.length];
        int half = windowSize / 2;
        for (int i = 0; i < d.length; i++) {
            int start = Math.max(0, i - half);
            int end   = Math.min(d.length - 1, i + half);
            double sum = 0;
            for (int j = start; j <= end; j++) sum += d[j];
            out[i] = sum / (end - start + 1);
        }
        return new ChannelData(src.getName() + "_滑动平均滤波", out, src.getSampleRate());
    }
}
