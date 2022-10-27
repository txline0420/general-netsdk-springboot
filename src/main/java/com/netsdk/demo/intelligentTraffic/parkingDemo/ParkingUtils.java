package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/20 16:24
 */
public class ParkingUtils {

    ////////////////// SDK 初始化/清理资源 /////////////////////
    //////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数需要是静态的，防止被系统回收
    private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调

    /**
     * Init boolean. sdk 初始化
     *
     * @return the boolean
     */
    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        ParkingUtils.enableLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000;                        //登录请求响应超时时间设置为3S
        int tryTimes = 1;                           //登录时尝试建立链接 1 次
        netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);
        // 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间
        netsdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 打开 sdk log
     */
    private static void enableLog() {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("sdklog/");
        if (!path.exists()) path.mkdir();

        // 这里的log保存地址依据实际情况自己调整
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Failed to open NetSDK log");
    }

    /**
     * Cleanup. 清除 sdk 环境
     */
    public static void cleanup() {
        if (bLogOpen) {
            netsdk.CLIENT_LogClose();
        }
        if (bInit) {
            netsdk.CLIENT_Cleanup();
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netsdk.CLIENT_Cleanup();
        System.exit(0);
    }

    //////////////////////////////////////////  RPC接口 /////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 停车超时配置获取
    public static boolean ParkingTimeoutGetConfig(NetSDKLib.LLong lLoginHandle, NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT stuInfo) {
        if (0 == lLoginHandle.longValue()) {
            System.out.println("Invalid login handle");
            return false;
        }
        int emType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_PARKING_TIMEOUT_DETECT;
        int channel = -1;        // 只支持-1，不要写具体的通道
        stuInfo.write();
        boolean bRet = netsdk.CLIENT_GetConfig(lLoginHandle, emType, channel, stuInfo.getPointer(), stuInfo.size(), 3000, null);
        if (!bRet) {
            System.out.printf("CLIENT_GetConfig of NET_EM_CFG_PARKING_TIMEOUT_DETECT error，错误码:%s\n", ToolKits.getErrorCode());
            return false;
        }
        stuInfo.read(); // 从指针获取数据
        System.out.println("CLIENT_GetConfig of NET_EM_CFG_PARKING_TIMEOUT_DETECT ok");
        return true;
    }

    // 停车超时配置设置
    public static boolean ParkingTimeoutSetConfig(NetSDKLib.LLong lLoginHandle, NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT stuInfo) {
        if (0 == lLoginHandle.longValue()) {
            System.out.println("Invalid login handle");
            return false;
        }
        int channel = -1;             // 只支持-1，不要写具体的通道
        int emType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_PARKING_TIMEOUT_DETECT;
        stuInfo.write();
        boolean bRet = netsdk.CLIENT_SetConfig(lLoginHandle, emType, channel, stuInfo.getPointer(), stuInfo.size(), 3000, new IntByReference(0), null);
        if (!bRet) {
            System.out.printf("CLIENT_SetConfig of NET_EM_CFG_PARKING_TIMEOUT_DETECT error，错误码:%s\n", ToolKits.getErrorCode());
            return false;
        }
        System.out.println("CLIENT_SetConfig of NET_EM_CFG_PARKING_TIMEOUT_DETECT ok");
        return true;
    }

    // 手动抓图
    public static boolean ParkingSnapshot(NetSDKLib.LLong lLoginHandle, NetSDKLib.NET_IN_SNAP_MNG_SHOT stuInBuf) {
        if (0 == lLoginHandle.longValue()) {
            System.out.println("Invalid login handle");
            return false;
        }

        System.out.println("CLIENT_ControlDeviceEx start");

        // 出参
        NetSDKLib.NET_OUT_SNAP_MNG_SHOT stuOutBuf = new NetSDKLib.NET_OUT_SNAP_MNG_SHOT();
        int emType = NetSDKLib.CtrlType.CTRLTYPE_CTRL_SNAP_MNG_SNAP_SHOT;

        stuInBuf.write();
        stuOutBuf.write();
        boolean bRet = netsdk.CLIENT_ControlDeviceEx(lLoginHandle, emType, stuInBuf.getPointer(), stuOutBuf.getPointer(), 5000);
        if (!bRet) {
            System.err.printf("Failed to snap shot, last error: %s\n", ToolKits.getErrorCode());
            return false;
        }
        System.out.println("Snap shot succeed.");
        return true;
    }

    public static boolean RemoveParkingCarInfo(NetSDKLib.LLong lLoginHandle, NetSDKLib.NET_IN_REMOVE_PARKING_CAR_INFO stuInBuf) {
        if (0 == lLoginHandle.longValue()) {
            System.out.println("Invalid login handle");
            return false;
        }

        System.out.println("CLIENT_RemoveParkingCarInfo start");

        NetSDKLib.NET_OUT_REMOVE_PARKING_CAR_INFO stuOutBuf = new NetSDKLib.NET_OUT_REMOVE_PARKING_CAR_INFO();

        stuInBuf.write();
        stuOutBuf.write();
        boolean bRet = netsdk.CLIENT_RemoveParkingCarInfo(lLoginHandle, stuInBuf.getPointer(), stuOutBuf.getPointer(), 3000);
        if (!bRet) {
            System.err.printf("CLIENT_RemoveParkingCarInfo error:%s\n", ToolKits.getErrorCode());
            return false;
        }
        System.out.println("CLIENT_RemoveParkingCarInfo succeed.");
        return true;
    }
}
