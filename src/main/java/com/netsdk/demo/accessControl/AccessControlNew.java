package com.netsdk.demo.accessControl;

import com.netsdk.demo.util.Base64Util;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class AccessControlNew {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);

	String address 			= "172.23.12.248"; // 172.26.6.104
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin123";
	
	private boolean bFaceOperate = true;	// 控制是否带人脸操作
	private AccessFaceOperate accessFaceOperate = null;
	
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
	
	public void EndTest() {
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			stopListen();
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
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
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username,
				password, nSpecCap, null, deviceInfo, nError);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		EndTest();
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
    	
    	accessFaceOperate = new AccessFaceOperate(loginHandle);
	}
    
	
	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	

    /**
	 * 订阅报警信息
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
	 * 取消订阅报警信息
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
	  		case NetSDKLib.NET_ALARM_FINGER_PRINT:   {  // 获取指纹事件
	  			final ALARM_CAPTURE_FINGER_PRINT_INFO msg = new ALARM_CAPTURE_FINGER_PRINT_INFO();
	  			ToolKits.GetPointerData(pStuEvent, msg);
	  			
	  			System.out.println("门通道号(从0开始):" + msg.nChannelID);
	  			System.out.println("事件时间:" + msg.stuTime.toStringTime());
	  			System.out.println("门读卡器ID:" + new String(msg.szReaderID).trim());
	  			
	  			int length = msg.nPacketNum * msg.nPacketLen;
	  			byte[] buffer = new byte[length];
  				msg.szFingerPrintInfo.read(0, buffer, 0, length);
	  			
  				String figerStr = Base64Util.getEncoder().encodeToString(buffer); // 将获取到的指纹转成没有乱码的字符串
  				System.out.println("指纹数据:" + figerStr);

	  			break;
	  		}
	  		case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: {     // 门禁事件
				ALARM_ACCESS_CTL_EVENT_INFO msg = new ALARM_ACCESS_CTL_EVENT_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println(msg.toString());
				
				if (msg.nErrorCode == 0x10) {
					// 密码开门
					if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_PWD_ONLY) {
						System.out.println("密码开门失败");
					}
					else if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_CARD) {
					// 刷卡开门  - (1202B-D 的 二维码方式)
						System.out.println("刷卡方式失败");
					}
				}

				/// 触发开门
//				new Thread(new Runnable() {
//					@Override
//					public void run() {
//						openDoor();
//					}
//				}).start();
				
				break;
			}
	  		case NetSDKLib.NET_ALARM_ACCESS_CTL_STATUS: { 	// 门禁状态事件
	  			ALARM_ACCESS_CTL_STATUS_INFO msg = new ALARM_ACCESS_CTL_STATUS_INFO();
	  			ToolKits.GetPointerData(pStuEvent, msg);
	  			
	  			System.out.println("门通道号:" + msg.nDoor);
	  			System.out.println("事件发生的时间:" + msg.stuTime.toStringTime());
	  			
	  			if(msg.emStatus == 1) {
	  				System.out.println("门禁状态 : 开门.");
	  			} else if(msg.emStatus == 2) {
	  				System.out.println("门禁状态 : 关门.");
	  			}			
	  			
	  			break;
	  		}
	  		default:
	  			break;
	  		} 		
	  		return true;
		}
	}
	
	/**
	 * 智能订阅
	 */
	public void realLoadPicture() {
		accessFaceOperate.realLoadPicture();
	}
	
	/**
	 * 取消智能订阅
	 */
	public void stopRealLoadPicture() {
		accessFaceOperate.Detach();
	}
	
	////////////////////////////////////开关门 ///////////////////////////////////////
	
	/**
	 * 开门
	 */
	public static void openDoor() {
		NET_CTRL_ACCESS_OPEN open = new NET_CTRL_ACCESS_OPEN();
		open.nChannelID = 0;
		
		open.write();
		boolean openSuccess = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_ACCESS_OPEN, open.getPointer(), null, 5000);
		open.read();
		
		if (!openSuccess) {
			System.err.println("open error: 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
		}
	}
	
	/**
	 * 关门 
	 */
    public void closeDoor() {
    	final NET_CTRL_ACCESS_CLOSE close = new NET_CTRL_ACCESS_CLOSE();
    	close.nChannelID = 0; // 对应的门编号 - 如何开全部的门
    	close.write();
    	boolean result = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, 
					    			CtrlType.CTRLTYPE_CTRL_ACCESS_CLOSE,
					    			close.getPointer(),
					    			null,
					    			5000);
    	close.read();
    	if (!result) {
    		System.err.println("close error: 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
    	}
    }
    
    /**
     * 查询门（开、关）状态
     */
    public void queryDoorStatus() {
    	int cmd = NetSDKLib.NET_DEVSTATE_DOOR_STATE;
		NET_DOOR_STATUS_INFO doorStatus = new NET_DOOR_STATUS_INFO();
		IntByReference retLenByReference = new IntByReference(0);
		
		doorStatus.write();
		boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, 
												cmd, 
												doorStatus.getPointer(), 
												doorStatus.size(),
												retLenByReference,
												3000);
		doorStatus.read();
		if (!bRet) {
			System.err.println("Failed to queryDoorStatus. Error Code 0x" 
					+ Integer.toHexString(netsdkApi.CLIENT_GetLastError()));
			return;
		}
		
		String stateType[] = {"未知", "门打开", "门关闭", "门异常打开"};
		System.out.println("doorStatus -> Channel: " + doorStatus.nChannel
					+ " type: " + stateType[doorStatus.emStateType]);
    }
    	
	//////////////////////////////////// 下发指纹 //////////////////////////////////////
	/**
	 * 指纹采集
	 * @param nChannelID   门禁序号(从开始)
	 * @param szReaderID   读卡器ID, 值为2
	 */
	public void captureFingerprint() {
		int nChannelID = 0;
		String szReaderID = "2";
		
		NET_CTRL_CAPTURE_FINGER_PRINT captureFingerprint = new NET_CTRL_CAPTURE_FINGER_PRINT();
		captureFingerprint.nChannelID = nChannelID; // 门禁序号(从开始)
		System.arraycopy(szReaderID.getBytes(), 0, captureFingerprint.szReaderID, 0, szReaderID.getBytes().length);  // 读卡器ID
		
		captureFingerprint.write();
		boolean openSuccess = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_CAPTURE_FINGER_PRINT, captureFingerprint.getPointer(), null, 5000);
		captureFingerprint.read();
		
		if (!openSuccess) {
			System.err.println("CaptureFingerprint Failed." + getErrorCode());
		}
	}
	
	/**
	 * 插入指纹
	 */
	public void insertFingerprint() {
		String finggerStr = "xR5jAAOIEiX/NCfFhIliVhAHpkYeiirZ+HP4g4WIyu43S4ZJVoZ7UshNtwgxibuh+C4mxkqGU67RD8YFM4pryfhyFoY/h7Pt4JYoijKGpIIZS6nGtYbkfQiyOUe5iLSB30WZBEKJtIr4MheEVogkwdYvygi3ibTdJ8XIg7eHZPbgvg8bSoi89eZ0KQSuh2ytCM5LUaeHFQ0hvk+Wh4oiFRf/t4RrhuIhB8n2wyOH+5XZ4Gh2NIfLyeEcSJZLhUwd18f2BEWFXIHXxfaEx4ddGs9/v5hHiaVe+Ln7Ak+IZar/9/rDwoklthf/y0IIiGH6/zIlQwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGEYMyNXZEMndoR2EWUzZiaEM+qTSHYksRJ2QyUyMxAyM0TxJFdPFGMiVVEz5Slkl4wyFkMRMfJzGQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaY+xaNHQFhcwqM2BVghxXQISYjTsaA2O6YagjEqOekUxGA2IWkYAKcBSYcukRANDstPJOMhHlkUUbEJCsolMRRcHtYZVgM7TJYr0VEDQB9u8sIDRTcRZcEMGJkbsZIXUT9XwuFHVGYVwFMRPg9oENMRS9YS9kIDGrgF1sNWNHgG12ElNnYW9xQ0IxcY0CIVIlsGVdNMGeoE11UlQRUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABYIFQYLCgQRCQ0FAw8MEBIHGRcOAgEAGhsdAAAAAP/K";
		
		byte[] szFingerPrintInfo = Base64Util.getDecoder().decode(finggerStr);   // 将字符串转为指纹数据
		
		// 门禁卡记录集信息
		NET_RECORDSET_ACCESS_CTL_CARD accessInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		final String cardNo = "1011"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "1123"; /// 用户ID
		final String role = "责任人"; // 角色
    	setPublicInfo(cardNo, useId, role, accessInsert);
		
		// 启用成员 stuFingerPrintInfoEx
		accessInsert.bEnableExtended = 1; // 1-true, 0-false
		
		// 指纹数据信息, 先申请内存
		accessInsert.stuFingerPrintInfoEx.nLength = szFingerPrintInfo.length;        			// 单个数据包长度,单位字节
		accessInsert.stuFingerPrintInfoEx.nCount = 1;         									// 包个数
		accessInsert.stuFingerPrintInfoEx.nPacketLen = szFingerPrintInfo.length;       			// pPacketData 指向内存区的大小，用户填写
		accessInsert.stuFingerPrintInfoEx.pPacketData = new Memory(szFingerPrintInfo.length);   // 所有指纹数据包, 用户申请内存,大小至少为nLength * nCount
		accessInsert.stuFingerPrintInfoEx.pPacketData.clear(szFingerPrintInfo.length);  
		
		// 指纹数据包
		accessInsert.stuFingerPrintInfoEx.pPacketData.write(0, szFingerPrintInfo, 0, szFingerPrintInfo.length);
		
		accessInsert.nDoorNum = 1; // 门个数 表示双门控制器
		accessInsert.sznDoors[0] = 0; // 表示第一个门有权限
		accessInsert.nTimeSectionNum = 1; // 与门数对应
		accessInsert.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		
		// 创建时间
		accessInsert.stuCreateTime.setTime(2019, 4, 26, 9, 9, 9);
		
		// 使用次数
		accessInsert.nUserTime = 200;
		
		// 有效开始/结束时间
		accessInsert.bIsValid = 1;
		accessInsert.stuValidStartTime.setTime(2019, 4, 26, 0, 0, 0);
		accessInsert.stuValidEndTime.setTime(2019, 5, 26, 0, 0, 0);
		
		if(!AccessCtlOperate.insert(accessInsert)) {
			System.err.println("insert password failed." + getErrorCode());
			return;
		} 
	}
	
	/**
	 * 更新指纹
	 */
	public void updateFingerprint() {
		String finggerStr = "xTxpAASFsk3+8hoDh4ky0ghH2oR7hjp658Wp" +
				"Q4eJQyEQBdoFgIerDuhFuAQGhsr6BvQpguuF804HMjpFaIUjOQ" +
				"AJyUSGiAwlCEupSGSFnA3gS7nF2IUUVe62SYVXhcSBoJG5iGSGvJqI" +
				"lZsRkYhksjBNnUmjh4VOEFEpizyGVUnol4oQQohVft6sOQdWh93lzarniBeJK" +
				"4EANhtCKoqEmuA0C4M+im2+/zPJAzCKrcX5OipCtIpF9g6DqQOth32CAA8bUiiGdbog0wqKWofGYtxwecgQhsZi" +
				"ONMO3VqH5p7b8kmGiooaSQfF/MEWilKZ+Dgeg4iJuwYQBgyDJ4uEMfg3/kOsi53VB/++RTaLbf74N/uFY4NzPegFyQNfg3Ot6EXawda" +
				"CO63/fhpAWIS0YehJyoRSg9SR+IfYRFCCbQXnxcrBR4RtTeCJykPKgrU5/rYagr6EvbIOeDhCOoWt5dB/yI" +
				"dDgd3FB8f4BEGDpf3oR/oDQYImAQfJ+kM+iy5x+HXMRUaJznnQtCrGNoteifk4CkUohZaWyIepBTGEdsLX/9qBHoZO0pD" +
				"JGog6iybq+Tn3x1yH7vrbco2HN4R+8s//+EIhhbcW18f3RZqGnx4LfhvXO4qvOvlB+0qQhpc9/L5MVlJH878zUyMvjxF2YSIlVRJUamE" +
				"lNH8nRUGD8niPUp/xI6f28/czRPMPMfH2QvIvQSNEcyNB/zIyRSM/8zXzUvMzYfNFQ/IzUh9CNSf1X//xL0U6MohGebIWlDNWhRZi/xIVgkVP+Hhn" +
				"9f9RAUWhRi8eafk+QXSytDwtFjGCklxSMTK8pNZYFILYkrkkYmS30qhUEcKqtNswRwKFkq8QQSHHkRBtESNRkfhdEbK9ovslIVKMoYYdMtGdgSM0" +
				"ERE1cnIOQQTYoLJkIQJ0gJsNFFTugetWEhME5zESEnLokpsbFFS+kYVfVLGbs" +
				"E4qQTaYgcEOIRPEkfQRJGDhkWsOEmXWoLdpRKKNoA5tRENBoM" +
				"5GEFEkosISZBLUkKlXIHbdkGRRAQHRoAwLDw4NFwgJChglKxEoKiYaGQcSBDIbIy0pNBQWFVVs";
		
		byte[] szFingerPrintInfo = Base64Util.getDecoder().decode(finggerStr);   // 将字符串转为指纹数据
		
		// 门禁卡记录集信息
		NET_RECORDSET_ACCESS_CTL_CARD accessUpdate = new NET_RECORDSET_ACCESS_CTL_CARD();
		accessUpdate.nRecNo = AccessCtlOperate.getRecordNo();  // 需要修改的记录集编号,由插入获得
		
		final String cardNo = "1011"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "1123"; /// 用户ID
		final String role = "负责人"; // 角色
    	setPublicInfo(cardNo, useId, role, accessUpdate);
		
		// 启用成员 stuFingerPrintInfoEx
		accessUpdate.bEnableExtended = 1; // 1-true, 0-false
		
		// 指纹数据信息, 先申请内存
		accessUpdate.stuFingerPrintInfoEx.nLength = szFingerPrintInfo.length;        							// 单个数据包长度,单位字节
		accessUpdate.stuFingerPrintInfoEx.nCount = 1;         							// 包个数
		accessUpdate.stuFingerPrintInfoEx.nPacketLen = szFingerPrintInfo.length;       			// pPacketData 指向内存区的大小，用户填写
		accessUpdate.stuFingerPrintInfoEx.pPacketData = new Memory(szFingerPrintInfo.length);    // 所有指纹数据包, 用户申请内存,大小至少为nLength * nCount
		accessUpdate.stuFingerPrintInfoEx.pPacketData.clear(szFingerPrintInfo.length); 
		
		// 指纹数据包
		accessUpdate.stuFingerPrintInfoEx.pPacketData.write(0, szFingerPrintInfo, 0, szFingerPrintInfo.length);
		
		accessUpdate.bIsValid = 1;
		accessUpdate.stuValidStartTime.setTime(2019, 4, 26, 0, 0, 0);
		accessUpdate.stuValidEndTime.setTime(2019, 5, 26, 10, 10, 10);
		
		if (!AccessCtlOperate.update(accessUpdate)) {
			System.err.println("update password failed." + getErrorCode());
    	}
		
    	System.out.println("update fingerprint success.");
	}
	
	/**
	 * 删除指纹
	 */
	public void deleteFingerprint() {
  
    	if(!AccessCtlOperate.delete(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD, AccessCtlOperate.getRecordNo())){
    		System.err.println("delete fingerprint failed." + getErrorCode());
    	} 
    	
    	System.out.println("delete fingerprint success.");
	}
	
	/**
	 * 清除记录
	 */
	public void clearFingerprint() {
    	if(!AccessCtlOperate.clear(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD)){
    		System.err.println("clear fingerprint failed." + getErrorCode());
    	}
    	
    	System.out.println("clear fingerprint success.");
	}
    
    ////////////////////////////////////// 发卡操作  //////////////////////////////////////////
    /**
     * 插入卡信息
     */
    public void insertCard() {

		NET_RECORDSET_ACCESS_CTL_CARD cardInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		final String cardNo = "8190D743"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "123456"; /// 用户ID
		final String role = "经理"; // 角色
    	setPublicInfo(cardNo, useId, role, cardInsert);
		
		/// 设置有效时间
		cardInsert.stuValidStartTime.dwYear = 2019;
		cardInsert.stuValidStartTime.dwMonth = 4;
	
		cardInsert.stuValidEndTime.dwYear = cardInsert.stuValidStartTime.dwYear + 10; // 10 年有效
		cardInsert.stuValidEndTime.dwMonth = 4;
		
		///-- 设置开门权限
		cardInsert.nDoorNum = 2; // 门个数 表示双门控制器
		cardInsert.sznDoors[0] = 0; // 表示第一个门有权限
		cardInsert.sznDoors[1] = 1; // 表示第二个门有权限
		cardInsert.nTimeSectionNum = 2; // 与门数对应
		cardInsert.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		cardInsert.sznTimeSectionNo[1] = 255; // 表示第二个门全天有效
						
		/// 卡类型使用： 设置成来宾卡可以使能刷卡有效次数
//		cardInsert.emType = NET_ACCESSCTLCARD_TYPE.NET_ACCESSCTLCARD_TYPE_GUEST;
//		cardInsert.nUserTime = 10; // 表示刷卡成功10次后将失效
		
		if(!AccessCtlOperate.insert(cardInsert)) { // 返回的为CardNo 的10进制：即插入卡号3344 成功, 返回13124
    		System.err.println("Insert Card Failed." + getErrorCode());
			return;
		}
		// 下发人脸
		faceOperate(EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_ADD, useId);
    }
    
    /**
     * 更新卡信息:
     * 主要：不能更新卡号，原来插入的信息也要保留，仅修改对应的字段
     * 
     */
    public void updateCard() {
    	
    	NET_RECORDSET_ACCESS_CTL_CARD cardUpdate = new NET_RECORDSET_ACCESS_CTL_CARD();
    	
    	/// 注意：原来插入卡的字段要保留
    	cardUpdate.nRecNo = AccessCtlOperate.getRecordNo();
    	
    	final String cardNo = "8190D743"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "123456"; /// 用户ID
		final String role = "领导"; // 角色
    	setPublicInfo(cardNo, useId, role, cardUpdate);
    	
		/// 设置有效时间
		cardUpdate.stuValidStartTime.dwYear = 2017;
		cardUpdate.stuValidStartTime.dwMonth = 11;
	
		cardUpdate.stuValidEndTime.dwYear = cardUpdate.stuValidStartTime.dwYear + 10; // 10 年有效
		cardUpdate.stuValidEndTime.dwMonth = 11;
    	    	
		///-- 修改: 门的权限及时间段, 仅第一个门有效
		cardUpdate.nDoorNum = 1; // 门个数 表示双门控制器
		cardUpdate.sznDoors[0] = 0; // 表示第一个门有权限
		cardUpdate.nTimeSectionNum = 1; // 与门数对应
		cardUpdate.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		
		if (!AccessCtlOperate.update(cardUpdate)) {
    		System.err.println("Update Card Failed." + getErrorCode());
			return;
    	}
		
		System.out.println("Update Card Success.");
		
		faceOperate(EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_UPDATE, useId);
    }
    
    /**
     * 删除卡信息
     */
    public void deleteCard() {
    			
    	if(!AccessCtlOperate.delete(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD, AccessCtlOperate.getRecordNo())){
    		System.err.println("delete Card/Fingerprint Failed." + getErrorCode());
    		return;
    	}
    	
    	System.out.println("delete Card/Fingerprint RecordNo " + AccessCtlOperate.getRecordNo() + " Success!");
    	
    	final String useId = "123456"; /// 用户ID
		faceOperate(EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_REMOVE, useId);
    }
    
    /**
     * 清除卡
     */
    public void clearCard() {
    	if(!AccessCtlOperate.clear(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD)) {
    		System.err.println("Clear Card/Fingerprint Failed." + getErrorCode());
    		return;
    	}
		System.out.println("Clear Card/Fingerprint Success.");
		
		faceOperate(EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_CLEAR, null);
    }
    
    public void setPublicInfo(String cardNo, String useId, String role, NET_RECORDSET_ACCESS_CTL_CARD accessCard) {
		
		System.arraycopy(cardNo.getBytes(), 0, accessCard.szCardNo, 
				0, cardNo.getBytes().length); 
		
		System.arraycopy(useId.getBytes(), 0, accessCard.szUserID, 
				0, useId.getBytes().length); 
    	try {
    		
    		accessCard.emSex = NET_ACCESSCTLCARD_SEX.NET_ACCESSCTLCARD_SEX_MALE;
			System.arraycopy(role.getBytes("GBK"), 0, accessCard.szRole, 0, role.getBytes("GBK").length);
			String temp = "H/010-1800"; // 项目编号
			System.arraycopy(temp.getBytes(), 0, accessCard.szProjectNo, 0, temp.getBytes().length);
			temp = "河源恒大华府"; // 项目名称
			System.arraycopy(temp.getBytes("GBK"), 0, accessCard.szProjectName, 0, temp.getBytes("GBK").length);
			temp = "中建五局"; // 施工单位全称
			System.arraycopy(temp.getBytes("GBK"), 0, accessCard.szBuilderName, 0, temp.getBytes("GBK").length);
			temp = "001";
			System.arraycopy(temp.getBytes(), 0, accessCard.szBuilderID, 0, temp.getBytes().length);
			temp = "002";
			System.arraycopy(temp.getBytes(), 0, accessCard.szBuilderType, 0, temp.getBytes().length);
			temp = "003";
			System.arraycopy(temp.getBytes(), 0, accessCard.szBuilderTypeID, 0, temp.getBytes().length);
			temp = "004";
			System.arraycopy(temp.getBytes(), 0, accessCard.szPictureID, 0, temp.getBytes().length);
			temp = "005";
			System.arraycopy(temp.getBytes(), 0, accessCard.szContractID, 0, temp.getBytes().length);
			temp = "007"; // 工种ID
			System.arraycopy(temp.getBytes(), 0, accessCard.szWorkerTypeID, 0, temp.getBytes().length);
			temp = "MI6特工"; // 工种名称
			System.arraycopy(temp.getBytes("GBK"), 0, accessCard.szWorkerTypeName, 0, temp.getBytes("GBK").length);
			
			temp = "詹姆斯邦德"; // 卡命名
			System.arraycopy(temp.getBytes("GBK"), 0, accessCard.szCardName, 0, temp.getBytes("GBK").length);
			
			accessCard.bPersonStatus = 0;
		}catch (UnsupportedEncodingException e) {
			System.err.println("...UnsupportedEncodingException...");
		}
    }
    
    private void faceOperate(int emType, String userId) {
    	
    	if (!bFaceOperate) {
    		return;
    	}
    	
    	// 图片路径
    	String imagePath = "d:/31289.jpg";
    	
    	switch (emType) {
    		case EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_ADD:
    	    	accessFaceOperate.addFaceInfo(userId, imagePath);
    	    	break;
    		case EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_UPDATE:
    			accessFaceOperate.updateFaceInfo(userId, imagePath);
    	    	break;
    		case EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_REMOVE:
    			accessFaceOperate.removeFaceInfo(userId);
    	    	break;
    		case EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_CLEAR:
    			accessFaceOperate.clearFaceInfo();
    	    	break;
    	}
    }
    
    ////////////////////////////////////// 查询卡记录集  ///////////////////////////////////////
    /**
     * 按记录集编号查询卡
     */
    public void queryCardByRecNo() {
    	
		NET_RECORDSET_ACCESS_CTL_CARD accessCard = new NET_RECORDSET_ACCESS_CTL_CARD();
		// 记录集编号
		accessCard.nRecNo = AccessCtlOperate.getRecordNo();
		
		if (!AccessCtlOperate.queryRecordState(accessCard)) {
    		System.err.println("Query Card By RecNo Failed." + getErrorCode());
    		return;
		}
	}
    
    /**
     * 按卡号查询卡信息
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryCardByCardNo() {
    	final FIND_RECORD_ACCESSCTLCARD_CONDITION  queryCondition = new FIND_RECORD_ACCESSCTLCARD_CONDITION();
    	queryCondition.abCardNo = 1;
    	final String cardNo = "1011";
    	System.arraycopy(cardNo.getBytes(), 0, queryCondition.szCardNo, 0, cardNo.length());
    	
    	queryAccessCard(queryCondition);
    }
    
    /**
     * 查询所有卡
     */
    public void queryAllCard() {
    	final FIND_RECORD_ACCESSCTLCARD_CONDITION  queryCondition = new FIND_RECORD_ACCESSCTLCARD_CONDITION();
    	queryAccessCard(queryCondition);
    }
    
    /**
     * 查询门禁刷卡记录
     */
    public void queryAccessCard(FIND_RECORD_ACCESSCTLCARD_CONDITION queryCondition) {
    	
  		/// 1202B-D 不支持该功能
  		// System.out.println("Total Record Count: " + getTotalRecordCount(findRecordOut.lFindeHandle));

		final int nRecordCount = 10;  // 每次查询的最大个数, 1202B-D 最多支持10条
		///门禁刷卡记录记录集信息
		NET_RECORDSET_ACCESS_CTL_CARD[] records = new NET_RECORDSET_ACCESS_CTL_CARD[nRecordCount];
		for(int i = 0; i < nRecordCount; i++) {
			records[i] = new NET_RECORDSET_ACCESS_CTL_CARD();
		}

		AccessCtlOperate.queryRecord(queryCondition, records);
    }
    
    private static void printRecord(NET_RECORDSET_ACCESS_CTL_CARD record) {
		System.out.println("记录集编号:" + record.nRecNo);
		System.out.println("卡号:" + new String(record.szCardNo).trim());
		System.out.println("用户ID:" + new String(record.szUserID).trim());
		System.out.println("开始时间:" + record.stuValidStartTime.toStringTime());
		System.out.println("结束时间:" + record.stuValidEndTime.toStringTime());
		try {
			System.out.println("卡命名:" + new String(record.szCardName, "GBK").trim());
			System.out.println("角色:" + new String(record.szRole, "GBK").trim());
			System.out.println("项目编号:" + new String(record.szProjectNo).trim());
			System.out.println("项目名称:" + new String(record.szProjectName, "GBK").trim());
			System.out.println("施工单位全称:" + new String(record.szBuilderName, "GBK").trim());
			System.out.println("工种名称:" + new String(record.szWorkerTypeName, "GBK").trim());
		}catch(UnsupportedEncodingException e) {
			System.err.println("...UnsupportedEncodingException...");
		}
    }    
    
    ////////////////////////////////////// 门禁出入记录操作  //////////////////////////////////////////
    /**
     * 插入门禁出入记录
     */
    public void insertRecord() {
    	NET_RECORDSET_ACCESS_CTL_CARDREC recordInsert = new NET_RECORDSET_ACCESS_CTL_CARDREC();
   
    	final String cardNo = "7FFFFFFF"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "1122"; /// 用户ID
		final String role = "民工"; // 角色
    	setPublicInfo(cardNo, useId, role, recordInsert);
		
    	// 记录集编号
    	recordInsert.nRecNo = AccessCtlOperate.getRecordNo();
    	
		/// 刷卡时间
		recordInsert.stuTime.setTime(2019, 4, 25, 17, 17, 17);

		recordInsert.emMethod = NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_CARD; // 卡
		
		if(!AccessCtlOperate.insert(recordInsert)) { // 返回的为CardNo 的10进制：即插入卡号3344 成功, 返回13124
    		System.err.println("Insert Record Failed." + getErrorCode());
			return;
		}
    }
    
    /**
     * 更新门禁出入记录:
     */
    public void updateRecord() {
    	
    	NET_RECORDSET_ACCESS_CTL_CARDREC recordUpdate = new NET_RECORDSET_ACCESS_CTL_CARDREC();
    	
    	/// 注意：原来插入的字段要保留
    	recordUpdate.nRecNo = AccessCtlOperate.getRecordNo();
    	
    	final String cardNo = "7FFFFFFF"; /// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String useId = "1122"; /// 用户ID
		final String role = "干事"; // 角色
    	setPublicInfo(cardNo, useId, role, recordUpdate);
    	
		/// 设置刷卡时间
		recordUpdate.stuTime.setTime(2019, 4, 25, 18, 18, 18);

		recordUpdate.emMethod = NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_FACE_RECOGNITION; // 人脸识别
		
		if (!AccessCtlOperate.update(recordUpdate)) {
    		System.err.println("Update Record Failed." + getErrorCode());
			return;
    	}
		
		System.out.println("Update Record Success.");
    }
    
    /**
     * 删除门禁出入记录
     */
    public void deleteRecord() {
    			
    	if(!AccessCtlOperate.delete(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX, AccessCtlOperate.getRecordNo())){
    		System.err.println("delete Record Failed." + getErrorCode());
    		return;
    	}
    	
    	System.out.println("delete Record RecordNo " + AccessCtlOperate.getRecordNo() + " Success!");
    }
    
    /**
     * 清除门禁出入记录
     */
    public void clearRecord() {
    	if(!AccessCtlOperate.clear(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX)) {
    		System.err.println("Clear Record Failed." + getErrorCode());
    		return;
    	}
		System.out.println("Clear Record Success.");
    }   
    
    public void setPublicInfo(String cardNo, String useId, String role, NET_RECORDSET_ACCESS_CTL_CARDREC cardRecord) {
		
    	if (cardNo != null) {
    		System.arraycopy(cardNo.getBytes(), 0, cardRecord.szCardNo, 
    				0, cardNo.getBytes().length); 
    	}
		
    	if (useId != null) {
    		System.arraycopy(useId.getBytes(), 0, cardRecord.szUserID, 
    				0, useId.getBytes().length);
    	}
		 
    	try {
    		
    		if (role != null) {
    			System.arraycopy(role.getBytes("GBK"), 0, cardRecord.szRole, 0, role.getBytes("GBK").length);
    		}
			String temp = "H/010-1800"; // 项目编号
			System.arraycopy(temp.getBytes(), 0, cardRecord.szProjectNo, 0, temp.getBytes().length);
			temp = "河源恒大华府"; // 项目名称
			System.arraycopy(temp.getBytes("GBK"), 0, cardRecord.szProjectName, 0, temp.getBytes("GBK").length);
			temp = "中建五局"; // 施工单位全称
			System.arraycopy(temp.getBytes("GBK"), 0, cardRecord.szBuilderName, 0, temp.getBytes("GBK").length);

		}catch (UnsupportedEncodingException e) {
			System.err.println("...UnsupportedEncodingException...");
		}
    }
    ////////////////////////////////////// 查询刷卡记录  ///////////////////////////////////////
    /**
     * 按记录集编号查询卡
     */
    public void queryRecordByRecNo() {
    	
    	NET_RECORDSET_ACCESS_CTL_CARDREC cardRecord = new NET_RECORDSET_ACCESS_CTL_CARDREC();
		// 记录集编号
		cardRecord.nRecNo = AccessCtlOperate.getRecordNo();
		
		if (!AccessCtlOperate.queryRecordState(cardRecord)) {
    		System.err.println("Query Record By RecNo Failed." + getErrorCode());
    		return;
		}
	}
    
    /**
     * 按卡号查询 刷卡记录
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryRecordByCardNo() {
    	final FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryCondition.bCardNoEnable = 1;
    	final String cardNo = "7FFFFFFF";
    	System.arraycopy(cardNo.getBytes(), 0, queryCondition.szCardNo, 0, cardNo.length());
    	
    	queryAccessRecord(queryCondition);
    }
    
    /**
     * 按时间查询刷卡记录
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryRecordByTime() {
    	final FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryCondition.bTimeEnable = 1;
    	queryCondition.stStartTime.setTime(2019, 4, 25, 15, 20, 0);
    	queryCondition.stEndTime.setTime(2019, 4, 25, 15, 39, 0);
    	
    	queryAccessRecord(queryCondition);
    }
    
    /**
     * 不按时间或卡号查询刷卡记录
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryAllRecord() {
    	final FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryAccessRecord(queryCondition);
    }
    
    /**
     * 查询门禁刷卡记录
     */
    public void queryAccessRecord(FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX queryCondition) {
    	
  		/// 1202B-D 不支持该功能
  		// System.out.println("Total Record Count: " + getTotalRecordCount(findRecordOut.lFindeHandle));

		final int nRecordCount = 10;  // 每次查询的最大个数, 1202B-D 最多支持10条
		///门禁刷卡记录记录集信息
		NET_RECORDSET_ACCESS_CTL_CARDREC[] records = new NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
		for(int i = 0; i < nRecordCount; i++) {
			records[i] = new NET_RECORDSET_ACCESS_CTL_CARDREC();
		}
		
		AccessCtlOperate.queryRecord(queryCondition, records);
    }
    
    private static void printRecord(NET_RECORDSET_ACCESS_CTL_CARDREC record) {
    	try {
    		System.out.println("刷卡时间:" + record.stuTime.toStringTime()
    				+ "\n" + "卡号:" + new String(record.szCardNo).trim()
    				+ "\n" + "卡类型:" + record.emCardType
    				+ "\n" + "门号:" + record.nDoor
    				+ "\n" + "密码:" + new String(record.szPwd).trim()
    				+ "\n" + "开门方式:" + record.emMethod
    				+ "\n" + "开门结果：" + (record.bStatus == 1 ? "成功" : "失败")
    				+ "\n" + "角色:" + new String(record.szRole, "GBK").trim()
    				+ "\n" + "项目编号:" + new String(record.szProjectNo).trim()
    				+ "\n" + "项目名称:" + new String(record.szProjectName, "GBK").trim()
    				+ "\n" + "施工单位全称:" + new String(record.szBuilderName, "GBK").trim()
    				+ "\n" + "工种名称:" + new String(record.szWorkerTypeName, "GBK").trim()
    				);
    	}catch(UnsupportedEncodingException e) {
    		System.err.println("...UnsupportedEncodingException...");
    	}
    }
    
    ////////////////////////////////////下发密码 ///////////////////////////////////////
    
    /** 
     * 插入密码 
     */
    public void insertPassword() {
 
		// 密码的编号, 支持500个, 不重复
		final String userId = "1011"; 
		
		// 开门密码
		final String openDoorPassword = "888887"; 
		
		NET_RECORDSET_ACCESS_CTL_PWD accessInsert = new NET_RECORDSET_ACCESS_CTL_PWD();
		
		System.arraycopy(userId.getBytes(), 0, accessInsert.szUserID, 
				0, userId.getBytes().length);
		System.arraycopy(openDoorPassword.getBytes(), 0, accessInsert.szDoorOpenPwd, 
				0, openDoorPassword.getBytes().length); 
		
		/// 以下字段可以固定, 目前设备做了限制必须要带
		accessInsert.nDoorNum = 2; // 门个数 表示双门控制器
		accessInsert.sznDoors[0] = 0; // 表示第一个门有权限
		accessInsert.sznDoors[1] = 1; // 表示第二个门有权限
		accessInsert.nTimeSectionNum = 2; // 与门数对应
		accessInsert.nTimeSectionIndex[0] = 255; // 表示第一个门全天有效
		accessInsert.nTimeSectionIndex[1] = 255; // 表示第二个门全天有效
		
		if(!AccessCtlOperate.insert(accessInsert)){
    		System.err.println("Insert Password Failed." + getErrorCode());
			return;
		} 
    }
    
    /**
     * 更新密码
     */
    public void updatePassword() {
    	
    	NET_RECORDSET_ACCESS_CTL_PWD accessUpdate = new NET_RECORDSET_ACCESS_CTL_PWD();
    	accessUpdate.nRecNo = AccessCtlOperate.getRecordNo(); // 需要修改的记录集编号,由插入获得
    	
    	/// 密码编号, 必填否则更新密码不起作用
    	final String userId = String.valueOf(accessUpdate.nRecNo);
    	System.arraycopy(userId.getBytes(), 0, accessUpdate.szUserID, 
				0, userId.getBytes().length);
    	
    	// 新的开门密码
    	final String newPassord = "333333";
    	System.arraycopy(newPassord.getBytes(), 0, 
    			accessUpdate.szDoorOpenPwd, 0, newPassord.getBytes().length);
	
		/// 以下字段可以固定, 目前设备做了限制必须要带
    	accessUpdate.nDoorNum = 2; // 门个数 表示双门控制器
    	accessUpdate.sznDoors[0] = 0; // 表示第一个门有权限
    	accessUpdate.sznDoors[1] = 1; // 表示第二个门有权限
    	accessUpdate.nTimeSectionNum = 2; // 与门数对应
    	accessUpdate.nTimeSectionIndex[0] = 255; // 表示第一个门全天有效
    	accessUpdate.nTimeSectionIndex[1] = 255; // 表示第二个门全天有效
	
    	if(!AccessCtlOperate.update(accessUpdate)){
    		System.err.println("Update Password Failed." + getErrorCode());
			return;
		}
    	
		System.out.println("Update pawssword Success.");
    }
    
    /**
     * 删除密码
     */
    public void deletePassword() {
    	if(!AccessCtlOperate.delete(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD, AccessCtlOperate.getRecordNo())){
    		System.err.println("delete pawssword failed. " + getErrorCode());
    	} 
    	
    	System.out.println("delete pawssword RecordNo " + AccessCtlOperate.getRecordNo() + " Success!");
    }
    
    /**
     * 清理记录
     */   
    public void clearPassword() {
    	if(!AccessCtlOperate.clear(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD)) {
    		System.err.println("Clear pawssword Failed." + getErrorCode());
    		return;
    	}
		System.out.println("Clear pawssword Success.");
    }
    
    /////////////////////////////////// 下发二维码 ////////////////////////////////////////
    
    /**
     * 插入二维码    1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void insertQRPassword() {
    	
		NET_RECORD_ACCESSQRCODE_INFO qrcodeInsert = new NET_RECORD_ACCESSQRCODE_INFO();
		
		// 有效次数: 该二维码可以使用多少次
		qrcodeInsert.nLeftTimes = 2;
		
		// 二维码值
		final String QRCode = "123456";
		System.arraycopy(QRCode.getBytes(), 0, qrcodeInsert.szQRCode, 0, QRCode.getBytes().length);  
		
		/// 全天有效如何设置
		// 有效开始时间
		qrcodeInsert.stuStartTime.dwYear = 2017;
		qrcodeInsert.stuStartTime.dwMonth = 11;
		qrcodeInsert.stuStartTime.dwDay = 15;
		qrcodeInsert.stuStartTime.dwHour = 0;
		qrcodeInsert.stuStartTime.dwMinute = 0;
		qrcodeInsert.stuStartTime.dwSecond = 0;
	
		// 有效结束时间
		qrcodeInsert.stuEndTime.dwYear = 2017;
		qrcodeInsert.stuEndTime.dwMonth = 11;
		qrcodeInsert.stuEndTime.dwDay = 15;
		qrcodeInsert.stuEndTime.dwHour = 23;
		qrcodeInsert.stuEndTime.dwMinute = 59;
		qrcodeInsert.stuEndTime.dwSecond = 59;
		
		if(!AccessCtlOperate.insert(qrcodeInsert)){
    		System.err.println("Insert QR Failed." + getErrorCode());
			return;
		} 
    }
    
    /**
     * 更新二维码    1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void updateQRPassword() {
		NET_RECORD_ACCESSQRCODE_INFO qrcodeUpdate = new NET_RECORD_ACCESSQRCODE_INFO();
		
		// 需要修改的记录集编号,由插入获得
		qrcodeUpdate.nRecNo = AccessCtlOperate.getRecordNo();   
		
		// 新二维码, 可以为任意字符串
		final String newQRCode = "444444";
		System.arraycopy(newQRCode.getBytes(), 0, qrcodeUpdate.szQRCode, 0, newQRCode.getBytes().length);  
		
		if(!AccessCtlOperate.update(qrcodeUpdate)){
    		System.err.println("Update QR Failed." + getErrorCode());
			return;
		}
		
		System.out.println("Update QR Success.");
    }
    
    /**
     * 删除二维码	 1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void deleteQRPassword() {  	
	
		if(!AccessCtlOperate.delete(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE, AccessCtlOperate.getRecordNo())){
    		System.err.println("delete QR Failed." + getErrorCode());
    		return;
    	}
		
    	System.out.println("delete QR RecordNo " + AccessCtlOperate.getRecordNo() + " Success!");
    }
    
    /**
     * 清理二维码    1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void clearQRPassword() {
		if(!AccessCtlOperate.clear(EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE)) {
    		System.err.println("Clear QR Failed." + getErrorCode());
    		return;
    	}
		System.out.println("Clear QR Success.");
    }
 
    ////////////////////////////////////// 记录集 ///////////////////////////////////////
    
    // 方法和字段均为静态的，不考虑并发或混合操作
    public static class AccessCtlOperate
    {
        private static int nRecordNo = 0; // 记录集编号 
        
        private static LLong lFindeHandle = new LLong(0);

        /**
    	 * 插入记录集
    	 */
    	public static boolean insert(SdkStructure object) {
    	
    		int ctrlType = CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT;
    		int emType = getRecordType(object);
    		if (emType == EM_NET_RECORD_TYPE.NET_RECORD_UNKNOWN) {
    			System.err.println("the input SdkStructure [" + object.getClass() + "] invalid!");
    			return false;
    		}
    		
    		// 插入指纹必须用  CTRLTYPE_CTRL_RECORDSET_INSERTEX，不能用 CTRLTYPE_CTRL_RECORDSET_INSERT
    		if (object instanceof NET_RECORDSET_ACCESS_CTL_CARD) {
				NET_RECORDSET_ACCESS_CTL_CARD accessCard = (NET_RECORDSET_ACCESS_CTL_CARD)object;
				if (accessCard.bEnableExtended == 1) { // 带指纹
					ctrlType = CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERTEX;
				}
			}
    					
    		// 记录集新增操作(insert)参数
    		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
    		insert.stuCtrlRecordSetInfo.emType = emType;   // 记录集类型
    		insert.stuCtrlRecordSetInfo.pBuf = object.getPointer();
    		
    		object.write();
    		insert.write();
    		if (!netsdkApi.CLIENT_ControlDevice(loginHandle, 
    				ctrlType, insert.getPointer(), 5000)) {
    			return false;
    		}
   
    		insert.read();
    		object.read();
    		
    		System.out.println("insert nRecNo : " + insert.stuCtrlRecordSetResult.nRecNo);
    		nRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
    		
    		return true;
    	}
    	
    	 /**
    	 * 更新记录集
    	 */
    	public static boolean update(SdkStructure object) {
    		
    		int ctrlType = CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE;
    		int emType = getRecordType(object);
    		if (emType == EM_NET_RECORD_TYPE.NET_RECORD_UNKNOWN) {
    			System.err.println("the input SdkStructure [" + object.getClass() + "] invalid!");
    			return false;
    		}
    		
    		// 更新指纹必须用  CTRLTYPE_CTRL_RECORDSET_UPDATEEX，不能用 CTRLTYPE_CTRL_RECORDSET_UPDATE
    		if (object instanceof NET_RECORDSET_ACCESS_CTL_CARD) {
				NET_RECORDSET_ACCESS_CTL_CARD accessCard = (NET_RECORDSET_ACCESS_CTL_CARD)object;
				if (accessCard.bEnableExtended == 1) { // 带指纹
					ctrlType = CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATEEX;
				}
			}
    		
        	NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
        	update.emType = emType;    // 记录集信息类型
        	update.pBuf = object.getPointer();
    	
        	object.write();
        	update.write();
        	if (!netsdkApi.CLIENT_ControlDevice(loginHandle, ctrlType, update.getPointer(), 5000)) {
    			return false;
    		}
 
//        	update.read();
//        	object.read();
        	
        	return true;
    	}
    	
    	/**
    	 * 删除指定记录
    	 */
    	public static boolean delete(int emType, int nRecordNo) {
    		
        	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
        	remove.emType = emType;
        	remove.pBuf = new IntByReference(nRecordNo).getPointer();
    	
        	remove.write();
        	
        	return netsdkApi.CLIENT_ControlDevice(loginHandle, 
        			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
//        	remove.read();
    	}
    	
    	/**
    	 * 清除记录
    	 */
    	public static boolean clear(int emType) {
    		NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
        	clear.emType = emType;    // 记录集信息类型
        	
        	clear.write();
        	return netsdkApi.CLIENT_ControlDevice(loginHandle, 
        			CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
//        	clear.read();
    	}
    	
    	
    	/**
         * 查询记录
         */
        public static void queryRecord(SdkStructure queryCondition, SdkStructure[] records) {
      		
      		if(!findRecord(queryCondition)) {
      			return;
      		}
      		      		
      		/// 1202B-D 不支持该功能
      		// System.out.println("Total Record Count: " + getTotalRecordCount(lFindeHandle));
    		
    		findNextRecord(records);
    		
    		findRecordClose();
        }
    	
        private static boolean findRecord(SdkStructure queryCondition) {
        	
        	int emType = getRecordType(queryCondition);
    		if (emType == EM_NET_RECORD_TYPE.NET_RECORD_UNKNOWN) {
    			System.err.println("the input query condition SdkStructure [" + queryCondition.getClass() + "] invalid!");
    			return false;
    		}
    		
        	///CLIENT_FindRecord入参
      		NET_IN_FIND_RECORD_PARAM findRecordIn = new NET_IN_FIND_RECORD_PARAM();
      		findRecordIn.emType = emType; 
      		findRecordIn.pQueryCondition = queryCondition.getPointer();
      		
      		///CLIENT_FindRecord出参
      		NET_OUT_FIND_RECORD_PARAM findRecordOut = new NET_OUT_FIND_RECORD_PARAM();
      	
      		queryCondition.write();
      		findRecordIn.write();
      		findRecordOut.write();
      		if (!netsdkApi.CLIENT_FindRecord(loginHandle, findRecordIn, findRecordOut, 5000)) {
      			System.err.println("Find Record Failed." + getErrorCode());
      			return false;
      		}
      		
      		findRecordOut.read();
      		
      		lFindeHandle = findRecordOut.lFindeHandle;
      		
      		return true;
        }
        
        private static boolean findNextRecord(SdkStructure[] records) {
        	
        	boolean bRet = true;
        	
        	int nRecordCount = records.length;
        	
        	///CLIENT_FindNextRecord入参
    		NET_IN_FIND_NEXT_RECORD_PARAM findNextRecordIn = new NET_IN_FIND_NEXT_RECORD_PARAM();
    		findNextRecordIn.lFindeHandle = lFindeHandle;
    		findNextRecordIn.nFileCount = nRecordCount;  //想查询的记录条数
    		
    		///CLIENT_FindNextRecord出参
    		NET_OUT_FIND_NEXT_RECORD_PARAM findNextRecordOut = new NET_OUT_FIND_NEXT_RECORD_PARAM();
    		findNextRecordOut.nMaxRecordNum = nRecordCount;
    		findNextRecordOut.pRecordList = new Memory(records[0].size() * nRecordCount);
    		findNextRecordOut.pRecordList.clear(records[0].size() * nRecordCount);	
    		
    		// 将  native 数据初始化
    		ToolKits.SetStructArrToPointerData(records, findNextRecordOut.pRecordList);
    		
    		int nLoop = 0;  //循环的次数
    		int recordIndex = 1;	
    		while(true) {  //循环查询			
    			
    			if(!netsdkApi.CLIENT_FindNextRecord(findNextRecordIn, findNextRecordOut, 5000) )  {
    				System.err.println("FindNextRecord Failed" + getErrorCode());
    				bRet = false;
    				break;
    			} 	
    			
    			/// 将 native 数据转为 java 数据
    			ToolKits.GetPointerDataToStructArr(findNextRecordOut.pRecordList, records);
    			for(int i = 0; i < findNextRecordOut.nRetRecordNum; i++) {
    				recordIndex = i + nLoop * nRecordCount;				
    				System.out.println("----------------[" + recordIndex + "]----------------" );
    				output(records[i]);
    			}							
    			
    			if (findNextRecordOut.nRetRecordNum < nRecordCount)	{
    				break;
    			}		
    			nLoop++;
    		}
    		
//    		long peer = Pointer.nativeValue(findNextRecordOut.pRecordList);
//    		Pointer.nativeValue(findNextRecordOut.pRecordList, 0);
//    		Native.free(peer);
    		
			return bRet;
        }
        
        private static void findRecordClose() {
        	if  (!netsdkApi.CLIENT_FindRecordClose(lFindeHandle)) {
      			System.err.println("Find Record Failed." + getErrorCode());
        	}
        }
        
        public static boolean queryRecordState(SdkStructure condition) {
        	
        	int emType = getRecordType(condition);
    		if (emType == EM_NET_RECORD_TYPE.NET_RECORD_UNKNOWN) {
    			System.err.println("the input query condition SdkStructure [" + condition.getClass() + "] invalid!");
    			return false;
    		}
    		
    		NET_CTRL_RECORDSET_PARAM record = new NET_CTRL_RECORDSET_PARAM();
    		record.emType = emType;
    		record.pBuf = condition.getPointer();
    		
        	IntByReference intRetLen = new IntByReference();
        	
        	condition.write();
        	record.write();
    		if (!netsdkApi.CLIENT_QueryDevState(loginHandle, NetSDKLib.NET_DEVSTATE_DEV_RECORDSET, 
    				record.getPointer(), record.size(), intRetLen, 3000)) {
    			return false;
    		}
    		
    		record.read();
    		condition.read();
    		
    		output(condition);
    		
    		return true;
    	}
        
        private static void output(SdkStructure record) { // 具体输出在上层定义
        	
        	if (record instanceof NET_RECORDSET_ACCESS_CTL_CARDREC) {

        		printRecord((NET_RECORDSET_ACCESS_CTL_CARDREC)record);
        		
    		}else if(record instanceof NET_RECORDSET_ACCESS_CTL_CARD) {
        		
    			printRecord((NET_RECORDSET_ACCESS_CTL_CARD)record);
    			
    		}
        }

    	public static int getRecordNo() {
    		return nRecordNo;
    	}
    	
    	private static int getRecordType(SdkStructure object) {
    		int type = EM_NET_RECORD_TYPE.NET_RECORD_UNKNOWN;

    		if (object instanceof NET_RECORDSET_ACCESS_CTL_CARD 
    				|| object instanceof FIND_RECORD_ACCESSCTLCARD_CONDITION) {
    			
    			type = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
    			
    		}else if (object instanceof NET_RECORD_ACCESSQRCODE_INFO) {
    			
    			type = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE;
    			
    		}else if (object instanceof NET_RECORDSET_ACCESS_CTL_PWD) {
    			
    			type = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;
    			
    		}else if (object instanceof NET_RECORDSET_ACCESS_CTL_CARDREC 
    				|| object instanceof FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX) {
    			
    			type = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
    			
    		}
    		
    		return type;
    	}
    }
	
	public void RunTest() {
		CaseMenu menu = new CaseMenu();	
		menu.addItem(new CaseMenu.Item(this , "订阅报警信息——startListen", "startListen"));  // 开门一般通过门禁事件
		menu.addItem(new CaseMenu.Item(this , "取消订阅报警信息——stopListen", "stopListen"));  // 开门一般通过门禁事件
		
		menu.addItem(new CaseMenu.Item(this , "智能订阅——realLoadPicture" , "realLoadPicture"));		// 具体定义在AccessFaceOperate
		menu.addItem(new CaseMenu.Item(this , "取消智能订阅——stopRealLoadPicture" , "stopRealLoadPicture")); // 具体定义在AccessFaceOperate
	
		menu.addItem(new CaseMenu.Item(this , "开门——openDoor", "openDoor"));
		menu.addItem(new CaseMenu.Item(this , "关门——closeDoor", "closeDoor"));
		menu.addItem(new CaseMenu.Item(this , "查询门（开、关）状态——queryDoorStatus", "queryDoorStatus"));
		menu.addItem(new CaseMenu.Item(this , "指纹采集——captureFingerprint", "captureFingerprint"));
		
		// 指纹插入和更新用EX枚举, 指纹及卡对应的删除和清除其实是一样的, 本质上卡和指纹都是一样的, 实际上指纹的操作包含了卡操作。
		menu.addItem(new CaseMenu.Item(this , "下发指纹——insertFingerprint", "insertFingerprint")); 
		menu.addItem(new CaseMenu.Item(this , "更新指纹——updateFingerprint", "updateFingerprint")); 
//		menu.addItem(new CaseMenu.Item(this , "删除指纹——deleteFingerprint", "deleteFingerprint")); 
//		menu.addItem(new CaseMenu.Item(this , "清除指纹——clearFingerprint", "clearFingerprint")); 
		// 下发卡信息内包含下发人脸 -faceOperate
		menu.addItem(new CaseMenu.Item(this , "下发卡信息——insertCard" , "insertCard"));
		menu.addItem(new CaseMenu.Item(this , "更新卡信息——updateCard" , "updateCard"));
		menu.addItem(new CaseMenu.Item(this , "删除卡/指纹信息——deleteCard", "deleteCard"));
		menu.addItem(new CaseMenu.Item(this , "清除卡/指纹信息——clearCard", "clearCard"));
		menu.addItem(new CaseMenu.Item(this , "按记录集编号查询卡信息——queryCardByRecNo", "queryCardByRecNo"));
		menu.addItem(new CaseMenu.Item(this , "按卡号查询卡信息——queryCardByCardNo", "queryCardByCardNo"));
		menu.addItem(new CaseMenu.Item(this , "查询所有卡信息——queryAllCard", "queryAllCard"));
		
		menu.addItem(new CaseMenu.Item(this , "插入门禁出入记录——insertRecord" , "insertRecord"));
		menu.addItem(new CaseMenu.Item(this , "更新门禁出入记录——updateRecord" , "updateRecord"));
		menu.addItem(new CaseMenu.Item(this , "删除门禁出入记录——deleteRecord", "deleteRecord"));
		menu.addItem(new CaseMenu.Item(this , "清除门禁出入记录——clearRecord", "clearRecord"));
		menu.addItem(new CaseMenu.Item(this , "按记录集编号查询刷卡记录——queryRecordByRecNo", "queryRecordByRecNo"));
		menu.addItem(new CaseMenu.Item(this , "按卡号查询刷卡记录——queryRecordByCardNo", "queryRecordByCardNo"));
		menu.addItem(new CaseMenu.Item(this , "按时间查询刷卡记录——queryRecordByTime", "queryRecordByTime"));
		menu.addItem(new CaseMenu.Item(this , "查询所有刷卡记录——queryAllRecord", "queryAllRecord"));

		
		menu.addItem(new CaseMenu.Item(this , "下发密码——insertPassword", "insertPassword"));
		menu.addItem(new CaseMenu.Item(this , "更新密码——updatePassword", "updatePassword"));
		menu.addItem(new CaseMenu.Item(this , "删除密码——deletePassword", "deletePassword"));
		menu.addItem(new CaseMenu.Item(this , "清除密码——clearPassword", "clearPassword"));
		
		menu.addItem(new CaseMenu.Item(this , "下发二维码——insertQRPassword" , "insertQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "更新二维码——updateQRPassword" , "updateQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "删除二维码——deleteQRPassword", "deleteQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "清除二维码——clearQRPassword", "clearQRPassword"));

		menu.run();
	}

	
	public static void main(String[]args) {
		AccessControlNew demo = new AccessControlNew();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}

