package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * MointorWall
 */
public class MonitorWallEng {

	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private LLong loginHandle = new LLong(0);   // login handle
	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); 
	
	private Map<Integer, DecChannel> mapChannel = new HashMap<Integer, DecChannel>();
	private Map<String, ArrayList<NET_SPLIT_SOURCE>> mapSplitSource = new HashMap<String, ArrayList<NET_SPLIT_SOURCE>>();

	/*************************************************************************************
	*								General function								 	 * 
	*************************************************************************************/
	// device disconnect callback
	// call CLIENT_Init to set it, when device reconnect, sdk will call it.
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
		
	// device reconnect callback
	// call CLIENT_SetAutoReconnect to set it, when device reconnect, sdk will call it.
	public class HaveReConnect implements fHaveReConnect {
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
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);
    	
		// Set re-connection callback function after disconnection. Internal SDK auto connect again after disconnection (Optional)
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null); 
		
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
    	loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		EndTest();
    	}
	}
	
	/**
	 * Logout Device And Cleanup Sdk 
	 */
	public void EndTest()
	{
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}
	
	/**
     * Operate Type
     * */
	public enum OPERATE_TYPE {
		SET_SPLIT_SOURCE,				// set or replace split source
		REMOVE_SPLIT_SOURCE,			// remove split source
	};
	
	/**
	 * Get Matrix Cameras Info，Using By Set Split Source
	 */
	public boolean GetMatrixCamerasInfo() {	
		
		mapSplitSource.clear();
		
		int cameraCount = 512;	

		NET_MATRIX_CAMERA_INFO[]  cameraInfo = new NET_MATRIX_CAMERA_INFO[cameraCount];
		for(int i = 0; i < cameraCount; i++) {
			cameraInfo[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		NET_IN_MATRIX_GET_CAMERAS inMatrix = new NET_IN_MATRIX_GET_CAMERAS();
		
		NET_OUT_MATRIX_GET_CAMERAS outMatrix = new NET_OUT_MATRIX_GET_CAMERAS();
		outMatrix.nMaxCameraCount = cameraCount;
		outMatrix.pstuCameras = new Memory(cameraInfo[0].size() * cameraCount);
		outMatrix.pstuCameras.clear(cameraInfo[0].size() * cameraCount);
		
		ToolKits.SetStructArrToPointerData(cameraInfo, outMatrix.pstuCameras);  // write array memory to Pointer
		if (!netsdkApi.CLIENT_MatrixGetCameras(loginHandle, inMatrix, outMatrix, 5000)) {
			System.err.println("MatrixGetCameras Failed." + ToolKits.getErrorCode());
			return false;
		}

		ToolKits.GetPointerDataToStructArr(outMatrix.pstuCameras, cameraInfo);  // read Pointer's data to array
		
		for (int j = 0; j < outMatrix.nRetCameraCount; j++) {
			if(cameraInfo[j].bRemoteDevice == 0) {   // filter remote device
				continue;
			}
			
			String ip = new String(cameraInfo[j].stuRemoteDevice.szIp).trim();
			ArrayList<NET_SPLIT_SOURCE> lstSplitSource = mapSplitSource.get(ip);
			if (lstSplitSource != null) {
				lstSplitSource.add(convertMatrixCamera(cameraInfo[j]));
			}else {
				lstSplitSource = new ArrayList<NET_SPLIT_SOURCE>();
				lstSplitSource.add(convertMatrixCamera(cameraInfo[j]));
				mapSplitSource.put(ip, lstSplitSource);
			}
		}
		
		return true;
	}
	
	
	/**
	 * Convert Matrix Camera Info to Split Source
	 */
	public NET_SPLIT_SOURCE convertMatrixCamera(NET_MATRIX_CAMERA_INFO cameraInfo) {
		NET_SPLIT_SOURCE stSplitSource = new NET_SPLIT_SOURCE();
				
		stSplitSource.bEnable = 1; // enable
		
		System.arraycopy(cameraInfo.stuRemoteDevice.szIp, 0, stSplitSource.szIp, 0, cameraInfo.stuRemoteDevice.szIp.length); // ip
		
		stSplitSource.nPort = cameraInfo.stuRemoteDevice.nPort; // port
		
		System.arraycopy(cameraInfo.stuRemoteDevice.szUserEx, 0, stSplitSource.szUserEx, 0, cameraInfo.stuRemoteDevice.szUserEx.length); // user

		System.arraycopy(cameraInfo.stuRemoteDevice.szPwdEx, 0, stSplitSource.szPwdEx, 0, cameraInfo.stuRemoteDevice.szPwdEx.length); // password
								
		stSplitSource.nVideoChannel = cameraInfo.stuRemoteDevice.nVideoInputChannels;  // video input channels
		
		stSplitSource.nChannelID = cameraInfo.nUniqueChannel;  // channel id

		stSplitSource.nStreamType = cameraInfo.emStreamType - 1; // stream type
		
		stSplitSource.byConnType = 0; // connect type -1: auto, 0: TCP, 1: UDP, 2: multicast
		
		stSplitSource.dwRtspPort = cameraInfo.stuRemoteDevice.nRtspPort; // Rtsp port
				
		stSplitSource.emProtocol = NET_DEVICE_PROTOCOL.NET_PROTOCOL_PRIVATE2;  // Protocol type
		
		return stSplitSource;
	}
	
	/**
	 * Video Out Channel Class
	 * using as data, all fields are public  
	 */
	class DecChannel
	{
		public int						nChannelID;					// channel id
		public int						nCardID;					// card id
		public int						nCardSlot;					// card slot
		public boolean					bSpliceScreen;				// splice screem or not (composite channel's part)
		public boolean					bComposite;					// composite channel or not
		public String					monitorWallName;			// composite name
		
		public DecChannel(int nChannelID, int nCardID, int nCardSlot) {
			this.nChannelID = nChannelID;
			this.nCardID = nCardID;
			this.nCardSlot = nCardSlot;
			this.bSpliceScreen = false;
			this.bComposite = false;
			this.monitorWallName = null;
		}
	}
	
	/**
	 * Get Physical Channel Info
	 */
	public boolean GetPhysicalChannelInfo() {
		
 		NET_MATRIX_CARD_LIST pstuCardList = new NET_MATRIX_CARD_LIST();
 		if (! netsdkApi.CLIENT_QueryMatrixCardInfo(loginHandle, pstuCardList, 5000)) {
 			System.err.println("GetPhysicalChannelInfo failed!" + ToolKits.getErrorCode());
 			return false;
 		}
 		
 		NET_MATRIX_CARD stuCard;
		for (int i = 0; i < pstuCardList.nCount; i++) {
			stuCard = pstuCardList.stuCards[i];
			
			if(stuCard.bEnable == 0) {   // skip disable cards
				continue;
			}
			
			if ((stuCard.dwCardType & NetSDKLib.NET_MATRIX_CARD_OUTPUT) 
					== NetSDKLib.NET_MATRIX_CARD_OUTPUT && stuCard.nVideoOutChn > 0) { // output card
				for (int j = stuCard.nVideoOutChnMin; j <= stuCard.nVideoOutChnMax; ++j){
					DecChannel decChannel = new DecChannel(j, i, j - stuCard.nVideoOutChnMin); 
					mapChannel.put(j, decChannel);
				}
			}else if ((stuCard.dwCardType & NetSDKLib.NET_MATRIX_CARD_DECODE) 
					== NetSDKLib.NET_MATRIX_CARD_DECODE && stuCard.nVideoDecChn > 0) { // decode card
				
				for (int j = stuCard.nVideoDecChnMin; j <= stuCard.nVideoDecChnMax; ++j) {
					DecChannel decChannel = new DecChannel(j, i, j - stuCard.nVideoDecChnMin);
					mapChannel.put(j, decChannel);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Get Composite Channel Info
	 */
	public boolean GetCompositeChannelInfo() {

		int nComposite = 256; // max splicing screen number
		NET_COMPOSITE_CHANNEL[] compositeChn = new NET_COMPOSITE_CHANNEL[nComposite];
		for(int i = 0; i <nComposite; i++) {
			compositeChn[i] = new NET_COMPOSITE_CHANNEL();
		}
		
		int memorySize = nComposite * compositeChn[0].size();
		Memory memory = new Memory(memorySize);
		memory.clear(memorySize);
		
		ToolKits.SetStructArrToPointerData(compositeChn, memory);
		
		IntByReference intRetLen = new IntByReference();
		if (!netsdkApi.CLIENT_QueryDevState(loginHandle, NetSDKLib.NET_DEVSTATE_COMPOSITE_CHN, 
				memory, memorySize, intRetLen, 3000)) {
			System.err.println("GetCompositeChannelInfo failed!" + ToolKits.getErrorCode());
 			return false;
		}
			
		int nSpliceCount = intRetLen.getValue() / compositeChn[0].size();   // splicing screen number

		if(nSpliceCount > nComposite) {
			nSpliceCount = nComposite;
		}
		
		ToolKits.GetPointerDataToStructArr(memory, compositeChn);

		for (int i = 0; i < nSpliceCount; ++i)
		{	
			if (compositeChn[i].szCompositeID[0] == '\0') {
				continue;
			}
			
			if (!mapChannel.containsKey(compositeChn[i].nVirtualChannel)) { // composite channel
				DecChannel decChannel = new DecChannel(compositeChn[i].nVirtualChannel, -1, -1); 
				decChannel.bComposite = true;
				decChannel.monitorWallName = new String(compositeChn[i].szMonitorWallName).trim();
				mapChannel.put(compositeChn[i].nVirtualChannel, decChannel);
			}else {
				System.err.println("get composite channel " + compositeChn[i].nVirtualChannel+ " again.");
			}
		}
		
		return true;
	}
	
	/**
	 * Get Monitor Wall Configure
	 */
	public boolean GetMonitorWallConfig() {
		
		int nMaxMonitorWall = 10;   // max monitor wall count (the struct is too large for you to create more at a time)
				
		AV_CFG_MonitorWall[] monitorWall = new AV_CFG_MonitorWall[nMaxMonitorWall];
		for (int i = 0; i < nMaxMonitorWall; i++) {
			monitorWall[i] = new AV_CFG_MonitorWall();
		}
		
		int nRetCount = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_MONITORWALL, monitorWall);
		if (nRetCount == -1) {
			System.err.println("GetMonitorWallConfig failed!" + ToolKits.getErrorCode());
 			return false;
		}
		
		if (nRetCount > nMaxMonitorWall) {
			nRetCount = nMaxMonitorWall;
		}
		
		for(int i = 0; i < nRetCount; i++) {
			for(int j = 0; j < monitorWall[i].nBlockCount; j++) {
				for(int k = 0; k < monitorWall[i].stuBlocks[j].nTVCount; k++) {			
					DecChannel devChannel = mapChannel.get(monitorWall[i].stuBlocks[j].stuTVs[k].nChannelID);
					if (devChannel != null){
						devChannel.bSpliceScreen = true; 
					}else { // the normal code will never to here
						System.err.println("Had config channel " + monitorWall[i].stuBlocks[j].stuTVs[k].nChannelID + ", but can't find it in channel map.");
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Get Video Out Channel Info
	 */
	public void  GetVideoOutChannelInfo() {
		
		mapSplitSource.clear();
		
		System.out.println("Get Physical Channel Info...");
		GetPhysicalChannelInfo();
		System.out.println("Get Composite Channel Info...");
		GetCompositeChannelInfo();
		System.out.println("Get Monitor Wall Config...");
		GetMonitorWallConfig();
	}
	
	/**
	 * Get Out Channel's Window Count
	 */
	public int GetWhnCount(int nOutChannel) {
		
		int nWindow = 0;

		NET_SPLIT_MODE_INFO splitMode = new NET_SPLIT_MODE_INFO();

		if (!netsdkApi.CLIENT_GetSplitMode(loginHandle, nOutChannel, splitMode, 3000)) {
			System.err.println("GetWhnCount [GetSplitMode] Filed" + ToolKits.getErrorCode());
			return nWindow;
		}
		
		if(splitMode.emSplitMode == NET_SPLIT_MODE.NET_SPLIT_FREE) { // free open window mode

			NET_IN_SPLIT_GET_WINDOWS stuInGetWhn = new NET_IN_SPLIT_GET_WINDOWS();
			stuInGetWhn.nChannel = nOutChannel;

			NET_OUT_SPLIT_GET_WINDOWS stuOutGetWhn = new NET_OUT_SPLIT_GET_WINDOWS();
			
			if (netsdkApi.CLIENT_GetSplitWindowsInfo(loginHandle, stuInGetWhn, stuOutGetWhn, 3000)) {
				
				nWindow = stuOutGetWhn.stuWindows.nWndsCount;

				/*for (int i = 0; i < stuOutGetWhn.stuWindows.nRetWndsCountEx; ++i) {
					DH_RECT stuRect =stuOutGetWhn.stuWindows.stuWnds[i].stuRect;
					if ((stuRect.left.longValue() == 0) && (stuRect.top.longValue() == 0) 
							&& (stuRect.right.longValue() == 0) && (stuRect.bottom.longValue() == 0)) { 
						// this window not open
					}
				}*/
			}
		} else {
			nWindow = splitMode.emSplitMode;
		}
		
		return nWindow;
	}
	
	
	/**
	 * Open Split Window
	 */
	public void OpenSplitWindow() {
			
		Scanner scanner = new Scanner(System.in);
							
		ShowVideoOutChannelInfo();

		System.out.print("Input Out Channel");
		if (mapChannel.isEmpty()) {
			System.out.print(": ");
		}else {
			System.out.print(" From Matrix Channel List: ");
		}
		int nChannel = scanner.nextInt();
		
		OpenSplitWindow(nChannel, scanner);
	}
	
	public int OpenSplitWindow(int nChannel, Scanner scanner) {
							
		NET_IN_SPLIT_OPEN_WINDOW stIn = new NET_IN_SPLIT_OPEN_WINDOW();
		
		stIn.nChannel = nChannel;
		
		System.out.print("Input Windon Position, 0~8192 (left top right bottom), eg 0 0 8192 8192: ");
		// Windon Position, 0~8192
		stIn.stuRect.left = new NativeLong(scanner.nextInt());
		stIn.stuRect.top = new NativeLong(scanner.nextInt());
		stIn.stuRect.right = new NativeLong(scanner.nextInt());
		stIn.stuRect.bottom = new NativeLong(scanner.nextInt());
					
		NET_OUT_SPLIT_OPEN_WINDOW stOut = new NET_OUT_SPLIT_OPEN_WINDOW();
		
		if(netsdkApi.CLIENT_OpenSplitWindow(loginHandle, stIn, stOut, 3000)) {
			System.out.println("Out Channel " + stIn.nChannel + " Open Split Window Success！ Window ID:" + stOut.nWindowID);
		} else {
			System.err.println("Out Channel " + stIn.nChannel + " Open Split Window Failed！ " + ToolKits.getErrorCode());
			return -1;
		}
		
		return stOut.nWindowID;
	}
	
	/**
	 * Close Split Window
	 */
	public void CloseSplitWindow() {
		
		Scanner scanner = new Scanner(System.in);
		
		NET_IN_SPLIT_CLOSE_WINDOW stIn = new NET_IN_SPLIT_CLOSE_WINDOW();

		ShowVideoOutChannelInfo();
		
		System.out.print("Input Out Channel");
		if (mapChannel.isEmpty()) {
			System.out.print(": ");
		}else {
			System.out.print(" From Matrix Channel List: ");
		}
		stIn.nChannel = scanner.nextInt();
		
		int nWindowCount = GetWhnCount(stIn.nChannel); // you should know window status first
		if (nWindowCount !=0) {
			System.out.print("Input Window Number Range[0~" + nWindowCount + "): ");
			stIn.nWindowID = scanner.nextInt();
		}else {
			System.out.print("Input Window Number(Start 0): ");
			stIn.nWindowID = scanner.nextInt();
		}
		
		stIn.pszCompositeID = "";				 // Composite ID

		NET_OUT_SPLIT_CLOSE_WINDOW stOut = new NET_OUT_SPLIT_CLOSE_WINDOW();
		
		if(netsdkApi.CLIENT_CloseSplitWindow(loginHandle, stIn, stOut, 3000)) {
			System.out.println("Out Channel " + stIn.nChannel + " Window " + stIn.nWindowID + " Close Split Window Success！");
		} else {
			System.err.println("Out Channel " + stIn.nChannel + " Window " + stIn.nWindowID + " Close Split Window Failed！ " + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * Show Matrix Camera Info
	 */
	public void ShowMatrixCamerasInfo() {
		if (GetMatrixCamerasInfo()) {
			System.out.println("---------------------------------------------------------");
			for (String ip : mapSplitSource.keySet()) {
				System.out.print(ip + " ");
			}
			System.out.println();
			System.out.println("---------------------------------------------------------");
		}
	}
	
	/**
	 * Show Video Out Channel Info
	 */
	public void ShowVideoOutChannelInfo() {
		
		if (mapChannel.isEmpty()) {
			GetVideoOutChannelInfo();
		}
		
		if (mapChannel.isEmpty()) {
			return;
		}
		
		System.out.println("Matrix Channel List: ");
		for (DecChannel decChannel : mapChannel.values()) {
			if (!decChannel.bComposite) {
				if (!decChannel.bSpliceScreen) {
					System.out.print(decChannel.nChannelID + " ");
				}else {
					System.out.print(decChannel.nChannelID + "(TV) ");
				}
			}else {
				System.out.print(decChannel.nChannelID + "(" + decChannel.monitorWallName + ") ");
			}
		}
		System.out.println();
	}
	
	/**
	 * Set Split Source
	 */
	public void SetSplitSource() {
		
		Scanner scanner = new Scanner(System.in);
		NET_SPLIT_SOURCE splitSource = new NET_SPLIT_SOURCE();
		
		// Step 1: Select Video Out Channel
		ShowVideoOutChannelInfo(); 
		System.out.print("Input Out Channel");
		if (mapChannel.isEmpty()) {
			System.out.print(": ");
		}else {
			System.out.print(" From Matrix Channel List: ");
		}
		int nOutChannel = scanner.nextInt();
					
		// Step 2: Select Window Number
		int nWindow = 0;
		int nWindowCount = GetWhnCount(nOutChannel);
		if (nWindowCount !=0) {
			System.out.print("Input Window Number Range[0~" + nWindowCount + "): ");
			nWindow = scanner.nextInt();
		}else {
			System.out.println("No Windows, Need Open Split Window.");
			nWindow = OpenSplitWindow(nOutChannel, scanner);
			if (nWindow == -1) {
				return;
			}
		}
			
		// Step 3: Select Matrix Cameras
		GetMatrixCamerasInfo(); // TODO if you want input device info by yourself just don't call this function
		if (mapSplitSource.isEmpty()) {
			
			splitSource.bEnable = 1;  // enable
			
			System.out.print("Input Device IP: ");
			String ip = scanner.next();
			System.arraycopy(ip.getBytes(), 0, splitSource.szIp, 0, ip.getBytes().length);
			
			System.out.print("Input Device Port: ");
			splitSource.nPort = scanner.nextInt();
			
			System.out.print("Input Device User: ");
			String user = scanner.next();
			System.arraycopy(user.getBytes(), 0, splitSource.szUserEx, 0, user.getBytes().length);
			
			System.out.print("Input Device Password: ");
			String pwd = scanner.next();
			System.arraycopy(pwd.getBytes(), 0, splitSource.szPwdEx, 0, pwd.getBytes().length);
			
			System.out.print("Input Device Channel(Start 0): ");
			splitSource.nChannelID = scanner.nextInt();

			System.out.print("Input Device Video Channel Count: ");
			splitSource.nVideoChannel = scanner.nextInt();
			
			splitSource.nStreamType = 0;

			splitSource.byConnType = 0;

			splitSource.emProtocol = NET_DEVICE_PROTOCOL.NET_PROTOCOL_PRIVATE2;  // default
		
		}else {
			
			System.out.println("Ip List: ");
			for (String ip : mapSplitSource.keySet()) {
				System.out.print(ip + " ");
			}
			System.out.println();
			
			System.out.print("Input Ip From Ip List: ");
			String ip = scanner.next();
			
			ArrayList<NET_SPLIT_SOURCE> lstSplitSource = mapSplitSource.get(ip);
			if (lstSplitSource == null) {
				System.out.println("IP Incorrect...");
				return;
			}
			
			if (lstSplitSource.size() > 1) {
				System.out.println("Channel List: ");
				for (NET_SPLIT_SOURCE itemSource : lstSplitSource) {
					System.out.print(itemSource.nChannelID + " ");
				}
				System.out.println();
				System.out.print("Input Channel ID From Channel List: ");
				int nChannelID = scanner.nextInt();
				
				for (NET_SPLIT_SOURCE itemSource : lstSplitSource) {
					if (itemSource.nChannelID == nChannelID) {
						splitSource = itemSource;
						break;
					}
				}
				
				if (splitSource.bEnable != 1) {
					System.out.println("Channel ID Incorrect...");
				}
			}else {
				splitSource = lstSplitSource.get(0);
			}
			
			if (splitSource.szUserEx[0] == '\0') {
				System.out.print("Input Device User: ");
				String user = scanner.next();
				System.arraycopy(user.getBytes(), 0, splitSource.szUserEx, 0, user.getBytes().length);
			}
			
			if (splitSource.szPwdEx[0] == '\0') {
				System.out.print("Input Device Password: ");
				String pwd = scanner.next();
				System.arraycopy(pwd.getBytes(), 0, splitSource.szPwdEx, 0, pwd.getBytes().length);
			}
		}
		
		// Step 4: Set Split Source
		int nSrcCount = 1; // Split Source number (Fixed to 1)
		if (netsdkApi.CLIENT_SetSplitSource(loginHandle, nOutChannel, nWindow, splitSource, nSrcCount, 3000)) {
			System.out.println("Set Or Replace Split Source Success!");
		} else {
			System.err.println("Set Or Replace Split Source Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	public void RemoveSplitSource() {
		
		Scanner scanner = new Scanner(System.in);
		
		// Step 1: Input Video Out Channel
		ShowVideoOutChannelInfo(); 
		System.out.print("Input Out Channel");
		if (mapChannel.isEmpty()) {
			System.out.print(": ");
		}else {
			System.out.print(" From Matrix Channel List: ");
		}
		int nOutChannel = scanner.nextInt();

		// Step 2: Select Window Number
		int nWindow = 0;
		int nWindowCount = GetWhnCount(nOutChannel);
		if (nWindowCount !=0) {
			System.out.print("Input Window Number Range[0~" + nWindowCount + "): ");
			nWindow = scanner.nextInt();
		}else {
			System.out.print("Input Window Number(Start 0): ");
			nWindow = scanner.nextInt();
		}
		
		// Step 3: Set Split Source
		int nSrcCount = 1; // Split Source number (Fixed to 1)
		NET_SPLIT_SOURCE splitSource = new NET_SPLIT_SOURCE();

		if (netsdkApi.CLIENT_SetSplitSource(loginHandle, nOutChannel, nWindow, splitSource, nSrcCount, 3000)) {
			System.out.println("Remove Split Source Success!");
		} else {
			System.err.println("Remove Split Source Failed!" + ToolKits.getErrorCode());
		}
	}
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			= "171.2.3.116";  
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "Show Matrix Cameras Info" , "ShowMatrixCamerasInfo"));
		menu.addItem(new CaseMenu.Item(this , "Show Video Out Channel Info" , "ShowVideoOutChannelInfo"));
		menu.addItem(new CaseMenu.Item(this , "Set Or Replace Split Source" , "SetSplitSource"));
		menu.addItem(new CaseMenu.Item(this , "Remove Split Source" , "RemoveSplitSource"));
		menu.addItem(new CaseMenu.Item(this , "Open Split Window" , "OpenSplitWindow"));
		menu.addItem(new CaseMenu.Item(this , "Close Split Window" , "CloseSplitWindow"));
		
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		MonitorWallEng demo = new MonitorWallEng();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}

}
