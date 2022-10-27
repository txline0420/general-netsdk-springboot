package com.netsdk.demo.example.parkingDemo.callback;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 重连回调函数
 */
public class DefaultHaveReconnectCallBack implements NetSDKLib.fHaveReConnect {

    private static DefaultHaveReconnectCallBack singleInstance;

    public static DefaultHaveReconnectCallBack getInstance() {
        if (singleInstance == null) {
            singleInstance = new DefaultHaveReconnectCallBack();
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
