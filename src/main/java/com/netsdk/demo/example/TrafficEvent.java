package com.netsdk.demo.example;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.Vector;


class ITSEventMsg{
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;
      
    public void test(){
    	DEV_EVENT_TRAFFICJUNCTION_INFO msg=new DEV_EVENT_TRAFFICJUNCTION_INFO();
    	//结构体大小
    	
    	System.out.println(msg.size());
    	System.out.println(msg.stuObject.szText.hashCode());
    	System.out.println(msg.stTrafficCar.szPlateNumber.toString());
    }
	private SDKEnvironment sdkEnv;
	
	// 登录参数	
	private String address = "172.11.1.147";
	private Integer port = new Integer("37777");
	private String username = "admin";
	private String password = "admin123";
	
	// 设备信息
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	private LLong loginHandle = new LLong(0); // 登录句柄
	private LLong playHandle = new LLong(0);  // 预览句柄
	private LLong attachHandle = new LLong(0); // 智能事件订阅句柄
	
	private fAnalyzerDataCB    m_AnalyzerDataCB = new fAnalyzerDataCB(); // 智能事件回调
	private boolean bTriggerBtnClick = false; 
	
	private boolean bAttachFlag = false;
	private boolean bRealplayFlags = false;
	
	// 通道
	private Vector<String> chnlist = new Vector<String>(); 
	
	public ITSEventMsg() {
		sdkEnv = new SDKEnvironment();
		sdkEnv.init();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mainFrame = new JFrame("交通事件处理Demo");
				mainFrame.setSize(897, 721);
				mainFrame.setLayout(new BorderLayout());
				mainFrame.setLocationRelativeTo(null);
				mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				mainFrame.setVisible(true);
				
				loginJPanel = new LoginPanel(); // 登录面板 
				messagePanel = new MessagePanel(); // 事件信息提示
				realPlayPanel = new RealPlayPanel(); // 实时监视
				eventInfoPanel = new EventInfoPanel(); // 事件及图片
			
				mainFrame.add(loginJPanel, BorderLayout.NORTH);
				mainFrame.add(realPlayPanel, BorderLayout.EAST);
				mainFrame.add(eventInfoPanel, BorderLayout.CENTER);
				mainFrame.add(messagePanel, BorderLayout.SOUTH);
				
				WindowAdapter closeWindowAdapter =  new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						System.out.println("Window Closing");			
						logoutBtnPerformed(null); // 登出
						sdkEnv.cleanup();
						mainFrame.dispose();
					}
				};
				mainFrame.addWindowListener(closeWindowAdapter);
			}
		});		
	}
	
	/**
	 * NetSDK 库初始化
	 */
	private class SDKEnvironment {
    
	    private boolean bInit = false;
	    private boolean bLogopen = false;
	    
	    private DisConnect disConnect = new DisConnect();  // 设备断线通知回调
	    private HaveReConnect haveReConnect = new HaveReConnect(); // 网络连接恢复
	    
	    // 初始化
	    public boolean init() {
	    	
			// SDK 库初始化, 并设置断线回调
			bInit = netsdkApi.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}
			
			// 打开日志，可选
			LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
			
			File path = new File(".");			
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\ITSEventMsg_" + System.currentTimeMillis() + ".log";
				
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
			
			setLog.bSetPrintStrategy = 1;
			setLog.nPrintStrategy = 0;
			bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
			if (!bLogopen) {
				System.err.println("Failed to open NetSDK log !!!");
			}
			
			// 获取版本, 可选操作
			System.out.printf("NetSDK Version [%d]\n", netsdkApi.CLIENT_GetSDKVersion());		
			
			// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
			// 此操作为可选操作，但建议用户进行设置
			netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
			
			// 设置登录超时时间和尝试次数 , 此操作为可选操作	   
			int waitTime = 5000;   // 登录请求响应超时时间设置为 5s
			int tryTimes = 3;      // 登录时尝试建立链接3次
			netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
			
			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
			// 接口设置的登录设备超时时间和尝试次数意义相同
			// 此操作为可选操作
			NET_PARAM netParam = new NET_PARAM();
			netParam.nConnectTime = 10000; // 登录时尝试建立链接的超时时间
			netParam.nPicBufSize = 8 * 1024 * 1024;
			netsdkApi.CLIENT_SetNetworkParam(netParam);
	    	
	    	return true;
	    }
	    
	    // 清除环境
	    public void cleanup() {
	    	if (bLogopen) {
	    		netsdkApi.CLIENT_LogClose();
	    	}
	    	
	    	if (bInit) {
	    		netsdkApi.CLIENT_Cleanup();
	    	}
	    }
	    
	    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	    public class DisConnect implements fDisConnect  {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
	        	
	        	// 取消订阅
	        	detachIVSEvent();
	        }
	    }
		
		// 网络连接恢复，设备重连成功回调
		// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	    public class HaveReConnect implements fHaveReConnect {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
	        	
	        	// 重新订阅
	        	attachIVSEvent();
	        }
	    }
	}
	
	///////////////// 事件动作相关接口 ///////////////////////////
	/**
	 * 登录按钮
	 */
	private void loginBtnPerformed(ActionEvent e) {
		address = ipTextArea.getText();
		port = Integer.parseInt(portTextArea.getText());
		username = nameTextArea.getText();
		password = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + address
						+  "\n端口号：" + port
						+  "\n用户名：" + username
						+  "\n密码：" + password);
		
		// 登录设备
		IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(address, port.intValue(), username , password , 0, null, deviceInfo, nError);
        if(loginHandle.longValue() == 0)
        {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%x]\n" , address , port , netsdkApi.CLIENT_GetLastError());
            JOptionPane.showMessageDialog(mainFrame, "登录失败");
        }
        else
        {
        	System.out.println("Login Success [ " + address +" ]");
        	JOptionPane.showMessageDialog(mainFrame, "登录成功");        	
        	loginJPanel.enableCompents(true);
        	
			for(int i = 0; i < deviceInfo.byChanNum; i++) {
				chnlist.add("通道 " + i);
			}
						
		    if(deviceInfo.byChanNum > 0) {
		    	chnComboBox.setSelectedIndex(0);
		    }
        }
	}
	
	/**
	 * 登出按钮
	 */
	private void logoutBtnPerformed(ActionEvent e) {
		if (loginHandle.longValue() != 0) {
			System.out.println("LogOut Button Action");		
			
			// 确保关闭监视
			stopRealPlay();
			realplayButton.setText("监视");
			bRealplayFlags = false;
    		
			// 停止订阅事件
			detachIVSEvent();
			attachButton.setText("订阅");
			bAttachFlag = false;
			
    		if (netsdkApi.CLIENT_Logout(loginHandle)) {
	    		System.out.println("Logout Success [ " + address +" ]");
    			loginHandle.setValue(0);
    			loginJPanel.enableCompents(true);
    			loginJPanel.setTriggerBtnText("开闸");
	        	plateImagePanel.setOpaque(true);
	        	plateImagePanel.repaint();
	        	SnapImagePanel.setOpaque(true);
	        	SnapImagePanel.repaint();
	        	bTriggerBtnClick = false;
	        	chnlist.clear();
				chnComboBox.setModel(new DefaultComboBoxModel(chnlist));
    		}    		
    	}
	}
	
	/**
	 * 老版本开闸按钮
	 */
	private void Old_TriggerButtonPerformed(ActionEvent e) {
		if (loginHandle.longValue() != 0) {
			System.out.println("Trigger Button Action");
			
			ALARMCTRL_PARAM param = new ALARMCTRL_PARAM();
			param.nAction = bTriggerBtnClick ? 1 : 0; // 1：触发报警；0：停止报警. 按钮按下
			if (netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_TRIGGER_ALARM_OUT, param.getPointer(), null, 3000)) {
				System.out.println("控制成功");
				bTriggerBtnClick = !bTriggerBtnClick;
				loginJPanel.setTriggerBtnText(bTriggerBtnClick ? "触发外部报警":"关闭外部报警");
			}else {
				System.out.printf("Failed to Open 0x%x", netsdkApi.CLIENT_GetLastError());
			}
		}
	}
	
	/**
	 * 新版本开闸按钮
	 */
	private void New_TriggerButtonPerformed(ActionEvent e) {
		if (loginHandle.longValue() != 0) {
			System.out.println("New Trigger Button Action");
			
			if (!bTriggerBtnClick) { // 开闸
				NET_CTRL_OPEN_STROBE openStrobe = new NET_CTRL_OPEN_STROBE();
				openStrobe.nChannelId = 0;
				String plate = "浙A888888";
				
				System.arraycopy(plate.getBytes(), 0, openStrobe.szPlateNumber, 0, plate.getBytes().length);
				openStrobe.write();
				if (netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_OPEN_STROBE, openStrobe.getPointer(), null, 3000))
				{
				    System.out.println("Open Success!");
				    bTriggerBtnClick = true;
				    loginJPanel.setTriggerBtnText("关闸");
				}
				else {
					System.out.printf("Failed to Open 0x%x\n", netsdkApi.CLIENT_GetLastError());
				} 
				openStrobe.read();
					
			}
			else { // 关闸
				NET_CTRL_CLOSE_STROBE closeStrobe = new NET_CTRL_CLOSE_STROBE();
                closeStrobe.nChannelId = 0;
                closeStrobe.write();
                if (netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_CLOSE_STROBE, closeStrobe.getPointer(), null, 3000))
    			{
                	System.out.println("Close Success!");
                	bTriggerBtnClick = false;
				    loginJPanel.setTriggerBtnText("开闸");
				    
                }else {
                	System.out.printf("Failed to Close 0x%x\n", netsdkApi.CLIENT_GetLastError());
                }
                closeStrobe.read();
			}		
		}
	}
	
	/**
	 * 开始实时监视按钮事件
	 */
	private void startRealPlay() {
		if (loginHandle.longValue() == 0) {
			System.err.println("Please login first");
			JOptionPane.showMessageDialog(mainFrame, "请先登录");
			return;
		}
		
		int channel = chnComboBox.getSelectedIndex(); // 预览通道号
		int playType = NET_RealPlayType.NET_RType_Realplay; // 实时预览
		
		playHandle = netsdkApi.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(realplayWindow), playType);
		if (playHandle.longValue() == 0) {
			int error = netsdkApi.CLIENT_GetLastError();
			JOptionPane.showMessageDialog(mainFrame, "开始实时监视失败，错误码：" + String.format("[0x%x]", error));
		} else {
			System.out.println("[通道" + channel + "] 拉流成功！");
		}
	}
	
	/**
	 * 结束实时预览按钮事件
	 */
	private void stopRealPlay() {
		if (playHandle.longValue() == 0) {
			System.out.println("Make sure the realplay Handle is valid");
			return;
		}
		
		if (netsdkApi.CLIENT_StopRealPlayEx(playHandle)) {
			System.out.println("Success to stop realplay");
			
			playHandle.setValue(0);
			realplayWindow.repaint();
		}
	}
	
	// 订阅实时上传智能分析数据
    private void attachIVSEvent() { 
    	
    	/**
		 * 说明：
		 * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
		 *  下列仅订阅了0通道的智能事件.
		 */
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = chnComboBox.getSelectedIndex(); //  通道 

        attachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , m_AnalyzerDataCB , null , null);
        if( attachHandle.longValue() != 0  )
        {
    		System.out.println("[通道" + ChannelId + "] 订阅成功！");
        }
        else
        {
            System.out.printf("CLIENT_RealLoadPictureEx Failed!LastError = %x\n", netsdkApi.CLIENT_GetLastError() );
            return;
        }
    }
    
    // 停止上传智能分析数据－图片
    private void detachIVSEvent() {
        if (0 != attachHandle.longValue()) {
            netsdkApi.CLIENT_StopLoadPic(attachHandle);
            System.out.println("Stop detach IVS event");
            attachHandle.setValue(0);
        }
    }
    
    /* 智能报警事件回调 */
    public class fAnalyzerDataCB implements fAnalyzerDataCallBack/*, StdCallCallback*/ {
        private String m_imagePath;
        NET_MSG_OBJECT plateObject; // 车牌信息
        NET_MSG_OBJECT vehicleObject; // 车辆信息
        NET_TIME_EX utc; // 事件时间
        
        int lane = 0; // 车道号
        String EventMsg; // 事件信息
        int nConfide = 0; // 置信度, 只有特定设备才支持，一般设备默认都是0不填充
        
        String bigPicture; // 大图
        String smallPicture; // 小图
              
        public fAnalyzerDataCB() {
        	m_imagePath = "./PlateNumber/";
            File path = new File(m_imagePath);
            if (!path.exists()) {
                path.mkdir();
            }
        }
        
        // 回调
        public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
                Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                Pointer dwUser, int nSequence, Pointer reserved) 
        {
            if (lAnalyzerHandle.longValue() == 0) {
                return -1;
            }         
            System.out.println("--------dwAlarmType-------------" + dwAlarmType);  
            // 获取事件信息        
            plateObject = new NET_MSG_OBJECT(); // 车牌信息
            vehicleObject = new NET_MSG_OBJECT(); // 车辆信息
            utc = new NET_TIME_EX(); // 事件时间
            
            lane = -1; // 车道号
            EventMsg = ""; // 事件信息
            nConfide = 0; // 置信度, 只有特定设备才支持，一般设备默认都是0不填充
            
            GetStuObject(dwAlarmType, pAlarmInfo);
            
            // 保存大图片
            SavePlatePic(plateObject, pBuffer, dwBufSize);
            
            // 更新界面
            messageTextArea.append(EventMsg);
            
            // 显示车牌、车牌颜色、车身颜色、时间、车道号
            try {
            	// plateLicenseTextField.setText(new String(plateObject.szText, "UTF-8").trim()); // Linux 平台下使用UTF-8格式
				plateLicenseTextField.setText(new String(plateObject.szText, "GBK").trim()); // Windows 平台下使用GBK格式
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
            if (1 == plateObject.bColor) {
            	plateColorTextField.setText(GetColorString(plateObject.rgbaMainColor));
            }
            else {
            	plateColorTextField.setText("未填充颜色");
            }
            
            if (1 == vehicleObject.bColor) {
            	vehicleColorTextField.setText(GetColorString(vehicleObject.rgbaMainColor));
            }
            else {
            	vehicleColorTextField.setText("未填充颜色");
            }
            
            laneNumberTextField.setText(String.format("%d", lane));
            snapTimeTextField.setText(String.format("%02d:%02d:%02d", utc.dwHour, utc.dwMinute, utc.dwSecond));
                    
            return 0;
        }
        
        // 获取识别对象 车身对象 事件发生时间 车道号等信息
        private boolean GetStuObject(int dwAlarmType, Pointer pAlarmInfo)
        {
        	if(pAlarmInfo == null) {
        		return false;
        	}
        	NET_EVENT_FILE_INFO fileInfo = new NET_EVENT_FILE_INFO();
        	
        	boolean isAlarmTypeParsed = true;
        	switch(dwAlarmType)
            {
	        	case NetSDKLib.EVENT_IVS_SNAPMANUAL:   ///< 手动抓图事件(SnapManual事件)
	        	{
	        		DEV_EVENT_SNAPMANUAL msg = new DEV_EVENT_SNAPMANUAL(); 
	        	  	ToolKits.GetPointerData(pAlarmInfo, msg);
	        	  	
	        	  	// 以下为事件的信息，具体的图片保存是根据回调里的  Pointer pBuffer, int dwBufSize 来生成。
	        		
	        		EventMsg = "[ "+ msg.UTC.toString() + " ] " + "SnapManual事件" + "通道号 : " + msg.nChannelID + "图片的序号(同一时间内(精确到秒)可能有多张图片, 从0开始)：" + msg.byImageIndex;  
	        		 
	        	  	JOptionPane.showMessageDialog(null, "手动抓图成功！");
	        	  	
	        		break;
	        	}
	        	case NetSDKLib.EVENT_IVS_VEHICLE_RECOGNITION:  ///< 车牌对比事件
	        	{	
	        		DEV_EVENT_VEHICLE_RECOGNITION_INFO msg = new DEV_EVENT_VEHICLE_RECOGNITION_INFO();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);
	
	                plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    
                    System.out.println("channel : " + msg.nChannel);
                    
                    // 对比信息在   msg.stuCarCandidate数组里
	
	                EventMsg = "[ "+ "车辆动作:" + msg.nVehicleAction + ";" + utc.toStringTime() + " ] " + "车牌对比事件";   
	        		
	        		break;
	        	}
	            case NetSDKLib.EVENT_IVS_TRAFFIC_FLOWSTATE: ///< 交通流量统计事件
	            {
	            	DEV_EVENT_TRAFFIC_FLOW_STATE msg = new DEV_EVENT_TRAFFIC_FLOW_STATE();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);

                    utc = msg.UTC;
  
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "交通流量统计事件" + "flowNum :" + msg.nStateNum;              
	                break;
	            }
        		case NetSDKLib.EVENT_IVS_TRAFFIC_TURNLEFT: //<违章左转
        		{
        			DEV_EVENT_TRAFFIC_TURNLEFT_INFO msg = new DEV_EVENT_TRAFFIC_TURNLEFT_INFO();
        			ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "违章左转事件";
        			break;
        		}	
        		case NetSDKLib.EVENT_IVS_TRAFFIC_TURNRIGHT: ///<违章右转
        		{
        			DEV_EVENT_TRAFFIC_TURNRIGHT_INFO msg = new DEV_EVENT_TRAFFIC_TURNRIGHT_INFO();
        			ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "违章右转事件";
        			break;
        		}
        		case NetSDKLib.EVENT_IVS_TRAFFIC_UTURN: ///<违章掉头
        		{
        			DEV_EVENT_TRAFFIC_UTURN_INFO msg = new DEV_EVENT_TRAFFIC_UTURN_INFO();
        			ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "违章掉头事件";
        			break;
        		}
        		case NetSDKLib.EVENT_IVS_TRAFFIC_RUNYELLOWLIGHT: ///<闯黄灯
        		{
        			DEV_EVENT_TRAFFIC_RUNYELLOWLIGHT_INFO msg = new DEV_EVENT_TRAFFIC_RUNYELLOWLIGHT_INFO();
        			ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "闯黄灯事件";
        			break;
        		}
                case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION: ///< 路口事件
                {
                	DEV_EVENT_TRAFFICJUNCTION_INFO msg = new DEV_EVENT_TRAFFICJUNCTION_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    
                  //  EventMsg = "[ "+ utc.toStringTime() + " ] " + "路口事件" + "[" +  "速度"+ "]：" + msg.stuNonMotor.fSpeed;
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_RUNREDLIGHT: ///< 交通违章-闯红灯事件
                {
                	DEV_EVENT_TRAFFIC_RUNREDLIGHT_INFO msg = new DEV_EVENT_TRAFFIC_RUNREDLIGHT_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "闯红灯交通违章-闯红灯事件";
                    
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_OVERLINE: ///< 交通违章-压车道线事件
                {
                	DEV_EVENT_TRAFFIC_OVERLINE_INFO msg = new DEV_EVENT_TRAFFIC_OVERLINE_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "压车道线事件";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_OVERYELLOWLINE: ///< 交通违章-压黄线事件
                {
                	 DEV_EVENT_TRAFFIC_OVERYELLOWLINE_INFO msg = new DEV_EVENT_TRAFFIC_OVERYELLOWLINE_INFO();
                	 ToolKits.GetPointerData(pAlarmInfo, msg);
                     plateObject = msg.stuObject;
                     vehicleObject = msg.stuVehicle;
                     utc = msg.UTC;
                     lane = msg.nLane;                 
                     nConfide = msg.stuObject.nConfidence;
                     fileInfo = msg.stuFileInfo;
                     EventMsg = "[ "+ utc.toStringTime() + " ] " + "压黄线事件";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_OVERSPEED: ///< 交通违章-超速
                {
                	DEV_EVENT_TRAFFIC_OVERSPEED_INFO msg = new DEV_EVENT_TRAFFIC_OVERSPEED_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "超速事件";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKING: ///< 交通违章-违章停车
                {
                	 DEV_EVENT_TRAFFIC_PARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKING_INFO();
                	 ToolKits.GetPointerData(pAlarmInfo, msg);
                     plateObject = msg.stuObject;
                     vehicleObject = msg.stuVehicle;
                     utc = msg.UTC;
                     lane = msg.nLane;                 
                     nConfide = msg.stuObject.nConfidence;
                     fileInfo = msg.stuFileInfo;
                     EventMsg = "[ "+ utc.toStringTime() + " ] " + "违章停车";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_WRONGROUTE: ///< 交通违章-不按车道行驶
                {
                	 DEV_EVENT_TRAFFIC_WRONGROUTE_INFO msg = new DEV_EVENT_TRAFFIC_WRONGROUTE_INFO();
                	 ToolKits.GetPointerData(pAlarmInfo, msg);
                     plateObject = msg.stuObject;
                     vehicleObject = msg.stuVehicle;
                     utc = msg.UTC;
                     lane = msg.nLane;                 
                     nConfide = msg.stuObject.nConfidence;
                     fileInfo = msg.stuFileInfo;
                     EventMsg = "[ "+ utc.toStringTime() + " ] " + "不按车道行驶";
                     break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_YELLOWPLATEINLANE: ///< 交通违章-黄牌车占道事件
                {
                	 DEV_EVENT_TRAFFIC_YELLOWPLATEINLANE_INFO msg = new DEV_EVENT_TRAFFIC_YELLOWPLATEINLANE_INFO();
                	 ToolKits.GetPointerData(pAlarmInfo, msg);
                     
                     plateObject = msg.stuObject;
                     vehicleObject = msg.stuVehicle;
                     utc = msg.UTC;
                     lane = msg.nLane;                 
                     nConfide = msg.stuObject.nConfidence;
                     fileInfo = msg.stuFileInfo;
                     EventMsg = "[ "+ utc.toStringTime() + " ] " + "黄牌车占道事件";
                     break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_VEHICLEINROUTE: ///< 有车占道事件
                {
                	DEV_EVENT_TRAFFIC_VEHICLEINROUTE_INFO msg = new DEV_EVENT_TRAFFIC_VEHICLEINROUTE_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "有车占道事件";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP: /// 手动抓拍事件
                {
                	DEV_EVENT_TRAFFIC_MANUALSNAP_INFO msg = new DEV_EVENT_TRAFFIC_MANUALSNAP_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);              	     	
                	
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "手动抓拍事件";
                    
                	JOptionPane.showMessageDialog(null, "手动抓拍成功！");   
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_THROW: /// 抛洒物
                {
                	DEV_EVENT_TRAFFIC_THROW_INFO msg = new DEV_EVENT_TRAFFIC_THROW_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "抛洒物";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PEDESTRAIN: /// 交通行人
                {
                	DEV_EVENT_TRAFFIC_PEDESTRAIN_INFO msg = new DEV_EVENT_TRAFFIC_PEDESTRAIN_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    nConfide = msg.stuObject.nConfidence;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "交通行人";
                	break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFICJAM: /// 交通拥堵
                {
                	DEV_EVENT_TRAFFICJAM_INFO msg = new DEV_EVENT_TRAFFICJAM_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "交通拥堵";
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: /// 车位有车事件
                {
                	DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "车位有车事件";
                    
//                    NetSDKLib.DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO trafficCar = msg.stTrafficCar;                  
//                    System.out.println("停车场车位号：" + new String(trafficCar.szCustomParkNo).trim()
//                    		 +"\n行驶方向 " + trafficCar.byDirection 
//                    		 +"\nVehicleSize "+ trafficCar.nVehicleSize
//                    		 +"\nszPlateColor "+ new String(trafficCar.szPlateColor).trim()
//                    		 +"\nszVehicleColor "+ new String(trafficCar.szVehicleColor).trim()
//                    		);
                    
                	break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: /// 车位无车事件
                {
                	DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;                 
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "车位无车事件";
                	break;
                }
                default:
                	isAlarmTypeParsed = false;
                    System.out.printf("Get Event 0x%x\n", dwAlarmType);
                    EventMsg = "未处理事件 dwAlarmType = " + String.format("0x%x", dwAlarmType); 
                    break;
            }
        	
        	if (isAlarmTypeParsed) {
        		EventMsg = EventMsg + ";组编号 GroupID = " + fileInfo.nGroupId 
        							+ ";图片组总数 bount = " + fileInfo.bCount 
        							+ ";当前图片序号 bIndex = " + fileInfo.bIndex 
        							+ ";置信度 = " + nConfide;
        		
        		// 车牌
        		String PlateNumber = null;
				try {
					PlateNumber = new String(plateObject.szText, "GBK").trim();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
        		if (PlateNumber.length() > 0) {
        			EventMsg += ";车牌号 = " + PlateNumber;
        		}
        		
        		// 车牌类型
        		String plateType =  new String(plateObject.szObjectSubType).trim();
        		if (plateType.length() > 0) {
        			EventMsg += ";车牌类型 = " + plateType;
        		}
        		
        		// 车标
        		String vehicleType = new String(vehicleObject.szText).trim();
        		if (vehicleType.length() > 0) {
        			EventMsg += ";车标 = " + vehicleType;
        		}
        		
        		//　车辆类型
        		String vehicleSubType = new String(vehicleObject.szObjectSubType).trim();
        		if (vehicleSubType.length() > 0) {
        			EventMsg += ";车辆类型  = " + vehicleSubType;
        		}
        		
        		
        		EventMsg += "\n";
        		return true;
        	}
        	else {
        		return false;
        	}
        }
    
        // 颜色对照表
        private String GetColorString(int RGBColor) {
        	String strColor = "未定义颜色";
        	int Color = (RGBColor >> 8) & 0x00ffffff;
        	switch(Color) {
        	case 0x000000:
        		strColor = "黑色";
        		break;
        	case 0xFFFFFF:
        		strColor = "白色";
        		break;
        	case 0xFF0000:
        		strColor = "红色";
        		break;
        	case 0x0000FF:
        		strColor = "蓝色";
        		break;
        	case 0x00FF00:
        		strColor = "绿色";
        		break;
        	case 0xFFFF00:
        		strColor = "黄色";
        		break;
        	case 0x808080:
        		strColor = "灰色";
        		break;
        	case 0xFFA500:
        		strColor = "橙色";
        		break;
        	}
        	
        	return strColor;
        }
    
        // 显示车牌小图:大华早期交通抓拍机，设备不传单独的车牌小图文件，只传车牌在大图中的坐标；由应用来自行裁剪。
        // 2014年后，陆续有设备版本，支持单独传车牌小图，小图附录在pBuffer后面。
        private void SavePlatePic(NET_MSG_OBJECT plateObject, Pointer pBuffer, int dwBufSize) {
        	// 清空
        	SnapImagePanel.setOpaque(true);
			SnapImagePanel.repaint(); 
			
			plateImagePanel.setOpaque(true);
			plateImagePanel.repaint();
        	
			BufferedImage snapImage = null;
        	
        	if (pBuffer != null && dwBufSize > 0) {
            	bigPicture = m_imagePath + "Big_" + UUID.randomUUID().toString() + ".jpg";
            	ToolKits.savePicture(pBuffer, 0, dwBufSize, bigPicture);
                
                try {
            		File bigFile = new File(bigPicture);
            		snapImage = ImageIO.read(bigFile);
            		
            		//System.out.println("weigh/heigh " + snapImage.getHeight(null) + " " + snapImage.getWidth(null));
            		SnapImagePanel.setOpaque(false);
        			SnapImagePanel.setImage(snapImage);
        			SnapImagePanel.repaint(); 			
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
        	}
        	
        	if (plateObject.bPicEnble == 1) {
        		//根据pBuffer中数据偏移保存小图图片文件
        		int picLength = plateObject.stPicInfo.dwFileLenth;
        		if (picLength > 0) {
            		smallPicture = m_imagePath + "small_" + UUID.randomUUID().toString() + ".jpg";
            		ToolKits.savePicture(pBuffer, plateObject.stPicInfo.dwOffSet, picLength, smallPicture);
        		}
        		
    			if(smallPicture == null) {
    				return;
    			}
    			
        		try {  		
        			File smallFile = new File(smallPicture);
        			if(smallFile != null) {
        	 			Image plateImage = ImageIO.read(smallFile);
            			plateImagePanel.setOpaque(false);
            			plateImagePanel.setImage(plateImage);
            			plateImagePanel.repaint();
        			}     					 
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}	
        	else {
        		if(plateObject.BoundingBox == null) {
        			return;
        		}
        		//根据大图中的坐标偏移计算显示车牌小图
                if (plateObject.BoundingBox.bottom.longValue() == 0 
                		&& plateObject.BoundingBox.top.longValue() == 0) {
                    return ;
                }

                DH_RECT dhRect = plateObject.BoundingBox;
        		//1.BoundingBox的值是在8192*8192坐标系下的值，必须转化为图片中的坐标
                //2.OSD在图片中占了64行,如果没有OSD，下面的关于OSD的处理需要去掉(把OSD_HEIGHT置为0)
        		final int OSD_HEIGHT = 0;
        		
                long nWidth = snapImage.getWidth(null);
                long nHeight = snapImage.getHeight(null);
                
                nHeight = nHeight - OSD_HEIGHT;
                if ((nWidth <= 0) || (nHeight <= 0)) {
                    return ;
                }
                
                DH_RECT dstRect = new DH_RECT();
                
                dstRect.left.setValue((long)((nWidth * dhRect.left.longValue()) / 8192.0));
                dstRect.right.setValue((long)((nWidth * dhRect.right.longValue()) / 8192.0));
                dstRect.bottom.setValue((long)((nHeight * dhRect.bottom.longValue()) / 8192.0));
                dstRect.top.setValue((long)((nHeight * dhRect.top.longValue()) / 8192.0));

                int x = dstRect.left.intValue();
                int y = dstRect.top.intValue() + OSD_HEIGHT;
                int w = dstRect.right.intValue() - dstRect.left.intValue();
                int h = dstRect.bottom.intValue() - dstRect.top.intValue();
                //System.out.println(" x =" + x + ", y =" + y + "; w = "+ w +"; h = "+ h);
                try {
                    BufferedImage plateImage = snapImage.getSubimage(x, y, w, h);
                    smallPicture = m_imagePath + "small_" + UUID.randomUUID().toString() + ".jpg";
                    ImageIO.write(plateImage, "jpg", new File(smallPicture));
                    
        			plateImagePanel.setOpaque(false);
        			plateImagePanel.setImage(plateImage);
        			plateImagePanel.repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
    }
    
    /**
     * 手动抓拍按钮事件
     */
    private void snapPic(int chn) {
    	if (loginHandle.longValue() == 0) {
    		System.err.println("Plese Login First");
    		JOptionPane.showMessageDialog(mainFrame, "请先登录");
    		return;
    	}
    	
    	MANUAL_SNAP_PARAMETER snapParam = new MANUAL_SNAP_PARAMETER();
    	snapParam.nChannel = chn;
    	
    	snapParam.write();
    	boolean bRet = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_MANUAL_SNAP, snapParam.getPointer(), null, 5000);
    	if (!bRet) {
    		System.err.println("Failed to manual snap, last error " + String.format("[0x%x]", netsdkApi.CLIENT_GetLastError()));
    	}
    	else {
    		System.out.println("Seccessed to manual snap");
    	}
		snapParam.read();
    }
    
    /**
     * 即时抓图(又名手动抓图)
     */
    public void snapShot(int chn) {
    	if (loginHandle.longValue() == 0) {
    		System.err.println("Plese Login First");
    		JOptionPane.showMessageDialog(mainFrame, "请先登录");
    		return;
    	}
    	
    	// 入参
    	NET_IN_SNAP_MNG_SHOT stIn = new NET_IN_SNAP_MNG_SHOT();
    	stIn.nChannel = chn;            // 通道号
    	stIn.nTime = 1;;                // 连拍次数, 0表示停止抓拍,正数表示连续抓拍的张数
    	
    	// 出参
    	NET_OUT_SNAP_MNG_SHOT stOut = new NET_OUT_SNAP_MNG_SHOT();
    	
    	stIn.write();
    	stOut.write();
    	boolean bRet = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_SNAP_MNG_SNAP_SHOT, stIn.getPointer(), stOut.getPointer(), 5000);
    	if (!bRet) {
    		System.err.println("Failed to snap shot, last error " + String.format("[0x%x]", netsdkApi.CLIENT_GetLastError()));
    	}
    	else {
    		System.out.println("Seccessed to snap shot.");
    	}
    	stIn.read();
    	stOut.read();
    }
	
	/////////////////////界面组件//////////////////////////
	///////////////// 界面组件相关成员变量  //////////////////
	/**
	 * 带背景的面板组件
	 */
	private class PaintPanel extends JPanel {
	    
	    /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		/**
	     * 背景图片
	     */
	    private Image image;
	    
	    /**
	     * 构造方法
	     */
	    public PaintPanel() {
	        super();
	        setOpaque(true); // 非透明
	        setLayout(null);
	        setBackground(new Color(153, 240, 255));
			setForeground(new Color(0, 0, 0));
	    }
	    
	    /**
	     * 设置图片的方法
	     */
	    public void setImage(Image image) {
	        this.image = image;
	    }
	    
	    @Override
	    protected void paintComponent(Graphics g) {// 重写绘制组件外观
	        if (image != null) {
	            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);// 绘制图片与组件大小相同
	        }
	        super.paintComponent(g);// 执行超类方法
	    }
	}
	
	/**
	 * 设置边框
	 */
	private void setBorderEx(JComponent object, String title, int width) {
		Border innerBorder = BorderFactory.createTitledBorder(title);
		Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);	
		object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
	}
	
	/**
	 * 登录面板
	 */
	private class LoginPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public LoginPanel() {
			loginBtn = new JButton("登入");
			logoutBtn = new JButton("登出");
			triggerBtn = new JButton("开闸");
			nameLabel = new JLabel("用户名");
			passwordLabel = new JLabel("密码");
			nameTextArea = new JTextField(username, 8);
			passwordTextArea = new JPasswordField(password, 8);
			ipLabel = new JLabel("设备地址");
			portLabel = new JLabel("端口号");
			ipTextArea = new JTextField(address, 15);
			portTextArea = new JTextField("37777", 6);
			
			setLayout(new FlowLayout());
			setBorderEx(this, "登录", 2);
			
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
			add(triggerBtn);
			
			enableCompents(false);
			
			// 登录按钮. 监听事件
			loginBtn.addActionListener(new ActionListener() {		
				public void actionPerformed(ActionEvent e) {
					loginBtnPerformed(e);
				}
			});
			
			// 登出按钮. 监听事件
			logoutBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					logoutBtnPerformed(e);
					enableCompents(false);
				}
			});	
		
			// 开闸. 监听事件
			triggerBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
//					Old_TriggerButtonPerformed(e); // 老版本
					New_TriggerButtonPerformed(e); // 新版本
				}
			});
		}
	
		public void enableCompents(boolean enable) {
			loginBtn.setEnabled(!enable);
			logoutBtn.setEnabled(enable);
			triggerBtn.setEnabled(enable);
		}
		
		public void setTriggerBtnText(final String text) {
			triggerBtn.setText(text);
		}
		
		private JButton loginBtn;
		private JButton logoutBtn;
		private JButton triggerBtn;
		private JLabel nameLabel;
		private JLabel passwordLabel;
		private JLabel ipLabel;
		private JLabel portLabel;
	}
	
	/**
	 * 事件信息显示面板
	 */
	private class MessagePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public MessagePanel() {
			setBorderEx(this, "事件信息提示", 2);
			
			Dimension dim = getPreferredSize();
			dim.height = 226;
			setPreferredSize(dim);
			setLayout(new BorderLayout());
			
			messageTextArea = new JTextArea();
			
			add(new JScrollPane(messageTextArea), BorderLayout.CENTER);
		}
	}

	/**
	 * 预览面板
	 */
	private class RealPlayPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public RealPlayPanel() {
			setBorderEx(this, "实时预览", 2);
			setLayout(new BorderLayout());
			Dimension dim = getPreferredSize();
			dim.height = 367;
			dim.width = 374;
			setPreferredSize(dim);
	
			realplayWindow = new Panel();	
			chnComboBox = new JComboBox(chnlist);
			attachButton = new JButton("订阅");
			realplayButton = new JButton("监视");
				
			realplayWindow.setBackground(new Color(153, 240, 255));
			realplayWindow.setForeground(new Color(0, 0, 0));
			realplayWindow.setBounds(5, 5, 350, 290);
			realplayWindow.setSize(358, 294);
			
			JPanel btnPanel = new JPanel();
			btnPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			btnPanel.setLayout(new GridLayout(1, 3, 5, 5));		
			btnPanel.add(chnComboBox);
			btnPanel.add(attachButton);
			btnPanel.add(realplayButton);
			
			add(realplayWindow, BorderLayout.CENTER);
			add(btnPanel, BorderLayout.SOUTH);	
			
			// 订阅按钮动作监听
			attachButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(!bAttachFlag) {
						attachIVSEvent();
						attachButton.setText("停止订阅");
						bAttachFlag = true;
					} else {
						detachIVSEvent();
						attachButton.setText("订阅");
						bAttachFlag = false;
					}	
				}
			});
			
			// 监视按钮监听
			realplayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(!bRealplayFlags) {
						startRealPlay();
						realplayButton.setText("停止监视");
						bRealplayFlags = true;
					} else {
						stopRealPlay();
						realplayButton.setText("监视");
						bRealplayFlags = false;
					}	
				}
			});
		}
	}
	
	/**
	 * 事件及图片面板
	 */
	private class EventInfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public EventInfoPanel() {
			Dimension dimension = new Dimension(-1, -1);
			
			setBorderEx(this, "事件及图片", 2);
			setLayout(new BorderLayout());
			
			SnapImagePanel = new PaintPanel(); // 事件大图
			snapIamgeLabel = new JLabel("事件大图");			
			plateImagePanel = new PaintPanel(); // 车牌小图
			plateImageLabel = new JLabel("车牌小图");
			plateLicenseLabel = new JLabel("车牌号码");
			plateColorLabel = new JLabel("车牌颜色");
			vehicleColorLabel = new JLabel("车身颜色");
			snapTimeLabel = new JLabel("抓拍时间");
			laneNumberLabel = new JLabel("车道号");
			plateLicenseTextField = new JTextField("", 2);
			plateColorTextField = new JTextField("", 2);
			vehicleColorTextField = new JTextField("", 2);
			snapTimeTextField = new JTextField("", 2);
			laneNumberTextField = new JTextField("", 2);		
			snapPicButton = new JButton("手动抓拍");
			snapShotButton = new JButton("手动抓图");
			
			//////// 车牌及抓图时间面板		
			JPanel paramPanel = new JPanel();
			JPanel textPanel = new JPanel();
			JPanel platePanel = new JPanel();
			
			dimension.width = 145;
			dimension.height = 49;
			plateImagePanel.setPreferredSize(dimension);
			platePanel.setLayout(new BorderLayout());
			platePanel.add(plateImagePanel, BorderLayout.SOUTH);
			platePanel.add(plateImageLabel, BorderLayout.CENTER);
					
			SnapImagePanel.setBackground(new Color(153, 240, 255));
			SnapImagePanel.setForeground(new Color(0, 0, 0));
			SnapImagePanel.setBounds(5, 5, 290, 270);
			SnapImagePanel.setSize(291, 277);
			
			dimension.width = 150;
			dimension.height = 50;
			paramPanel.setPreferredSize(dimension);
			paramPanel.setLayout(new BorderLayout());
			paramPanel.setBorder(new EmptyBorder(5,5,5,5));
			
			textPanel.setLayout(new GridLayout(9, 2));
			textPanel.add(plateLicenseLabel);
			textPanel.add(plateLicenseTextField);
			
			textPanel.add(plateColorLabel);
			textPanel.add(plateColorTextField);
			
			textPanel.add(vehicleColorLabel);
			textPanel.add(vehicleColorTextField);
			
			textPanel.add(laneNumberLabel);
			textPanel.add(laneNumberTextField);
			
			textPanel.add(snapTimeLabel);
			textPanel.add(snapTimeTextField);
			
			paramPanel.add(platePanel, BorderLayout.NORTH);
			paramPanel.add(textPanel, BorderLayout.CENTER);
			
			JPanel snapPanel = new JPanel();
			paramPanel.add(snapPanel, BorderLayout.SOUTH);
			
			
			snapPanel.setLayout(new GridLayout(2, 1));
			snapPanel.add(snapPicButton);
			snapPanel.add(snapShotButton);
			
			///////// 事件大图面板 ////////////////////////////
			JPanel snapJPanel = new JPanel();
			snapJPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			snapJPanel.setLayout(new BorderLayout());
			snapJPanel.add(snapIamgeLabel, BorderLayout.NORTH);
			snapJPanel.add(SnapImagePanel, BorderLayout.CENTER);
			
			add(snapJPanel, BorderLayout.CENTER);
			add(paramPanel, BorderLayout.EAST);
			
			// 手动抓拍
			snapPicButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {		
					snapPic(chnComboBox.getSelectedIndex());
				}
			});
			
			// 手动抓图
			snapShotButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {		
					snapShot(chnComboBox.getSelectedIndex());
				}
			});
		}
	}
	
	/**
	 * 主界面组件
	 */
	private JFrame mainFrame; 
	
	/**
	 * 登录条组件
	 */
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;
	
	private JTextField ipTextArea;
	private JTextField portTextArea;
	
	private LoginPanel loginJPanel; 
	
	/**
	 * 事件信息显示组件
	 */
	private MessagePanel messagePanel;
	private JTextArea messageTextArea;
	
	/**
	 * 实时预览组件
	 */
	private RealPlayPanel realPlayPanel;
	private Panel realplayWindow;
	private JButton attachButton;
	private JButton realplayButton;
	private JComboBox chnComboBox;
	
	/**
	 * 事件及图片组件
	 */
	private EventInfoPanel eventInfoPanel;
	private PaintPanel SnapImagePanel;
	private PaintPanel plateImagePanel;
	private JLabel snapIamgeLabel;
	private JLabel plateImageLabel;
	
	private JLabel plateLicenseLabel;
	private JLabel plateColorLabel;
	private JLabel vehicleColorLabel;
	private JLabel snapTimeLabel;
	private JLabel laneNumberLabel;
	
	private JTextField plateLicenseTextField;
	private JTextField plateColorTextField;
	private JTextField vehicleColorTextField;
	private JTextField snapTimeTextField;
	private JTextField laneNumberTextField;
	
	private JButton snapPicButton;
	private JButton snapShotButton;
}

public class TrafficEvent {		

	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		ITSEventMsg demo = new ITSEventMsg();
		/*NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO msg=new NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO();
		Field f=Unsafe.class.getDeclaredField("theunsafe");
		Unsafe unsafe=(Unsafe) f.get(null);
		long sztext=unsafe.objectFieldOffset(NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO.class.getDeclaredField("szText"));*/
	}	
}
