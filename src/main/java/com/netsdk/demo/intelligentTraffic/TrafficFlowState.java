package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.DateChooserJButton;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


/**
 * 车流量查询界面demo
 */
class TrafficFlowStateFrame extends JFrame{
	private static final long serialVersionUID = 1L;
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.23.1.32";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin";
	
	//设备信息
	private NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	
	private LLong m_hLoginHandle = new LLong(0);   //登陆句柄
	
	private boolean bInit    = false;
	private boolean bLogopen = false;
	
	private DisConnect disConnect       = new DisConnect();    //设备断线通知回调
	private HaveReConnect haveReConnect = new HaveReConnect(); //网络连接恢复
		
	//设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class DisConnect implements NetSDKLib.fDisConnect {
		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
		}
	}
			
	//网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements NetSDKLib.fHaveReConnect {
		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}
	
	//初始化
	public boolean init() {
		
		bInit = NetSdk.CLIENT_Init(disConnect, null);
		if(!bInit) {
			System.out.println("Initialize SDK failed");
			return false;
		}
		
		//打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		File path = new File(".");		
		String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";
		
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy    = 0;
		bLogopen = NetSdk.CLIENT_LogOpen(setLog);
		if(!bLogopen ) {
			System.err.println("Failed to open NetSDK log");
		}
		
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);
	    
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		NetSdk.CLIENT_SetNetworkParam(netParam);	
		return true;
	}
	
	//清除环境
	public void cleanup() {
		if(bLogopen) {
			NetSdk.CLIENT_LogClose();
		}
		
		if(bInit) {
			NetSdk.CLIENT_Cleanup();
		}
	}
	
	public TrafficFlowStateFrame() {
		init();
	    setTitle("车流量查询");
	    setSize(800, 180);
	    setLayout(new BorderLayout());
	    setLocationRelativeTo(null);
	 
	    loginPanel = new LoginPanel();
	    trafficFlowPanel = new TrafficFlowPanel();
	    
	    add(loginPanel, BorderLayout.NORTH);
	    add(trafficFlowPanel, BorderLayout.CENTER);
	    
	    addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
	    		dispose();	
	    		SwingUtilities.invokeLater(new Runnable() {
	    			public void run() {
	    				logout();
	    				cleanup();
	    				System.exit(0);
	    			}
	    		});
	    	}
	    });
	}
	
	//设置边框
	private void setBorderEx(JComponent object, String title, int width) {
	    Border innerBorder = BorderFactory.createTitledBorder(title);
	    Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);
	    object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));	 
	}
	
	//登录面板
	private class LoginPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public LoginPanel() {
			setLayout(new FlowLayout());
		    setBorderEx(this, "登录", 2);
		    
			loginBtn = new JButton("登录");
			logoutBtn = new JButton("登出");
			nameLabel = new JLabel("用户名");
			passwordLabel = new JLabel("密码");
			nameTextArea = new JTextField(m_strUser, 6);
			passwordTextArea = new JPasswordField(m_strPassword, 6);
			ipLabel = new JLabel("设备地址");
			portLabel = new JLabel("端口号");
			ipTextArea = new JTextField(m_strIp, 8);
			portTextArea = new JTextField(m_nPort.toString(), 4);
    
		    add(ipLabel);
		    add(ipTextArea);
		    add(portLabel);
		    add(portTextArea);
		    add(nameLabel);
		    add(nameTextArea);
		    add(passwordLabel);
		    add(passwordTextArea);
		    add(loginBtn);
		    add(logoutBtn);
		    
		    logoutBtn.setEnabled(false);
		    
		    //登录按钮，监听事件
		    loginBtn.addActionListener(new ActionListener() {
		    	public void actionPerformed(ActionEvent e) {
		    		login();
		    	}
		    });
			
		    //登出按钮，监听事件
		    logoutBtn.addActionListener(new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		logout();
		    	}
		    });
		}
	}
	
	// 车流量查询
	private class TrafficFlowPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public TrafficFlowPanel() {
			setLayout(new FlowLayout());
		    setBorderEx(this, "车流量查询", 2);
		    
		    startTimeLabel = new JLabel("开始时间");
		    endTimeLabel = new JLabel("结束时间");
		    startTimeBtn = new DateChooserJButton();
		    endTimeBtn = new DateChooserJButton();
		    queryBtn = new JButton("查询");
		    vehicleCountLabel = new JLabel("车流量总数");
		    vehicleCountTextArea = new JTextField("", 8);
		    
		    add(startTimeLabel);
		    add(startTimeBtn);
		    add(endTimeLabel);
		    add(endTimeBtn);
		    add(queryBtn);
		    add(vehicleCountLabel);
		    add(vehicleCountTextArea);
		    
		    startTimeBtn.setEnabled(false);
		    endTimeBtn.setEnabled(false);
		    queryBtn.setEnabled(false);
		    
		    queryBtn.addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					findTrafficFlowState(startTimeBtn.getText(), endTimeBtn.getText());			
				}
			});
		    
		}
		
	}
	
	//登录按钮事件
	private void login() {	
		m_strIp = ipTextArea.getText();
		m_nPort = Integer.parseInt(portTextArea.getText());
		m_strUser = nameTextArea.getText();
		m_strPassword = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + m_strIp + "\n端口号：" + m_nPort 
				+ "\n用户名：" + m_strUser + "\n密码：" + m_strPassword);

		IntByReference nError = new IntByReference(0);
		m_hLoginHandle = NetSdk.CLIENT_LoginEx2(m_strIp, m_nPort.intValue(), m_strUser, m_strPassword, 0, null, m_stDeviceInfo, nError);
		if(m_hLoginHandle.longValue() == 0) {
			int error = 0;
			error = NetSdk.CLIENT_GetLastError();
			System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[0x%x]\n", m_strIp, m_nPort, error);
		} else {
			System.out.println("Login Success [ " + m_strIp + " ]");
			logoutBtn.setEnabled(true);
			loginBtn.setEnabled(false);	
		    startTimeBtn.setEnabled(true);
		    endTimeBtn.setEnabled(true);
		    queryBtn.setEnabled(true);
		}	
	}
	//登出按钮事件
	private void logout() {
		if(m_hLoginHandle.longValue() != 0) {
			System.out.println("Logout Button Action");

		
			if(NetSdk.CLIENT_Logout(m_hLoginHandle)) {
				System.out.println("Logout Success [ " + m_strIp + " ]");
				m_hLoginHandle.setValue(0);
				logoutBtn.setEnabled(false);
				loginBtn.setEnabled(true);	
			    startTimeBtn.setEnabled(false);
			    endTimeBtn.setEnabled(false);
			    queryBtn.setEnabled(false);
			}
		}
	}
	
    ////交通流量查询
	private void findTrafficFlowState(String startTime, String endTime) {
		// 开始时间
		String[] startStr = startTime.split(" ");
		String[] startTimeStr1 = startStr[0].split("-");
		String[] startTimeStr2 = startStr[1].split(":");
		
		// 结束时间
		String[] endStr = endTime.split(" ");
		String[] endTimeStr1 = endStr[0].split("-");
		String[] endTimeStr2 = endStr[1].split(":");
		
		// 设置查询条件
		NetSDKLib.FIND_RECORD_TRAFFICFLOW_CONDITION flowCondition = new NetSDKLib.FIND_RECORD_TRAFFICFLOW_CONDITION();
		flowCondition.bStatisticsTime = 1;  //查询是否为统计时间
		flowCondition.bStartTime = 1; // 使能
		flowCondition.bEndTime = 1; // 使能
		
		flowCondition.stStartTime.dwYear = Integer.parseInt(startTimeStr1[0]);
		flowCondition.stStartTime.dwMonth = Integer.parseInt(startTimeStr1[1]);
		flowCondition.stStartTime.dwDay = Integer.parseInt(startTimeStr1[2]);
		flowCondition.stStartTime.dwHour = Integer.parseInt(startTimeStr2[0]);
		flowCondition.stStartTime.dwMinute = Integer.parseInt(startTimeStr2[1]);
		flowCondition.stStartTime.dwSecond = Integer.parseInt(startTimeStr2[2]);
		
	    flowCondition.stEndTime.dwYear = Integer.parseInt(endTimeStr1[0]);
	    flowCondition.stEndTime.dwMonth = Integer.parseInt(endTimeStr1[1]);		
	    flowCondition.stEndTime.dwDay = Integer.parseInt(endTimeStr1[2]);
	    flowCondition.stEndTime.dwHour = Integer.parseInt(endTimeStr2[0]);
	    flowCondition.stEndTime.dwMinute = Integer.parseInt(endTimeStr2[1]);
	    flowCondition.stEndTime.dwSecond = Integer.parseInt(endTimeStr2[2]);
	    
		System.out.println("开始时间：" + flowCondition.stStartTime.toStringTime() + "\n" + "结束时间：" + flowCondition.stEndTime.toStringTime());
	    	
		// CLIENT_FindRecord 入参
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICFLOW_STATE; 
		stuFindInParam.pQueryCondition = flowCondition.getPointer();
		
		// CLIENT_FindRecord 出参
		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();
		
		flowCondition.write(); 
		boolean bRet = NetSdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 3000);
		flowCondition.read();		
		if(!bRet) {
			System.err.println("Can Not Find This Record" + Integer.toHexString(NetSdk.CLIENT_GetLastError()));
			return;
		}
		
		int count = 0;  //循环的次数
		int nFindCount = 0;	///查询到的总数初始化
		int nRecordCount = 10;  // 每次查询的个数
		int vehicleCount = 0;
		NetSDKLib.NET_RECORD_TRAFFIC_FLOW_STATE[] pstRecord = new NetSDKLib.NET_RECORD_TRAFFIC_FLOW_STATE[nRecordCount];
		for(int i=0; i<nRecordCount; i++) {
			pstRecord[i] = new NetSDKLib.NET_RECORD_TRAFFIC_FLOW_STATE();
		}
		
		///CLIENT_FindNextRecord入参
		NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
		stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
		stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
		
		///CLIENT_FindNextRecord出参
		NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
		stuFindNextOutParam.nMaxRecordNum = nRecordCount;
		stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
		stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);	
	
		ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);	//将数组内存拷贝给Pointer指针		
		
		while(true) {  //循环查询			
			if(NetSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000) ) {
				ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

				for(int i=0; i < stuFindNextOutParam.nRetRecordNum; i++) {
					nFindCount = i + count * nRecordCount;				
					
					System.out.println("[" + nFindCount + "]通过车辆总数:" + pstRecord[i].nVehicles);
					vehicleCount += pstRecord[i].nVehicles;
				}							
				
				if (stuFindNextOutParam.nRetRecordNum <= nRecordCount)
				{			
					break;					
				} else {
					count ++;
				}
			} else {
				System.err.println("FindNextRecord Failed" + NetSdk.CLIENT_GetLastError());
				break;
			}
		}

		NetSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);  

		vehicleCountTextArea.setText(String.valueOf(vehicleCount));
	}

	/*
	 * 登录
	 */
	private LoginPanel loginPanel;	
	private JButton loginBtn;
	private JButton logoutBtn;
	
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JLabel ipLabel;
	private JLabel portLabel;
	private JTextField ipTextArea;
	private JTextField portTextArea;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;
	
    private TrafficFlowPanel trafficFlowPanel;
    private DateChooserJButton startTimeBtn;
    private DateChooserJButton endTimeBtn;
    private JLabel startTimeLabel;
    private JLabel endTimeLabel;
    private JButton queryBtn;
    private JLabel vehicleCountLabel;
    private JTextField vehicleCountTextArea;
}


public class TrafficFlowState {
	public static void main(String[] args) {	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TrafficFlowStateFrame demo = new TrafficFlowStateFrame();	
				demo.setVisible(true);
			}
		});		
	}
}
