package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class MatrixCameras {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private LLong loginHandle = new LLong(0);   //登陆句柄
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); 
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{		
		//初始化SDK库
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);
    	
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
		
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
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
		
    	// 向设备登入
    	int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
												m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Failed! %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    		EndTest();
    	}
	}
	
	/**
	 * 获取所有有效显示源
	 * @throws UnsupportedEncodingException 
	 */
	public void MatrixGetCameras () throws UnsupportedEncodingException {					
		// 显示源信息数组初始化
		int nCameraCount = 60;
		int nMaxVideoInputCount = 10; //视频输入通道最大数
		NET_MATRIX_CAMERA_INFO[]  cameras = new NET_MATRIX_CAMERA_INFO[nCameraCount];
		// 视频输入通道信息数组初始化
		NET_VIDEO_INPUTS[] inputs= new NET_VIDEO_INPUTS[nMaxVideoInputCount];
		for (int j = 0; j < nMaxVideoInputCount; j++) {
			inputs[j] = new NET_VIDEO_INPUTS();
		}
		for(int i = 0; i < nCameraCount; i++) {
			NET_MATRIX_CAMERA_INFO camera = new NET_MATRIX_CAMERA_INFO();
			NET_REMOTE_DEVICE device = new NET_REMOTE_DEVICE();
			device.nMaxVideoInputCount = nMaxVideoInputCount;// 视频输入通道最大数							
			device.pstuVideoInputs = new Memory(inputs[0].size() * nMaxVideoInputCount);	
			device.pstuVideoInputs.clear(inputs[0].size() * nMaxVideoInputCount);
			// 将数组内存拷贝到Pointer
			ToolKits.SetStructArrToPointerData(inputs, device.pstuVideoInputs);
			camera.stuRemoteDevice = device;										
			cameras[i] = camera;
		}	
		
		/*
		 *  入参
		 */
		NET_IN_MATRIX_GET_CAMERAS stuIn = new NET_IN_MATRIX_GET_CAMERAS();
		
		/*
		 *  出参
		 */
		NET_OUT_MATRIX_GET_CAMERAS stuOut = new NET_OUT_MATRIX_GET_CAMERAS();
		stuOut.nMaxCameraCount = nCameraCount;
		stuOut.pstuCameras = new Memory(cameras[0].size() * nCameraCount);
		stuOut.pstuCameras.clear(cameras[0].size() * nCameraCount);
		
		ToolKits.SetStructArrToPointerData(cameras, stuOut.pstuCameras);  // 将数组内存拷贝到Pointer
		
		if(netsdkApi.CLIENT_MatrixGetCameras(loginHandle, stuIn, stuOut, 5000)) {
			ToolKits.GetPointerDataToStructArr(stuOut.pstuCameras, cameras);  // 将 Pointer 的内容 输出到   数组
			
			for(int j = 0; j < stuOut.nRetCameraCount; j++) {
				if(cameras[j].bRemoteDevice == 0) {   
					System.out.println("NVR通道号：" + cameras[j].nUniqueChannel);
					System.out.println("前端通道号(远程设备)：" + cameras[j].nChannelID);
					System.out.println("设备ID : " + new String(cameras[j].szDevID).trim());
					//System.out.println("IP : " + new String(cameras[j].stuRemoteDevice.szIp).trim());
					//System.out.println("nPort : " + cameras[j].stuRemoteDevice.nPort);
					//System.out.println("szUser : " + new String(cameras[j].stuRemoteDevice.szUser).trim());
					//System.out.println("szPwd : " + new String(cameras[j].stuRemoteDevice.szPwd).trim());
					System.out.println("通道个数 : " + cameras[j].stuRemoteDevice.nVideoInputChannels + "\n");					
				} else {
					System.out.println("NVR通道号：" + cameras[j].nUniqueChannel);
					System.out.println("前端通道号(远程设备)：" + cameras[j].nChannelID);
					System.out.println("设备ID : " + new String(cameras[j].szDevID).trim());
					System.out.println("设备类型 : " + new String(cameras[j].stuRemoteDevice.szDevClass).trim());
					System.out.println("设备序列号 : " + new String(cameras[j].stuRemoteDevice.szSerialNo).trim());
					System.out.println("实际返回通道个数 : " + cameras[j].stuRemoteDevice.nRetVideoInputCount);					
					ToolKits.GetPointerDataToStructArr(cameras[j].stuRemoteDevice.pstuVideoInputs, inputs);  // 将 Pointer 的内容 输出到   数组	
					for (int i = 0; i < cameras[j].stuRemoteDevice.nRetVideoInputCount; i++) {
						System.out.println("使能："+inputs[i].bEnable);
						System.out.println("通道名称："+new String(inputs[i].szChnName,"GBK"));
						System.out.println("控制ID："+new String(inputs[i].szControlID));
						System.out.println("主码流url地址："+new String(inputs[i].szMainStreamUrl)+"\n");
					}					
				}
			}
		} else {
			System.err.println("获取所有有效显示源失败！" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 设置显示源
	 */
	public void MatrixSetCameras() {
		// 设置的显示源个数
		int nCameraCount = 1;  
		
		// 显示源信息数组初始化
		NET_MATRIX_CAMERA_INFO[] cameras = new NET_MATRIX_CAMERA_INFO[nCameraCount];
		for(int i = 0; i < nCameraCount; i++) {
			cameras[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		cameras[0].nUniqueChannel = 0;
		cameras[0].nChannelID = 0; // 通道号
		
		/*
		 * 入参
		 */
		NET_IN_MATRIX_SET_CAMERAS stuIn = new NET_IN_MATRIX_SET_CAMERAS();
		stuIn.nCameraCount = nCameraCount;
		stuIn.pstuCameras = new Memory(cameras[0].size() * nCameraCount);
		stuIn.pstuCameras.clear(cameras[0].size() * nCameraCount);
		
		ToolKits.SetStructArrToPointerData(cameras, stuIn.pstuCameras);  // 将数组内存拷贝到Pointer
		
		/*
		 * 出参
		 */
		NET_OUT_MATRIX_SET_CAMERAS stuOut = new NET_OUT_MATRIX_SET_CAMERAS();
		
		if(netsdkApi.CLIENT_MatrixSetCameras(loginHandle, stuIn, stuOut, 5000)) {
			System.out.println("设置显示源成功！");
		} else {
			System.err.println("设置显示源失败！" + ToolKits.getErrorCode());
		}
	}
	
	
	public void GetRemoteDevice() {
		int nChnCount = deviceinfo.byChanNum;
		AV_CFG_RemoteDevice deviceInfo[] = new AV_CFG_RemoteDevice[nChnCount];
		for (int i = 0; i < nChnCount; ++ i) {
			deviceInfo[i] = new AV_CFG_RemoteDevice();
		}
		
		/// 获取服务器所有远程设备的信息
		int realCount = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_REMOTEDEVICE, deviceInfo);
		System.out.println("个数：" + realCount);
		if(realCount < 1) {
			return;
		}

		for(int i = 0; i < realCount; ++ i)
		{
			try {
				String remoteInfoString = "用户名： " + new String(deviceInfo[i].szUser).trim() + "\n"
						+ "密码： " + new String(deviceInfo[i].szPassword).trim() + "\n"
						+ "设备IP：" + new String(deviceInfo[i].szIP).trim() + "\n"
						+ "设备Port：" + deviceInfo[i].nPort + "\n"
						+ "设备名称：" + new String(deviceInfo[i].szName, "GBK").trim() + "\n"
						+ "部署地点： " + new String(deviceInfo[i].szAddress, "GBK").trim() + "\n"
						+ "设备类型:" + new String(deviceInfo[i].szDevClass).trim() + "\n"
						+ "设备型号:" + new String(deviceInfo[i].szDevType).trim();	
				
				System.out.println(remoteInfoString);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}	
	}
	
	
	////////////////////////////////////////////////////////////////
	// 登陆信息
	String m_strIp 			= "172.32.103.250"; 
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		
		menu.addItem(new CaseMenu.Item(this , "获取远程设备信息" , "GetRemoteDevice"));
		
		menu.addItem(new CaseMenu.Item(this , "获取所有有效显示源" , "MatrixGetCameras"));
		menu.addItem(new CaseMenu.Item(this , "设置显示源" , "MatrixSetCameras"));
		
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		MatrixCameras demo = new MatrixCameras();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
