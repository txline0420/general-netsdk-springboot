package com.netsdk.demo.customize;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

/**
 * 登录,拉流测试
 */
public class MacRealPlay {
    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 登陆句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 监视预览句柄
    private static NetSDKLib.LLong lRealHandle = new NetSDKLib.LLong(0);

    // 播放通道号
    private static int g_lRealPort = 0;
    public MacRealPlay(NetSDKLib.LLong loginHandle)
    {
        this.loginHandle = loginHandle;
    }
    //Pointer hWnd = Native.getComponentPointer(w);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    //private NET_TIME m_startTime = new NET_TIME(); // 开始时间
    //private NET_TIME m_stopTime = new NET_TIME(); // 结束时间

    public void InitTest(){
        // 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        //设置子链接重连
        netSdk.CLIENT_SetSubconnCallBack(CBfSubDisConnect.getInstance(),null);

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

    public void RealPlay(){
                NetSDKLib.LLong realPlayHandler=netSdk.CLIENT_StartRealPlay(loginHandle,0,null,0,RealDataCallBackEx.RealDataCallBackExHolder.instance,RealPlayDisConnect.RealPlayDisConnectHolder.instance,null,5000);
                if(realPlayHandler.intValue()==0){
                    System.out.println("realplay failed.the error is "+ToolKits.getErrorCode());
                    return;
                }else{
                    System.out.println("realplay success.");
                }
    }
    /**
     * 实时监视数据回调函数
     */
    private static class RealDataCallBackEx implements NetSDKLib.fRealDataCallBackEx{
        private static class RealDataCallBackExHolder{
            private static RealDataCallBackEx instance=new RealDataCallBackEx();
        }
        private RealDataCallBackEx getInstance(){
            return RealDataCallBackExHolder.instance;
        }
        @Override
        public void invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {
            System.out.println("回调data type: "+dwDataType+",buffer size"+dwBufSize);
        }
    }

    /**
     * fRealPlayDisConnect
     * 视频监视断开回调函数
     */
    private static class RealPlayDisConnect implements NetSDKLib.fRealPlayDisConnect{
        private static class RealPlayDisConnectHolder{
            private static RealPlayDisConnect instance=new RealPlayDisConnect();
        }
        private RealPlayDisConnect getInstance(){
            return RealPlayDisConnectHolder.instance;
        }
        @Override
        public void invoke(NetSDKLib.LLong lOperateHandle, int dwEventType, Pointer param, Pointer dwUser) {
            System.out.println("RealPlayDisConnect:"+lOperateHandle+",event type: "+dwEventType+"pointer is: "+param);
        }
    }

    public void LoginOut(){
        System.out.println("End Test");
        netSdk.CLIENT_StopRealPlayEx(lRealHandle);
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
            private static DisConnectCallBack instance = new DisConnectCallBack();
        }

        public static DisConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

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
            private static HaveReConnectCallBack instance = new HaveReConnectCallBack();
        }

        public static HaveReConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        @Override
        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }
    }



    /**
     * 动态子连接断开回调函数
     */
    private static class CBfSubDisConnect implements NetSDKLib.fSubDisConnect {
        private CBfSubDisConnect() {
        }

        private static class CallBackHolder {
            private static CBfSubDisConnect instance = new CBfSubDisConnect();
        }

        public static CBfSubDisConnect getInstance() {
            return CallBackHolder.instance;
        }

        @Override
        public void invoke(int emInterfaceType, Boolean bOnline,
                           NetSDKLib.LLong lOperateHandle, NetSDKLib.LLong lLoginID, Pointer dwUser) {
            // TODO Auto-generated method stub
            switch(emInterfaceType)
            {
                case NetSDKLib.EM_INTERFACE_TYPE.DH_INTERFACE_REALPLAY:
                    System.out.printf("实时监视接口: Short connect is %d\n", bOnline);
                    break;
                case NetSDKLib.EM_INTERFACE_TYPE.DH_INTERFACE_PREVIEW:
                    System.out.printf("多画面预览接口: Short connect is %d\n", bOnline);
                    break;
                case NetSDKLib.EM_INTERFACE_TYPE.DH_INTERFACE_PLAYBACK:
                    System.out.printf("回放接口: Short connect is %d\n", bOnline);
                    break;
                case NetSDKLib.EM_INTERFACE_TYPE.DH_INTERFACE_DOWNLOAD:
                    System.out.printf("下载接口: Short connect is %d\n", bOnline);
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * 实时监视数据回调函数--扩展(pBuffer内存由SDK内部申请释放)
     */
    private static class CbfRealDataCallBackEx implements NetSDKLib.fRealDataCallBackEx {
        private CbfRealDataCallBackEx() {
        }

        private static class CallBackHolder {
            private static CbfRealDataCallBackEx instance = new CbfRealDataCallBackEx();
        }

        public static CbfRealDataCallBackEx getInstance() {
            return CallBackHolder.instance;
        }

        @Override
        public void invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer,
                           int dwBufSize, int param, Pointer dwUser) {
            int bInput=0;
            if(0 != lRealHandle.longValue())
            {
                System.out.println("the data type is: "+dwDataType+",buffer size is: "+dwBufSize+",buffer is: "+pBuffer.getByteBuffer(0,dwBufSize).toString());
            }


        }

    }
     public void RunTest(){
         CaseMenu menu=new CaseMenu();
         menu.addItem((new CaseMenu.Item(this , "Login" , "Login")));
         menu.addItem((new CaseMenu.Item(this , "RealPlay" , "RealPlay")));
         menu.addItem(new CaseMenu.Item(this,"LoginOut","LoginOut"));
         menu.run();
     }
    ////////////////////////////////////////////////////////////////
    private String m_strIp 				    = "172.23.28.86";
    private int   m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";
    ////////////////////////////////////////////////////////////////

    public static void main(String []args){
        final MacRealPlay XM=new MacRealPlay(lRealHandle);
        XM.InitTest();
        /*new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                XM.Login();
            }
        }).start();*/
        XM.RunTest();
        XM.LoginOut();
    }
}
