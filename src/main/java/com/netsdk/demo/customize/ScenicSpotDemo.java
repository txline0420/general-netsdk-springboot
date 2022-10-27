package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.NET_EM_SHAPE_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/10/26 17:38
 */
public class ScenicSpotDemo {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    ////////////////////////////////// SDK 初始化相关 //////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数需要是静态的，防止被系统回收
    private static final NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static final NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调

    /**
     * Init boolean. sdk 初始化
     *
     * @return the boolean
     */
    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.err.println("Initialize SDK failed");
            return false;
        }
        System.out.println("Initialize SDK Succeed");

        EnableLog();  // 配置日志

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
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间, 拉流的响应时间也是这个
        netsdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 打开 sdk log
     */
    private static void EnableLog() {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("sdklog/");
        if (!path.exists()) path.mkdir();

        // 这里的log保存地址依据实际情况自己调整
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Failed to open NetSDK log");
        System.out.println("Open NetSDK Log Succeed");
        System.out.println(logPath);
    }

    /**
     * Cleanup. 清除 sdk 环境
     */
    public static void Cleanup() {
        if (bLogOpen) {
            netsdk.CLIENT_LogClose();
            System.out.println("NetSDK Log Closed");
        }
        if (bInit) {
            netsdk.CLIENT_Cleanup();
            System.out.println("NetSDK Clean Up Succeed");
        }
    }

    /**
     * 清理并退出
     */
    public static void CleanAndExit() {
        netsdk.CLIENT_Cleanup();
        System.exit(0);
    }

    ////////////////////////////////// SDK 登录相关 ////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    public NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);              // 登录句柄

    // 高安全登陆
    private void Login(String strIp, int port, String strUser, String strPassword) {
        // 入参
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        System.arraycopy(strIp.getBytes(), 0, pstlnParam.szIP, 0, strIp.length());                      // IP
        pstlnParam.nPort = port;                                                                                       // Port
        System.arraycopy(strUser.getBytes(), 0, pstlnParam.szUserName, 0, strUser.length());            // Username
        System.arraycopy(strPassword.getBytes(), 0, pstlnParam.szPassword, 0, strPassword.length());    // Password

        // 出参
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 高安全登陆
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Succeed!\n", strIp);
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Device Address: " + strIp + " Port: " + port);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        } else {
            System.err.printf("Login Device[%s] Failed.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
    }

    // 登出
    private void LoginOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            m_hLoginHandle.setValue(0);
        }
    }

    ///////////////////////////////////  景物信息管理接口 ////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private final Scanner sc = new Scanner(System.in);   // 控制台输入

    /**
     * 获取景物点支持的能力
     */
    public void TestScenicSpotGetCaps() {

        System.out.println("请输入全景相机的通道号(SDK从0计数):");
        int channel = sc.nextInt();

        NET_IN_SCENICSPOT_GETCAPS_INFO stuInGetCapInfo = new NET_IN_SCENICSPOT_GETCAPS_INFO();
        stuInGetCapInfo.nChannel = channel;
        NET_OUT_SCENICSPOT_GETCAPS_INFO stuOutGetCapInfo = new NET_OUT_SCENICSPOT_GETCAPS_INFO();

        stuInGetCapInfo.write();
        stuOutGetCapInfo.write();
        boolean ret = netsdk.CLIENT_ScenicSpotGetCaps(m_hLoginHandle, stuInGetCapInfo.getPointer(), stuOutGetCapInfo.getPointer(), 3000);
        if (!ret) {
            System.err.println(String.format("获取景物点支持能力失败: %s", ToolKits.getErrorCode()));
        }
        System.out.println("获取景物点支持能力成功");

        stuOutGetCapInfo.read();

        // 打印信息
        StringBuilder capInfo = new StringBuilder().append(String.format("————————————通道[%3d] 景物点支持的能力情况————————\n", channel))
                .append("bEnable 是否支持景物点功能: ").append(stuOutGetCapInfo.stuCaps.bEnable == 0 ? "不支持" : "支持").append("\n");
        if (stuOutGetCapInfo.stuCaps.bEnable != 0) {
            capInfo.append("nTotalNum 最多支持景物点数: ").append(stuOutGetCapInfo.stuCaps.nTotalNum).append("\n")
                    .append("nRegionNum 单画面最多支持景物点数: ").append(stuOutGetCapInfo.stuCaps.nRegionNum);
        }
        System.out.println(capInfo.toString());
    }

    /**
     * 获取所有景物点信息
     * 8 目全景 AR 支持 256 路，查询全部会看见多出的最后一路，而且编号也太对，先不要使用它，用前256路
     * 另外注意，通道必须指定是全景相机的通道，设置在球机通道内是无效的
     */
    public void TestScenicSpotGetPointInfos() {

        System.out.println("请输入全景相机的通道号(SDK从0计数):");
        int channel = sc.nextInt();

        // 景物点的信息不是一次性获取的，而是根据 nOffset 和 nLimit 参数多次获取，类似于网页分页查询
        // 请根据自己的网络状况合理设置参数
        // 这里 我以10条一次的方式 顺序获取所有景物点信息

        int nMaxFetch = 10;
        int offset = 0;

        NET_IN_SCENICSPOT_GETPOINTINFOS_INFO stuInGetPointInfo = new NET_IN_SCENICSPOT_GETPOINTINFOS_INFO();
        stuInGetPointInfo.nChannelID = channel;
        stuInGetPointInfo.nOffset = offset;
        stuInGetPointInfo.nLimit = nMaxFetch;

        NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO stuOutGetPointInfo = new NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO();

        ScenicSpoteGetPointInfos(stuInGetPointInfo, stuOutGetPointInfo, m_hLoginHandle);

        // 景物点信息总数
        int totalCount = stuOutGetPointInfo.nTotal;
        // 打印信息
        int retCount = stuOutGetPointInfo.nRetSceneNum;

        PrintScenicSpotInfos(stuOutGetPointInfo, retCount);
        offset += retCount;

        // 说明还有没获取的信息, 循环继续获取
        while (!(retCount < nMaxFetch)) {
            stuInGetPointInfo.nOffset = offset;
            ScenicSpoteGetPointInfos(stuInGetPointInfo, stuOutGetPointInfo, m_hLoginHandle);
            retCount = stuOutGetPointInfo.nRetSceneNum;
            PrintScenicSpotInfos(stuOutGetPointInfo, retCount);
            offset += retCount;
        }

    }

    /**
     * 获取单个景物点信息
     * 提示: 景物点的编号 nIndex 默认是和偏移量 nOffset 一一对应的，设置时也请不要破坏这种对应关系
     * 另外注意，通道必须指定是全景相机的通道，设置在球机通道内是无效的
     */
    public void TestScenicSpotGetPointInfoSingle() {

        System.out.println("请输入全景相机的通道号(SDK从0计数):");
        int channel = sc.nextInt();

        System.out.println("请输入需要获取景物点信息的序号(从0计数):");
        int offset = sc.nextInt();

        // 景物点的信息是根据 nOffset 和 nLimit 参数获取的，类似于网页分页查询
        // 指定 nOffset 并设置 nLimit = 1 即可获取指定景物点信息

        NET_IN_SCENICSPOT_GETPOINTINFOS_INFO stuInGetPointInfo = new NET_IN_SCENICSPOT_GETPOINTINFOS_INFO();
        stuInGetPointInfo.nChannelID = channel;
        stuInGetPointInfo.nOffset = offset;
        stuInGetPointInfo.nLimit = 1;

        NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO stuOutGetPointInfo = new NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO();

        ScenicSpoteGetPointInfos(stuInGetPointInfo, stuOutGetPointInfo, m_hLoginHandle);

        // 景物点信息总数
        int totalCount = stuOutGetPointInfo.nTotal;
        // 打印信息
        int retCount = stuOutGetPointInfo.nRetSceneNum;

        PrintScenicSpotInfos(stuOutGetPointInfo, retCount);
    }

    // 打印景物点信息组
    private static void PrintScenicSpotInfos(NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO stuOutGetPointInfo, int retCount) {
        for (int i = 0; i < retCount; i++) {
            StringBuilder info = new StringBuilder();
            try {
                info.append("nIndex 编号: ").append(stuOutGetPointInfo.stuPointInfos[i].nIndex).append("\n")
                        .append("bEnable 是否生效: ").append(stuOutGetPointInfo.stuPointInfos[i].bEnable == 0 ? "不生效" : "生效").append("\n")
                        .append("bTitleAttribute 是否有子标题: ").append(stuOutGetPointInfo.stuPointInfos[i].bTitleAttribute == 0 ? "是" : "否").append("\n")
                        .append("stuPoint 景物点8192坐标: ").append(
                        String.format("(%4d, %4d)",
                                stuOutGetPointInfo.stuPointInfos[i].stuPoint.nX,
                                stuOutGetPointInfo.stuPointInfos[i].stuPoint.nY)).append("\n")
                        .append("szTitleName 一级标题名称: ").append(new String(stuOutGetPointInfo.stuPointInfos[i].szTitleName, encode)).append("\n")
                        .append("byTitleType 一级标题类型: ").append(stuOutGetPointInfo.stuPointInfos[i].byTitleType).append("\n")
                        .append("emShapeType 景物形状: ").append(NET_EM_SHAPE_TYPE.getNoteByValue(stuOutGetPointInfo.stuPointInfos[i].emShapeType)).append("\n")
                        .append("nRetPolygonPointNum 轮廓点个数: ").append(stuOutGetPointInfo.stuPointInfos[i].nRetPolygonPointNum).append("\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            for (int j = 0; j < stuOutGetPointInfo.stuPointInfos[i].nRetPolygonPointNum; j++) {
                info.append(String.format("轮廓点[%2d]:(%4d, %4d)\n", j, stuOutGetPointInfo.stuPointInfos[i].stuPolygon[j].nx,
                        stuOutGetPointInfo.stuPointInfos[i].stuPolygon[j].ny));
            }
            System.out.println(info.append("——————————————————————————————\n").toString());
        }
    }

    // 获取景物点信息
    private static void ScenicSpoteGetPointInfos(NET_IN_SCENICSPOT_GETPOINTINFOS_INFO stuInGetPointInfo,
                                                 NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO stuOutGetPointInfo,
                                                 NetSDKLib.LLong m_hLoginHandle) {
        stuInGetPointInfo.write();
        stuOutGetPointInfo.write();
        boolean ret = netsdk.CLIENT_ScenicSpotGetPointInfos(m_hLoginHandle, stuInGetPointInfo.getPointer(), stuOutGetPointInfo.getPointer(), 3000);
        if (!ret) {
            System.err.println(String.format("获取景物点信息失败: %s", ToolKits.getErrorCode()));
        }
        stuOutGetPointInfo.read();
    }

    /**
     * 设置景物点信息
     * 特别指出：Polygon 即轮廓坐标也是全景相机的 8192 坐标，不是球机的
     * 另外注意，通道必须指定是全景相机的通道，设置在球机通道内是无效的
     */
    public void TestScenicSpotSetPointInfo() {

        System.out.println("请输入全景相机的通道号(SDK从0计数):");
        int channel = sc.nextInt();

        System.out.println("请输入需要获取景物点信息的序号(从0计数):");
        int offset = sc.nextInt();

        /**
         * 这里的用例是:
         * {
         *     "channel": #{channel},
         *     "Index": #{offset},
         *     "Enable": true,
         *     "TitleAttribute": true,
         *     "Point": [ 6642, 5127 ],
         *     "ShapType": 0,
         *     "Polygon": [[6000,5000],[6000,6000],[7000,6000],[7000,5000]],
         *     "TitleName": "景物 001",
         *     "TitleType": 1
         * }
         */

        ///////////////////////////////// 获取原先信息 //////////////////////////////////
        ////////////////////// 理论上，在原来的基础上修改是最合适的 ////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        NET_IN_SCENICSPOT_GETPOINTINFOS_INFO stuInGetPointInfo = new NET_IN_SCENICSPOT_GETPOINTINFOS_INFO();
        stuInGetPointInfo.nChannelID = channel;
        stuInGetPointInfo.nOffset = offset;
        stuInGetPointInfo.nLimit = 1;

        NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO stuOutGetPointInfo = new NET_OUT_SCENICSPOT_GETPOINTINFOS_INFO();

        ScenicSpoteGetPointInfos(stuInGetPointInfo, stuOutGetPointInfo, m_hLoginHandle);
        POINTINFOS prePointInfo = stuOutGetPointInfo.stuPointInfos[0];

        ///////////////////////////////// 设置新的信息 //////////////////////////////////
        ///////////////////// 当然这里，出于示例，我把所有有效字段都填一下 //////////////////
        ///////////////////////////////////////////////////////////////////////////////

        NET_IN_SCENICSPOT_SETPOINTINFO_INFO stuInSetPointInfo = new NET_IN_SCENICSPOT_SETPOINTINFO_INFO();
        stuInSetPointInfo.nChannel = channel;
        stuInSetPointInfo.nIndex = offset;
        stuInSetPointInfo.bEnable = 1;                                               // "Enable": true 启用AR标签
        byte[] szTitleName = new byte[0];
        try {
            szTitleName = "景物001".getBytes(encode);                                // 标签名称，中文字符必须指定编码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(szTitleName, 0, stuInSetPointInfo.szTitleName, 0, szTitleName.length); // "TitleName": "景物001"
        stuInSetPointInfo.byTitleType = 1;          // "TitleType": 1 标题类型 协议没有列出具体有哪些类型 默认写1
        stuInSetPointInfo.emShapeType = NET_EM_SHAPE_TYPE.NET_EM_SHAPE_TYPE_MANSARD.getValue();    // "ShapType": 0 轮廓类型
        stuInSetPointInfo.stuPoint = new POINTCOORDINATE(6642, 5127);                        // "Point": [ 6642, 5127 ] 云台在全景相机内的坐标
        stuInSetPointInfo.nPolygonPointNum = 4;                                                    // "Polygon" 轮廓坐标集，全景坐标的8192坐标
        stuInSetPointInfo.stuPolygon[0] = new NetSDKLib.DH_POINT((short) 6000, (short) 5000);
        stuInSetPointInfo.stuPolygon[1] = new NetSDKLib.DH_POINT((short) 6000, (short) 6000);
        stuInSetPointInfo.stuPolygon[2] = new NetSDKLib.DH_POINT((short) 7000, (short) 6000);
        stuInSetPointInfo.stuPolygon[3] = new NetSDKLib.DH_POINT((short) 7000, (short) 5000);
        stuInSetPointInfo.bTitleAttribute = 1;      // "TitleAttribute": true 有子标题
        // 由于OSD是在8900平台端叠加的，此Demo无法验证具体叠加效果。 "TitleType" "TitleAttribute" 的具体参数可能需要依据在平台端的测试结果作调整

        NET_OUT_SCENICSPOT_SETPOINTINFO_INFO stuOutSetPointInfo = new NET_OUT_SCENICSPOT_SETPOINTINFO_INFO();

        stuInSetPointInfo.write();
        stuOutSetPointInfo.write();
        boolean ret = netsdk.CLIENT_ScenicSpotSetPointInfo(m_hLoginHandle, stuInSetPointInfo.getPointer(), stuOutSetPointInfo.getPointer(), 3000);
        if (!ret) {
            System.err.println(String.format("设置景物点信息失败: %s", ToolKits.getErrorCode()));
        }
        System.out.println("设置景物点信息成功");
    }

    /**
     * 以景物标注点为中心，进行三维定位
     * 特别指出： 由于AR标签并不保存球机三维坐标( NET_IN_SCENICSPOT_SETPOINTINFO_INFO内的 stuPosition是无效字段)
     * 所以球机只会依据全景坐标点平移，不会 放大/缩小
     * 另外，景物轮廓的OSD显示是8900平台端的功能，设备的预览界面并不会显示。
     * 注意，通道必须指定是全景相机的通道，设置在球机通道内是无效的
     */
    public void TestScenicSpotTurnToPoint() {

        System.out.println("请输入全景相机的通道号(SDK从0计数):");
        int channel = sc.nextInt();

        System.out.println("请输入需要进行三维定位景物点的编号(从0计数):");
        int index = sc.nextInt();

        NET_IN_SCENICSPOT_TURNTOPOINT_INFO stuInTurnToPoint = new NET_IN_SCENICSPOT_TURNTOPOINT_INFO();
        stuInTurnToPoint.nChannel = channel;
        stuInTurnToPoint.nIndex = index;

        NET_OUT_SCENICSPOT_TURNTOPOINT_INFO stuOutTurnToPoint = new NET_OUT_SCENICSPOT_TURNTOPOINT_INFO();

        stuInTurnToPoint.write();
        stuOutTurnToPoint.write();

        boolean ret = netsdk.CLIENT_ScenicSpotTurnToPoint(m_hLoginHandle, stuInTurnToPoint.getPointer(), stuOutTurnToPoint.getPointer(), 3000);
        if (!ret) {
            System.err.println(String.format("景物点三维定位失败: %s", ToolKits.getErrorCode()));
        }
        System.out.println("景物点三维定位成功");
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /// 为了弥补缺少球机三维坐标的问题，我把 球机绝对坐标跳转 和 获取球机绝对坐标 的接口也列在这里 ///
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * 球机绝对坐标跳转
     * 注意，通道必须指定是球机通道
     */
    public boolean ptzControlPreciseControl() {

        System.out.println("请输入云台相机的通道号(SDK从0计数):");
        int nChannelID = sc.nextInt();

        System.out.println("请输入X坐标:");
        int xParam = sc.nextInt();

        System.out.println("请输入Y坐标:");
        int yParam = sc.nextInt();

        System.out.println("请输入Z坐标:");
        int zoomParam = sc.nextInt();

        /*
         *  1）xParam：水平角度(0~3600)
         * 	2）yParm：垂直坐标(0~900) 即正 90度转角, 但现在很多设备已支持负转角，请以实际测试为准
         * 	3）zoomParm：变倍(1~128),变倍为档位,并非实际变倍倍数比如球机最大变倍能力为50倍，接口的变倍值填写20，实际变倍值为  50*(20/128)
         */
        return netsdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_EXACTGOTO,           // 精确控制
                xParam, yParam, zoomParam, 1);

    }

    /**
     * 获取球机绝对坐标
     * 注意，通道必须指定是球机通道
     */
    public void ptzQueryPreciseControlStatus() {

        System.out.println("请输入云台相机的通道号(SDK从0计数):");
        int nChannelID = sc.nextInt();

        int nType = NetSDKLib.NET_DEVSTATE_PTZ_LOCATION;
        NetSDKLib.NET_PTZ_LOCATION_INFO ptzLocationInfo = new NetSDKLib.NET_PTZ_LOCATION_INFO();
        ptzLocationInfo.nChannelID = nChannelID;
        IntByReference intRetLen = new IntByReference();

        ptzLocationInfo.write();
        boolean bRet = netsdk.CLIENT_QueryDevState(m_hLoginHandle, nType, ptzLocationInfo.getPointer(), ptzLocationInfo.size(), intRetLen, 3000);
        ptzLocationInfo.read();

        if (bRet) {
            System.out.println("xParam:" + ptzLocationInfo.nPTZPan);
            System.out.println("yParam:" + ptzLocationInfo.nPTZTilt);
            System.out.println("zoomParam:" + ptzLocationInfo.nPTZZoom);
        } else {
            System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
        }
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {
        ScenicSpotDemo.Init();                                    // 初始化SDK库
        this.Login(m_strIp, m_nPort, m_strUser, m_strPassword);   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取景物点支持的能力", "TestScenicSpotGetCaps"));
        menu.addItem(new CaseMenu.Item(this, "获取所有景物点信息", "TestScenicSpotGetPointInfos"));
        menu.addItem(new CaseMenu.Item(this, "获取单个景物点信息", "TestScenicSpotGetPointInfoSingle"));
        menu.addItem(new CaseMenu.Item(this, "设置景物点信息", "TestScenicSpotSetPointInfo"));
        menu.addItem(new CaseMenu.Item(this, "以景物标注点为中心进行三维定位", "TestScenicSpotTurnToPoint"));
        menu.addItem(new CaseMenu.Item(this, "获取当前云台坐标", "ptzQueryPreciseControlStatus"));
        menu.addItem(new CaseMenu.Item(this, "云台坐标跳转", "ptzControlPreciseControl"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.LoginOut();                  // 退出
        System.out.println("See You...");
        ScenicSpotDemo.CleanAndExit();    // 清理资源并退出
    }

    ////////////////////////////////////////////////////////////////
    public String m_strIp = "10.18.128.106";
    public int m_nPort = 37777;
    public String m_strUser = "admin";
    public String m_strPassword = "admin123";
    ////////////////////////////////////////////////////////////////

    public static void main(String[] args) {

        ScenicSpotDemo demo = new ScenicSpotDemo();
        if (args.length == 4) {
            demo.m_strIp = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
