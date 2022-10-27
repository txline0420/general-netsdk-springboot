package com.netsdk.demo.customize;

import com.netsdk.demo.util.Testable;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class GetRemoteCamera implements Testable {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;

	private LLong loginHandle = new LLong(0);
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	
	private static class DisconnectCallback implements fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
	private static class HaveReconnectCallback implements fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
		}
	}

	@Override
	public void initTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
    	final int nSpecCap = 0; /// login device by TCP
    	final String address = "172.32.5.130";
    	final int port = 37777;
    	final String usrname = "admin";
    	final String password = "admin123";
       	IntByReference nError = new IntByReference(0);
    	
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, usrname , 
    			password, nSpecCap, null, deviceinfo, nError);
		if(loginHandle.longValue() == 0)
		{
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);		
	}

	@Override
	public void runTest() {
		if(loginHandle.longValue() == 0) {
			System.err.println("请先登录设备！");
			return;
		}
		
		// 获取远程设备的信息
		GetMatrixCamerasInfo();
		
		// 获取摄像机通道状态
		QueryChannelState();
	}

	@Override
	public void endTest() {
		if( loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
			loginHandle.setValue(0);
		}
		
		/// 清理资源, 只需调用一次
		netsdkApi.CLIENT_Cleanup();
		
		System.out.println("See You...");
	}
	
	// 获取远程设备的信息
	public void GetMatrixCamerasInfo () {	
		int cameraCount = deviceinfo.byChanNum;  // 通道号个数
		NET_MATRIX_CAMERA_INFO[]  cameraInfo = new NET_MATRIX_CAMERA_INFO[cameraCount];
		for(int i = 0; i < cameraCount; i++) {
			cameraInfo[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		// 入参
		NET_IN_MATRIX_GET_CAMERAS inMatrix = new NET_IN_MATRIX_GET_CAMERAS();
		
		// 出参
		NET_OUT_MATRIX_GET_CAMERAS outMatrix = new NET_OUT_MATRIX_GET_CAMERAS();
		outMatrix.nMaxCameraCount = cameraCount;
		outMatrix.pstuCameras = new Memory(cameraInfo[0].size() * cameraCount);
		outMatrix.pstuCameras.clear(cameraInfo[0].size() * cameraCount);
		
		ToolKits.SetStructArrToPointerData(cameraInfo, outMatrix.pstuCameras);  // 将数组内存拷贝到Pointer
		
		if(netsdkApi.CLIENT_MatrixGetCameras(loginHandle, inMatrix, outMatrix, 5000)) {
			ToolKits.GetPointerDataToStructArr(outMatrix.pstuCameras, cameraInfo);  // 将 Pointer 的内容 输出到   数组
			
			for(int j = 0; j < outMatrix.nRetCameraCount; j++) {
				if(cameraInfo[j].bRemoteDevice == 0) {   // 过滤远程设备
					continue;
				}
				System.out.println("通道号：" + cameraInfo[j].nChannelID);
				System.out.println("IP : " + new String(cameraInfo[j].stuRemoteDevice.szIp).trim());
				System.out.println("nPort : " + cameraInfo[j].stuRemoteDevice.nPort);
				System.out.println("szUser : " + new String(cameraInfo[j].stuRemoteDevice.szUser).trim());
				System.out.println("szPwd : " + new String(cameraInfo[j].stuRemoteDevice.szPwd).trim());
				System.out.println("通道个数 : " + cameraInfo[j].stuRemoteDevice.nVideoInputChannels);
			}
		} else {
			System.err.println("MatrixGetCameras Failed." + ToolKits.getErrorCode());
		}
	}
	
	// 获取摄像机通道状态
	private void QueryChannelState() {
		int nQueryType = NetSDKLib.NET_QUERY_GET_CAMERA_STATE;
		
		// 入参
		NET_IN_GET_CAMERA_STATEINFO stIn = new NET_IN_GET_CAMERA_STATEINFO();
		stIn.bGetAllFlag = 1; // 1-true,查询所有摄像机状态
		
		// 摄像机通道信息
		int chnCount = deviceinfo.byChanNum;  // 通道个数
		NET_CAMERA_STATE_INFO[] cameraInfo = new NET_CAMERA_STATE_INFO[chnCount];
		for(int i = 0; i < chnCount; i++) {
			cameraInfo[i] = new NET_CAMERA_STATE_INFO();
		}
		
		// 出参
		NET_OUT_GET_CAMERA_STATEINFO stOut = new NET_OUT_GET_CAMERA_STATEINFO();
		stOut.nMaxNum = chnCount;
		stOut.pCameraStateInfo = new Memory(cameraInfo[0].size() * chnCount);
		stOut.pCameraStateInfo.clear(cameraInfo[0].size() * chnCount);
		
		ToolKits.SetStructArrToPointerData(cameraInfo, stOut.pCameraStateInfo);  // 将数组内存拷贝到Pointer
    	
		stIn.write();
		stOut.write();
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
    	stIn.read();
		stOut.read();
		
		if(bRet) {
			ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, cameraInfo);  // 将 Pointer 的内容 输出到   数组
			
			System.out.println("查询到的摄像机通道状态有效个数：" + stOut.nValidNum);
			
			for(int i = 0; i < stOut.nValidNum; i++) {
				System.out.println("通道号：" + cameraInfo[i].nChannel);
				System.out.println("连接状态：" + getChannelState(cameraInfo[i].emConnectionState));   
			}
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	// 通道状态对应关系
	private String getChannelState(int state) {
		String channelState = "";
		switch (state) {
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_UNKNOWN:
			channelState = "未知";
			break;
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_CONNECTING:
			channelState = "正在连接";
			break;
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_CONNECTED:
			channelState = "已连接";
			break;
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_UNCONNECT:
			channelState = "未连接";
			break;
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_EMPTY:
			channelState = "未连接";
			break;
		case EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_DISABLE:
			channelState = "通道未配置,无信息";
			break;
		default:
			channelState = "通道有配置,但被禁用";
			break;
		}
		return channelState;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GetRemoteCamera cameras = new GetRemoteCamera();
		
		cameras.initTest();
		
		cameras.runTest();
		
		cameras.endTest();

	}

}
