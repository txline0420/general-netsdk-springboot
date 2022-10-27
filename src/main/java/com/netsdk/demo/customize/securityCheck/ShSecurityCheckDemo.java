package com.netsdk.demo.customize.securityCheck;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.securityCheck.SecurityGateAttachAlarmStatisticsCallBack;
import com.netsdk.lib.structure.NET_IN_SECURITYGATE_ATTACH_ALARM_STATISTICS;
import com.netsdk.lib.structure.NET_OUT_SECURITYGATE_ATTACH_ALARM_STATISTICS;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;
/**
 * @author 291189
 * @version 1.0
 * @description 安全门报警订阅和统计信息
 * @date 2021/6/29
 */
public class ShSecurityCheckDemo {
    /**

     10.35.232.160 安检门环境
     admin  admin123

     安检机 10.35.233.144
     admin  admin123

     */

    // 配置登陆地址，端口，用户名，密码

    private String m_strIp 				    = "10.35.232.160";
    private int    m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";

    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;


   // public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 订阅句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

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

        Login();
    }
    public void Login(){

        // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
    }

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


    /**
     * 订阅安全门报警统计信息接口
     */
    public void attachDadarAlarmInfo() {

        //入参
        NET_IN_SECURITYGATE_ATTACH_ALARM_STATISTICS net_in_securitygate_attach_alarm_statistics=new NET_IN_SECURITYGATE_ATTACH_ALARM_STATISTICS();

        UUID uuid= UUID.randomUUID();
        byte[] szUUID = net_in_securitygate_attach_alarm_statistics.szUUID;

        String numbers =uuid.toString();
        //beda31a3-a7b5-4126-81b9-6e67402b7b0c?
        int i = numbers.lastIndexOf("-");

        String[] split = numbers.split("-");
        //UUID算法(8-4-4-16格式)

        numbers=split[0]+"-"+split[1]+""+split[2]+"-"+split[3]+split[4];

        System.arraycopy(numbers.getBytes(), 0, szUUID, 0, numbers.getBytes().length);

        net_in_securitygate_attach_alarm_statistics.szUUID=szUUID;

        Pointer user=new Memory(1024);
        net_in_securitygate_attach_alarm_statistics.dwUser=user;
        net_in_securitygate_attach_alarm_statistics.cbNotify= SecurityGateAttachAlarmStatisticsCallBack.getInstance();
        Pointer pInParam=new Memory(net_in_securitygate_attach_alarm_statistics.size());
        ToolKits.SetStructDataToPointer(net_in_securitygate_attach_alarm_statistics, pInParam, 0);

        //出参

        NET_OUT_SECURITYGATE_ATTACH_ALARM_STATISTICS net_out_securitygate_attach_alarm_statistics=
                new NET_OUT_SECURITYGATE_ATTACH_ALARM_STATISTICS();

        Pointer pOutParam=new Memory(net_out_securitygate_attach_alarm_statistics.size());
        ToolKits.SetStructDataToPointer(net_out_securitygate_attach_alarm_statistics, pOutParam, 0);

        AttachHandle
                = netSdk.CLIENT_SecurityGateAttachAlarmStatistics(loginHandle, pInParam,
                pOutParam, 5000);

        if(AttachHandle.longValue() == 0) {
            System.out.printf("SecurityGateAttachAlarmStatistics fail, ErrCode=%x\n", netSdk.CLIENT_GetLastError() );
        }else {
            System.out.println("SecurityGateAttachAlarmStatistics success");
        }
    }



    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "attachDadarAlarmInfo" , "attachDadarAlarmInfo"));
        menu.run();
    }




    public static  void main(String[] args){
        ShSecurityCheckDemo shSecurityCheckDemo=new ShSecurityCheckDemo();
        shSecurityCheckDemo.InitTest();
        shSecurityCheckDemo.RunTest();
        shSecurityCheckDemo.LoginOut();

    }



}
