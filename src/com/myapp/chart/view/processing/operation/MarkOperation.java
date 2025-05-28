package com.myapp.chart.view.processing.operation;

import com.myapp.chart.model.ChannelData;

public class MarkOperation implements ProcessingOperation {
    @Override public String getName() { return "标记"; }
    @Override public boolean needsParam()      { return true; }
    @Override public boolean needsWindowSize() { return false; }
    @Override public boolean needsMarkType()   { return true; }

    @Override
    public ChannelData process(ChannelData src, double param, int windowSize, boolean greaterOrEqual) {
        double[] d = src.getData();
        ChannelData marked = new ChannelData(src.getName() + "_标记", d.clone(), src.getSampleRate());
        boolean inSeg = false;
        int segStart = 0;
        for (int i = 0; i < d.length; i++) {
            boolean cond = greaterOrEqual ? (d[i] >= param) : (d[i] <= param);
            if (!inSeg && cond) {
                inSeg = true;
                segStart = i;
            } else if (inSeg && !cond) {
                inSeg = false;
                marked.addHighlightRange(segStart, i - 1);
            }
        }
        if (inSeg) {
            marked.addHighlightRange(segStart, d.length - 1);
        }
        return marked;
    }
}
