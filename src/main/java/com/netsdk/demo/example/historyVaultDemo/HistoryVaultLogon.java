package com.netsdk.demo.example.historyVaultDemo;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;

public class HistoryVaultLogon {

    // 配置登陆地址，端口，用户名，密码
    private static String m_strIpAddr = "10.172.161.89";
    private static int m_nPort = 37777;
    private static String m_strUser = "admin";
    private static String m_strPassword = "admin123";

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    // The constant loginHandle. 登录句柄
    public static LLong loginHandle = new LLong(0);

    // The constant high level loginHandle 高安全级别登陆句柄
    public static LLong m_hLoginHandle = new LLong(0);

    /**
     * The device extra info 设备信息扩展
     */
    public static NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();

    // the status of sdk init result
    private static boolean bInit = false;

    // the status of sdk log result
    private static boolean bLogOpen = false;


    /****************************登陆回调类*********************************************
     * ********************************************************************************
     * ********************************************************************************
     *
     * The type Disconnect callback. 设备断线回调
     */
    public static class DisConnectCallBack implements NetSDKLib.fDisConnect {

        private static DisConnectCallBack instance = new DisConnectCallBack();

        public static DisConnectCallBack getInstance() {
            return instance;
        }

        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
            // Todo
        }
    }

    /**
     * The type Have reconnect callback. 设备连接恢复
     */
    public static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {

        private static HaveReConnectCallBack instance = new HaveReConnectCallBack();

        public static HaveReConnectCallBack getInstance() {
            return instance;
        }

        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
            // Todo
        }
    }

    /******************************登陆方法*******************************************
     * ******************************************************************************
     * ******************************************************************************
     *
     * Init boolean. sdk 初始化
     *
     * @param disConnect    the disconnect instance 断线回调
     * @param haveReConnect the have reconnect instance 重连回调
     * @return the boolean
     */
    public static boolean init(NetSDKLib.fDisConnect disConnect,
                               NetSDKLib.fHaveReConnect haveReConnect) {
        bInit = netsdk.CLIENT_Init(disConnect, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        enableLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 5000;                        //登录请求响应超时时间设置为5S
        int tryTimes = 1;                           //登录时尝试建立链接1 次
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
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + getDate() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Failed to open NetSDK log");
    }

    // 获取当前时间
    public static String getDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.
                format(new java.util.Date()).
                replace(" ", "_").
                replace(":", "-");
        return date;
    }

    /**
     * login. TCP 登录
     */
    public static void login() {

        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登录
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdk.CLIENT_LoginEx2(m_strIpAddr, m_nPort, m_strUser, m_strPassword,
                nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("login Device[%s] Success!\n", m_strIpAddr);
        } else {
            System.err.printf("login Device[%s] Fail.Error[%s]\n", m_strIpAddr, "Test Error Code.");
            logOut();
        }
    }

    /**
     * login with high level 高安全级别登陆
     */
    public static void LoginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIpAddr.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netsdk.CLIENT_GetLastError());
        } else {
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
        }
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
     * logout 退出
     */
    public static void logOut() {
        System.out.println("LogOut Success");
        if (loginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(loginHandle);
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netsdk.CLIENT_Cleanup();
        System.exit(0);
    }
}
