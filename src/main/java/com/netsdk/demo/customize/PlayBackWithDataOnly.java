package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.LastError;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.PlaySDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author ： 47040
 * @since ： Created in 2020/12/10 19:30
 */
public class PlayBackWithDataOnly {

    ////////////////// SDK 初始化/清理资源 /////////////////////
    //////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    // The constant play sdk
    public static final PlaySDKLib playsdk = PlaySDKLib.PLAYSDK_INSTANCE;

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

        enableLog();  // 配置日志

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

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public boolean loginWithHighLevel() {

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
                    ToolKits.getErrorCode());
            return false;
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
            return true;
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("LogOut Success");
        }
    }

    //////////////////////////// 录像回放相关 /////////////////////////////
    //////////////////////////////////////////////////////////////////////

    private NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0);   // 回放句柄

    private final NetSDKLib.NET_TIME m_startTime = new NetSDKLib.NET_TIME(); // 开始时间
    private final NetSDKLib.NET_TIME m_stopTime = new NetSDKLib.NET_TIME();  // 结束时间

    private static final DownLoadPosCallBack m_PlayBackDownLoadPos = new DownLoadPosCallBack(); // 回放数据下载进度

    private static final DataCallBack m_dataCallBack = new DataCallBack(); // 回放数据回调

    private static final PlayDecCallBack m_playDecCallBack = new PlayDecCallBack();  // Play SDK 回放

    private static final ExecutorService dataParseService = Executors.newFixedThreadPool(5);

    private static final int channel = 10;   // NetSDK 对应的 设备通道

    private static final int port = 10;     // PlaySDK 解码端口


    // 回放进度回调
    public static class DownLoadPosCallBack implements NetSDKLib.fDownLoadPosCallBack {
        public void invoke(NetSDKLib.LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {
            if (0 != dwDownLoadSize) {
                // System.out.println("PlayBack DownLoadCallback: [ " + dwTotalSize + " ]" + " [ " + dwDownLoadSize + " ]");
                if (-1 == dwDownLoadSize) {
                    System.out.println("回放结束");
                }
            }
        }
    }

    private static final LinkedBlockingDeque<ByteArrayOutputStream> queue = new LinkedBlockingDeque<>();

    // 回放数据回调
    public static class DataCallBack implements NetSDKLib.fDataCallBack {
        public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
            ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
            try {
                memoryStream.write(pBuffer.getByteArray(0, dwBufSize));
                queue.offer(memoryStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class PlayDecCallBack implements PlaySDKLib.fDecCBFun {

        @Override
        public void invoke(int nPort, Pointer pBuf, int nSize, Pointer pFrameInfo, Pointer pUserData, int nReserved2) {
            // Todo
            dataParseService.submit(new Runnable() {
                @Override
                public void run() {
                    queryOSDTime(10);
                }
            });
        }
    }

    /**
     * 开启回放
     */
    public void StartPlayBack() {

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("Please Login First");
            return;
        }

        // 配置 PlaySDK
        playsdk.PLAY_OpenStream(port, null, 0, 4 * 1024 * 1024);
        playsdk.PLAY_SetDecCallBackEx(port, m_playDecCallBack, null);
        playsdk.PLAY_Play(port, null);

        // 设置回放时的码流类型
        IntByReference steamType = new IntByReference(0);           // 0-主辅码流,1-主码流,2-辅码流
        int emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE;

        boolean bret = netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, emType, steamType.getPointer());
        if (!bret) {
            System.err.println("Set Stream Type Failed" + ToolKits.getErrorCode());
        }

        // 设置回放时的录像文件类型
        IntByReference emFileType = new IntByReference(0); // 所有录像 NET_RECORD_TYPE
        emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_TYPE;
        bret = netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, emType, emFileType.getPointer());
        if (!bret) {
            System.err.println("Set Record Type Failed " + ToolKits.getErrorCode());
        }

        m_startTime.setTime(2021, 12, 15, 0, 0, 0);    // 开始时间
        m_stopTime.setTime(2021, 12, 15, 0, 2, 30);    // 结束时间

        m_hPlayHandle = netsdk.CLIENT_PlayBackByTimeEx(m_hLoginHandle, channel, m_startTime, m_stopTime,
                null, m_PlayBackDownLoadPos, null, m_dataCallBack, null);

        if (m_hPlayHandle.longValue() == 0) {
            int error = netsdk.CLIENT_GetLastError();
            System.err.println("PlayBackByTimeEx Failed " + ToolKits.getErrorCode());
            switch (error) {
                case LastError.NET_NO_RECORD_FOUND:
                    System.out.println("查找不到录像");
                    break;
                default:
                    System.out.println("开启失败");
                    break;
            }
        } else {
            System.out.println("PlayBackByTimeEx Succeed.");
        }

        // 启动新线程 顺序向PlaySDK写入数据 线程在句柄清空后关闭
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (m_hPlayHandle.longValue() != 0) {
                    try {
                        ByteArrayOutputStream stream = queue.take();
                        final byte[] data = stream.toByteArray();
                        playsdk.PLAY_InputData(port, data, data.length);
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 停止回放
     */
    public void StopPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        if (!netsdk.CLIENT_StopPlayBack(m_hPlayHandle)) {
            System.err.println("StopPlayBack Failed");
            return;
        }

        // 关闭PlaySDK解码端口
        playsdk.PLAY_Stop(port);
        playsdk.PLAY_CloseStream(port);

        System.out.println("StopPlayBack Succeed.");

        m_hPlayHandle.setValue(0);
    }

    public void queryOSDTime(){
        queryOSDTime(10);
    }

    public static void queryOSDTime(int port) {
        PlaySDKLib.TimeInfo timeInfo = new PlaySDKLib.TimeInfo();
        timeInfo.write();
        boolean ret = playsdk.PLAY_QueryInfo(port, 1, timeInfo.getPointer(), timeInfo.size(), new IntByReference(0));
        if (!ret) {
            System.err.println("Get OSD Time Failed " + playsdk.PLAY_GetLastErrorEx(port));
            return;
        }
        timeInfo.read();
        System.out.println("OSD Time: " + timeInfo.toString());

    }


    public void run() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "开始播放", "StartPlayBack"));
        menu.addItem(new CaseMenu.Item(this, "结束播放", "StopPlayBack"));
        menu.addItem(new CaseMenu.Item(this, "获取OSD时间", "queryOSDTime"));
        menu.run();
    }


    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private final String m_strIpAddr = "172.8.88.220";
    private final int m_nPort = 37777;
    private final String m_strUser = "admin";
    private final String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        PlayBackWithDataOnly demo = new PlayBackWithDataOnly();


        PlayBackWithDataOnly.Init();
        if (demo.loginWithHighLevel()) {
            demo.run();
        }
        demo.logOut();
        PlayBackWithDataOnly.cleanAndExit();
    }
}
