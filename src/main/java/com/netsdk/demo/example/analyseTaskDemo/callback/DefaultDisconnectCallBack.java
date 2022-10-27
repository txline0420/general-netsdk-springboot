package com.netsdk.demo.example.analyseTaskDemo.callback;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 断线回调函数
 */
public class DefaultDisconnectCallBack implements NetSDKLib.fDisConnect {

    private static DefaultDisconnectCallBack singleInstance;

    public static DefaultDisconnectCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new DefaultDisconnectCallBack();
        }
        return singleInstance;
    }

    @Override
    public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.println(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +
                        " Device[ip: " + pchDVRIP + ",port: " + nDVRPort + "] is disconnect.");
    }
}
