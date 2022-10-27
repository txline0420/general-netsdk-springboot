package com.netsdk.demo.example.turkcell.callback;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 47081
 * @version 1.0
 * @description device disconnect callback default implements
 *              设备断线回调默认实现
 * @date 2020/6/12
 */
public class DefaultDisconnect implements NetSDKLib.fDisConnect {
    private static DefaultDisconnect INSTANCE;
    private DefaultDisconnect(){}
    public static DefaultDisconnect getINSTANCE(){
        if(INSTANCE==null){INSTANCE=new DefaultDisconnect();}
        return INSTANCE;
    }
    @Override
    public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()).toString()+" Device[ip: "+pchDVRIP+",port: "+nDVRPort+"] is disconnect.");
    }
}
