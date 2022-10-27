package com.netsdk.demo.customize.analyseTaskDemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/17 19:05
 */
public class AnalyseTaskUtils {
    // 获取当前时间
    public static String getDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.
                format(new java.util.Date()).
                replace(" ", "_").
                replace(":", "-");
        return date;
    }

    // 读取文件
    public static byte[] readFile2Buffer(String filePath) {
        File file = new File(filePath);
        long fileSize = file.length();

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead;
            while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                throw new IOException("读取文件有错误 " + file.getName());
            }
            inputStream.close();
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
