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

public class AccessControl {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);

	String address 			= "172.23.8.94"; // 172.26.6.104
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin123";
	
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
	}
    
	
	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
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
				new Thread(new Runnable() {
					@Override
					public void run() {
						openDoor();
					}
				}).start();
				
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
     * 插入指纹成功后返回的编号, 用于后续的更新、删除等操作
     * 插入指纹必须用  CTRLTYPE_CTRL_RECORDSET_INSERTEX，不能用 CTRLTYPE_CTRL_RECORDSET_INSERT
     */
    private static int fingerprintRecordNo = 0;
    
	/**
	 * 插入指纹
	 */
	public static void insertFingerprint() {
		String finggerStr = "xR5jAAOIEiX/NCfFhIliVhAHpkYeiirZ+HP4g4WIyu43S4ZJVoZ7UshNtwgxibuh+C4mxkqGU67RD8YFM4pryfhyFoY/h7Pt4JYoijKGpIIZS6nGtYbkfQiyOUe5iLSB30WZBEKJtIr4MheEVogkwdYvygi3ibTdJ8XIg7eHZPbgvg8bSoi89eZ0KQSuh2ytCM5LUaeHFQ0hvk+Wh4oiFRf/t4RrhuIhB8n2wyOH+5XZ4Gh2NIfLyeEcSJZLhUwd18f2BEWFXIHXxfaEx4ddGs9/v5hHiaVe+Ln7Ak+IZar/9/rDwoklthf/y0IIiGH6/zIlQwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGEYMyNXZEMndoR2EWUzZiaEM+qTSHYksRJ2QyUyMxAyM0TxJFdPFGMiVVEz5Slkl4wyFkMRMfJzGQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaY+xaNHQFhcwqM2BVghxXQISYjTsaA2O6YagjEqOekUxGA2IWkYAKcBSYcukRANDstPJOMhHlkUUbEJCsolMRRcHtYZVgM7TJYr0VEDQB9u8sIDRTcRZcEMGJkbsZIXUT9XwuFHVGYVwFMRPg9oENMRS9YS9kIDGrgF1sNWNHgG12ElNnYW9xQ0IxcY0CIVIlsGVdNMGeoE11UlQRUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABYIFQYLCgQRCQ0FAw8MEBIHGRcOAgEAGhsdAAAAAP/K";
		
		byte[] szFingerPrintInfo = Base64Util.getDecoder().decode(finggerStr);   // 将字符串转为指纹数据
		
		// 门禁卡记录集信息
		NET_RECORDSET_ACCESS_CTL_CARD accessInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		// 卡号
		String cardNo = "1011";
		System.arraycopy(cardNo.getBytes(), 0, accessInsert.szCardNo, 0, cardNo.getBytes().length);
		
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
		accessInsert.stuCreateTime.setTime(2018, 12, 19, 14, 1, 1);
		
		// 使用次数
		accessInsert.nUserTime = 200;
		
		// 有效开始/结束时间
		accessInsert.bIsValid = 1;
		accessInsert.stuValidStartTime.setTime(2018, 12, 18, 10, 1, 1);
		accessInsert.stuValidEndTime.setTime(2018, 12, 20, 10, 1, 1);
		
		// 记录集新增操作(insert)参数
		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
		insert.stuCtrlRecordSetInfo.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;   // 记录集类型
		insert.stuCtrlRecordSetInfo.pBuf = accessInsert.getPointer();
		
		accessInsert.write();
		insert.write();
		boolean success = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERTEX, insert.getPointer(), 5000);
		insert.read();
		accessInsert.read();
		
		if(!success) {
			System.err.println("insert password failed." + getErrorCode());
			return;
		} 

		System.out.println("Fingerprint nRecNo : " + insert.stuCtrlRecordSetResult.nRecNo);
		fingerprintRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
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
		accessUpdate.nRecNo = fingerprintRecordNo;  // 需要修改的记录集编号,由插入获得
		
		// 卡号
		String cardNo = "1011";
		System.arraycopy(cardNo.getBytes(), 0, accessUpdate.szCardNo, 0, cardNo.getBytes().length);
		
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
		
    	NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
    	update.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	update.pBuf = accessUpdate.getPointer();
	
    	accessUpdate.write();
    	update.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATEEX, update.getPointer(), 5000);
    	update.read();
		accessUpdate.read();
		if (!result) {
			System.err.println("update password failed." + getErrorCode());
    	}
	}
	
	/**
	 * 删除指纹
	 */
	public void removeFingerprint() {
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
    	remove.pBuf = new IntByReference(fingerprintRecordNo).getPointer();
	
    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.println("remove fingerprint failed." + getErrorCode());
    	} 
	}
	
	/**
	 * 清除记录
	 */
	public void clearFingerprint() {
		NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
    	clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	
    	clear.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
    	clear.read();
    	if(!result){
    		System.err.println("clear fingerprint failed." + getErrorCode());
    	}
	}
    
    ////////////////////////////////////下发密码 ///////////////////////////////////////
    /**
     * 插入密码成功后返回的编号, 用于后续的更新、删除等操作
     */
    private int passwordRecordNo = 0;
    
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
		
		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
		insert.stuCtrlRecordSetInfo.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;    // 记录集信息类型
		insert.stuCtrlRecordSetInfo.pBuf = accessInsert.getPointer();
		
		accessInsert.write();
		insert.write();
		boolean success = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, insert.getPointer(), 5000);
		insert.read();
		accessInsert.read();
		
		if(!success) {
			System.err.println("insert password failed. 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
			return;
		} 

		System.out.println("Password nRecNo : " + insert.stuCtrlRecordSetResult.nRecNo);
		passwordRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
    }
    
    /**
     * 更新密码
     */
    public void updatePassword() {
    	
    	NET_RECORDSET_ACCESS_CTL_PWD accessUpdate = new NET_RECORDSET_ACCESS_CTL_PWD();
    	accessUpdate.nRecNo = passwordRecordNo; // 需要修改的记录集编号,由插入获得
    	
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
	
    	NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
    	update.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;    // 记录集信息类型
    	update.pBuf = accessUpdate.getPointer();
	
    	accessUpdate.write();
    	update.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE, update.getPointer(), 5000);
    	update.read();
		accessUpdate.read();
		if (!result) {
			System.err.println("update password failed. 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
    	}
    }
    
    /**
     * 删除密码
     */
    public void deletePassword() {
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;
    	remove.pBuf = new IntByReference(passwordRecordNo).getPointer();
	
    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.println(" remove pawssword failed. 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
    	} 
    }
    
    /**
     * 清理记录
     */
    public void clearPassword() {
    	NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
    	clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;    // 记录集信息类型
    	
    	clear.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
    	clear.read();
    	if(!result){
    		System.err.println(" clear pawssword failed. 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
    	}
    }
    
    /////////////////////////////////// 下发二维码 ////////////////////////////////////////
    /**
     * 插入二维码成功后返回的编号, 用于后续的更新、删除等操作
     */
    private int QRRecordNo = 0;
    
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
		
		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
		insert.stuCtrlRecordSetInfo.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE;    // 记录集信息类型
		insert.stuCtrlRecordSetInfo.pBuf = qrcodeInsert.getPointer();
		
		insert.write();
		qrcodeInsert.write();
		boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, insert.getPointer(), 5000);
		qrcodeInsert.read();
		insert.read();
		
		if(!result){
			System.err.printf("Insert QR Failed. %x \n", netsdkApi.CLIENT_GetLastError());
			return;
		} 
		
		System.out.println("QR nRecNo : " + insert.stuCtrlRecordSetResult.nRecNo);
		QRRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
		
    }
    
    /**
     * 更新二维码    1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void updateQRPassword() {
		NET_RECORD_ACCESSQRCODE_INFO qrcodeUpdate = new NET_RECORD_ACCESSQRCODE_INFO();
		
		// 需要修改的记录集编号,由插入获得
		qrcodeUpdate.nRecNo = QRRecordNo;   
		
		// 新二维码, 可以为任意字符串
		final String newQRCode = "444444";
		System.arraycopy(newQRCode.getBytes(), 0, qrcodeUpdate.szQRCode, 0, newQRCode.getBytes().length);  
		
		NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
		update.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE;    // 记录集信息类型
		update.pBuf = qrcodeUpdate.getPointer();
		
		update.write();
		qrcodeUpdate.write();
		boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE, update.getPointer(), 5000);
		qrcodeUpdate.read();
		update.read();
		
		if(!result){
			System.err.printf("Update RQ Failed. %x \n", netsdkApi.CLIENT_GetLastError());
			return;
		}
    }
    
    /**
     * 删除二维码	 1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void deleteQRPassword() {  	
		NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
		remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE;    // 记录集信息类型
		remove.pBuf = new IntByReference(QRRecordNo).getPointer();
		
		remove.write();
		boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
		if(!result){
			System.err.printf("Remove QR Failed. %x \n", netsdkApi.CLIENT_GetLastError());
			return;
		}
    }
    
    /**
     * 清理二维码    1202B-D 不支持该接口. 参考发卡, 二维码和卡号等值
     */
    public void clearQRPassword() {
		NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
		clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSQRCODE;    // 记录集信息类型
		
		clear.write();
		boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
		clear.read();
		
		if(!result) {
			System.err.printf("Clear Failed. %x \n", netsdkApi.CLIENT_GetLastError());
		}
    }
    
    ////////////////////////////////////// 下发卡  //////////////////////////////////////////
    /**
     * 卡编号
     */
    private int cardRecordNo = 0;
    
    /**
     * 插入卡信息
     */
    public void insertCard() {

		NET_RECORDSET_ACCESS_CTL_CARD cardInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		/// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String cardNo = "7FFFFFFF";
		System.arraycopy(cardNo.getBytes(), 0, cardInsert.szCardNo, 
				0, cardNo.getBytes().length); 
		
		/// 用户ID
		final String useId = "1122";
		System.arraycopy(useId.getBytes(), 0, cardInsert.szUserID, 
				0, useId.getBytes().length); 
		
		/// 设置有效时间
		cardInsert.stuValidStartTime.dwYear = 2017;
		cardInsert.stuValidStartTime.dwMonth = 11;
	
		cardInsert.stuValidEndTime.dwYear = cardInsert.stuValidStartTime.dwYear + 10; // 10 年有效
		cardInsert.stuValidEndTime.dwMonth = 11;
		
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
		System.out.println("Insert Success. Card nRecNo " + cardRecordNo + ": [ Hex = " + Integer.toHexString(cardRecordNo) + " ]");
    }
    
    /**
     * 更新卡信息:
     * 主要：不能更新卡号，原来插入的信息也要保留，仅修改对应的字段
     * 
     */
    public void updateCard() {
    	System.out.println("Update Card RecordNo " + cardRecordNo);
    	
    	NET_RECORDSET_ACCESS_CTL_CARD cardUpdate = new NET_RECORDSET_ACCESS_CTL_CARD();
    	
    	/// 注意：原来插入卡的字段要保留
    	cardUpdate.nRecNo = cardRecordNo;
    	
    	/// 卡号, 16进制, 最大支持8位, 不要为负数的值
		final String cardNo = "7FFFFFFF";
		System.arraycopy(cardNo.getBytes(), 0, cardUpdate.szCardNo, 
				0, cardNo.getBytes().length); 
		
		/// 用户ID
		final String useId = "1122";
		System.arraycopy(useId.getBytes(), 0, cardUpdate.szUserID, 
				0, useId.getBytes().length); 
		
		/// 设置有效时间
		cardUpdate.stuValidStartTime.dwYear = 2017;
		cardUpdate.stuValidStartTime.dwMonth = 11;
	
		cardUpdate.stuValidEndTime.dwYear = cardUpdate.stuValidStartTime.dwYear + 10; // 10 年有效
		cardUpdate.stuValidEndTime.dwMonth = 11;
    	
		//---------------------------------------------------------
    	
		///-- 修改: 门的权限及时间段, 仅第一个门有效
		cardUpdate.nDoorNum = 1; // 门个数 表示双门控制器
		cardUpdate.sznDoors[0] = 0; // 表示第一个门有权限
		cardUpdate.nTimeSectionNum = 1; // 与门数对应
		cardUpdate.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		  	
    	NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
    	update.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	update.pBuf = cardUpdate.getPointer();
	
    	cardUpdate.write();
    	update.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE, update.getPointer(), 5000);
    	update.read();
    	cardUpdate.read();
		if (!result) {
			System.err.printf("Update Card Failed. %x \n", netsdkApi.CLIENT_GetLastError());
			return;
    	}
    }
    
    /**
     * 删除卡信息
     */
    public void deleteCard() {
    	
    	System.out.println("Delete Card RecordNo " + cardRecordNo);	
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
    	remove.pBuf = new IntByReference(cardRecordNo).getPointer();
	
    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.printf("Remove Card Failed. %x \n", netsdkApi.CLIENT_GetLastError());
    		return;
    	} 
    }
    
    /**
     * 清除卡
     */
    public void clearCard() {
    	NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
    	clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	
    	clear.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
    	clear.read();
    	if(!result) {
    		System.err.printf("Clear Card Failed. %x \n", netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    ////////////////////////////////////// 查询刷卡记录  ///////////////////////////////////////
    /**
     * 查询门禁刷卡记录
     */
    public void queryAccessRecord(FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX queryCondition) {
    	
    	/// 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
    	if (queryCondition == null) {
    		queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	}
    	
    	///CLIENT_FindRecord入参
  		NET_IN_FIND_RECORD_PARAM findRecordIn = new NET_IN_FIND_RECORD_PARAM();
  		findRecordIn.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
  		findRecordIn.pQueryCondition = queryCondition.getPointer();
  		
  		///CLIENT_FindRecord出参
  		NET_OUT_FIND_RECORD_PARAM findRecordOut = new NET_OUT_FIND_RECORD_PARAM();
  	
  		queryCondition.write();
  		findRecordIn.write();
  		findRecordOut.write();
  		boolean success = netsdkApi.CLIENT_FindRecord(loginHandle, findRecordIn, findRecordOut, 5000);
  		findRecordOut.read();
  		findRecordIn.read();
  		queryCondition.read();
  		
  		if(!success) {
  			System.err.println("Can Not Find This Record: " + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
  			return;
  		}
  		
  		System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + findRecordOut.lFindeHandle);
  		
  		/// 1202B-D 不支持该功能
  		// System.out.println("Total Record Count: " + getTotalRecordCount(findRecordOut.lFindeHandle));

		final int nRecordCount = 10;  // 每次查询的最大个数, 1202B-D 最多支持10条
		///门禁刷卡记录记录集信息
		NET_RECORDSET_ACCESS_CTL_CARDREC[] records = new NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
		for(int i = 0; i < nRecordCount; i++) {
			records[i] = new NET_RECORDSET_ACCESS_CTL_CARDREC();
		}
		
		///CLIENT_FindNextRecord入参
		NET_IN_FIND_NEXT_RECORD_PARAM findNextRecordIn = new NET_IN_FIND_NEXT_RECORD_PARAM();
		findNextRecordIn.lFindeHandle = findRecordOut.lFindeHandle;
		findNextRecordIn.nFileCount = nRecordCount;  //想查询的记录条数
		
		///CLIENT_FindNextRecord出参
		NET_OUT_FIND_NEXT_RECORD_PARAM findNextRecordOut = new NET_OUT_FIND_NEXT_RECORD_PARAM();
		findNextRecordOut.nMaxRecordNum = nRecordCount;
		findNextRecordOut.pRecordList = new Memory(records[0].dwSize * nRecordCount);
		findNextRecordOut.pRecordList.clear(records[0].dwSize * nRecordCount);	
		
		// 将  native 数据初始化
		ToolKits.SetStructArrToPointerData(records, findNextRecordOut.pRecordList);
		
		int count = 0;  //循环的次数
		int recordIndex = 1;	
		while(true) {  //循环查询			
			
			if(!netsdkApi.CLIENT_FindNextRecord(findNextRecordIn, findNextRecordOut, 5000) )  {
				System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
				break;
			} 	
			
			/// 将 native 数据转为 java 数据
			ToolKits.GetPointerDataToStructArr(findNextRecordOut.pRecordList, records);
			for(int i = 0; i < findNextRecordOut.nRetRecordNum; i++) {
				recordIndex = i + count * nRecordCount;				
				System.out.println("----------------[" + recordIndex + "]----------------" );
				System.out.println("刷卡时间:" + records[i].stuTime.toStringTime()
						+ "\n" + "卡号:" + new String(records[i].szCardNo).trim()
						+ "\n" + "卡类型:" + records[i].emCardType
						+ "\n" + "门号:" + records[i].nDoor
						+ "\n" + "密码:" + new String(records[i].szPwd).trim()
						+ "\n" + "开门方式:" + records[i].emMethod
						+ "\n" + "开门结果：" + (records[i].bStatus == 1 ? "成功" : "失败")
						);
			}							
			
			if (findNextRecordOut.nRetRecordNum < nRecordCount)	{
				break;
			}		
			count ++;

		}
		success = netsdkApi.CLIENT_FindRecordClose(findRecordOut.lFindeHandle);  
		if (!success) {
			System.err.println("Failed to Close: " + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
		}
    }
    
    /**
     * 按卡号查询 刷卡记录
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryRecordByNo() {
    	final FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryCondition.bCardNoEnable = 1;
    	final String cardNo = "12345";
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
    	queryCondition.stStartTime.setTime(2017, 12, 11, 15, 39, 0);
    	queryCondition.stEndTime.setTime(2017, 12, 11, 16, 39, 0);
    	
    	queryAccessRecord(queryCondition);
    }
    
    /**
     * 不按时间或卡号查询刷卡记录
     * 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
     */
    public void queryAllRecords() {
    	final FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryAccessRecord(queryCondition);
    }
    
    /**
     * 获取查询总记录条数
     * @param findHandle 查询句柄
     * @return
     */
    public int getTotalRecordCount(long findHandle) {
    	NET_IN_QUEYT_RECORD_COUNT_PARAM paramIn = new NET_IN_QUEYT_RECORD_COUNT_PARAM();
	    paramIn.lFindeHandle.setValue(findHandle);
	    NET_OUT_QUEYT_RECORD_COUNT_PARAM paramOut = new NET_OUT_QUEYT_RECORD_COUNT_PARAM();
	    boolean bRet = netsdkApi.CLIENT_QueryRecordCount(paramIn, paramOut, 3000);
	    if (!bRet) {
	    	System.err.println("Can't FindNextRecord" + Integer.toHexString(netsdkApi.CLIENT_GetLastError()));
	        return -1;
	    }

	    return paramOut.nRecordCount;
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
    
    
  	
	
	public void RunTest() {
		CaseMenu menu = new CaseMenu();	
		menu.addItem(new CaseMenu.Item(this , "captureFingerprint", "captureFingerprint"));

		menu.addItem(new CaseMenu.Item(this , "insertFingerprint", "insertFingerprint"));
		
		menu.addItem(new CaseMenu.Item(this , "startListen", "startListen"));  // 开门一般通过门禁事件
		
		menu.addItem(new CaseMenu.Item(this , "Door Open", "openDoor"));
		menu.addItem(new CaseMenu.Item(this , "Door Close", "closeDoor"));
		
		menu.addItem(new CaseMenu.Item(this , "Pwd Insert : id - 1010, pwd - 888888 ", "insertPassword"));
		menu.addItem(new CaseMenu.Item(this , "Pwd Update : id - 1010, pwd - 333333 ", "updatePassword"));
		menu.addItem(new CaseMenu.Item(this , "Pwd Delete : id - 1010", "deletePassword"));
		menu.addItem(new CaseMenu.Item(this , "Pwd Clear", "clearPassword"));
		
		menu.addItem(new CaseMenu.Item(this , "QR Insert  : QR - 123456 " , "insertQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "QR Update  : QR - 444444" , "updateQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "QR Delete", "deleteQRPassword"));
		menu.addItem(new CaseMenu.Item(this , "QR Clear", "clearQRPassword"));
		
		menu.addItem(new CaseMenu.Item(this , "Card Insert : cardNo - 3344" , "insertCard"));
		menu.addItem(new CaseMenu.Item(this , "Card Update : cardNo - 4455" , "updateCard"));
		menu.addItem(new CaseMenu.Item(this , "Card Delete", "deleteCard"));
		menu.addItem(new CaseMenu.Item(this , "Card Clear", "clearCard"));
		
		menu.addItem(new CaseMenu.Item(this , "Query All Records", "queryAllRecords"));
		menu.addItem(new CaseMenu.Item(this , "Query Records By Time", "queryRecordByTime"));
		menu.addItem(new CaseMenu.Item(this , "Query Records By Car No", "queryRecordByNo"));
		
		menu.addItem(new CaseMenu.Item(this , "Query Door Status", "queryDoorStatus"));
		
		menu.run();
	}

	
	public static void main(String[]args) {
		AccessControl demo = new AccessControl();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}

