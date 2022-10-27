package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib.SdkStructure;
import com.netsdk.lib.SDKCallback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface VPRLib extends Library {
	
	VPRLib VPR_INSTANCE = (VPRLib)Native.load("D:\\Work\\Demo\\JAVA\\JNADemo\\libs\\win32\\VPR.dll", VPRLib.class);
	VPRLib VPR_config = (VPRLib)Native.load("D:\\Work\\Demo\\JAVA\\JNADemo\\libs\\win32\\VideoCard_DAHUA.dll", VPRLib.class);
	//车牌识别接口错误码
	public static final int	    RET_OK							=0;						// 操作成功
	public static final int		ERROR_DEVICE_UNRESPONSIVE		=-100;					// 设备无响应
	public static final int		ERROR_INPARAM					=-1000;					// 传入参数出错
	public static final int		ERROR_DEVICE_OCCUPIED			=-1001;					// 设备被占用
	public static final int		ERROR_OPEN_FAILURE				=-1002;					// 打开失败
	public static final int		ERROR_SETSHOW_WIN				=-1003;					// 设置显示错误(WIN)
	public static final int		ERROR_SETSHOW_LINUX				=-1004;					// 设置显示错误(LINUX)
	public static final int		ERROR_STOPSHOW					=-1005;					// 停止显示错误
	public static final int		ERROR_GETIMAGE					=-1006;					// 获取图片错误
	public static final int		ERROR_GETIMAGE_FILE				=-1007;					// 获取图片文件错误
	public static final int		ERROR_OTHER						=-2000;					// 其它错误	
	
	public static class T_VLPINFO extends SdkStructure
	{
		public int					vlpInfoSize;		                        // 识别结构体的大小
		public byte[]				vlpTime=new byte[20];		                // 识别时间，格式 “yyyyMMddHHmmsszzz”
		public int					vlpCarClass;		                        // 车型
		public byte[]		        vlpColor=new byte[2];		                // 车牌颜色（数字编码）"00" 蓝色、 "01" 黄色、 "02" 黑色、 "03" 白色,“04”渐变绿色，“05”黄绿双拼色，“06”蓝白渐变色
		public byte[]		        vlpText=new byte[16];		                // 车牌文字， GBK编码
		public int		            vlpReliability;		                        // 识别车牌可信度（采用四位表示 9999 表示为 99.99%）
		public int[]		        imageLength=new int[3];		                // 识别图片长度:[0]=场景图长度，[1]=车牌图长度，[2]=二值化图长度
		public Pointer[]		    image=new Pointer[3];			            // 识别图片:[0]=场景图，[1]=车牌图，[2]=二值化图
		
		public T_VLPINFO() {
			this.vlpInfoSize = this.size();
		}
		
		 @Override
		protected int getNativeAlignment(Class<?> type, Object value, boolean isFirstElement) {
			 int alignment = super.getNativeAlignment(type, value, isFirstElement);
			 return Math.min(1, alignment);
	      }
	}
	
	
	/*******************************车牌识别***********************************/
	// 设置识别结果回调定义 
	public interface CBFun_GetRegResult extends SDKCallback {
        public void invoke(int nHandle, T_VLPINFO pVlpResult, Pointer pUser);
    }
	
	// 设备状态回调定义
	public interface CBFun_GetDevStatus extends SDKCallback {
        public void invoke(int nHandle, int nStatus, Pointer pUser);
    }	
	//资源初始化
	public 	int  VLPR_Init();
	//释放资源
	public 	int  VLPR_Deinit();
	//连接设备
	public 	int  VLPR_Login(int nType,String sParas);
	//断开设备连接
	public 	int  VLPR_Logout(int nHandle);
	//设置识别结果回调
	public 	int  VLPR_SetResultCallBack(int nHandle,CBFun_GetRegResult pFunc,Pointer pUser);
	//设置设备状态回调
	public 	int  VLPR_SetStatusCallBack(int nHandle,int nTimeInvl,CBFun_GetDevStatus pFunc,Pointer pUser);
	//手动触发抓拍
	public 	int  VLPR_ManualSnap(int nhandle);
	//获取设备状态
    public 	int  VLPR_GetStatus(int nHandle, IntByReference pStatusCode);
	//获取错误码详细描述
	public 	int  VLPR_GetStatusMsg(int nStatusCode, Pointer sStatusMsg, int nStatusMsgLen);
	//获取设备版本信息
	public 	int  VLPR_GetHWVersion(int nHandle, Pointer sHWVersion, int nHWVerMaxLen, Pointer sAPIVersion, int nAPIVerMaxLen);
	
	
	
	/*******************************高清视频流接口***********************************/
	//初始化视频流
	public int  VC_Init(int nType, String sParas);
	//释放资源
	public int VC_Deinit(int nHandle);

	public int VC_StartDisplay(int nHandle, int nWidth, int nHeight, int nWinId);

	public int VC_StopDisplay(int nHandle);

	public int VC_GetImage(int nHandle, int nFormat, Pointer sImage, IntByReference nLength);

	public int VC_GetImageFile(int nHandle, int nFormat, String sFilleName);

	public int VC_TVPDisplay(int nHandle, int nRow, int nCol,String sText);
	
	public int VC_TVPClear(int nHandle, int nRow, int nCol,  int nLengt);

	public int VC_SyncTime(int nHandle, String sSysTime);

	public int VC_ShowTime(int nHandle, int nStyle);

	public int VC_GetStatus(int nHandle, IntByReference pStatusCode);

	public int VC_GetStatusMsg(int nStatusCode, Pointer sStatusMsg, int nStatusMsgLen);

	public int VC_GetHWVersion( Pointer sDevVersion, int nDevVerLen, Pointer sAPIVersion, int nAPIVerLen);
	
}
class test implements VPRLib{

	@Override
	public int VLPR_Init() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_Deinit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_Login(int nType, String sParas) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_Logout(int nHandle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_SetResultCallBack(int nHandle, CBFun_GetRegResult pFunc,
			Pointer pUser) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_SetStatusCallBack(int nHandle, int nTimeInvl,
			CBFun_GetDevStatus pFunc, Pointer pUser) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_ManualSnap(int nhandle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_GetStatus(int nHandle, IntByReference pStatusCode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_GetStatusMsg(int nStatusCode, Pointer sStatusMsg,
			int nStatusMsgLen) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VLPR_GetHWVersion(int nHandle, Pointer sHWVersion,
			int nHWVerMaxLen, Pointer sAPIVersion, int nAPIVerMaxLen) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_Init(int nType, String sParas) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_Deinit(int nHandle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_StartDisplay(int nHandle, int nWidth, int nHeight, int nWinId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_StopDisplay(int nHandle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_GetImage(int nHandle, int nFormat, Pointer sImage,
			IntByReference nLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_GetImageFile(int nHandle, int nFormat, String sFilleName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_TVPDisplay(int nHandle, int nRow, int nCol, String sText) {
		VPRLib vprlib 	= VPRLib.VPR_config;
		System.out.println(sText);
		sText+="\0";
		System.out.println(sText);
		int lo=vprlib.VC_TVPDisplay(nHandle, nRow, nCol, sText);
		return lo;
	}

	@Override
	public int VC_TVPClear(int nHandle, int nRow, int nCol, int nLengt) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_SyncTime(int nHandle, String sSysTime) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_ShowTime(int nHandle, int nStyle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_GetStatus(int nHandle, IntByReference pStatusCode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_GetStatusMsg(int nStatusCode, Pointer sStatusMsg,
			int nStatusMsgLen) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int VC_GetHWVersion(Pointer sDevVersion, int nDevVerLen,
			Pointer sAPIVersion, int nAPIVerLen) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}