package com.netsdk.demo.faceDetetect;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.FaceDetetectSDKLib;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * @author 251823
 * @version 1.0
 * @description 分析人脸图片
 * @date 2021/05/08
 */
public class FaceDetetctDemo {

	// 初始化动态库
	static FaceDetetectSDKLib sdkApi = FaceDetetectSDKLib.FACEDETETECTSDK_INSTANCE;
	
	
	/**
	 * 初始化
	 * */
	
	public void init() {
		// 打开日志
		//sdkApi.CLIENT_API_LogOpen();
		/*
		 * if(!sdkApi.CLIENT_API_LogOpen()) {
		 * System.out.println("CLIENT_API_LogOpen failed!\\n-------------------------");
		 * }else { System.out.
		 * println("CLIENT_API_LogOpen success!------------------------------"); }
		 */
		// 第一步：初始化接口
		// 错误信息
		Pointer nError = new Memory(4);
		// 配置文件路径pFliePath: ActivationCode.json/server.pem/license.dat三个文件所在路径
		String pFliePath = "/home/netsdk/251823/DemoTest";		
		if (!sdkApi.CLIENT_API_Init(nError,pFliePath)) {
			System.out.println("CLIENT_API_Init failed!\\n------------------------------"+nError.getInt(0));
			cleanUp();
		}else {
			System.out.println("CLIENT_API_Init success!------------------------------");
		}
	}

	/**
	 * 分析人脸图片
	 */
	public void analyzeFaceImage() {
		Pointer nError = new Memory(4);
		// 第二步：分析人脸图片
		String imagePath = "./S_19793_1.jpg";
		//int fileSize = 0;
		int uImageWidth = 0;
		int uImageHeight = 0;
		// 获取图片大小，宽高
		File picture = new File(imagePath);
		try {
			BufferedImage sourceImg = ImageIO.read(new FileInputStream(picture));
			uImageWidth = sourceImg.getWidth();
			uImageHeight = sourceImg.getHeight();
			// fileSize = (int) picture.length();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 获取图片字节流
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		byte[] imageByteArray = null;
		try {
			is = new FileInputStream(picture);
			baos = new ByteArrayOutputStream();
			// 3、操作(分段读取)
			byte[] flush = new byte[1024 * 10];// 缓冲容器
			int len = -1;// 接收长度
			try {
				while ((len = is.read(flush)) != -1) {
					// 写出到字节数组中
					baos.write(flush, 0, len);
				}
				baos.flush();
				imageByteArray = baos.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			// 4、释放资源
			try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// 调算法库分析
		Pointer pPicData = new Memory(imageByteArray.length);		 
		pPicData.write(0, imageByteArray, 0, imageByteArray.length);		
		boolean flg = sdkApi.CLIENT_API_AnalyzeFaceImage(pPicData, imageByteArray.length, uImageWidth, uImageHeight, nError);
		if(!flg) {
			System.out.println("CLIENT_API_AnalyzeFaceImage failed!,nError ="+nError.getInt(0));
		}else {
			System.out.println("CLIENT_API_AnalyzeFaceImage success!------------------------------");
		}
		
			
	}

	/**
	 * 退出清理
	 * 
	 */
	public void cleanUp() {
		sdkApi.CLIENT_API_CleanUp();
	}

	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "初始化", "init"));
		menu.addItem(new CaseMenu.Item(this, "分析人脸图片", "analyzeFaceImage"));
		menu.run();
	}

	public static void main(String[] args) {
		
		FaceDetetctDemo demo = new FaceDetetctDemo();
		demo.RunTest();				
	}

	
}
