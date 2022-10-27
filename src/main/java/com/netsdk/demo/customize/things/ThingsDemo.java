package com.netsdk.demo.customize.things;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_PROPERTIES_VALUE_TYPE;
import com.netsdk.lib.enumeration.EM_THINGS_CONNECT_STATE;
import com.netsdk.lib.enumeration.EM_THINGS_SERVICE_TYPE;
import com.netsdk.lib.enumeration.EM_THINGS_TRIGGER_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.text.SimpleDateFormat;

import static com.netsdk.lib.Utils.getOsPrefix;



/**
 * @author 251823
 * @description  智慧用电功能
 * @date 2022/04/27
 */
public class ThingsDemo {

	// SDk对象初始化
	public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
	public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

	// 判断是否初始化
	private static boolean bInit = false;
	// 判断log是否打开
	private static boolean bLogOpen = false;
	// 设备信息
	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	// 登录句柄
	private static LLong m_hLoginHandle = new LLong(0);
	// 语音对讲句柄
	public static LLong m_hTalkHandle = new LLong(0);	
	//设备录音记录开关，控制设备开启对讲后，是否将设备收集音频数据记录到指定文件
	public static Boolean recordFlag = false;

	// 回调函数需要是静态的，防止被系统回收
	// 断线回调
	private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();
	// 重连回调
	private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();
	
	// 订阅句柄
	private static LLong AttachHandle = new LLong(0);

	// 编码格式
	public static String encode;

	static {
		String osPrefix = getOsPrefix();
		if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
			encode = "GBK";
		} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
			encode = "UTF-8";
		}
	}

    // 获取当前时间
    public static String getDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.
                format(new java.util.Date()).
                replace(" ", "_").
                replace(":", "-");
        return date;
    }

	/**
	 * 初始化SDK库
	 */
	public static boolean Init() {
		bInit = netsdk.CLIENT_Init(disConnectCB, null);
		if (!bInit) {
			System.out.println("Initialize SDK failed");
			return false;
		}
		// 配置日志
		ThingsDemo.enableLog();

		// 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
		netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

		// 设置登录超时时间和尝试次数，可选
		// 登录请求响应超时时间设置为3S
		int waitTime = 3000;
		// 登录时尝试建立链接 1 次
		int tryTimes = 1;
		netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);
		// 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		// 登录时尝试建立链接的超时时间
		netParam.nConnectTime = 10000;
		// 设置子连接的超时时间
		netParam.nGetConnInfoTime = 3000;
		netsdk.CLIENT_SetNetworkParam(netParam);
		return true;
	}

	/**
	 * 打开 sdk log
	 */
	private static void enableLog() {
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		File path = new File("sdklog/");
		if (!path.exists())
			path.mkdir();

		// 这里的log保存地址依据实际情况自己调整
		String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + getDate()
				+ ".log";
		setLog.nPrintStrategy = 0;
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		System.out.println(logPath);
		setLog.bSetPrintStrategy = 1;
		bLogOpen = netsdk.CLIENT_LogOpen(setLog);
		if (!bLogOpen)
			System.err.println("Failed to open NetSDK log");
	}

	/**
	 * 高安全登录
	 */
	public void loginWithHighLevel() {
		// 输入结构体参数
		NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
			{
				szIP = m_strIpAddr.getBytes();
				nPort = m_nPort;
				szUserName = m_strUser.getBytes();
				szPassword = m_strPassword.getBytes();
			}
		};
		// 输出结构体参数
		NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

		// 写入sdk
		m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
		if (m_hLoginHandle.longValue() == 0) {
			System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
					netsdk.CLIENT_GetLastError());
		} else {
			// deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
			System.out.println("Login Success");
			System.out.println("Device Address：" + m_strIpAddr);
			// System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
		}
	}

	/**
	 * 退出
	 */
	public void logOut() {
		if (m_hLoginHandle.longValue() != 0) {
			netsdk.CLIENT_Logout(m_hLoginHandle);
			System.out.println("LogOut Success");
		}
	}

	/**
	 * 清除 sdk环境
	 */
	public static void cleanup() {
		if (bLogOpen) {
			netsdk.CLIENT_LogClose();
		}
		if (bInit) {
			netsdk.CLIENT_Cleanup();
		}
	}

	/**
	 * 清理并退出
	 */
	public static void cleanAndExit() {
		netsdk.CLIENT_Cleanup();
		System.exit(0);
	}

	
	/**
	 * 智慧用电Things配置获取接口
	 */
	public void getThingsConfig() {	
		//    参数json示例
		//	  "GetThingsConfig" : {
		//	      "In" : {
		//	        "szPropeName" : ["SBJCXX_SBBM", "SBJCXX_SBXH", "SBJCXX_XLH", "CJXX_YJBJLB"],
		//			"szDeviceID": "2",
		//			"szProductID": "001"
		//	      },
		//	      "Out" : {
		//			"nMaxPropertiesCount" : 5
		//		  }
		//	   }		
		
		// 入参		
		NET_IN_THINGS_GET pIn = new NET_IN_THINGS_GET();
		String szProductID ="001"; // 产品ID
		System.arraycopy(szProductID.getBytes(), 0, pIn.szProductID, 0, szProductID.getBytes().length);		
		String szDeviceID ="2";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);		
		String[] szPropeNameArr = {"SBJCXX_SBBM", "SBJCXX_SBXH", "SBJCXX_XLH", "CJXX_YJBJLB"};	// 物模型标识
		int nProperCount = szPropeNameArr.length;// 物模型标识个数
		pIn.nProperCount = nProperCount;// pstuGetInProperName个数	
		NET_PROPERTIES_NAME[] pstuGetInProperName = new NET_PROPERTIES_NAME[nProperCount];
		for (int i = 0; i < pstuGetInProperName.length; i++) {
			pstuGetInProperName[i] = new NET_PROPERTIES_NAME(); // 必须初始化数组
			System.arraycopy(szPropeNameArr[i].getBytes(), 0, pstuGetInProperName[i].szPropertiesName, 0, szPropeNameArr[i].getBytes().length);	
		}	
		pIn.pstuGetInProperName = new Memory(pstuGetInProperName[0].size()*nProperCount ); // 内存申请
		pIn.pstuGetInProperName.clear(pstuGetInProperName[0].size()*nProperCount );
		ToolKits.SetStructArrToPointerData(pstuGetInProperName, pIn.pstuGetInProperName);	// 将物模型标识信息传给指针						
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_GET pOut = new NET_OUT_THINGS_GET();		
		int nMaxPropertiesCount =5; 
		pOut.nMaxPropertiesCount = nMaxPropertiesCount; // 用户分配的pstuGetOutProperInfo个数
		NET_PROPERTIES_INFO[] pstuGetOutProperInfo = new NET_PROPERTIES_INFO[nMaxPropertiesCount];
		for (int i = 0; i < pstuGetOutProperInfo.length; i++) {
			pstuGetOutProperInfo[i] = new NET_PROPERTIES_INFO(); // 必须初始化数组
		}
		pOut.pstuGetOutProperInfo = new Memory(pstuGetOutProperInfo[0].size()*nMaxPropertiesCount); // 内存申请
		pOut.pstuGetOutProperInfo.clear(pstuGetOutProperInfo[0].size()*nMaxPropertiesCount);
		ToolKits.SetStructArrToPointerData(pstuGetOutProperInfo, pOut.pstuGetOutProperInfo);	// 将物模型标志信息传给指针				
		pOut.write();
		
		boolean flg = netsdk.CLIENT_GetThingsConfig(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			pOut.read();						
			ToolKits.GetPointerDataToStructArr(pOut.pstuGetOutProperInfo,pstuGetOutProperInfo); // 将指针转为具体的信息
			System.out.println("产品ID:"+  new String( pOut.szProductID));
			System.out.println("设备ID:"+new String(pOut.szDeviceID));
			int nRetPropertiesCount = pOut.nRetPropertiesCount;		
			System.out.println("实际返回的pstuGetOutProperInfo个数:"+nRetPropertiesCount);
			for (int i = 0; i < nRetPropertiesCount; i++) {
				System.out.println("-----------第"+(i+1)+"个属性信息---------");
				System.out.println("szValue对应的类型:"+EM_PROPERTIES_VALUE_TYPE.getNoteByValue(pstuGetOutProperInfo[i].emValueType));
				System.out.println("szKey:"+new String(pstuGetOutProperInfo[i].szKey));
				if(pstuGetOutProperInfo[i].emValueType == EM_PROPERTIES_VALUE_TYPE.EM_PROPERTIES_VALUE_INTARRAY.getValue()) {
					NET_PROPERTIES_INTARRAY_VALUE[] stuIntArrayValue = pstuGetOutProperInfo[i].stuIntArrayValue;
					int nIntArrayValueNum = pstuGetOutProperInfo[i].nIntArrayValueNum;// stuIntArrayValue实际个数
					for (int j = 0; j < nIntArrayValueNum; j++) {
						System.out.println("stuIntArrayValue"+j+":"+stuIntArrayValue[j].nValue);												
					}
				}else {
					System.out.println("szValue:"+pstuGetOutProperInfo[i].szValue);
				}				
			}															
		} else {
			System.err.println("CLIENT_GetThingsConfig fail:"+ToolKits.getErrorCode());
		}				
	}
	/**
	 * 智慧用电Things配置设置接口
	 */
	public void setThingsConfig() {	
		//    参数json示例
		//	  "SetThingsConfig" : {
		//	      "In" : {
		//			"szDeviceID": "2",
		//			"szProductID": "001",
		//			"ProperInfo":
		//			[
		//			  {
		//			    "ValueType":1,
		//			    "Key":"BJPZ_GZYJFZ_AX",
		//			    "Value":"30"
		//			  }
		//			]
		//	      },
		//	      "Out" : {
		//		  }
		//	   }	
		
		// 入参		
		NET_IN_THINGS_SET pIn = new NET_IN_THINGS_SET();
		String szProductID ="001"; // 产品ID
		System.arraycopy(szProductID.getBytes(), 0, pIn.szProductID, 0, szProductID.getBytes().length);		
		String szDeviceID ="2";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);
		int nProperCount = 1;
		pIn.nProperCount = nProperCount;// pstuSetInProperInfo个数
		NET_PROPERTIES_INFO[] pstuSetInProperInfo = new NET_PROPERTIES_INFO[nProperCount];
		for (int i = 0; i < pstuSetInProperInfo.length; i++) {
			pstuSetInProperInfo[i] = new NET_PROPERTIES_INFO(); // 必须初始化数组
		}
		// 属性值信息  
		NET_PROPERTIES_INFO obj1  = new NET_PROPERTIES_INFO();
		// 当emValueType为EM_PROPERTIES_VALUE_INTARRAY时，对stuIntArrayValue进行赋值；当emValueType为其余枚举时对stuIntArrayValue赋值
		obj1.emValueType = EM_PROPERTIES_VALUE_TYPE.EM_PROPERTIES_VALUE_INT.getValue();  // 对应的类型；有int,bool,string类型 {@link EM_PROPERTIES_VALUE_TYPE}
		String Key = "BJPZ_GZYJFZ_AX";
		System.arraycopy(Key.getBytes(), 0, obj1.szKey, 0, Key.getBytes().length);	
		String szValue = "30";
		System.arraycopy(szValue.getBytes(), 0, obj1.szValue, 0, szValue.getBytes().length);					
		pstuSetInProperInfo[0] = obj1;										
		pIn.pstuSetInProperInfo = new Memory(pstuSetInProperInfo[0].size()*nProperCount); // 内存申请
		pIn.pstuSetInProperInfo.clear(pstuSetInProperInfo[0].size()*nProperCount);
		ToolKits.SetStructArrToPointerData(pstuSetInProperInfo, pIn.pstuSetInProperInfo);	// 将物模型标志信息传给指针				
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_SET pOut = new NET_OUT_THINGS_SET();				
		pOut.write();
		
		boolean flg = netsdk.CLIENT_SetThingsConfig(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			System.out.println("CLIENT_SetThingsConfig success!");										
		} else {
			System.err.println("CLIENT_SetThingsConfig fail!:"+ToolKits.getErrorCode());
		}				
	}
	
	/**
	 * 智慧用电Things获取设备能力集接口
	 */
	public void getThingsCaps() {
		//    参数json示例
		//		"GetThingsCaps" : {
		//	      "In" : {
		//			"szDeviceID": "1"
		//	      },
		//	      "Out" : {
		//			"nMaxProperCount" : 10
		//		  }
		//	   	}
		// 入参		
		NET_IN_THINGS_GET_CAPS pIn = new NET_IN_THINGS_GET_CAPS();	
		String szDeviceID ="1";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);			
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_GET_CAPS pOut = new NET_OUT_THINGS_GET_CAPS();	
		int nMaxProperCount =10; 
		pOut.nMaxProperCount = nMaxProperCount; // 用户分配的pstuGetCapsProperName个数				
		NET_PROPERTIES_NAME[] pstuGetCapsProperName = new NET_PROPERTIES_NAME[nMaxProperCount];
		for (int i = 0; i < pstuGetCapsProperName.length; i++) {
			pstuGetCapsProperName[i] = new NET_PROPERTIES_NAME(); // 必须初始化数组
		}
		pOut.pstuGetCapsProperName = new Memory(pstuGetCapsProperName[0].size()*nMaxProperCount); // 内存申请
		pOut.pstuGetCapsProperName.clear(pstuGetCapsProperName[0].size()*nMaxProperCount);
		ToolKits.SetStructArrToPointerData(pstuGetCapsProperName, pOut.pstuGetCapsProperName);	// 将物模型标志信息传给指针									
		pOut.write();		
		boolean flg = netsdk.CLIENT_GetThingsCaps(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			pOut.read();
			int nRetProperCount = pOut.nRetProperCount;// 实际返回的pstuGetCapsProperName个数
			System.out.println("实际返回的pstuGetCapsProperName个数:"+nRetProperCount);
			ToolKits.GetPointerDataToStructArr(pOut.pstuGetCapsProperName,pstuGetCapsProperName); // 将指针转为具体的信息
			for (int i = 0; i < nRetProperCount; i++) {
				System.out.println("szPropertiesName["+i+1+"]:"+new String(pstuGetCapsProperName[i].szPropertiesName));
			}									
		} else {
			System.err.println("CLIENT_GetThingsCaps fail!:"+ToolKits.getErrorCode());
		}		
	}
	
	/**
	 * 智慧用电Things获取设备列表接口
	 */
	public void getThingsDevList() {
		//    参数json示例
		//	    "GetThingsDevList" : {
		//	      "In" : {
		//	      },
		//	      "Out" : {
		//			"nMaxDevListCount" : 10
		//		  }
		//	    }
		// 入参		
		NET_IN_THINGS_GET_DEVLIST pIn = new NET_IN_THINGS_GET_DEVLIST();										
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_GET_DEVLIST pOut = new NET_OUT_THINGS_GET_DEVLIST();	
		int nMaxDevListCount =10; 
		pOut.nMaxDevListCount = nMaxDevListCount; // 用户分配的pstuDevListInfo个数			
		NET_THINGS_DEVLIST_INFO[] pstuDevListInfo = new NET_THINGS_DEVLIST_INFO[nMaxDevListCount];
		for (int i = 0; i < pstuDevListInfo.length; i++) {
			pstuDevListInfo[i] = new NET_THINGS_DEVLIST_INFO(); // 必须初始化数组
		}
		pOut.pstuDevListInfo = new Memory(pstuDevListInfo[0].size()*nMaxDevListCount); // 内存申请
		pOut.pstuDevListInfo.clear(pstuDevListInfo[0].size()*nMaxDevListCount);
		ToolKits.SetStructArrToPointerData(pstuDevListInfo, pOut.pstuDevListInfo);	// 将物模型标志信息传给指针				
		pOut.write();	
		
		boolean flg = netsdk.CLIENT_GetThingsDevList(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			pOut.read();
			int nRetDevListCount = pOut.nRetDevListCount;// 实际返回的pstuDevListInfo个数
			System.out.println("实际返回的pstuDevListInfo个数:"+nRetDevListCount);
			ToolKits.GetPointerDataToStructArr(pOut.pstuDevListInfo,pstuDevListInfo); // 将指针转为设备列表信息	
			for (int i = 0; i < nRetDevListCount; i++) {
				System.out.println("szDevID["+i+1+"]:"+new String(pstuDevListInfo[i].szDevID));
				System.out.println("szDevClass["+i+1+"]:"+new String(pstuDevListInfo[i].szDevClass));
			}						
		} else {
			System.err.println("CLIENT_GetThingsDevList fail!:"+ToolKits.getErrorCode());
		}						
	}
	
	/**
	 * 智慧用电Things获取设备连接状态信息接口
	 */
	public void getThingsNetState() {
		// 入参		
		NET_IN_THINGS_GET_NETSTATE pIn = new NET_IN_THINGS_GET_NETSTATE();										
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_GET_NETSTATE pOut = new NET_OUT_THINGS_GET_NETSTATE();			
		pOut.write();	
		
		boolean flg = netsdk.CLIENT_GetThingsNetState(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			pOut.read();
			System.out.println("设备连接状态:"+EM_THINGS_CONNECT_STATE.getNoteByValue(pOut.emConnectState));
		} else {
			System.err.println("CLIENT_GetThingsNetState fail!:"+ToolKits.getErrorCode());
		}									
	}
	
	/**
	 * 智慧用电Things物模型属性订阅接口
	 */
	public void attachThingsInfo() {
		//	    参数json示例
		//	  "AttachThingsInfo" : {
		//	      "In" : {
		//	        "emTopics" : 0,
		//			"szDeviceID": "2",
		//			"szProductID": "001"
		//	      },
		//	      "Out" : {
		//		  }
		//	   }
		// 入参 CLIENT_AttachThingsInfo
		// 入参		
		NET_IN_THINGS_ATTACH pIn = new NET_IN_THINGS_ATTACH();	
		pIn.emTopics = 2; // 订阅类型 {@link EM_ATTACH_TOPICS}
		String szProductID ="001"; // 产品ID
		System.arraycopy(szProductID.getBytes(), 0, pIn.szProductID, 0, szProductID.getBytes().length);		
		String szDeviceID ="2";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);
		pIn.cbThingsInfo = CBfThingsCallBack.getInstance();// 物模型属性信息回调
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_ATTACH pOut = new NET_OUT_THINGS_ATTACH();			
		pOut.write();	
		AttachHandle = netsdk.CLIENT_AttachThingsInfo(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (AttachHandle.longValue() == 0) {
			System.err.printf("attachThingsInfo fail, ErrCode=%x\n", ToolKits.getErrorCode());
		} else {
			System.out.println("attachThingsInfo success");
		}								
	}
	
	/**
	 * 智慧用电Things取消物模型属性订阅接口
	 */
	public void detachThingsInfo() {
		if (AttachHandle.longValue() != 0) {
			netsdk.CLIENT_DetachThingsInfo(AttachHandle);
		} else {
			System.out.println("订阅句柄为空,请先订阅");
		}
	}
	
	/**
	 * 物模型属性订阅回调函数,lAttachHandle为CLIENT_AttachThingsInfo接口的返回值
	 * 	中pstResult 物模型属性订阅回调信息, 参考{@link NET_CB_THINGS_INFO}
	 */
	private static class CBfThingsCallBack implements NetSDKLib.fThingsCallBack {

		private CBfThingsCallBack() {
		}

		private static class CallBackHolder {
			private static CBfThingsCallBack instance = new CBfThingsCallBack();
		}

		public static CBfThingsCallBack getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public void invoke(LLong lAttachHandle, Pointer pstResult, Pointer dwUser) {
			NET_CB_THINGS_INFO strInfo = new NET_CB_THINGS_INFO();
			ToolKits.GetPointerData(pstResult, strInfo);
			System.out.println("订阅类型:"+strInfo.emTopics);
			System.out.println("sid:"+strInfo.nSID);
			System.out.println("产品ID:"+new String(strInfo.szProductID));
			System.out.println("设备ID:"+new String(strInfo.szDeviceID));
			if(strInfo.emTopics == 2) {
				System.out.println("szEventID:"+new String(strInfo.szEventID));
			}
			System.out.println("实际返回的物模型属性信息数量:"+strInfo.nRetProperInfoNum);
			NET_CB_THINGS_PROPER_INFO[] pstuProperInfo = new NET_CB_THINGS_PROPER_INFO[strInfo.nRetProperInfoNum];
			for (int i = 0; i < pstuProperInfo.length; i++) {
				pstuProperInfo[i] = new NET_CB_THINGS_PROPER_INFO();// 初始化
			}
			ToolKits.GetPointerDataToStructArr(strInfo.pstuProperInfo, pstuProperInfo);
			for (int i = 0; i < pstuProperInfo.length; i++) {
				System.out.println("-----------第"+(i+1)+"个属性信息---------");
				System.out.println("szValue对应的类型:"+EM_PROPERTIES_VALUE_TYPE.getNoteByValue(pstuProperInfo[i].emValueType));
				System.out.println("szKey:"+new String(pstuProperInfo[i].szKey));
				if(pstuProperInfo[i].emValueType == EM_PROPERTIES_VALUE_TYPE.EM_PROPERTIES_VALUE_INTARRAY.getValue()) {
					NET_PROPERTIES_INTARRAY_VALUE[] stuIntArrayValue = pstuProperInfo[i].stuIntArrayValue;
					int nIntArrayValueNum = pstuProperInfo[i].nIntArrayValueNum;// stuIntArrayValue实际个数
					for (int j = 0; j < nIntArrayValueNum; j++) {
						System.out.println("stuIntArrayValue"+j+":"+stuIntArrayValue[j].nValue);												
					}
				}else {
					System.out.println("szValue:"+new String(pstuProperInfo[i].szValue));
				}
			}
			
		}
	}
	
	/**
	 * 智慧用电Things获取设备历史数据功能
	 */
	public void getThingsHistoryData() {
		//     参数json示例
		//	   "StartHistoryData" : {
		//	      "In" : {
		//	        "stuTopics" : [
		//			  {"szTopics":""}
		//			]
		//	      },
		//	      "Out" : {
		//		  }
		//	   }  

		// 开始获取设备历史数据接口		
		NET_IN_THINGS_START_HISTORYDATA pIn = new NET_IN_THINGS_START_HISTORYDATA();	
		pIn.nTopicsCount = 1; // 历史数据主题个数		
		String szTopics1 = "";  // 历史数据具体内容，即历史数据分类由物模型定义决定,可以不填，默认下""		   
		System.arraycopy(szTopics1.getBytes(), 0, pIn.stuTopics[0].szTopics, 0, szTopics1.getBytes().length);													
		pIn.write();				
		NET_OUT_THINGS_START_HISTORYDATA pOut = new NET_OUT_THINGS_START_HISTORYDATA();			
		pOut.write();	
		LLong findHandle = netsdk.CLIENT_StartThingsHistoryData(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		// 查找句柄
		if (findHandle.longValue() == 0) {
			System.err.printf("CLIENT_StartThingsHistoryData fail, ErrCode=%x\n", ToolKits.getErrorCode());
			return;
		}
		pOut.read();
		// 符合查询条件的总数
		int totalCount = pOut.dwCount;		
		if(totalCount < 1) {
			System.out.println("符合查询条件的总数小于1");
			return;
		}else {
			System.out.println("符合查询条件的总数:"+totalCount);
		}
		
		//	   "DoFindHistoryData" : {
		//	      "In" : {
		//	        "uCount" : 10,
		//			"uOffset":0
		//	      },
		//	      "Out" : {
		//		    "nMaxHisDataNum" : 10,
		//			"nMaxProInfoNum": 10
		//		  }
		//	   },
		// 获取设备历史数据结果接口
		NET_IN_THINGS_DOFIND_HISTORYDATA findPIn = new NET_IN_THINGS_DOFIND_HISTORYDATA();	
		findPIn.uOffset = 10; // 偏移量
		findPIn.uCount = 10; // 每次查询数量
		findPIn.write();	
		
		NET_OUT_THINGS_DOFIND_HISTORYDATA findPpOut = new NET_OUT_THINGS_DOFIND_HISTORYDATA();		
		int nMaxProInfoNum = 10; // 用户分配的pstuProInfo个数
		// 初始化查询结果物模型属性信息
		NET_THINGS_HISTORYDATA_PROPER_INFO[] pstuProInfo = new NET_THINGS_HISTORYDATA_PROPER_INFO[nMaxProInfoNum];
		for (int i = 0; i < pstuProInfo.length; i++) {
			pstuProInfo[i] = new NET_THINGS_HISTORYDATA_PROPER_INFO();
		}
		int nMaxHisDataNum = 10; // 用户分配的pstuHistoryData个数
		findPpOut.nMaxHisDataNum = nMaxHisDataNum;					
		NET_THINGS_HISTORYDATA[] pstuHistoryData = new NET_THINGS_HISTORYDATA[nMaxHisDataNum];
		for (int i = 0; i < pstuHistoryData.length; i++) {	
			// 初始化历史数据内容
			NET_THINGS_HISTORYDATA historyData = new NET_THINGS_HISTORYDATA();
			historyData.nMaxProInfoNum = nMaxProInfoNum; // 用户分配的pstuProInfo个数
			historyData.pstuProInfo = new Memory(pstuProInfo[0].size() * nMaxProInfoNum);	
			historyData.pstuProInfo.clear(pstuProInfo[0].size() * nMaxProInfoNum);						
			// 将数组内存拷贝到Pointer
			ToolKits.SetStructArrToPointerData(pstuProInfo, historyData.pstuProInfo);									
			pstuHistoryData[i] = historyData;
		}		
		findPpOut.pstuHistoryData = new Memory(pstuHistoryData[0].size() * nMaxHisDataNum);	
		findPpOut.pstuHistoryData.clear(pstuHistoryData[0].size() * nMaxHisDataNum);	
		ToolKits.SetStructArrToPointerData(pstuHistoryData, findPpOut.pstuHistoryData);			
		findPpOut.write();					
		boolean flg = netsdk.CLIENT_DoFindThingsHistoryData(findHandle, findPIn.getPointer(), findPpOut.getPointer(), 3000);	
		if (!flg) {
			System.err.printf("CLIENT_DoFindThingsHistoryData fail, ErrCode=%x\n", ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_DoFindThingsHistoryData success");
			findPpOut.read();
			ToolKits.GetPointerDataToStructArr(findPpOut.pstuHistoryData, pstuHistoryData);  // 将 Pointer 的内容 输出到   数组
			System.out.println("查询到的数量:"+findPpOut.nCount);
			for (int i = 0; i < findPpOut.nCount; i++) {
				System.out.println("历史数据内容"+(i+1)+"------------");
				System.out.println("标识符:"+new String(pstuHistoryData[i].szEventID));
				ToolKits.GetPointerDataToStructArr(pstuHistoryData[i].pstuProInfo, pstuProInfo);
				System.out.println("实际返回的pstuProInfo个数:"+pstuHistoryData[i].nRetProInfoNum);
				for (int j = 0; j < pstuHistoryData[i].nRetProInfoNum; j++) {
					System.out.println("---第"+(j+1)+"个查询结果物模型属性信息------------");
					System.out.println("szKey:"+new String(pstuProInfo[j].szKey));
					if(pstuProInfo[j].emValueType == EM_PROPERTIES_VALUE_TYPE.EM_PROPERTIES_VALUE_INTARRAY.getValue()) {
						NET_PROPERTIES_INTARRAY_VALUE[] stuIntArrayValue = pstuProInfo[j].stuIntArrayValue;
						int nIntArrayValueNum = pstuProInfo[j].nIntArrayValueNum;// stuIntArrayValue实际个数
						for (int t = 0; t < nIntArrayValueNum; t++) {
							System.out.println("stuIntArrayValue"+t+":"+stuIntArrayValue[t].nValue);												
						}
					}else {
						System.out.println("szValue:"+new String(pstuProInfo[j].szValue));
					}
				}

			}			
	    }		
		// 停止获取设备历史数据,具体调用停止接口需要利用符合查询条件的总数，查询的结果等判断
		netsdk.CLIENT_StopThingsHistoryData(findHandle);
		
		
	}
	
	/**
	 * 智慧用电Things物模型服务调用接口
	 */
	public void thingsServiceOperateAddRule() {
		//		  "addRule" : {
		//        "In" : {
		//		    "szDeviceID": "",
		//		    "szProductID": "002",
		//		    "szClassName": "timer",
		//		    "RuleContent": {
		//			  "nActionNum": 1,
		//			  "Action": 
		//			  [
		//			    {
		//			      "emActionUri":1,
		//				  "SetProperty":{
		//				    "szProductId": "001",
		//				    "szDeviceName": "1",
		//				    "szPropertyName": "CJXX_ZMZT",
		//					"emPropertyValueType":2,
		//				    "szPropertyValue": "false"
		//				  }
		//			    }
		//			  ],
		//			  "nTriggerNum": 1,
		//			  "Trigger": 
		//			  [
		//			    {
		//			      "emUri":1,
		//				  "TriggerTimer":{
		//				    "emCronType":1,
		//				    "szTimezoneID": "Asia/Shanghai",
		//				    "szSeconds": "0",
		//				    "szMinutes": "0",
		//				    "szHours": "0",
		//					"szDayofMonth": "24",
		//					"szMonth": "3",
		//					"szDayofWeek": "?",
		//					"szYear": "2022"
		//				  }
		//			    }
		//			  ]
		//			}
		//        },
		//        "Out" : {
		//	      }
		//    }				
		// 物模型服务类型 ,参考枚举{@link EM_THINGS_SERVICE_TYPE}，接口CLIENT_ThingsServiceOperate入参根据枚举值变化
		int type = EM_THINGS_SERVICE_TYPE.EM_THINGS_SERVICE_TYPE_ADDRULE.getValue(); // 添加联动规则,入参:NET_IN_THINGS_SERVICE_ADDRULE,出参:NET_OUT_THINGS_SERVICE_ADDRULE
		
		// 入参		
		NET_IN_THINGS_SERVICE_ADDRULE pIn = new NET_IN_THINGS_SERVICE_ADDRULE();	
		String szProductID ="002"; // 产品ID
		System.arraycopy(szProductID.getBytes(), 0, pIn.szProductID, 0, szProductID.getBytes().length);		
		String szDeviceID ="";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);
		String szClassName ="timer";// 类型名称“timer”、“alarm”
		System.arraycopy(szClassName.getBytes(), 0, pIn.szClassName, 0, szClassName.getBytes().length);
		NET_THINGS_SERVICE_RULECONTENT stuRuleContent = new NET_THINGS_SERVICE_RULECONTENT(); // 联动规则内容
		stuRuleContent.emRuleContentType = 1; //场景规则类型 {@link com.netsdk.lib.enumeration.EM_THINGS_RULECONTENT_TYPE}		
		
		int nTriggerNum = 1;
		stuRuleContent.nTriggerNum = nTriggerNum;// 场景触发个数
		NET_THINGS_SERVICE_RULECONTENT_TRIGGER[] pstuTrigger = new NET_THINGS_SERVICE_RULECONTENT_TRIGGER[nTriggerNum];// 场景的触发器
		for (int i = 0; i < pstuTrigger.length; i++) {
			NET_THINGS_SERVICE_RULECONTENT_TRIGGER obj = new NET_THINGS_SERVICE_RULECONTENT_TRIGGER(); //初始化
			obj.emUri = 1; //场景的触发类型,见EM_THINGS_TRIGGER_TYPE {@link com.netsdk.lib.enumeration.EM_THINGS_TRIGGER_TYPE}
			if(obj.emUri == EM_THINGS_TRIGGER_TYPE.EM_THINGS_TRIGGER_TYPE_TRIGGER_TIMER.getValue()) {
				NET_THINGS_RULECONTENT_TRIGGER_TIMER stuTriggerTimer = new NET_THINGS_RULECONTENT_TRIGGER_TIMER();
				String szTimezoneID = "Asia/Shanghai";
				System.arraycopy(szTimezoneID.getBytes(), 0, stuTriggerTimer.szTimezoneID, 0, szTimezoneID.getBytes().length);
				stuTriggerTimer.emCronType = 1;// Corn表达式类 {@link com.netsdk.lib.enumeration.EM_THINGS_TRIGGER_TIMER_CORN_TYPE}
				System.arraycopy("2022".getBytes(), 0, stuTriggerTimer.stuCron.szYear, 0, "2022".getBytes().length);
				System.arraycopy("?".getBytes(), 0, stuTriggerTimer.stuCron.szDayofWeek, 0, "?".getBytes().length);
				System.arraycopy("3".getBytes(), 0, stuTriggerTimer.stuCron.szMonth, 0, "3".getBytes().length);
				System.arraycopy("24".getBytes(), 0, stuTriggerTimer.stuCron.szDayofMonth, 0, "24".getBytes().length);
				System.arraycopy("0".getBytes(), 0, stuTriggerTimer.stuCron.szHours, 0, "0".getBytes().length);
				System.arraycopy("0".getBytes(), 0, stuTriggerTimer.stuCron.szMinutes, 0, "0".getBytes().length);
				System.arraycopy("0".getBytes(), 0, stuTriggerTimer.stuCron.szSeconds, 0, "0".getBytes().length);
				obj.stuTriggerTimer = stuTriggerTimer;
			}else if(obj.emUri == EM_THINGS_TRIGGER_TYPE.EM_THINGS_TRIGGER_TYPE_TRIGGER_PROPERTY.getValue()) {
				NET_THINGS_RULECONTENT_TRIGGER_PROPERTY stuTriggerProperty = new NET_THINGS_RULECONTENT_TRIGGER_PROPERTY();	
				System.arraycopy("001".getBytes(), 0, stuTriggerProperty.szProductId, 0, "001".getBytes().length);
				System.arraycopy("1".getBytes(), 0, stuTriggerProperty.szDeviceName, 0, "1".getBytes().length);
				System.arraycopy("CJXX_ZMZT".getBytes(), 0, stuTriggerProperty.szPropertyName, 0, "CJXX_ZMZT".getBytes().length);
				stuTriggerProperty.emCompareValueType = 2;// 对比值类型 {@link com.netsdk.lib.enumeration.EM_PROPERTIES_VALUE_TYPE}
				System.arraycopy("==".getBytes(), 0, stuTriggerProperty.szCompareType, 0, "==".getBytes().length);
				System.arraycopy("false".getBytes(), 0, stuTriggerProperty.szCompareValue, 0, "false".getBytes().length);
				obj.stuTriggerProperty = stuTriggerProperty;
			}
			pstuTrigger[i] = obj;			
		}
		stuRuleContent.pstuTrigger = new Memory(pstuTrigger[0].size()*nTriggerNum);
		stuRuleContent.pstuTrigger.clear(pstuTrigger[0].size()*nTriggerNum);
		ToolKits.SetStructArrToPointerData(pstuTrigger, stuRuleContent.pstuTrigger);
		
		int nActionNum = 1; 
		stuRuleContent.nActionNum = 1;// 场景触发执行动作个数
		NET_THINGS_SERVICE_RULECONTENT_ACTION[] pstuAction = new NET_THINGS_SERVICE_RULECONTENT_ACTION[nActionNum];// 场景的触发器
		for (int i = 0; i < pstuAction.length; i++) {
			NET_THINGS_SERVICE_RULECONTENT_ACTION obj = new NET_THINGS_SERVICE_RULECONTENT_ACTION();
			obj.emActionUri = 1; // 场景触发执行动作类型,见EM_THINGS_ACTION_TYPE {@link com.netsdk.lib.enumeration.EM_THINGS_ACTION_TYPE}			
			System.arraycopy("001".getBytes(), 0, obj.stuActionSetProperty.szProductId, 0, "001".getBytes().length);
			System.arraycopy("1".getBytes(), 0, obj.stuActionSetProperty.szDeviceName, 0, "1".getBytes().length);
			System.arraycopy("CJXX_ZMZT".getBytes(), 0, obj.stuActionSetProperty.szPropertyName, 0, "CJXX_ZMZT".getBytes().length);
			obj.stuActionSetProperty.emPropertyValueType = 2;// 属性值类型 {@link com.netsdk.lib.enumeration.EM_PROPERTIES_VALUE_TYPE}			
			System.arraycopy("false".getBytes(), 0,  obj.stuActionSetProperty.szPropertyValue, 0, "false".getBytes().length);			
			pstuAction[i] = obj;
		}
		stuRuleContent.pstuAction = new Memory(pstuAction[0].size()*nActionNum);
		stuRuleContent.pstuAction.clear(pstuAction[0].size()*nActionNum);
		ToolKits.SetStructArrToPointerData(pstuAction, stuRuleContent.pstuAction);		
		
		pIn.stuRuleContent = stuRuleContent;		
		pIn.write();
				
		// 出参
		NET_OUT_THINGS_SERVICE_ADDRULE pOut = new NET_OUT_THINGS_SERVICE_ADDRULE();			
		pOut.write();
		
		boolean flg = netsdk.CLIENT_ThingsServiceOperate(m_hLoginHandle, type, pIn.getPointer(), pOut.getPointer(), 3000);
		if (!flg) {
			System.err.printf("CLIENT_ThingsServiceOperate fail, ErrCode=%x\n", ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_ThingsServiceOperate success");
			pOut.read();
			System.out.println("规则ID:"+ new String(pOut.szRuleID));
			System.out.println("物模型协议错误码:"+ pOut.nErrcode);
		}	
	}
	
	/**
	 * 智慧用电Things阀门控制
	 */
	public void thingsServiceOperateValveControl() {
		int type = EM_THINGS_SERVICE_TYPE.EM_THINGS_SERVICE_TYPE_VALVECONTROL.getValue();		
		// 入参		
		NET_IN_THINGS_SERVICE_VALVECONTROL pIn = new NET_IN_THINGS_SERVICE_VALVECONTROL();	
		String szProductID ="001"; // 产品ID
		System.arraycopy(szProductID.getBytes(), 0, pIn.szProductID, 0, szProductID.getBytes().length);		
		String szDeviceID ="2";// 设备ID
		System.arraycopy(szDeviceID.getBytes(), 0, pIn.szDeviceID, 0, szDeviceID.getBytes().length);
		pIn.bSwitch = 0;		
		pIn.write();
		// 出参
		NET_OUT_THINGS_SERVICE_VALVECONTROL pOut = new NET_OUT_THINGS_SERVICE_VALVECONTROL();			
		pOut.write();
		
		boolean flg = netsdk.CLIENT_ThingsServiceOperate(m_hLoginHandle, type, pIn.getPointer(), pOut.getPointer(), 3000);
		if (!flg) {
			System.err.printf("CLIENT_ThingsServiceOperate fail, ErrCode=%x\n", ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_ThingsServiceOperate success");
			pOut.read();
			System.out.println("设置结果，实际闸门控制设置成功/失败已该值为准:"+pOut.bResult);
		}	
	}
	
	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.13.3.137";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		ThingsDemo demo = new ThingsDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		ThingsDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things配置获取接口", "getThingsConfig"));
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things配置设置接口", "setThingsConfig"));
		
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things获取设备能力集接口", "getThingsCaps"));
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things获取设备列表接口", "getThingsDevList")); 
		//menu.addItem(new CaseMenu.Item(this, "智慧用电Things获取设备连接状态信息接口", "getThingsNetState"));
		
		// 第一步：执行thingsAttach接口,订阅属性和事件上报，订阅成功返回订阅句柄attachHandle
		// 第二步：查看回调函数数据，为实时上报或者定时上报属性和事件数据，并解析数据（请不要再回调进行大量业务操作，导致卡回调）、
		// 第三步：执行thingsDetach接口，取消订阅，并销毁订阅句柄
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things物模型属性订阅接口", "attachThingsInfo"));
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things取消物模型属性订阅接口", "detachThingsInfo"));
				
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things获取设备历史数据功能", "getThingsHistoryData"));
		
		// 物模型服务类型:阀门控制 /电量清零 /漏电自检/漏电自检异步/空开红蓝灯闪烁(寻找空开)/恢复出厂设置/联动规则相关功能/添加设备相关功能/离线日志清除/获取所有空开当前报警状态  -参考该方法
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things物模型服务调用接口", "thingsServiceOperateAddRule"));
		menu.addItem(new CaseMenu.Item(this, "智慧用电Things阀门控制", "thingsServiceOperateValveControl"));
		
		
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		ThingsDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
