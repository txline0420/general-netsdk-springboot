package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class findByPasser {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);  
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    public class picUrl extends SdkStructure{
    	public String URL ;
    }
    
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
private String m_strIp 				    = "172.23.12.138";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////
String[] faceSexStr = {"未知", "男", "女"};
String[] idStr = {"未知", "身份证", "护照",};
	
	
	public void findFaceRecognitionDB() throws UnsupportedEncodingException {		
		
		// IVVS设备，查询条件只有  stInStartFind.stPerson 里的参数有效
		NET_IN_STARTFIND_FACERECONGNITION stInStartFind = new NET_IN_STARTFIND_FACERECONGNITION();
		// 人员信息查询条件是否有效, 并使用扩展结构体
		stInStartFind.bPersonExEnable = 0;   
		
		System.arraycopy("1".getBytes(), 0, stInStartFind.stPersonInfoEx.szGroupID, 0, "1".getBytes().length);   // 人员组ID
		
	    //设置过滤条件
		stInStartFind.stFilterInfo.nGroupIdNum = 1;   // 人员组数
		System.arraycopy("1".getBytes(), 0, stInStartFind.stFilterInfo.szGroupIdArr[0].szGroupId, 0, "1".getBytes().length);  // 人员组ID
		stInStartFind.stFilterInfo.nRangeNum = 1;
		stInStartFind.stFilterInfo.szRange[0] = NetSDKLib.EM_FACE_DB_TYPE.NET_FACE_DB_TYPE_PASSERBY;
		stInStartFind.stFilterInfo.emFaceType = NetSDKLib.EM_FACERECOGNITION_FACE_TYPE.EM_FACERECOGNITION_FACE_TYPE_UNKOWN;

		stInStartFind.nChannelID = -1;
	    
	    //让设备根据查询条件整理结果集
	    NET_OUT_STARTFIND_FACERECONGNITION stOutParam = new NET_OUT_STARTFIND_FACERECONGNITION();
	    stInStartFind.write();
	    stOutParam.write();
	    if(netSdk.CLIENT_StartFindFaceRecognition(loginHandle, stInStartFind,  stOutParam, 2000))
	    {
	    	System.out.printf("Handle Token = %d\n" , stOutParam.nToken);
	        
	    	int doNextCount = 0;	// 查询次数
	    	int count = 10;    		// 每次查询的个数
	        //分页查找数据
	        NetSDKLib.NET_IN_DOFIND_FACERECONGNITION  stFindIn = new NetSDKLib.NET_IN_DOFIND_FACERECONGNITION();
	        stFindIn.lFindHandle = stOutParam.lFindHandle;
	        stFindIn.nCount      = count;  // 当前想查询的记录条数
	      	stFindIn.nBeginNum   = 0;  //每次递增
	        
	        NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION stFindOut = new NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION();;	
	        stFindOut.bUseCandidatesEx = 1;   // 使用候选对象扩展结构体
	        for(int m = 0 ; m< count; m++) {
	        	stFindOut.stuCandidatesEx[m].stPersonInfo.szFacePicInfo[0].nFilePathLen = 1024;
	        	stFindOut.stuCandidatesEx[m].stPersonInfo.szFacePicInfo[0].pszFilePath = new Memory(1024);	
	        }
	        
	        do 
	        {
	        	stFindIn.write();
	        	stFindOut.write();
	            if(netSdk.CLIENT_DoFindFaceRecognition(stFindIn, stFindOut, 1000))
	            {
	                System.out.printf("Record Number [%d]\n" , stFindOut.nCadidateExNum);
	      
	        		for(int i = 0; i < stFindOut.nCadidateExNum; i++)
	                {                         
	        			int index = i + doNextCount * count;    // 查询的总个数 - 1, 从0开始
	                	
	        			try {
							System.out.println("人员所属组名" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szGroupName, "GBK").trim()
									          +"人员唯一标识符" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szUID).trim()
									          +"人员名称" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szPersonName, "GBK").trim()
									          +"性别" + new String(faceSexStr[stFindOut.stuCandidatesEx[i].stPersonInfo.bySex]).trim()
									          +"证件类型" + new String(idStr[stFindOut.stuCandidatesEx[i].stPersonInfo.byIDType]).trim()
									          +"证件编码" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szID).trim()
									          +"生日" + String.valueOf(stFindOut.stuCandidatesEx[i].stPersonInfo.wYear) + "-" + 
							                    String.valueOf(0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byMonth) + "-" + 
							                    String.valueOf(0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byDay)
							                  +"省份" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szProvince, "GBK").trim()
							                  +"城市" + new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szCity, "GBK").trim());
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	               	        		
	                    
	                    // 图片地址
	                    for (int j = 0; j < stFindOut.stuCandidatesEx[i].stPersonInfo.wFacePicNum; j++)
	                    {	        
	                    	//picUrl pic= new picUrl();
	                    	//ToolKits.GetPointerData(stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].pszFilePath, pic);
	                    	//System.out.println(pic.URL);
	                    	//System.out.println(GetPointerDataToString(stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].pszFilePath));
	                    	downloadRemoteFile(stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].pszFilePath);
	                    }         
	                }                          
	            }
	            else
	            {
	            	System.out.printf("CLIENT_DoFindFaceRecognition Failed!LastError = %x\n" , netSdk.CLIENT_GetLastError());
	            	break;
	            }

	            if( stFindOut.nCadidateNum < stFindIn.nCount)
	            {
	            	System.out.printf("No More Record, Find End!\n");
	                break;
	            } else {
		            stFindIn.nBeginNum += count;
		            doNextCount++;
	            }
	        }while (true);
	          
	        netSdk.CLIENT_StopFindFaceRecognition(stOutParam.lFindHandle);	        
	    }
	    else
	    {
	        System.out.println("CLIENT_StartFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
	    }
	}
	
	/**
	 * 下载图片
	 * @param szFileName 需要下载的文件名
	 */
	public boolean downloadRemoteFile(Pointer szFileName) {
		// 入参
		NET_IN_DOWNLOAD_REMOTE_FILE stIn = new NET_IN_DOWNLOAD_REMOTE_FILE();
		stIn.pszFileName = szFileName/*new NativeString(szFileName).getPointer()*/;
		stIn.pszFileDst = new NativeString("./face"+System.currentTimeMillis()+".jpg").getPointer(); // 存放路径
		
		// 出参
		NET_OUT_DOWNLOAD_REMOTE_FILE stOut = new NET_OUT_DOWNLOAD_REMOTE_FILE();
		
		stIn.write();
		stOut.write();
		if(netSdk.CLIENT_DownloadRemoteFile(loginHandle, stIn, stOut, 3000)) {
			System.out.println("下载图片成功!");
		} else {
			System.err.println("下载图片失败!" +  ToolKits.getErrorCode());
			return false;
		}
		return true;
	}
	
	public String GetPointerDataToString(Pointer pointer) {	
		String str = "";
		if(pointer == null) {
			return str;
		}		
		
		int length = 0;
		while (length < 2048) {
			if (pointer.getByte(length) == '\0') {
				break;
			}
			++length;
		}
		
		byte[] buffer = pointer.getByteArray(0, length);
		try {
			str = new String(buffer, "GBK").trim();
		} catch (UnsupportedEncodingException e) {
			return str;
		}

		return str;
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
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "findFaceRecognitionDB" , "findFaceRecognitionDB"));	
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		findByPasser demo = new findByPasser();
		demo.InitTest();
		demo.RunTest();
		demo.LoginOut();
	}
}
