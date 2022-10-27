package com.netsdk.demo.customize.securityCheck;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.securityCheck.AnalyzerDataCallBack;
import com.netsdk.lib.callback.securityCheck.NotifyPopulationStatisticsInfoCallBack;
import com.netsdk.lib.structure.NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_IN_GET_POPULATION_STATISTICS;
import com.netsdk.lib.structure.NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_POPULATION_STATISTICS;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 291189
 * @version 1.0
 * @description 安全门功能demo
 * @date 2021/7/05
 */
public class SafetyCheckDemo {
    /**

     10.35.232.160 admin admin123

     */
    private String m_strIpAddr 				    = "10.35.232.160";
    private int    m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";


    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;


    // public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 订阅安检人数句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

    // 订阅安检人数句柄
    private static NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

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


    public void InitTest(){
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

        loginWithHighLevel();
    }
    /**
     * 高安全登录
     */
    public void loginWithHighLevel() {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = m_strIpAddr.getBytes();
                nPort = m_nPort;
                szUserName = m_strUser.getBytes();
                szPassword = m_strPassword.getBytes();
            }
        };

        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
        loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netSdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * 退出清理环境
     */
    public void LoginOut(){
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
        if (osPrefix.toLowerCase().startsWith("win64-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

   // CLIENT_AttachPopulationStatistics
    // 订阅安检门人数变化信息,pstInParam与pstOutParam内存由用户申请释放
    // CLIENT_NET_API LLONG CALL_METHOD CLIENT_AttachPopulationStatistics(LLONG lLoginID, NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstInParam, NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstOutParam , int nWaitTime);

    public void AttachPopulationStatistics(){
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        DetachPopulationStatistics();
        //入参
        NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO inParam=new NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO();

        Pointer user=new Memory(1024);
        inParam.dwUser=user;
        inParam.cbNotifyPopulationStatisticsInfo = NotifyPopulationStatisticsInfoCallBack.getInstance();

        Pointer pInParam=new Memory(inParam.size());

        ToolKits.SetStructDataToPointer(inParam, pInParam, 0);

        //出参
        NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO outParam=new NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO();

        Pointer pOutParam=new Memory(outParam.size());

        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
////CLIENT_NET_API LLONG CALL_METHOD CLIENT_AttachPopulationStatistics(LLONG lLoginID, NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstInParam, NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstOutParam , int nWaitTime);
        AttachHandle= netSdk.CLIENT_AttachPopulationStatistics(loginHandle,pInParam,pOutParam,3000);

        if (AttachHandle.longValue() != 0) {
            System.out.println("CLIENT_AttachPopulationStatistics Success");
        } else {
            System.out.println("CLIENT_AttachPopulationStatistics Failed!LastError = %s\n"+ToolKits.getErrorCode());
        }
    }

    // 取消订阅安检门人数变化信息
    public void DetachPopulationStatistics(){
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_DetachPopulationStatistics(AttachHandle);
        }

    }
 // 获取安检门人数统计信息
 //CLIENT_NET_API BOOL CALL_METHOD CLIENT_GetPopulationStatistics(LLONG lLoginID, const NET_IN_GET_POPULATION_STATISTICS *pInParam, NET_OUT_GET_POPULATION_STATISTICS *pOutParam, int nWaitTime);
 //public boolean CLIENT_GetPopulationStatistics(NetSDKLib.LLong lLoginID, Pointer pstInParam, Pointer pstOutParam, int nWaitTime);

   public void  GetPopulationStatistics(){
        //入参
       NET_IN_GET_POPULATION_STATISTICS inParam=new NET_IN_GET_POPULATION_STATISTICS();

       Pointer pInParam=new Memory(inParam.size());

       ToolKits.SetStructDataToPointer(inParam, pInParam, 0);

       //出参
       NET_OUT_GET_POPULATION_STATISTICS msg=new NET_OUT_GET_POPULATION_STATISTICS();

       Pointer pOutParam=new Memory(msg.size());

       ToolKits.SetStructDataToPointer(msg, pOutParam, 0);

       //CLIENT_NET_API BOOL CALL_METHOD CLIENT_GetPopulationStatistics(LLONG lLoginID, const NET_IN_GET_POPULATION_STATISTICS *pInParam, NET_OUT_GET_POPULATION_STATISTICS *pOutParam, int nWaitTime);

       boolean DoFind = netSdk.CLIENT_GetPopulationStatistics(loginHandle, pInParam, pOutParam, 3000);

       if(!DoFind){
           System.err.printf("Do Find PopulationStatistics.Error[%s]\n", ToolKits.getErrorCode());
           return;
       }

       ToolKits.GetPointerData(pOutParam, msg);

       int nPassPopulation = msg.nPassPopulation; // 正向通过人数

       System.out.println("正向通过人数:"+nPassPopulation);

       int nMetalAlarmPopulation = msg.nMetalAlarmPopulation;

       System.out.println("正向触发金属报警人数:"+nMetalAlarmPopulation);

       int nReversePassPopulation = msg.nReversePassPopulation;

       System.out.println("反向通过人数:"+nReversePassPopulation);

       int nReverseMetalAlarmPopulation = msg.nReverseMetalAlarmPopulation;

       System.out.println("反向触发金属报警人数:"+nReverseMetalAlarmPopulation);

       long nTempNormalPopulation = msg.nTempNormalPopulation;

       System.out.println("体温正常人数:"+nTempNormalPopulation);

       long nTempAlarmPopulation = msg.nTempAlarmPopulation;

       System.out.println("体温异常人数:"+nTempAlarmPopulation);

   }

    /**
     * 订阅智能分析数据
     */
    public void realLoadPicture() {
        int bNeedPicture = 1; // 是否需要图片
        int ChannelId = 0;   // -1代表全通道

        m_hAttachHandle =  netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_SECURITYGATE_PERSONALARM,
                bNeedPicture , AnalyzerDataCallBack.getInstance() , null , null);
        if(m_hAttachHandle.longValue() != 0) {
            System.out.println("智能订阅成功.");
        } else {
            System.err.println("智能订阅失败." + ToolKits.getErrorCode());
            return;
        }
    }

    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (m_hAttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(m_hAttachHandle);
        }
    }
    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "订阅安检门人数变化信息" , "AttachPopulationStatistics"));
        menu.addItem(new CaseMenu.Item(this , "取消订阅安检门人数变化信息" , "DetachPopulationStatistics"));
        menu.addItem((new CaseMenu.Item(this , "获取安检门人数统计信息" , "GetPopulationStatistics")));
        menu.addItem((new CaseMenu.Item(this , "订阅智能分析数据" , "realLoadPicture")));
        menu.addItem((new CaseMenu.Item(this , "取消实时智能分析数据" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static  void main(String[] args){
        SafetyCheckDemo safetyCheckDemo=new SafetyCheckDemo();
        safetyCheckDemo.InitTest();
        safetyCheckDemo.RunTest();
        safetyCheckDemo.LoginOut();

    }
}
