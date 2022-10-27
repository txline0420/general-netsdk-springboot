package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.DH_LOG_QUERY_TYPE;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DH_DEVICE_LOG_ITEM_EX;
import com.netsdk.lib.structure.NET_TIME;
import com.netsdk.lib.structure.QUERY_DEVICE_LOG_PARAM;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 设备系统日志查询Demo
 * @author 47081
 */
public class QueryDeviceLog {
    /**
     * sdk接口
     */
    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    /**
     * 登陆句柄
     */
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);
    /**
     * 设备信息扩展结构体
     */
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    /**
     * sdk初始化操作
     */
    public void initTest(){
        // 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }
    }

    /**
     * 登录设备
     */
    public void login(){
        // 以TCP方式登入
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
           loginOut();
        }
    }

    /**
     * 登出设备
     */
    public void loginOut(){
        System.out.println("End Test");
        if( loginHandle.longValue() != 0)
        {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }

    /**
     * 设备断线回调
     */
    private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

        private DisConnectCallBack() {
        }

        private static class CallBackHolder {
            private static final DisConnectCallBack INSTANCE = new DisConnectCallBack();
        }

        public static DisConnectCallBack getInstance() {
            return CallBackHolder.INSTANCE;
        }

        @Override
        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
        }
    }

    /**
     * 设备重连回调
     */
    private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
        private HaveReConnectCallBack() {
        }

        private static class CallBackHolder {
            private static final HaveReConnectCallBack INSTANCE = new HaveReConnectCallBack();
        }

        public static HaveReConnectCallBack getInstance() {
            return CallBackHolder.INSTANCE;
        }

        @Override
        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }
    }

    /**
     * 设备系统日志
     */
    public void queryLog(){

         //查询条件,作为入参
        QUERY_DEVICE_LOG_PARAM param=new QUERY_DEVICE_LOG_PARAM();
        //要查询的日志类型
        param.emLogType= DH_LOG_QUERY_TYPE.DHLOG_ALL.ordinal();

         //通道号
        param.nChannelID=0;
        //开始查询的条数
        param.nStartNum=31;
        //结束查询的条数,30-1+1=30,查询的条数是30条
        param.nEndNum=60;
        int logNum=param.nEndNum-param.nStartNum+1;
        //日志数据结构体类型,写0 c层可能校验不通过,建议写1，使用DH_DEVICE_LOG_ITEM_EX作为日志数据的结构体，
        // 因为c层对出参buffer长度的校验是以DH_DEVICE_LOG_ITEM_EX结构体长度来校验的
        // 而DH_DEVICE_LOG_ITEM_EX结构体的长度比DH_DEVICE_LOG_ITEM结构体要长得多
        param.nLogStuType=1;

        //要查询的起始时间段
        param.stuStartTime= new NET_TIME("2020/1/1/0/0/0");
        //要查询的结束时间段
        param.stuEndTime= new NET_TIME("2020/5/20/12/20/23");

        //入参
        Pointer queryParam=new Memory(param.size());
        ToolKits.SetStructDataToPointer(param,queryParam,0);
        //日志数据结构体
        DH_DEVICE_LOG_ITEM_EX logBuffer=new DH_DEVICE_LOG_ITEM_EX();
        //出参,分配内存
        Pointer pointer=new Memory(logBuffer.size()*logNum);
        pointer.clear(logBuffer.size()*logNum);
        //出参,查询到的日志条数
        IntByReference relogNum = new IntByReference(1);
        boolean bSet=netSdk.CLIENT_QueryDeviceLog(loginHandle,queryParam,pointer,logBuffer.size()*logNum,relogNum,3000);
        System.out.println("get system log is:"+bSet);
        if(bSet){
            System.out.println("返回的log 条数:"+relogNum.getValue());
            if(relogNum.getValue()>0){
                DH_DEVICE_LOG_ITEM_EX[] arrays=(DH_DEVICE_LOG_ITEM_EX[])new DH_DEVICE_LOG_ITEM_EX().toArray(relogNum.getValue());
                ToolKits.GetPointerDataToStructArr(pointer,arrays);
                for(DH_DEVICE_LOG_ITEM_EX item:arrays){
                    String time=item.getDate();
                    String operator=item.getOperator(Charset.forName("GBK"));
                    String operation=item.getOperation(Charset.forName("GBK"));
                    String log=item.getLog(Charset.forName("GBK"));
                    String detailLog=item.getDetailLog(Charset.forName("GBK"));
                    System.out.println(time+","+operator+","+operation+","+log+","+detailLog);
                }
            }
        }else{
            System.out.println("get log error: the error code is "+ToolKits.getErrorCode());
        }
    }

    /**
     * 控制台运行界面
     */
    public void runTest(){
        CaseMenu menu=new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "Login" , "login")));
        menu.addItem((new CaseMenu.Item(this , "QueryDeviceLog" , "queryLog")));
        menu.addItem(new CaseMenu.Item(this,"LoginOut","loginOut"));
        menu.run();
    }

    ////////////////////////////////////////////////////////////////
    private String m_strIp 				    = "172.23.12.14";
    private int   m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";
    ////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        final QueryDeviceLog queryLog=new QueryDeviceLog();
        queryLog.initTest();
        queryLog.runTest();
    }
}
