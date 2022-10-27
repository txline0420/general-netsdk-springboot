package com.netsdk.lib.streamParserAndPackage;
import com.netsdk.lib.LibraryLoad;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;


public interface StreamParser  extends Library{
	//StreamParser INSTANCE = (StreamParser) Native.loadLibrary((Platform.isWindows()? "StreamParser":"C"), StreamParser.class);
    StreamParser INSTANCE = Native.load(LibraryLoad.getLoadLibrary("StreamParser"), StreamParser.class);
	
	// 接口返回值
	public static interface  SP_RESULT
	{
		public static final int SP_SUCCESS = 0;									/*成功*/
		public static final int SP_ERROR_INVALID_HANDLE = 1;					/*无效句柄*/
		public static final int SP_ERROR_FILE_TYPE_NOSUPPORT = 2;				/*文件类型不支持*/
		public static final int SP_ERROR_STREAM_NOSUPPORT= 3;					/*流类型不支持*/
		public static final int SP_ERROR_PARAMETER = 6;							/*参数有误*/			
		public static final int SP_ERROR_BAD_FORMATTED = 9;    					/*文件格式错误*/
		public static final int SP_ERROR_BUFFER_OVERFLOW = 12;					/*内部缓冲区溢出*/
		public static final int SP_ERROR_SYSTEM_OUT_OF_MEMORY = 13; 			/*系统内存不够*/
		public static final int SP_ERROR_LIST_EMPTY = 14;						/*列表为空*/
	}
	
	// 帧类型
	public static interface SP_FRAME_TYPE
	{
		public static final int SP_FRAME_TYPE_UNKNOWN = 0;						/*帧类型不可知*/
		public static final int SP_FRAME_TYPE_VIDEO = 1;						/*帧类型是视频帧*/
		public static final int SP_FRAME_TYPE_AUDIO = 2;						/*帧类型是音频帧*/
		public static final int SP_FRAME_TYPE_DATA = 3;							/*帧类型是数据帧*/
	}
	
	// 帧子类型
	public static interface SP_FRAME_SUB_TYPE
	{
		public static final int SP_FRAME_SUB_TYPE_DATA_INVALID = -1;			/*数据无效*/
		public static final int	SP_FRAME_SUB_TYPE_VIDEO_I_FRAME = 0;			/*I帧*/
		public static final int	SP_FRAME_SUB_TYPE_VIDEO_P_FRAME	= 1;			/*P帧*/
		public static final int	SP_FRAME_SUB_TYPE_VIDEO_B_FRAME = 2;			/*B帧*/
		public static final int	SP_FRAME_SUB_TYPE_VIDEO_JPEG_FRAME = 8;			/*JPEG帧*/
	}
	
	// 视频编码类型
	public static interface SP_ENCODE_VIDEO_TYPE
	{
		public static final int SP_ENCODE_VIDEO_UNKNOWN = 0;					/*视频编码格式不可知*/
		public static final int	SP_ENCODE_VIDEO_MPEG4 = 1;						/*视频编码格式是MPEG4*/
		public static final int	SP_ENCODE_VIDEO_HI_H264 = 2;					/*视频编码格式是海思H264*/
		public static final int	SP_ENCODE_VIDEO_JPEG = 3;						/*视频编码格式是标准JPEG*/
		public static final int	SP_ENCODE_VIDEO_DH_H264 = 4;					/*视频编码格式是大华码流H264*/
		public static final int	SP_ENCODE_VIDEO_MPEG2 = 9;         				/*视频编码格式是MPEG2*/
		public static final int	SP_ENCODE_VIDEO_DH_H265 = 12;					/*视频编码格式是H265*/
		public static final int	SP_ENCODE_VIDEO_H263 = 35;	     				/*视频编码格式是H263*/
	}
	
	// 音频编码类型
	public static interface SP_ENCODE_AUDIO_TYPE
	{
		public static final int SP_ENCODE_AUDIO_UNKNOWN = 0;
		public static final int	SP_ENCODE_AUDIO_PCM = 7;						/*音频编码格式是PCM8*/
		public static final int	SP_ENCODE_AUDIO_G729 = 8;						/*音频编码格式是G729*/
		public static final int	SP_ENCODE_AUDIO_IMA = 9;						/*音频编码格式是IMA*/
		public static final int SP_ENCODE_PCM_MULAW = 10;						/*音频编码格式是PCM MULAW*/
		public static final int SP_ENCODE_AUDIO_G721 = 11;						/*音频编码格式是G721*/
		public static final int SP_ENCODE_PCM8_VWIS = 12;						/*音频编码格式是PCM8_VWIS*/
		public static final int SP_ENCODE_MS_ADPCM = 13;						/*音频编码格式是MS_ADPCM*/
		public static final int SP_ENCODE_AUDIO_G711A = 14;						/*音频编码格式是G711A*/
		public static final int SP_ENCODE_AUDIO_AMR = 15;						/*音频编码格式是AMR*/
		public static final int SP_ENCODE_AUDIO_PCM16 = 16;						/*音频编码格式是PCM16*/
		public static final int SP_ENCODE_AUDIO_G711U = 22;						/*音频编码格式是G711U*/
		public static final int SP_ENCODE_AUDIO_G723 = 23;						/*音频编码格式是G723*/
		public static final int SP_ENCODE_AUDIO_AAC = 26;						/*音频编码格式是AAC*/
		public static final int SP_ENCODE_AUDIO_MP2 = 31;						/*音频编码格式是mp2*/
		public static final int SP_ENCODE_AUDIO_OGG = 32;						/*音频编码格式是ogg vorbis*/
		public static final int	SP_ENCODE_AUDIO_MP3 = 33;						/*音频编码格式是mp3*/
		public static final int SP_ENCODE_AUDIO_G722_1 = 34;					/*音频编码格式是G722.1*/
		public static final int SP_ENCODE_AUDIO_AC = 49;						/*音频编码格式是AC3*/
	}
	
	// 加密类型
	public static interface SP_ENCRYPT_TYPE
	{
		public static final int SP_ENCRYPT_UNKOWN = 0;
		public static final int SP_ENCRYPT_AES = 1;
	}
	
	// 时间信息
	@Structure.FieldOrder(value={"nYear","nMonth","nDay","nHour","nMinute","nSecond","nMilliSecond"})
	class SP_TIME extends Structure
	{
		public int nYear;														/*年*/
		public int nMonth;														/*月*/		
		public int nDay;														/*日*/	
		public int nHour;														/*小时*/
		public int nMinute;														/*分钟*/
		public int nSecond;														/*秒*/
		public int nMilliSecond;												/*毫秒*/
	}
	
	// 帧信息
	@Structure.FieldOrder(value={"frameType","frameSubType","frameEncodeType","reserved1","streamPointer","streamLen",
								 "framePointer","frameLen","frameTime","timeStamp","frameSeq","frameRate",
								 "width","height","reserved2","reserved3","samplePerSec","bitsPerSample",
								 "channels","isValid","reaserved4"})
	class SP_FRAME_INFO extends Structure
	{
			
		/*类型*/
		public int frameType;													/*帧类型*/
		public int frameSubType;												/*帧子类型*/
		public int frameEncodeType;												/*帧编码类型*/
		public int reserved1;													/*保留字段*/
		
		/*数据*/
		public Pointer streamPointer;											/*指向码流数据*/
		public int	   streamLen;												/*码流长度*/
		public Pointer  framePointer;												/*指向帧头*/
		public int    frameLen;													/*帧长度（包括帧头、数据、帧尾）*/
		
		/*时间*/
		public SP_TIME frameTime = new SP_TIME();								/*时间信息*/
		public int 	timeStamp;													/*时间戳*/
		
		/*序号*/
		public int		frameSeq;												/*帧序号*/
		
		/*视频属性，关键帧才有*/
		public int		frameRate;												/*帧率*/
		public int 	width;														/*宽*/
		public int		height;													/*高*/
		
		public int		reserved2;
		public int		reserved3;
		
		/*音频属性*/
		public int		samplePerSec;											/*采样频率*/
		public int 	bitsPerSample;												/*采样位数*/
		public int		channels;												/*声道数*/
		
		/*错误标志*/
		public int		isValid;												/*0为有效，非0表示真错误*/
		
		// 64位保留字段
		public byte[] reaserved4 = new byte[432];								/*保留字节*/
		
		// 32位保留字段
		//public byte[] reaserved4 = new byte[412];								/*保留字节*/
	}
	
	/********************************************************************
	 *	Funcname: 	    	SP_CreateStreamParser
	 *	Purpose:			创建流分析器
	 *  InputParam:         nBufferSize: 需要开辟的缓冲区大小，不能小于SP_PaseData每次传入的数据流长度
	 *  OutputParam:      	无
	 *  Return:				NULL: 创建流分析器失败
	 *						其他值：流解析器句柄   
	*********************************************************************/
	Pointer SP_CreateStreamParser(int nBufferSize);

	 /********************************************************************
	 *	Funcname: 	    	SP_ParseData
	 *	Purpose:			输入数据流,并同步进行分析
	 *  InputParam:         handle:	通过SP_CreateStreamParser返回的句柄
	 *								stream:	数据流缓冲地址
	 *								length:	数据流长度
	 *  OutputParam:      	无
	 *  Return:				详见SP_RESULT
	*********************************************************************/
	int SP_ParseData(Pointer handle, byte[] stream, int length);

	/********************************************************************
	 *	Funcname: 	    	SP_GetOneFrame
	 *	Purpose:			同步获取一帧信息,反复调用直到失败
	 *  InputParam:         handle:	通过SP_CreateStreamParser返回的句柄
	 *								frameInfo: 外部SP_FRAME_INFO的一个结构地址。
	 *  OutputParam:      	无
	 *  Return:				详见SP_RESULT 
	*********************************************************************/
	int SP_GetOneFrame(Pointer handle, Pointer frameInfo);

	/********************************************************************
	 *	Funcname: 	    	SP_StreamEncryptKey
	 *	Purpose:	                            设置实时流解析秘钥
	 *  InputParam:         handle: 通过SP_CreateStreamParser或SP_CreateFileParser返回的句柄。
	 *						type : 秘钥类型 ：SP_ENCRYPT
	 *						key：秘钥数据
	 *						keylen：秘钥长度
	 *  OutputParam:        无
	 *  Return:             详见SP_RESULT
	*********************************************************************/
	int SP_StreamEncryptKey(Pointer handle, int type, byte[] key, int keylen);

	/********************************************************************
	 *	Funcname: 	    	SP_Destroy
	 *	Purpose:			销毁码流分析器
	 *  InputParam:         handle: 通过SP_CreateStreamParser返回的句柄。
	 *  OutputParam:      	无
	 *  Return:				详见SP_RESULT
	*********************************************************************/
	int SP_Destroy(Pointer handle);

	/********************************************************************
	 *	Funcname: 	    	SP_GetLastError
	 *	Purpose:			获得码流分析库错误码
	 *  InputParam:         handle: 通过SP_CreateStreamParser或SP_CreateFileParser返回的句柄。
	 *  OutputParam:      	无
	 *  Return:				详见SP_RESULT
	*********************************************************************/
	int SP_GetLastError(Pointer handle);
}
