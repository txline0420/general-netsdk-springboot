package com.netsdk.demo.customize;

import com.netsdk.demo.customize.VPRLib.T_VLPINFO;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class CarPlateRecognition {
	/*static NetSDKLib netsdkApi=NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;*/
	
	/*static {
		System.setProperty("jna.debug_load", "1");
	}*/
	
	static VPRLib vprlib 	= VPRLib.VPR_INSTANCE;
	static VPRLib config 	= VPRLib.VPR_config;
	private static int nHandle;
	/*// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements NetSDKLib.fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements NetSDKLib.fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}
	
	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); */
	
	public void InitTest()
	{				
		//初始化SDK库
		vprlib.VLPR_Init(); 
    	
		/*// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
		
		//设置登录超时时间和尝试次数，可选
		int waitTime = 10000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		
		File path = new File(".");			
		String logPath = path.getAbsoluteFile().getParent() + File.separator + "sdklog";
		
		File file = new File(logPath);
		if (!file.exists()) {
			file.mkdir();
		}
		
		logPath = file + File.separator + "123456789.log";
		
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
	
		System.out.println();
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
		if (!bLogopen) {
			System.err.println("Failed to open NetSDK log !!!");
		}*/
		
    	// 向设备登入
    	int nType = 1;
    	String sParas = "172.23.30.60,37777,admin,admin123";
    	nHandle= vprlib.VLPR_Login(nType, sParas);
		
		if(nHandle>0) {
    		System.out.printf("Login Device Port Success!\n");
    	}
    	else {	
    		System.out.printf("Login Device  Port Fail.Last Error[%d]\n" , nHandle);
    		EndTest();
    	}
	}
	public void EndTest()
	{
		System.out.println("End Test");
		if( nHandle> 0)
		{
			vprlib.VLPR_Logout(nHandle);
		}
		System.out.println("See You...");
		vprlib.VLPR_Deinit();
		System.exit(0);
	}
	
	//手动抓图
	public void ManualSnap(){
		int nRet=vprlib.VLPR_ManualSnap(nHandle);
		if(nRet==0){
			System.out.printf("VLPR_ManualSnap success!");
			Pointer pUser=new Memory(128);
			int SetResult=vprlib.VLPR_SetResultCallBack(nHandle, RpFunc, pUser);
			if(SetResult==0){
				System.out.println("VLPR_SetResultCallBack Success");
			}else{
				System.err.printf("VLPR_SetResultCallBack Failed! [%d]\n", SetResult);
			}
		}else{
			System.err.printf("VLPR_ManualSnap Failed! [%d]\n", nHandle);
		}
	}
	
	public void GetInfo(){
		Pointer pUser=new Memory(128);
		int SetResult=vprlib.VLPR_SetStatusCallBack(nHandle, 10002, SpFunc, pUser);
		if(SetResult==0){
			System.out.println("VLPR_SetStatusCallBack Success");
		}else{
			System.err.printf("VLPR_SetStatusCallBack Failed! [%d]\n", SetResult);
		}
	}
	
	private CBFun_GetRegRt RpFunc = new CBFun_GetRegRt();
	private CBFun_GetDevSs SpFunc =new CBFun_GetDevSs();
	
	/*public void CBfun_get() {
		int SetResult=vprlib.VLPR_SetResultCallBack(nHandle, RpFunc, null);
		if(SetResult==0){
			System.out.println("VLPR_SetResultCallBack Success Channel: " + nHandle);
		}else{
			System.err.printf("VLPR_SetResultCallBack Failed! [%d]\n", nHandle);
		}
	}*/
	public class CBFun_GetRegRt implements VPRLib.CBFun_GetRegResult{

		@Override
		public void invoke(int nHandle, T_VLPINFO pVlpResult, Pointer pUser) {
			// TODO Auto-generated method stub
			if(RpFunc==null){
				System.out.println("CBFun_GetRegResult Failed!");
			}else{
				File path = new File("D:/EventPicture/");
	            if (!path.exists()) {
	                path.mkdir();
	            }
				System.out.println("车牌颜色"+pVlpResult.vlpColor+"车牌号"+pVlpResult.vlpText);
				//场景图
				String strFileName = path + "\\" + System.currentTimeMillis() + ".jpg";
				ToolKits.savePicture(pVlpResult.image[0] , pVlpResult.imageLength[0] , strFileName);
				//车牌图
				ToolKits.savePicture(pVlpResult.image[1] , pVlpResult.imageLength[1] , strFileName);
				//二值化图
				ToolKits.savePicture(pVlpResult.image[2] , pVlpResult.imageLength[2] , strFileName);
			}
		}
	}
	public class CBFun_GetDevSs implements VPRLib.CBFun_GetDevStatus{

		@Override
		public void invoke(int nHandle, int nStatus, Pointer pUser) {
			// TODO Auto-generated method stub
			if(SpFunc==null){
				System.out.println("CBFun_GetDevSs Failed!");
			}else{
				getStatusCode();
			}
		}
	}
	public void getStatusCode(){
		IntByReference pStatusCode=new IntByReference();
		int nRet=vprlib.VLPR_GetStatus(nHandle, pStatusCode);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 状态:" + pStatusCode.getValue());
			break;
		}
		default:
			System.out.println("操作失败");
		}
		getStatusMsg(pStatusCode.getValue());
	}
	
	public void getStatusMsg(int nStatusCode){
		
		int nStatusMsgLen = 128;
		Pointer sStatusMsg=new Memory(nStatusMsgLen);
		sStatusMsg.clear(nStatusMsgLen);

		int nRet = vprlib.VLPR_GetStatusMsg(nStatusCode, sStatusMsg, nStatusMsgLen);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 状态信息:" + new String(sStatusMsg.getByteArray(0, nStatusMsgLen)).trim()); // 中文要转GBK
			break;
		}
		default:
			System.out.println("操作失败 ");
			break;
		}
		GetHWVersion();
	}
	public void GetHWVersion(){
		int nHWVerMaxLen=128;
		int nAPIVerMaxLen=128;
		Pointer sHWVersion=new Memory(nHWVerMaxLen);
		Pointer sAPIVersion=new Memory(nAPIVerMaxLen);
		sHWVersion.clear(nHWVerMaxLen);
		sAPIVersion.clear(nAPIVerMaxLen);
		int nRet=vprlib.VLPR_GetHWVersion(nHandle, sHWVersion, nHWVerMaxLen, sAPIVersion, nAPIVerMaxLen);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 硬件版本信息:" + new String(sHWVersion.getByteArray(0, nHWVerMaxLen)).trim()+"/n"+"设备固件版本信息"+
		new String(sAPIVersion.getByteArray(0, nAPIVerMaxLen)).trim()); // 中文要转GBK
			break;
		}
		default:
			System.out.println("操作失败 ");
		}
	}
	
	public void run(){
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "ManualSnap", "ManualSnap"));
		menu.addItem(new CaseMenu.Item(this , "GetInfo", "GetInfo"));
		menu.run();
	}
	public static void main(String[]args){
		CarPlateRecognition CPR=new CarPlateRecognition();
		CPR.InitTest();
		CPR.run();
		CPR.EndTest();
	}
	
}
