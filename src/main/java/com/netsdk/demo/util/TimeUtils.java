package com.netsdk.demo.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @description:
 * @author: 251589
 * @time: 2020/11/17 16:34
 */
public class TimeUtils {

    public static String getTimeStr(long timestamp) {
        String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timestamp));
    }

    private TimeUtils() {
    }
}
