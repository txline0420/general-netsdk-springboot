package com.netsdk.demo.example;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class XrayMachine {
	
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	
	// 登陆句柄
    private LLong loginHandle = new LLong(0);
    
	// 登陆句柄
    private LLong lFindID = new LLong(0);
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	//private NET_TIME m_startTime = new NET_TIME(); // 开始时间
	//private NET_TIME m_stopTime = new NET_TIME(); // 结束时间
    
	public void InitTest(){
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
	
	// 开始查找X光机包裹信息
	public void StartFindXRayPkg(){
		NET_IN_START_FIND_XRAY_PKG NSFXP=new NET_IN_START_FIND_XRAY_PKG();
		NET_OUT_START_FIND_XRAY_PIC NOSFT=new NET_OUT_START_FIND_XRAY_PIC();
		//查询结果按时间排序(1.按时间升序。2.按时间降序)
		NSFXP.emTimeOrder=1;
		//开始时间
		NSFXP.stuStartTime.setTime(2019,11,20,00,00,00);
		/*NSFXP.stuStartTime.dwYear=2019;
		NSFXP.stuStartTime.dwMonth=10;
		NSFXP.stuStartTime.dwDay=23;
		NSFXP.stuStartTime.dwHour=12;
		NSFXP.stuStartTime.dwMinute=12;
		NSFXP.stuStartTime.dwSecond=12;*/
		//结束时间
		NSFXP.stuEndTime.setTime(2019,11,20,23,59,59);
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
		if(lFindID.longValue()!=0){
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
		
		NET_IN_DO_FIND_XRAY_PKG NIODFXP  = new NET_IN_DO_FIND_XRAY_PKG();
		NET_OUT_DO_FIND_XRAY_PKG NODFXPG = new NET_OUT_DO_FIND_XRAY_PKG();
		NET_XRAY_PKG_INFO[] NXPI = new NET_XRAY_PKG_INFO[10];
		for(int i=0;i<10;i++) {
			NXPI[i] = new NET_XRAY_PKG_INFO();
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
				NET_IN_DOWNLOAD_REMOTE_FILE stIn = new NET_IN_DOWNLOAD_REMOTE_FILE();
				stIn.pszFileName = new NativeString(new String(ColorOverlayPath).trim()).getPointer();
				stIn.pszFileDst = new NativeString("./ColorOverlayPath"+(j++)+".jpg").getPointer(); // 存放路径

				// 出参
				NET_OUT_DOWNLOAD_REMOTE_FILE stOut = new NET_OUT_DOWNLOAD_REMOTE_FILE();
				
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
	
             	 ////////////////////////////////////////////////////////////////
                   private String m_strIp 				    = "172.9.200.200";  
                   private int    m_nPort 				    = 37777;
                   private String m_strUser 			    = "admin";
                   private String m_strPassword 		    = "admin123";
                 ////////////////////////////////////////////////////////////////
	
	public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "开始查找X光机包裹信息" , "StartFindXRayPkg")));
		menu.addItem((new CaseMenu.Item(this , "查询X光机包裹的信息" , "DoFindXRayPkg")));
		menu.addItem((new CaseMenu.Item(this , "结束查询X光机包裹的信息" , "StopFindXRayPkg")));
		menu.run();
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

        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
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

        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }
    }
    
    
    public static void main(String []args){
	    XrayMachine XM=new XrayMachine();
	    XM.InitTest();
	    XM.RunTest();
	    XM.LoginOut();
    }
}

