package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE;
import com.netsdk.lib.NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;


public class FindFileExDemo extends Initialization {

	/**
	 * 获取文件数量
	 *
	 * @param findHandle 查询句柄
	 * @return 失败返回  -1 成功返回 实际数量
	 */
	public int getTotalFiles(LLong findHandle) {
		IntByReference nCount = new IntByReference(0);
		boolean bRet = netSdk.CLIENT_GetTotalFileCount(findHandle, nCount, null, 3000);
		if (!bRet) {
			System.err.println("GetTotalFileCount failed! " + netSdk.CLIENT_GetLastError());
			return -1;
		}

		System.out.println("The Total File: " + nCount.getValue());

		return nCount.getValue();
	}
	/**
	 * 查询对比数据
	 */
	public void findFile() {
		int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE;



		/**
		 *  查询条件
		 */
		NetSDKLib.NET_IN_MEDIA_QUERY_FILE findContion = new NetSDKLib.NET_IN_MEDIA_QUERY_FILE();
		Scanner scanner = new Scanner(System.in);
		System.out.print("通道号:");
		String tmp = scanner.nextLine();
		findContion.nChannelID = Integer.parseInt(tmp);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setLenient(false);
		System.out.println("时间输入格式请参考\"yyyy-MM-dd HH:mm:ss\"");
		System.out.print("开始时间：");
		String stringRec = scanner.nextLine();
		Date startTime = new Date();
		Date endTime = new Date();
		try {
			startTime = sdf.parse(stringRec);
		} catch (ParseException e) {
			System.out.println("Format Error !!");
			e.printStackTrace();
		}
		System.out.print("结束时间：");
		stringRec = scanner.nextLine();
		try {
			endTime = sdf.parse(stringRec);
		} catch (ParseException e) {
			System.out.println("Format Error !!");
			e.printStackTrace();
		}
//		System.out.println(findContion.nChannelID);
//		System.out.println(startTime.toString());
//		System.out.println(endTime.toString());
		// 文件类型，0-所有，1-jpg，2-dav
		findContion.nMediaType = 1;
		//筛选查询事件数量为1
		findContion.nEventCount = 1;
		//对应智能事件人脸识别，0x00000117
		findContion.nEventLists[0] = 0x00000117;
		Calendar cstartTime = Calendar.getInstance();
		Calendar cendTime = Calendar.getInstance();
		cstartTime.setTime(startTime);
		cendTime.setTime(endTime);
		// 开始时间
		findContion.stuStartTime.dwYear = cstartTime.get(Calendar.YEAR);
		findContion.stuStartTime.dwMonth = cstartTime.get(Calendar.MONTH) + 1;
		findContion.stuStartTime.dwDay = cstartTime.get(Calendar.DAY_OF_MONTH);
		findContion.stuStartTime.dwHour = cstartTime.get(Calendar.HOUR_OF_DAY);
		findContion.stuStartTime.dwMinute = cstartTime.get(Calendar.MINUTE);
		findContion.stuStartTime.dwSecond = cstartTime.get(Calendar.SECOND);

		// 结束时间
		findContion.stuEndTime.dwYear = cendTime.get(Calendar.YEAR);
		findContion.stuEndTime.dwMonth = cendTime.get(Calendar.MONTH) + 1;
		findContion.stuEndTime.dwDay = cendTime.get(Calendar.DAY_OF_MONTH);
		findContion.stuEndTime.dwHour = cendTime.get(Calendar.HOUR_OF_DAY);
		findContion.stuEndTime.dwMinute = cendTime.get(Calendar.MINUTE);
		findContion.stuEndTime.dwSecond = cendTime.get(Calendar.SECOND);

//		System.out.println(findContion.stuStartTime.toStringTimeEx());
//		System.out.println(findContion.stuEndTime.toStringTimeEx());
		/**
		 * 以下注释的查询条件参数，目前设备不支持，后续会逐渐增加
		 */
//		// 地点,支持模糊匹配
//		String machineAddress = "";
//		System.arraycopy(machineAddress.getBytes(), 0, findContion.szMachineAddress, 0, machineAddress.getBytes().length);
//
//		// 待查询报警类型
//		findContion.nAlarmType = EM_FACERECOGNITION_ALARM_TYPE.NET_FACERECOGNITION_ALARM_TYPE_ALL;

		// 通道号，-1查询所有
//		findContion.nChannelID = -1;

//		// 人员组数
//		findContion.nGroupIdNum = 1;
//
//		// 人员组ID(人脸库ID)
//		String groupId = "";
//		System.arraycopy(groupId.getBytes(), 0, findContion.szGroupIdArr[0].szGroupId, 0, groupId.getBytes().length);
//
//		// 人员信息扩展是否有效
//		findContion.abPersonInfoEx = 1;
//
//		// 人员组ID(人脸库ID)
//		System.arraycopy(groupId.getBytes(), 0, findContion.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);

		findContion.write();
		LLong lFindHandle = netSdk.CLIENT_FindFileEx(loginHandle, type, findContion.getPointer(), null, 3000);
		if(lFindHandle.longValue() == 0) {
			System.err.println("FindFileEx Failed!" + netSdk.CLIENT_GetLastError());
			return;
		}
		findContion.read();

		int totalFileCount = getTotalFiles(lFindHandle);
		if(totalFileCount != -1){
			System.out.println("文件数量：" + getTotalFiles(lFindHandle));
		}else{
			System.out.println("获取文件数量失败！！");
		}


		int nMaxConut = 10;
		NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] faceRecognitionInfo = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[nMaxConut];
		for (int i = 0; i < faceRecognitionInfo.length; ++i) {
			faceRecognitionInfo[i] = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE();
			//faceRecognitionInfo[i].bUseCandidatesEx = 1;
		}

		int MemorySize = faceRecognitionInfo[0].size() * nMaxConut;
		Pointer pointer = new Memory(MemorySize);
		pointer.clear(MemorySize);

		ToolKits.SetStructArrToPointerData(faceRecognitionInfo, pointer);

		//循环查询
		int nCurCount = 0;
		int nFindCount = 0;
		while(true) {
			int nRetCount = netSdk.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, pointer, MemorySize, null, 3000);
			ToolKits.GetPointerDataToStructArr(pointer, faceRecognitionInfo);

			if (nRetCount <= 0) {
				System.err.println("FindNextFileEx failed!" + netSdk.CLIENT_GetLastError());
				break;
			}

			for (int i = 0; i < nRetCount; i++) {
				nFindCount = i + nCurCount * nMaxConut;
				System.out.println("[" + nFindCount + "]通道号 :" + faceRecognitionInfo[i].nChannelID);
				System.out.println("[" + nFindCount + "]报警发生时间 :" + faceRecognitionInfo[i].stuStartTime.toStringTime());
				System.out.println("[" + nFindCount + "]文件类型 :" + faceRecognitionInfo[i].byFileType);
				// 人脸图
				System.out.println("[" + nFindCount + "]人脸图路径 :" + new String(faceRecognitionInfo[i].szFilePath).trim());
				DownloadRemoteFile(new String(faceRecognitionInfo[i].szFilePath));

				System.out.println();
			}

			if(nRetCount < nMaxConut) {
				break;
			} else {
				nCurCount++;
			}
		}

		netSdk.CLIENT_FindCloseEx(lFindHandle);
	}

	public  void  DownloadRemoteFile(String filePath){
		NET_IN_DOWNLOAD_REMOTE_FILE pInParam=new NET_IN_DOWNLOAD_REMOTE_FILE();
		pInParam.pszFileName = new NativeString(filePath).getPointer();
		pInParam.pszFileDst = new NativeString("./"+filePath.substring(filePath.lastIndexOf("/")+1,filePath.lastIndexOf("."))+"face.jpg").getPointer();
		NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam=new NET_OUT_DOWNLOAD_REMOTE_FILE();
		if(!netSdk.CLIENT_DownloadRemoteFile(loginHandle, pInParam, pOutParam, 3000)){
			System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		}else{
			System.out.println("CLIENT_DownloadRemoteFile success");
		}
	}


	public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "findFile" , "findFile")));
		menu.run();
	}

	public static void  main(String []args){
		FindFileExDemo FindFileExDemo=new FindFileExDemo();
		Scanner sc = new Scanner(System.in);
		System.out.print("ip:");
		String ip = sc.nextLine();
		System.out.print("port:");
		String tmp = sc.nextLine();
		int port = Integer.parseInt(tmp);
		System.out.print("username:");
		String username = sc.nextLine();
		System.out.print("password:");
		String pwd = sc.nextLine();
		InitTest(ip,port,username,pwd);
		FindFileExDemo.RunTest();
		LoginOut();
	}
}
