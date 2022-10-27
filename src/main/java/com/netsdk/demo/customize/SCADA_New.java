package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import static com.netsdk.lib.Utils.getOsPrefix;

public class SCADA_New {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;  // 系统编码

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux")) {
            encode = "UTF-8";
        } else if (osPrefix.toLowerCase().startsWith("mac")) {
            encode = "UTF-8";
        }
    }

    // 设备信息
    private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();

    //登陆句柄
    private LLong m_hLoginHandle = new LLong(0);

    // 报警监听标识
    private boolean bListening = false;

    private LLong m_hAttachHandle = new LLong(0);

    private LLong m_hAlarmAttachHandle = new LLong(0);

    Vector<String> m_lstDeviceID = new Vector<String>();

    /**
     * 通用服务
     */
    static class SDKGeneralService {

        // 网络断线回调
        // 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
        public static class DisConnect implements fDisConnect {

            private DisConnect() {
            }

            private static class CallBackHolder {
                private static final DisConnect cb = new DisConnect();
            }

            public static final DisConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
            }
        }

        // 网络连接恢复回调
        // 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
        public static class HaveReConnect implements fHaveReConnect {

            private HaveReConnect() {
            }

            private static class CallBackHolder {
                private static final HaveReConnect cb = new HaveReConnect();
            }

            public static final HaveReConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
            }
        }

        /**
         * SDK初始化
         */
        public static void init() {
            // SDK资源初始化
            netsdkApi.CLIENT_Init(DisConnect.getInstance(), null);

            // 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
            netsdkApi.CLIENT_SetAutoReconnect(HaveReConnect.getInstance(), null);

            // 打开SDK日志（可选）
            LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();

            File path = new File(".");
            String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";

            setLog.bSetFilePath = 1;
            System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

            setLog.bSetPrintStrategy = 1;
            setLog.nPrintStrategy = 0;
            boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
            if (!bLogopen) {
                System.err.println("Failed to open NetSDK log !!!");
            }

            // 设置报警监听回调
            netsdkApi.CLIENT_SetDVRMessCallBack(MessCallback.getInstance(), null);
        }

        /**
         * SDK反初始化——释放资源
         */
        public static void cleanup() {
            netsdkApi.CLIENT_Cleanup();
            System.exit(0);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////设备登录//////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * 高安全设备登陆
     */
    public boolean loginDevice() {

        NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIp.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdkApi.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIp, m_nPort, ToolKits.getErrorCode());
        } else {
            deviceinfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Succeed");
            System.out.println("Device Address：" + m_strIp);
            System.out.println("设备包含：" + deviceinfo.byChanNum + "个通道");
        }

        return m_hLoginHandle.longValue() != 0;
    }

    /**
     * 登出设备
     */
    public void logoutDevice() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(m_hLoginHandle);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////订阅回调 (必须写成静态单例模式，回调是子线程)//////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * 普通报警监听回调
     */
    private static class MessCallback implements fMessCallBack {

        private MessCallback() {
        }

        private static class CallBackHolder {
            private static final MessCallback cb = new MessCallback();
        }

        public static MessCallback getInstance() {
            return CallBackHolder.cb;
        }

        @Override
        public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
                              int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
                              Pointer dwUser) {
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_SCADA_DEV_ALARM:  // 检测采集设备报警事件   "SCADADevAlarm"
                {
                    ALARM_SCADA_DEV_INFO msg = new ALARM_SCADA_DEV_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("[检测采集设备报警事件] nChannel :" + msg.nChannel + " nAction :" + msg.nAction +
                            " nAlarmFlag :" + msg.nAlarmFlag);
                    break;
                }
                default:
                    break;

            }

            return true;
        }
    }

    /**
     * 订阅监测点位信息回调
     */
    private static class SCADAAttachInfoCallBack implements fSCADAAttachInfoCallBack {
        private SCADAAttachInfoCallBack() {
        }

        private static class CallBackHolder {
            private static final SCADAAttachInfoCallBack cb = new SCADAAttachInfoCallBack();
        }

        public static final SCADAAttachInfoCallBack getInstance() {
            return CallBackHolder.cb;
        }

        @Override
        public void invoke(LLong lLoginID, LLong lAttachHandle,
                           NET_SCADA_NOTIFY_POINT_INFO_LIST pInfo, int nBufLen, Pointer dwUser) {
            System.out.println("————————————————————【订阅监测点位信息回调】————————————————————");
            for (int i = 0; i < pInfo.nList; i++) {
                System.out.println(" 设备名称:" + new String(pInfo.stuList[i].szDevName).trim());
                System.out.println(" 点位名(与点位表的取值一致):" + new String(pInfo.stuList[i].szPointName).trim());
                System.out.println(" 现场监控单元ID:" + new String(pInfo.stuList[i].szFSUID).trim());
                System.out.println(" 点位ID:" + new String(pInfo.stuList[i].szID).trim());
                System.out.println(" 探测器ID:" + new String(pInfo.stuList[i].szSensorID).trim());
                System.out.println(" 点位类型:" + pInfo.stuList[i].emPointType);
                System.out.println(" 采集时间 : " + pInfo.stuList[i].stuCollectTime.toStringTime());
            }
            System.out.println("————————————————————【订阅监测点位信息回调】————————————————————");

        }
    }

    /**
     * 订阅监测点报警信息回调
     */
    private static class SCADAAlarmAttachInfoCallBack implements fSCADAAlarmAttachInfoCallBack {
        private SCADAAlarmAttachInfoCallBack() {
        }

        private static class CallBackHolder {
            private static final SCADAAlarmAttachInfoCallBack cb = new SCADAAlarmAttachInfoCallBack();
        }

        public static final SCADAAlarmAttachInfoCallBack getInstance() {
            return CallBackHolder.cb;
        }

        @Override
        public void invoke(LLong lAttachHandle,
                           NET_SCADA_NOTIFY_POINT_ALARM_INFO_LIST pInfo, int nBufLen,
                           Pointer dwUser) {

            System.out.println("————————————————————【订阅监测点报警信息回调】————————————————————");
            for (int i = 0; i < pInfo.nList; i++) {
                System.out.println(" 设备ID:" + new String(pInfo.stuList[i].szDevID).trim());
                System.out.println(" 点位ID:" + new String(pInfo.stuList[i].szPointID).trim());
                System.out.println(" 报警描述:" + new String(pInfo.stuList[i].szAlarmDesc).trim());
                System.out.println(" 报警标志:" + (pInfo.stuList[i].bAlarmFlag == 1));
                System.out.println(" 报警时间:" + pInfo.stuList[i].stuAlarmTime.toStringTime());
                System.out.println(" 报警级别(0~6):" + pInfo.stuList[i].nAlarmLevel);
                System.out.println(" 报警编号(同一个告警的开始和结束的编号是相同的):" + pInfo.stuList[i].nSerialNo);
            }
            System.out.println("————————————————————【订阅监测点报警信息回调】————————————————————");
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////SCADA 功能////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取当前主机接入的外部设备ID
     */
    public boolean queryDeviceIdList() {
        m_lstDeviceID.clear(); // 清空外部设备ID列表

        int nCount = deviceinfo.byChanNum; // 设备的通道总数

        NET_SCADA_DEVICE_ID_INFO[] stuDeviceIDList = new NET_SCADA_DEVICE_ID_INFO[nCount];
        for (int i = 0; i < stuDeviceIDList.length; ++i) {
            stuDeviceIDList[i] = new NET_SCADA_DEVICE_ID_INFO();
        }

        NET_SCADA_DEVICE_LIST stuSCADADeviceInfo = new NET_SCADA_DEVICE_LIST();
        stuSCADADeviceInfo.nMax = nCount;
        int nSize = stuDeviceIDList[0].size() * nCount;
        stuSCADADeviceInfo.pstuDeviceIDInfo = new Memory(nSize);   // 监测设备信息
        stuSCADADeviceInfo.pstuDeviceIDInfo.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuDeviceIDList, stuSCADADeviceInfo.pstuDeviceIDInfo);

        if (queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_DEVICE_LIST, stuSCADADeviceInfo)) {
            System.err.println("获取当前主机接入的外部设备ID失败" + ToolKits.getErrorCode());  // 接口调用失败
            return false;
        }

        if (stuSCADADeviceInfo.nRet == 0) {
            System.out.println("当前主机接入的外部设备ID有效个数为0.");                        // 外部设备没有有效ID
            return false;
        }

        // 从 Pointer 提取数据
        ToolKits.GetPointerDataToStructArr(stuSCADADeviceInfo.pstuDeviceIDInfo, stuDeviceIDList);
        // 打印数据并更新设备ID
        System.out.println("获取当前主机接入的外部设备ID的有效个数：" + stuSCADADeviceInfo.nRet);
        for (int i = 0; i < stuSCADADeviceInfo.nRet; ++i) {
            String deviceID = "";
            try {
                System.out.printf("外部设备[%d] 设备id[%s] 设备名称[%s]\n", i,
                        new String(stuDeviceIDList[i].szDeviceID, encode).trim(),
                        new String(stuDeviceIDList[i].szDevName, encode).trim());
                deviceID = new String(stuDeviceIDList[i].szDeviceID, encode).trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            m_lstDeviceID.add(deviceID);  // 更新外部设备ID列表
        }
        return true;
    }

    /**
     * 通过设备ID获取监测点位信息
     */
    public void querySpotInfo(byte[] bDeviceId, NET_SCADA_POINT_BY_ID_INFO[] stuPointList) {

        int nCount = 20; // TODO 根据实际所需修改 33010011837001 支持 10个
        if (stuPointList != null) {
            nCount = stuPointList.length;
        } else {
            stuPointList = new NET_SCADA_POINT_BY_ID_INFO[nCount];
            for (int i = 0; i < stuPointList.length; ++i) {
                stuPointList[i] = new NET_SCADA_POINT_BY_ID_INFO();
            }
        }
        // 入参
        NET_SCADA_INFO_BY_ID stuSCADAInfo = new NET_SCADA_INFO_BY_ID();
        System.arraycopy(bDeviceId, 0, stuSCADAInfo.szSensorID, 0, bDeviceId.length);
        stuSCADAInfo.nMaxCount = nCount;
        int nSize = stuPointList[0].size() * nCount;
        stuSCADAInfo.pstuInfo = new Memory(nSize);   // 监测设备信息
        stuSCADAInfo.pstuInfo.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuPointList, stuSCADAInfo.pstuInfo);
        if (queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_INFO_BY_ID, stuSCADAInfo)) {
            System.err.println("通过设备ID获取监测点位信息失败" + ToolKits.getErrorCode());
            return;
        }

        if (stuSCADAInfo.nRetCount == 0) {   // 异常，获取不到信息
            System.out.println("通过设备ID获取监测点位信息返回个数为0.");
            return;
        }
        // 获取数据
        ToolKits.GetPointerDataToStructArr(stuSCADAInfo.pstuInfo, stuPointList);
        System.out.println("通过设备ID获取监测点位信息返回个数：" + stuSCADAInfo.nRetCount);
        int nPointCount = Math.min(stuSCADAInfo.nMaxCount, stuSCADAInfo.nRetCount);
        for (int i = 0; i < nPointCount; ++i) {
            System.out.printf("点位类型[%d] 监测点位ID[%s] 数据状态[%d]\n",
                    stuPointList[i].emType, new String(stuPointList[i].szID).trim(), stuPointList[i].nStatus);
        }

    }

    /**
     * 查询设备状态
     */
    public boolean queryDevState(int nType, SdkStructure stuInfo) {

        IntByReference intRetLen = new IntByReference();
        stuInfo.write();
        if (!netsdkApi.CLIENT_QueryDevState(m_hLoginHandle, nType, stuInfo.getPointer(), stuInfo.size(), intRetLen, 3000)) {
            return true;
        }
        stuInfo.read();
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////SCADA Demo 用例///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * 查询当前主机接入的外部设备ID
     */
    public void case_scada_getdevicelist() {
        // 注意：获取SCADA能力集 获取点位表路径信息 按照监测点位类型获取监测点位信息 这三个功能设备现在不支持

        System.out.println("——————————————————获取当前主机接入的外部设备ID——————————————————");
        queryDeviceIdList();
    }

    /**
     * 获取阈值
     * 用例: 查询第一个ID设备的信息点阈值
     */
    public void case_scada_getThreshold() {

        System.out.println("——————————————————获取第一个ID外部设备的阈值——————————————————");

        if (m_lstDeviceID.isEmpty() && !queryDeviceIdList()) {
            return;
        }

        String deviceId = m_lstDeviceID.get(0);        // Demo 里取第一个设备的 ID
        int nPointCount = 20;                          // 点位阈值信息最大个数，假设是20个
        NET_SCADA_ID_THRESHOLD_INFO[] stuThresholdList = new NET_SCADA_ID_THRESHOLD_INFO[nPointCount];
        for (int i = 0; i < stuThresholdList.length; ++i) {
            stuThresholdList[i] = new NET_SCADA_ID_THRESHOLD_INFO();
        }

        NET_IN_SCADA_GET_THRESHOLD stuGetIn = new NET_IN_SCADA_GET_THRESHOLD();
        System.arraycopy(deviceId.getBytes(), 0, stuGetIn.szDeviceID, 0, deviceId.getBytes().length);
        NET_OUT_SCADA_GET_THRESHOLD stuGetOut = new NET_OUT_SCADA_GET_THRESHOLD();
        stuGetOut.nMax = nPointCount;
        int nSize = stuThresholdList[0].size() * nPointCount;
        stuGetOut.pstuThresholdInfo = new Memory(nSize);
        stuGetOut.pstuThresholdInfo.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuThresholdList, stuGetOut.pstuThresholdInfo);
        if (!netsdkApi.CLIENT_SCADAGetThreshold(m_hLoginHandle, stuGetIn, stuGetOut, 3000)) {
            System.err.println("获取阈值失败" + ToolKits.getErrorCode());
        }
        // 读取数据
        ToolKits.GetPointerDataToStructArr(stuGetOut.pstuThresholdInfo, stuThresholdList);
        // 打印展示
        System.out.println("通过设备获取监测点位信息返回个数：" + stuGetOut.nRet);
        int nRetCount = Math.min(stuGetOut.nMax, stuGetOut.nRet);
        for (int i = 0; i < nRetCount; ++i) {
            try {
                System.out.printf("点位类型[%d] 监测点位ID[%s] 告警门限[%f] 绝对阈值[%f] 相对阈值[%f] 数据状态[%d]\n",
                        stuThresholdList[i].emPointType, new String(stuThresholdList[i].szID, encode).trim(),
                        stuThresholdList[i].fThreshold, stuThresholdList[i].fAbsoluteValue, stuThresholdList[i].fRelativeValue,
                        stuThresholdList[i].nStatus);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置阈值
     * 用例: 查询第一个ID设备的信息点阈值, 然后原封不动下发
     */
    public void case_scada_setThreshold() {

        System.out.println("——————————————————获取并设置第一个ID外部设备的阈值——————————————————");

        if (m_lstDeviceID.isEmpty() && !queryDeviceIdList()) {
            return;
        }

        //////////////////////////////////// 获取下发数据 /////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////

        String deviceId = m_lstDeviceID.get(0);        // Demo 里取第一个设备的 ID
        int nPointCount = 20;                          // 点位阈值信息最大个数，假设是20个
        NET_SCADA_ID_THRESHOLD_INFO[] stuThresholdList = new NET_SCADA_ID_THRESHOLD_INFO[nPointCount];
        for (int i = 0; i < stuThresholdList.length; ++i) {
            stuThresholdList[i] = new NET_SCADA_ID_THRESHOLD_INFO();
        }

        NET_IN_SCADA_GET_THRESHOLD stuGetIn = new NET_IN_SCADA_GET_THRESHOLD();
        System.arraycopy(deviceId.getBytes(), 0, stuGetIn.szDeviceID, 0, deviceId.getBytes().length);
        NET_OUT_SCADA_GET_THRESHOLD stuGetOut = new NET_OUT_SCADA_GET_THRESHOLD();
        stuGetOut.nMax = nPointCount;
        int nSize = stuThresholdList[0].size() * nPointCount;
        stuGetOut.pstuThresholdInfo = new Memory(nSize);
        stuGetOut.pstuThresholdInfo.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuThresholdList, stuGetOut.pstuThresholdInfo);
        if (!netsdkApi.CLIENT_SCADAGetThreshold(m_hLoginHandle, stuGetIn, stuGetOut, 3000)) {
            System.err.println("获取阈值失败" + ToolKits.getErrorCode());
        }
        // 读取数据到 stuThresholdList
        ToolKits.GetPointerDataToStructArr(stuGetOut.pstuThresholdInfo, stuThresholdList);
        int nRetCount = Math.min(stuGetOut.nMax, stuGetOut.nRet);

        //////////////////////////////// 原封不动下发数据 /////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////

        // 现在 stuThresholdList 中共有 nRetCount 个有效数据
        NET_IN_SCADA_SET_THRESHOLD stuSetIn = new NET_IN_SCADA_SET_THRESHOLD();
        System.arraycopy(deviceId.getBytes(), 0, stuSetIn.szDeviceID, 0, deviceId.getBytes().length);
        stuSetIn.nMax = nRetCount;
        nSize = stuThresholdList[0].size() * nRetCount;
        stuSetIn.pstuThresholdInfo = new Memory(nSize);
        stuSetIn.pstuThresholdInfo.clear(nSize);

        // Todo 如果要修改，请自行在 stuThresholdList 内修改

        long offset = 0;
        for (int i = 0; i < nRetCount; ++i) {
            ToolKits.SetStructDataToPointer(stuThresholdList[i], stuSetIn.pstuThresholdInfo, offset);
            offset += stuThresholdList[i].size();
        }

        NET_OUT_SCADA_SET_THRESHOLD stuSetOut = new NET_OUT_SCADA_SET_THRESHOLD();
        if (!netsdkApi.CLIENT_SCADASetThreshold(m_hLoginHandle, stuSetIn, stuSetOut, 3000)) {
            System.err.println("设置阈值失败" + ToolKits.getErrorCode());
            return;
        }

        System.out.printf(" 有效的存放设置阈值成功的id个数[%d] 设置阈值失败的id个数[%d]\n", stuSetOut.nSuccess, stuSetOut.nFail);
    }

    /**
     * 查询监控点的实时值
     * 用例: 查询第一个ID设备 监控点的实时值
     */
    public void case_scada_get() {
        System.out.println("——————————————————获取第一个ID外部设备的实时监控值——————————————————");

        if (m_lstDeviceID.isEmpty() && !queryDeviceIdList()) {
            return;
        }

        try {
            byte[] deviceId = m_lstDeviceID.get(0).getBytes(encode);  // Demo 里取第一个设备的 ID
            int nMaxCount = 20;                                       // 监控值最大个数，假设是20个，33010011837001 支持10个
            NET_SCADA_POINT_BY_ID_INFO[] stuThresholdList = new NET_SCADA_POINT_BY_ID_INFO[nMaxCount];
            for (int i = 0; i < stuThresholdList.length; ++i) {
                stuThresholdList[i] = new NET_SCADA_POINT_BY_ID_INFO();
            }
            querySpotInfo(deviceId, stuThresholdList);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 订阅报警信息
     */
    public boolean case_startListen() {
        if (bListening) {
            return true;
        }

        // 订阅报警 （报警回调函数已经在在initSdk中设置）
        bListening = netsdkApi.CLIENT_StartListenEx(m_hLoginHandle);
        if (!bListening) {
            System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
        } else {
            System.out.println("订阅报警成功.");
        }
        return bListening;
    }

    /**
     * 取消订阅报警信息
     */
    public void case_stopListen() {
        if (bListening) {
            System.out.println("取消订阅报警信息.");
            netsdkApi.CLIENT_StopListen(m_hLoginHandle);
            bListening = false;
        }
    }

    /**
     * 订阅监测点位报警信息
     */
    public void case_scada_alarm_attach() {
        // 入参
        NET_IN_SCADA_ALARM_ATTACH_INFO stIn = new NET_IN_SCADA_ALARM_ATTACH_INFO();
        stIn.cbCallBack = SCADAAlarmAttachInfoCallBack.getInstance();

        // 出参
        NET_OUT_SCADA_ALARM_ATTACH_INFO stOut = new NET_OUT_SCADA_ALARM_ATTACH_INFO();

        m_hAlarmAttachHandle = netsdkApi.CLIENT_SCADAAlarmAttachInfo(m_hLoginHandle, stIn, stOut, 3000);
        if (m_hAlarmAttachHandle.longValue() != 0) {
            System.out.println("订阅监测点位报警信息成功！");
        } else {
            System.err.println("订阅监测点位报警信息失败！" + ToolKits.getErrorCode());
        }
    }

    /**
     * 取消订阅监测点位报警信息
     */
    public void case_scada_alarm_detach() {
        if (m_hAlarmAttachHandle.longValue() != 0) {
            netsdkApi.CLIENT_SCADAAlarmDetachInfo(m_hAlarmAttachHandle);
            m_hAlarmAttachHandle.setValue(0);
        }
    }

    /**
     * 订阅监测点位实时数据
     */
    public void case_scada_real_attach() {
        // 入参
        NET_IN_SCADA_ATTACH_INFO stIn = new NET_IN_SCADA_ATTACH_INFO();
        stIn.cbCallBack = SCADAAttachInfoCallBack.getInstance();
        stIn.emPointType = EM_NET_SCADA_POINT_TYPE.EM_NET_SCADA_POINT_TYPE_ALL;

        // 出参
        NET_OUT_SCADA_ATTACH_INFO stOut = new NET_OUT_SCADA_ATTACH_INFO();

        m_hAttachHandle = netsdkApi.CLIENT_SCADAAttachInfo(m_hLoginHandle, stIn, stOut, 3000);
        if (0 != m_hAttachHandle.longValue()) {
            System.out.println("订阅监测点位实时数据信息成功!");
        } else {
            System.err.println("订阅监测点位实时数据信息失败" + ToolKits.getErrorCode());
        }
    }

    /**
     * 取消监测点位实时数据
     */
    public void case_scada_real_detach() {
        if (m_hAttachHandle.longValue() != 0) {
            netsdkApi.CLIENT_SCADADetachInfo(m_hAttachHandle);
            m_hAttachHandle.setValue(0);

        }
    }

    /**
     * 查询历史数据
     * 注意：查询的数据量测试发现是有上限，建议区间设置 1 小时
     * 用例：查询第一个ID外设的第一个监测点位 的历史数据(2020/10/14/11:0:0 - 2020/10/14/12:0:0)
     */
    public void case_scada_find_history() {
        if (m_lstDeviceID.isEmpty() && !queryDeviceIdList()) {
            return;
        }

        byte[] bDeviceId = new byte[0];
        byte[] bID = new byte[0];
        try {
            bDeviceId = m_lstDeviceID.get(0).getBytes(encode);        // 第一个ID外设
            NET_SCADA_POINT_BY_ID_INFO[] stuPointList = new NET_SCADA_POINT_BY_ID_INFO[10];
            for (int i = 0; i < stuPointList.length; ++i) {
                stuPointList[i] = new NET_SCADA_POINT_BY_ID_INFO();
            }
            // (假设查询成功)
            querySpotInfo(bDeviceId, stuPointList);
            bID = new String(stuPointList[0].szID, encode).trim().getBytes(encode);   // 第一个监测点位
            if (bID.length > 32) return; // 长度超过32无效
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        NET_TIME startTime = new NET_TIME();
        startTime.setTime(2020, 11, 14, 11, 0, 0);

        NET_TIME endTime = new NET_TIME();
        endTime.setTime(2020, 11, 14, 12, 0, 0);

        ///////////////////////////// 开始查询 ////////////////////////////////////
        NET_IN_SCADA_START_FIND stuInFind = new NET_IN_SCADA_START_FIND();
        stuInFind.stuStartTime = startTime;    // 开始时间
        stuInFind.bEndTime = 1;                // 限制结束时间
        stuInFind.stuEndTime = endTime;        // 结束时间

        System.arraycopy(bDeviceId, 0, stuInFind.szDeviceID, 0, bDeviceId.length);     // DeviceID
        System.arraycopy(bID, 0, stuInFind.szID, 0, bID.length);                       // 监测点位ID

        NET_OUT_SCADA_START_FIND stuOutFind = new NET_OUT_SCADA_START_FIND();

        LLong lFindHandle = netsdkApi.CLIENT_StartFindSCADA(m_hLoginHandle, stuInFind, stuOutFind, 5000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("开始查询SCADA点位历史数据失败！" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("符合查询条件的总数:" + stuOutFind.dwTotalCount);

        if (stuOutFind.dwTotalCount == 0) {   // 没有查到任何数据
            System.err.println("没有查询到任何SCADA点位历史数据");
            netsdkApi.CLIENT_StopFindSCADA(lFindHandle);
            return;
        }

        int nFindCount = 50;   // 顺序从设备端取回数据，这个值请根据自己的带宽适当填写

        NET_IN_SCADA_DO_FIND stInDoFind = new NET_IN_SCADA_DO_FIND();
        stInDoFind.nStartNo = 0;
        stInDoFind.nCount = nFindCount;

        NET_OUT_SCADA_DO_FIND stOutDoFind = new NET_OUT_SCADA_DO_FIND();
        stOutDoFind.nMaxNum = nFindCount;
        int nSize = new NET_SCADA_POINT_BY_ID_INFO().size() * nFindCount;
        stOutDoFind.pstuInfo = new Memory(nSize);
        while (true) {

            NET_SCADA_POINT_BY_ID_INFO[] stuFindResult = new NET_SCADA_POINT_BY_ID_INFO[nFindCount];
            for (int i = 0; i < stuFindResult.length; i++) {
                stuFindResult[i] = new NET_SCADA_POINT_BY_ID_INFO();
            }
            stOutDoFind.pstuInfo.clear(nSize);
            ToolKits.SetStructArrToPointerData(stuFindResult, stOutDoFind.pstuInfo);

            if (stInDoFind.nStartNo + nFindCount >= stuOutFind.dwTotalCount) {  // 最后一批数据
                stInDoFind.nCount = stuOutFind.dwTotalCount - (stInDoFind.nStartNo);
            }

            if (!netsdkApi.CLIENT_DoFindSCADA(lFindHandle, stInDoFind, stOutDoFind, 5000)) {
                System.err.println("获取SCADA点位历史数据失败！" + ToolKits.getErrorCode());
                break;
            }
            if (stOutDoFind.nRetNum <= 0) {
                System.err.println("获取SCADA点位历史数据个数:" + stOutDoFind.nRetNum);
                break;
            }

            ToolKits.GetPointerDataToStructArr(stOutDoFind.pstuInfo, stuFindResult);
            for (int i = 0; i < stOutDoFind.nRetNum; ++i) {
                System.out.printf((stInDoFind.nStartNo + i + 1) + " 点位类型[%d] 监测点位ID[%s] 数据状态[%d] 记录时间[%s]\n",
                        stuFindResult[i].emType, new String(stuFindResult[i].szID).trim(),
                        stuFindResult[i].nStatus, stuFindResult[i].stuTime.toStringTimeEx());
            }

            stInDoFind.nStartNo += stOutDoFind.nRetNum;

            if (stInDoFind.nStartNo >= stuOutFind.dwTotalCount) {
                break;
            }

        }

        // 结束查询
        netsdkApi.CLIENT_StopFindSCADA(lFindHandle);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////// 简易控制台 ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 一键订阅
     */
    public void attach() {
        case_scada_real_attach();            // 订阅实时数据
        case_startListen();                  // 订阅报警事件
        case_scada_alarm_attach();           // 订阅报警数据
    }

    /**
     * 一键取消
     */
    public void detach() {
        case_scada_real_detach();        // 取消订阅实时数据
        case_stopListen();                    // 取消订阅报警事件
        case_scada_alarm_detach();       // 取消订阅报警数据
    }

    public void InitTest() {

        SDKGeneralService.init(); // SDK初始化
        if (!loginDevice()) { // 登陆设备
            EndTest();
        }
    }

    public void EndTest() {
        detach();    // 取消订阅
        logoutDevice(); //	登出设备
        System.out.println("See You...");
        SDKGeneralService.cleanup(); // 反初始化
        System.exit(0);
    }

    /**
     * case: SCADA.getDeviceList, 获取当前接入外部设备ID  {@link SCADA_New#case_scada_getdevicelist}
     * case: SCADA.getThreshold,  获取阈值              {@link SCADA_New#case_scada_getThreshold}
     * case: SCADA.SetThreshold,  设置阈值              {@link SCADA_New#case_scada_setThreshold}
     * case: SCADA.get            获取监控点的实时值     {@link SCADA_New#case_scada_get}
     * case: startListen          订阅普通报警          {@link SCADA_New#case_startListen}
     * case: stopListen           取消普通报警          {@link SCADA_New#case_stopListen}
     * case: SCADA.alarmAttach    订阅监测点位报警信息   {@link SCADA_New#case_scada_alarm_attach}
     * case: SCADA.alarmDetach    取消监测点位报警信息   {@link SCADA_New#case_scada_alarm_detach}
     * case: SCADA.attach         订阅监测点位实时信息   {@link SCADA_New#case_scada_real_attach}
     * case: SCADA.detach         取消监测点位实时信息   {@link SCADA_New#case_scada_real_detach}
     * case: SCADA.startFind.     查询历史数据          {@link SCADA_New#case_scada_find_history}
     */
    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();

        menu.addItem(new CaseMenu.Item(this, "case: SCADA.getDeviceList, 获取当前接入外部设备ID", "case_scada_getdevicelist"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.getThreshold,  获取阈值", "case_scada_getThreshold"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.setThreshold,  设置阈值", "case_scada_setThreshold"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.get            获取监控点的实时值", "case_scada_get"));
        menu.addItem(new CaseMenu.Item(this, "case: startListen          订阅普通报警", "case_startListen"));
        menu.addItem(new CaseMenu.Item(this, "case: stopListen           取消普通报警", "case_stopListen"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.alarmAttach    订阅监测点位报警信息", "case_scada_alarm_attach"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.alarmDetach    取消监测点位报警信息", "case_scada_alarm_detach"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.attach         订阅监测点位实时信息", "case_scada_real_attach"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.detach         取消监测点位实时信息", "case_scada_real_detach"));
        menu.addItem(new CaseMenu.Item(this, "case: SCADA.startFind.     查询历史数据", "case_scada_find_history"));
        menu.run();
    }

    ////////////////////////////////////////////////////////////////
    private final String m_strIp = "172.3.3.174";
    private final int m_nPort = 37777;
    private final String m_strUser = "admin";
    private final String m_strPassword = "admin123";
    ////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        SCADA_New demo = new SCADA_New();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
