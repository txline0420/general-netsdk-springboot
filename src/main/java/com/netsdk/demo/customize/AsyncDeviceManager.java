package com.netsdk.demo.customize;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class AsyncDeviceManager {  // 本函数只实现添加一个设备（简化）
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	LLong loginHandle;
	
	private boolean bAttachFlag = false;
	private LLong l_hAttachHandle = new LLong(0);
	private boolean bAttachStateFlag = false;
	private LLong l_hAttachStateHandle = new LLong(0);
	
	private boolean bSuccess = false;
	private String deviceID = new String();
	private String url = new String();
	private int emAddState = EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_UNKNOWN;
	private int nTaskID = 0;
	public int nLogicChannel = -1;
	public int nRemoteChannel = 0; // 通道默认0
	private AddDeviceThread addDeviceThread;
	
	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public AsyncDeviceManager(LLong loginHandle, String url) {
		this.loginHandle = loginHandle;
		this.url = url;
	}
	
	public void ExecTask() throws InterruptedException {
		
		if (!AttachAddDevice()) { // 注册回调失败
			return;
		}
		
//		if (!AttachDeviceState()) { // 注册回调失败
//			return;
//		}	
		
		addDeviceThread = new AddDeviceThread();
		addDeviceThread.start();
		
		if (!AddDevice()) { // 添加设备失败
			DetachAddDevice();
//			DetachDeviceState();
			return;
		}
		
		addDeviceThread.join(25000); 		// 最多等待25秒
		DetachAddDevice(); 
		return;
	}
	
	public void SetChannel(int nRemoteChannel) {
		this.nRemoteChannel = nRemoteChannel;
	}
	
	public boolean IsAddSuccess() {
		return deviceID.isEmpty();
	}
	
	public boolean IsSetChannelSuccess() {
		return nLogicChannel != -1;
	}
	
	public boolean IsTaskSuccess()
	{
		return bSuccess;
	}
	
	public boolean AttachDeviceState() { // 注册设备状态回调
		
		if (bAttachStateFlag) {
			System.err.println("Had AttachDeviceState.");
			return true;
		}
		
		NET_IN_ATTACH_DEVICE_STATE pstInParam = new NET_IN_ATTACH_DEVICE_STATE();
		fDeviceStateCB  fDeviceStatecb = new fDeviceStateCB();
		pstInParam.cbDeviceState = fDeviceStatecb;
		//pstInParam.dwUser = (LLong)this;
		
		NET_OUT_ATTACH_DEVICE_STATE pstOutParam = new NET_OUT_ATTACH_DEVICE_STATE();
			
		l_hAttachStateHandle = netsdkApi.CLIENT_AttachDeviceState(loginHandle, pstInParam, pstOutParam, 3000);
		if(l_hAttachStateHandle.longValue() != 0) {
			System.out.println("AttachAddDevice Succeed!");
			bAttachStateFlag = true;
			return true;
		}
		else {
			System.err.printf("AttachDeviceState Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
		
	public class fDeviceStateCB implements fDeviceStateCallBack{	

		@Override
		public void invoke(LLong lAttachHandle,
				NET_CB_ATTACH_DEVICE_STATE pstDeviceState, Pointer dwUser) {
			
			if (pstDeviceState.emNotifyType == EM_DEVICE_NOTIFY_TYPE.EM_DEVICE_NOTIFY_TYPE_NEW)
			{
				
				for (int i = 0; i < pstDeviceState.nRetCount; i++) {
					String strDeviceID = new String(pstDeviceState.szDeviceIDsArr[i].szDeviceID);
					if (!strDeviceID.isEmpty()) {
						lock.lock();
						deviceID = strDeviceID;
						emAddState = EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_SUCCESS;
						condition.signalAll();
						lock.unlock();
					}
				}
			} else {
				lock.lock();
				emAddState = EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_UNKNOWN; // 此处未详细解析
				condition.signalAll();
				lock.unlock();
			}
	
			System.out.println("fDeviceStateCB return.");
			
			return;
		}
	}
	
	public boolean AttachAddDevice() { // 注册添加设备回调
		
		if (bAttachFlag) {
			System.err.println("Had AttachAddDevice.");
			return true;
		}
		
		NET_IN_ATTACH_ADD_DEVICE pstInParam = new NET_IN_ATTACH_ADD_DEVICE();
		fAddDeviceCB fAddDevicecb = new fAddDeviceCB();
		pstInParam.cbAddDevice = fAddDevicecb;
		
		NET_OUT_ATTACH_ADD_DEVICE pstOutParam = new NET_OUT_ATTACH_ADD_DEVICE();
			
		l_hAttachHandle = netsdkApi.CLIENT_AttachAddDevice(loginHandle, pstInParam, pstOutParam, 3000);
		if(l_hAttachHandle.longValue() != 0) {
			System.out.println("AttachAddDevice Succeed!");
			bAttachFlag = true;
			return true;
		}
		else {
			System.err.printf("AttachAddDevice Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
		
	public class fAddDeviceCB implements fAddDeviceCallBack{
		
		@Override
		public void invoke(LLong lAttachHandle,
				NET_CB_ATTACH_ADD_DEVICE pstAddDevice, Pointer dwUser) {
			
			if ((pstAddDevice.emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_FAILURE
				|| pstAddDevice.emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_STOP
				|| pstAddDevice.emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_SUCCESS)
				&& pstAddDevice.nTaskID == nTaskID){
				lock.lock();
				emAddState = pstAddDevice.emAddState;
				condition.signalAll();
				lock.unlock();
				System.out.println("fAddDeviceCB add device return.");
			}
			return; 
		}
	}
	
	private class AddDeviceThread extends Thread{ // 处理任务线程
		private long startTime;
		public void run() {	
			startTime = System.currentTimeMillis();
			
			lock.lock();
			try {
				condition.await();
				if (emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_FAILURE
						|| emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_STOP) { // 添加失败
					System.out.println("添加失败");
					releaseResource();
				}else if (emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_SUCCESS) { // 添加成功
					execTask();
				}
			}catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				lock.unlock();
			}
			
			System.out.println(System.currentTimeMillis() - startTime + "ms");
		}
		
		private void execTask() {
			
			GetAddDeviceInfo();
			releaseResource();
			
			if (deviceID.isEmpty()) { // 获取不到设备ID
				System.err.println("获取不到设备ID");
				return;
			}
			
			if (!SetConnectChannel()) { 	// 设置通道失败
				return;
			}
			
			if (!GetChannelInfo()) { // 获取通道信息失败
				return;
			}
			
			System.out.println(System.currentTimeMillis() - startTime + "ms");
			
			if (!setFaceConfig()) { // 设置人脸配置失败
				return;
			}
			
			bSuccess = true;
			
		}
		
		private void releaseResource()
		{
			if (deviceID.isEmpty()) {
				CancelTask(nTaskID);
			}
			else {
				ConfirmTask(nTaskID);
			}
			
			DetachDeviceState();
			DetachAddDevice();
		}
		
		private boolean setFaceConfig() {

//		    String scene = "FaceCompare";    	// 人脸识别场景是否有效(此场景为单使能模式会关闭人脸检测)
//		    String scene = "FaceAttribute";   	// 人脸检测场景是(此场景为单使能模式会关闭人脸识别)
		    String scene = "FaceAnalysis";  	// 人脸识别及人脸检测场景同时设置  
	
			if (!SetFaceScene(scene)) { // 人脸识别及人脸检测场景同时设置
				return false;
			}
			
			if (!SetRuleEnable()) {
				return false;
			}
			
			return true;
		}
	}
	
	public boolean AddDevice() { // 添加设备
		
		NET_IN_ASYNC_ADD_DEVICE pstInParam = new NET_IN_ASYNC_ADD_DEVICE();
		
		pstInParam.nCount = 1;
		System.arraycopy(url.getBytes(), 0, pstInParam.szUrlsArr[0].szUrl, 0, url.getBytes().length);  // URL

		NET_OUT_ASYNC_ADD_DEVICE pstOutParam = new NET_OUT_ASYNC_ADD_DEVICE();
			
		boolean bRet = netsdkApi.CLIENT_AsyncAddDevice(loginHandle, pstInParam, pstOutParam, 3000);
		if(bRet) {
			System.out.println("AsyncAddDevice Succeed!");
			nTaskID = pstOutParam.nTaskID;
			return true;
		}
		else {
			System.err.printf("AsyncAddDevice Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
	
	public boolean GetAddDeviceInfo() { 	// 获取添加中的设备状态	
		
		NET_IN_GET_ADD_DEVICE_LIST_INFO pstInParam = new NET_IN_GET_ADD_DEVICE_LIST_INFO();
		pstInParam.nTaskID = nTaskID;
		pstInParam.nCount = 1;
		pstInParam.nIndex[0] = 0;
	
		NET_OUT_GET_ADD_DEVICE_LIST_INFO pstOutParam = new NET_OUT_GET_ADD_DEVICE_LIST_INFO();

		boolean bRet = netsdkApi.CLIENT_GetAddDeviceInfo(loginHandle, pstInParam, pstOutParam, 3000);
		if(!bRet)
		{
			System.err.printf("GetAddDeviceInfo Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	
		for (int i = 0; i < pstOutParam.nRetCount; i++)
		{
			if (pstOutParam.stuDeviceInfo[i].emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_SUCCESS)
			{
				deviceID = new String(pstOutParam.stuDeviceInfo[i].szDeviceID);
			}
			else if (pstOutParam.stuDeviceInfo[i].emAddState == EM_DEVICE_ADD_STATE.EM_DEVICE_ADD_STATE_FAILURE){
				System.err.printf("Add Device Fail. URL [%s] ErrorCode【%d】 \n", new String(pstOutParam.stuDeviceInfo[i].szUrl), pstOutParam.stuDeviceInfo[i].nErrorCode);
				return false;
			}
		}
		return true;
	}
	
	public boolean GetDeviceInfo(String[] deviceIDs) { 	// 获取已添加的设备信息	
		
		NET_IN_GET_DEVICE_LIST_INFO pstInParam = new NET_IN_GET_DEVICE_LIST_INFO();
		NET_OUT_GET_DEVICE_LIST_INFO pstOutParam = new NET_OUT_GET_DEVICE_LIST_INFO();
		
		if (deviceIDs != null) {	
			pstInParam.nCount = deviceIDs.length;
			for(int i = 0; i < pstInParam.nCount; i++) {
				System.arraycopy(deviceIDs[i].getBytes(), 0, pstInParam.szDeviceIDsArr[i].szDeviceID, 0, deviceIDs[i].getBytes().length);
			}
			pstOutParam.nMaxCount = pstInParam.nCount;
		}else {
			pstInParam.nCount = 0;
			pstOutParam.nMaxCount = NetSDKLib.MAX_LINK_DEVICE_NUM;
		}
		
		NET_GET_DEVICE_INFO[]  deviceInfo = new NET_GET_DEVICE_INFO[pstOutParam.nMaxCount];
		for(int i = 0; i < pstOutParam.nMaxCount; i++) {
			deviceInfo[i] = new NET_GET_DEVICE_INFO();
		}
		pstOutParam.pstuDeviceInfo = new Memory(deviceInfo[0].size() * pstOutParam.nMaxCount);
		pstOutParam.pstuDeviceInfo.clear(deviceInfo[0].size() * pstOutParam.nMaxCount);
		ToolKits.SetStructArrToPointerData(deviceInfo, pstOutParam.pstuDeviceInfo);  // 将数组内存拷贝到Pointer

		boolean bRet = netsdkApi.CLIENT_GetDeviceInfo(loginHandle, pstInParam, pstOutParam, 3000);
		if(!bRet)
		{
			System.err.printf("GetDeviceInfo Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}

		ToolKits.GetPointerDataToStructArr(pstOutParam.pstuDeviceInfo, deviceInfo); // 将 Pointer 的内容 输出到   数组
		for (int i = 0; i < pstOutParam.nRetCount; i++)
		{
			System.out.println("设备ID:" + new String(deviceInfo[i].szDeviceID).trim());
			System.out.println("url:" + new String(deviceInfo[i].szUrl).trim());
			System.out.println("设备序列号:" + new String(deviceInfo[i].szSerialNo).trim());
			System.out.println("设备类型:" + new String(deviceInfo[i].szDeviceType).trim());
			System.out.println();
		}
		return true;
	}
	
	// 设置连接通道
	public boolean SetConnectChannel() {
		
		NET_IN_SET_CONNECT_CHANNEL pstInParam = new NET_IN_SET_CONNECT_CHANNEL();
		System.arraycopy(deviceID.getBytes(), 0, pstInParam.szDeviceID, 0, deviceID.getBytes().length);
		pstInParam.nCount = 1;
		pstInParam.nChannels[0] = nRemoteChannel;
		
		NET_OUT_SET_CONNECT_CHANNEL pstOutParam = new NET_OUT_SET_CONNECT_CHANNEL();
		
		boolean bRet = netsdkApi.CLIENT_SetConnectChannel(loginHandle, pstInParam, pstOutParam, 30000);
		if(!bRet) {
			System.err.printf("SetConnectChannel Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}

		return true;
	}

	// 获取设备通道信息
	public boolean GetChannelInfo() {

		nLogicChannel = -1;
		NET_IN_GET_CHANNEL_INFO pstInParam = new NET_IN_GET_CHANNEL_INFO();
		System.arraycopy(deviceID.getBytes(), 0, pstInParam.szDeviceID, 0, deviceID.getBytes().length);
		
		NET_GET_CHANNEL_INFO[] channelInfo = new NET_GET_CHANNEL_INFO[NetSDKLib.MAX_DEVICE_CHANNEL_NUM];
		for (int i = 0; i < channelInfo.length; i++) {
			channelInfo[i] = new NET_GET_CHANNEL_INFO();
		}
		
		NET_OUT_GET_CHANNEL_INFO pstOutParam = new NET_OUT_GET_CHANNEL_INFO();
		pstOutParam.nMaxCount = channelInfo.length;
		pstOutParam.pstuChannelInfo = new Memory(channelInfo[0].size() * pstOutParam.nMaxCount);
		pstOutParam.pstuChannelInfo.clear(channelInfo[0].size() * pstOutParam.nMaxCount);
		ToolKits.SetStructArrToPointerData(channelInfo, pstOutParam.pstuChannelInfo);  // 将数组内存拷贝到Pointer
		
		boolean bRet = netsdkApi.CLIENT_GetChannelInfo(loginHandle, pstInParam, pstOutParam, 3000);
		if(!bRet) {
			System.err.printf("GetChannelInfo Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
		
		ToolKits.GetPointerDataToStructArr(pstOutParam.pstuChannelInfo, channelInfo);  // 将 Pointer 的内容 输出到   数组
		for (int i = 0; i < pstOutParam.nRetCount; i++)
		{
			if (nRemoteChannel == channelInfo[i].nRemoteChannel) {
				nLogicChannel = channelInfo[i].nLogicChannel;
				break;
			}
		}

		return nLogicChannel != -1;
	}

	public boolean DetachDeviceState() { // 注销设备状态回调
		
		if (!bAttachStateFlag) {
			return true;
		}
		
		boolean bRet = netsdkApi.CLIENT_DetachDeviceState(l_hAttachStateHandle);
		if(bRet) {
			System.out.println("DetachDeviceState Succeed!");
			bAttachStateFlag = false;
			return true;
		}
		else {
			System.err.printf("DetachDeviceState Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}

	public boolean DetachAddDevice() { // 注销添加设备回调
		
		if (!bAttachFlag) {
			return true;
		}
		
		boolean bRet = netsdkApi.CLIENT_DetachAddDevice(l_hAttachHandle);
		if(bRet) {
			System.out.println("DetachAddDevice Succeed!");
			bAttachFlag = false;
			return true;
		}
		else {
			System.err.printf("DetachAddDevice Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
	
	// 删除设备
	public boolean RemoveDevice(String strDeviceID) {
		
		if (strDeviceID == null) {
			strDeviceID = deviceID;
		}
		
		NET_IN_REMOVE_DEVICE pstInParam = new NET_IN_REMOVE_DEVICE();

		pstInParam.nCount = 1;
		System.arraycopy(strDeviceID.getBytes(), 0, pstInParam.szDeviceIDsArr[0].szDeviceID, 0, strDeviceID.getBytes().length);
		
		NET_OUT_REMOVE_DEVICE pstOutParam = new NET_OUT_REMOVE_DEVICE();
			
		boolean bRet = netsdkApi.CLIENT_RemoveDevice(loginHandle, pstInParam, pstOutParam, 30000);
		if(bRet) {
			System.out.println("RemoveDevice Succeed!");
			deviceID = "";
			return true;
		}
		else {
			System.err.printf("RemoveDevice Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
	
	// 中止添加设备任务
	public boolean CancelTask(int nTaskID) {
		
		NET_IN_CANCEL_ADD_TASK pstInParam = new NET_IN_CANCEL_ADD_TASK();
		
		pstInParam.nTaskID = nTaskID;
		
		NET_OUT_CANCEL_ADD_TASK pstOutParam = new NET_OUT_CANCEL_ADD_TASK();
			
		boolean bRet = netsdkApi.CLIENT_CancelAddDeviceTask(loginHandle, pstInParam, pstOutParam, 10000);
		if(bRet) {
			System.out.println("CancelAddDeviceTask Succeed!");
			return true;
		}
		else {
			System.err.printf("CancelAddDeviceTask Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
	
	// 确认添加设备任务
	public boolean ConfirmTask(int nTaskID) {
		
		NET_IN_CONFIRM_ADD_TASK pstInParam = new NET_IN_CONFIRM_ADD_TASK();
		
		pstInParam.nTaskID = nTaskID;
		
		NET_OUT_CONFIRM_ADD_TASK pstOutParam = new NET_OUT_CONFIRM_ADD_TASK();
			
		boolean bRet = netsdkApi.CLIENT_ConfirmAddDeviceTask(loginHandle, pstInParam, pstOutParam, 10000);
		if(bRet) {
			System.out.println("ConfirmAddDeviceTask Succeed!");
			return true;
		}
		else {
			System.err.printf("ConfirmAddDeviceTask Fail.Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return false;
		}
	}
	
	// 设置人脸识别和检测场景
	public boolean SetFaceScene(String scene) {
		
		String command = NetSDKLib.CFG_CMD_ANALYSEGLOBAL;
		CFG_ANALYSEGLOBAL_INFO msg = new CFG_ANALYSEGLOBAL_INFO(); 

		if(ToolKits.GetDevConfig(loginHandle, nLogicChannel, command, msg)) { // 获取
			
			System.arraycopy(scene.getBytes(), 0, msg.szSceneType, 0, scene.getBytes().length);		
			
			if(ToolKits.SetDevConfig(loginHandle, nLogicChannel, command, msg)) {
				System.out.println("设置场景成功!");
				return true;
			} else {
				System.err.println("设置场景失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取场景失败!" + ToolKits.getErrorCode());
		}
		
		return false;
	}
		
	// 设置人脸识别和检测规则使能
	public boolean SetRuleEnable() {

		String command = NetSDKLib.CFG_CMD_ANALYSERULE;
		
		int ruleCount = 10;  // 事件规则个数
		CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
		for(int i = 0; i < ruleCount; i++) {
			ruleInfo[i] = new CFG_RULE_INFO();
		}
		
		CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO(); 	
		analyse.nRuleLen = 1024 * 1024 * 40;
		analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
		analyse.pRuleBuf.clear(1024 * 1024 * 40);   		
		
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, nLogicChannel, command, analyse)) {
			int offset = 0;
			System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);
			
			int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;
			
			for(int i = 0; i < count; i++) {
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);
				
				offset += ruleInfo[0].size();   // 智能规则偏移量

				switch (ruleInfo[i].dwRuleType) {
					case NetSDKLib.EVENT_IVSS_FACEATTRIBUTE: 	// IVSS人脸检测
					{
						CFG_FACEATTRIBUTE_INFO msg = new CFG_FACEATTRIBUTE_INFO();
						ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
						
						System.out.println("规则名称：" + new String(msg.szRuleName).trim());
						System.out.println("使能：" + msg.bRuleEnable);
						
						// 设置使能开
						msg.bRuleEnable = 1;				
						ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
						break;
					}
					case NetSDKLib.EVENT_IVS_FACEANALYSIS: 	// 人脸分析
					{
						CFG_FACEANALYSIS_INFO msg = new CFG_FACEANALYSIS_INFO();
						ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
						
						System.out.println("规则名称：" + new String(msg.szRuleName).trim());
						System.out.println("使能：" + msg.bRuleEnable);
						
						// 设置使能开
						msg.bRuleEnable = 1;				
						ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
						
						break;
					}
					case NetSDKLib.EVENT_IVSS_FACECOMPARE: 		// IVSS人脸识别
					{
						CFG_FACECOMPARE_INFO msg = new CFG_FACECOMPARE_INFO();
						ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
						
						System.out.println("规则名称：" + new String(msg.szRuleName).trim());
						System.out.println("使能：" + msg.bRuleEnable);
						
						// 设置使能开
						msg.bRuleEnable = 1;				
						ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
						
						break;
					}
					default:
						break;
				}
				
				offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
			}

			// 设置			
			if(ToolKits.SetDevConfig(loginHandle, nLogicChannel, command, analyse)) {
				 System.out.println("设置使能成功!");
				return true;
			} else {
				System.err.println("设置使能失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取使能失败!" + ToolKits.getErrorCode());
		}
		return false;
	}
	
	public static int main() /*throws InterruptedException*/ {
//		LLong loginHandle = new LLong(0); // 替换登陆句柄
//		String url= "dahua://admin:admin@10.33.11.9:37778";
//		AsyncDeviceManager asyncDeviceManager = new AsyncDeviceManager(loginHandle, url);
//		asyncDeviceManager.ExecTask();
//		return asyncDeviceManager.nLogicChannel;
		
		NET_OUT_GET_DEVICE_LIST_INFO stOut = new NET_OUT_GET_DEVICE_LIST_INFO();
		System.out.println(stOut.dwSize);
		return 0;
	}

}
