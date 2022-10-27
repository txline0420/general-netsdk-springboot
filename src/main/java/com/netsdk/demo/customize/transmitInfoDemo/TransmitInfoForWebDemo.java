package com.netsdk.demo.customize.transmitInfoDemo;


import com.alibaba.fastjson.JSONObject;
import com.netsdk.demo.customize.transmitInfoDemo.entity.DeviceInfo;
import com.netsdk.demo.customize.transmitInfoDemo.module.LoginModule;
import com.netsdk.demo.customize.transmitInfoDemo.module.SdkUtilModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_IN_TRANSMIT_INFO;
import com.netsdk.lib.NetSDKLib.NET_OUT_TRANSMIT_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;


/**
 * @author 251823
 * @description 透传接口演示样例
 * @date 2022/3/23
 */
public class TransmitInfoForWebDemo {

    static NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
    // 登录句柄
    private final LLong m_hLoginHandle;
    // 设备信息
    private final NetSDKLib.NET_DEVICEINFO_Ex m_hDeviceInfo;     
	// 订阅句柄
	private LLong attachHandle = new LLong(0);
	// SID
	private String sid = "";
	// 跨平台编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode()); 

    public TransmitInfoForWebDemo(DeviceInfo deviceInfo) {
        this.m_hLoginHandle = deviceInfo.m_hLoginHandle;
        this.m_hDeviceInfo = deviceInfo.m_stDeviceInfo;
    }

    public TransmitInfoForWebDemo(LLong loginHandle, NetSDKLib.NET_DEVICEINFO_Ex deviceinfo) {
        this.m_hLoginHandle = loginHandle;
        this.m_hDeviceInfo = deviceinfo;
    }
    
    /////////////////////////////////////// 业务接口 ///////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////// 
    
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
	


   

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化测试
     */
    public void InitTest() {
        SdkUtilModule.Init();  // 初始化SDK库
        LLong loginHandle = LoginModule.TcpLoginWithHighSecurity(m_ipAddr, m_nPort, m_username, m_password, m_hDeviceInfo);  // 高安全登录
        if (loginHandle.intValue() == 0) {
            SdkUtilModule.cleanup();
        }
        m_hLoginHandle.setValue(loginHandle.longValue());
    }

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
        System.out.println("子测试已退出");
    }

    /**
     * 结束测试
     */
    public void EndTest() {
        System.out.println("End Test");
        LoginModule.logout(m_hLoginHandle);  // 登出
        System.out.println("See You...");
        SdkUtilModule.cleanup();             // 清理资源
        System.exit(0);
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_ipAddr = "172.13.3.206"; 
    private int m_nPort = 37777;
    private String m_username = "admin";
    private String m_password = "admin123";   
    //////////////////////////////////////////////////////////////////////3
    

    public static void main(String[] args) {

        NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
        LLong loginHandle = new LLong(0);
        TransmitInfoForWebDemo demo = new TransmitInfoForWebDemo(loginHandle, deviceInfo);
        if (args.length == 4) {
            demo.m_ipAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_username = args[2];
            demo.m_password = args[3];
        }
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
