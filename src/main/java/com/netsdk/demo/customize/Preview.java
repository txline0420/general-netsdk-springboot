package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Native;
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
 * 实时预览
 */
class Preview extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;
    
	private SDKEnvironment sdkEnv;
	
	// 登录参数	
	private String m_strIp             = "172.31.12.139";
	private Integer m_nPort            = new Integer("37777");
	private String m_strUser           = "admin";
	private String m_strPassword       = "admin";
	// 设备信息
    private NetSDKLib.NET_DEVICEINFO deviceInfo = new NetSDKLib.NET_DEVICEINFO();
	
	private LLong loginHandle = new LLong(0); 	// 登录句柄
	private LLong m_lOperateHandle1 = new LLong(0);    // 监控句柄
	private LLong m_lOperateHandle2 = new LLong(0);    // 监控句柄
    
	/**
	 * Preview 构造
	 */
	public Preview() {
		sdkEnv = new SDKEnvironment();
		sdkEnv.init();
		setTitle("实时监控");
		setSize(850, 550);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);
		setVisible(true);
		
		loginPanel  = new LoginPanel();           // 登录面板
		realPlayPanel1 	 = new RealPlayPanel1();   // 播放面板1		
		realPlayPanel2 	 = new RealPlayPanel2();   // 播放面板2
		
		add(loginPanel, BorderLayout.NORTH);
		add(realPlayPanel1, BorderLayout.WEST);
		add(realPlayPanel2, BorderLayout.EAST);	
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("RealPlay Window Closing");			
				LoginOutButtonPerformed();
				stopRealPlay1();
				stopRealPlay2();
	    		sdkEnv.cleanup();
				dispose();
			}
		});
	}
	
	/**
	 * NetSDK 库初始化
	 */
	private class SDKEnvironment { 
	    private boolean bInit = false;
	    private boolean bLogopen = false;
	    
	    private DisConnect 		 disConnect    = new DisConnect();    // 设备断线通知回调
	    private HaveReConnect    haveReConnect = new HaveReConnect(); // 网络连接恢复
	    
	    // 初始化
	    public boolean init() {    	
			// SDK 库初始化, 并设置断线回调
			bInit = netsdkApi.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}
			
			// 打开日志，可选
			NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
			
			File path = new File(".");			
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\RealPlay_" + System.currentTimeMillis() + ".log";
			
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
			NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
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
	    public class DisConnect implements NetSDKLib.fDisConnect  {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
	        }
	    }
		
		// 网络连接恢复，设备重连成功回调
		// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	    public class HaveReConnect implements NetSDKLib.fHaveReConnect {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
	        }
	    }
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
			setLayout(new FlowLayout());
			setBorderEx(this, "登录", 2);
			
			loginBtn = new JButton("登入");
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
			// 登录按钮. 监听事件
			loginBtn.addActionListener(new ActionListener() {		
				public void actionPerformed(ActionEvent e) {
					LoginButtonPerformed();
				}
			});
			
			// 登出按钮. 监听事件
			logoutBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LoginOutButtonPerformed();
					stopRealPlay1();
					stopRealPlay2();
					sdkEnv.cleanup();
				}
			});	
		}
	}
	
	/**
	 * 播放面板 1
	 */
	private class RealPlayPanel1 extends PaintPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RealPlayPanel1() {
			setBorderEx(this, "播放窗口1", 2);
			setLayout(new BorderLayout());
			Dimension dim = getPreferredSize();
			dim.width = 410;
			setPreferredSize(dim);	
			
			JPanel jp1 = new JPanel();
			stBtn1 = new JButton("开始监控1");
			endBtn1 = new JButton("停止监控1");
			label1 = new JLabel("通道号:");
			textArea1 = new JTextField("0", 8);
        	stBtn1.setEnabled(false);
        	endBtn1.setEnabled(false);
			
			jp1.setLayout(new FlowLayout());
			jp1.add(stBtn1);
			jp1.add(endBtn1);
			jp1.add(label1);
			jp1.add(textArea1);
			
			realPlayWindow1 = new Panel();
			add(jp1, BorderLayout.NORTH);
			add(realPlayWindow1, BorderLayout.CENTER);
			
			stBtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					startRealPlay1();					
				}
			});
			
			endBtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stopRealPlay1();			
				}
			});
		}
	}
	
	/**
	 * 播放面板2
	 */
	private class RealPlayPanel2 extends PaintPanel {		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RealPlayPanel2() {		
			setLayout(new BorderLayout());
			setBorderEx(this, "播放窗口2", 2);
			Dimension dim = getPreferredSize();
			dim.width = 410;
			setPreferredSize(dim);
			
			JPanel jp2 = new JPanel();
			stBtn2 = new JButton("开始监控2");
			endBtn2 = new JButton("停止监控2");
			label2 = new JLabel("通道号:");
			textArea2 = new JTextField("1", 8);
        	stBtn2.setEnabled(false);
        	endBtn2.setEnabled(false);
			
			jp2.setLayout(new FlowLayout());
			jp2.add(stBtn2);
			jp2.add(endBtn2);
			jp2.add(label2);
			jp2.add(textArea2);
			
			realPlayWindow2 = new Panel();
			add(jp2, BorderLayout.NORTH);
			add(realPlayWindow2, BorderLayout.CENTER);
			
			stBtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					startRealPlay2();					
				}
			});
			
			endBtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stopRealPlay2();				
				}
			});
		}
	}	
	
	//带背景的面板组件
	private class PaintPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Image image; //背景图片	
		
		public PaintPanel() {
			super();
			setOpaque(true); //非透明
			setLayout(null);
			setBackground(new Color(153, 240, 255));
			setForeground(new Color(0, 0, 0));
		}			
		
		protected void paintComponent(Graphics g) {    //重写绘制组件外观
			if(image != null) {
				g.drawImage(image, 0, 0, getWidth(), getHeight(), this);//绘制图片与组件大小相同
			}
			super.paintComponent(g); // 执行超类方法
		}	
	}

    //////////////////////// Border Extends //////////////////////
	/**
	 * 设置边框
	 */
	private void setBorderEx(JComponent object, String title, int width) {
		Border innerBorder = BorderFactory.createTitledBorder(title);
		Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);	
		object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
	}
	
	//////////////////////// Operations of PlayBackFrame ////////////////
	
	/**
	 * 登录按钮
	 */
	private void LoginButtonPerformed() {
		m_strIp = ipTextArea.getText();
		m_nPort = Integer.parseInt(portTextArea.getText());
		m_strUser = nameTextArea.getText();
		m_strPassword = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + m_strIp
						+  "\n端口号：" + m_nPort
						+  "\n用户名：" + m_strUser
						+  "\n密码：" + m_strPassword);
		
		// 登录设备
		IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx(m_strIp, m_nPort.intValue(), m_strUser , m_strPassword , 0, null, deviceInfo, nError);
        if(loginHandle.longValue() == 0)
        {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
            JOptionPane.showMessageDialog(this, "登录失败");
        }
        else
        {
        	System.out.println("Login Success [ " + m_strIp +" ]");
        	JOptionPane.showMessageDialog(this, "登录成功");
        	logoutBtn.setEnabled(true);
        	loginBtn.setEnabled(false);
        	stBtn1.setEnabled(true);
        	stBtn2.setEnabled(true);
        }
	}
		
	/**
	 * 登出按钮
	 */
	private void LoginOutButtonPerformed() {
		if (loginHandle.longValue() != 0) {
			System.out.println("LogOut Button Action");
  		
			stopRealPlay1();
			stopRealPlay2();
			
    		if (netsdkApi.CLIENT_Logout(loginHandle)) {
	    		System.out.println("Logout Success [ " + m_strIp +" ]");
    			loginHandle.setValue(0);
    			logoutBtn.setEnabled(false);
	        	loginBtn.setEnabled(true);
	        	stBtn1.setEnabled(false);
	        	endBtn1.setEnabled(false);
	        	stBtn2.setEnabled(false);
	        	endBtn2.setEnabled(false);
    		}    		
    	}
	}
	
	// 开始实时监视支持设置码流回调
    private void startRealPlay1() {
		int channel = Integer.parseInt(textArea1.getText()); // 预览通道号， 设备有多通道的情况，可手动更改
		int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览		
		
		m_lOperateHandle1 = netsdkApi.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(realPlayWindow1), playType);
		if (m_lOperateHandle1.longValue() == 0) {
			int error = netsdkApi.CLIENT_GetLastError();
			System.err.println("开始实时监视失败，错误码：" + String.format("[0x%x]", error));
		} else {
			System.out.println("Success to start realplay");
			realPlayWindow1.setVisible(true);
			realPlayPanel1.setOpaque(false);// 设置透明
			stBtn1.setEnabled(false);	
			endBtn1.setEnabled(true);
    	}
    }
    private void startRealPlay2() {
		int channel = Integer.parseInt(textArea2.getText()); // 预览通道号， 设备有多通道的情况，可手动更改
		int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览	
		
		m_lOperateHandle2 = netsdkApi.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(realPlayWindow2), playType);
		if (m_lOperateHandle2.longValue() == 0) {
			int error = netsdkApi.CLIENT_GetLastError();
			System.err.println("开始实时监视失败，错误码：" + String.format("[0x%x]", error));
		} else {
			System.out.println("Success to start realplay");
			realPlayWindow2.setVisible(true);
			realPlayPanel2.setOpaque(false);
			stBtn2.setEnabled(false);	
			endBtn2.setEnabled(true);
    	}
    }
	
	// 结束实时监视
	private void stopRealPlay1() {
		System.out.println("Stop Tasks!");

		if(!netsdkApi.CLIENT_StopRealPlayEx(m_lOperateHandle1)) {
			return;
		} else {
			System.out.println("StopRealPlay Succeed!");
			m_lOperateHandle1.setValue(0);
			realPlayWindow1.repaint();
			stBtn1.setEnabled(true);
			endBtn1.setEnabled(false);
		}		
	}
	private void stopRealPlay2() {
		System.out.println("Stop Tasks!");
	
		if(!netsdkApi.CLIENT_StopRealPlayEx(m_lOperateHandle2)) {
			return;
		} else {
			System.out.println("StopRealPlay Succeed!");
			m_lOperateHandle2.setValue(0);
			realPlayWindow2.repaint();
			stBtn2.setEnabled(true);
			endBtn2.setEnabled(false);
		}		
	}
	
	public void capturePicture() {
		File pathFile = new File(".");			
		String path = pathFile.getAbsoluteFile().getParent() + "\\RealPlay_" + System.currentTimeMillis() + ".jpg";
		if(netsdkApi.CLIENT_CapturePicture(m_lOperateHandle1, path)) {
			System.out.println(path);
		}
	}
	
	//////////////////////// Components of PlayBackFrame ///////////////
	/**
	 * 登录界面
	 */
	private LoginPanel loginPanel; 
	
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
	 * 播放界面
	 */
	private RealPlayPanel1 realPlayPanel1;
	private RealPlayPanel2 realPlayPanel2;
	private Panel realPlayWindow1;
	private Panel realPlayWindow2;
	private JLabel label1;
	private JTextField textArea1;
	private JLabel label2;
	private JTextField textArea2;
	private JButton stBtn1;
	private JButton endBtn1;
	private JButton stBtn2;
	private JButton endBtn2;
	
	public static void main(String[] args) {
		Preview demo = new Preview();
		demo.setVisible(true);
	}
}
