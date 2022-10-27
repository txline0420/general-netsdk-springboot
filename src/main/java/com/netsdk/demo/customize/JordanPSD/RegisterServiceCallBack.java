package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.Utils;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RegisterServiceCallBack implements NetSDKLib.fServiceCallBack {

    private static volatile RegisterServiceCallBack instance;

    // 跨平台编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode());

    // 用于存法回调信息的缓存队列
    public static BlockingQueue<ListenInfo> ServerInfoQueue = new LinkedBlockingQueue<>(1000);

    public static RegisterServiceCallBack getInstance() {
        if (instance != null) return instance;
        synchronized (RegisterServiceCallBack.class) {
            if (instance == null) instance = new RegisterServiceCallBack();
        }
        return instance;
    }

    @Override
    public int invoke(NetSDKLib.LLong lHandle, String pIp, int wPort, int lCommand, Pointer pParam, int dwParamLen, Pointer dwUserData) {

        // System.out.println("receive info from device: [ip: " + pIp + ",port: " + wPort + "].");
        switch (lCommand) {
            case NetSDKLib.EM_LISTEN_TYPE.NET_DVR_DISCONNECT: {
                // System.out.println("验证期间设备断线回调");
                break;
            }
            case NetSDKLib.EM_LISTEN_TYPE.NET_DVR_SERIAL_RETURN: {
                // 以序列号上报
                String devSerial = new String(pParam.getByteArray(0, dwParamLen), encode).trim();
                ServerInfoQueue.offer(new ListenInfo(devSerial, pIp, wPort));
                break;
            }
            case NetSDKLib.EM_LISTEN_TYPE.NET_DEV_AUTOREGISTER_RETURN: {
                // 一般用不到
                System.out.println("设备注册携带序列号和令牌");
                // todo
                break;
            }
            case NetSDKLib.EM_LISTEN_TYPE.NET_DEV_NOTIFY_IP_RETURN: {
                // 一般用不到
                System.out.println("设备仅上报IP, 不作为主动注册用");
                // todo
                break;
            }
            default: {
                System.out.println("收到未知类型数据");
            }
        }
        return 0;
    }
}
