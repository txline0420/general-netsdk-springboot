package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class SmartLockOld {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	// 登录句柄
	private static LLong loginHandle = new LLong(0);	

	// 登录参数
	String address 			= "172.5.1.37"; 
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin";
	
	// 断线回调，设备断线后，回调会收到数据
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
	
	// 重连回调，设备断线后，设备会自动重连，重连成功后，回调会收到信息
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
	
	public void EndTest() {
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
			loginHandle.setValue(0);
		}
		System.out.println("See You...");

		// 释放SDK资源，在关闭工程时调用
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest() {
		// 初始化SDK库, 设置断线回调
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		// 设置断线自动重连功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
		// 设置登录超时时间和尝试次数，可选
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
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, 
    											nSpecCap, null, deviceInfo, nError);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		
    		EndTest();
    		return;
    	}
    	
    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
	}
	
	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}

	/**
	 * 查询智能锁序列号
	 */
	public void QueryDevSerialNo() {
		// 智能锁网关最多可以连6个
		int nCount = 6;
		
		NET_CODEID_INFO[] codeid = new NET_CODEID_INFO[nCount];
		for(int i = 0; i < nCount; i++) {
			codeid[i] = new NET_CODEID_INFO();
		}
	
		// 查询命令
		int nType = NetSDKLib.NET_DEVSTATE_GET_CODEID_LIST;
		
		// 入参结构体
		NET_GET_CODEID_LIST msg = new NET_GET_CODEID_LIST();
		msg.nStartIndex = 0;
		msg.nQueryNum = nCount;
		
		// 申请内存
		msg.pstuCodeIDInfo = new Memory(codeid[0].size() * nCount);
		msg.pstuCodeIDInfo.clear(codeid[0].size() * nCount);
		
		ToolKits.SetStructArrToPointerData(codeid, msg.pstuCodeIDInfo);
		
    	IntByReference intRetLen = new IntByReference();
    	
    	msg.write();
		boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, msg.getPointer(), msg.size(), intRetLen, 5000);
		msg.read();
		
		if(bRet) {
			// 将获取的信息输出到对应的结构体数组
			ToolKits.GetPointerDataToStructArr(msg.pstuCodeIDInfo, codeid);
			
			// 打印具体信息
			for(int i = 0; i < msg.nRetCodeIDNum; i++) {
				// 老版本的智能锁，用的是通道号来区分锁
				System.out.println("nChannel:" + (codeid[i].nChannel - 1));
			}
		} else {
			System.err.println("查找序列号失败, " + getErrorCode());
		}
	}
	
    /**
     * 卡编号
     */
    private int cardRecordNo = 0;
    
	/**
	 * 添加智能锁用户
	 */
	public void AddSmartLock() {
		NET_RECORDSET_ACCESS_CTL_CARD cardInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		/// 卡号是唯一标识
		final String cardNo = "7FF4F45E";
		System.arraycopy(cardNo.getBytes(), 0, cardInsert.szCardNo, 
				0, cardNo.getBytes().length); 
		
		/// 密码
		final String passwd = "1234567";
		System.arraycopy(passwd.getBytes(), 0, cardInsert.szPsw, 
				0, passwd.getBytes().length);  
		
		// 卡状态
		cardInsert.emStatus = NET_ACCESSCTLCARD_STATE.NET_ACCESSCTLCARD_STATE_NORMAL;
		
		// 卡类型
		cardInsert.emType = NET_ACCESSCTLCARD_TYPE.NET_ACCESSCTLCARD_TYPE_GENERAL;
		
		// 使用次数
		cardInsert.nUserTime = 255;
		
		/// 创建时间
		cardInsert.stuCreateTime.dwYear = 2018;
		cardInsert.stuCreateTime.dwMonth = 11;
		cardInsert.stuCreateTime.dwDay = 16;
		cardInsert.stuCreateTime.dwHour = 11;
		cardInsert.stuCreateTime.dwMinute = 11;
		cardInsert.stuCreateTime.dwSecond = 11;
		
		/// 设置有效时间
		cardInsert.stuValidStartTime.dwYear = 2018;
		cardInsert.stuValidStartTime.dwMonth = 11;
		cardInsert.stuValidStartTime.dwDay = 16;
		cardInsert.stuValidStartTime.dwHour = 11;
		cardInsert.stuValidStartTime.dwMinute = 11;
		cardInsert.stuValidStartTime.dwSecond = 11;
	
		cardInsert.stuValidEndTime.dwYear = 2018; 
		cardInsert.stuValidEndTime.dwMonth = 11;
		cardInsert.stuValidEndTime.dwDay = 18; 
		cardInsert.stuValidEndTime.dwHour = 11;
		cardInsert.stuValidEndTime.dwMinute = 11; 
		cardInsert.stuValidEndTime.dwSecond = 11;
		
		///-- 设置权限
		// 如果有三把锁，对应的通道是 0 1 2，在第一次添加的时候，全部添加。
		// 如果修改，删除2，只需要重新下发，对应通道填 0  1，卡号跟之前的一样即可
		// 不管添加、删除都有事件返回，收到事件了，才是真正的设置成功
		cardInsert.nDoorNum = 2; 
		cardInsert.sznDoors[0] = 0; // 填写通道号，对应网关里的智能锁
		cardInsert.sznDoors[0] = 1; // 填写通道号，对应网关里的智能锁
		
		cardInsert.nTimeSectionNum = 1; 
		cardInsert.sznTimeSectionNo[0] = 255; // 填写通道号，对应网关里的智能锁
		
		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
		insert.stuCtrlRecordSetInfo.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD; // 门禁卡
		insert.stuCtrlRecordSetInfo.pBuf = cardInsert.getPointer();
		
		cardInsert.write();
		insert.write();
		boolean success = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, 
														 insert.getPointer(), 5000);
		insert.read();
		cardInsert.read();
		
		if(!success) {
			System.err.printf("Insert Card Failed.  %x \n", netsdkApi.CLIENT_GetLastError());
			return;
		}
		
		// 返回的为CardNo 的10进制：即插入卡号3344 成功, 返回13124
		cardRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
		System.out.println(cardRecordNo);
	}
	
	/**
	 * 删除智能锁用户
	 */
	public void RemoveSmartLock() {	
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
    	remove.pBuf = new IntByReference(2146759774).getPointer();   // 记录集编号
	
    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.println("删除智能锁用户失败, " + getErrorCode());
    		return;
    	} else {
    		System.out.println("删除智能锁用户成功!");
    	}
	}
	
	/**
	 * 根据记录集编号，查询对应的锁
	 */
	public void GetSmartLock() {
		NET_RECORDSET_ACCESS_CTL_CARD condition = new NET_RECORDSET_ACCESS_CTL_CARD();
		// 记录集编号
		condition.nRecNo = 2146759774;
		
		int nType = NetSDKLib.NET_DEVSTATE_DEV_RECORDSET;
		NET_CTRL_RECORDSET_PARAM record = new NET_CTRL_RECORDSET_PARAM();
		record.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
		record.pBuf = condition.getPointer();
		
    	IntByReference intRetLen = new IntByReference();
    	
    	condition.write();
    	record.write();
		boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, record.getPointer(), record.size(), intRetLen, 3000);
		record.read();
		condition.read();
		
		if(bRet) {		
			if(condition.nDoorNum != 0) {
				for(int i = 0; i < condition.nDoorNum; i++) {
					System.out.println("Chn:" + String.valueOf(condition.sznDoors[i]));  // 对应智能锁
				}			
			} else {
				System.out.println("不存在");
			}
		} else {
			System.err.println("QueryDev Failed!" + netsdkApi.CLIENT_GetLastError());
		}
	}
	
	/**
	 * 订阅
	 * @return
	 */
	public void startListen() {		
	    // 设置报警回调函数
		netsdkApi.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);
		
		// 订阅报警
		boolean bRet = netsdkApi.CLIENT_StartListenEx(loginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
		}
		else {
			System.out.println("订阅报警成功.");
		}
	}
	
	/**
	 * 取消订阅
	 * @return
	 */
	public void stopListen() {
		// 停止订阅报警
		boolean bRet = netsdkApi.CLIENT_StopListen(loginHandle);
	   	if (bRet) {
	   		System.out.println("取消订阅报警信息.");
	   	}	
	}
	
	/**
	 * 报警信息回调函数原形,建议写成单例模式
	 */
	private static class fAlarmDataCB implements fMessCallBack{
		private fAlarmDataCB(){}
		
		private static class fAlarmDataCBHolder {
			private static fAlarmDataCB callback = new fAlarmDataCB();
		}
		
		public static fAlarmDataCB getCallBack() {
			return fAlarmDataCBHolder.callback;
		}
		
	  	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,  NativeLong nDevicePort, Pointer dwUser){
//	  		System.out.printf("command = %x\n", lCommand);
	  		switch (lCommand)
	  		{
	  		case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: {  // 门禁事件
				ALARM_ACCESS_CTL_EVENT_INFO msg = new ALARM_ACCESS_CTL_EVENT_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);

				System.out.println("开门失败的原因：" + msg.nErrorCode);  // 见注释		
				
				// 密码开锁
				if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_PWD_ONLY) {
					System.out.println("密码开锁");
				}
				// 刷卡开锁 
				else if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_CARD) {
					System.out.println("刷卡开锁");
				}
				// 指纹开锁
				else if(msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_FINGERPRINT) {
					System.out.println("指纹开锁");
				}
				
				System.out.println("智能锁序列号：" + new String(msg.szSN).trim());
				break;
			}
	  		case NetSDKLib.NET_ALARM_ACCESS_CARD_OPERATE: {  // 门禁卡数据操作事件,插入、删除等操作会有事件上报
	  			ALARM_ACCESS_CARD_OPERATE_INFO msg = new ALARM_ACCESS_CARD_OPERATE_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				
				System.out.println("卡号:" + new String(msg.szCardNo).trim());
				
				if(msg.emActionType == 1) {
					System.out.println("插入！");
				} else if(msg.emActionType == 2) {
					System.out.println("修改！");
				} else if(msg.emActionType == 3) {
					System.out.println("删除！");
				}
				
				if(msg.emResult == 0) {
					System.out.println("失败！");
				} else if(msg.emResult == 1) {
					System.out.println("成功！");
				}
				
				System.out.println("通道:" + msg.nChannelID);
				
	  			break;
	  		}
	  		case NetSDKLib.NET_ALARM_MODULE_LOST: {  // 扩展模块掉线事件,通过此事件来获取锁有没有掉线
	  			ALARM_MODULE_LOST_INFO msg = new ALARM_MODULE_LOST_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
	  			
				System.out.println("上报时间:" + msg.stuTime.toStringTimeEx());
				System.out.println("在线情况:" + (msg.bOnline == 1? "在线":"离线"));
				System.out.println("锁序号:" + new String(msg.szSN).trim());
	  			break;
	  		}
	  		default:
	  			break;
	  		} 		
	  		return true;
		}
	}
	
	
	public void RunTest() {
		CaseMenu menu = new CaseMenu();	
		
		menu.addItem(new CaseMenu.Item(this , "查询智能锁序列号", "QueryDevSerialNo"));	
		
		menu.addItem(new CaseMenu.Item(this , "添加智能锁用户", "AddSmartLock"));	
		menu.addItem(new CaseMenu.Item(this , "删除智能锁用户", "RemoveSmartLock")); 
		
		// 事件订阅
		menu.addItem(new CaseMenu.Item(this , "订阅事件", "startListen"));	
		menu.addItem(new CaseMenu.Item(this , "取消订阅", "stopListen")); 
		
		// 获取和修改目前暂未实现
		menu.addItem(new CaseMenu.Item(this , "获取智能锁用户", "GetSmartLock"));	
		menu.addItem(new CaseMenu.Item(this , "修改智能锁用户", "ModifySmartLock"));  	
		
		menu.run();
	}

	
	public static void main(String[]args) {	
		SmartLockOld demo = new SmartLockOld();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
