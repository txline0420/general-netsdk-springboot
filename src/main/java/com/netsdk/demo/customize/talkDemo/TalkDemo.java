package com.netsdk.demo.customize.talkDemo;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NETDEV_TALKDECODE_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.streamParserAndPackage.StreamPackage;
import com.netsdk.lib.streamParserAndPackage.StreamParser;
import com.netsdk.lib.structure.NET_AUDIO_DECODE_FORMAT;
import com.netsdk.lib.structure.NET_IN_AUDIO_DECODE_CAPS;
import com.netsdk.lib.structure.NET_OUT_AUDIO_DECODE_CAPS;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.*;

/**
 * 语音对讲接口实现 
 * 1.把一个pcm音频文件，通过语音对讲流程，发送到设备进行语音播放  
 * 2.把设备返回的带大华头的音频数据，通过playsdk给的接口把大华头去掉，去掉头后的数据保存到本地
 * 3.测试音频数据以16位8K的PCM数据为例  测试设备：H8  TP7i  VTO
 * 
 * @author 251823
 */
public class TalkDemo {
	//初始化动态库
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;
	static StreamPackage StreamPackage_INSTANCE = StreamPackage.INSTANCE;
	static StreamParser StreamParser_INSTANCE = StreamParser.INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

	// 登录句柄
	private static LLong loginHandle = new LLong(0);

	// 语音对讲句柄
	public static LLong m_hTalkHandle = new LLong(0);
	
	//设备录音记录开关，控制设备开启对讲后，是否将设备收集音频数据记录到指定文件
	public static Boolean recordFlag = false;

	String address = "172.23.12.248"; // 172.23.12.194  172.23.12.92  172.23.12.181
	int port = 37777;
	String username = "admin";
	String password = "admin123";

	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();

		private DisconnectCallback() {
		}

		public static DisconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
		}
	}

	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();

		private HaveReconnectCallback() {
		}

		public static HaveReconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
		}
	}

	public void InitTest() {
		// 初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);

		// 设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

		// 向设备登入
		int nSpecCap = 0;
		IntByReference nError = new IntByReference(0);
		loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, nSpecCap, null, deviceInfo, nError);

		if (loginHandle.longValue() == 0) {
			System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port,
					netsdkApi.CLIENT_GetLastError());
			// 释放SDK资源，在关闭工程时调用
			EndTest();
			return;
		}

		System.out.printf("Login Device [%s:%d] Success. \n", address, port);
	}
	
	public void EndTest() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	/**
	 * 获取接口错误码
	 * 
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff)
				+ " ). 参考  NetSDKLib.java }";
	}
	
	/**
	 * 功能：查询设备语音对讲支持的音频格式
	 * 描述：查询设备语音对讲支持的音频格式，主要设备能力如编码类型,采样率,位数等
	 * */
	public void queryDevState() {
		//CLIENT_QueryDevState  NET_DEVSTATE_TALK_ECTYPE		
		NetSDKLib.NETDEV_TALKFORMAT_LIST devTalkFormatList = new NetSDKLib.NETDEV_TALKFORMAT_LIST();
		devTalkFormatList.write();
		IntByReference pRetLen = new IntByReference(0); 		
		netsdkApi.CLIENT_QueryDevState(loginHandle, NetSDKLib.NET_DEVSTATE_TALK_ECTYPE, devTalkFormatList.getPointer(), devTalkFormatList.size(), pRetLen, 1000);
		devTalkFormatList.read();
		System.out.println("devTalkFormatList:"+devTalkFormatList.nSupportNum);
		NETDEV_TALKDECODE_INFO[] type = devTalkFormatList.type;
		for (int i = 0; i < devTalkFormatList.nSupportNum; i++) {
			System.out.println("--------------------------开始------------------------------");
			System.out.println("编码类型:"+type[i].encodeType);//查考枚举 NET_TALK_CODING_TYPE
			System.out.println("位数:"+type[i].nAudioBit);
			System.out.println("采样率:"+type[i].dwSampleRate);
			System.out.println("打包周期:"+type[i].nPacketPeriod);
			System.out.println("---------------------------结束-----------------------------");
		}		
	}
	
	
	/**
	 * 功能：获取音频解码能力集
	 * 描述：1.获取音频解码能力集，channel 为0 表示本地通道号(NVR)，大于1表示对应的通道号
	 * 注意事项： 当channel = 0且sourceType = 0 时，表示设备的音频对讲解码能力；当channel>=1 且 sourceType = 0 时，表示对应通道的音频对讲解码能力。   
	 * */
	public void queryAudioDecodeCaps() {
		//入参
		NET_IN_AUDIO_DECODE_CAPS pInBuf = new NET_IN_AUDIO_DECODE_CAPS();
		pInBuf.nChannel = 2;// channel 为0 表示本地通道号(NVR)，大于1表示对应的通道号
		pInBuf.emSourceType = 0; //数据流源类型      0 对讲数据
		pInBuf.write();
		
		//出参
		NET_OUT_AUDIO_DECODE_CAPS pOutBuf = new NET_OUT_AUDIO_DECODE_CAPS();
		pOutBuf.write();		
		boolean flg = netsdkApi.CLIENT_QueryDevInfo(loginHandle, NetSDKLib.NET_QUERY_AUDIO_DECODE_CAPS, pInBuf.getPointer(), pOutBuf.getPointer(), null, 3000);
		if (flg) {
			pOutBuf.read();	
			System.out.println("nFormatsRet:"+ pOutBuf.nFormatsRet);
			NET_AUDIO_DECODE_FORMAT[] stuDecodeFormats = pOutBuf.stuDecodeFormats;
			for (int i = 0; i < pOutBuf.nFormatsRet; i++) {
				System.out.println("--------------------------开始------------------------------");
				System.out.println("编码类型:"+stuDecodeFormats[i].emCompression);//查考枚举 NetSDKLib.NET_EM_AUDIO_FORMAT
				System.out.println("音频采样频率:"+stuDecodeFormats[i].nFrequency);
				System.out.println("音频采样深度:"+stuDecodeFormats[i].nDepth);
				System.out.println("音频打包周期:"+stuDecodeFormats[i].nPacketPeriod);
				System.out.println("---------------------------结束-----------------------------");
			}	
			
		} else {
			System.out.println("获取音频解码能力集:" + ENUMERROR.getErrorMessage());
		}
		
		
	}
	

	/**
	 *  功能：开始对讲
	 *  描述：开启对讲模式,调用CLIENT_StartTalkEx接口，会执行数据回调函数AudioDataCB
	 *  场景：1.直接登录设备相机,进行音频播放,不需要设置转发模式
	 *      2.经过了NVR中转，登陆NVR后，需要设置 int isTransfer  = 1; //1为转发  0为不转发     int Channel = 11; //设置转发通道号（具体使用场景设置值）
	 */
	public static boolean startTalk() {
		// 设置语音对讲编码格式(此设置根据设备的具体能力) 只对客户端起作用，服务器方式无意义
		NETDEV_TALKDECODE_INFO talkEncode = new NETDEV_TALKDECODE_INFO();
		talkEncode.encodeType = NetSDKLib.NET_TALK_CODING_TYPE.NET_TALK_PCM;// 语音编码类型      为带头PCM
		talkEncode.dwSampleRate = 8000;//采样率
		talkEncode.nAudioBit = 16;//位数
		talkEncode.nPacketPeriod = 25; //打包周期
		talkEncode.write();
		if (netsdkApi.CLIENT_SetDeviceMode(loginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_ENCODE_TYPE,
				talkEncode.getPointer())) {
			System.out.println("Set Talk Encode Type Succeed!");
		} else {
			System.err.println("Set Talk Encode Type Failed!" + getErrorCode());
			return false;
		}

		// 设置语音对讲喊话参数 (NET_TALK_SERVER_MODE  设置服务器方式进行语音对讲)
		if (netsdkApi.CLIENT_SetDeviceMode(loginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_SERVER_MODE, null)) { 
			System.out.println("Set Talk Speak Mode Succeed!");
		} else {
			System.err.println("Set Talk Speak Mode Failed!" + ToolKits.getErrorCode());
			return false;
		}	
		
		
		// 设置语音对讲是否为转发模式(场景：1.直接登录设备相机,不需要设置转发模式;2.经过了NVR中转,需要设置 )
		int isTransfer  = 1; //1为转发  0为不转发
		int Channel = 2; //设置转发通道号
		NetSDKLib.NET_TALK_TRANSFER_PARAM talkTransfer = new NetSDKLib.NET_TALK_TRANSFER_PARAM();
		talkTransfer.bTransfer = isTransfer;//1为转发
		talkTransfer.write();
		if(netsdkApi.CLIENT_SetDeviceMode(loginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_TRANSFER_MODE, talkTransfer.getPointer())) {
			System.out.println("Set Talk Transfer Mode Succeed!");
		} else {
			System.err.println("Set Talk Transfer Mode Failed!" + ToolKits.getErrorCode());
			return false;
		}
		
		if (talkTransfer.bTransfer == 1) { 
			// 转发模式设置转发通道	
			IntByReference nChn = new IntByReference(Channel);
			if(netsdkApi.CLIENT_SetDeviceMode(loginHandle, NetSDKLib.EM_USEDEV_MODE.NET_TALK_TALK_CHANNEL, nChn.getPointer())) {
				System.out.println("Set Talk Channel Succeed!");
			} else {
				System.err.println("Set Talk Channel Failed!" + ToolKits.getErrorCode());
				return false;
			}
		}
		
		// 开启对讲(会执行数据回调AudioDataCB)
		m_hTalkHandle = netsdkApi.CLIENT_StartTalkEx(loginHandle, AudioDataCB.getInstance(), null);
		if (m_hTalkHandle.longValue() == 0) {
			System.err.println("Start Talk Failed!" + ToolKits.getErrorCode());
			return false;
		} else {
			System.out.println("Start Talk Success");		
			return true;
		}

	}


	/**
	 *  功能：结束对讲
	 */
	public static void stopTalk() {
		if (m_hTalkHandle.longValue() == 0) {
			return;
		}
		if (netsdkApi.CLIENT_StopTalkEx(m_hTalkHandle)) {		
			m_hTalkHandle.setValue(0);
		} else {
			System.err.println("Stop Talk Failed!" + getErrorCode());
		}
	}

	/**
	 * 功能：语音对讲的数据回调
	 * 描述：1.参数AudioDataCB = 1时，设备返回的带大华头的音频数据;
	 * 	   2.在demo中可以将记录开关参数recordFlag = true,调用解析库接口，获取裸音频数据写入文件
	 */
	private static class AudioDataCB implements NetSDKLib.pfAudioDataCallBack {

		private AudioDataCB() {
		}

		private static AudioDataCB audioCallBack = new AudioDataCB();

		public static AudioDataCB getInstance() {
			return audioCallBack;
		}		

		public void invoke(LLong lTalkHandle, Pointer pDataBuf, int dwBufSize, byte byAudioFlag, Pointer dwUser) {

			if (lTalkHandle.longValue() != m_hTalkHandle.longValue()) {
				return;
			}
			if (byAudioFlag == 1 && recordFlag == true) {	
				try {					
					File file = new File("in.dav");
					if(!file.exists()){//文件不存在新建文件
						file.createNewFile();
					}
					OutputStream out = new FileOutputStream(file,true);
					out.write(pDataBuf.getByteArray(0, dwBufSize));
					out.flush();
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}				
				
				// 把设备返回的带大华头的音频数据，通过playsdk给的接口把大华头去掉，去掉头后的数据保存到本地
				Pointer handle_callback = StreamParser_INSTANCE.SP_CreateStreamParser(1320);
				// 送入解析库解析数据,去掉大华音频头
				StreamParser_INSTANCE.SP_ParseData(handle_callback, pDataBuf.getByteArray(0, dwBufSize), dwBufSize);
				StreamParser.SP_FRAME_INFO info = new StreamParser.SP_FRAME_INFO();
				info.write();
				if (StreamParser.SP_RESULT.SP_SUCCESS == StreamParser_INSTANCE.SP_GetOneFrame(handle_callback,info.getPointer())) {
					info.read();
					// 将裸音频数据写入文件
					try {
						//if(!new File("D:/test/").isDirectory()){
				        	//new File("D:/test/").mkdirs();// 目录不存在就新建目录
				       // }
				        File file = new File("out.dav");
				        if(!file.exists()){//文件不存在新建文件				        	
				        	file.createNewFile();							
				        }
						OutputStream out = new FileOutputStream(file,true);
						out.write(info.streamPointer.getByteArray(0, info.streamLen));
						out.flush();
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}				
				StreamParser_INSTANCE.SP_Destroy(handle_callback);
			}
		}
	}


	
	/**
	 * 功能：音频数据封装回调函数
	 * 描述：调用封装音频头后执行的回调函数,可以获取带有大华音频头的音频数据
	 */
	public static class SGDataCB implements StreamPackage.SGDataCB_t {
		public SGDataCB() {}
		public void invoke(Pointer data, Pointer user) {
			// data实际是SGOutputData结构体指针
			StreamPackage.SGOutputData outData = new StreamPackage.SGOutputData();
			outData.write();
			Pointer outDatPointer = outData.getPointer();
			outDatPointer.write(0, data.getByteArray(0, outData.size()), 0, outData.size());
			outData.read();

			// 音频数据发送给设备 CLIENT_TalkSendData
			LLong lSendSize = netsdkApi.CLIENT_TalkSendData(m_hTalkHandle, outData.data_pointer, outData.data_size);
			System.out.printf("CallBack finshed\n"+"---"+lSendSize);			
		}
	}

	/**
	 * 功能：裸音频数据加大华音频头后播放
	 * 描述：1.裸音频数据调用封装库接口加大华音频头，调用封装接口SG_InputFrame 会执行回调函数SGDataCB，获取封装后的音频数据;
	 * 	   2.注意封装参数参数设定，demo中以以16位8K的PCM数据为例;
	 * 	   3.部分设备对讲时候只能保存1秒左右的数据，所以发送音频文件的时候，必须要控制发送速度，以免发送过快导致设备丢数据。
	 */
	public void setPackage() throws IOException, InterruptedException {
		// 封装回调参数设置 及封装句柄创建
		StreamPackage.SGCreateParam sgParam = new StreamPackage.SGCreateParam();
		StreamPackage.SGDataCB_t callback = new SGDataCB();
		sgParam.sg_datacb = callback;
		sgParam.sg_free = null; // 这个回调对于JAVA来说没有意义
		sgParam.sg_malloc = null; // 这个回调对于JAVA来说没有意义
		sgParam.write();
		Pointer sghandle = StreamPackage_INSTANCE
				.SG_CreateHandle(StreamPackage.SG_STREAM_TYPE.SG_STREAM_TYPE_DAV_STREAM, sgParam.getPointer());
		// 打开文件
		//FileInputStream in = new FileInputStream("D:\\test\\luoyinpin\\luopcm_2.dav");
		FileInputStream in = new FileInputStream("out3.dav");
		byte[] tempbytes = new byte[1280];
		int numberOfByteRead = 0; // 从文件中读取字节数

		while (true) {
			// 读取文件流
			numberOfByteRead = in.read(tempbytes);
			if (numberOfByteRead > 0) {
				// 获取到的裸数据送入封装库
				StreamPackage.SGFrameInfo sginfo = new StreamPackage.SGFrameInfo();
				Pointer pointer = new Memory(numberOfByteRead);
				pointer.write(0, tempbytes, 0, numberOfByteRead);
				// 268是C中结构体大小，64位下测试为268，内部会进行校验，失败会不进行封装
				sginfo.struct_size = 268;
				// 帧信息赋值
				sginfo.frame_time = 0; //帧时间戳
				sginfo.frame_pointer = pointer; //帧数据指针
				sginfo.frame_size = numberOfByteRead;//帧数据长度
				sginfo.frame_type = 2;//帧类型    参考枚举 SG_FRAME_TYPE  2为音频类型
				sginfo.frame_sub_type = 0; //帧子类型   参考枚举 SG_FRAME_SUB_TYPE   0为I帧
				sginfo.frame_encode = 16;//编码类型     参考枚举  SG_ENCODE_TYPE    16为音频编码格式是PCM16   14为音频编码格式是G711A  22为音频编码格式是G711U
  
				// 音频信息赋值
				sginfo.bit_per_sample = 16;  //音频采样位数
				sginfo.sample_rate = 8000;   //音频采样率  根据设备能力设置
				sginfo.channels = 2; //通道    根据设备能力设置
				sginfo.write();
				int packRet = StreamPackage_INSTANCE.SG_InputFrame(sghandle, sginfo.getPointer());
				if (packRet == 0) {
					//部分设备对讲时候只能保存1秒左右的数据，所以发送音频文件的时候，必须要控制发送速度，以免发送过快导致设备丢数据。
					//以16位8K的PCM数据为例，1秒钟的数据量为16*8000 bit即16*8000/8 byte(16000字节) 
					//设置音频采集率为8000 ，位数为16 ，每秒读取byte[] tempbytes = new byte[1280]  1280字节
					//计算1280*1000/16000 = 80
					Thread.sleep(80);
				}

			} else {
				// 文件读取完毕，销毁句柄
				System.out.println("文件读取完毕，销毁句柄");
				in.close();
				StreamPackage_INSTANCE.SG_CreateTailer(sghandle, null);
				StreamPackage_INSTANCE.SG_DestroyHandle(sghandle);
				break;
			}
		}

	}

	/**
	 * 功能：大华音频去掉大华音频头样例
	 * 描述：调用解析库接口将大华音频数据去掉大华音频头，写入指定文件
	 * 
	 */
	public void setParser() throws IOException {
		// 创建解析器
		Pointer handle = StreamParser_INSTANCE.SP_CreateStreamParser(1320);
		InputStream in = new FileInputStream("D:\\test\\luoyinpin\\pcm16_2.dav");
		OutputStream out = new FileOutputStream("D:\\test\\jiexiku\\jiexi_2.dav");
		byte[] tempbytes = new byte[1024];
		int numberOfByteRead = 0; // 从文件中读取字节数
		while (true) {
			// 读取文件流
			numberOfByteRead = in.read(tempbytes);
			if (numberOfByteRead > 0) {
				// 送入解析库解析数据
				StreamParser_INSTANCE.SP_ParseData(handle, tempbytes, numberOfByteRead);
				StreamParser.SP_FRAME_INFO info = new StreamParser.SP_FRAME_INFO();
				info.write();
				// 从解析库获取数据
				while (StreamParser.SP_RESULT.SP_SUCCESS == StreamParser_INSTANCE.SP_GetOneFrame(handle,
						info.getPointer())) {
					info.read();
					Pointer streamPointer = info.streamPointer;
					out.write(streamPointer.getByteArray(0, info.streamLen));
				}
			} else {
				// 文件读取完毕，销毁句柄
				in.close();
				out.close();
				System.out.println("文件读取完毕，销毁句柄");
				StreamParser_INSTANCE.SP_Destroy(handle);
				break;
			}
		}
	}
	
	/**
	 * 功能：大华音频数据先解后装最后播放样例
	 * 描述：先解析大华音频数据,在封装裸音频数据，最后播放
	 * 
	 */
	@SuppressWarnings("resource")
	public void setPackageAndParser() throws InterruptedException, FileNotFoundException {
		// 创建解析器
		Pointer handle = StreamParser_INSTANCE.SP_CreateStreamParser(1280);

		byte[] tempbytes = new byte[1024];
		int numberOfByteRead = 0; // 从文件中读取字节数

		// 封装回调参数设置 及封装句柄创建
		StreamPackage.SGCreateParam sgParam = new StreamPackage.SGCreateParam();
		StreamPackage.SGDataCB_t callback = new SGDataCB();
		sgParam.sg_datacb = callback;
		sgParam.sg_free = null; // 这个回调对于JAVA来说没有意义
		sgParam.sg_malloc = null; // 这个回调对于JAVA来说没有意义
		sgParam.write();
		Pointer sghandle = StreamPackage_INSTANCE
				.SG_CreateHandle(StreamPackage.SG_STREAM_TYPE.SG_STREAM_TYPE_DAV_STREAM, sgParam.getPointer());
		// 打开文件
		InputStream in = new FileInputStream("D:\\test\\luoyinpin\\pcm16_2.dav");
		while (true) {
			try {
				// 读取文件流
				numberOfByteRead = in.read(tempbytes);
				if (numberOfByteRead > 0) {
					// 送入解析库解析数据
					StreamParser_INSTANCE.SP_ParseData(handle, tempbytes, numberOfByteRead);
					StreamParser.SP_FRAME_INFO info = new StreamParser.SP_FRAME_INFO();
					info.write();
					// 从解析库获取数据
					while (StreamParser.SP_RESULT.SP_SUCCESS == StreamParser_INSTANCE.SP_GetOneFrame(handle,
							info.getPointer())) {
						info.read();
						// info.framePointer和info.frameLen是数据指针及长度，包括帧头+ 数据
						// info.streamPointer和info.streamLen是数据指针及长度，只有数据
						// 获取到的裸数据送入封装库
						StreamPackage.SGFrameInfo sginfo = new StreamPackage.SGFrameInfo();

						// 268是C中结构体大小，64位下测试为268，内部会进行校验，失败会不进行封装
						sginfo.struct_size = 268;

						// 帧信息赋值
						sginfo.frame_time = info.timeStamp;
						sginfo.frame_pointer = info.streamPointer;
						sginfo.frame_size = info.streamLen;
						sginfo.frame_type = info.frameType;
						sginfo.frame_sub_type = info.frameSubType;
						sginfo.frame_encode = info.frameEncodeType;

						if (info.frameType == StreamParser.SP_FRAME_TYPE.SP_FRAME_TYPE_VIDEO) {
							// 视频信息赋值
							sginfo.frame_rate = info.frameRate;
							sginfo.width = info.width;
							sginfo.heigth = info.height;
						} else if (info.frameType == StreamParser.SP_FRAME_TYPE.SP_FRAME_TYPE_AUDIO) {
							// 音频信息赋值
							sginfo.bit_per_sample = info.bitsPerSample;
							sginfo.sample_rate = info.samplePerSec;
							sginfo.channels = info.channels;
						}

						sginfo.write();
						int packRet = StreamPackage_INSTANCE.SG_InputFrame(sghandle, sginfo.getPointer());
						if (0 == packRet) {
							System.out.println("封装音频头成功");
							//保证每秒
							Thread.sleep(83);
						}
					}
				} else {
					// 文件读取完毕，销毁句柄
					System.out.println("文件读取完毕，销毁句柄");
					StreamParser_INSTANCE.SP_Destroy(handle);
					StreamPackage_INSTANCE.SG_CreateTailer(sghandle, null);
					StreamPackage_INSTANCE.SG_DestroyHandle(sghandle);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	/**
	 * 功能：开始设备录音记录到指定文件
	 * 描述：1.设置参数recordFlag控制是否记录设备返回的带大华头的音频数据
	 * 	   2.注意此设置无法操作设备
	 * */
	public void startRecordToFile() {
		recordFlag = true;
	}
	/**
	 * 功能：停止设备录音记录到指定文件
	 * 描述：1.设置参数recordFlag控制是否记录设备返回的带大华头的音频数据
	 * 	   2.注意此设置无法操作设备
	 * */
	public void stopRecordToFile() {
		recordFlag = false;
	}		
	

	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		//menu.addItem(new CaseMenu.Item(this, "查询设备语音对讲支持的音频格式", "queryDevState"));
		menu.addItem(new CaseMenu.Item(this, "获取音频解码能力集", "queryAudioDecodeCaps"));
		menu.addItem(new CaseMenu.Item(this, "开始对讲", "startTalk"));
		menu.addItem(new CaseMenu.Item(this, "停止对讲", "stopTalk"));
		menu.addItem(new CaseMenu.Item(this, "开始设备录音记录到指定文件", "startRecordToFile"));
		menu.addItem(new CaseMenu.Item(this, "停止设备录音记录到指定文件", "stopRecordToFile"));
		menu.addItem(new CaseMenu.Item(this, "大华音频去掉大华音频头样例", "setParser"));
		menu.addItem(new CaseMenu.Item(this, "裸音频数据封装大华音频头后播放样例", "setPackage"));
		menu.addItem(new CaseMenu.Item(this, "大华音频数据先解后装最后播放样例", "setPackageAndParser"));	
		menu.run();
	}

	public static void main(String[] args) {
		TalkDemo demo = new TalkDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}

}
