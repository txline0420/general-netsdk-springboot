package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Scanner;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/8/16 10:57
 */
public class LoginMore {
    public   NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    public  void InitTest(){
        // 初始化SDK库
        netSdk.CLIENT_Init(DisconnectCallback.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

        //打开日志，可选0
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }

    }
    /**
     * 设备断线回调
     */
    private static class DisconnectCallback implements NetSDKLib.fDisConnect {
        private static DisconnectCallback instance = new DisconnectCallback();

        private DisconnectCallback() {
        }

        public static DisconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }
    /**
     * 设备重连回调
     */
    private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
        private static HaveReconnectCallback instance = new HaveReconnectCallback();

        private HaveReconnectCallback() {
        }

        public static HaveReconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
        }
    }

    /**
     * 高安全登录
     */
    public   NetSDKLib.LLong loginWithHighLevel(String ip,int port,String user,String password) {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = ip.getBytes();
                nPort = port;
                szUserName = user.getBytes();
                szPassword = password.getBytes();
            }
        };

        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
     NetSDKLib.LLong loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            //  System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port, netSdk.CLIENT_GetLastError());
            System.out.println("Login Device[%s] Port[%d]Failed. "+ ip+":"+ port+":"+ netSdk.CLIENT_GetLastError());

        } else {
            NetSDKLib.NET_DEVICEINFO_Ex   deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + ip);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }

        return loginHandle;
    }

    /**
     * 退出清理环境
     */
    public  void LoginOut(NetSDKLib.LLong lLong){
        System.out.println("End Test");
        if( lLong.longValue() != 0)
        {
            netSdk.CLIENT_Logout(lLong);
        }
        System.out.println("See You...");

    }

    public static void main(String[] args) {
        LoginMore loginMore=new LoginMore();
        loginMore.InitTest();
        NetSDKLib.LLong lLong = loginMore.loginWithHighLevel("172.23.12.114",37777,"admin123","admin");
        NetSDKLib.LLong lLong1 = loginMore.loginWithHighLevel("10.34.3.219",37777,"admin123","admin");
        NetSDKLib.LLong lLong2 = loginMore.loginWithHighLevel("10.34.3.219",37701,"a","admin");

        Scanner scanner=new Scanner(System.in);
        String next = scanner.next();
        loginMore.LoginOut(lLong);
        loginMore.LoginOut(lLong1);
        loginMore.LoginOut(lLong2);
        loginMore.netSdk.CLIENT_Cleanup();
    }
}
