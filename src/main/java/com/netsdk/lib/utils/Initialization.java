package com.netsdk.lib.utils;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/7/19 15:58
 */
public class Initialization {

    public  static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    public  static NetSDKLib config =  NetSDKLib.CONFIG_INSTANCE;

    // 登陆句柄
    public static NetSDKLib.LLong loginHandle;

  public static   NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

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
    public static void InitTest(String ip,int port,String user,String password ){
        // 初始化SDK库
        netSdk.CLIENT_Init(DisconnectCallback.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

        //打开日志，可选0
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        System.out.println("logPath:"+logPath);
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }

        loginWithHighLevel(ip,port,user,password);
    }

    /**
     * 高安全登录
     */
    public static void loginWithHighLevel(String ip,int port,String user,String password) {
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
        loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
          //  System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port, netSdk.CLIENT_GetLastError());
            throw new RuntimeException("Login Device[%s] Port[%d]Failed. "+ ip+":"+ port+":"+ netSdk.CLIENT_GetLastError());

        } else {
            NetSDKLib.NET_DEVICEINFO_Ex   deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + ip);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
            m_stDeviceInfo= pstOutParam.stuDeviceInfo;
        }

    }




    /**
     * 退出清理环境
     */
    public static void LoginOut(){
        System.out.println("End Test");
        if( loginHandle.longValue() != 0)
        {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }
    // 编码格式
    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }

    }


    public static  void  printlns(String context){
        System.out.println(context);
    }
}
