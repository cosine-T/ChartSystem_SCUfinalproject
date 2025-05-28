package com.myapp.chart.view.channel;

/** 坐标与刻度的通用计算工具 */
public final class AxisUtil {

    private AxisUtil() {}   // 工具类不允许实例化

    /** 数据值 → 像素 Y 坐标（坐标原点在左上） */
    public static int mapY(double v, double base, double range, int height) {
        return (int) (height - (v - base) / range * height);
    }

    /** 刻度步进，例如 1·10ⁿ, 2·10ⁿ, 5·10ⁿ, 10·10ⁿ */
    public static double niceStep(double range, int ticks) {
        double raw  = range / ticks;
        double mag  = Math.pow(10, Math.floor(Math.log10(raw)));
        double norm = raw / mag;
        if (norm <= 1) return 1 * mag;
        if (norm <= 2) return 2 * mag;
        if (norm <= 5) return 5 * mag;
        return 10 * mag;
    }
}
