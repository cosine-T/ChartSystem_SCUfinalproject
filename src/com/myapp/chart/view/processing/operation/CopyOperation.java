package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class CopyOperation implements ProcessingOperation {
    @Override public String getName() { return "复制"; }
    @Override public boolean needsParam()      { return false; }
    @Override public boolean needsWindowSize() { return false; }
    @Override public boolean needsMarkType()   { return false; }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        double[] out = src.getData().clone();
        return new ChannelData(src.getName() + "_复制", out, src.getSampleRate());
    }
}
