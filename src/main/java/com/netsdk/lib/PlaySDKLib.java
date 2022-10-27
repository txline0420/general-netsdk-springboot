package com.netsdk.lib;

import com.netsdk.lib.NetSDKLib.SdkStructure;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * PlaySDK JNA接口封装
 */
public interface PlaySDKLib extends Library {

    PlaySDKLib PLAYSDK_INSTANCE = Native.load(LibraryLoad.getLoadLibrary("dhplay"), PlaySDKLib.class);

    /**
     * 错误码表
     *
     * @author 29779
     */
    public class PlaySDKLastError {
        public static final int DH_PLAY_NOERROR = 0;        //没有错误
        public static final int DH_PLAY_PARA_OVER = 1;        //输入参数非法
        public static final int DH_PLAY_ORDER_ERROR = 2;        //调用顺序不对
        public static final int DH_PLAY_TIMER_ERROR = 3;        //多媒体时钟设置失败
        public static final int DH_PLAY_DEC_VIDEO_ERROR = 4;        //视频解码失败
        public static final int DH_PLAY_DEC_AUDIO_ERROR = 5;        //音频解码失败
        public static final int DH_PLAY_ALLOC_MEMORY_ERROR = 6;        //分配内存失败
        public static final int DH_PLAY_OPEN_FILE_ERROR = 7;        //文件操作失败
        public static final int DH_PLAY_CREATE_OBJ_ERROR = 8;        //创建线程事件等失败
        public static final int DH_PLAY_CREATE_DDRAW_ERROR = 9;        //创建directDraw失败
        public static final int DH_PLAY_CREATE_OFFSCREEN_ERROR = 10;        //创建后端缓存失败
        public static final int DH_PLAY_BUF_OVER = 11;        //缓冲区满,输入流失败
        public static final int DH_PLAY_CREATE_SOUND_ERROR = 12;        //创建音频设备失败
        public static final int DH_PLAY_SET_VOLUME_ERROR = 13;        //设置音量失败
        public static final int DH_PLAY_SUPPORT_FILE_ONLY = 14;        //只能在播放文件时才能使用
        public static final int DH_PLAY_SUPPORT_STREAM_ONLY = 15;        //只能在播放流时才能使用
        public static final int DH_PLAY_SYS_NOT_SUPPORT = 16;        //系统不支持,解码器只能工作在Pentium 3以上
        public static final int DH_PLAY_FILEHEADER_UNKNOWN = 17;        //没有文件头
        public static final int DH_PLAY_VERSION_INCORRECT = 18;        //解码器和编码器版本不对应
        public static final int DH_PLAY_INIT_DECODER_ERROR = 19;        //初始化解码器失败
        public static final int DH_PLAY_CHECK_FILE_ERROR = 20;        //文件太短或码流无法识别
        public static final int DH_PLAY_INIT_TIMER_ERROR = 21;        //初始化多媒体时钟失败
        public static final int DH_PLAY_BLT_ERROR = 22;        //位拷贝失败
        public static final int DH_PLAY_UPDATE_ERROR = 23;        //显示overlay失败
        public static final int DH_PLAY_MEMORY_TOOSMALL = 24;        //缓冲太小
    }

    /**
     * 视频码流类型
     *
     * @author 29779
     */
    public static class T_VIDEO extends SdkStructure {
        public static final int T_UYVY = 1;        //UYVY类型的YUV数据,现在不支持.
        public static final int T_IYUV = 3;        //IYUV(I420)类型YUV数据
        public static final int T_RGB32 = 7;        //RGB32类型
    }

    public static class RenderType extends SdkStructure {
        public static final int RENDER_NOTSET = 0;
        public static final int RENDER_GDI = 1;
        public static final int RENDER_X11 = RENDER_GDI;
        public static final int RENDER_D3D = 2;
        public static final int RENDER_OPENGL = RENDER_D3D;
        public static final int RENDER_DDRAW = 3;
    }

    public static class DecodeType extends SdkStructure {
        public static final int DECODE_NOTSET = 0;
        public static final int DECODE_SW = 1;
        public static final int DECODE_HW = 2;         //拷贝模式
        public static final int DECODE_HW_FAST = 3;  //直接显示模式
        public static final int DECODE_MSDK = 4;
    }

    /**
     * PLAY_GetFreePort 获取空闲通道号,上限为501.与PLAY_ReleasePort匹对使用.
     *
     * @param plPort plPort,输出参数,返回获取的通道号.
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_GetFreePort(IntByReference plPort);

    /**
     * PLAY_OpenStream  打开流播放
     *
     * @param nPort        通道号
     * @param pFileHeadBuf 目前不使用,填NULL.
     * @param nSize        目前不适用,填0.
     * @param nBufPoolSize 设置播放器中存放数据流的缓冲区大小. 范围是[SOURCE_BUF_MIN,SOURCE_BUF_MAX].一般设为900*1024，如果数
     *                     据送过来相对均匀,可调小该值,如果数据传输不均匀,可增大该值.
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_OpenStream(int nPort, byte[] pFileHeadBuf, int nSize, int nBufPoolSize);

    /**
     * 抓图回调函数
     */
    public interface IDisplayCBFun extends SDKCallback {
        /**
         * IDisplsyCBFun,
         *
         * @param nPort     通道号
         * @param pBuf      返回图像数据
         * @param nSize     返回图像数据大小
         * @param nWidth    画面宽,单位像素
         * @param nHeight   画面高
         * @param nStamp    时标信息，单位毫秒
         * @param nType     数据类型,T_RGB32,T_UYVY,详见 T_VIDEO
         * @param pReserved 对应用户自定义参数
         * @see T_VIDEO
         */
        void invoke(int nPort, Pointer pBuf, int nSize, int nWidth, int nHeight, int nStamp, int nType, Pointer pReserved);
    }

    /**
     * PLAY_SetDisplayCallBack
     * 设置视频抓图回调函数.如果要停止回调,可以把回调函数指针设为NULL,该函数可以在任何时候调用
     *
     * @param nPort
     * @param DisplayCBFun 抓图回调函数,
     * @param pUserData    用户自定义参数
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_SetDisplayCallBack(int nPort, IDisplayCBFun DisplayCBFun, Pointer pUserData);

    /**
     * PLAY_Play 开始播放.如果已经播放,改变当前播放状态为正常速度播放.
     *
     * @param nPort 通道号
     * @param hWnd  播放窗口句柄
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_Play(int nPort, Pointer hWnd);

    /**
     * PLAY_InputData 输 入数据流,PLAY_Play之后使用
     *
     * @param nPort 通道号
     * @param pBuf  缓冲区地址
     * @param nSize 缓冲区大小
     * @return 成功返回 1,失败返回 0. 如失败,一般是缓冲区已满,用户可暂停输入，一段时间之后再输入流，确保播放库不丢失数据。
     */
    public int PLAY_InputData(int nPort, byte[] pBuf, int nSize);

    /**
     * PLAY_Stop 停止播放
     *
     * @param nPort 通道号
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_Stop(int nPort);

    /**
     * PLAY_CloseStream 关闭流
     *
     * @param nPort 通道号
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_CloseStream(int nPort);

    /**
     * PLAY_ReleasePort 释放通道号,与PLAY_RealsePort匹对使用.
     *
     * @param lPort
     * @return 成功返回 1,失败返回 0.
     */
    public int PLAY_ReleasePort(int lPort);

    /**
     * PLAY_GetLastError 获取错误码
     *
     * @param nPort 通道号
     * @return 获得当前错误的错误码.请参见错误码宏定义
     * @see PlaySDKLastError
     */
    public int PLAY_GetLastError(int nPort);

    /**
     * PLAY_GetLastErrorEx 获取错误码
     *
     * @param nPort 通道号
     * @return 获得当前错误的错误码.请参见错误码宏定义
     * @see PlaySDKLastErrorEx
     */
    public int PLAY_GetLastErrorEx(int nPort);

    /**
     * PLAY_GetPlayedTimeEx 获取文件当前播放时间
     *
     * @param nPort 通道号
     * @return 文件当前播放的时间, 单位毫秒.
     */
    public int PLAY_GetPlayedTimeEx(int nPort);

    /**
     * PLAY_SetPlayedTimeEx 设置文件当前播放时间
     *
     * @param nPort 通道号    nTime,设置文件播放位置到指定时间,单位毫秒.
     * @return BOOL, 成功返回TRUE, 失败返回FALSE.
     */
    public boolean PLAY_SetPlayedTimeEx(int nPort, int nTime);

    /**
     * PLAY_SetEngine 指定解码器(Windows平台), PLAY_Play之前调用有效， 如果单一设置其中一个Engine，可以将另一个传入NOTSET, 例如:PLAY_SetEngine(0, DECODE_HW, RENDER_NOTSET);
     *
     * @param nPort      通道号
     * @param decodeType 解码模式（仅限于H264, Hevc)
     * @param renderType 渲染模式
     * @return true:成功    false:失败
     */
    public boolean PLAY_SetEngine(int nPort, int decodeType, int renderType);

    /**
     * PLAY_SetSecurityKey   设置AES解密密钥
     *
     * @param nPort   解码通道
     * @param szKey   密钥的指针
     * @param nKeylen 密钥的长度
     * @return true:成功    false:失败
     */
    public boolean PLAY_SetSecurityKey(int nPort, String szKey, int nKeylen);

    public static class LOG_LEVEL extends SdkStructure {
        public static final int LOG_LevelUnknown = 0; // unknown
        public static final int LOG_LevelFatal = 1;         // fatal, when setting this level, (fatal) will output
        public static final int LOG_LevelError = 2;         // error,when setting this level, (fatal,error) will output
        public static final int LOG_LevelWarn = 3;         // warn, when setting this level, (fatal,error,warn) will output
        public static final int LOG_LevelInfo = 4;         // info, when setting this level, (fatal,error,warn,info) will output
        public static final int LOG_LevelTrace = 5;         // Trace, when setting this level, (fatal,error,warn,info,trace) will output
        public static final int LOG_LevelDebug = 6;         // Debug, when setting this level, (fatal,error,warn,info,trace,debug) will output
    }

    ;

    public void PLAY_SetPrintLogLevel(int logLevel);

    // 按时间回放进度回调函数原形
    public interface fDecCBFun extends SDKCallback {

        // pFrameInfo FRAME_INFO
        void invoke(int nPort, Pointer pBuf, int nSize, Pointer pFrameInfo, Pointer pUserData, int nReserved2);
    }

    /**
     * 设置解码回调，替换播放器中的显示部分，由用户自己控制显示，该函数在
     * PLAY_Play之前调用，在PLAY_Stop时自动失效，下次调用PLAY_Play之前
     * 需要重新设置。解码部分不控制速度，只要用户从回调函数中返回，解码器
     * 就会解码下一部分数据。适用于只解码不显示的情形。
     *
     * @return BOOL，成功返回TRUE，失败返回FALSE
     * @param[in] nPort 通道号
     * @param[out] DecCBFun 解码回调函数指针,不能为NULL
     * @param[in] pUserData 用户自定义参数
     * @note 如果返回失败，可以调用PLAY_GetLastErrorEx接口获取错误码。
     */
    public boolean PLAY_SetDecCallBackEx(int nPort, fDecCBFun DecCBFun, Pointer pUserData);

    /* 帧信息 */
    public static class FRAME_INFO extends SdkStructure {
        public int nWidth;                    // 画面宽，单位像素。如果是音频数据则为0
        public int nHeight;                    // 画面高，如果是音频数据则为0
        public int nStamp;                    // 时标信息，单位毫秒
        public int nType;                    // 视频帧类型，T_AUDIO16，T_RGB32，T_IYUV
        public int nFrameRate;                // 视频表示帧率，音频表示采样率
    }

    /**
     * 查询信息。
     *
     * @param[in] nPort 通道号
     * @param[in] cmdType 指定状态查询指令，见CMD_TYPE_E
     * @param[in] buf 存放信息的缓冲
     * @param[in] buflen 缓冲长度
     * @param[out] returnlen 获取的信息的有效数据长度
     * @return BOOL，成功返回TRUE，失败返回FALSE
     * @note 如果返回失败，可以调用PLAY_GetLastErrorEx接口获取错误码。
     */
    public boolean PLAY_QueryInfo(int nPort, int cmdType, Pointer buf, int buflen, IntByReference returnlen);

    public static class TimeInfo extends SdkStructure {
        public int year;
        public int month;
        public int day;
        public int hour;
        public int minute;
        public int second;

        @Override
        public String toString() {
            return year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second;
        }
    }
}
