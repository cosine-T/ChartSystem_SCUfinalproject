package com.myapp.chart.file;

import com.myapp.chart.model.ChannelData;
import com.myapp.chart.model.DataModel;

import javax.swing.*;
import java.awt.Component;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 二进制文件读取器：按 4 字节小端 float 解码，多通道、可选窗口读取，自动抽样降频。
 */
public class BinaryReader implements FileReader {

    // 常量配置
    private static final String DIALOG_TITLE         = "打开二进制文件";
    private static final String INPUT_CHANNEL_PROMPT = "请输入通道数量：";
    private static final String INPUT_RATE_PROMPT    = "请输入采样率（Hz）：";
    private static final String ERROR_CANCEL_OPEN    = "取消打开";
    private static final String CHANNEL_NAME_PREFIX  = "CH";
    private static final int    BYTES_PER_SAMPLE     = 4;          // 每个样本占用字节数
    private static final int    DEFAULT_BUFFER_SIZE  = 40 * 1024;  // 40 KiB，4 的倍数
    private static final int    MAX_SAMPLES_PER_CH   = 1_000_000;  // 每通道最大样本数（用于计算抽样系数）

    @Override
    public DataModel read(File file) throws Exception {
        return read(file, 0, -1);
    }

    /**
     * 窗口化读取二进制数据
     *
     * @param file           二进制文件
     * @param startSample    起始样本索引（inclusive）
     * @param windowSamples  希望读取的样本数量，-1 表示读取全部
     */
    public DataModel read(File file, long startSample, long windowSamples) throws Exception {
        // 1. 用户输入通道数与采样率
        int channelCount = promptChannelCount();
        float sampleRate = promptSampleRate();

        // 2. 计算实际要读取的样本范围
        long totalSamples = file.length() / (BYTES_PER_SAMPLE * channelCount);
        long start        = clamp(startSample, 0, totalSamples);
        long length       = windowSamples < 0
                ? totalSamples - start
                : clamp(windowSamples, 0, totalSamples - start);

        // 3. 计算抽样因子 & 输出长度
        int decimate = calculateDecimationFactor(length);
        int outLen   = (int) ((length + decimate - 1) / decimate);

        // 4. 初始化每通道数据容器
        List<ChannelData> channels = initializeChannels(channelCount, outLen, sampleRate, decimate);

        // 5. 逐帧读取并按需要抽样
        readAndDecimate(file, start, length, channelCount, decimate, channels);

        return new DataModel(file.getName(), channels);
    }

    /** 弹出对话框，获取通道数 */
    private int promptChannelCount() {
        String input = JOptionPane.showInputDialog(
                (Component) null,
                INPUT_CHANNEL_PROMPT,
                DIALOG_TITLE,
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) throw new IllegalStateException(ERROR_CANCEL_OPEN);
        return Integer.parseInt(input.trim());
    }

    /** 弹出对话框，获取采样率 */
    private float promptSampleRate() {
        String input = JOptionPane.showInputDialog(
                (Component) null,
                INPUT_RATE_PROMPT,
                DIALOG_TITLE,
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) throw new IllegalStateException(ERROR_CANCEL_OPEN);
        return Float.parseFloat(input.trim());
    }

    /** 计算抽样降频因子，确保每通道样本数不超过上限 */
    private int calculateDecimationFactor(long length) {
        int factor = (int) Math.ceil(length / (double) MAX_SAMPLES_PER_CH);
        return Math.max(factor, 1);
    }

    /** 初始化各通道的 ChannelData 实例 */
    private List<ChannelData> initializeChannels(int count, int outLen, float rate, int decimate) {
        List<ChannelData> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double[] data = new double[outLen];
            // 初始填 NaN
            for (int j = 0; j < outLen; j++) data[j] = Double.NaN;
            String name = CHANNEL_NAME_PREFIX + (i + 1);
            list.add(new ChannelData(name, data, rate / decimate));
        }
        return list;
    }

    /**
     * 从文件按帧读取，并在解码后按 decimate 进行抽样降频。
     */
    private void readAndDecimate(File file,
                                 long start,
                                 long length,
                                 int channelCount,
                                 int decimate,
                                 List<ChannelData> channels) throws Exception {

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteBuffer bb = ByteBuffer.allocate(BYTES_PER_SAMPLE)
                .order(ByteOrder.LITTLE_ENDIAN);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // 定位到起始字节
            long byteOffset = start * channelCount * BYTES_PER_SAMPLE;
            raf.seek(byteOffset);

            long framesLeft  = length;
            long globalFrame = 0;  // 相对于读取窗口的帧计数

            int framesPerBuf = DEFAULT_BUFFER_SIZE / (BYTES_PER_SAMPLE * channelCount);

            while (framesLeft > 0) {
                int toReadFrames = (int) Math.min(framesPerBuf, framesLeft);
                int toReadBytes  = toReadFrames * channelCount * BYTES_PER_SAMPLE;
                int got = raf.read(buffer, 0, toReadBytes);
                if (got <= 0) break;

                // 按样本解码并分通道
                for (int off = 0; off < got; off += BYTES_PER_SAMPLE) {
                    int chIndex = (off / BYTES_PER_SAMPLE) % channelCount;
                    if (globalFrame % decimate == 0) {
                        int idx = (int) (globalFrame / decimate);
                        if (idx < channels.get(chIndex).getData().length) {
                            bb.clear();
                            bb.put(buffer, off, BYTES_PER_SAMPLE).flip();
                            channels.get(chIndex).getData()[idx] = bb.getFloat();
                        }
                    }
                    // 当最后一个通道读取完，才增加全局帧计数
                    if (chIndex == channelCount - 1) {
                        globalFrame++;
                    }
                }
                framesLeft -= toReadFrames;
            }
        }
    }

    /** 限幅工具 */
    private long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(v, max));
    }
}