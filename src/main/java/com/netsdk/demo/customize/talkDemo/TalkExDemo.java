package com.netsdk.demo.customize.talkDemo;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_TALK_CODING_TYPE;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_AUDIO_SOURCE_FLAG;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.*;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 
 * 1、对单兵设备传送过来的带大华码流的进行剥头，并把音频相关信息(编码格式、采样频率、采样位数等)解析给客户。
 * 2、对客户的音频数据进行加头，同时需要客户传送音频相关信息（编码格式、采样频率、采样位数等）； 3、按照固定频率进行音频发送。
 * 
 * @author 251823
 */
public class TalkExDemo {

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
		TalkExDemo.enableLog();

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
	 *  功能：开始对讲
	 *  描述：开启对讲模式,调用CLIENT_StartTalkByDataType接口，会执行数据回调函数AudioDataCallBackEx
	 *  场景：1.直接登录设备相机,进行音频播放,不需要设置转发模式
	 *      2.经过了NVR中转，登陆NVR后，需要设置 int isTransfer  = 1; //1为转发  0为不转发     int Channel = 11; //设置转发通道号（具体使用场景设置值）
	 */
	public  boolean startTalk() {
		// 设置语音对讲编码格式(此设置根据设备的具体能力) 只对客户端起作用，服务器方式无意义
		NetSDKLib.NETDEV_TALKDECODE_INFO talkEncode = new NetSDKLib.NETDEV_TALKDECODE_INFO();
		talkEncode.encodeType = NET_TALK_CODING_TYPE.NET_TALK_PCM;// 语音编码类型      为带头PCM
		talkEncode.dwSampleRate = 8000;//采样率
		talkEncode.nAudioBit = 16;//位数
		talkEncode.nPacketPeriod = 25; //打包周期
		talkEncode.write();
		if (netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_ENCODE_TYPE,
				talkEncode.getPointer())) {
			System.out.println("Set Talk Encode Type Succeed!");
		} else {
			System.err.println("Set Talk Encode Type Failed!" +  ToolKits.getErrorCode());
			return false;
		}

		// 设置语音对讲喊话参数 (NET_TALK_SERVER_MODE  设置服务器方式进行语音对讲)
		if (netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_SERVER_MODE, null)) { 
			System.out.println("Set Talk Speak Mode Succeed!");
		} else {
			System.err.println("Set Talk Speak Mode Failed!" + ToolKits.getErrorCode());
			return false;
		}	
				
		// 设置语音对讲是否为转发模式(场景：1.直接登录设备相机,不需要设置转发模式;2.经过了NVR中转,需要设置 )
		int isTransfer  = 0; //1为转发  0为不转发
		int Channel = 2; //设置转发通道号
		NetSDKLib.NET_TALK_TRANSFER_PARAM talkTransfer = new NetSDKLib.NET_TALK_TRANSFER_PARAM();
		talkTransfer.bTransfer = isTransfer;//1为转发
		talkTransfer.write();
		if(netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_TRANSFER_MODE, talkTransfer.getPointer())) {
			System.out.println("Set Talk Transfer Mode Succeed!");
		} else {
			System.err.println("Set Talk Transfer Mode Failed!" + ToolKits.getErrorCode());
			return false;
		}
		
		if (talkTransfer.bTransfer == 1) { 
			// 转发模式设置转发通道	
			IntByReference nChn = new IntByReference(Channel);
			if(netsdk.CLIENT_SetDeviceMode(m_hLoginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_TALK_CHANNEL, nChn.getPointer())) {
				System.out.println("Set Talk Channel Succeed!");
			} else {
				System.err.println("Set Talk Channel Failed!" + ToolKits.getErrorCode());
				return false;
			}
		}
		
		// 开启对讲(会执行数据回调AudioDataCallBackEx)
		NET_IN_START_TALK_INFO pInParam = new NET_IN_START_TALK_INFO();
		pInParam.pfAudioDataCallBackEx = AudioDataCallBackEx.getInstance();
		pInParam.write();		
		NET_OUT_START_TALK_INFO pOutParam = new NET_OUT_START_TALK_INFO();
		pOutParam.write();
		
		m_hTalkHandle = netsdk.CLIENT_StartTalkByDataType(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(), 5000);
		if (m_hTalkHandle.longValue() == 0) {
			System.err.println("Start Talk Failed!" + ToolKits.getErrorCode());
			return false;
		} else {
			System.out.println("Start Talk Success");		
			return true;
		}
	}
	
	
	
	/**
	 * 功能：语音对讲的数据回调
	 * 描述：1.参数emAudioFlag = 1时，设备返回的带大华头的音频数据;
	 * 	   2.在demo中可以将记录开关参数recordFlag = true,调用解析库接口，获取裸音频数据写入文件
	 */
	private static class AudioDataCallBackEx implements NetSDKLib.fAudioDataCallBackEx {

		private AudioDataCallBackEx() {
		}

		private static AudioDataCallBackEx audioCallBack = new AudioDataCallBackEx();

		public static AudioDataCallBackEx getInstance() {
			return audioCallBack;
		}		

		@Override
		public void invoke(LLong lTalkHandle, NET_AUDIO_DATA_CB_INFO stAudioInfo, int emAudioFlag, Pointer dwUser) {			
			if(lTalkHandle.longValue() != m_hTalkHandle.longValue()) {
				return;
			}
			
			// emAudioFlag: 音频数据来源，参考枚举 EM_AUDIO_SOURCE_FLAG
			if(emAudioFlag == EM_AUDIO_SOURCE_FLAG.EM_AUDIO_SOURCE_FLAG_LOCAL.getValue()) {
				// 本地录音数据，可以发送给设备
				//LLong lSendSize = netsdk.CLIENT_TalkSendData(m_hTalkHandle, stAudioInfo.pBuf, stAudioInfo.dwBufSize);
				//if(lSendSize.longValue() != (long)stAudioInfo.dwBufSize) {
					//System.err.println("send incomplete" + lSendSize.longValue() + ":" + stAudioInfo.dwBufSize);
				//} else {
				   // System.out.println("本地音频发送给设备");
				//}																
			}else if(emAudioFlag == EM_AUDIO_SOURCE_FLAG.EM_AUDIO_SOURCE_FLAG_REMOTE.getValue() && recordFlag == true){
				System.out.println("位数:"+stAudioInfo.nAudioBit);
				System.out.println("采样率:"+stAudioInfo.dwSampleRate);
				try {					
					File file = new File("pbuf.dav");
					if(!file.exists()){//文件不存在新建文件
						file.createNewFile();
					}
					OutputStream out = new FileOutputStream(file,true);
					out.write(stAudioInfo.pBuf.getByteArray(0, stAudioInfo.dwBufSize));
					out.flush();
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}		
				
				try {					
					File file1 = new File("pRawBuf.dav");
					if(!file1.exists()){//文件不存在新建文件
						file1.createNewFile();
					}
					OutputStream out = new FileOutputStream(file1,true);
					out.write(stAudioInfo.pRawBuf.getByteArray(0, stAudioInfo.dwRawBufSize));
					out.flush();
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// 远程音频数据，即从设备接收到音频数据								
				//是否需要加音频头。 TRUE，表示需要SDK根据下面的音频信息加音频头；FALSE，表示不需要SDK根据音频信息加音频头，直接发送pBuf指向的数据给设备。
				boolean bNeedHead = true;
				if(bNeedHead) {
					// 不带音频头的音频裸数据
					//sendDataByStream(lTalkHandle,stAudioInfo.pRawBuf,stAudioInfo.dwRawBufSize,bNeedHead);
				}else {
					// 带音频头的音频数据
					//sendDataByStream(lTalkHandle,stAudioInfo.pBuf,stAudioInfo.dwBufSize,bNeedHead);
				}
														
			}									
		}
	}
	
	
	
	/**
	 * 功能：发送语音文件中的音频数据到设备
	 * 描述：1.必须开始对讲，在调用播放功能
	 * 	   2.该接口只支持不带音频头的音频裸数据。
	 *     3.emEncodeType参数只支持PCM G711a G711u
	 *     4.注意封装参数参数设定，demo中以以16位8K的PCM数据为例;
	 */
	public void talkSendDataByFile() {
		NET_IN_TALK_SEND_DATA_FILE stIn = new NET_IN_TALK_SEND_DATA_FILE();
	    /**
	     *  是否需要加音频头。
	     *  TRUE，表示需要SDK根据下面的音频信息加音频头；
	     *  FALSE，表示不需要SDK根据音频信息加音频头，直接发送pFilePath路径指向的数据给设备。
	     */
        stIn.bNeedHead = true;
    	stIn.cbSendPos = null;
        stIn.dwSampleRate = 8000;
        stIn.nAudioBit = 16;
        stIn.emEncodeType = NET_TALK_CODING_TYPE.NET_TALK_DEFAULT;
        stIn.dwSendInterval = 100;
        byte[] pFilePathByteArr = "pRawBuf.dav".getBytes();
        
        Pointer pFilePath = new Memory(pFilePathByteArr.length);
        pFilePath.write(0, pFilePathByteArr, 0, pFilePathByteArr.length);        
        stIn.pFilePath = pFilePath;        
        stIn.write();
		
        NET_OUT_TALK_SEND_DATA_FILE stOut = new NET_OUT_TALK_SEND_DATA_FILE();
        stOut.write();        
        LLong flg = netsdk.CLIENT_TalkSendDataByFile(m_hTalkHandle, stIn.getPointer(), stOut.getPointer());  
        if(flg ==  m_hTalkHandle) {
        	System.out.println("发送语音文件中的音频数据到设备成功");
        }
	}
	
	/**
	 * 功能：停止发送音频文件
	 * 描述：与talkSendDataByFile配合使用，可以停止发送音频文件
	 */
	public void stopTalkSendDataByFile(){
		boolean flg = netsdk.CLIENT_StopTalkSendDataByFile(m_hTalkHandle);
		if(flg) {
			System.out.println("停止发送音频文件成功");
		}		
	}
	
	
	/**
	 * 功能：发送语音数据到设备
	 * 描述：1.必须开始对讲，在调用播放功能
	 *     2.emEncodeType参数只支持PCM G711a G711u
	 *     3.注意封装参数参数设定，demo中以16位8K的PCM数据为例;
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("resource")
	public void talkSendDataByStream() throws IOException, InterruptedException {
		//是否需要加音频头。 TRUE，表示需要SDK根据下面的音频信息加音频头；FALSE，表示不需要SDK根据音频信息加音频头，直接发送pBuf指向的数据给设备。
		boolean bNeedHead = true;
		// 打开文件
		InputStream in;
		if(bNeedHead) {
			in = new FileInputStream("pRawBuf.dav");
		}else {
			in = new FileInputStream("pBuf.dav");
		}

		byte[] tempbytes = new byte[1280];
		int numberOfByteRead = 0; // 从文件中读取字节数
		while (true) {
			// 读取文件流
			numberOfByteRead = in.read(tempbytes);			
			if (numberOfByteRead > 0) {
				// 发送语音数据到设备
				Pointer pDataBuf = new Memory(numberOfByteRead);
				pDataBuf.write(0, tempbytes, 0, numberOfByteRead);				
				sendDataByStream(m_hTalkHandle, pDataBuf, numberOfByteRead, bNeedHead);
				
			    //部分设备对讲时候只能保存1秒左右的数据，所以发送音频文件的时候，必须要控制发送速度，以免发送过快导致设备丢数据。
			    //以16位8K的PCM数据为例，1秒钟的数据量为16*8000 bit即16*8000/8 byte(16000字节) 
			    //设置音频采集率为8000 ，位数为16 ，每秒读取byte[] tempbytes = new byte[1280]  1280字节
			    //计算1280*1000/16000 = 64
				Thread.sleep(83);
			}else {
				break;
			}	
		}				  
	}
	
	public static void sendDataByStream(LLong lTalkHandle,Pointer pDataBuf,int dwBufSize,boolean bNeedHead){		
		NET_IN_TALK_SEND_DATA_STREAM stIn = new NET_IN_TALK_SEND_DATA_STREAM();	
        //是否需要加音频头。 TRUE，表示需要SDK根据下面的音频信息加音频头；FALSE，表示不需要SDK根据音频信息加音频头，直接发送pBuf指向的数据给设备。
		stIn.bNeedHead = bNeedHead;
		if(stIn.bNeedHead) {				
			stIn.emEncodeType = NET_TALK_CODING_TYPE.NET_TALK_DEFAULT;
            stIn.nAudioBit = 16;
            stIn.dwSampleRate = 8000;
            stIn.pBuf = pDataBuf;
            stIn.dwBufSize = dwBufSize;
		} else {
			stIn.emEncodeType = NET_TALK_CODING_TYPE.NET_TALK_PCM;
            stIn.nAudioBit = 16;
            stIn.dwSampleRate = 8000;
            stIn.pBuf = pDataBuf;
            stIn.dwBufSize = dwBufSize;
		}
        stIn.write();				
		NET_OUT_TALK_SEND_DATA_STREAM stOut = new NET_OUT_TALK_SEND_DATA_STREAM();
		stOut.write();
		netsdk.CLIENT_TalkSendDataByStream(lTalkHandle, stIn.getPointer(), stOut.getPointer());	  
	}
	
	
	
	
	/**
	 *  功能：结束对讲
	 */
	public static void stopTalk() {
		if (m_hTalkHandle.longValue() == 0) {
			return;
		}
		if (netsdk.CLIENT_StopTalkEx(m_hTalkHandle)) {		
			m_hTalkHandle.setValue(0);
		} else {
			System.err.println("Stop Talk Failed!" +ToolKits.getErrorCode());
		}
	}


	/**
	 * 功能：开始设备录音记录到指定文件
	 * 描述：1.设置参数recordFlag控制是否记录设备返回的音频数据
	 * 	   2.注意此设置无法操作设备
	 * */
	public void startRecordToFile() {
		recordFlag = true;
	}
	/**
	 * 功能：停止设备录音记录到指定文件
	 * 描述：1.设置参数recordFlag控制是否记录设备返回的音频数据
	 * 	   2.注意此设置无法操作设备
	 * */
	public void stopRecordToFile() {
		recordFlag = false;
	}	
	
	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.23.12.248";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		TalkExDemo demo = new TalkExDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		TalkExDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "开始对讲", "startTalk"));
		menu.addItem(new CaseMenu.Item(this, "停止对讲", "stopTalk"));
		
		menu.addItem(new CaseMenu.Item(this, "开始设备录音记录到指定文件", "startRecordToFile"));
		menu.addItem(new CaseMenu.Item(this, "停止设备录音记录到指定文件", "stopRecordToFile"));
		
		menu.addItem(new CaseMenu.Item(this, "发送语音文件中的音频数据到设备", "talkSendDataByFile"));
		menu.addItem(new CaseMenu.Item(this, "停止发送音频文件", "stopTalkSendDataByFile"));
		
		menu.addItem(new CaseMenu.Item(this, "发送语音数据到设备", "talkSendDataByStream"));				
		
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		TalkExDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
