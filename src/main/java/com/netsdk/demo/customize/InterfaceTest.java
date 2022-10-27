package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 接口测试主界面
 * @author 29779
 */
class InterfaceTest extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;
    private SDKEnvironment sdkEnv;
    
    // 登录参数	
 	private String address = "10.34.3.164";//172.23.12.19
 	private Integer port = new Integer("37777");
 	private String username = "admin";
 	private String password = "admin";//admin11111
 	// 设备信息
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
    
	private LLong loginHandle = new LLong(0); // 登录句柄
	
	// IVS < 视频分析通道, 设备信息 > 关联表
	private Map<Integer, IVSRemoteDevInfo> m_channelDevMap = new HashMap<Integer, IVSRemoteDevInfo>();
	
	// IVS 定制
	private ExecutorService 	exeService; // 查询远程相关信息线程池

	// 示例单元
	private IVSEventCase ivsEventCase;
	private RealPlayCase realPlayCase;
	private IVSRemoteDeviceCase ivsRemoteDeviceCase;
	private SubscribleAlarmCase subscribleAlarmCase;

	public InterfaceTest() {
		exeService = Executors.newFixedThreadPool(3);
		
		sdkEnv = new SDKEnvironment();
		sdkEnv.init(); // sdk 库初始化
		
		setSize(750, 640);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);	
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		loginJPanel = new LoginPanel();
		realPlayPanel = new RealPlayPanel();
		messagePanel = new MessagePanel();
		testCasePanel = new TestCasePanel();
		
		add(loginJPanel, BorderLayout.NORTH);
		add(realPlayPanel, BorderLayout.CENTER);
		add(testCasePanel, BorderLayout.EAST);
		add(messagePanel, BorderLayout.SOUTH);
		
		WindowAdapter closeAdapter = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Window Closing");			
				
				//登出
				LogOutButtonPerformed(null);
				
				sdkEnv.cleanup();
				dispose();
				
				exeService.shutdown();
			}
		};
		addWindowListener(closeAdapter);
	}
	
	///////////////// sdk 相关信息 /////////////////
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
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\Interface_" + System.currentTimeMillis() + ".log";
			
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
	        }
	    }
		
		// 网络连接恢复，设备重连成功回调
		// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	    public class HaveReConnect implements fHaveReConnect {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
	        }
	    }
	}

	/**
	 * 结束调用
	 */
	private void StopTask() {
		System.out.println("Stop Tasks");
		
		if (ivsEventCase != null) {
			ivsEventCase.stop();
		}
		
		if (realPlayCase != null) {
			realPlayCase.stop();
		}
		
		if (subscribleAlarmCase != null) {
			subscribleAlarmCase.stop();
		}
		
		// 结束监视
	}
	///////////////// 面板  //////////////////
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
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public LoginPanel() {
			loginBtn = new JButton("登入");
			logoutBtn = new JButton("登出");
			nameLabel = new JLabel("用户名");
			passwordLabel = new JLabel("密码");
			nameTextArea = new JTextField(username, 6);
			passwordTextArea = new JPasswordField(password, 6);
			ipLabel = new JLabel("设备地址");
			portLabel = new JLabel("端口号");
			ipTextArea = new JTextField(address, 8);
			portTextArea = new JTextField(port.toString(), 4);
			
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
			
			logoutBtn.setEnabled(false);
			// 登录按钮. 监听事件
			loginBtn.addActionListener(new ActionListener() {		
				public void actionPerformed(ActionEvent e) {
					LoginButtonPerformed(e);
				}
			});
			
			// 登出按钮. 监听事件
			logoutBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LogOutButtonPerformed(e);
				}
			});	
		}
	}
	
	/**
	 * 消息面板
	 */
	private class MessagePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public MessagePanel() {
			setBorderEx(this, "信息提示", 2);
			
			Dimension dim = getPreferredSize();
			dim.height = 150;
			setPreferredSize(dim);
			setLayout(new BorderLayout());
			
			messageTextArea = new JTextArea();	
			
			JScrollPane scrollPane = new JScrollPane(messageTextArea);
			add(scrollPane, BorderLayout.CENTER);
		}
	}

	/**
	 * 预览面板
	 */
	private class RealPlayPanel extends PaintPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RealPlayPanel() {
			setBorderEx(this, "容器", 2);
			setLayout(new BorderLayout());
			Dimension dim = getPreferredSize();
			dim.height = 319;
			dim.width = 390;
			setPreferredSize(dim);
			
			realplayWindow = new Panel();
			add(realplayWindow, BorderLayout.CENTER);
		}
	}
	
	/**
	 * 测试用例面板
	 */
	private class TestCasePanel extends JPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TestCasePanel() {
			setBorderEx(this, "用例面板", 2);
			setLayout(new BorderLayout(30, 5));
			Dimension dim = getPreferredSize();
			dim.height = 200;
			dim.width = 250;
			setPreferredSize(dim);
			
			// Panel for Testing Items			
			DefaultListModel caseList = new DefaultListModel();
			caseList.addElement("0-实时监视");
			caseList.addElement("1-订阅智能事件");
			caseList.addElement("2-IVS查询远程设备信息");
			caseList.addElement("3-订阅报警信息");
			
			testJList = new JList(caseList);
			testJList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION); // 支持一行操作
			testJList.setSelectedIndex(0);
			testJList.setBorder(new LineBorder(null));
							
			// Panel for Buttons 
			JPanel btnJPanel = new JPanel();
			btnJPanel.setLayout(new FlowLayout());
			startTestButton = new JButton("开始测试");
			stopTestButton = new JButton("结束测试");
			
			startTestButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					startTestButtonPerformed(testJList.getSelectedIndex());
				}
			});
			
			stopTestButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stopTestButtonPerformed(testJList.getSelectedIndex());
				}
			});
			
			btnJPanel.add(startTestButton);
			btnJPanel.add(stopTestButton);
			
			add(testJList, BorderLayout.CENTER);
			add(btnJPanel, BorderLayout.SOUTH);
		}
	}
	
	///////////////// 事件执行 /////////////////////
	/**
	 * 登录按钮事件
	 */
	private void LoginButtonPerformed(ActionEvent e) {		
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
        	int error = 0;
        	error = netsdkApi.CLIENT_GetLastError();
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[0x%x]\n" , address , port , error);
            JOptionPane.showMessageDialog(this, "登录失败，错误码  : " + String.format("[0x%x]", error));
        }
        else
        {
        	System.out.println("Login Success [ " + address +" ]"
        						+ " 设备通道数 " + deviceInfo.byChanNum);
        	JOptionPane.showMessageDialog(this, "登录成功");
        	logoutBtn.setEnabled(true);
        	loginBtn.setEnabled(false);
        }
	}
	
	/**
	 * 登出按钮事件
	 */
	private void LogOutButtonPerformed(ActionEvent e) {
		if (loginHandle.longValue() != 0) {
			System.out.println("LogOut Button Action");
    		
			// 结束调用接口
			StopTask();
			
    		if (netsdkApi.CLIENT_Logout(loginHandle)) {
	    		System.out.println("Logout Success [ " + address +" ]");
    			loginHandle.setValue(0);
    			logoutBtn.setEnabled(false);
	        	loginBtn.setEnabled(true);
	        	testJList.setEnabled(true);
	        	startTestButton.setEnabled(true);
	        	stopTestButton.setEnabled(false);
    		}    		
    	}
	}
	
	/**
	 * 开始测试按钮事件
	 */
	private void startTestButtonPerformed(int index) {
		if (loginHandle.longValue() == 0) {
			System.err.println("Please login first");
			JOptionPane.showMessageDialog(this, "请先登录");
			return;
		}
		
		boolean bRet = false;		
		switch(index)
		{
		case 0: // 实时监视
			messageTextArea.append("开启实时监视\n");
			realPlayCase = new RealPlayCase(loginHandle);
			bRet = realPlayCase.start();
			break;
		case 1: // 智能事件
			messageTextArea.append("订阅智能事件\n");
			ivsEventCase = new IVSEventCase(loginHandle);
			bRet = ivsEventCase.start();
			break;

		case 2: // 查询IVS远程设备信息
			messageTextArea.append("IVS 查询远程设备信息\n");
			ivsRemoteDeviceCase = new IVSRemoteDeviceCase(loginHandle, deviceInfo.byChanNum);
			bRet = ivsRemoteDeviceCase.GetRemoteDeivceInfo();
//			exeService.submit(new RelatedRemoteDevInfo(loginHandle, 0, "Test Msg\n"));
			break;
		case 3: // 订阅报警信息
			messageTextArea.append("订阅报警信息\n");
			subscribleAlarmCase = new SubscribleAlarmCase(loginHandle);
			bRet = subscribleAlarmCase.start();
			break;
		
		default:
			break;
		}
		
		if (bRet) {
			stopTestButton.setEnabled(true);
			startTestButton.setEnabled(false);
			testJList.setEnabled(false);
		}
	}
	
	/**
	 * 结束测试按钮事件
	 */
	private void stopTestButtonPerformed(int index) {
		if (loginHandle.longValue() == 0) {
			System.err.println("Please login first");
			JOptionPane.showMessageDialog(this, "请先登录");
			return;
		}
		
		boolean bRet = false;	
		switch(index) {
		case 0:
			bRet = realPlayCase.stop();
			break;
		case 1:
			bRet = ivsEventCase.stop();
			break;
		case 3: // 订阅报警信息
			messageTextArea.append("停止订阅报警信息\n");
			subscribleAlarmCase = new SubscribleAlarmCase(loginHandle);
			bRet = subscribleAlarmCase.stop();
			break;
		default:
			break;
		}
		
		if (bRet) {
			stopTestButton.setEnabled(false);
			startTestButton.setEnabled(true);
			testJList.setEnabled(true);
		}
	}
	
	////////////////// 示例  //////////////////
	/**
	 * 实时预览示例
	 */
	private class RealPlayCase {
		private LLong 			hLoginHandle; // 登录句柄
		private LLong 			m_hPlayHandle;// 预览句柄
		
		public RealPlayCase(LLong hLoginHandle) {
			this.hLoginHandle = hLoginHandle;
			m_hPlayHandle = new LLong(0);
		}
		
		/**
		 * 开始拉流
		 */
		public boolean start() {
			if (hLoginHandle.longValue() == 0) {
				System.err.println("Please login first");
				return false;
			}
			
			int channel = 0; // 预览通道号， 设备有多通道的情况，可手动更改
			int playType = NET_RealPlayType.NET_RType_Realplay; // 实时预览
			
			m_hPlayHandle = netsdkApi.CLIENT_RealPlayEx(hLoginHandle, channel, Native.getComponentPointer(realplayWindow), playType);
			if (m_hPlayHandle.longValue() == 0) {
				int error = netsdkApi.CLIENT_GetLastError();
				System.err.println("开始实时监视失败，错误码：" + String.format("[0x%x]", error));
				return false;
			}
			else {
				System.out.println("Success to start realplay");
				realplayWindow.setVisible(true);
				realPlayPanel.setOpaque(false);
				realPlayPanel.repaint();
				
				return true;
			}
		} 
		
		/**
		 * 结束拉流
		 */
		public boolean stop() {
			if (m_hPlayHandle.longValue() == 0) {
				System.out.println("Make sure the realplay Handle is valid");
				return false;
			}
			
			if (netsdkApi.CLIENT_StopRealPlayEx(m_hPlayHandle)) {
				System.out.println("Success to stop realplay");
				
				realPlayPanel.setOpaque(true);
				realPlayPanel.repaint();
				
				m_hPlayHandle.setValue(0);
				realplayWindow.repaint();
				
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * 智能报警事件示例
	 */
	private class IVSEventCase {
		private LLong 			hLoginHandle; // 登录句柄
		private LLong 			m_hAttachHandle; // 事件订阅句柄
		private fAnalyzerDataCB    	m_AnalyzerDataCB; // 智能事件回调
		
		//IVS 定制
		private ExecutorService 	executorService; // 查询远程相关信息线程池
		
		public IVSEventCase(LLong hLoginHandle) {
			this.hLoginHandle = hLoginHandle;
			m_AnalyzerDataCB = new fAnalyzerDataCB();
			m_hAttachHandle = new LLong(0);
			
			executorService = Executors.newFixedThreadPool(3);
		}
		
		/**
		 * 开始订阅智能事件
		 */
		public boolean start() {
			boolean bRet = false;
			
			if (hLoginHandle.longValue() == 0 ) {
				System.err.println("Please Login First");
				return false;
			}
			
			/**
			 * 说明：
			 * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
			 *  下列仅订阅了0通道的智能事件.
			 *  订阅IVS-IP7200全部通道需要轮训调用 CLIENT_RealLoadPictureEx
			 */
			int bNeedPicture = 1; // 是否需要图片
			int ChannelId = 0; // 0 通道 

	        m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(hLoginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , m_AnalyzerDataCB , null , null);
	        if( m_hAttachHandle.longValue() != 0  )
	        {
	            System.out.println("CLIENT_RealLoadPictureEx Success\n");
	            bRet = true;
	        }
	        else
	        {
	        	bRet = false;
	            System.out.printf("CLIENT_RealLoadPictureEx Failed!LastError = %x\n", netsdkApi.CLIENT_GetLastError() );
	        }
			
			return bRet;
		}
		
		/**
		 * 结束订阅智能事件
		 */
		public boolean stop() {
			
			if (0 != m_hAttachHandle.longValue()) {
	            netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
	            System.out.println("Stop detach IVS event");
	            m_hAttachHandle.setValue(0);
	            return true;
	        }
			
			if (null != executorService)
			{
				executorService.shutdownNow();
			}
			
			return false;
		}	
	
		/* 智能报警事件回调 */
	    class fAnalyzerDataCB implements fAnalyzerDataCallBack {
	        private String m_imagePath;
	        NET_MSG_OBJECT m_stuObject; 	// 物体信息
	        NET_TIME_EX utc; // 事件时间
	       
	        String EventMsg; // 事件信息        
	        
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
	            
	            // 获取事件信息        
	            m_stuObject = new NET_MSG_OBJECT();
	            utc = new NET_TIME_EX(); // 事件时间
	            
	            // 解析事件
	            if (GetStuObject(dwAlarmType, pAlarmInfo)) {
	            	// 保存图片
	            	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
	            }
	            
	            switch (dwAlarmType) {
	            // 称重平台检测事件(对应 DEV_EVENT_WEIGHING_PLATFORM_DETECTION_INFO)
				case NetSDKLib.EVENT_IVS_WEIGHING_PLATFORM_DETECTION: {
					DEV_EVENT_WEIGHING_PLATFORM_DETECTION_INFO msg = new DEV_EVENT_WEIGHING_PLATFORM_DETECTION_INFO();
					ToolKits.GetPointerData(pAlarmInfo, msg);
					
					System.out.println("通道号" + msg.nChannelID +
							"事件名称 :" + new String(msg.szName).trim() + 
							"发生时间:" + msg.UTC.toStringTime() + 
							"水果信息个数:" + msg.nCandidateFruitNum);
					
					for(int i = 0; i < msg.nCandidateFruitNum; i++)
					{
						System.out.println("水果" + i + ":" +
								"相似度 :" + msg.stuFruitInfos[i].nSimilarity + 
								"水果类型:" + msg.stuFruitInfos[i].emFruitType);
					}
					
					File path = new File("./AccessPicture/");
		            if (!path.exists()) {
		                path.mkdir();
		            }
		            String snapPicPath = path + "\\" + System.currentTimeMillis() + "AccessSnapPicture.jpg";  // 保存图片地址
		            byte[] buffer = pBuffer.getByteArray(0, dwBufSize);
	    			ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(buffer);
	    			try {
	    				BufferedImage bufferedImage = ImageIO.read(byteArrInputGlobal);
	    				if(bufferedImage != null) {
	    					ImageIO.write(bufferedImage, "jpg", new File(snapPicPath));
	    					System.out.println("抓拍图片保存路径：" + snapPicPath);
	    				}	    				
	    			} catch (IOException e2) {
	    				e2.printStackTrace();
	    			}
					break;
				}
	            default:
	            	break;
	            }

	            // 更新界面
	            // messageTextArea.append(EventMsg);
	                    
	            return 0;
	        }
	        
	        // 获取识别对象 车身对象 事件发生时间 车道号等信息
	        private boolean GetStuObject(int dwAlarmType, Pointer pAlarmInfo)
	        {
	        	boolean bRet = true;
	        	if(pAlarmInfo == null) {
	        		return false;
	        	}
	        	
	        	int channel = -1;
	        	System.out.printf("======>"+dwAlarmType);
	        	switch(dwAlarmType)
	            {
	        		
		            case NetSDKLib.EVENT_IVS_STAYDETECTION : // 停留事件
			        {
			        	System.out.printf("【停留事件】\n");
			        	DEV_EVENT_STAY_INFO msg = new DEV_EVENT_STAY_INFO();
			        	ToolKits.GetPointerData(pAlarmInfo, msg);
//			        	stuFileInfo = msg.stuFileInfo;
//			        	stPicInfo = msg.stuObject.stPicInfo;
			        	System.out.printf("【停留事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s \n", 
			        			msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime);
			        	break;
			        }
		        	case NetSDKLib.EVENT_IVS_FACEDETECT: /// 人脸检测事件
		        	{
		        		DEV_EVENT_FACEDETECT_INFO msg = new DEV_EVENT_FACEDETECT_INFO();
		        		ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = msg.stuObject;
	                    utc = msg.UTC;
	                    EventMsg = 	">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID +"; 人脸检测事件";
		        		break;
		        	}
		        	case NetSDKLib.EVENT_IVS_FACERECOGNITION:  ///< 人脸识别事件
					{
	                	DEV_EVENT_FACERECOGNITION_INFO msg = new DEV_EVENT_FACERECOGNITION_INFO();
	                    ToolKits.GetPointerData(pAlarmInfo, msg);  
	                    
	                    System.out.println("szName : " + new String(msg.szName).trim() + "\n" );
						break;
					}
	                case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION: // 警戒线事件
	                {
	                	DEV_EVENT_CROSSLINE_INFO msg = new DEV_EVENT_CROSSLINE_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = msg.stuObject;
	                    utc = msg.UTC;
	                    EventMsg = 	">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID +"; 警戒线事件";
	                    if (msg.bDirection == 1) {//表示入侵方向, 0-由左至右, 1-由右至左
	                    	EventMsg += "; 入侵方向: 由右至左";
	                    }
	                    else {
	                    	EventMsg += "; 入侵方向: 由左至右";
	                    }
	                    
//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                	break;
	                }
	                case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: // 警戒区事件
	                {
	                	DEV_EVENT_CROSSREGION_INFO msg = new DEV_EVENT_CROSSREGION_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = msg.stuObject;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 警戒区事件; ";
	                    
	                    String[] Dir = {"进入", "离开" , "出现" , "消失"};
	                    
	                    if (msg.bDirection >= 0 && msg.bDirection < Dir.length) {// 0-进入, 1-离开,2-出现,3-消失
	                    	EventMsg += Dir[msg.bDirection];
	                    }
	                    
	                    EventMsg += "; nObjectNum = " + msg.nObjectNum;

//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo(); 
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_WANDERDETECTION: // 徘徊事件
	                {
	                	DEV_EVENT_WANDER_INFO msg = new DEV_EVENT_WANDER_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                	m_stuObject = null;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 徘徊事件";

//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_FIGHTDETECTION: // 斗殴事件
	                {
	                	DEV_EVENT_FIGHT_INFO msg = new DEV_EVENT_FIGHT_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = null;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 斗殴事件";

//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_AUDIO_ABNORMALDETECTION: //  声音异常检测
	                {
	                	DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO msg = new DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = null;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 声音异常检测";
	                    EventMsg += "; 声音强度 " + msg.nDecibel;

//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_CLIMBDETECTION: // 攀高检测事件
	                {
	                	DEV_EVENT_IVS_CLIMB_INFO msg = new DEV_EVENT_IVS_CLIMB_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = msg.stuObject;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 攀高检测事件";
	                    
//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_LEAVEDETECTION: // 离岗检测事件
	                {
	                	DEV_EVENT_IVS_LEAVE_INFO msg = new DEV_EVENT_IVS_LEAVE_INFO();
	                	ToolKits.GetPointerData(pAlarmInfo, msg);
	                    m_stuObject = msg.stuObject;
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 离岗检测事件";
	                    
//	                    EventMsg += m_channelDevMap.get(new Integer(msg.nChannelID)).getInfo();
	                    channel = msg.nChannelID;
	                    break;
	                }
	                case NetSDKLib.EVENT_IVS_TRAFFIC_FCC: // 加油站提枪、挂枪事件
	                {
	                    DEV_EVENT_TRAFFIC_FCC_INFO msg = new DEV_EVENT_TRAFFIC_FCC_INFO();
	                    ToolKits.GetPointerData(pAlarmInfo, msg);
	                    utc = msg.UTC;
	                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 加油站提枪、挂枪事件";
	                    EventMsg += "; 车牌号 " + new String(msg.szText).trim();
	                    EventMsg += "; nLitre " + msg.nLitre + "; dwMoney " + msg.dwMoney;
	                    break;
	                }
	                default:
	                	bRet = false;
	                    System.out.printf("Get Event 0x%x\n", dwAlarmType);
	                    EventMsg = ">> " + "未处理事件 dwAlarmType = " + String.format("0x%x", dwAlarmType); 
	                    break;
	            }
	        	
	        	EventMsg += "\n";
	        	
	        	// 添加到线程池中
	        	if (channel >= 0)
	        	{
		        	executorService.submit(new RelatedRemoteDevInfo(hLoginHandle, channel, EventMsg));
	        	}
	        	
	        	return bRet;
	        }
	        
	        // 2014年后，陆续有设备版本，支持单独传车牌小图，小图附录在pBuffer后面。
	        private void SavePlatePic(NET_MSG_OBJECT stuObject, Pointer pBuffer, int dwBufSize) {
	        	// 清空
	        	realPlayPanel.setOpaque(true);
				realPlayPanel.repaint();         	
	        	
				// 保存大图
	        	if (pBuffer != null && dwBufSize > 0) {
	            	bigPicture = m_imagePath + "Big_" + UUID.randomUUID().toString() + ".jpg";
	            	ToolKits.savePicture(pBuffer, 0, dwBufSize, bigPicture);
	                
	                try {
	            		File bigFile = new File(bigPicture);
	            		Image snapImage = ImageIO.read(bigFile);
	            		realplayWindow.setVisible(false);
	            		realPlayPanel.setOpaque(false);
	        			realPlayPanel.setImage(snapImage);
	        			realPlayPanel.repaint(); 			
	    			} catch (Exception e) {
	    				e.printStackTrace();
	    			}
	        	}
	        	
	        	// 保存小图
	        	if (stuObject == null) {
	        		return;
	        	}
	        	if (stuObject.bPicEnble == 1) {
	        		//根据pBuffer中数据偏移保存小图图片文件
	        		int picLength = stuObject.stPicInfo.dwFileLenth;
	        		if (picLength > 0) {
	            		smallPicture = m_imagePath + "small_" + UUID.randomUUID().toString() + ".jpg";
	            		ToolKits.savePicture(pBuffer, stuObject.stPicInfo.dwOffSet, picLength, smallPicture);
	        		}
	        	}	
	        }
	    
	    }
	}
	
	/**
	 * 查询IVS远程设备
	 * @author 29779
	 */
    private class RelatedRemoteDevInfo implements Runnable {
    	private LLong hLoginHandle;
    	private int 	channel;
    	private String  msgString;
		public void run() {
			System.out.println("Get information: " + Thread.currentThread().getName());
			
			int nQueryType = NetSDKLib.NET_QUERY_IVS_REMOTE_DEVICE_INFO;			
			NET_IN_IVS_REMOTE_DEV_INFO inParam = new NET_IN_IVS_REMOTE_DEV_INFO();
			inParam.nChannel = channel;
			NET_OUT_IVS_REMOTE_DEV_INFO outParam = new NET_OUT_IVS_REMOTE_DEV_INFO();
			
			inParam.write();
			outParam.write();
			boolean bRet = netsdkApi.CLIENT_QueryDevInfo(hLoginHandle, nQueryType, inParam.getPointer(), outParam.getPointer(), null, 3000);
			if (bRet) {
				inParam.read();
				outParam.read();
				try {
					synchronized (messageTextArea) {
						String msgInfo = "用户名： " + new String(outParam.szUser).trim() + "\n"
								+ "密码： " + new String(outParam.szPassword).trim() + "\n"
								+ "设备IP：" + new String(outParam.szIP).trim() + "\n"
								+ "设备Port：" + outParam.nPort + "\n"
								+ "部署地点： " + new String(outParam.szAddress).trim() + "\n";
						System.out.println(msgInfo);
						
						messageTextArea.append(msgString + msgInfo);
					}				
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				System.err.println("LastErrorCode = " + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
			}
		}
		
		public RelatedRemoteDevInfo(LLong hLogin, int channel, String msgString) {
			this.hLoginHandle = hLogin;
			this.channel = channel;
			this.msgString = msgString;
		}
    }

    /**
     * 远程设备真实通道及其他信息
     */
    private class IVSRemoteDevInfo{
    	private AV_CFG_RemoteDevice remoteDevice; // 设备信息
    	private int realChannel; // 前端IPC通道号
    	
    	IVSRemoteDevInfo(AV_CFG_RemoteDevice deviceInfo, int channel) {
    		this.remoteDevice = deviceInfo;
    		this.realChannel = channel;
    	}
    	
    	String getInfo() {
    		String devName = new String(remoteDevice.szName).trim();
    		String msgString = "\n用户名： " + new String(remoteDevice.szUser).trim() + "\n"
					+ "密码： " + new String(remoteDevice.szPassword).trim() + "\n"
					+ "设备IP：" + new String(remoteDevice.szIP).trim() + "\n"
					+ "设备Port：" + remoteDevice.nPort + "\n"
					+ "设备名：" + devName + "\n"
					+ "部署地点： " + new String(remoteDevice.szAddress).trim() + "\n"
					+ "前端通道号: " + realChannel + "\n";
    		return msgString;
    	}
    }

    /**
     * 获取IVS设备远程设备信息示例
     */
    private class IVSRemoteDeviceCase{
    	private LLong 			hLoginHandle; // 登录句柄
    	private int					nMaxChannel;  // 服务器最大视频分析通道数
    	private Map<String, AV_CFG_RemoteDevice> RemoteDevMap;
		
		public IVSRemoteDeviceCase(LLong hLoginHandle, int maxChannel) {
			this.hLoginHandle = hLoginHandle;
			this.nMaxChannel = maxChannel;
			this.RemoteDevMap = new HashMap<String, AV_CFG_RemoteDevice>();
			this.RemoteDevMap.clear();
		}
		/**
		 * 获取远程设备信息
		 * @return
		 */
		public boolean GetRemoteDeivceInfo() {
			int chn = -1;  // 通道号
			int remoteDevCount = 10; // 最大有 NetSDKLib.MAX_REMOTE_DEV_NUM, 这里仅取10个配置信息
			AV_CFG_RemoteDevice deviceInfo[] = new AV_CFG_RemoteDevice[remoteDevCount];
			for (int i = 0; i < remoteDevCount; ++ i) {
				deviceInfo[i] = new AV_CFG_RemoteDevice();
			}
			
			/// 获取服务器所有远程设备的信息
			int realCount = GetDevConfig(hLoginHandle, chn, NetSDKLib.CFG_CMD_REMOTEDEVICE, deviceInfo);
			for(int i = 0; i < realCount; ++ i)
			{
				String devName = new String(deviceInfo[i].szName).trim();
				System.out.println("用户名： " + new String(deviceInfo[i].szUser).trim() + "\n"
						+ "密码： " + new String(deviceInfo[i].szPassword).trim() + "\n"
						+ "设备IP：" + new String(deviceInfo[i].szIP).trim() + "\n"
						+ "设备Port：" + deviceInfo[i].nPort + "\n"
						+ "设备名：" + devName + "\n"
						+ "部署地点： " + new String(deviceInfo[i].szAddress).trim() + "\n"
						);
				RemoteDevMap.put(devName, deviceInfo[i]);
			}
			
			/// 获取服务器视频分析通道的信息，该通道和事件上报通道对应
			m_channelDevMap.clear();
			
			CFG_ANALYSESOURCE_INFO channelInfo = new CFG_ANALYSESOURCE_INFO();
			for(int channel = 0; channel < nMaxChannel; ++channel) {
				if (!GetDevConfig(hLoginHandle, channel, NetSDKLib.CFG_CMD_ANALYSESOURCE, channelInfo)) {
					continue;
				}
				
				String devName = new String(channelInfo.szRemoteDevice).trim();
				if (channelInfo.bEnable == 1) {			
					System.out.println("Channel = " + channel
						+ "\n 设备名称：" + devName
						+ "\n 前端设备的视频通道号：" + channelInfo.nChannelID
						+ "\n "
						);
				}
				
				// 添加到关联表中				
				AV_CFG_RemoteDevice devInfo = RemoteDevMap.get(devName);
				if (deviceInfo != null) {
					IVSRemoteDevInfo ivsRemoteDevInfo = new IVSRemoteDevInfo(devInfo, channelInfo.nChannelID);
					m_channelDevMap.put(new Integer(channel), ivsRemoteDevInfo);
				}	
			}
			
			return false;
		}
		
    }
    
    /**
     * 订阅报警信息示例
     */
    private class SubscribleAlarmCase {
    	private LLong 					hLoginHandle; 	// 登录句柄
		private fMessCallBack    	m_AlarmDataCB; 	// 事件回调
		
		public SubscribleAlarmCase(LLong hLoginHandle) {
			this.hLoginHandle = hLoginHandle;
			m_AlarmDataCB = new fAlarmDataCB();
		}
		
		/**
		 * 订阅报警信息
		 * @return
		 */
		public boolean start() {
			boolean bRet = false;
			
			if (hLoginHandle.longValue() == 0 ) {
				messageTextArea.append("Please Login First\r\n");
				return false;
			}
			
		    // 设置报警回调函数
			netsdkApi.CLIENT_SetDVRMessCallBack(m_AlarmDataCB, null);
			
			// 订阅报警
			bRet = netsdkApi.CLIENT_StartListenEx(hLoginHandle);
			if (!bRet) {
				messageTextArea.append(String.format("Subscrible alarm event failed! LastError = 0x%x\n", netsdkApi.CLIENT_GetLastError()));
			}
			else {
				messageTextArea.append("Subscrible alarm event success.\r\n");
			}
		
			return bRet;
		}
		
		/**
		 * 取消订阅报警信息
		 * @return
		 */
		public boolean stop() {
			// 停止订阅报警
		   	if (netsdkApi.CLIENT_StopListen(hLoginHandle)) {
		   		messageTextArea.append("Stop subscrible alarm event success.\r\n");
		   		return true;
		   	}
			
			return false;
		}
		
		/**
		 * 报警信息回调函数原形
		 */
		class fAlarmDataCB implements fMessCallBack{
		  	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,  NativeLong nDevicePort, Pointer dwUser){
		  		switch (lCommand)
		  		{
		  		case NetSDKLib.NET_ALARM_ALARM_EX2:
		  			ALARM_ALARM_INFO_EX2 stuALARM_ALARM_INFO_EX2 = new ALARM_ALARM_INFO_EX2();
		  			ToolKits.GetPointerData(pStuEvent, stuALARM_ALARM_INFO_EX2);
		  			
		  			messageTextArea.append("Channel is " + stuALARM_ALARM_INFO_EX2.nChannelID + "\r\n");
		  			messageTextArea.append("Action is " + stuALARM_ALARM_INFO_EX2.nAction + "\r\n");
		  			messageTextArea.append("Happend time is " + stuALARM_ALARM_INFO_EX2.stuTime.toStringTime() + "\r\n");
		  			messageTextArea.append("Sense type is " + stuALARM_ALARM_INFO_EX2.emSenseType + "\r\n");
		  			messageTextArea.append("Defence area type is " + stuALARM_ALARM_INFO_EX2.emDefenceAreaType + "\r\n");
		  			break;
		  		default:
		  			break;
		  		}
		  		
		  		return true;
			}
		}
		
    }
    
	///////////////// 组件 ////////////////////
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
	 * 登录组件
	 */
	private LoginPanel loginJPanel; 
	
	private JButton loginBtn;
	private JButton logoutBtn;
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;
	private JLabel ipLabel;
	private JLabel portLabel;
	private JTextField ipTextArea;
	private JTextField portTextArea;
	
	/**
	 * 消息提示组件
	 */
	private MessagePanel messagePanel;
	private JTextArea messageTextArea;
	
	/**
	 * 实时预览组件
	 */
	private RealPlayPanel realPlayPanel;
	private Panel realplayWindow;
	
	/**
	 * 用例组件
	 */
	private TestCasePanel testCasePanel;
	private JList 	testJList;
	private JButton startTestButton;
	private JButton stopTestButton;

	/*********** 常用接口 **************/
	/**
	 * 设置配置
	 * @param strCmd 命令
	 * @param cmdObject 命令相关类
	 * @param hLoginHandle 登录句柄
	 * @param nChn 通道号
	 * @return
	 */
	private boolean SetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
        boolean result = false;
    	int nBufferLen = 100*1024;
        byte szBuffer[] = new byte[nBufferLen];
        for(int i=0; i<nBufferLen; i++)szBuffer[i]=0;
        IntByReference error = new IntByReference(0);
        IntByReference restart = new IntByReference(0);
        
		cmdObject.write();
		if (configApi.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(),
				szBuffer, nBufferLen))
        {	
			cmdObject.read();
        	if( netsdkApi.CLIENT_SetNewDevConfig(hLoginHandle, strCmd , nChn , szBuffer, nBufferLen, error, restart, 3000))
        	{
        		result = true;
        	}
        	else
        	{
        		 System.out.printf("Set %s Config Failed! Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
	        	 result = false;
        	}
        }
        else
        {
        	System.out.println("Packet " + strCmd + " Config Failed!");
         	result = false;
        }
        
        return result;
    }

	/**
	 * 获取配置
	 * @param strCmd 命令
	 * @param cmdObject 命令相关类
	 * @param hLoginHandle 登录句柄
	 * @param nChn 通道号
	 * @return
	 */
	private boolean GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
		boolean result = false;
		IntByReference error = new IntByReference(0);
		int nBufferLen = 100*1024;
	    byte[] strBuffer = new byte[nBufferLen];
	   
	    if(netsdkApi.CLIENT_GetNewDevConfig( hLoginHandle, strCmd , nChn, strBuffer, nBufferLen,error,3000) )
	    {  
	    	cmdObject.write();
			if (configApi.CLIENT_ParseData(strCmd, strBuffer, cmdObject.getPointer(),
					cmdObject.size(), null))
	     	{
				cmdObject.read();
	     		result = true;
	     	}
	     	else
	     	{
	     		System.out.println("Parse " + strCmd + " Config Failed!");
	     		result = false;
		 	}
		 }
		 else
		 {
			 System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
			 result = false;
		 }
			
	     return result;
	  }

	/**
	 * 获取配置
	 * @param hLoginHandle 登录句柄
	 * @param nChn 通道
	 * @param strCmd 命令
	 * @param cmdObjects 对象数组
	 * @return 成功返回 有效数组个数
	 */
	private int GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure[] cmdObjects)
	{
		IntByReference error = new IntByReference(0);
		int nBufferLen = 100*1024;
	    byte[] strBuffer = new byte[nBufferLen];
	    
	    if(!netsdkApi.CLIENT_GetNewDevConfig(hLoginHandle, strCmd , nChn, strBuffer, nBufferLen, error, 3000))
	    {
	    	System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
	    	return -1;
	    }
	    
	    IntByReference retLength = new IntByReference(0);
	    int memorySize = cmdObjects.length * cmdObjects[0].size();
	    Pointer objectsPointer = new Memory(memorySize);
	    objectsPointer.clear(memorySize);
	    
	    ToolKits.SetStructArrToPointerData(cmdObjects, objectsPointer);
	    
		if (!configApi.CLIENT_ParseData(strCmd, strBuffer, objectsPointer, memorySize, retLength.getPointer())) {		     		
     		System.out.println("Parse " + strCmd + " Config Failed!");
     		return -1;
		}
		
		ToolKits.GetPointerDataToStructArr(objectsPointer, cmdObjects);
		
		return (retLength.getValue() / cmdObjects[0].size());
	}
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				InterfaceTest demo = new InterfaceTest();
				demo.setVisible(true);
			}
		});
	}
}
