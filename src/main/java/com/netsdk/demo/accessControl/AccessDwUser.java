package com.netsdk.demo.accessControl;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class AccessDwUser {

    public static NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // 登录句柄
    public static Map<String, NetSDKLib.LLong> lLongMap = new HashMap<>();
    // 订阅句柄
    public static Map<String, NetSDKLib.LLong> attachLongMap = new HashMap<>();

    // 设备记录集
    public static Vector<CDevInfo> devInfo = new Vector<>();

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(DisconnectCallback.getInstance(), null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        enableLog();  // 配置日志

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

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    private static class DisconnectCallback implements NetSDKLib.fDisConnect {
        private static final DisconnectCallback instance = new DisconnectCallback();

        private DisconnectCallback() {
        }

        public static DisconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            // todo 回调里其他 netsdk 接口请开新线程 GateModule.stopRealLoadPic(attachLongMap.get(deviceId));
        }
    }

    /**
     * 需要下发的结构体请使用基本数据类型,避免使用String
     */
    public static class CDevInfo extends NetSDKLib.SdkStructure {
        public byte[] address = new byte[30];
        public int port;                        // 设备序列号
        public byte[] szSN = new byte[32];
        public NetSDKLib.LLong hLogin;          //登录句柄
        public int nChannel;

        public CDevInfo() {
        }

        public CDevInfo(String strAddr, int nPort, String strSN) {
            System.arraycopy(strAddr.getBytes(), 0, address, 0, strAddr.length());
            port = nPort;
            System.arraycopy(strSN.getBytes(), 0, szSN, 0, strSN.length());
        }

        @Override
        public String toString() {
            return "CDevInfo [szSN=" + new String(szSN).trim() + ", hLogin=" + hLogin.longValue() + ", nChannel=" + nChannel + "]";
        }
    }

    // 全登录与全订阅
    public void loginAndAttach() {
        if ((devInfo != null && devInfo.size() > 0)) {
            System.out.println("设备登入,设备总数：" + devInfo.size());
            for (CDevInfo dev : devInfo) {
                String szNo = new String(dev.szSN).trim();
                if (!lLongMap.containsKey(szNo)) {
                    doLoginAndAttach(dev);
                }
            }
        }
    }

    // 全退订与全登出
    public void detachAndLogout() {

        this.detachAll();
        this.logoutAll();
    }

    // 全登出
    public void logoutAll(){
        if ((devInfo != null && devInfo.size() > 0)) {
            System.out.println("设备登出,设备总数：" + devInfo.size());
            for (CDevInfo dev : devInfo) {
                String szNo = new String(dev.szSN).trim();
                if (!lLongMap.containsKey(szNo)) {
                    doLogOut(dev);
                }
            }
        }
    }

    // 全退订
    public void detachAll() {
        if ((attachLongMap != null && attachLongMap.size() > 0)) {
            System.out.println("需要订阅设备总数：" + attachLongMap.size());
            for (String key : attachLongMap.keySet()) {
                NetSDKLib.LLong m_hAttachHandle = attachLongMap.get(key);
                if (m_hAttachHandle.longValue() != 0) {
                    netsdk.CLIENT_StopLoadPic(m_hAttachHandle);
                }
            }
        }
    }

    // 全登录与全订阅
    public void doLoginAndAttach(CDevInfo dev) {
        String address = new String(dev.address).trim();
        int port = dev.port;
        String deviceId = new String(dev.szSN).trim();
        Pointer pointer = ToolKits.GetGBKStringToPointer(deviceId);
        //0:tcp登入2：主动注册登入
        final int nSpecCap = 0;
        final IntByReference error = new IntByReference();
        final NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
        /************************************************* 登录设备 ************************************************/
        //登录设备
        NetSDKLib.LLong m_hLoginHandle = netsdk.CLIENT_LoginEx2(address, (short) port, m_strUser, m_strPassword, nSpecCap, pointer, deviceInfo, error);
        if (m_hLoginHandle.longValue() == 0) {
            System.out.println(String.format("Login Device Failed ! deviceID: %s，address: %s,port: %d, Last Error: %s", deviceId, address, port, ToolKits.getErrorCode()));
            return;
        }
        System.out.println("登录设备成功，设备ID: " + deviceId);
        dev.nChannel = deviceInfo.byChanNum;
        dev.hLogin = m_hLoginHandle;

        /************************************************* 订阅设备 ************************************************/
        //回调函数
        AnalyzerDataCallBack analyzerDataCallBack = AnalyzerDataCallBack.getInstance();

        int bNeedPicture = 1; // 是否需要图片
        dev.write();
        NetSDKLib.LLong hAttach = netsdk.CLIENT_RealLoadPictureEx(dev.hLogin, 0, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCallBack, dev.getPointer(), null);
        dev.read();
        if (hAttach.longValue() == 0) {
            System.out.println("Failed to realLoad pic. deviceId；" + deviceId);
            return;
        }
        System.out.println("订阅成功，设备ID: " + deviceId);
        attachLongMap.put(deviceId, hAttach);
        //登录句柄
        lLongMap.put(deviceId, m_hLoginHandle);
    }

    public void doLogOut(CDevInfo dev) {
        NetSDKLib.LLong m_hLoginHandle = dev.hLogin;
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("LogOut Success " + Arrays.toString(dev.szSN));
        }
    }

    /**
     * 订阅回调
     */
    private static class AnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {
        private static final AnalyzerDataCallBack instance = new AnalyzerDataCallBack();

        private AnalyzerDataCallBack() {
        }

        public static AnalyzerDataCallBack getInstance() {
            return instance;
        }


        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            byte[] bufferBytes = new byte[dwBufSize];
            pBuffer.read(0, bufferBytes, 0, dwBufSize);
            File path = new File(".\\FaceRecoder");
            if (!path.exists()) {
                path.mkdir();
            }

            ///< 门禁事件
            if (dwAlarmType == NetSDKLib.EVENT_IVS_ACCESS_CTL) {
                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();

                ToolKits.GetPointerData(pAlarmInfo, msg);
                //卡号
                String card = new String(msg.szCardNo).trim();
                //开门用户
                String userId = new String(msg.szUserID).trim();
                //开门错误码
                int messageCode = msg.nErrorCode;
                //设备ID
                CDevInfo data = new CDevInfo();
                ToolKits.GetPointerData(dwUser, data);
                String szSn = new String(data.szSN).trim();
                System.out.println(String.format("设备ID: %s", szSn));
            }
            return 0;
        }
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_strUser = "admin";
    private String m_strPassword = "admin";
    //////////////////////////////////////////////////////////////////////

    public void InitTest(){
        Init(); // 打开工程，初始化
        this.loginAndAttach();
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.detachAndLogout();  // 退订并退出
        System.out.println("See You...");

        cleanAndExit();  // 清理资源并退出
    }

    public static void main(String[] args) {

        for (int i = 0; i < 100; i++) {
            devInfo.add(new CDevInfo("10.34.3.63", 37777 + i, "device:" + (37777 + i)));
        }
        AccessDwUser demo = new AccessDwUser();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}



