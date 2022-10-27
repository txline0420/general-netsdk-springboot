package com.netsdk.demo.customize.vehicle;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.DEV_RADAR_CONFIG;
import com.netsdk.lib.structure.NET_IN_TRAFFIC_RADAR_GET_OBJECT_INFO;
import com.netsdk.lib.structure.NET_OBJECT_RADAR_INFO;
import com.netsdk.lib.structure.NET_OUT_TRAFFIC_RADAR_GET_OBJECT_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;
import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR;

/**
 * @author 251589
 * @version V1.0
 * @Description: 车辆轨迹和位置
 * @date 2020/12/14 20:16
 */
public class TrackAndPositionDemo {

    static NetSDKLib netSDK = NetSDKLib.NETSDK_INSTANCE;
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);     //登陆句柄
    private NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0); // 订阅句柄
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public class fDisConnectCB implements NetSDKLib.fDisConnect {
        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public class HaveReConnect implements NetSDKLib.fHaveReConnect {
        public void invoke(NetSDKLib.LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    private fDisConnectCB disConnectCB = new fDisConnectCB();
    private HaveReConnect haveReConnect = new HaveReConnect();

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netSDK.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");
        netSDK.CLIENT_Cleanup();
        System.exit(0);
    }


    public void InitTest() {
        //初始化SDK库
        netSDK.CLIENT_Init(disConnectCB, null);

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        netSDK.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000; //登录请求响应超时时间设置为3S
        int tryTimes = 3;    //登录时尝试建立链接3次
        netSDK.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        netSDK.CLIENT_SetNetworkParam(netParam);

        // 打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + "track" + System.currentTimeMillis() + ".log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        boolean logOpen = netSDK.CLIENT_LogOpen(setLog);
        if (!logOpen) {
            System.err.println("Failed to open NetSDK log !!!");
        }

        // 设备登入
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSDK.CLIENT_LoginEx2(ip, port, username,
                password, nSpecCap, pCapParam, deviceInfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d]Success!\n", ip, port);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%s]\n", ip, port, ToolKits.getErrorCode() + "errorMsg" + ENUMERROR.getErrorMessage());
            EndTest();
        }
    }

    /**
     * 获取车辆轨迹、位置
     */
    public void getVehicleTrackAndPosition() {
        // 入参
        int queryType = netSDK.NET_QUERY_TRAFFIC_RADAR_GET_OBJECT;
        int channel = 0;
        int maxObjectNum = 10;
        NET_IN_TRAFFIC_RADAR_GET_OBJECT_INFO inParam = new NET_IN_TRAFFIC_RADAR_GET_OBJECT_INFO();
        inParam.nChannel = channel;
        NET_OUT_TRAFFIC_RADAR_GET_OBJECT_INFO outParam = new NET_OUT_TRAFFIC_RADAR_GET_OBJECT_INFO();
        outParam.nMaxObjectNum = maxObjectNum;

        // 出参
        NET_OBJECT_RADAR_INFO[] radarInfos = new NET_OBJECT_RADAR_INFO[maxObjectNum];
        for (int i = 0; i < maxObjectNum; i++) {
            radarInfos[i] = new NET_OBJECT_RADAR_INFO();
        }
        int memorySize = radarInfos[0].size() * maxObjectNum;
        outParam.pObjectInfo = new Memory(memorySize);
        outParam.pObjectInfo.clear(memorySize);
        ToolKits.SetStructArrToPointerData(radarInfos, outParam.pObjectInfo);  // 将数组内存拷贝到Pointer
        inParam.write();
        outParam.write();
        boolean ret = netSDK.CLIENT_QueryDevInfo(loginHandle, queryType, inParam.getPointer(), outParam.getPointer(), null, 3000);
        inParam.read();
        outParam.read();
        if (ret) {
            ToolKits.GetPointerDataToStructArr(outParam.pObjectInfo, radarInfos);
            System.out.println("getVehicleTrackAndPosition succeed!\n 共查询到目标个数： " + outParam.nObjectNum);
            for (int i = 0; i < outParam.nObjectNum; i++) {
                System.out.println("物体ID: " + radarInfos[i].nID);
                System.out.println("物体在道路方向上的坐标，雷达为坐标原点 单位：cm: " + radarInfos[i].nVerticalPos);
                System.out.println("物体在垂直道路方向上的坐标，雷达为坐标原点 单位：cm: " + radarInfos[i].nHorizontalPos);
                System.out.println("物体长度 单位：cm: " + radarInfos[i].nObjectLen);
                System.out.println("第" + i + "个目标");
            }
        } else {
            System.out.println("getVehicleTrackAndPosition failed!" + ToolKits.getErrorCode() + "  " + ENUMERROR.getErrorMessage());
        }
    }

    public void getRadarConfig() {
        int type = NET_EM_CFG_RADAR;    // 雷达配置，对应结构体 DEV_RADAR_CONFIG
        int channelId = 0;
        DEV_RADAR_CONFIG config = new DEV_RADAR_CONFIG();
        config.write();
        boolean ret = netSDK.CLIENT_GetConfig(loginHandle, type, channelId, config.getPointer(), config.dwSize, 3000, null);
        if (ret){
            config.read();
            System.out.println("设备地址，如果串口上挂了多个串口设备，通过地址区分： " + config.nAddress);
            System.out.println("速度先来情况下等待时间，速度来时尚未抓拍 范围 (1 -- 5000ms)： " + config.nPreSpeedWait);
            System.out.println("速度后来情况下等待时间，抓拍时还没有来速度 范围 (1 -- 5000ms)： " + config.nDelaySpeedWait);
            System.out.println("雷达配置是否可用： " + config.bDahuaRadarEnable);
            System.out.println("雷达参数： " + config.stuDhRadarConfig);
            System.out.println("森思泰克77Ghz网络雷达配置是否可用: " + config.bSTJ77D5RadarEnable);
            System.out.println("森思泰克77Ghz网络雷达配置: " + config.stuSTJ77D5RadarConfig);

        } else {
            System.out.println("CLIENT_GetConfig Failed! "+ ToolKits.getErrorCode() + "  " + ENUMERROR.getErrorMessage());
        }
    }

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }


    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "车辆轨迹和坐标位置", "getVehicleTrackAndPosition"));
        menu.addItem(new CaseMenu.Item(this, "获取雷达配置", "getRadarConfig"));
        menu.run();
    }

    ////////////////////////////////////////////////////////////////
    String ip = "192.168.129.119";
    int port = 37777;
    String username = "admin";
    String password = "admin123";

    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        TrackAndPositionDemo demo = new TrackAndPositionDemo();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
