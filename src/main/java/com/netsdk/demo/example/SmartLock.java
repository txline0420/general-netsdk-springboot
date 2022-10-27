package com.netsdk.demo.example;

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

public class SmartLock {
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
				System.out.println("nChannel:" + codeid[i].nChannel);
				
				// 新版本的智能锁，是用序列号来区分锁
				System.out.println("序列号：" + new String(codeid[i].szSerialNumber).trim());
			}
		} else {
			System.err.println("查找序列号失败, " + getErrorCode());
		}
	}
	
	
	/**
	 * 添加智能锁用户
	 * 函数功能：通过此接口去新增或者更新用户信息，下发的卡密码指纹可选，可一次下发多个组合
	 */
	public void AddSmartLock() {
		/*
		 * 入参
		 */
		NET_IN_SMARTLOCK_UPDATE_USER_INFO stIn = new NET_IN_SMARTLOCK_UPDATE_USER_INFO();
		
		// 新版本的智能锁，用智能锁序列号区分锁
		String sn = "373664716842 ";
		System.arraycopy(sn.getBytes(), 0, stIn.szSerialNumber, 0, sn.getBytes().length);
		
		// 身份拥有者(与AccessControlCard记录集中的UserID概念一致)
		String credentialHolder= "23355555";
		System.arraycopy(credentialHolder.getBytes(), 0, stIn.szCredentialHolder, 0, credentialHolder.getBytes().length);
		
		// 用户名称
		String username = "jJDJJD九点";
		try {
			System.arraycopy(username.getBytes("GBK"), 0, stIn.szUserName, 0, username.getBytes("GBK").length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// 开始时间
		stIn.stuStartTime.dwYear = 2018;
		stIn.stuStartTime.dwMonth = 10;
		stIn.stuStartTime.dwDay = 16;
		stIn.stuStartTime.dwHour = 10;
		stIn.stuStartTime.dwMinute = 10;
		stIn.stuStartTime.dwSecond = 10;		
		
		// 结束时间
		stIn.stuEndTime.dwYear = 2018;
		stIn.stuEndTime.dwMonth = 10;
		stIn.stuEndTime.dwDay = 17;
		stIn.stuEndTime.dwHour = 10;
		stIn.stuEndTime.dwMinute = 10;
		stIn.stuEndTime.dwSecond = 10;	
		
		////// 卡信息  //////
		// 一维：用户序号   卡号    卡类型
		// 二维：卡信息个数
		String[][] cardInfos = {{"1", "SJJSDIDI", "0"},
							    {"2", "SKSKI446", "0"}};
		
		stIn.nCardInfoNum = cardInfos.length;  				// 卡信息数量
		
		for(int i = 0; i < cardInfos.length; i++) {
			stIn.stuCardInfo[i].emCardType = 1;     								// 开门方式：0-未知， 1-卡开门， 2-密码开门， 3-指纹
			stIn.stuCardInfo[i].nIndex = Integer.parseInt(cardInfos[i][0]);         // 用户信息序号 
			// 卡号
			System.arraycopy(cardInfos[i][1].getBytes(), 0, stIn.stuCardInfo[i].szCardNo, 0, cardInfos[i][1].getBytes().length);
			
			stIn.stuCardInfo[i].emCardType = Integer.parseInt(cardInfos[i][2]);    // 卡类型，参考 NET_ACCESSCTLCARD_TYPE
		}

		
		////// 密码 ///////
		// 一维：用户序号   密码    使用次数
		// 二维：密码个数
		String[][] pwdInfos = {{"1", "2123235", "10"},
			    			   {"2", "5566446", "10"}};
		
		stIn.nPwdInfoNum = pwdInfos.length;				   	// 密码个数
		
		for(int i = 0; i < pwdInfos.length; i++) {
			stIn.stuPwdInfo[i].emType = 2;     	  								  // 开门方式：0-未知， 1-卡开门， 2-密码开门， 3-指纹
			stIn.stuPwdInfo[i].nIndex = Integer.parseInt(pwdInfos[i][0]);         // 用户信息序号 
			// 密码
			System.arraycopy(pwdInfos[i][1].getBytes(), 0, stIn.stuPwdInfo[i].szPassword, 0, pwdInfos[i][1].getBytes().length);
			
			stIn.stuPwdInfo[i].dwUseTime = Integer.parseInt(pwdInfos[i][2]);      // 使用次数
		}
		
		////// 指纹 ///////
		// 一维：用户序号   指纹数据字符串
		// 二维：指纹个数
		String[][] fingerprintInfos = {{"1", "xTxpAASFsk3+8hoDh4ky0ghH2oR7hjp658Wp" +
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
				"5GEFEkosISZBLUkKlXIHbdkGRRAQHRoAwLDw4NFwgJChglKxEoKiYaGQcSBDIbIy0pNBQWFVVs"},
				
			    			   		   {"2", "xTxpAASFsk3+8hoDh4ky0ghH2oR7hjp658Wp" +
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
				"5GEFEkosISZBLUkKlXIHbdkGRRAQHRoAwLDw4NFwgJChglKxEoKiYaGQcSBDIbIy0pNBQWFVVs"}};
		
		// 指纹内存申请
		for(int i = 0; i < fingerprintInfos.length; i++) {
			stIn.stuFingerPrintInfo[i].nFingerprintLen = 1536;
			stIn.stuFingerPrintInfo[i].pFingerprintData = new Memory(1536);
			stIn.stuFingerPrintInfo[i].pFingerprintData.clear(1536);
		}
		
		stIn.nFingerPrintInfoNum = fingerprintInfos.length;				   	// 指纹信息个数
		
		for(int i = 0; i < fingerprintInfos.length; i++) {
			stIn.stuFingerPrintInfo[i].emType = 3;     	  										  // 开门方式：0-未知， 1-卡开门， 2-密码开门， 3-指纹
			stIn.stuFingerPrintInfo[i].nIndex = Integer.parseInt(fingerprintInfos[i][0]);         // 用户信息序号 
			
			byte[] szFingerPrintInfo = Base64Util.getDecoder().decode(fingerprintInfos[i][1]);    // 将字符串转为指纹数据
			
			stIn.stuFingerPrintInfo[i].nFingerprintLen = szFingerPrintInfo.length;    			  // 指纹数据长度,不超过1.5
			
			// 指纹数据
			stIn.stuFingerPrintInfo[i].pFingerprintData.write(0, szFingerPrintInfo, 0, szFingerPrintInfo.length);	
		}
		
		// 任务ID
		stIn.nTaskID = 1;
		
		/*
		 * 出参
		 */
		NET_OUT_SMARTLOCK_UPDATE_USER_INFO stOut = new NET_OUT_SMARTLOCK_UPDATE_USER_INFO();
		
		if(netsdkApi.CLIENT_UpdateSmartLockUser(loginHandle, stIn, stOut, 6000)) {
			System.out.println("添加智能锁用户成功！");
		} else {
			System.err.println("添加智能锁用户失败, " + getErrorCode());
		}
	}
	
	/**
	 * 删除智能锁用户
	 * 函数功能：根据用户ID删除对应的用户名称，包括ID下的指纹密码卡信息
	 */
	public void RemoveSmartLock() {
		/*
		 * 入参
		 */
		NET_IN_SMARTLOCK_REMOVE_USER_INFO stIn = new NET_IN_SMARTLOCK_REMOVE_USER_INFO();
		// 智能锁序列号
		String sn = "4D062A7PAZCC9C4";
		System.arraycopy(sn.getBytes(), 0, stIn.szSerialNumber, 0, sn.getBytes().length);
		
		// 身份拥有者(与AccessControlCard记录集中的UserID概念一致)
		String credentialHolder= "23355555";
		System.arraycopy(credentialHolder.getBytes(), 0, stIn.szCredentialHolder, 0, credentialHolder.getBytes().length);
		
		// 开门方式, 0 表示全部， 1-卡开门， 2-密码开门， 3-指纹
		stIn.emType = 0;  
		
		// 某种开门方式的索引号，-1表示全部
		stIn.nIndex = -1;
		
		// 任务ID
		stIn.nTaskID = 1;
		
		/*
		 * 出参
		 */
		NET_OUT_SMARTLOCK_REMOVE_USER_INFO stOut = new NET_OUT_SMARTLOCK_REMOVE_USER_INFO();
		
		if(netsdkApi.CLIENT_RemoveSmartLockUser(loginHandle, stIn, stOut, 5000)) {
			System.out.println("删除智能锁用户成功");
		} else {
			System.err.println("删除智能锁用户失败, " + getErrorCode());
		}
	}
	
	/**
	 * 获取智能锁用户
	 * 函数功能：通过此接口去查找设备上已经下发的用户信息，入参偏移量表示从哪个位置开始查询，每次最多返回32条信息
	 * 暂不支持
	 */
	public void GetSmartLock() {
		/*
		 * 入参
		 */
		NET_IN_GET_SMART_LOCK_REGISTER_INFO stIn = new NET_IN_GET_SMART_LOCK_REGISTER_INFO();
		// 智能锁序列号
		String sn = "4D062A7PAZCC9C4";
		System.arraycopy(sn.getBytes(), 0, stIn.szSerialNumber, 0, sn.getBytes().length);
		
		// 用户列表偏移量
		stIn.nOffset = 0;
		
		/*
		 * 出参
		 */
		NET_OUT_GET_SMART_LOCK_REGISTER_INFO stOut = new NET_OUT_GET_SMART_LOCK_REGISTER_INFO();
		
		if(netsdkApi.CLIENT_GetSmartLockRegisterInfo(loginHandle, stIn, stOut, 5000)) {
			System.out.println("查询到的用户数量：" + stOut.nTotalCount);
			System.out.println("实际查询到的用户数量：" + stOut.nReturnCount + "\n");
			
			for(int i = 0; i < stOut.nReturnCount; i++) {
				System.out.println("开锁方式类型：" + stOut.stuRegisterInfo[i].emType);
				System.out.println("用户ID：" + new String(stOut.stuRegisterInfo[i].szUserID).trim());
				System.out.println("用户名称：" + new String(stOut.stuRegisterInfo[i].szName).trim() + "\n");
			}
		} else {
			System.err.println("获取智能锁用户失败, " + getErrorCode());
		}
	}
	
	/**
	 * 修改智能锁用户
	 * 函数功能：单独修改用户ID对应的用户名称。
	 * 暂不支持
	 */
	public void ModifySmartLock() {
		/*
		 * 入参
		 */
		NET_IN_SET_SMART_LOCK_USERNAME stIn = new NET_IN_SET_SMART_LOCK_USERNAME();
		stIn.emType = 1;   // 开锁方式类型, 0-未知， 1-卡开门， 2-密码开门， 3-指纹
		
		// 智能锁序列号
		String sn = "4D062A7PAZCC9C4";
		System.arraycopy(sn.getBytes(), 0, stIn.szSerialNumber, 0, sn.getBytes().length);
		
		// 用户ID(非AccessControlCard记录集中的UserID概念)
		String userId = "23355555";
		System.arraycopy(userId.getBytes(), 0, stIn.szUserID, 0, userId.getBytes().length);
		
		// 需要修改成的名称
		String name = "dkkdkdhj";
		try {
			System.arraycopy(name.getBytes("GBK"), 0, stIn.szName, 0, name.getBytes("GBK").length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
		/*
		 * 出参
		 */
		NET_OUT_SET_SMART_LOCK_USERNAME stOut = new NET_OUT_SET_SMART_LOCK_USERNAME();
		
		if(netsdkApi.CLIENT_SetSmartLockUsername(loginHandle, stIn, stOut, 5000)) {
			System.out.println("修改智能锁用户成功！");
		} else {
			System.err.println("修改智能锁用户失败, " + getErrorCode());
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
		SmartLock demo = new SmartLock();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
