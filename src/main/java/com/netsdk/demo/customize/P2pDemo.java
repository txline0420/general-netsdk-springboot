package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;

import java.io.File;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220218007 NetSDK私有P2P部分JAVA SDK封装
 * @date 2022/2/21 15:55
 */
public class P2pDemo{

    public  static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    public  static NetSDKLib config =  NetSDKLib.CONFIG_INSTANCE;

    // 登陆句柄
    public static NetSDKLib.LLong loginHandle;

    static NetSDKLib.LLong lSubBizHandle =new NetSDKLib.LLong(0);
    static NetSDKLib.LLong lTransmitHandle =new NetSDKLib.LLong(0);
    static NetSDKLib.LLong lListenServer =new NetSDKLib.LLong(0);

    public static void InitTest(){
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


    }
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

    /**
     * 高安全登录
     */
    public static void loginWithHighLevel(String ip,int port,String user,String password) {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = ip.getBytes();
                nPort = port;
                szUserName = user.getBytes();
                szPassword = password.getBytes();
            }
        };

        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
        loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            //  System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port, netSdk.CLIENT_GetLastError());
            throw new RuntimeException("Login Device[%s] Port[%d]Failed. "+ ip+":"+ port+":"+ netSdk.CLIENT_GetLastError());

        } else {
            NetSDKLib.NET_DEVICEINFO_Ex   deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + ip);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }

    }

    //设置私有隧道参数
    public void setTransmitTunnelParam(){

        NET_IN_SET_TRANSMITTUNNEL_PARAM input=new NET_IN_SET_TRANSMITTUNNEL_PARAM();

        byte[] szLoaclIP = input.szLoaclIP;

        String ip="10.34.3.219";

        ToolKits.StringToByteArray(ip,szLoaclIP);

        int[] nPort = input.nPort;

        nPort[0]=60001;
        nPort[1]=60010;

        NET_OUT_SET_TRANSMITTUNNEL_PARAM out=new NET_OUT_SET_TRANSMITTUNNEL_PARAM();

            input.write();
            out.write();

        boolean b
                = netSdk.CLIENT_SetTransmitTunnelParam(input.getPointer(), out.getPointer());

        if(b){
            System.out.println("setTransmitTunnelParam success");
        }else {
            System.out.println("setTransmitTunnelParam fail:"+ ToolKits.getErrorCode());
        }
    }

    //创建sdk私有p2p透传服务模块
    public void CreateSubBusinessModule(){

        NET_IN_CREAT_SUB_BUSINESS_MDL_INFO input=new NET_IN_CREAT_SUB_BUSINESS_MDL_INFO();
        input.emLinkType=1;

        byte[] szBSID = input.szBSID;
        String ip="10.34.3.219";

        ToolKits.StringToByteArray(ip,szBSID);

        NET_OUT_CREAT_SUB_BUSINESS_MDL_INFO output=new NET_OUT_CREAT_SUB_BUSINESS_MDL_INFO();

        input.write();
        output.write();

         lSubBizHandle
                = netSdk.CLIENT_CreateSubBusinessModule(input.getPointer(), output.getPointer());

        if(lSubBizHandle==null||lSubBizHandle.longValue()==0){
            System.out.println("CLIENT_CreateSubBusinessModule fail:"+ ToolKits.getErrorCode());
        }else {
            System.out.println("CLIENT_CreateSubBusinessModule success");
            System.out.println("lSubBizHandle="+lSubBizHandle);
        }

    }
     //启动对下监听服务
    public void StartSubLinkListenServer(){
        NET_IN_START_SUBLINK_LISTEN_SERVER input=new NET_IN_START_SUBLINK_LISTEN_SERVER();

        NET_LOCAL_SERVER_NET_INFO stuLoaclServerInfo =new NET_LOCAL_SERVER_NET_INFO();
        //   stuLoaclServerInfo.nLocalPort=8010;

        byte[]		szLocalIp=new byte[64];
        String ip="10.34.3.219";

        ToolKits.StringToByteArray(ip,szLocalIp);

        stuLoaclServerInfo.szLocalIp=szLocalIp;
        stuLoaclServerInfo.nLocalPort=60000;

        String user="ss,bb,cc";
        Pointer dwUserData = ToolKits.GetGBKStringToPointer(user);

        input.dwUserData=dwUserData;

        input.stuLoaclServerInfo=stuLoaclServerInfo;

         input.cbSubLinkServiceCallBack= SubLinkService.getInstance();

         NET_OUT_START_SUBLINK_LISTEN_SERVER out=new NET_OUT_START_SUBLINK_LISTEN_SERVER();

            input.write();

            out.write();

       lListenServer
                = netSdk.CLIENT_StartSubLinkListenServer(input.getPointer(), out.getPointer());
        if(lListenServer==null||lListenServer.longValue()==0){
            System.out.println("lListenServer fail:"+ ToolKits.getErrorCode());
        }else {
            System.out.println("lListenServer success");
            System.out.println("lListenServer="+lListenServer);
        }

    }
    //请求设备创建p2p透传
  public void   TransferSubLinkInfo(){
      NET_IN_TRANSFER_SUBLINK_INFO input=new NET_IN_TRANSFER_SUBLINK_INFO();

      input.emLinkType=1;

      byte[]					szBSID=new byte[64];

      String ip="10.34.3.219";

      ToolKits.StringToByteArray(ip,szBSID);

      input.szBSID=szBSID;

      NET_LOCAL_SERVER_NET_INFO stuLoaclServerInfo =new NET_LOCAL_SERVER_NET_INFO();
      stuLoaclServerInfo.nLocalPort=60000;
      stuLoaclServerInfo.szLocalIp=szBSID;

      input.stuLoaclServerInfo=stuLoaclServerInfo;

      NET_OUT_TRANSFER_SUBLINK_INFO out=new NET_OUT_TRANSFER_SUBLINK_INFO();

      input.write();

      out.write();

      boolean b
              = netSdk.CLIENT_TransferSubLinkInfo(loginHandle, input.getPointer(), out.getPointer(), 3000);
      if(b){
          System.out.println("创建p2p透传 success");
      }else {
          System.out.println("创建p2p透传 fail:"+ ToolKits.getErrorCode());
      }
  }
   /*@param[in]     lSubBizHandle    业务sdk句柄，由CLIENT_CreateSubBusinessModule接口返回
     *@param[in]     pInParam  接口输入参数, 内存资源由用户申请和释放 NET_IN_CREATE_TRANSMIT_TUNNEL
     *@param[out]    pOutParam 接口输出参数, 内存资源由用户申请和释放 NET_OUT_CREATE_TRANSMIT_TUNNEL

    */
    //创建私有透传p2p隧道

   public void  createTransmitTunnel(){

       NET_IN_CREATE_TRANSMIT_TUNNEL input=new NET_IN_CREATE_TRANSMIT_TUNNEL();
                input.emProxyType=1;
                input.emProxyMode=1;
                input.nPort=80;

       input.cbDisConnectCallBack= TransmitDisConnectC.getInstance();

       String user="ss,bb,cc";
       Pointer dwUserData = ToolKits.GetGBKStringToPointer(user);
       input.dwUserData=dwUserData;
       NET_OUT_CREATE_TRANSMIT_TUNNEL out=new NET_OUT_CREATE_TRANSMIT_TUNNEL();

       input.write();
       out.write();

        lTransmitHandle
               = netSdk.CLIENT_CreateTransmitTunnel(lSubBizHandle, input.getPointer(), out.getPointer());

        if(lTransmitHandle==null||lTransmitHandle.longValue()==0){
            System.out.println("创建私有透传隧道 fail:"+ ToolKits.getErrorCode());
        }else {
            out.read();

            int nPort = out.nPort;
            System.out.println("对上侦听端口:"+nPort);
            int emWebProtocol
                    = out.emWebProtocol;
            System.out.println("emWebProtocol:"+emWebProtocol);

            System.out.println("创建私有透传隧道 success");
        }

   }

   //销毁私有p2p隧道
    public void DestroyTransmitTunnel(){

        boolean b
                = netSdk.CLIENT_DestroyTransmitTunnel(lTransmitHandle);
        if(b){
            System.out.println("销毁隧道 success");
        }else {
            System.out.println("销毁隧道 fail:"+ ToolKits.getErrorCode());
        }
    }

    //停止子连接监听服务
    public void StopSubLinkListenServer(){

        boolean b
                = netSdk.CLIENT_StopSubLinkListenServer(lListenServer);
        if(b){
            System.out.println("停止子连接监听服务 success");
        }else {
            System.out.println("停止子连接监听服务 fail:"+ ToolKits.getErrorCode());
        }
    }



    //销毁sdk私有p2p透传服务模块
    public void DestroySubBusinessModule(){
//lSubBizHandle  业务sdk句柄，由CLIENT_CreateSubBusinessModule接口返回
        boolean b
                = netSdk.CLIENT_DestroySubBusinessModule(lSubBizHandle);
        if(b){
            System.out.println("销毁业务sdk模块 success");
        }else {
            System.out.println("销毁业务sdk模块 fail:"+ ToolKits.getErrorCode());
        }
    }


    private static class SubLinkService implements NetSDKLib.fSubLinkServiceCallBack {
        private final File picturePath;
        private static SubLinkService instance;

        private SubLinkService() {
            picturePath = new File("./AnalyzerPicture/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }

        public static SubLinkService getInstance() {
            if (instance == null) {
                synchronized (SubLinkService.class) {
                    if (instance == null) {
                        instance = new SubLinkService();
                    }
                }
            }
            return instance;
        }


        @Override
        public void invoke(NetSDKLib.LLong lListenServer, NetSDKLib.LLong lSubBizHandle, Pointer pstSubLinkCallBack) {
        //lListenServer 子链接监听服务句柄, 由CLIENT_StartSubLinkListenServer接口返回
        System.out.println("lListenServer:"+lListenServer);
        //lSubBizHandle 分压业务sdk句柄, 由CLIENT_CreateSubBusinessModule接口返回
            System.out.println("lSubBizHandle:"+lSubBizHandle);

            NET_SUBLINK_SERVER_CALLBACK msg=new NET_SUBLINK_SERVER_CALLBACK();

            Pointer dwUserData = msg.dwUserData;
           // System.out.println("dwUserData size:"+dwUserDat);
            String s
                    = ToolKits.GetPointerDataToGBKString(dwUserData);
             System.out.println("dwUserData:"+s);


            ToolKits.GetPointerData(pstSubLinkCallBack, msg);

            NET_DEV_NETWORK_INFO stuDevNetInfo = msg.stuDevNetInfo;

            System.out.println("szDevIP:"+new String(stuDevNetInfo.szDevIP));

            System.out.println("nDevPort:"+stuDevNetInfo.nDevPort);

        }

    }

    /**
     *   隧道业务连接断开回调
     */
    private static class TransmitDisConnectC implements NetSDKLib.fTransmitDisConnectCallBack {
        private final File picturePath;
        private static TransmitDisConnectC instance;

        private TransmitDisConnectC() {
            picturePath = new File("./AnalyzerPicture/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }

        public static TransmitDisConnectC getInstance() {
            if (instance == null) {
                synchronized (TransmitDisConnectC.class) {
                    if (instance == null) {
                        instance = new TransmitDisConnectC();
                    }
                }
            }
            return instance;
        }

/*@param[out]    lSubBizHandle 下载句柄, 由CLIENT_CreateSubBusinessModule接口返回
 *@param[out]    lOperateHandle 业务句柄
 *@param[out]    pstDisConnectInfo 断线回调数据 NET_TRANSMIT_DISCONNECT_CALLBACK
*/
        @Override
        public void invoke(NetSDKLib.LLong lSubBizHandle, NetSDKLib.LLong lOperateHandle, Pointer pstDisConnectInfo) {

            // 下载句柄, 由CLIENT_CreateSubBusinessModule接口返回
            System.out.println("lSubBizHandle:"+lSubBizHandle);
            // 业务句柄
            System.out.println("lOperateHandle:"+lOperateHandle);

            NET_TRANSMIT_DISCONNECT_CALLBACK msg=new NET_TRANSMIT_DISCONNECT_CALLBACK();

            ToolKits.GetPointerData(pstDisConnectInfo, msg);

            NET_DEV_NETWORK_INFO stuDevNetInfo = msg.stuDevNetInfo;

            System.out.println("szDevIP:"+new String(stuDevNetInfo.szDevIP));

            System.out.println("nDevPort:"+stuDevNetInfo.nDevPort);


        }

    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "设置私有隧道参数" , "setTransmitTunnelParam")));
        menu.addItem((new CaseMenu.Item(this , "创建sdk私有p2p透传服务模块" , "CreateSubBusinessModule")));
        menu.addItem((new CaseMenu.Item(this , "启动对下监听服务" , "StartSubLinkListenServer")));
        menu.addItem((new CaseMenu.Item(this , "请求设备创建私有p2p透传连接" , "TransferSubLinkInfo")));
        menu.addItem((new CaseMenu.Item(this , "创建私有p2p隧道" , "createTransmitTunnel")));
        menu.addItem((new CaseMenu.Item(this , "销毁私有p2p参数" , "DestroyTransmitTunnel")));
        menu.addItem((new CaseMenu.Item(this , "停止对下监听服务" , "StopSubLinkListenServer")));
        menu.addItem((new CaseMenu.Item(this , "销毁sdk私有p2p透传服务模块" , "DestroySubBusinessModule")));
        menu.run();
    }

    /**
     * 退出清理环境
     */
    public static void LoginOut(){
        System.out.println("End Test");
        if( loginHandle.longValue() != 0)
        {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }
    public static void main(String[] args) {
      //  Scanner scanner=new Scanner(System.in);
        P2pDemo p2pDemo=new P2pDemo();
        InitTest();
        loginWithHighLevel("172.23.19.53",37777,"admin","admin321");//登录
        p2pDemo.RunTest();

        LoginOut();

    }

}
