package com.netsdk.demo.event;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.NetSDKLib.NET_IN_FACE_OPEN_DOOR;
import com.netsdk.lib.NetSDKLib.NET_OUT_FACE_OPEN_DOOR;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_HEALTH_CODE_STATUS;
import com.netsdk.lib.structure.NET_HEALTH_CODE_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.stream.FileImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TwiceEducation {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private LLong loginHandle = new LLong(0);       
    
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

	public void Test_FaceOpenDoor(){
		if(loginHandle.longValue()==0){
			return;
		}
		//入参
		NET_IN_FACE_OPEN_DOOR pInParam=new NET_IN_FACE_OPEN_DOOR();
		/*NET_OPENDOOR_IMAGEINFO stuImageInfo=new NET_OPENDOOR_IMAGEINFO();
		int nReadLenOnce = 32*1024 - 32;
		stuImageInfo.pLibImage=new Memory(nReadLenOnce);
		stuImageInfo.nLibImageLen=image("D:\\1.jpg").length;
		System.arraycopy(image("D:\\1.jpg"), 0, stuImageInfo.pLibImage.getByteArray(0, nReadLenOnce),0 , image("D:\\1.jpg").length);
		stuImageInfo.pSnapImage=new Memory(nReadLenOnce);
		stuImageInfo.nSnapImageLen=image("D:\\2.jpg").length;
		System.arraycopy(image("D:\\2.jpg"), 0, stuImageInfo.pSnapImage.getByteArray(0, nReadLenOnce), 0, image("D:\\2.jpg").length);
		pInParam.stuImageInfo=stuImageInfo;*/
		//int nReadLenOnce = 32*1024 - 32;
		pInParam.nChannel=0;
		pInParam.emCompareResult=0;
		byte[] UserID="147850".getBytes();
		System.arraycopy(UserID, 0, pInParam.stuMatchInfo.szUserID, 0, UserID.length);
		
		NET_HEALTH_CODE_INFO health_code_info=new NET_HEALTH_CODE_INFO();
		//TODO
		health_code_info.emHealthCodeStatus=EM_HEALTH_CODE_STATUS.EM_HEALTH_CODE_STATUS_GREEN.ordinal();
		pInParam.stuMatchInfo.pstuHealthCodeInfo=new Memory(health_code_info.size());
		ToolKits.SetStructDataToPointer(health_code_info,pInParam.stuMatchInfo.pstuHealthCodeInfo,0);


		/*
		 * pInParam.stuImageInfo.pLibImage=new Memory(nReadLenOnce);
		 * pInParam.stuImageInfo.nLibImageLen=image("D:\\1.jpg").length;
		 * System.arraycopy(image("D:\\1.jpg"), 0,
		 * pInParam.stuImageInfo.pLibImage.getByteArray(0, nReadLenOnce), 0,
		 * image("D:\\1.jpg").length); pInParam.stuImageInfo.pSnapImage=new
		 * Memory(nReadLenOnce);
		 * pInParam.stuImageInfo.nSnapImageLen=image("D:\\2.jpg").length;
		 * System.arraycopy(image("D:\\2.jpg"), 0,
		 * pInParam.stuImageInfo.pSnapImage.getByteArray(0, nReadLenOnce), 0,
		 * image("D:\\2.jpg").length);
		 */
		
		//出参
		NET_OUT_FACE_OPEN_DOOR pOutParam=new NET_OUT_FACE_OPEN_DOOR();
		
		boolean bRet =netSdk.CLIENT_FaceOpenDoor(loginHandle, pInParam, pOutParam, 3000);
	    if (!bRet)
	    {
			System.err.printf("Test_FaceOpenDoor field[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
	        return;
	    }
	}

	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netSdk.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  LastError.java }";
	}
	public byte[] image(String path){
	    byte[] data = null;
	    FileImageInputStream input = null;
	    try {
	      input = new FileImageInputStream(new File(path));
	      ByteArrayOutputStream output = new ByteArrayOutputStream();
	      byte[] buf = new byte[1024];
	      int numBytesRead = 0;
	      while ((numBytesRead = input.read(buf)) != -1) {
	      output.write(buf, 0, numBytesRead);
	      }
	      data = output.toByteArray();
	      output.close();
	      input.close();
	    }
	    catch (FileNotFoundException ex1) {
	      ex1.printStackTrace();
	    }
	    catch (IOException ex1) {
	      ex1.printStackTrace();
	    }
		return data;
	}
	
	public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "Test_FaceOpenDoor" , "Test_FaceOpenDoor")));
		menu.run();
	}
   ////////////////////////////////////////////////////////////////
      private String m_strIp 				= "172.5.4.178";
      private int    m_nPort 				= 37777;
      private String m_strUser 			    = "admin";
      private String m_strPassword 		    = "admin123";
   ///////////////////////////////////////////////////////////////

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

public static void main(String []args){
	TwiceEducation XM=new TwiceEducation();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}

}
