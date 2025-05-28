package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class DiffOperation implements ProcessingOperation {
    @Override public String getName() { return "差分"; }
    @Override public boolean needsParam()      { return false; }
    @Override public boolean needsWindowSize() { return false; }
    @Override public boolean needsMarkType()   { return false; }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        double[] data = src.getData();
        double[] out  = new double[data.length];
        out[0] = 0;
        for (int i = 1; i < data.length; i++) {
            out[i] = data[i] - data[i - 1];
        }
        return new ChannelData(src.getName() + "_差分", out, src.getSampleRate());
    }
}
