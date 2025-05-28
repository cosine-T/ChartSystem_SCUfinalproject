package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class AmplifyOperation implements ProcessingOperation {
    @Override public String getName() { return "增幅"; }
    @Override public boolean needsParam()      { return true; }
    @Override public boolean needsWindowSize() { return false; }
    @Override public boolean needsMarkType()   { return false; }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        double[] d = src.getData();
        double[] out = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            out[i] = d[i] * param;
        }
        return new ChannelData(src.getName() + "_增幅", out, src.getSampleRate());
    }
}
