package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * 智能交通： 交通流量统计
 * @author 29779
 *
 */
public class TrafficFlowStatistic {
	
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO deviceInfo = new NET_DEVICEINFO();
	private LLong loginHandle = new LLong(0);

	String m_strIp 			= "192.168.1.53";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	
	
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	public class fAnalyzerDataCB implements fAnalyzerDataCallBack{
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved){
			switch(dwAlarmType )
			{
				case NetSDKLib.EVENT_IVS_TRAFFIC_FLOWSTATE:
					DEV_EVENT_TRAFFIC_FLOW_STATE stFlowState = new DEV_EVENT_TRAFFIC_FLOW_STATE();
					ToolKits.GetPointerData(pAlarmInfo , stFlowState);
					for(int i =0 ; i< stFlowState.nStateNum ; i++)
		            {
		                System.out.printf("Lane[%d] Flow[%d] Period[%d]\n" , 
	                		stFlowState.stuStates[i].nLane , 
	                		stFlowState.stuStates[i].dwFlow , 
	                		stFlowState.stuStates[i].dwPeriod );
		            }
					System.out.printf("\n");
					break;
				default:
					System.out.printf("Get Event %x\n" , dwAlarmType);
					break;
			}
			return 0;
		}

	}

	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB();
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{
		//初始化SDK库
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);
    	
    	// 向设备登入
    	int nSpecCap = 0;
    	IntByReference error = new IntByReference();
		loginHandle = netsdkApi.CLIENT_LoginEx(m_strIp, (short)m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,null, deviceInfo,error);
		
		if(loginHandle.longValue() != 0)
		{
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else
    	{	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		EndTest();
    	}
	}
	
	public void attachEvent() {
		int bNeedPicture = 0;
		LLong hAttachHandle = new LLong(0);
		
		for( int i=0 ; i < deviceInfo.union.byChanNum ; i++)
		{
			hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, i ,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , m_AnalyzerDataCB , null , null);
			if( hAttachHandle.longValue() != 0  )
			{
				System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n" , i);
			}
			else
			{
				System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %x\n" , i , netsdkApi.CLIENT_GetLastError() );
				return;
			}
		}
	}
		
	public void findTrafficFlowState() {
		// 设置查询条件
		FIND_RECORD_TRAFFICFLOW_CONDITION condition = new FIND_RECORD_TRAFFICFLOW_CONDITION();
		condition.bStartTime = 1; // 使能
		condition.bEndTime = 1; // 使能
		
		condition.stStartTime.setTime(2017, 12, 8, 18, 0, 0);
		condition.stEndTime.setTime(2017, 12, 8, 19, 0, 0);
		
		// CLIENT_FindRecord 入参
		NET_IN_FIND_RECORD_PARAM findRecordIn = new NET_IN_FIND_RECORD_PARAM();
		findRecordIn.emType = EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICFLOW_STATE;
		findRecordIn.pQueryCondition = condition.getPointer();
		
		// CLIENT_FindRecord 出参
		NET_OUT_FIND_RECORD_PARAM findRecordOut = new NET_OUT_FIND_RECORD_PARAM();
		
		condition.write(); 
		findRecordIn.write();
		findRecordOut.write();
		boolean bRet = netsdkApi.CLIENT_FindRecord(loginHandle, findRecordIn, findRecordOut, 3000);
		findRecordOut.read();
		findRecordIn.read();
		condition.read();		
		if(!bRet) {
			System.err.println("Can Not Find This Record" + Integer.toHexString(netsdkApi.CLIENT_GetLastError()));
			return;
		}
		int nRecordCount = 10;
		while(true) {			
			NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = findRecordOut.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount;
			
			NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			NET_RECORD_TRAFFIC_FLOW_STATE pstRecordEx = new NET_RECORD_TRAFFIC_FLOW_STATE();
			stuFindNextOutParam.pRecordList = new Memory(pstRecordEx.dwSize * nRecordCount);   //分配(stRecordEx.dwSize * nRecordCount)个内存

			// 把内存里的dwSize赋值
			for (int i=0; i<stuFindNextOutParam.nMaxRecordNum; ++i)
			{
				ToolKits.SetStructDataToPointer(pstRecordEx, stuFindNextOutParam.pRecordList, i*pstRecordEx.dwSize);
			}
			
			pstRecordEx.write();
			boolean zRet = netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 3000);
			pstRecordEx.read();
			if (!zRet) {
				System.err.println("Can't FindNextRecord" + Integer.toHexString(netsdkApi.CLIENT_GetLastError()));
				break;
			}
			
			System.out.println("queryEx stuFindOutParam . dwSize " + findRecordOut.dwSize);
			System.out.println("queryEx stuFindOutParam . lFindHanlde " + findRecordOut.lFindeHandle);
			for(int i=0; i < stuFindNextOutParam.nRetRecordNum; i++) {
				ToolKits.GetPointerDataToStruct(stuFindNextOutParam.pRecordList, i*pstRecordEx.dwSize, pstRecordEx);
		        System.out.println("nRecordNum: " + pstRecordEx.nRecordNum 
		        			+ " nChannel: " + pstRecordEx.nChannel
		        			+ " nLane: " + pstRecordEx.nLane);
			}
			
			if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
				break;
			}
		}
		netsdkApi.CLIENT_FindRecordClose(findRecordOut.lFindeHandle);
	}
		
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "findTrafficFlowState", "findTrafficFlowState"));
		menu.addItem(new CaseMenu.Item(this, "attachEvent", "attachEvent"));
		menu.run();
	}

	public static void main(String[]args)
	{
		TrafficFlowStatistic demo = new TrafficFlowStatistic();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}

