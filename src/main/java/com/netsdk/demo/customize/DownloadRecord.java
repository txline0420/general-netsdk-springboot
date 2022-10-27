package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetFinalVar;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_STORAGE_DISK_PREDISKCHECK;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.enumeration.NET_VOLUME_TYPE;
import com.netsdk.lib.structure.NET_CFG_FILE_HOLD_DAYS_INFO;
import com.netsdk.lib.structure.NET_IN_STORAGE_DEV_INFOS;
import com.netsdk.lib.structure.NET_OUT_STORAGE_DEV_INFOS;
import com.netsdk.lib.structure.NET_STORAGE_DEVICE;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DownloadRecord {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	// 设备信息
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();

	// 登陆句柄
	private LLong m_loginHandle = new LLong(0);   

	// 下载句柄

	private LLong n_hDownLoadHandle = new LLong(0);

    private TimeDownLoadDataCallBack m_DownLoadData = new TimeDownLoadDataCallBack(); // 录像下载数据回调 
	// 按时间下载数据回调
	public class TimeDownLoadDataCallBack implements fDataCallBack {
    	public int invoke(LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
//    		System.out.println("TimeDownLoadDataCallBack [ " + dwUser +" ]");
    		return 0;
    	}
    }
	
    class LockFileInfo {
    	public int  nCluster; // 簇号 
    	public int	nDriveNo; // 磁盘号
    	
    	public LockFileInfo(int nCluster, int nDriveNo) {
    		this.nCluster = nCluster;
    		this.nDriveNo = nDriveNo;
    	}
    }
    
    // 加锁文件列表
    private List<LockFileInfo> lstLockFile = new ArrayList<LockFileInfo>();
    
    // 报警监听标示
    private boolean bListening = false;
    
	/*************************************************************************************
	*								General function								 	 * 
	*************************************************************************************/
	// device disconnect callback
	// call CLIENT_Init to set it, when device reconnect, sdk will call it.
	public static class DisConnectCallback implements fDisConnect{
		
		private DisConnectCallback() {}
		
		private static class CallBackHolder {
			private static final DisConnectCallback cb = new DisConnectCallback();
		}
		
		public static final DisConnectCallback getInstance() {
			return CallBackHolder.cb;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
		
	// device reconnect callback
	// call CLIENT_SetAutoReconnect to set it, when device reconnect, sdk will call it.
	public static class HaveReConnectCallback implements fHaveReConnect{
		
		private HaveReConnectCallback() {}
		
		private static class CallBackHolder {
			private static final HaveReConnectCallback cb = new HaveReConnectCallback();
		}
		
		public static final HaveReConnectCallback getInstance() {
			return CallBackHolder.cb;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}	
    }

	/**
	 * Init Sdk and Login Device 
	 */
	public void InitTest()
	{		
		// init sdk 
		netsdkApi.CLIENT_Init(DisConnectCallback.getInstance(), null);
    	
		// Set re-connection callback function after disconnection. Internal SDK auto connect again after disconnection (Optional)
		netsdkApi.CLIENT_SetAutoReconnect(HaveReConnectCallback.getInstance(), null); 
		
		// Set alarm listen callback
		netsdkApi.CLIENT_SetDVRMessCallBack(MessCallback.getInstance(), null);

		// Set device connection timeout value and trial times, Optional
		int waitTime = 5000; // connection 5s timeout
		int tryTimes = 3;    // trial 3 times
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// Open SDK log, Optional
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
		
		File path = new File(".");			
		String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";
			
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
	
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
		if (!bLogopen) {
			System.err.println("Failed to open NetSDK log !!!");
		}
		
    	// login device
    	int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	m_loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(m_loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d] Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d] Failed. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    		EndTest();
    	}
	}
	
	/**
	 * Logout Device And Cleanup Sdk 
	 */
	public void EndTest()
	{
		if( m_loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(m_loginHandle);
		}
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}
	
	/*************************************************************************************
	*								Download Record Function							 * 
	*************************************************************************************/
	
	/**
	 *  设置回放时的码流类型
	 * @param m_streamType 码流类型
	 */
	public boolean setStreamType(int m_streamType) {
        int emType = EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE; // 回放录像枚举
        IntByReference steamType = new IntByReference(m_streamType); // 0-主辅码流,1-主码流,2-辅码流
        return netsdkApi.CLIENT_SetDeviceMode(m_loginHandle, emType, steamType.getPointer());
	}
	
	/**
	 *  下载录像
	 * @param nStreamType 码流类型
	 * @param nChannel 通道号
	 * @param stTimeStart 开始时间
	 * @param stTimeEnd 结束时间
	 * @param savedFileName 保存录像文件名
	 */
	public boolean downloadRecordFile(int nStreamType, int nChannel, 
			NET_TIME stTimeStart, NET_TIME stTimeEnd, String savedFileName) {
		
		if (!setStreamType(nStreamType)) {
        	System.err.println("Set Stream Type Failed!." + ToolKits.getErrorCode());
        	return false;
		}
		
		int scType = 3; 	// scType:         码流转换类型,0-DAV码流(默认); 1-PS流,3-MP4
		int nRecordFileType = EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_ALL; // 下载所有录像
		 
		/*m_hDownLoadHandle = netsdkApi.CLIENT_DownloadByTimeEx(m_loginHandle, nChannel, nRecordFileType, 
															stTimeStart, stTimeEnd, savedFileName, 
															DownloadPosCallback.getInstance(), null, null, null, null);*/
		
		n_hDownLoadHandle = netsdkApi.CLIENT_DownloadByTimeEx2(m_loginHandle, nChannel, nRecordFileType, 
				stTimeStart, stTimeEnd, savedFileName, 
				DownloadPosCallback.getInstance(), null, m_DownLoadData, null, scType, null);
		
		
		if(n_hDownLoadHandle.longValue() != 0) {
			System.out.println("Downloading RecordFile...");
		} else {
			System.err.println("Download RecordFile Failed!" + ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());
			return false;
		}
		
		return true;
	}
	
	/**
	 *  停止下载录像
	 */
	public void stopDownLoadRecordFile() {
		stopDownLoadRecordFile(n_hDownLoadHandle);
	}
	
	/**
	 *  停止下载录像
	 * @param hDownLoadHandle 下载句柄
	 */
	private static void stopDownLoadRecordFile(LLong hDownLoadHandle) {
		if (hDownLoadHandle.longValue() == 0) {		
			return;
		}
		netsdkApi.CLIENT_StopDownload(hDownLoadHandle);
	}
		
	/**
	 * 下载进度回调
	 * 回调建议写成单例模式, 回调里处理数据，需要另开线程
	 */
    public static class DownloadPosCallback implements fDownLoadPosCallBack {
    	
    	private DownloadPosCallback() {}
		
		private static class CallBackHolder {
			private static final DownloadPosCallback cb = new DownloadPosCallback();
		}
		
		public static final DownloadPosCallback getInstance() {
			return CallBackHolder.cb;
		}
		
    	@Override
    	public void invoke(LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {
       	 	
    		System.out.println("Download pos： " + dwDownLoadSize*100 / dwTotalSize);
    		if(dwDownLoadSize == -1) { // 下载结束
        		System.out.println("Downloading Complete. ");
    			new StopDownloadTask(lPlayHandle).start();
			}
    		System.out.println(dwDownLoadSize);
    	}  
    	
    	private class StopDownloadTask extends Thread {
    		private LLong lDownloadHandle;
			
			public StopDownloadTask(LLong lDownloadHandle) {
				this.lDownloadHandle = lDownloadHandle;
			}
			
			public void run() {				
				stopDownLoadRecordFile(lDownloadHandle);
			}
		}
    }
    
	/**
	 *  按时间标记录像
	 * @param nChannel 通道号
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 * @param nMark 标记动作 1:标记, 0:清除
	 */
    public boolean markFileByTime(int nChannel, NET_TIME_EX startTime, NET_TIME_EX endTime, int nMark) {
        
    	NET_IN_SET_MARK_FILE_BY_TIME stuIn = new NET_IN_SET_MARK_FILE_BY_TIME();
        NET_OUT_SET_MARK_FILE_BY_TIME stuOut = new NET_OUT_SET_MARK_FILE_BY_TIME();

        stuIn.nChannel = nChannel;
        stuIn.stuStartTime = startTime;
        stuIn.stuEndTime = endTime;
        stuIn.bFlag = nMark;

        boolean bRet = netsdkApi.CLIENT_SetMarkFileByTime(m_loginHandle, stuIn, stuOut, 10000);
        if(bRet) {
			System.out.println("Set Mark File By Time Success.");
		} else {
			System.err.println("Set Mark File By Time Failed!" + ToolKits.getErrorCode());
			return false;
		}
        return bRet;
    }
    
	/**
	 *  查询某段时间内的录像加锁文件
	 * @param nChannel 通道号
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 */
    public  boolean queryMarkFile(int nChannel, NET_TIME startTime, NET_TIME endTime) {
        
    	lstLockFile.clear();
    	
		// 获取标记录像信息
		NET_IN_MEDIA_QUERY_FILE stuQueryCondition = new NET_IN_MEDIA_QUERY_FILE();
		
		stuQueryCondition.nMediaType = 2; // 查询dav
		stuQueryCondition.nChannelID = nChannel;
		stuQueryCondition.nFalgCount = 1;
		stuQueryCondition.emFalgLists[0] = EM_RECORD_SNAP_FLAG_TYPE.FLAG_TYPE_MARKED;
		stuQueryCondition.stuStartTime = startTime;
		stuQueryCondition.stuEndTime = endTime;
		
		stuQueryCondition.write();
		LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(m_loginHandle, 
				EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE, stuQueryCondition.getPointer(), null, 3000);
		if(lFindHandle.longValue() == 0){
			System.err.println("Find Marked File Failed!" + ToolKits.getErrorCode());
			return false;
		}
		
		int nMaxQueryCount = 24;
		NET_OUT_MEDIA_QUERY_FILE[] outfile = new NET_OUT_MEDIA_QUERY_FILE[nMaxQueryCount];
		for (int i = 0; i < nMaxQueryCount; i++) {
		    outfile[i] = new NET_OUT_MEDIA_QUERY_FILE();
		}
		
		int nMemorySize = outfile[0].size() * nMaxQueryCount;
		Pointer pMediaFileInfo = new Memory(nMemorySize);
		pMediaFileInfo.clear(nMemorySize);
		ToolKits.SetStructArrToPointerData(outfile, pMediaFileInfo);
		
		int nFindCount = 0;
		while(true) {
			int nRetCount = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxQueryCount, pMediaFileInfo, nMemorySize, null, 3000);
			if(nRetCount < 0) {
				System.err.println("Find Next Marked File Failed!" + ToolKits.getErrorCode());
				netsdkApi.CLIENT_FindCloseEx(lFindHandle);	
				return false;
			}
		    
			ToolKits.GetPointerDataToStructArr(pMediaFileInfo, outfile);
			for (int i = 0; i < nRetCount; i++) {
				lstLockFile.add(new LockFileInfo(outfile[i].nCluster, outfile[i].nDriveNo));
				++nFindCount;
				System.out.println("标记文件[" + nFindCount + "]通道 :" + outfile[i].nChannelID
						+ " 开始时间 :" + outfile[i].stuStartTime.toStringTime()
						+ " 结束时间 :" + outfile[i].stuEndTime.toStringTime());
			}
			
			 if(nRetCount < nMaxQueryCount) {
			     break;
			 }
		}
		
		if (nFindCount == 0) {
			System.out.println("此时间段无加锁文件");
		}
		
		netsdkApi.CLIENT_FindCloseEx(lFindHandle);	
		
		return true;
    }
    
    /**
	 *  按文件解锁录像
	 * @param stFileInfo 文件信息 (queryMarkFile获取)
	 */
    public boolean UnlockRecordByFile(LockFileInfo stFileInfo) {
        
    	NET_IN_SET_MARK_FILE stuIn = new NET_IN_SET_MARK_FILE();
    	stuIn.emLockMode = EM_MARKFILE_MODE.EM_MARK_FILE_BY_NAME_MODE;  // 通过文件名方式对录像加锁
    	stuIn.emFileNameMadeType = EM_MARKFILE_NAMEMADE_TYPE.EM_MARKFILE_NAMEMADE_JOINT; // 拼接文件名方式
    	stuIn.nDriveNo = stFileInfo.nDriveNo;
    	stuIn.nStartCluster = stFileInfo.nCluster;
    	stuIn.byImportantRecID = (byte)0; // 清除
    	NET_OUT_SET_MARK_FILE stuOut = new NET_OUT_SET_MARK_FILE();
  
    	boolean bRet = netsdkApi.CLIENT_SetMarkFile(m_loginHandle, stuIn, stuOut, 3000);
    	if(bRet) {
 			System.out.printf("Unlock Record DriveNo[%d] Cluster[%d] Success. \n", stFileInfo.nDriveNo, stFileInfo.nCluster);
 		} else {
 			System.err.printf("Unlock Record DriveNo[%d] Cluster[%d] Failed! %s \n", stFileInfo.nDriveNo, stFileInfo.nCluster, ToolKits.getErrorCode());
 			return false;
 		}
         return bRet;
    }
    
	/**
	 * 报警监听
	 */
	public boolean startListen() {
		if (bListening) {
			return true;
		}
		
		bListening = netsdkApi.CLIENT_StartListenEx(m_loginHandle);
		if (!bListening) {
			System.err.println("Start Listen Failed!" + ToolKits.getErrorCode());
		} else { 
			System.out.println("Start Listen Success."); 
		}
		
		return bListening;
	}
	
	public static class MessCallback implements fMessCallBack {
		
		private final String[] radioStatus = {"异常", "活跃", "降级", "休眠", "同步中"};
		private MessCallback() {}
		
		private static class CallBackHolder {
			private static final MessCallback cb = new MessCallback();
		}
		
		public static final MessCallback getInstance() {
			return CallBackHolder.cb;
		}

		@Override
		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
				int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
				Pointer dwUser) {
			switch (lCommand) {
	            case NetSDKLib.NET_SHELTER_ALARM_EX: // 视频遮挡
	            	AlarmInBytes(pStuEvent, dwBufLen, "视频遮挡");
	            	break;
	            case NetSDKLib.NET_DISKFULL_ALARM_EX: // 硬盘满
	            	AlarmInBytes(pStuEvent, dwBufLen, "硬盘满");
	            	break;
	            case NetSDKLib.NET_DISKERROR_ALARM_EX: // 硬盘故障
	            	AlarmInBytes(pStuEvent, dwBufLen, "硬盘故障");
	            	break;
	            case NetSDKLib.NET_ALARM_RAID_STATE : // RAID异常报警
				{
					ALARM_RAID_INFO msg = new ALARM_RAID_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			for (int i = 0; i < msg.nRaidNumber; ++i) {
		  				if (msg.stuRaidInfo[i].byStatus >= 0 && 
		  						msg.stuRaidInfo[i].byStatus < radioStatus.length) {
		  					System.out.printf("[RAID异常报警] Raid名称:%s 状态:%s \n", 
				  					new String(msg.stuRaidInfo[i].szName).trim(), radioStatus[msg.stuRaidInfo[i].byStatus]); // 未做异常考虑
		  				}else {
		  					System.out.printf("[RAID异常报警] Raid名称:%s 状态:未知 \n", new String(msg.stuRaidInfo[i].szName).trim());
		  				}
		  			}
		  			break;
				}
	            case NetSDKLib.NET_ALARM_FRONTDISCONNECT : // 前端IPC断网报警  
				{
		  			ALARM_FRONTDISCONNET_INFO msg = new ALARM_FRONTDISCONNET_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			System.out.printf("[前端IPC断网报警] 时间:%s 前端IPC:%s 对应通道:%d 断网%s \n", 
		  					msg.stuTime.toStringTime(), new String(msg.szIpAddress).trim(), 
		  					msg.nChannelID, msg.nAction == 0? "开始": "已恢复");
		  			break;
				}
	            default:
	            	break;
	            	
	        }
				
			return true;
		}
		
		private void AlarmInBytes(Pointer pStuEvent, int dwBufLen, String alarmReason) {
			byte []alarm = new byte[dwBufLen];
        	pStuEvent.read(0, alarm, 0, dwBufLen);
        	for (int i = 0; i < dwBufLen; i++) {
        		if (alarm[i] == 1) {
    				System.out.println("通道[" + i + "]" + alarmReason);
        		}else {
        			System.out.println("通道[" + i + "]" + alarmReason + "状态正常");
        		}
        	}
		}
    }
	
	/**
	 * 停止报警监听
	 */
	public void stopListen() {
		if (bListening) {
			netsdkApi.CLIENT_StopListen(m_loginHandle);
			bListening = false;
		}
	}
	
	/**
	 * 查询在线状态
	 */
	public boolean queryOnlineState() {

		IntByReference retLen = new IntByReference(0);
		Pointer p = new Memory(Integer.SIZE);
		p.clear(Integer.SIZE);
		if (!netsdkApi.CLIENT_QueryDevState(m_loginHandle, 
											NetSDKLib.NET_DEVSTATE_ONLINE, 
											p, 
											Integer.SIZE, 
											retLen, 
											3000)) {
			System.err.println("Query Online State Failed!" + ToolKits.getErrorCode());
			return false;
		}
		
		int []a = new int[1];
		p.read(0, a, 0, 1);
		System.out.println("设备" + (a[0]==1?"在线":"断线"));
		return true;
	}
	
	/**
	 * 查询硬盘状态
	 */
	public boolean queryHardDiskState() {
		IntByReference intRetLen = new IntByReference();
		NET_DEV_HARDDISK_STATE diskInfo = new NET_DEV_HARDDISK_STATE();
		if (netsdkApi.CLIENT_QueryDevState(m_loginHandle, 
								  NetSDKLib.NET_DEVSTATE_DISK, 
								  diskInfo.getPointer(), 
								  diskInfo.size(), 
								  intRetLen, 
								  5000)) {
			diskInfo.read();
			
			String[] diskType = {"读写驱动器", "只读驱动器", "备份驱动器或媒体驱动器", "冗余驱动器", "快照驱动器"};
			String[] diskStatus = {"休眠", "活动", "故障"};
			String[] diskSignal = {"本地", "远程"};
			for (int i = 0; i < diskInfo.dwDiskNum; ++i) {
				System.out.printf("硬盘[%d] 硬盘号:%d 分区号:%d 容量:%dMB 剩余空间:%dMB 标识:%s 类型:%s 状态:%s \n", 
						i+1, diskInfo.stDisks[i].bDiskNum, diskInfo.stDisks[i].bSubareaNum, 
						diskInfo.stDisks[i].dwVolume, diskInfo.stDisks[i].dwFreeSpace, diskSignal[diskInfo.stDisks[i].bSignal],
						diskType[(diskInfo.stDisks[i].dwStatus&0xF0) >> 4], diskStatus[diskInfo.stDisks[i].dwStatus&0x0F]);
			}
		}else {
			System.err.println("Query Hard Disk State Failed!" + ToolKits.getErrorCode());
			return false;
		}
		return true;
	}
	
	/**
	 * 查询Raid状态
	 */
	public boolean queryRaidStatue() {
        IntByReference intRetLen = new IntByReference();
        ALARM_RAID_INFO raidInfo = new ALARM_RAID_INFO();
		if (netsdkApi.CLIENT_QueryDevState(m_loginHandle, 
										  NetSDKLib.NET_DEVSTATE_RAID_INFO, 
										  raidInfo.getPointer(), 
										  raidInfo.size(), 
										  intRetLen, 
										  5000)) {
			raidInfo.read();
			
			if (raidInfo.nRaidNumber == 0) {
				System.out.println("无Raid");
			}
			
			final String[] radioStatus = {"异常", "活跃", "降级", "休眠", "同步中"};
			for (int i = 0; i < raidInfo.nRaidNumber; ++i) {
  				if (raidInfo.stuRaidInfo[i].byStatus >= 0 && 
  						raidInfo.stuRaidInfo[i].byStatus < radioStatus.length) {
  					System.out.printf("Raid名称:%s 状态:%s \n", 
		  					new String(raidInfo.stuRaidInfo[i].szName).trim(), radioStatus[raidInfo.stuRaidInfo[i].byStatus]); // 未做异常考虑
  				}else {
  					System.out.printf("Raid名称:%s 状态:未知 \n", new String(raidInfo.stuRaidInfo[i].szName).trim());
  				}
  			}
	   }else {
			System.err.println("Query Radio State Failed!" + ToolKits.getErrorCode());
			return false;
	   }
		
	   return true;
	}
	
	/**
	 * 获取摄像机状态
	 */
	private boolean queryCameraState() {
		NET_CAMERA_STATE_INFO[] arrCameraStatus = new NET_CAMERA_STATE_INFO[deviceinfo.byChanNum];
		for(int i = 0; i < arrCameraStatus.length; i++) {
			arrCameraStatus[i] = new NET_CAMERA_STATE_INFO();
		}
		
		// 入参
		NET_IN_GET_CAMERA_STATEINFO stIn = new NET_IN_GET_CAMERA_STATEINFO();
		stIn.bGetAllFlag = 1; // 全部
		
		// 出参
		NET_OUT_GET_CAMERA_STATEINFO stOut = new NET_OUT_GET_CAMERA_STATEINFO();
		stOut.nMaxNum = deviceinfo.byChanNum;
		stOut.pCameraStateInfo = new Memory(arrCameraStatus[0].size() * deviceinfo.byChanNum);
		stOut.pCameraStateInfo.clear(arrCameraStatus[0].size() * deviceinfo.byChanNum);
		ToolKits.SetStructArrToPointerData(arrCameraStatus, stOut.pCameraStateInfo);  // 将数组内存拷贝到Pointer
		
		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(m_loginHandle, NetSDKLib.NET_QUERY_GET_CAMERA_STATE, 
    			stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, arrCameraStatus);  // 将Pointer拷贝到数组内存
			final String [] connectionState = {"未知", "正在连接", "已连接", "未连接", "通道未配置,无信息", "通道有配置,但被禁用"};
			for (int i = 0; i < stOut.nValidNum; ++i) {
				System.out.printf("通道%d:%s ", arrCameraStatus[i].nChannel, connectionState[arrCameraStatus[i].emConnectionState]);
				if ((i+1)%8 == 0) {
					System.out.println();
				}
			}
			
		} else {
			System.err.println("Query Camera State Failed!" + ToolKits.getErrorCode());
		}
		
		return bRet;
	}
    
    /**
	 * 获取自动维护配置
	 * @return 成功返回配置信息, 失败返回null 
	 */
    public NETDEV_AUTOMT_CFG getAutoRecordCyc() {
    	NETDEV_AUTOMT_CFG stuCfg = new NETDEV_AUTOMT_CFG();
		IntByReference nReturnLen = new IntByReference(0);
		if (!netsdkApi.CLIENT_GetDevConfig(m_loginHandle, NetSDKLib.NET_DEV_AUTOMTCFG, -1, 
				stuCfg.getPointer(), stuCfg.size(), nReturnLen, 5000)) {
			System.out.println("getAutoRecordCyc Failed!" + ToolKits.getErrorCode());
			return null;
		}
		
		stuCfg.read();
		return stuCfg;
    }
    
    /**
   	 * 自动录像保存周期设置
   	 * @param stuCfg 自动维护配置
   	 */
    public boolean setAutoRecordCyc(NETDEV_AUTOMT_CFG stuCfg) {
    	stuCfg.write();
		if (!netsdkApi.CLIENT_SetDevConfig(m_loginHandle, NetSDKLib.NET_DEV_AUTOMTCFG, -1, 
				stuCfg.getPointer(), stuCfg.size(), 5000)) {
			System.out.println("setAutoRecordCyc Failed!" + ToolKits.getErrorCode());
			return false;
		}else {
			System.out.println("setAutoRecordCyc Success!");
		}
		
		return true;
	}
    
	/*************************************************************************************
	*								Download Record Implement							 * 
	*************************************************************************************/
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
	 *  下载录像
	 */
	public void downloadRecordFile() {
		
		int nStreamType = 0;		// 0-主辅码流,1-主码流,2-辅码流
		int nChannel = 0; 			// 通道号
		NET_TIME stTimeStart = new NET_TIME(); 	// 开始时间
		NET_TIME stTimeEnd = new NET_TIME(); 	// 结束时间
        
		Calendar calendar = Calendar.getInstance();

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("默认下载通道0主辅码流的最近10分钟录像");
		System.out.print("如果想要自己设置通道或时间段请输入1: ");		
		if (scanner.nextInt() == 1) {
			try {
				System.out.print("请输入通道号(从0开始): ");
				nChannel = scanner.nextInt();
				
				System.out.print("请输入开始时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse("2020-9-15 11:38:00"));
				setTime(calendar, stTimeStart);
				
				System.out.print("请输入结束时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse("2019-12-02 11:39:00"));
				setTime(calendar, stTimeEnd);
			} catch (ParseException e) {
				System.err.println("时间输入非法");
				return;
			}
			
		}else {
			calendar.setTime(new Date());
			setTime(calendar, stTimeEnd);
			
			calendar.add(Calendar.MINUTE, -10);
			setTime(calendar, stTimeStart);
		}

		String savedFileName = "dowload_" + System.currentTimeMillis() + ".mp4"; // 保存录像文件名
		
		downloadRecordFile(nStreamType, nChannel, stTimeStart, stTimeEnd, savedFileName);
	}
	
	/**
	 * 按时间标记录像
	 */
    public void markFileByTime() {
    	int nChannel = 0; 
    	NET_TIME_EX stTimeStart = new NET_TIME_EX();
    	NET_TIME_EX stTimeEnd = new NET_TIME_EX();
    	int nMark = 1;
    	Calendar calendar = Calendar.getInstance();

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("默认对通道0前6个小时的录像加解锁");
		System.out.print("如果想要自己设置通道和时间段请输入1: ");		
		if (scanner.nextInt() == 1) {
			try {
				System.out.print("请输入标记类型(1加锁  0 解锁): ");
				nMark = scanner.nextInt();
				
				System.out.print("请输入通道号(从0开始, -1代表全通道): ");
				nChannel = scanner.nextInt();
				
				System.out.print("请输入开始时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse(scanner.next()));
				setTime(calendar, stTimeStart);

				System.out.print("请输入结束时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse(scanner.next()));
				setTime(calendar, stTimeEnd);
			} catch (ParseException e) {
				System.err.println("时间输入非法");
				return;
			}
		}else {
			System.out.print("请输入标记类型(1加锁  0 解锁): ");
			nMark = scanner.nextInt();
			
			calendar.setTime(new Date());
			setTime(calendar, stTimeEnd);
			
			calendar.add(Calendar.HOUR_OF_DAY, -6);
			setTime(calendar, stTimeStart);
		}

    	markFileByTime(nChannel, stTimeStart, stTimeEnd, nMark);
    }
    
    /**
	 *  获取某段时间内的录像加锁文件
	 */
    public void queryMarkFile() {
    	int nChannel = 0; 
    	NET_TIME stTimeStart = new NET_TIME();
    	NET_TIME stTimeEnd = new NET_TIME();
       	Calendar calendar = Calendar.getInstance();

    	@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
    	System.out.println("默认查询通道0前6个小时的加锁录像");
		System.out.print("如果想要自己设置通道和时间段请输入1: ");		
		if (scanner.nextInt() == 1) {
			try {
				System.out.print("请输入通道号(从0开始, -1代表全通道): ");
				nChannel = scanner.nextInt();
				
				System.out.print("请输入开始时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse(scanner.next()));
				setTime(calendar, stTimeStart);

				System.out.print("请输入结束时间(格式:yyyy-MM-dd HH:mm:ss): ");
				calendar.setTime(format.parse(scanner.next()));
				setTime(calendar, stTimeEnd);
			} catch (ParseException e) {
				System.err.println("时间输入非法");
				return;
			}
		}else {
			calendar.setTime(new Date());
			setTime(calendar, stTimeEnd);
			
			calendar.add(Calendar.HOUR_OF_DAY, -6);
			setTime(calendar, stTimeStart);
		}
    	
    	queryMarkFile(nChannel, stTimeStart, stTimeEnd);
    }
    
    /**
	 *  按文件录像解锁
	 */
    public void UnlockRecordByFile() {
    	if (lstLockFile.isEmpty()) {
    		queryMarkFile();
    	}
    	
    	for (LockFileInfo stFileInfo : lstLockFile) {
    		UnlockRecordByFile(stFileInfo);
    	}
    	
    	lstLockFile.clear();
    }
    
    private void setTime(Calendar calendar, NET_TIME_EX stuTime) {
    	stuTime.dwYear = calendar.get(Calendar.YEAR);
    	stuTime.dwMonth = calendar.get(Calendar.MONTH) + 1;
    	stuTime.dwDay = calendar.get(Calendar.DAY_OF_MONTH);
    	stuTime.dwHour = calendar.get(Calendar.HOUR_OF_DAY);
    	stuTime.dwMinute = calendar.get(Calendar.MINUTE);
    	stuTime.dwSecond = calendar.get(Calendar.SECOND);
    }
    
    private void setTime(Calendar calendar, NET_TIME stuTime) {
    	stuTime.setTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
				calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }
    
    /**
  	 *  查询设备状态
  	 */
    public void queryDeviceStatus() {
    	
    	System.out.println("-------------------设备在线状态-------------------");
    	queryOnlineState();
    	
    	System.out.println("-------------------硬盘状态-------------------");
    	queryHardDiskState(); // 查询硬盘状态
    	
    	System.out.println("-------------------Raid状态-------------------");
    	queryRaidStatue(); // 查询Raid状态
    	
    	System.out.println("-------------------摄像机状态-------------------");
    	queryCameraState(); // 获取摄像机状态
    }
	 public void GetIVSSDisk() {
		 	NET_IN_STORAGE_DEV_INFOS inInfo = new NET_IN_STORAGE_DEV_INFOS();
			inInfo.emVolumeType = NET_VOLUME_TYPE.VOLUME_TYPE_ALL; // 单盘可以查询支持的工作组
			NET_OUT_STORAGE_DEV_INFOS outInfo = new NET_OUT_STORAGE_DEV_INFOS();
			for (int i = 0; i < outInfo.stuStoregeDevInfos.length; i ++) 
			{
				outInfo.stuStoregeDevInfos[i].dwSize = outInfo.stuStoregeDevInfos[0].dwSize;
			}
			inInfo.write();
			outInfo.write();
			boolean bRet = netsdkApi.CLIENT_QueryDevInfo(m_loginHandle, NetSDKLib.NET_QUERY_DEV_STORAGE_INFOS, inInfo.getPointer(), outInfo.getPointer(), null, 5000);
			inInfo.read();
			outInfo.read();
			if (!bRet) {
				System.err.println("CLIENT_QueryDevInfo Failed!" + ToolKits.getErrorCode());
				return;
			}
			for (int i = 0; i < outInfo.nDevInfosNum; i ++) 
			{
				NET_STORAGE_DEVICE device = outInfo.stuStoregeDevInfos[i];
				System.out.println(device.nPhysicNo); // 槽位
				System.out.println(new String(device.szName).trim());    // 名称
				System.out.println(device.nTotalSpace); 
				System.out.println(device.nFreeSpace);
				System.out.println(new String(device.szModule).trim()); // 型号
				String valume[] = {"0-物理卷", "1-Raid卷", "2-VG虚拟卷", "3-ISCSI", "4-独立物理卷", "5-全局热备卷", "6-NAS卷(包括FTP, SAMBA, NFS)", "7-独立RAID卷（指没有加入到，虚拟卷组等组中）"};
				System.out.println(valume[device.byVolume]); // 
				if (device.byVolume == 1) {
					System.out.println("Raid Stat: " + device.stuRaid.nState);					
				} else { // 单盘问题
					 String arState[] = 
			        {
			            "Offline-物理硬盘脱机状态", "Running-物理硬盘运行状态", "Active-RAID活动", "Sync-RAID同步", "Spare-RAID热备(局部)", 
			            "Faulty-RAID失效", "Rebuilding-RAID重建", "Removed-RAID移除", "WriteError-RAID写错误", "WantReplacement-RAID需要被替换", 
			            "Replacement-RAID是替代设备", "GlobalSpare-全局热备", "Error-一般错误，部分分区可用", "RaidSub-该盘目前是单盘，原先是块Raid子盘,有可能在重启后自动加入Raid", 
			        };
			        if ((int)device.byState != NetFinalVar.NET_STORAGE_DEV_RUNNING) {
			        	System.out.println(arState[(int)device.byState]); // 状态
			        	continue;
			        }
			        if (device.emPreDiskCheck != EM_STORAGE_DISK_PREDISKCHECK.EM_STORAGE_DISK_PREDISKCHECK_UNKNOWN) {
			        	System.out.println("device.emPreDiskCheck " + device.emPreDiskCheck);
			        }
				} 		       
			}
	 }
	
   /**
	* 自动录像保存周期设置
	*/
    public void setAutoRecordCyc() {
    	// 先获取再配置
    	NETDEV_AUTOMT_CFG stuCfg = getAutoRecordCyc();
    	if (stuCfg != null) {
    		System.out.println("设置前自动录像保存天数(0：从不删除, 1：24H,2：48H,3：72H,4：96H,...):" + stuCfg.byAutoDeleteFilesTime);
    		@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
    		System.out.print("请输入自动录像保存天数(0：从不删除, 1：24H,2：48H,3：72H,4：96H,...):");
    		stuCfg.byAutoDeleteFilesTime = scanner.nextByte();
    		setAutoRecordCyc(stuCfg);
    	}		
	}
	
	/**
     * 自动录像保存周期设置扩展 新设备支持
     */
    public void setAutoRecordCycEx() {
    	for (int i = 0; i < deviceinfo.byChanNum; i ++) {
    		NET_CFG_FILE_HOLD_DAYS_INFO cfg =  new NET_CFG_FILE_HOLD_DAYS_INFO();
    		cfg.write();
    		boolean bGet = netsdkApi.CLIENT_GetConfig(m_loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FILE_HOLD_DAYS, i, cfg.getPointer(), cfg.size(), 5000, null);
    		cfg.read();
    		if (!bGet) {
        		System.err.println("Failed to Get. Channel:" + i + " " + ToolKits.getErrorCode());
        	} else {
        		System.out.println("Channel: " + i +";nDays: " + cfg.nDays);
        		cfg.nDays += 1;
        	}
    		
    		cfg.write();
    		boolean bSet = netsdkApi.CLIENT_SetConfig(m_loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FILE_HOLD_DAYS, i, cfg.getPointer(), cfg.size(), 5000, null,null);
    		cfg.read();
    		if (!bSet) {
        		System.err.println("Failed to Set. Channel:" + i + " " + ToolKits.getErrorCode());
        	}
    	}
    }
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			= "172.23.12.14";  
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin11111";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "下载录像" , "downloadRecordFile"));
		menu.addItem(new CaseMenu.Item(this , "停止下载录像" , "stopDownLoadRecordFile"));
		
		menu.addItem(new CaseMenu.Item(this , "按时间对录像加解锁" , "markFileByTime"));
		menu.addItem(new CaseMenu.Item(this , "按时间查询加锁文件" , "queryMarkFile"));
		menu.addItem(new CaseMenu.Item(this , "按文件录像解锁" , "UnlockRecordByFile"));

		menu.addItem(new CaseMenu.Item(this , "报警监听" , "startListen"));
		menu.addItem(new CaseMenu.Item(this , "停止报警监听" , "stopListen"));
		menu.addItem(new CaseMenu.Item(this , "查询设备状态" , "queryDeviceStatus"));

		menu.addItem(new CaseMenu.Item(this , "获取raid状态" , "GetIVSSDisk"));
		menu.addItem(new CaseMenu.Item(this , "自动录像保存周期设置" , "setAutoRecordCycEx"));
		
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		DownloadRecord demo = new DownloadRecord();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
