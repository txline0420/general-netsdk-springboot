package com.netsdk.lib;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
/**
 * FaceDetetectSDK JNA接口封装
 * 
 * */
public interface FaceDetetectSDKLib extends Library{
	
	
	
	FaceDetetectSDKLib FACEDETETECTSDK_INSTANCE =
		      Native.load("facedetectsdk", FaceDetetectSDKLib.class);
	
	
	    /************************************************************************
	     ** 接口定义
	     ***********************************************************************/  
	    
	    /**
	    * @Method			CLIENT_API_Init
	    * @brief			初始化，获取License
	    * @param [in] 		pFliePath: ActivationCode.json/server.pem/license.dat三个文件所在路径
	    * @param [out] 		nError: 错误信息
	    *					1: 初始化失败
	    *					2: 获取本地NetProtoco.json文件失败
	    *					3: 申请LicenseManager实例失败
	    *					4: 申请license读写回调失败
	    *					5: 服务器参数设置校验失败
	    *					6: license文件下载失败
	    *					7: license网络授权失败
	    *					8: 申请ShareManager实例失败
	    *					9: 共享量管理器结构体配置失败
	    *					10: 设置事件回调函数失败
	    *					11: 启动共享量管理模块失败
	    * @return			true成功， false失败
	    */
	    public boolean CLIENT_API_Init(Pointer nError,String pFliePath);
	    
	    
	    /**
	    * @Method			CLIENT_API_CleanUp
	    * @brief			退出清理
	    * @return			-
	    */
	    public void CLIENT_API_CleanUp();
	    
	    
	    /**
	    * @Method			CLIENT_API_AnalyzeFaceImage
	    * @brief			分析人脸图片
	    * @param [in] 		pPicData: 人脸图片base64编码后数据
	    * @param [in] 		lPicLen: 图片base64编码后数据长度
	    * @param [in] 		nWidth: 图片宽
	    * @param [in] 		nHeight: 图片高
	    * @param [out] 		nError: 错误信息
	    *					-101: 图片base64解码失败
	    *					-102: JPG转YUV失败
	    *					-103：获取人脸矩形区域异常
	    *					-104：人脸数为0 异常
	    *					-105：人脸数大于1 异常
	    *					-106：瞳距异常
	    *					-107：消费共享量失败
	    *					-108：共享量不足
	    *					// 以下是算法接口返回的错误码
	    *					-1	//内存不足
	    *					-2	//参数不正确
	    *					-3	//色彩空间不支持
	    *					-4	//图像尺寸不支持
	    *					-5	//图像跨距和宽度不相等
	    *					-6	//功能未实现
	    *					-7	//不支持的功能类型
	    *					-8	//不支持的规则类型
	    *					-9	//规则数超限
	    *					-10 //通道数超限
	    *					-11 //指定了相应功能，但没有配置信息
	    *					(-12)//每条规则的功能数目数量超限
	    *					(-13)//线或者区域的点数超限
	    *					(-14)//错误的功能类型.一般指该功能类型和规则类型不符
	    *					(-15)//断言失败
	    *					(-16)//规则点数错误
	    *					(-17)//图像尺寸超限
	    *					(-18)//错误的场景类型
	    *					(-19)//错误的算法类型
	    *					(-20)//多车道检测库返回错误信息
	    *					(-21)//帧率不正确
	    *					(-22)//屏蔽区域数量超限
	    *					(-23)//景深参数没有区域
	    *					(-24)//景深参数没有点信息
	    *					(-25)//DFFP出错,标定出错，无法计算或计算结果出错
	    *					(-26)//无法识别的轨迹帧版本
	    *					(-91)//程序初始化失败				
	    * @return			true 成功， false 失败
	    */
	    public boolean CLIENT_API_AnalyzeFaceImage(Pointer pPicData, int nPicLen, int nWidth, int nHeight, Pointer nError);
	    
	    
	    /**
	    * @Method			CLIENT_API_OpenLog
	    * @brief			开启日志,本地当前目前下会生成一个faceinmagever_log/faceinmagever_log.log日子文件
	    * @return			true成功， false失败
	    */
	    public boolean CLIENT_API_LogOpen();
	    
	    /**
	    * @Method			CLIENT_API_OpenLog
	    * @brief			关闭日志
	    * @return			true成功， false失败
	    */
	    public boolean  CLIENT_API_LogClose();
	    	    

}
