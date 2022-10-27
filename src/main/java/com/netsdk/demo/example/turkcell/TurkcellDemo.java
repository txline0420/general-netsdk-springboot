package com.netsdk.demo.example.turkcell;



import com.netsdk.demo.example.turkcell.callback.DefaultDisconnect;
import com.netsdk.demo.example.turkcell.callback.DefaultHaveReconnect;
import com.netsdk.demo.example.turkcell.callback.TurkcellAnalyzerCallback;
import com.netsdk.demo.example.turkcell.callback.TurkcellGPSCallback;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_ALL;

/**
 * @author 47081
 * @version 1.0
 * @description MNVR订阅智能事件，上报内容:
 *              1.车牌抓图,上报内容包括时间、车牌图片、车牌信息
 *              2.打电话、吸烟、疲惫、不看路，上报内容包括事件类型、时间、图片
 * @date 2020/6/12
 */
public class TurkcellDemo {
    private NetSDKLib netsdk=NetSDKLib.NETSDK_INSTANCE;
    private NetSDKLib configsdk=NetSDKLib.CONFIG_INSTANCE;
    private NetSDKLib.LLong loginHandler;
    private List<NetSDKLib.LLong> realLoadHandlers;
    private NetSDKLib.fGPSRevEx gpsRev;
    private NetSDKLib.fAnalyzerDataCallBack analyzerDataCallBack;
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo;
    private boolean bInit=false;
    private boolean bLogopen=true;

    /**
     * sdk init
     * sdk初始化
     * @return
     */
    public boolean init(){
        gpsRev= TurkcellGPSCallback.getInstance();
        analyzerDataCallBack= TurkcellAnalyzerCallback.getInstance();
        bInit = netsdk.CLIENT_Init(DefaultDisconnect.getINSTANCE(), null);
        if(!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("./sdklog/");
        if (!path.exists()) {
            path.mkdir();
        }
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogopen = netsdk.CLIENT_LogOpen(setLog);
        if(!bLogopen ) {
            System.err.println("Failed to open NetSDK log");
        }

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(DefaultHaveReconnect.getINSTANCE(), null);

        //设置登录超时时间和尝试次数，可选
        //登录请求响应超时时间设置为5S
        int waitTime = 5000;
        //登录时尝试建立链接1次
        int tryTimes = 3;
        netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);

        // GDPR使能全局开关
        netsdk.CLIENT_SetGDPREnable(true);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        // 登录时尝试建立链接的超时时间
        netParam.nConnectTime = 10000;
        // 设置子连接的超时时间
        netParam.nGetConnInfoTime = 3000;
        netsdk.CLIENT_SetNetworkParam(netParam);
        return bInit;
    }

    /**
     * login to the device
     */
    public boolean login(){
        //入参
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstInParam=new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        pstInParam.nPort=port;
        pstInParam.szIP=ip.getBytes();
        pstInParam.szPassword=password.getBytes();
        pstInParam.szUserName=username.getBytes();
        //出参
        deviceInfo=new NetSDKLib.NET_DEVICEINFO_Ex();
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam=new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();
        pstOutParam.stuDeviceInfo=deviceInfo;
        loginHandler=netsdk.CLIENT_LoginWithHighLevelSecurity(pstInParam, pstOutParam);
        if(loginHandler.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port, ToolKits.getErrorCode());
            return false;
        }

        System.out.println("Login Success [ ip: " + ip + ",port: "+port+" ]");
        return true;
    }

    /**
     * logout
     */
    public void logout(){
        boolean isLogout=netsdk.CLIENT_Logout(loginHandler);
        if(isLogout){
            netsdk.CLIENT_Cleanup();
            System.out.println("logout success.sdk cleaned");
        }else{
            System.out.println("failed to Logout.the error is:"+ToolKits.getErrorCode());
        }
    }

    /**
     * 订阅GPS信息，GPS信息处理在回调中进行
     */
    public void subGps(){
        //设置GPS订阅回调
        netsdk.CLIENT_SetSubcribeGPSCallBackEX(gpsRev,null);
        //订阅GPS信息
        //
        int bStart=1;
        int keepTime=-1;
        boolean isSubGps=netsdk.CLIENT_SubcribeGPS(loginHandler,bStart,keepTime,2);
        if(isSubGps){
            System.out.println("subcribeGPS success.");
        }else{
            System.out.println("subcribeGPS failed.the error is "+ToolKits.getErrorCode());
        }
    }

    /**
     * 不订阅GPS信息
     * bStart: 1: subcribe,0:dis subcribe
     */
    public void disSubGps(){
        int bStart=0;
        //unit: second
        int keepTime=-1;
        boolean isSubGPS=netsdk.CLIENT_SubcribeGPS(loginHandler,bStart,keepTime,2);
        if(isSubGPS){
            System.out.println("cancel subcribe gps.success");
        }else{
            System.out.println("failed to cancel subcribe gps.the error is "+ToolKits.getErrorCode());

        }
    }

    /**
     * 订阅智能事件,事件处理在回调中进行
     */
    public void subIntelligentEvent(){
        if(realLoadHandlers==null){
            realLoadHandlers=new ArrayList<>();
        }else if(!realLoadHandlers.isEmpty()){
            realLoadHandlers.clear();
        }
        int channelID=0;
        int needPicture=1;
        NetSDKLib.LLong realLoadHandler;
        //sub channel 0,1,2
        for (int i = 0; i < 2; i++) {
            channelID=i;
            realLoadHandler=netsdk.CLIENT_RealLoadPictureEx(loginHandler,channelID,EVENT_IVS_ALL,needPicture,analyzerDataCallBack,null,null);
            if(realLoadHandler.longValue()!=0){
                realLoadHandlers.add(realLoadHandler);
                System.out.println("RealLoadPicture channel "+i+" success.");
            }else{
                System.out.println("RealLoadPicture channel "+i+" failed. the error is: "+ToolKits.getErrorCode());
            }
        }
    }

    /**
     * 取消订阅
     */
    public void disSubIntelligentEvent(){
        for (NetSDKLib.LLong realLoadHandler:realLoadHandlers) {
            boolean isDisSub=netsdk.CLIENT_StopLoadPic(realLoadHandler);
            if(isDisSub){
                realLoadHandlers.remove(realLoadHandler);
                System.out.println("cancel intelligentEvent,handler: "+realLoadHandler.longValue()+" subscribe success.");
            }else{
                System.out.println("failed to cancel intelligentEvent subscribe.the error is "+ToolKits.getErrorCode());
            }
        }
    }
    public void runTest(){
        CaseMenu menu=new CaseMenu();
        menu.addItem(new CaseMenu.Item(this,"订阅GPS信息","subGps"));
        menu.addItem(new CaseMenu.Item(this,"取消订阅GPS信息","disSubGps"));
        menu.addItem(new CaseMenu.Item(this,"订阅智能分析事件","subIntelligentEvent"));
        menu.addItem(new CaseMenu.Item(this,"取消订阅智能分析事件","disSubIntelligentEvent"));
        menu.run();
    }

    private final String ip="172.23.222.103";
    private final int port=37777;
    private final String username="admin";
    private final String password="Admin123";

    public static void main(String[] args){
        TurkcellDemo demo=new TurkcellDemo();
        if(demo.init()){
            if(demo.login()){
                demo.runTest();
            }
        }
        demo.logout();
    }
}
