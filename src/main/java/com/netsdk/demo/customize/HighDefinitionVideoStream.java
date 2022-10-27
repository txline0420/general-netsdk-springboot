package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class HighDefinitionVideoStream {
	static VPRLib vprlib 	= VPRLib.VPR_config;
	private static int nHandle;
	public void inti(){
		int nType =1;
		String sParas = "172.23.30.60,37777,admin,admin123";
		nHandle=vprlib.VC_Init(nType, sParas);
		if(nHandle>0) {
			System.out.printf("Login Device Port Success!\n");
		}
		else {	
			System.out.printf("Login Device  Port Fail.Last Error[%d]\n" , nHandle);
			EndTest();
		}
	}
	
	public void EndTest(){
		System.out.println("End Test");
		if( nHandle> 0)
		{
			vprlib.VC_Deinit(nHandle);
		}
		System.out.println("See You...");
		System.exit(0);
	}
	//启动显示视频(windows)
	public void StartDisplay(){
		int nWidth=400;
		int nHeight=400;
		int nWinId=0;
		int nRet=vprlib.VC_StartDisplay(nHandle, nWidth, nHeight, nWinId);
		if(nRet==0){
			System.out.println("VC_StartDisplay success.");
			System.out.println("是否进行抓图？");
			String choose;
			Scanner in=new Scanner(System.in);
			choose=in.next();
			if(choose.equals("y")){
				GetImage();  //获取图片
				GetImageFile();   //获取图片文件
			}
		}else{
			System.out.printf("VC_StartDisplay Fail.Last Error[0x%x]\n" , nRet);
		}
	}
	/*//启动显示视频(linux)
	public void StartDisplayEx(){
		int nWidth=400;
		int nHeight=400;
		int nTop=0;
		int nLeft=0;
		int nRet=vprlib.VC_StartDisplayEx(nHandle, nWidth, nHeight, nTop, nLeft);
		if(nRet==0){
			System.out.println("VC_StartDisplayEx success.");
		}else{
			System.out.printf("VC_StartDisplayEx Fail.Last Error[0x%x]\n" , nRet);
		}
	}*/
	//停止显示视频
	public void StopDisplay(){
		int nRet=vprlib.VC_StopDisplay(nHandle);
		if(nRet==0){
			System.out.println("VC_StartDisplayEx success.");
		}else{
			System.out.printf("VC_StartDisplayEx Fail.Last Error[0x%x]\n" , nRet);
		}
	}
	//获取图片
	public void GetImage(){
		int nFormat=1;
		IntByReference nLength=new IntByReference(100);
		Pointer sImage=new Memory(100);
		int nRet=vprlib.VC_GetImage(nHandle, nFormat, sImage, nLength);
		if(nRet==0){
			File path = new File("D:/EventPicture/");
            if (!path.exists()) {
                path.mkdir();
            }
            String strFileName = path + "\\" + System.currentTimeMillis() + ".jpg";
            ToolKits.savePicture(sImage, nLength.getValue(), strFileName);
			System.out.println("VC_GetImage success. nLength:"+nLength.getValue());
		}else{
			System.out.printf("VC_GetImage Fail.Last Error[%d]\n" , nRet);
		}
	}
	//获取图片文件
	public void GetImageFile(){
		int nFormat=1;
		String sFilleName="D:/EventPicture/image.jpg";
		int nRet=vprlib.VC_GetImageFile(nHandle, nFormat, sFilleName);
		if(nRet==0){
			System.out.println("VC_GetImageFile success.");
		}else{
			System.out.printf("VC_GetImageFile Fail.Last Error[%d]\n" , nRet);
		}
	}
	//字符叠加
	public void TVPDisplay(){
		int nRow=3;
		int nCol=3;
		String sText="湖南高速欢迎你！\n";
		sText+="车型：一  车种：0! \n";
		sText+="车牌：湘A889k8";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sText = sText + "\n时间" + sdf.format(new Date());
		int nRet=vprlib.VC_TVPDisplay(nHandle, nRow, nCol, sText);
		if(nRet==0){
			System.out.println("叠加内容成功 \n");// 中文要转GBK
		}else{
			System.out.printf("叠加内容失败 .Last Error[0x%x]\n" , nRet);
		}
	}
	//字符清除
	public void TVPClear(){
		int nRow=3;
		int nCol=3;
		int nLengt=500;
		int nRet=vprlib.VC_TVPClear(nHandle, nRow, nCol, nLengt);
		if(nRet==0){
			System.out.println("清除叠加内容成功");
		}else{
			System.out.printf("清除叠加内容失败 .Last Error[0x%x]\n" , nRet);
		}
	}
	
	//同步时间
	public void SyncTime(){
		String sSysTime="20190912114610";                          //输入时间格式：yyyyMMddHHmmss
		int nRet=vprlib.VC_SyncTime(nHandle, sSysTime);
		if(nRet==0){
			System.out.println("同步时间成功");
		}else{
			System.out.printf("同步时间失败 .Last Error[%d]\n" , nRet);
		}
	}
	//设置时间显示格式
	public void ShowTime(){
		SyncTime();
		int nStyle=0;
		int nRet=vprlib.VC_ShowTime(nHandle, nStyle);
		if(nRet==0){
			System.out.println("设置时间显示格式成功");
		}else{
			System.out.printf("设置时间显示格式失败 .Last Error[0x%x]\n" , nRet);
		}
	}
	public void getStatus(){
		IntByReference pStatusCode=new IntByReference();
		int nRet=vprlib.VC_GetStatus(nHandle, pStatusCode);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 状态:" + pStatusCode.getValue());
			break;
		}
		default:
			System.out.printf("操作失败 .Last Error[0x%x]\n" , nRet);
		}
		getStatusMsg(pStatusCode.getValue());
	}
	
	public void getStatusMsg(int nStatusCode){
		int nStatusMsgLen = 128;
		Pointer sStatusMsg=new Memory(nStatusMsgLen);
		sStatusMsg.clear(nStatusMsgLen);
		int nRet = vprlib.VC_GetStatusMsg(nStatusCode, sStatusMsg, nStatusMsgLen);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 状态信息:" + new String(sStatusMsg.getByteArray(0, nStatusMsgLen)).trim()); // 中文要转GBK
			break;
		}
		default:
			System.out.printf("操作失败 .Last Error[0x%x]\n" , nRet);
			break;
		}
		GetVersion();
	}
	public void GetVersion(){
		int nDevVerLen=128;
		int nAPIVerLen=128;
		Pointer sDevVersion=new Memory(nDevVerLen);
		Pointer sAPIVersion=new Memory(nAPIVerLen);
		/*sDevVersion.clear(nDevVerLen);
		sAPIVersion.clear(nAPIVerLen);*/
		int nRet=vprlib.VC_GetHWVersion(sDevVersion, nDevVerLen, sAPIVersion, nAPIVerLen);
		switch(nRet){
		case VPRLib.RET_OK:{
			System.out.println("操作成功 硬件版本信息:" + new String(sDevVersion.getByteArray(0, nDevVerLen)).trim()+
					"设备固件版本信息"+new String(sAPIVersion.getByteArray(0, nAPIVerLen)).trim()); // 中文要转GBK
			break;
		}
		default:
			System.out.printf("操作失败 .Last Error[0x%x]\n" , nRet);
		}
	}
	
	public void run(){
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "StartDisplay", "StartDisplay"));
		menu.addItem(new CaseMenu.Item(this , "StopDisplay", "StopDisplay"));
		menu.addItem(new CaseMenu.Item(this , "TVPDisplay", "TVPDisplay"));
		menu.addItem(new CaseMenu.Item(this , "TVPClear", "TVPClear"));
		menu.addItem(new CaseMenu.Item(this , "ShowTime", "ShowTime"));
		menu.addItem(new CaseMenu.Item(this , "getStatus", "getStatus"));		
		menu.run();
	}
	public static void main(String[]args){
		HighDefinitionVideoStream hdv=new HighDefinitionVideoStream();
		hdv.inti();
		hdv.run();
		hdv.EndTest();
	}
}
