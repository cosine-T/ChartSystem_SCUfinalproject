package com.myapp.chart.file;

import com.biorecorder.edflib.EdfFileReader;
import com.biorecorder.edflib.HeaderConfig;
import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * EDF 文件读取器：按记录读取并支持指定窗口与抽样降频。
 */
public class EDFReader implements FileReader {

    // —— 常量配置 ——
    private static final int    CHUNK_SAMPLES          = 4096;       // 单次读取物理样本数
    private static final int    MAX_SAMPLES_PER_CH     = 1_000_000;  // 每通道最大样本数
    private static final String DEFAULT_CHANNEL_PREFIX = "CH";      // 默认通道名前缀
    private static final double DEFAULT_RECORD_DURATION = 1.0;      // 默认每记录时长（秒）

    @Override
    public DataModel read(File file) throws Exception {
        return read(file, 0, -1);
    }

    /**
     * 读取 EDF 文件，支持指定起始样本和窗口样本数。
     *
     * @param file           EDF 文件
     * @param startSample    起始样本索引（inclusive）
     * @param windowSamples  窗口样本数，<0 表示读取全部
     * @return 包含通道数据与 EDF HeaderConfig 的 DataModel
     */
    public DataModel read(File file, long startSample, long windowSamples) throws Exception {
        EdfFileReader reader = new EdfFileReader(file);
        try {
            HeaderConfig hdr       = reader.getHeader();
            double       recDur    = extractRecordDuration(hdr);
            int          nRecords  = reader.getNumberOfDataRecords();
            double       totalSec  = recDur * nRecords;
            int          channelCount = hdr.getNumberOfSignals();

            List<ChannelData> channels = new ArrayList<>(channelCount);
            // 逐通道读取数据
            for (int chIndex = 0; chIndex < channelCount; chIndex++) {
                channels.add(readChannel(reader, hdr, chIndex, startSample, windowSamples, totalSec));
            }
            return new DataModel(file.getName(), channels, hdr);
        } finally {
            reader.close();
        }
    }

    /**
     * 读取单个通道的数据，按块读取并按 decimate 抽样存入数组。
     */
    private ChannelData readChannel(EdfFileReader reader,
                                    HeaderConfig hdr,
                                    int chIndex,
                                    long startSample,
                                    long windowSamples,
                                    double totalSec) throws Exception {
        long totalSamples = reader.getNumberOfSamples(chIndex);

        // 1. 计算实际起点和长度
        long start  = clamp(startSample, 0, totalSamples);
        long length = (windowSamples < 0)
                ? totalSamples - start
                : clamp(windowSamples, 0, totalSamples - start);

        // 2. 计算抽样因子 & 输出长度
        int decimate = calculateDecimation(length);
        int outLen   = (int)((length + decimate - 1) / decimate);

        double[] data   = new double[outLen];
        double[] buffer = new double[CHUNK_SAMPLES];

        // 3. 按块读取物理样本，并仅取每 decimate-th 样本
        reader.setSamplePosition(chIndex, start);
        long readCount = 0, writeCount = 0;
        while (readCount < length) {
            int toRead = (int)Math.min(CHUNK_SAMPLES, length - readCount);
            int got    = reader.readPhysicalSamples(chIndex, buffer, 0, toRead);
            if (got <= 0) break;
            for (int i = 0; i < got; i++) {
                if ((readCount + i) % decimate == 0) {
                    data[(int)(writeCount++)] = buffer[i];
                }
            }
            readCount += got;
        }

        // 4. 通道名称和采样率
        String label = hdr.getLabel(chIndex).trim();
        if (label.isEmpty()) {
            label = DEFAULT_CHANNEL_PREFIX + (chIndex + 1);
        }
        float sampleRate = (float)(reader.getNumberOfSamples(chIndex) / totalSec / decimate);
        return new ChannelData(label, data, sampleRate);
    }

    /**
     * 从 HeaderConfig 中反射调用可用的方法来获取记录时长，找不到时返回默认值。
     */
    private double extractRecordDuration(HeaderConfig hdr) {
        for (String methodName : new String[]{
                "getDataRecordDuration",
                "getDurationOfDataRecord",
                "getDurationOfDataRecordSec"
        }) {
            try {
                Method m = HeaderConfig.class.getMethod(methodName);
                Object val = m.invoke(hdr);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        System.err.println("[EDFReader] 未找到记录时长方法，使用默认 " + DEFAULT_RECORD_DURATION + " 秒");
        return DEFAULT_RECORD_DURATION;
    }

    /** 计算抽样因子，确保每通道样本数不超过 MAX_SAMPLES_PER_CH */
    private int calculateDecimation(long length) {
        int factor = (int)Math.ceil(length / (double) MAX_SAMPLES_PER_CH);
        return Math.max(factor, 1);
    }

    /** 将数值限制在 [min, max] 范围内 */
    private long clamp(long v, long min, long max) {
        return v < min ? min : (v > max ? max : v);
    }
}