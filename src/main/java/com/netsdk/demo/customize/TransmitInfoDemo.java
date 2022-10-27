package com.netsdk.demo.customize;

import com.alibaba.fastjson.JSONObject;
import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_IN_TRANSMIT_INFO;
import com.netsdk.lib.NetSDKLib.NET_OUT_TRANSMIT_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @version 1.0
 * @description 透传接口使用样例
 * @date 2022/2/16
 */
public class TransmitInfoDemo {
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
	private LLong m_hLoginHandle = new LLong(0);
	
	// 订阅句柄
	private LLong attachHandle = new LLong(0);

	// 回调函数需要是静态的，防止被系统回收
	// 断线回调
	private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();
	// 重连回调
	private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();

	// 编码格式
	public static String encode;
	
	// SID
	private String sid = "";

	static {
		String osPrefix = getOsPrefix();
		if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
			encode = "GBK";
		} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
			encode = "UTF-8";
		}
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
		TransmitInfoDemo.enableLog();

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
		String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate()
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
			deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
			System.out.println("Login Success");
			System.out.println("Device Address：" + m_strIpAddr);
			System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
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
	 * 配置透传接口
	 */
	public String transmitInfoForWeb(String jsonParams) {
		String request = jsonParams;
		int dwInBufferSize = request.length();
		Pointer szInBuffer = new Memory(dwInBufferSize);
		szInBuffer.write(0, request.getBytes(), 0, request.getBytes().length);
		int dwOutBufferSize = 10 * 1024;
		Pointer szOutBuffer = new Memory(dwOutBufferSize);
		boolean ret = netsdk.CLIENT_TransmitInfoForWeb(m_hLoginHandle, szInBuffer, dwInBufferSize, szOutBuffer,
				dwOutBufferSize, null, 3000);
		if (ret) {
			System.out.println("TransmitInfoForWeb success");
			byte[] str = szOutBuffer.getByteArray(0, dwOutBufferSize);
			String strJson = new String(str);
			System.out.println("配置透传接口返回数据:" + strJson);
			return strJson;
		} else {
			//  public static final int NET_UNSUPPORTED = (0x80000000 | 79);  // 设备不支持该操作
			System.err.printf("TransmitInfoForWeb false Last Error[0x%x]\n", netsdk.CLIENT_GetLastError());
			return "";
		}
	}

	/**
	 * 配置透传接口扩展实现
	 */
	public void transmitInfoForWebEx() {
		JSONObject params = new JSONObject();
		params.put("DeviceID", "1");

		JSONObject JSONObject = new JSONObject();
		JSONObject.put("method", "Things.getDevCaps");
		JSONObject.put("params", params);
		String request = JSONObject.toString();
		System.out.println("request:" + request);
		NET_IN_TRANSMIT_INFO pIn = new NET_IN_TRANSMIT_INFO();
		pIn.emType = 0;
		pIn.emEncryptType = 0;
		String json = request;

		pIn.dwInJsonBufferSize = json.getBytes().length;
		pIn.szInJsonBuffer = json;

		Pointer pInParam = new Memory(pIn.size());
		ToolKits.SetStructDataToPointer(pIn, pInParam, 0);

		NET_OUT_TRANSMIT_INFO pOut = new NET_OUT_TRANSMIT_INFO();
		pOut.szOutBuffer = new Memory(1024 * 10);
		pOut.dwOutBufferSize = 1024 * 10;
		Pointer poutParam = new Memory(pOut.size());
		ToolKits.SetStructDataToPointer(pOut, poutParam, 0);

		boolean ret = netsdk.CLIENT_TransmitInfoForWebEx(m_hLoginHandle, pInParam, poutParam, 3000);

		if (!ret) {
			System.err.printf("transmitInfoForWebEX Failed!Last Error[0x%x]\n", ToolKits.getErrorCode());
		} else {
			System.out.println("transmitInfoForWebEX Succeed!");

			ToolKits.GetPointerDataToStruct(poutParam, 0, pOut);

			System.out.println("dwOutBinLen:" + pOut.dwOutBinLen);
			System.out.println("dwOutJsonLen:" + pOut.dwOutJsonLen);
			System.out.println("dwOutBufferSize:" + pOut.dwOutBufferSize);

			byte[] str = pOut.szOutBuffer.getByteArray(0, pOut.dwOutJsonLen);// 解析字节长度根据实际返回字符串结果定义
			System.out.println("配置透传扩展接口返回数据:" + new String(str));
		}
	}
	
	
	/**
	 * 订阅设备上报数据 
	 */
	public void thingsAttach() {
		// 协议接口入参json字符串
		String paramsJson = "{\"method\": \"Things.attach\",\"Params\": {\"DeviceID\": \"2\",\"ProductID\": \"001\",\"Topics\": [\"*\"]}}";
		
		// 入参
		NET_IN_ATTACH_TRANSMIT_INFO pIn = new NET_IN_ATTACH_TRANSMIT_INFO();
		pIn.cbTransmitInfo = AsyncTransmitInfoDataCB.getInstance();//回调函数
		pIn.dwUser = null;//用户数据		
		int dwInJsonBufferSize = paramsJson.length();
		Pointer szInJsonBuffer = new Memory(dwInJsonBufferSize);
		szInJsonBuffer.write(0, paramsJson.getBytes(), 0, paramsJson.getBytes().length);		
		pIn.dwInJsonBufferSize = dwInJsonBufferSize;//Json请求数据长度 
		pIn.szInJsonBuffer = szInJsonBuffer; //Json请求数据,用户申请空间
		pIn.bSubConnFirst = true;	//TRUE-当设备支持时，使用子连接方式接收订阅数据 FALSE-只在主连接接收订阅数据	
		Pointer pInParam = new Memory(pIn.size());
		ToolKits.SetStructDataToPointer(pIn, pInParam, 0);
		
		// 出参
		NET_OUT_ATTACH_TRANSMIT_INFO pOut = new NET_OUT_ATTACH_TRANSMIT_INFO();
		int initSize = 1024 * 10;
		pOut.dwOutBufferSize = initSize;	
		pOut.szOutBuffer= new Memory(initSize);;		
		Pointer pOutParam = new Memory(pOut.size());
		ToolKits.SetStructDataToPointer(pOut, pOutParam, 0);
		
		attachHandle = netsdk.CLIENT_AttachTransmitInfo(m_hLoginHandle, pInParam, pOutParam, 3000);
	    if (attachHandle.longValue() == 0){   
	    	System.err.println("CLIENT_AttachTransmitInfo failed\n");
	    }else {
	    	ToolKits.GetPointerDataToStruct(pOutParam, 0, pOut);
			System.out.println("订阅句柄:" + attachHandle);
			String str = new String(pOut.szOutBuffer.getByteArray(0, pOut.dwOutJsonLen));
			System.out.println("事件订阅成功返回数据:" + str);
			// 当请阅某一主题时会返回一个SID，这个ID用于用户取消订阅或者管理上报的数据。
			// 返回示例 {session': '36jk8pWoa07dea792692606585', 'params': {'SID': 11}, 'result': True, 'id': 13}
			JSONObject json = JSONObject.parseObject(str);
			JSONObject params = json.getJSONObject("params");
			sid = params.getString("SID");
			System.out.println("SID:"+sid);
	    }	    	    
	    
	}		
	
	/**
	 * 订阅回调
	 */
	private static class AsyncTransmitInfoDataCB implements NetSDKLib.AsyncTransmitInfoCallBack {		
	
		private static AsyncTransmitInfoDataCB instance;
		//private AsyncTransmitInfoDataCB() {}	
		public static AsyncTransmitInfoDataCB getInstance() {
			if (instance == null) {
				synchronized (AsyncTransmitInfoDataCB.class) {
					if (instance == null) {
						instance = new AsyncTransmitInfoDataCB();
					}
				}
			}
			return instance;
		}

		@Override
		public void invoke(LLong lAttachHandle, NET_CB_TRANSMIT_INFO pTransmitInfo, Pointer dwUser) {
			System.out.println("lAttachHandle:"+lAttachHandle);
			// 订阅设备上报回来数据,解析参考具体返回Json字符串结构
			byte[] str = pTransmitInfo.pBuffer.getByteArray(0, pTransmitInfo.dwJsonLen);			
			System.out.println("订阅回调数据:"+new String(str));
			
			JSONObject json = JSONObject.parseObject(new String(str));
			JSONObject params = json.getJSONObject("params");
			String topics = params.getString("Topics");
			System.out.println("Topics:"+topics);// “Props”: 表示订阅设备属性上报    “Events”: 标识订阅设备事件上报
		}
	}
	
	/**
	 * 取消订阅
	 */
	public void thingsDetach() {
		// 协议接口入参json字符串,SID的值取订阅成功的返回结果
		String paramsJson = "{\"params\": {\"SID\": "+ sid +"}, \"method\": \"Things.detach\"}";
		
		// 入参
		NET_IN_DETACH_TRANSMIT_INFO pIn = new NET_IN_DETACH_TRANSMIT_INFO();	
		int dwInJsonBufferSize = paramsJson.length();
		Pointer szInJsonBuffer = new Memory(dwInJsonBufferSize);
		szInJsonBuffer.write(0, paramsJson.getBytes(), 0, paramsJson.getBytes().length);		
		pIn.dwInJsonBufferSize = dwInJsonBufferSize;//Json请求数据长度 
		pIn.szInJsonBuffer = szInJsonBuffer; //Json请求数据,用户申请空间
		Pointer pInParam = new Memory(pIn.size());
		ToolKits.SetStructDataToPointer(pIn, pInParam, 0);
		
		// 出参
		NET_OUT_DETACH_TRANSMIT_INFO pOut = new NET_OUT_DETACH_TRANSMIT_INFO();
		int initSize = 1024 * 10;
		pOut.dwOutBufferSize = initSize; //应答数据缓冲空间长度
		pOut.szOutBuffer= new Memory(initSize);//应答数据缓冲空间, 用户申请空间		
		Pointer pOutParam = new Memory(pOut.size());
		ToolKits.SetStructDataToPointer(pOut, pOutParam, 0);		
		boolean flg = netsdk.CLIENT_DetachTransmitInfo(attachHandle, pInParam, pOutParam, 3000);
		if(flg) {
			System.out.println("CLIENT_DetachTransmitInfo success\n");
			attachHandle = new LLong(0);//订阅句柄清零
	    	ToolKits.GetPointerDataToStruct(pOutParam, 0, pOut);
			String str = new String(pOut.szOutBuffer.getByteArray(0, pOut.dwOutJsonLen));
			System.out.println("取消事件订阅接口返回数据:" + str);
		}
		
	}
	
	/**
	 * Things.getDevlist接口样例
	 */
	public void getDevlist() {
		String jsonParams = "{\"params\": {}, \"method\": \"Things.getDevlist\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.getDevCaps接口样例
	 */
	public void getDevCaps() {
		String jsonParams = "{\"params\": {\"DeviceID\": \"1\"}, \"method\": \"Things.getDevCaps\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.get接口样例
	 */
	public void get() {
		String jsonParams = "{\"params\": {\"Properties\": [\"CJXX_CJWDZ_LX\", \"SBJCXX_SBBM\", \"SBJCXX_SBXH\", \"SBJCXX_XLH\", \"SBJCXX_RJBBH\", \"SBJCXX_CPLX\", \"SBJCXX_SBMS\", \"SBJCXX_EDDL\", \"SBJCXX_SBLX\", \"SBJCXX_KKPS\", \"SBJCXX_LJLBNL\", \"CJXX_CJDLBWD\", \"BJPZ_LDBJSN\", \"BJPZ_DHBJSN\"], \"DeviceID\": \"2\", \"ProductID\": \"001\"}, \"method\": \"Things.get\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.set接口样例
	 */
	public void set() {
		String jsonParams = "{\"params\": {\"Properties\": [{\"BJPZ_DHBJLDMS\": 1}, {\"BJPZ_LDBJLDMS\": 0}], \"DeviceID\": \"2\", \"ProductID\": \"001\"}, \"method\": \"Things.set\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.getNetState接口样例
	 */
	public void getNetState() {
		String jsonParams = "{\"params\": {}, \"method\": \"Things.getNetState\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.service接口样例
	 */
	public void service() {
		String jsonParams = "{\"params\": {\"ParamIn\": [], \"ServiceID\": \"leakCurtPost\", \"DeviceID\": \"2\", \"ProductID\": \"001\"}, \"method\": \"Things.service\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	// 分页查询用的token和总数
	private String count = "";
	private String token = "";
	
	/**
	 * Things.startHistoryData接口样例
	 */
	public void startHistoryData() {
		String jsonParams = "{\"params\": {\"Topics\": [\"\"]}, \"method\": \"Things.startHistoryData\"}";
		String strJson = this.transmitInfoForWeb(jsonParams);
		if(strJson.equals("")) {
			return;
		}
		// strJson的字符串样例{"session": "awQMtQSb91fe54f62449364214", "params": {"Count": 0, "Token": 4}, "result": true, "id": 13}
		JSONObject json = JSONObject.parseObject(strJson);
		JSONObject params = json.getJSONObject("params");
		count = params.getString("Count");
		token = params.getString("Token");
	}
	
	/**
	 * Things.doHistoryData接口样例
	 */
	public void doHistoryData() {
		String jsonParams = "{\"params\": {\"Count\": "+count+", \"Token\": "+token+", \"Offset\": 0}, \"method\": \"Things.doHistoryData\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	/**
	 * Things.stopHistoryData接口样例
	 */
	public void stopHistoryData() {
		String jsonParams = "{\"params\": {\"Token\": "+ token +"}, \"method\": \"Things.stopHistoryData\"}";
		this.transmitInfoForWeb(jsonParams);
	}
	
	

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.13.3.46";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		TransmitInfoDemo demo = new TransmitInfoDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		TransmitInfoDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		// 事件订阅功能
		// 第一步：执行thingsAttach接口,订阅属性和事件上报，订阅成功返回订阅句柄attachHandle和SID
		// 第二步：查看回调函数数据，为实时上报或者定时上报属性和事件数据，并解析数据（请不要再回调进行大量业务操作，导致卡回调）、
		// 第三步：执行thingsDetach接口，取消订阅，并销毁订阅句柄		
		menu.addItem(new CaseMenu.Item(this, "Things.attach", "thingsAttach"));			// 订阅设备上报数据 
		menu.addItem(new CaseMenu.Item(this, "Things.detach", "thingsDetach"));			// 取消订阅
		
		// 配置协议
		// 配置协议接口入参json字符串，调用CLIENT_TransmitInfoForWeb透传接口，解析出参szInBuffer的返回数据
		menu.addItem(new CaseMenu.Item(this, "Things.getDevlist", "getDevlist"));	
		menu.addItem(new CaseMenu.Item(this, "Things.getDevCaps", "getDevCaps"));	
		menu.addItem(new CaseMenu.Item(this, "Things.get", "get"));
		menu.addItem(new CaseMenu.Item(this, "Things.set", "set"));
		menu.addItem(new CaseMenu.Item(this, "Things.getNetState", "getNetState"));
		menu.addItem(new CaseMenu.Item(this, "Things.service", "service"));
		
		menu.addItem(new CaseMenu.Item(this, "Things.startHistoryData", "startHistoryData"));//开始查询历史数据，获取分页查询用的token和总数。
		menu.addItem(new CaseMenu.Item(this, "Things.doHistoryData", "doHistoryData"));//分页查询
		menu.addItem(new CaseMenu.Item(this, "Things.stopHistoryData", "stopHistoryData"));//停止查询历史数据
																	
		//menu.addItem(new CaseMenu.Item(this, "transmitInfoForWebEx", "transmitInfoForWebEx"));								
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		TransmitInfoDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
