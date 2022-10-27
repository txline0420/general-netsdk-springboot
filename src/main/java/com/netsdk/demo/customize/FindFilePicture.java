package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

/**
 * 查找图片demo
 */
public class FindFilePicture {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄

	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
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
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		
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
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		EndTest();
    	}
	}

	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	
	/**
	 * 查找录像，需要指定查询录像的最大个数
	 */
	public void QueryRecordFile() {
		NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();
		stTimeStart.dwYear = 2017;
		stTimeStart.dwMonth = 8;
		stTimeStart.dwDay = 11;
		stTimeStart.dwHour = 2;
		stTimeStart.dwMinute = 0;
		stTimeStart.dwSecond = 0;
		
		NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();
		stTimeEnd.dwYear = 2017;
		stTimeEnd.dwMonth = 8;
		stTimeEnd.dwDay = 11;
		stTimeEnd.dwHour = 3;
		stTimeEnd.dwMinute = 0;
		stTimeEnd.dwSecond = 0;
		
		//**************按时间查找视频文件**************
		int nFileCount = 50; //每次查询的最大文件个数
		NetSDKLib.NET_RECORDFILE_INFO[] numberFile = (NetSDKLib.NET_RECORDFILE_INFO[])new NetSDKLib.NET_RECORDFILE_INFO().toArray(nFileCount);
		int maxlen = nFileCount * numberFile[0].size();

		IntByReference outFileCoutReference = new IntByReference(0);
		
		boolean cRet = netsdkApi.CLIENT_QueryRecordFile(loginHandle, 0, 0, stTimeStart, stTimeEnd, null, numberFile, maxlen, outFileCoutReference, 5000, false);
		
		if(cRet) {
			System.out.println("QueryRecordFile  Succeed! \n" + "查询到的视频个数：" + outFileCoutReference.getValue() 
							   + "\n" + "码流类型：" + numberFile[0].bRecType);	
			for(int i=0; i<outFileCoutReference.getValue(); i++) {
				System.out.println("【" + i + "】：");
				System.out.println("开始时间：" + numberFile[i].starttime);
				System.out.println("结束时间：" + numberFile[i].endtime);
				System.out.println("通道号：" + numberFile[i].ch);
			}
			
		} else {
			System.err.println("QueryRecordFile  Failed!" + netsdkApi.CLIENT_GetLastError());
		}
	}
	
	
	/**
	 *  查询录像/图片文件
	 */
	public void FindPicture() {
		int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE;
		
		// 查询条件
		NetSDKLib.NET_IN_MEDIA_QUERY_FILE queryCondition = new NetSDKLib.NET_IN_MEDIA_QUERY_FILE();
		
		// 工作目录列表,一次可查询多个目录,为空表示查询所有目录。
		// 目录之间以分号分隔,如“/mnt/dvr/sda0;/mnt/dvr/sda1”,szDirs==null 或"" 表示查询所有
		queryCondition.szDirs = "";                 
		
		// 文件类型,0:查询任意类型,1:查询jpg图片,2:查询dav
		queryCondition.nMediaType = 1;
		
		// 通道号从0开始,-1表示查询所有通道
		queryCondition.nChannelID = -1;
		
		// 开始时间
		queryCondition.stuStartTime.dwYear = 2020;
		queryCondition.stuStartTime.dwMonth = 12;
		queryCondition.stuStartTime.dwDay = 1;
		queryCondition.stuStartTime.dwHour = 0;
		queryCondition.stuStartTime.dwMinute = 0;
		queryCondition.stuStartTime.dwSecond = 0;
		
		// 结束时间
		queryCondition.stuEndTime.dwYear = 2020;
		queryCondition.stuEndTime.dwMonth = 12;
		queryCondition.stuEndTime.dwDay = 25;
		queryCondition.stuEndTime.dwHour = 0;
		queryCondition.stuEndTime.dwMinute = 0;
		queryCondition.stuEndTime.dwSecond = 0;
		
		queryCondition.write();
		LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, type, queryCondition.getPointer(), null, 3000);
		if(lFindHandle.longValue() == 0) {
			System.err.println("FindFileEx Failed!" + getErrorCode());
			return;
		}
		queryCondition.read();
		
		///////////////////////////
		int nMaxConut = 10;  // 每次查询的个数，循环查询
		NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] pMediaQueryFile = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[nMaxConut];
		for (int i = 0; i < pMediaQueryFile.length; ++i) {
			pMediaQueryFile[i] = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE();
		}
		
		int MemorySize = pMediaQueryFile[0].size() * nMaxConut;
		Pointer mediaFileInfo = new Memory(MemorySize);
		mediaFileInfo.clear(MemorySize);
		
		ToolKits.SetStructArrToPointerData(pMediaQueryFile, mediaFileInfo);
		
		//循环查询
		int nCurCount = 0;
		int nFindCount = 0;
		while(true) {
			int nRet = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, mediaFileInfo, MemorySize, null, 3000);
			ToolKits.GetPointerDataToStructArr(mediaFileInfo, pMediaQueryFile);
			System.out.println("nRet : " + nRet);
			
			if (nRet <= 0) {
                break;
			} 
	
			for (int i = 0; i < nRet; i++) {
				nFindCount = i + nCurCount * nMaxConut;
				System.out.println("[" + nFindCount + "]通道号 :" + pMediaQueryFile[i].nChannelID);
				System.out.println("[" + nFindCount + "]开始时间 :" + pMediaQueryFile[i].stuStartTime.toStringTime());
				System.out.println("[" + nFindCount + "]结束时间 :" + pMediaQueryFile[i].stuEndTime.toStringTime());
				if(pMediaQueryFile[i].byFileType == 1) {
					System.out.println("[" + nFindCount + "]文件类型 : jpg图片");
				} else if ((pMediaQueryFile[i].byFileType == 2)){
					System.out.println("[" + nFindCount + "]文件类型 : dav");
				}
				System.out.println("[" + nFindCount + "]文件路径 :" + new String(pMediaQueryFile[i].szFilePath).trim());
			}
			
			if(nRet < nMaxConut) {
				break;
			} else {
				nCurCount++;
			}
		} 
		
		// 关闭查询
		netsdkApi.CLIENT_FindCloseEx(lFindHandle);	
	}
	
	////////////////////////////////////////////////////////////////
//	String m_strIp 			= "172.23.2.92";
	String m_strIp 			= "10.80.9.45";
//	String m_strIp 			= "10.80.9.44";

	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "dahua2020";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		
		menu.addItem(new CaseMenu.Item(this , "FindPicture" , "FindPicture"));
		
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		FindFilePicture demo = new FindFilePicture();	
		
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
