package com.netsdk.demo.example.composeDemo;

import com.netsdk.lib.NetSDKLib;

import java.text.SimpleDateFormat;

import static com.netsdk.demo.example.composeDemo.ComposeLogon.netsdk;

public class ComposeTools {
    // 获取当前时间
    public static String getDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.format(new java.util.Date()).replace(" ", "_").replace(":", "-");

        return date;
    }

    // 查询分割能力(pstuCaps内存由用户申请释放)
    public static NetSDKLib.NET_SPLIT_CAPS GetSplitCaps(NetSDKLib.LLong hLoginHandle, int nChannel) {

        NetSDKLib.NET_SPLIT_CAPS pstuCaps = new NetSDKLib.NET_SPLIT_CAPS();
        boolean result = netsdk.CLIENT_GetSplitCaps(hLoginHandle, nChannel, pstuCaps, 3000);
        if (!result) {
            System.err.printf("Set %s Config Failed! Last Error = %s\n", "Get Split Caps", netsdk.CLIENT_GetLastError());
            return null;
        }
        return pstuCaps;
    }
}
