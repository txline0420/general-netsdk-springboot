package com.netsdk.demo.customize.securityCheck;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.securityCheck.PackageInformationCallBack;
import com.netsdk.lib.structure.NET_IN_XRAY_ATTACH_PACKAGE_STATISTICS;
import com.netsdk.lib.structure.NET_OUT_XRAY_ATTACH_PACKAGE_STATISTICS;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 291189
 * @version 1.0
 * @description 安检机包裹检测
 * @date 2021/7/1
 */
public class SecurityMachinePackageDetectionDemo {

    /**

     10.35.232.160 安检门环境
     admin  admin123

     安检机 10.35.233.144
     port 37777
     admin  admin123

     */
    private String m_strIpAddr 				    = "10.35.232.75";
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

    //X光机包裹信息句柄
    private static NetSDKLib.LLong lFindID = new NetSDKLib.LLong(0);

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

    /**
     *    订阅X光机包裹数量统计信息
     */

    public void attachPackageStatistics(){
        //入参
        NET_IN_XRAY_ATTACH_PACKAGE_STATISTICS inParam=new NET_IN_XRAY_ATTACH_PACKAGE_STATISTICS();

        UUID uuid= UUID.randomUUID();
        byte[] szUUID = inParam.szUUID;

        String numbers =uuid.toString();
        //beda31a3-a7b5-4126-81b9-6e67402b7b0c?
        int i = numbers.lastIndexOf("-");

        String[] split = numbers.split("-");
        //UUID算法(8-4-4-16格式)

        numbers=split[0]+"-"+split[1]+""+split[2]+"-"+split[3]+split[4];

        System.arraycopy(numbers.getBytes(), 0, szUUID, 0, numbers.getBytes().length);

        inParam.szUUID=szUUID;

        Pointer user=new Memory(1024);
        inParam.dwUser=user;
        inParam.cbNotify= PackageInformationCallBack.getInstance();
        Pointer pInParam=new Memory(inParam.size());
        ToolKits.SetStructDataToPointer(inParam, pInParam, 0);

        //出参
        NET_OUT_XRAY_ATTACH_PACKAGE_STATISTICS outParm=new NET_OUT_XRAY_ATTACH_PACKAGE_STATISTICS();

        Pointer pOutParam=new Memory(outParm.size());
        ToolKits.SetStructDataToPointer(outParm, pOutParam, 0);

        AttachHandle=  netSdk.CLIENT_XRayAttachPackageStatistics(loginHandle,pInParam,pOutParam,3000);

        if (AttachHandle.longValue() != 0) {
            System.out.println("CLIENT_XRayAttachPackageStatistics Success");
        } else {
            System.out.println("CLIENT_XRayAttachPackageStatistics Failed!LastError = %s\n"+ToolKits.getErrorCode());
        }
    }

    /**
     * 取消订阅X光机包裹数量统计信息
     */
    public void DetachPackageStatistics() {
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_XRayDetachPackageStatistics(AttachHandle);
        }
    }

    // 开始查找X光机包裹信息
    public void StartFindXRayPkg(){
        NetSDKLib.NET_IN_START_FIND_XRAY_PKG NSFXP=new NetSDKLib.NET_IN_START_FIND_XRAY_PKG();
        NetSDKLib.NET_OUT_START_FIND_XRAY_PIC NOSFT=new NetSDKLib.NET_OUT_START_FIND_XRAY_PIC();
        //查询结果按时间排序(1.按时间升序。2.按时间降序)
        NSFXP.emTimeOrder=1;
        //开始时间
        NSFXP.stuStartTime.setTime(2021,7,1,00,00,00);
		/*NSFXP.stuStartTime.dwYear=2019;
		NSFXP.stuStartTime.dwMonth=10;
		NSFXP.stuStartTime.dwDay=23;
		NSFXP.stuStartTime.dwHour=12;
		NSFXP.stuStartTime.dwMinute=12;
		NSFXP.stuStartTime.dwSecond=12;*/
        //结束时间
        NSFXP.stuEndTime.setTime(2021,7,1,23,59,59);
		/*NSFXP.stuEndTime.dwYear=2019;
		NSFXP.stuEndTime.dwMonth=10;
		NSFXP.stuEndTime.dwDay=24;
		NSFXP.stuEndTime.dwHour=12;
		NSFXP.stuEndTime.dwMinute=12;
		NSFXP.stuEndTime.dwSecond=12;*/
        int[] SimilarityArray={0,100};
        int[] emObjTypeArray={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26};

        System.arraycopy(SimilarityArray,0,NSFXP.nSimilarityRange,0,SimilarityArray.length);

        NSFXP.nObjTypeNum=26;

        System.arraycopy(emObjTypeArray,0,NSFXP.emObjType,0,NSFXP.nObjTypeNum);

        lFindID=netSdk.CLIENT_StartFindXRayPkg(loginHandle, NSFXP, NOSFT, 1000);

        if(AttachHandle.longValue()!=0){
            System.out.printf("Start FindXRayPkg Success!\n 包裹总数为"+NOSFT.nTotal);
        }else{
            System.err.printf("Start FindXRayPkg Fail.Error[%s]\n", ToolKits.getErrorCode());
        }
    }

    // 查询X光机包裹的信息
    public void DoFindXRayPkg(){

        if(lFindID.longValue() == 0) {
            System.out.println("请开始查找X光机包裹信息");
            return;
        }
         File picturePath= new File("./ColorOverlay/");
        if (!picturePath.exists()) {
            picturePath.mkdirs();
        }

        NetSDKLib.NET_IN_DO_FIND_XRAY_PKG NIODFXP  = new NetSDKLib.NET_IN_DO_FIND_XRAY_PKG();
        NetSDKLib.NET_OUT_DO_FIND_XRAY_PKG NODFXPG = new NetSDKLib.NET_OUT_DO_FIND_XRAY_PKG();
        NetSDKLib.NET_XRAY_PKG_INFO[] NXPI = new NetSDKLib.NET_XRAY_PKG_INFO[10];
        for(int i=0;i<10;i++) {
            NXPI[i] = new NetSDKLib.NET_XRAY_PKG_INFO();
        }
        NIODFXP.nCount=10;
        NIODFXP.nOffset=10;
        NODFXPG.nMaxCount=10;
        NODFXPG.pstuXRayPkgInfo = new Memory(NXPI[0].size()*10);
        NODFXPG.pstuXRayPkgInfo.clear(NXPI[0].size()*10);

        ToolKits.SetStructArrToPointerData(NXPI, NODFXPG.pstuXRayPkgInfo);  // 将数组内存拷贝给Pointer

        Boolean DoFind=netSdk.CLIENT_DoFindXRayPkg(lFindID, NIODFXP, NODFXPG, 3000);
        if(!DoFind){
            System.err.printf("Do Find XRay Pkg.Error[%s]\n", ToolKits.getErrorCode());
            return;
        }

        ToolKits.GetPointerDataToStructArr(NODFXPG.pstuXRayPkgInfo, NXPI);

        for(int j = 0; j< NODFXPG.nRetCount; j++) {

            System.out.println("包裹产生时间"+NXPI[j].stuTime+"\n关联的进口IPC通道号"+NXPI[j].nChannelIn+"\n关联的出口IPC通道号"+NXPI[j].nChannelOut
                    +"\n用户名"+new String(NXPI[j].szUser).trim()+"\n需要下载的文件名"+new String(NXPI[j].stuViewInfo[0].szColorOverlayImagePath).trim());

            for(int k=0;k<2;k++){
                byte[] EnergyPath=NXPI[j].stuViewInfo[k].szEnergyImagePath;
                byte[] ColorOverlayPath=NXPI[j].stuViewInfo[k].szColorOverlayImagePath;

                if(NXPI[j].stuViewInfo[k].nColorOverlayImageLength !=0){

                    // 入参
                    NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE stIn = new NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE();
                    stIn.pszFileName = new NativeString(new String(ColorOverlayPath).trim()).getPointer();
                    stIn.pszFileDst = new NativeString(picturePath + "\\"+System.currentTimeMillis()+"ColorOverlayPath"+".jpg").getPointer(); // 存放路径

                    // 出参
                    NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE stOut = new NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE();

                    if(netSdk.CLIENT_DownloadRemoteFile(loginHandle, stIn, stOut, 5000)) {
                        System.out.println("下载图片成功!");
                    } else {
                        System.err.println("下载图片失败!" + ToolKits.getErrorCode());
                    }
                }
            }
        }
    }

    // 结束查询X光机包裹的信息
    public void StopFindXRayPkg(){
        Boolean StopFind=netSdk.CLIENT_StopFindXRayPkg(lFindID);
        if(StopFind){
            System.out.println("Stop Find XRayPkg suceess");
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "attachPackageStatistics" , "attachPackageStatistics"));
        menu.addItem(new CaseMenu.Item(this , "DetachPackageStatistics" , "DetachPackageStatistics"));
        menu.addItem((new CaseMenu.Item(this , "StartFindXRayPkg" , "StartFindXRayPkg")));
        menu.addItem((new CaseMenu.Item(this , "DoFindXRayPkg" , "DoFindXRayPkg")));
        menu.addItem((new CaseMenu.Item(this , "StopFindXRayPkg" , "StopFindXRayPkg")));
        menu.run();
    }




    public static  void main(String[] args){
        SecurityMachinePackageDetectionDemo securityMachinePackageDetectionDemo=new SecurityMachinePackageDetectionDemo();
        securityMachinePackageDetectionDemo.InitTest();
        securityMachinePackageDetectionDemo.RunTest();
        securityMachinePackageDetectionDemo.LoginOut();

    }

}
