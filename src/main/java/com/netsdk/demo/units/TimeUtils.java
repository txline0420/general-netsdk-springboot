package com.netsdk.demo.units;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @description:
 * @author: 251589
 * @time: 2020/11/17 16:34
 */
public class TimeUtils {

    // 获取 年-月-日 时:分:秒格式的时间字符串 入参 long
    public static String getTimeStr(long timestamp) {
        String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timestamp));
    }

    // 获取 年-月-日 时:分:秒格式的时间字符串 入参 Date
    public static String getTimeStr(Date date) {
        String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }


    // 获取 年月日时分秒格式的时间字符串 入参 long
    public static String getTimeStringWithoutSign(long timestamp) {
        String format = "yyyMMddHHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timestamp));
    }

    // 获取 年月日时分秒格式的时间字符串 入参 Date
    public static String getTimeStringWithoutSign(Date date) {
        String format = "yyyMMddHHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    private TimeUtils() {
    }


}
