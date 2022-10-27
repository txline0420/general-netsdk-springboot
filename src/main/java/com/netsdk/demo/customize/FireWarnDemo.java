package com.netsdk.demo.customize;

import com.netsdk.demo.units.TimeUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LOG_SET_PRINT_INFO;
import com.netsdk.lib.NetSDKLib.NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO;
import com.netsdk.lib.NetSDKLib.NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.START_LISTEN_FINISH_RESULT_INFO;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.netsdk.lib.Utils.getOsPrefix;


/**
 * @description: 烟火报警及规则坐标信息  智能事件有图片（该事件在报警收尾各报一次)&&普通事件上报坐标
 * @author: 251589
 * @time: 2020/11/18 20:50
 */
public class FireWarnDemo {

    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    //登录句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 智能订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);


    /**
     * 通用服务
     */
    static class SDKGeneralService {

        // 网络断线回调
        // 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
        public static class DisConnect implements NetSDKLib.fDisConnect {

            private DisConnect() {
            }

            private static class CallBackHolder {
                private static final DisConnect cb = new DisConnect();
            }

            public static final DisConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
            }
        }

        // 网络连接恢复回调
        // 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
        public static class HaveReConnect implements NetSDKLib.fHaveReConnect {

            private HaveReConnect() {
            }

            private static class CallBackHolder {
                private static final HaveReConnect cb = new HaveReConnect();
            }

            public static final HaveReConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(NetSDKLib.LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
            }
        }

        /**
         * SDK初始化
         */
        public static void init() {
            // SDK资源初始化
            netSdk.CLIENT_Init(SDSnap.SDKGeneralService.DisConnect.getInstance(), null);

            // 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
            netSdk.CLIENT_SetAutoReconnect(SDSnap.SDKGeneralService.HaveReConnect.getInstance(), null);

            // 打开SDK日志（可选）
            LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
            setLog.bSetFilePath = 1;
            setLog.bSetPrintStrategy = 1;
            setLog.nPrintStrategy = 0;
            String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";

            System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

            boolean bLogopen = netSdk.CLIENT_LogOpen(setLog);
            if (!bLogopen)
                System.err.println("Failed to open NetSDK log !!!");

            netSdk.CLIENT_SetSnapRevCallBack(SDSnap.SnapCallback.getInstance(), null);
        }

        /**
         * SDK反初始化——释放资源
         */
        public static void cleanup() {
            netSdk.CLIENT_Cleanup();
        }
    }

    /**
     * 登录设备
     */
    public boolean login() {
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(ip, port, username,
                password, nSpecCap, pCapParam, deviceInfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d] Success!\n", ip, port);
        } else {
            System.out.printf("Login Device[%s] Port[%d] Failed. %s\n", ip, port, ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());
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
        SDSnap.SDKGeneralService.init(); // SDK初始化
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

    // 远程抓图
    public void snapPicture() {
        //设置抓图回调函数， 图片主要在m_SnapReceiveCB中返回
        netSdk.CLIENT_SetSnapRevCallBack(fSnapReceiveCB, null);

        // 发送抓图命令给前端设备，抓图的信息
        NetSDKLib.SNAP_PARAMS stuSnapParams = new NetSDKLib.SNAP_PARAMS();
        stuSnapParams.Channel = 1;  //抓图通道
        stuSnapParams.mode = 0;     //表示请求一帧
        stuSnapParams.Quality = 3;
        stuSnapParams.InterSnap = 5;
        stuSnapParams.CmdSerial = 100; // 请求序列号，有效值范围 0~65535，超过范围会被截断为

        IntByReference reserved = new IntByReference(0);

        if (netSdk.CLIENT_SnapPictureEx(loginHandle, stuSnapParams, reserved)) {
            System.out.println("CLIENT_SnapPictureEx success");
        } else {
            System.out.printf("CLIENT_SnapPictureEx Failed!Last Error[%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }

    private fSnapReceiveCB fSnapReceiveCB = new fSnapReceiveCB();

    public class fSnapReceiveCB implements NetSDKLib.fSnapRev {
        public void invoke(NetSDKLib.LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
            //pBuf收到的图片数据
            File currentPath = new File(".");
            String strFileName = currentPath.getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".jpg";

            byte[] buf = pBuf.getByteArray(0, RevLen);
            ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
            try {
                BufferedImage bufferedImage = ImageIO.read(byteArrInput);
                if (bufferedImage == null) {
                    return;
                }
                ImageIO.write(bufferedImage, "jpg", new File(strFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }

			//保存图片到本地文件
			ToolKits.savePicture(pBuf , RevLen , strFileName);
			System.out.println("strFileName : " + strFileName);
        }
    }

    /**
     * 远程抓图（异步）
     */
    public void snapPicture1() {
        // send capture picture instruction to device
        NetSDKLib.SNAP_PARAMS stuSnapParams = new NetSDKLib.SNAP_PARAMS();
        stuSnapParams.Channel = 0;            // channel
        stuSnapParams.mode = 0;               // capture picture mode
        stuSnapParams.Quality = 3;            // picture quality
        stuSnapParams.InterSnap = 0;
        stuSnapParams.CmdSerial = 0;

        IntByReference reserved = new IntByReference(0);
        if (netSdk.CLIENT_SnapPictureEx(loginHandle, stuSnapParams, reserved)) {
            System.out.println("CLIENT_SnapPictureEx success");
        } else {
            System.err.printf("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());

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

        // 回调需是单例
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
     * 智能訂閱事件 帶圖
     *
     * @return
     */
    public boolean realLoadPicture() {
        int bNeedPicture = 1;   // 是否需要图片
        int channel = 1; // 订阅全通道
        if (deviceInfo.byChanNum == 1)
            channel = 0;
        int emType = NetSDKLib.EVENT_IVS_FIREWARNING;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, emType, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Channel[%d] RealLoad Picture Succeed！\n", channel);
        } else {
            System.err.printf("Channel[%d] RealLoad Picture Failed！ errCode == %s\n errMsg == %s\n", channel, ToolKits.getErrorCode(), ENUMERROR.getErrorMessage());
        }
        return attachHandle.longValue() != 0;
    }

    /**
     * 订阅报警信息(普通事件)
     *
     * @return
     */
    public void startListen() {
        // 设置报警回调函数
        netSdk.CLIENT_SetDVRMessCallBack(FireWarningInfoCallBack.getSingleInstance(), null);
        // 订阅报警
        boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
        if (bRet) {
            System.out.println("订阅报警成功.");
        } else {
            System.err.printf("订阅报警失败! error:%d\n", netSdk.CLIENT_GetLastError() + ENUMERROR.getErrorMessage());
        }
    }

    /**
     * 退订报警信息(普通事件)
     *
     * @return
     */
    public void stopListen() {
        boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
        if (bRet) {
            System.out.println("取消订阅报警信息.");
        }
    }

    public static class FireWarningInfoCallBack implements NetSDKLib.fMessCallBackEx1 {
        private static FireWarningInfoCallBack singleInstance = new FireWarningInfoCallBack();

        public static FireWarningInfoCallBack getSingleInstance() {
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

        /**
         * @param lCommand
         * @param lLoginID
         * @param pStuEvent
         * @param dwBufLen
         * @param strDeviceIP
         * @param nDevicePort
         * @param bAlarmAckFlag
         * @param nEventID
         * @param dwUser
         * @return
         * @description 普通事件回調
         */
        @Override
        public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, int bAlarmAckFlag, NativeLong nEventID, Pointer dwUser) {
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_FIREWARNING_INFO: { // 0X31da 热成像火情报警信息上报(对应结构体 ALARM_FIREWARNING_INFO_DETAIL)
                    dealData(pStuEvent);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            /**
                             * 抓圖
                             */
                            int bNeedPicture = 1;   // 是否需要图片
                            int channel = -1; // 订阅全通道
                            int emType = NetSDKLib.EVENT_IVS_FIREWARNING;
                            AnalyzerDataCB analyzerDataCB = AnalyzerDataCB.getInstance();
                            NetSDKLib.LLong attachHandle = netSdk.CLIENT_RealLoadPictureEx(lLoginID, channel, emType, bNeedPicture, analyzerDataCB, null, null);
                            if (attachHandle.longValue() != 0) {
                                System.out.printf("Channel[%d] RealLoad Picture Succeed！\n", channel);
                            } else {
                                System.err.printf("Channel[%d] RealLoad Picture Failed！ errCode == %s\n errMsg == %s\n", channel, ToolKits.getErrorCode(), ENUMERROR.getErrorMessage());
                            }
                        }
                    }).start();
                    break;
                }
                case NetSDKLib.NET_ALARM_FIREWARNING: { // 0x31b5 热成像着火点事件 (对应结构体 ALARM_FIREWARNING_INFO)
                    dealData4FireWarning(pStuEvent);
                    break;
                }

                case NetSDKLib.NET_START_LISTEN_FINISH_EVENT: {// 订阅事件接口完成异步通知事件, 信息为 START_LISTEN_FINISH_RESULT_INFO
                    dealData4ListenFinish(pStuEvent);
                    break;
                }

                default: {
                    System.out.printf("其他事件 0x%x\n", lCommand);
                    break;
                }
            }
            return false;
        }

        // 热成像火情报警信息上报事件, 对应事件
        private  void dealData(Pointer pAlarmInfo) {
            NetSDKLib.ALARM_FIREWARNING_INFO_DETAIL msg = new NetSDKLib.ALARM_FIREWARNING_INFO_DETAIL();
            ToolKits.GetPointerData(pAlarmInfo, msg);
            for (int i = 0; i < msg.stuFireWarningInfo.length; i++) {
                System.out.println("msg 距离" + msg.stuFireWarningInfo[i].nDistance);
                System.out.println("msg GPS" + msg.stuFireWarningInfo[i].stuGpsPoint.dwLatidude + "," + msg.stuFireWarningInfo[i].stuGpsPoint.dwLongitude);
                System.out.println("msg 温度" + msg.stuFireWarningInfo[i].nTemperatureUnit);
                NetSDKLib.NET_FIREWARNING_INFO[]    stuFireWarningInfo = msg.stuFireWarningInfo;
                for (int j = 0; j < stuFireWarningInfo.length; j++) {
                    int nFSIDInner = stuFireWarningInfo[j].nFSID;
                    if (nFSID == nFSIDInner && nFSID != -1){
                        System.out.println("同一火情报警事件！");
                    }

                }
            }

        }

        // 订阅事件接口完成异步通知事件
        private  void dealData4ListenFinish(Pointer pAlarmInfo) {
            START_LISTEN_FINISH_RESULT_INFO msg = new START_LISTEN_FINISH_RESULT_INFO();
            ToolKits.GetPointerData(pAlarmInfo, msg);
            System.out.println("订阅事件接口完成异步通知事件完成" + msg.dwEventResult);
        }

        // 处理事件 坐標信息   热成像着火点报警
        private  void dealData4FireWarning(Pointer pAlarmInfo) {
            NetSDKLib.ALARM_FIREWARNING_INFO msg = new NetSDKLib.ALARM_FIREWARNING_INFO();
            ToolKits.GetPointerData(pAlarmInfo, msg);
            System.out.println("msg 距离" + msg.nDistance);
            System.out.println("msg GPS" + msg.stGpsPoint.dwLongitude + "," + msg.stGpsPoint.dwLatidude);
            System.out.println("msg 温度" + msg.fTemperature);

        }

    }

    private static int nFSID = -1;


    /**
     * 报警事件（智能）回调
     */
    public static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private NetSDKLib.DEV_EVENT_FIREWARNING_INFO detail;
        private File path;

        private AnalyzerDataCB() {
            detail = new NetSDKLib.DEV_EVENT_FIREWARNING_INFO();
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
                case NetSDKLib.EVENT_IVS_FIREWARNING: { // 火警事件(对应 DEV_EVENT_FIREWARNING_INFO)
                    dealData(pAlarmInfo);
                    break;
                }
                default: {
                    System.out.println("其他事件： " + dwAlarmType);
                    break;
                }
            }
            return 0;
        }

        // 处理事件
        private void dealData(Pointer pAlarmInfo) {
            ToolKits.GetPointerData(pAlarmInfo, detail);
            nFSID = detail.nFSID;
            System.out.println("火情信息所对应的事件ID" + nFSID);
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
        NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[] stuRule = new NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[nMaxRuleNum];
        for (int i = 0; i < nMaxRuleNum; ++i) {
            stuRule[i] = new NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
        }

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_SCENE_SNAP_SHOT_WITH_RULE2;    // 场景抓拍设置
        int nChannelID = 0;
        NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO stuCfg = new NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
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
        menu.addItem(new CaseMenu.Item(this, "开启订阅普通和智能事件", "attachAllEvent"));
        menu.addItem(new CaseMenu.Item(this, "开启退订普通和智能事件", "attachAllEvent"));
        menu.addItem(new CaseMenu.Item(this, "远程抓图", "snapPicture"));
        menu.run();
    }

    /**
     * 开启智能、普通事件订阅
     */
    public void attachAllEvent(){
        realLoadPicture(); // 根据产品定义，智能事件会在报警首尾各报一次 <-- 有图无坐标
        startListen(); // 该事件在报警发生中间隔上报  <-- 有坐标无图
    }

    /**
     * 退订智能、普通事件
     */
    public void deAttachAllEvent(){
        stopLoadPicture();
        stopListen();
    }


    /**
     * Parameter for login
     */
    ////////////////////////////////////////////////////////////////
    private String ip = "172.32.101.59"; //"10.11.16.168";
    private int port = 37777;
    private String username = "admin";
    private String password = "admin123";

    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        FireWarnDemo demo = new FireWarnDemo();
        demo.InitTest();
        System.out.println("Run Test");
        demo.RunTest();
        demo.EndTest();
    }
}


