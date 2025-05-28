package com.myapp.chart.file;

import com.myapp.chart.model.ChannelData;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 通道导出工具：支持导出为文本 (*.txt) 或二进制 (*.bin)。
 */
public class FileWriter {

    // —— 文本导出配置 ——
    private static final String TXT_ENCODING          = "UTF-8";
    private static final String TXT_SUCCESS_PREFIX    = "文本导出成功：";
    private static final String TXT_ERROR_PREFIX      = "文本导出失败：";

    // —— 二进制导出配置 ——
    private static final int    FLOAT_BYTE_SIZE       = Float.BYTES;                  // 4 字节
    private static final String BIN_SUCCESS_PREFIX    = "二进制导出成功：";
    private static final String BIN_ERROR_PREFIX      = "二进制导出失败：";

    // —— 通用对话框配置 ——
    private static final String DIALOG_TITLE_ERROR    = "错误";
    private static final int    MESSAGE_TYPE_INFO     = JOptionPane.INFORMATION_MESSAGE;
    private static final int    MESSAGE_TYPE_ERROR    = JOptionPane.ERROR_MESSAGE;

    /**
     * 导出为文本格式：逐行写入每个样本的浮点值（UTF-8 编码）。
     *
     * @param ch   通道数据
     * @param file 目标文件（.txt）
     */
    public static void exportTxt(ChannelData ch, File file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), TXT_ENCODING))) {
            for (double v : ch.getData()) {
                writer.write(Double.toString(v));
                writer.newLine();
            }
            JOptionPane.showMessageDialog(
                    null,
                    TXT_SUCCESS_PREFIX + file.getAbsolutePath(),
                    null,
                    MESSAGE_TYPE_INFO);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    TXT_ERROR_PREFIX + ex.getMessage(),
                    DIALOG_TITLE_ERROR,
                    MESSAGE_TYPE_ERROR);
        }
    }

    /**
     * 导出为二进制格式：按 float32 小端依次写入各样本（.bin）。
     *
     * @param ch   通道数据
     * @param file 目标文件（.bin）
     */
    public static void exportBin(ChannelData ch, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buffer = ByteBuffer.allocate(FLOAT_BYTE_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN);
            for (double v : ch.getData()) {
                buffer.putFloat(0, (float) v);
                fos.write(buffer.array());
            }
            JOptionPane.showMessageDialog(
                    null,
                    BIN_SUCCESS_PREFIX + file.getAbsolutePath(),
                    null,
                    MESSAGE_TYPE_INFO);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    BIN_ERROR_PREFIX + ex.getMessage(),
                    DIALOG_TITLE_ERROR,
                    MESSAGE_TYPE_ERROR);
        }
    }
}
