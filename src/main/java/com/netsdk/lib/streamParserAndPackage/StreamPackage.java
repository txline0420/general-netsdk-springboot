package com.netsdk.lib.streamParserAndPackage;

import com.netsdk.lib.LibraryLoad;
import com.netsdk.lib.SDKCallback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;


public interface StreamPackage  extends Library{
	StreamPackage INSTANCE = Native.load(LibraryLoad.getLoadLibrary("StreamPackage"), StreamPackage.class);
	
	// 接口返回值
	public static interface  SG_ERR_TYPE
	{
		public static final int SG_ERR_NOERR = 0;									/*成功*/
		public static final int SG_ERR_HANDLE_EMPTY = 1;							/*无效句柄*/
		public static final int SG_ERR_INIT_FAIL = 2;								/*初始化失败*/
		public static final int SG_ERR_PARAM_ERR = 3;								/*参数错误*/
	}
	
	// 码流封装类型
	public static interface SG_STREAM_TYPE
	{
		public static final int SG_STREAM_TYPE_DAV_STREAM = 19;						/*封装格式 DAV*/
	}
	
	// 帧类型
	public static interface SG_FRAME_TYPE
	{
		public static final int SG_FRAME_TYPE_VIDEO = 1;							/*视频帧*/
		public static final int SG_FRAME_TYPE_AUDIO = 2;							/*音频帧*/
		public static final int SG_FRAME_TYPE_EXT = 3;								/*大华扩展帧*/
	}
	
	// 视频帧子类型
	public static interface SG_FRAME_SUB_TYPE
	{
		public static final int SG_FRAME_SUB_TYPE_I = 0;							/* I帧 */
		public static final int SG_FRAME_SUB_TYPE_P = 1;							/* P帧 */
		public static final int SG_FRAME_SUB_TYPE_B = 2;							/* B帧 */
		public static final int SG_FRAME_SUB_TYPE_JPEG_FRAME = 8;           		/*JPEG 帧*/				
	}
	
	// 编码类型
	public static interface SG_ENCODE_TYPE
	{
		public static final int SG_ENCODE_VIDEO_UNKNOWN = 0;						/*视频编码格式不可知*/
		public static final int SG_ENCODE_VIDEO_MPEG4 = 1;							/*视频编码格式是MPEG4*/
		public static final int SG_ENCODE_VIDEO_JPEG = 3;							/*视频编码格式是标准JPEG*/
		public static final int SG_ENCODE_VIDEO_H264 = 4;							/*视频编码格式是大华码流H264*/

		public static final int	SG_ENCODE_AUDIO_PCM	= 7;							/*音频编码格式是PCM8*/
		public static final int	SG_ENCODE_AUDIO_G711A = 14;							/*音频编码格式是G711A*/
		public static final int	SG_ENCODE_AUDIO_PCM16 = 16;							/*音频编码格式是PCM16*/
		public static final int SG_ENCODE_AUDIO_G711U = 22;	                         /*音频编码格式是G711U*/
		public static final int SG_ENCODE_AUDIO_AAC = 26;							/*音频编码格式是AAC*/
		public static final int SG_ENCODE_AUDIO_MP2	= 31;							/*音频编码格式是MP2*/
	}
	
	// 解交错标志
	public static interface SG_DEINTERLACE_TYPE
	{
		public static final int SG_DEINTERLACE_NONE = 2;							/*无解交错*/ 
	}
	
	// 加密类型
	public static interface SG_ENCRYPT_TYPE
	{
		public static final int SG_ENCRYPT = 0;
	}
	
	// 文件头信息
	class SGHeaderInfo extends Structure
	{
		public int struct_size;
	}
	
	// 文件尾信息
	class SGTailerInfo extends Structure
	{
		public int struct_size;
	}
	
	// 帧数据信息
	@Structure.FieldOrder(value={"struct_size","frame_pointer","frame_size","frame_type","frame_sub_type",
								"frame_encode","frame_time","frame_data","width","heigth",
								"frame_rate","deinter_lace","sample_rate","bit_per_sample","channels",
								"bit_rate","rtp_channel","reserved","frame_seq","reserved2"})
	class SGFrameInfo extends Structure
	{
		public int struct_size;														/*结构体大小*/
		public Pointer frame_pointer;												/*帧数据指针*/
		public int	frame_size;														/*帧数据长度*/
		public int frame_type;														/*帧类型  SG_FRAME_TYPE*/
		public int frame_sub_type;													/*帧子类型 SG_FRAME_SUB_TYPE*/
		public int frame_encode;													/*编码类型 SG_ENCODE_TYPE*/
		public int frame_time;														/*帧时间戳*/
		public int frame_data;														/*日期时间,UTC时间*/
		
		public int width;															/*宽*/
		public int heigth;															/*高*/
		public int frame_rate;														/*帧率*/
		public int deinter_lace;													/*解交错信息 SG_DEINTERLACE_TYPE*/
		
		public int sample_rate;														/*音频采样率*/					
		public int bit_per_sample;													/*音频采样位数*/
		public int channels;														/*通道*/
		public int bit_rate;														/*音频比特率*/
		
		public byte rtp_channel;													/*通道号*/
		public byte[] reserved = new byte[7];										/*保留字段1*/
		public int frame_seq;														/*帧序号*/
		public byte[] reserved2 = new byte[188];									/*保留字段2*/
		
		public SGFrameInfo()
		{
			super(ALIGN_NONE);
		}
	}
	
	// 数据输出信息
	@Structure.FieldOrder(value={"struct_size","data_pointer","data_size","offset_type",
						"offset_pos"})
	class SGOutputData extends Structure
	{
		public int 	  struct_size;														/*结构体大小*/
		public Pointer data_pointer;													/*数据指针*/
		public int	  data_size;														/*数据长度*/
		public int 	  offset_type;														/*数据偏移类型*/
		public int 	  offset_pos;														/*数据偏移位置*/
		public SGOutputData()
		{
			super(ALIGN_NONE);
		}
	}
	
	// 内存分配函数
	public interface SGMalloc_t extends SDKCallback
	{
		public void invoke(int memory_size);
	}
	
	// 内存释放函数
	public interface SGFree_t extends SDKCallback
	{
		public void invoke(Pointer memory_pointer, 
						int memory_size);
	}
	
	// 数据输出回调函数
	public interface SGDataCB_t extends SDKCallback
	{
		public void invoke(Pointer data, Pointer user);
	}
	
	// 创建流封装器信息
	@Structure.FieldOrder(value={"struct_size","user","sg_malloc","sg_free","sg_datacb"})
	class SGCreateParam extends Structure
	{
		public int struct_size;															/*结构体大小*/
		public Pointer user;																/*用户数据*/
		public SGMalloc_t sg_malloc;														/*用户自定义内存分配函数指针*/
		public SGFree_t   sg_free;															/*用户自定义内存释放函数指针*/
		public SGDataCB_t sg_datacb;														/*用户自定义数据输出回调函数*/
		
		public SGCreateParam()
		{
			super(ALIGN_NONE);
		}
	}
	/********************************************************************
	*	Funcname:			SG_CreateHandle
	*	Purpose:			创建流封装器
	*   InputParam:         nType : 码流封装类型  SG_STREAM_TYPE
	*								pCreateParam : 创建流封装器信息
	*   Return:				void* : 返回打包句柄， 失败返回NULL
	*   Created:			2020.12.08 
	*********************************************************************/
	Pointer SG_CreateHandle(int nType, Pointer pCreateParam);
		
	/********************************************************************
	*	Funcname: 	    	SG_DestroyHandle
	*	Purpose:			销毁流封装器
	*   InputParam:         handle : 流封装器句柄
	*   Return:				成功返回 SG_ERR_NOERR，失败返回错误码
	*   Created:			2020.12.08  
	*********************************************************************/
	int	SG_DestroyHandle(Pointer handle);
		
	/********************************************************************
	*	Funcname: 	    	SG_CreateHeader
	*	Purpose:			创建文件头
	*   InputParam:         handle : 流封装器句柄
	*								pHeader : 文件头信息
	*   Return:				成功返回 SG_ERR_NOERR，失败返回错误码
	*   Created:			2020.12.08
	*********************************************************************/
	int SG_CreateHeader(Pointer handle, Pointer pHeader);
		
	/********************************************************************
	*	Funcname: 	    	StreamPacket_InputData
	*	Purpose:			传入媒体数据
	*   InputParam:         handle : 打包句柄
	*								frame_info : 需要打包的媒体信息
	*   Return:				成功返回 SG_ERR_NOERR，失败返回错误码
	*   Created:			2013.12.08 
	*********************************************************************/
	int SG_InputFrame(Pointer handle, Pointer pFrame);
		
	/********************************************************************
	*	Funcname: 	    	StreamPacket_CreateTailer
	*	Purpose:			删除打包句柄
	*   InputParam:         handle : 打包句柄
	*   Return:				成功返回 SG_ERR_NOERR，失败返回错误码
	*   Created:			2020.12.08 
	*********************************************************************/
	int SG_CreateTailer(Pointer handle, Pointer pTailer);
		
	/********************************************************************
	*	Funcname: 	    	SG_SetEncryptType
	*	Purpose:			设置加密类型
	*   InputParam:         handle : 打包句柄
							type: 加密类型，见SG_ENCRYPT_TYPE
							key: 秘钥指针
							keylen: 秘钥长度
	*   Return:             成功返回 SG_ERR_NOERR，失败返回错误码
	*   Created:	        2020.12.08
	*********************************************************************/
	int SG_SetEncryptType(Pointer handle, int type, byte[] key, int keyLen);
}
