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

public class FindMark {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);
    
    //文件查询句柄
    private LLong lFindHandle = new LLong(0);    
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    
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
	
////////////////////////////////////////////////////////////////
private String m_strIp 				    = "172.9.9.81";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void FindFileEx(){
	
	int emType=NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_SNAPSHOT_WITH_MARK;
	MEDIAFILE_SNAPSHORT_WITH_MARK_PARAM pQueryCondition=new MEDIAFILE_SNAPSHORT_WITH_MARK_PARAM();
	pQueryCondition.stuStartTime.setTime(2020, 2, 16, 00, 00, 00);
	pQueryCondition.stuEndTime.setTime(2020, 2, 18, 23, 59, 59);
	pQueryCondition.write();
	lFindHandle=netSdk.CLIENT_FindFileEx(loginHandle, emType, pQueryCondition.getPointer(), null, 3000);
	if(lFindHandle.longValue()==0){
		System.err.println("FindFileEx Failed!" + netSdk.CLIENT_GetLastError());
		return;
	}
	
	int nMaxConut=10;
	
	MEDIAFILE_SNAPSHORT_WITH_MARK_INFO[] MarkInfo=new MEDIAFILE_SNAPSHORT_WITH_MARK_INFO[nMaxConut];
	
	for (int i = 0; i < MarkInfo.length; ++i) {
		MarkInfo[i] = new MEDIAFILE_SNAPSHORT_WITH_MARK_INFO();
		/*System.out.println(MarkInfo[i].nChannelID+MarkInfo[i].nFileSize+new String(MarkInfo[i].szFilePath).trim());
		System.out.println(MarkInfo[i].stuMarkInfo.stuPosition.nx+MarkInfo[i].stuMarkInfo.stuPosition.ny);*/
	}
	
	int MemorySize=MarkInfo[0].size()*nMaxConut;
	Pointer pointer=new Memory(MemorySize);
	pointer.clear(MemorySize);	
	ToolKits.SetStructArrToPointerData(MarkInfo, pointer);
	
	//循环查询
	int nCurCount = 0;
	int nFindCount = 0;
	while(true) {
		int nRetCount = netSdk.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, pointer, MemorySize, null, 3000);
		ToolKits.GetPointerDataToStructArr(pointer, MarkInfo);
		
		if (nRetCount <= 0) {
			System.err.println("FindNextFileEx failed!" + netSdk.CLIENT_GetLastError());
            break;
		} 
		for (int i = 0; i < nRetCount; i++) {
			nFindCount = i + nCurCount * nMaxConut;
			System.out.println("[" + nFindCount + "]通道号 :" + MarkInfo[i].nChannelID);
			System.out.println("[" + nFindCount + "]开始时间 :" + MarkInfo[i].stuStartTime.toStringTime());
			System.out.println("[" + nFindCount + "]结束时间 :" + MarkInfo[i].stuEndTime.toStringTime());
			System.out.println("[" + nFindCount + "]文件路径 :" + new String(MarkInfo[i].szFilePath).trim());
			DownloadRemoteFile(new String(MarkInfo[i].szFilePath).trim());
		}
		if(nRetCount < nMaxConut) {
			break;
		} else {
			nCurCount++;
		}
	}
	netSdk.CLIENT_FindCloseEx(lFindHandle);
}

public  void  DownloadRemoteFile(String filePath){
	NET_IN_DOWNLOAD_REMOTE_FILE pInParam=new NET_IN_DOWNLOAD_REMOTE_FILE();
	pInParam.pszFileName = new NativeString(filePath).getPointer();
	pInParam.pszFileDst = new NativeString("./face.jpg").getPointer();
	NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam=new NET_OUT_DOWNLOAD_REMOTE_FILE();
	if(!netSdk.CLIENT_DownloadRemoteFile(loginHandle, pInParam, pOutParam, 3000)){
			System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	}else{
		System.out.println("CLIENT_DownloadRemoteFile success");
	}
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

public void RunTest(){
	CaseMenu menu=new CaseMenu();
	menu.addItem((new CaseMenu.Item(this , "FindFileEx" , "FindFileEx")));
	menu.run();
}

public static void  main(String []args){
	FindMark XM=new FindMark();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}
}
