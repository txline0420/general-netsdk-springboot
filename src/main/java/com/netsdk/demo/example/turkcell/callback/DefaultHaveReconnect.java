package com.netsdk.demo.example.turkcell.callback;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 47081
 * @version 1.0
 * @description sdk reconnect device callback default implements
 *              设备断线重连的回调函数默认实现
 * @date 2020/6/12
 */
public class DefaultHaveReconnect implements NetSDKLib.fHaveReConnect {
    private static DefaultHaveReconnect INSTANCE;
    private DefaultHaveReconnect(){}
    public static DefaultHaveReconnect getINSTANCE(){
        if(INSTANCE==null){INSTANCE=new DefaultHaveReconnect();}
        return INSTANCE;
    }
    @Override
    public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" Device[ ip: "+pchDVRIP+",port: "+nDVRPort+"] reconnect.");
    }
}
