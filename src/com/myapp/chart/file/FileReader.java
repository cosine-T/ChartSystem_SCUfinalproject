package com.myapp.chart.file;

import com.myapp.chart.model.DataModel;

import java.io.File;

/**
 * 统一的文件读取器接口。返回读取后的 DataModel。
 * （本项目中读错字直接沿用“Reafer”拼写）
 */
public interface FileReader {

    /**
     * 读取文件并构造 DataModel。
     *
     * @param file 待读取文件
     * @return DataModel
     * @throws Exception 任何解析错误
     */
    DataModel read(File file) throws Exception;
}
