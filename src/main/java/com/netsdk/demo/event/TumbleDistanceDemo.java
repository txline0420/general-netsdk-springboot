package com.netsdk.demo.event;

import com.netsdk.demo.intelligentTraffic.parkingDemo.ParkingUtils;
import com.netsdk.demo.units.TimeUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.DEV_EVENT_DISTANCE_DETECTION_INFO;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Date;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @Author 251589
 * @Description： 宁波智慧公交 跌倒事件和异常间距事件
 * @Date 2020/11/28 9:47
 */
public class TumbleDistanceDemo {

    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    //登录句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 智能订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);

    /**
     * 登录设备
     */
    public boolean login() {
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, pCapParam, deviceInfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d] Success!\n", m_strIp, m_nPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d] Failed. %s\n", m_strIp, m_nPort, ToolKits.getErrorCode());
        }

        return loginHandle.longValue() != 0;
    }


    /**
     * 登出设备
     */
    public void logout() {
        if (loginHandle.longValue() != 0)
            netSdk.CLIENT_Logout(loginHandle);
    }

    public void InitTest() {
        ParkingUtils.Init(); // SDK初始化
        if (!login())  // 登陆设备
            EndTest();
    }

    public void EndTest() {
        stopLoadPicture();    // 取消订阅
        logout(); //	登出设备
        System.out.println("See You...");
        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }

    /**
     * 远程抓图（异步）
     */
    public void snapPicture() {
        // send capture picture instruction to device
        NetSDKLib.SNAP_PARAMS stuSnapParams = new NetSDKLib.SNAP_PARAMS();
        stuSnapParams.Channel = 0;            // channel
        stuSnapParams.mode = 0;               // capture picture mode
        stuSnapParams.Quality = 3;            // picture quality
        stuSnapParams.InterSnap = 0;
        stuSnapParams.CmdSerial = 0;

        IntByReference reserved = new IntByReference(0);
        if (!netSdk.CLIENT_SnapPictureEx(loginHandle, stuSnapParams, reserved)) {
            System.err.printf("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_SnapPictureEx success");
        }
    }

    public static class SnapCallback implements NetSDKLib.fSnapRev {
        private static SnapCallback instance = new SnapCallback();

        private File path;

        private SnapCallback() {
            path = new File("./Snap/");
            if (!path.exists()) {
                path.mkdir();
            }
        }

        public static SnapCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
            if (EncodeType == 10) { // jpg
                String fileName = path + File.separator + "AsyncSnapPicture_" + TimeUtils.getTimeStringWithoutSign(new Date()) + ".jpg";
                //保存图片到本地文件
                ToolKits.savePicture(pBuf, RevLen, fileName);
            }
        }
    }


    /**
     * 【需求描述】 跌倒事件檢測
     */
    public boolean realLoadPicture() {
        int bNeedPicture = 1;   // 是否需要图片
        int channel = 0; // 订阅全通道
        if (deviceInfo.byChanNum == 1)
            channel = 0;

        // AnalyzerDataCB.getInstance() 回调
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel,
                NetSDKLib.EVENT_IVS_ALL, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Channel[%d] RealLoad Picture Succeed！\n", channel);
        } else {
            System.err.printf("Channel[%d] RealLoad Picture Failed！ errCode == %s\n errMsg == %s\n", channel, ToolKits.getErrorCode(), ENUMERROR.getErrorMessage());
        }
        return attachHandle.longValue() != 0;
    }

    /**
     * 订阅报警信息
     *
     * @return
     */
    public void startListen() {
        // 设置报警回调函数

        netSdk.CLIENT_SetDVRMessCallBack(ParkingMessageCallBackEx1.getSingleInstance(), null);

        // 订阅报警
        boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
        if (!bRet) {
            System.err.printf("订阅报警失败! error:%d\n", netSdk.CLIENT_GetLastError() + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("订阅报警成功.");
        }
    }

    public static class ParkingMessageCallBackEx1 implements NetSDKLib.fMessCallBackEx1 {

        private static com.netsdk.demo.intelligentTraffic.parkingDemo.ParkingMessageCallBackEx1 singleInstance;

        public static com.netsdk.demo.intelligentTraffic.parkingDemo.ParkingMessageCallBackEx1 getSingleInstance() {
            if (singleInstance == null) {
                singleInstance = new com.netsdk.demo.intelligentTraffic.parkingDemo.ParkingMessageCallBackEx1();
            }
            return singleInstance;
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

        @Override
        public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, int bAlarmAckFlag, NativeLong nEventID, Pointer dwUser) {
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_FIREWARNING_INFO: {
                    dealData(pStuEvent);
                    break;
                }
                default:
                    System.out.printf("Get Other Event 0x%x\n", lCommand);
                    break;
            }

            return true;
        }

        // 处理事件
        private static void dealData(Pointer pAlarmInfo) {
            NetSDKLib.ALARM_FIREWARNING_INFO_DETAIL msg = new NetSDKLib.ALARM_FIREWARNING_INFO_DETAIL();
            ToolKits.GetPointerData(pAlarmInfo, msg);
            for (int i = 0; i < msg.stuFireWarningInfo.length; i++) {
                System.out.println("msg 距离" + msg.stuFireWarningInfo[i].nDistance);
                System.out.println("msg GPS" + msg.stuFireWarningInfo[i].stuGpsPoint.dwLatidude + "," +msg.stuFireWarningInfo[i].stuGpsPoint.dwLongitude);
                System.out.println("msg 温度" + msg.stuFireWarningInfo[i].nTemperatureUnit);
            }

        }

    }


    /**
     * 跌倒事件回调
     */
    public static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        // (倒地报警事件)对应数据块描述信息
        private NetSDKLib.DEV_EVENT_TUMBLE_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_TUMBLE_DETECTION_INFO();

        private File path;

        private AnalyzerDataCB() {

            path = new File("./Snap/");
            if (!path.exists())
                path.mkdir();
        }

        private static class CallBackHolder {
            private static AnalyzerDataCB instance = new AnalyzerDataCB();
        }

        public static AnalyzerDataCB getInstance() {
            return CallBackHolder.instance;
        }

        // 回调
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo,
                          Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }
            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TUMBLE_DETECTION: {  // 跌倒事件
                    dealData4TumbleDetection(pAlarmInfo);
                    break;
                }
                case NetSDKLib.EVENT_IVS_DISTANCE_DETECTION: {  // 异常距离检测事件
                    dealData4DistanceDetection(pAlarmInfo);
                    break;
                }
                default:
                    System.out.printf("其他事件： 0x%x\n" , dwAlarmType);
                    break;
            }
            return 0;
        }
        // 处理事件
        private void dealData4TumbleDetection(Pointer pAlarmInfo) {
            ToolKits.GetPointerData(pAlarmInfo, msg);
            System.out.println("跌倒 -> 事件ID：" + msg.nEventID);
            System.out.println("跌倒 -> 事件动作: " + msg.nAction);
            System.out.println("跌倒 -> 目标ID: " + msg.nObjectID);
            System.out.println("跌倒 -> 智能事件所属大类: " + msg.emClassType);
            System.out.println("跌倒 -> 事件发生的时间: " + msg.UTC);
        }
        // 异常间距事件对应的数据块描述信息
        private void dealData4DistanceDetection(Pointer pAlarmInfo) {
            DEV_EVENT_DISTANCE_DETECTION_INFO distanceDetectionInfo = new DEV_EVENT_DISTANCE_DETECTION_INFO();
            ToolKits.GetPointerData(pAlarmInfo, distanceDetectionInfo);
            NetSDKLib.DH_MSG_OBJECT object = new NetSDKLib.DH_MSG_OBJECT();
            ToolKits.GetPointerData(distanceDetectionInfo.stuObject.getPointer(), object);
            System.out.println("异常间距 -> 事件ID：" + distanceDetectionInfo.nEventID);
            System.out.println("异常间距 -> 事件动作: " + distanceDetectionInfo.nAction);
            System.out.println("异常间距 -> 检测区域顶点数: " + distanceDetectionInfo.nDetectRegionNum);
            System.out.println("异常间距 -> 事件发生的时间: " + distanceDetectionInfo.UTC);
        }
    }

    /**
     * 停止智能订阅
     */
    public void stopLoadPicture() {
        if (attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
            attachHandle.setValue(0);
            System.out.println("Had Stop RealLoad Picture！");
        }
    }

    /**
     * 云台配置
     */
    public void cfgPTZ() {
        NetSDKLib.CFG_PTZ_INFO stuPTZ = new NetSDKLib.CFG_PTZ_INFO();
        if (!ToolKits.GetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZ, stuPTZ)) {
            System.err.println("Get PTZ Failed!" + ToolKits.getErrorCode());
            return;
        }

        System.out.printf("Enable: %d  PresetId:%d FreeSec:%d \n", stuPTZ.bEnable,
                stuPTZ.stuPresetHoming.nPtzPresetId, stuPTZ.stuPresetHoming.nFreeSec);

        stuPTZ.bEnable = 1;    // 使能
        stuPTZ.stuPresetHoming.nFreeSec = 36; // 归位时间，单位为秒
        if (!ToolKits.SetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZ, stuPTZ)) {
            System.err.println("Set PTZ Failed!" + ToolKits.getErrorCode());
        } else {
            System.err.println("Set PTZ Success!");
        }
    }

    // 选中目标进行抓拍
    public void snapPictureByAnalyseObject() {
        NetSDKLib.NET_IN_SNAP_BY_ANALYSE_OBJECT stuIn = new NetSDKLib.NET_IN_SNAP_BY_ANALYSE_OBJECT();

        stuIn.nChannelID = 0;    // 通道号
        stuIn.nSnapObjectNum = 1;    // 抓拍物体个数
        // 抓拍物体信息 (点坐标归一化到[0, 8192]坐标)
        // 实际使用时通过如下计算:  8192.0 * 相对坐标/显示宽(高) 其中左右通过显示的宽度计算，上下通过显示的高度计算
        // 如 (int)(8192.0*80/120)——浮点计算是为了使结果更精确
//    	stuIn.stuSnapObjects[0].stuBoundingBox.left = 0;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.top = 0;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.right = 8192;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.bottom = 8192;
        stuIn.stuSnapObjects[0].stuBoundingBox.left = 4154;
        stuIn.stuSnapObjects[0].stuBoundingBox.top = 3172;
        stuIn.stuSnapObjects[0].stuBoundingBox.right = 5160;
        stuIn.stuSnapObjects[0].stuBoundingBox.bottom = 4380;

        NetSDKLib.NET_OUT_SNAP_BY_ANALYSE_OBJECT stuOut = new NetSDKLib.NET_OUT_SNAP_BY_ANALYSE_OBJECT();
        if (!netSdk.CLIENT_SnapPictureByAnalyseObject(loginHandle, stuIn, stuOut, 5000)) {
            System.err.println("Snap Picture By Analyse Object Failed!" + ToolKits.getErrorCode());
        } else {
            System.err.println("Snap Picture By Analyse Object Success!");
        }
    }

    /**
     * 场景抓拍设置
     */
    public void snapShotWithRulecfg() {
        int nMaxRuleNum = 10;    // 可根据实际修改
        NetSDKLib.NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[] stuRule = new NetSDKLib.NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[nMaxRuleNum];
        for (int i = 0; i < nMaxRuleNum; ++i) {
            stuRule[i] = new NetSDKLib.NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
        }

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_SCENE_SNAP_SHOT_WITH_RULE2;    // 场景抓拍设置
        int nChannelID = 0;
        NetSDKLib.NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO stuCfg = new NetSDKLib.NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
        stuCfg.nMaxRuleNum = nMaxRuleNum;
        stuCfg.pstuSceneSnapShotWithRule = new Memory(nMaxRuleNum * stuRule[0].size());
        stuCfg.pstuSceneSnapShotWithRule.clear(nMaxRuleNum * stuRule[0].size());
        ToolKits.SetStructArrToPointerData(stuRule, stuCfg.pstuSceneSnapShotWithRule);

        // 获取
        stuCfg.write();
        if (!netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID,
                stuCfg.getPointer(), stuCfg.size(), 4000, null)) {
            System.err.println("获取场景抓拍配置失败!" + ToolKits.getErrorCode());
            return;
        }

        stuCfg.read();
        ToolKits.GetPointerDataToStructArr(stuCfg.pstuSceneSnapShotWithRule, stuRule);

        System.out.printf("stuRule[0]--PresetID:%d RuleNum:%d RuleType:%x SingleInterval:%d\n",
                stuRule[0].nPresetID, stuRule[0].nRetSnapShotRuleNum,
                stuRule[0].stuSnapShotWithRule[0].dwRuleType, stuRule[0].stuSnapShotWithRule[0].nSingleInterval[1]);

        // 设置，在获取的基础上设置
        IntByReference restart = new IntByReference(0);
        stuRule[0].stuSnapShotWithRule[0].nSingleInterval[1] = 15; // 抓图时间间隔
        ToolKits.SetStructArrToPointerData(stuRule, stuCfg.pstuSceneSnapShotWithRule);
        stuCfg.write();
        if (!netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID,
                stuCfg.getPointer(), stuCfg.size(), 4000, restart, null)) {
            System.err.println("设置场景抓拍配置失败!" + ToolKits.getErrorCode() + " \n errMsg: " + ENUMERROR.getErrorMessage());
        } else {
            System.err.println("设置场景抓拍配置成功!");
        }
    }


    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "远程抓图", "snapPicture"));
        menu.addItem(new CaseMenu.Item(this, "智能订阅", "realLoadPicture"));
        menu.addItem(new CaseMenu.Item(this, "停止智能订阅", "stopLoadPicture"));
        menu.run();
    }

    /**
     * Parameter for login
     */
    ////////////////////////////////////////////////////////////////
    private String m_strIp = "172.25.100.21";// "172.32.101.59"; //"10.11.16.168";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";

    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        TumbleDistanceDemo demo = new TumbleDistanceDemo();
        demo.InitTest();
        System.out.println("Run Test");
        demo.RunTest();
        demo.EndTest();
    }
}
