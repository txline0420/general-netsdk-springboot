package com.netsdk.demo.customize;

import com.netsdk.lib.LastError;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.SdkStructure;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;


/**
 * OSD 配置
 * @author 29779
 *
 */
public class OsdConfig extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib netsdkApi     = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi     = NetSDKLib.CONFIG_INSTANCE;
    
	private SDKEnvironment sdkEnv;
	
	// 登录参数	
	private String address = "172.29.4.59";
	private Integer port = new Integer("37777");
	private String username = "admin";
	private String password = "admin123";
	// 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private LLong loginHandle 	= new LLong(0); 	// 登录句柄
	private LLong realPlayHandle = new LLong(0);   // 监控句柄
    
	/**
	 * JNAOSD 构造
	 */
	public OsdConfig() {
		sdkEnv = new SDKEnvironment();
		sdkEnv.init();
		
		setTitle("设置OSD");
		setSize(960, 600);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);

		loginPanel  = new LoginPanel();         // 登录面板
		controlPanel = new ControlPanel();      // 主控制面板		
		realPlayPanel = new RealPlayPanel(); // 播放面板
		add(loginPanel, BorderLayout.NORTH);
		add(controlPanel, BorderLayout.EAST);
		add(realPlayPanel, BorderLayout.CENTER);	
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("RealPlay Window Closing");			
				LogoutBtnPerformed();
				loginPanel.initLoginState();
	    		sdkEnv.cleanup();
				dispose();
			}
		});
		
		setVisible(true);
	}
	
	/**
	 * 登录面板
	 */
	private class LoginPanel extends JPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void initLoginState() {
			loginBtn.setEnabled(true);
			logoutBtn.setEnabled(false);
			startPlayBtn.setEnabled(false);
			stopPlayBtn.setEnabled(false);
			channelComboBox.setEnabled(false);
		}
		
		public void setLoginState(boolean bLogin) {
			channelComboBox.setEnabled(bLogin);
			loginBtn.setEnabled(!bLogin);			
			setPlayState(!bLogin);
			logoutBtn.setEnabled(bLogin);
		}
		
		public void setPlayState(boolean bPlay) {
			startPlayBtn.setEnabled(!bPlay);
			stopPlayBtn.setEnabled(bPlay);
		}
		
		public LoginPanel() {
			setLayout(new FlowLayout());
			setBorderEx(this, "", 2);
			
			loginBtn = new JButton("登入");
			logoutBtn = new JButton("登出");
			nameLabel = new JLabel("用户名");
			passwordLabel = new JLabel("密码");
			nameTextArea = new JTextField("admin", 8);
			passwordTextArea = new JPasswordField("admin", 8);
			ipLabel = new JLabel("设备地址");
			portLabel = new JLabel("端口号");
			ipTextArea = new JTextField(address, 8);
			portTextArea = new JTextField(port.toString(), 4);
			startPlayBtn = new JButton("开始预览");
			stopPlayBtn = new JButton("停止预览");
			channelLabel = new JLabel("通道号");
			channelComboBox = new JComboBox();
									
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
			add(startPlayBtn);
			add(stopPlayBtn);
			add(channelLabel);
			add(channelComboBox);			
			
			initLoginState();
			
			// 登录按钮. 监听事件
			loginBtn.addActionListener(new ActionListener() {		
				public void actionPerformed(ActionEvent e) {
					if (LoginBtnPerformed()) {
						JOptionPane.showMessageDialog(null, "登入成功");
						setLoginState(true);
						
						String[] channelStrings = new String[deviceinfo.byChanNum];
						for (int i = 0; i < channelStrings.length; ++i) {
							channelStrings[i] = String.valueOf(i);
						}
						channelComboBox.setModel(new DefaultComboBoxModel(channelStrings));
					}
					else {						
						JOptionPane.showMessageDialog(null, "登入失败");
						initLoginState();
					}
				}
			});
			
			// 登出按钮. 监听事件
			logoutBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (LogoutBtnPerformed()) {
						initLoginState();					
						realPlayWindow.repaint();						
					}					
				}
			});	
			
			// 实时预览，监听事件
			startPlayBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (startRealPlay()) {
						setPlayState(true);
					}
				}
			});
			
			// 停止预览，监听事件
			stopPlayBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (stopRealPlay()) {
						setPlayState(false);
					}
				}
			});
		}
	}
	
	/**
	 * 主控制面板 
	 */
	private class ControlPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private GridBagLayout gb = new GridBagLayout();
		private GridBagConstraints gbc = new GridBagConstraints();
		
		private JTextField[] lines = new JTextField[5];
		private JComboBox alignComboBox = new JComboBox();
		private JComboBox idComboBox = new JComboBox();
		private JTextField leftTextField = new JTextField(10);
		private JTextField topTextField = new JTextField(10);
		private JTextField rightTextField = new JTextField(10);
		private JTextField bottomTextField = new JTextField(10);
		
		private String[] alignStrings = {"无效的对齐方式", "左对齐", "X坐标中对齐", "Y坐标中对齐", "居中", 
				"右对齐", "按照顶部对齐", "按照底部对齐", " 按照左上角对齐", "换行对齐"};
		
		// 配置只有一份, 设置前, 需要先获取
		private NetSDKLib.NET_CFG_VideoWidget cfg = new NetSDKLib.NET_CFG_VideoWidget();
		private boolean isCfgGetted = false;
		
		private void addComponent(Component comp, int x, int y, int gridwidth, int gridheight) {
			add(comp);
			gbc.gridx = x;
			gbc.gridy = y;
			gbc.gridwidth = gridwidth;
			gbc.gridheight = gridheight;
			gb.setConstraints(comp, gbc);
		}
		
		private void initComponent() {
			for (int i = 0; i < lines.length; i++) {
				lines[i].setText(null);
			}
			
			leftTextField.setText(null);
			topTextField.setText(null);
			rightTextField.setText(null);
			bottomTextField.setText(null);
		}
	
		public ControlPanel() {
			for (int i = 0; i < lines.length; i++) {
				lines[i] = new JTextField(10);
			}
			
			setBorderEx(this, "OSD 叠加区域", 2);
			
			setLayout(gb);
			Dimension dim = getPreferredSize();
			dim.width = 400;
			setPreferredSize(dim);
						
			gbc.fill = GridBagConstraints.BOTH; // 横向、纵向扩大
			addComponent(new JLabel("叠加区域ID", JLabel.RIGHT), 0, 0, 1, 1);
			
			gbc.weightx = 1;
			addComponent(idComboBox, 2, 0, 1, 1);
			gbc.weightx = 0;
			addComponent(new JPanel(), 2, 0, 0, 1);
			
			gbc.weighty = 1;
			addComponent(new JPanel(), 0, 1, 0, 1);
			gbc.weighty = 0;
			
			addComponent(new JLabel("叠加区域字符最多支持5行"), 0, 2, 0, 1);
	
			addComponent(lines[0], 0, 3, 0, 1);
			addComponent(lines[1], 0, 4, 0, 1);
			addComponent(lines[2], 0, 5, 0, 1);
			addComponent(lines[3], 0, 6, 0, 1);
			addComponent(lines[4], 0, 7, 0, 1);
			
			gbc.weighty = 1;
			addComponent(new JPanel(), 0, 8, 0, 1);		
			gbc.weighty = 0;
			
			addComponent(new JLabel("位置：虚拟坐标长宽分别虚拟成8191，左上角为原点"), 0, 9, 0, 1);
			addComponent(new JLabel("对齐方式"), 0, 10, 1, 1);
			addComponent(alignComboBox, 1, 10, 0, 1);
			
			addComponent(new JLabel("Left", JLabel.CENTER), 0, 11, 1, 1);
			addComponent(leftTextField, 1, 11, 1, 1);
			addComponent(new JLabel("Top", JLabel.CENTER), 2, 11, 1, 1);
			addComponent(topTextField, 3, 11, 0, 1);

			addComponent(new JLabel("Right", JLabel.CENTER), 0, 12, 1, 1);
			addComponent(rightTextField, 1, 12, 1, 1);
			addComponent(new JLabel("Bottom", JLabel.CENTER), 2, 12, 1, 1);
			addComponent(bottomTextField, 3, 12, 0, 1);
			
			final JButton getButton = new JButton("获取");
			final JButton setButton = new JButton("配置");
			addComponent(getButton, 0, 13, 2, 1);
			addComponent(setButton, 2, 13, 0, 1);
			
			// 获取配置
			getButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setButton.setEnabled(false);
					if (getOSDConfig()) {
						setButton.setEnabled(true);
					}
				}
			});
			
			// 设置配置
			setButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getButton.setEnabled(false);
					if (setOSDConfig()) {
						getButton.setEnabled(true);
					}
				}
			});
		
			idComboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						System.out.println("Event Item " + idComboBox.getSelectedIndex());
						flushOSDPanel(idComboBox.getSelectedIndex());
					}
				}
			});
		}
		
		private synchronized void flushOSDPanel(int index) {
			if (index < 0) {
				return;
			}
			
			initComponent();
			
			String textString;
			try {
				textString = new String(cfg.stuCustomTitle[index].szText, "GBK").trim();
				String[] lineStrings = textString.split("\\|", 5); // 最大支持5行 
				for (int i = 0; i < lineStrings.length; i++) {
					System.out.println("line " + i + " " + lineStrings[i]);
					lines[i].setText(lineStrings[i]);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 显示第一个自定义标题的位置
			leftTextField.setText(String.valueOf(cfg.stuCustomTitle[index].stuRect.nLeft));
			rightTextField.setText(String.valueOf(cfg.stuCustomTitle[index].stuRect.nRight));
			topTextField.setText(String.valueOf(cfg.stuCustomTitle[index].stuRect.nTop));
			bottomTextField.setText(String.valueOf(cfg.stuCustomTitle[index].stuRect.nBottom));
			
			// 对齐方式
			alignComboBox.setModel(new DefaultComboBoxModel(alignStrings));
			alignComboBox.setSelectedItem(alignStrings[cfg.stuCustomTitle[index].emTextAlign]);
		}
		
		private synchronized boolean getOSDConfig() {
			int channel = channelComboBox.getSelectedIndex(); // 获取当前通道
			System.out.println("channel " + channel);
			
			if (!GetDevConfig( NetSDKLib.CFG_CMD_VIDEOWIDGET, cfg , loginHandle, channel))
			{
				System.err.printf("Get OSD Config Failed!");
				return false;
			}
			
			isCfgGetted = true;
			
			int index = idComboBox.getSelectedIndex() > 0 ? idComboBox.getSelectedIndex() : 0;
			
			
			System.out.println("index" + index);
			System.out.println("nConverNum " + cfg.nConverNum);
			System.out.println("nSensorInfo " + cfg.nConverNum);
			System.out.println("fFontSizeScale " + cfg.fFontSizeScale);
			
			// 自定义标题数量
			System.out.println("Title Count " + cfg.nCustomTitleNum);
			String[] idstrs = new String[cfg.nCustomTitleNum];
			for (int i = 0; i < idstrs.length; ++i) {
				idstrs[i] = String.valueOf(i);
			}
			idComboBox.setModel(new DefaultComboBoxModel(idstrs));
			idComboBox.setSelectedIndex(index);
			
			// 刷新界面
			flushOSDPanel(index);
			
			return true;
		}
		
		private synchronized boolean setOSDConfig() {
			if (!isCfgGetted) {
				System.out.println("先获取配置");
				return false;
			}
			
			// 清空原来的 cfg.stuCustomTitle[index].szText 内容
			for (int i = 0 ; i < cfg.stuCustomTitle.length; ++i) {
				ToolKits.ByteArrZero(cfg.stuCustomTitle[i].szText);
			}
			
			int channel = channelComboBox.getSelectedIndex(); // 获取当前通道
			int index = idComboBox.getSelectedIndex();
			System.out.println("idex " + index);
			
			// 自定义标题数量
			cfg.nCustomTitleNum = 0;
			String textstr = "";
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].getText().length() > 0) {
					cfg.nCustomTitleNum ++;
					if (0 == i) {
						textstr = lines[i].getText();
					}
					else {
						textstr = textstr +"|" + lines[i].getText();
					}
				}
			}
			
			System.out.println(" TitleNum "+ cfg.nCustomTitleNum + "Set Text " + textstr);
			
			byte[] gbkText = textstr.getBytes(Charset.forName("GBK"));
			
			int copylen = gbkText.length; // textstr 最后的"|"不要
			copylen = copylen > NetSDKLib.NET_CFG_Custom_Title_Len ? NetSDKLib.NET_CFG_Custom_Title_Len : copylen;
			System.arraycopy(gbkText, 0, cfg.stuCustomTitle[index].szText, 0, copylen);
			
			cfg.stuCustomTitle[index].bEncodeBlend = 1;
			cfg.stuCustomTitle[index].bEncodeBlendExtra1 = 0;
			cfg.stuCustomTitle[index].bEncodeBlendExtra2 = 0;
			cfg.stuCustomTitle[index].bEncodeBlendExtra3 = 0;
			cfg.stuCustomTitle[index].bEncodeBlendSnapshot = 1;
			cfg.stuCustomTitle[index].bPreviewBlend = 1;
			
			// 显示自定义标题的位置
			cfg.stuCustomTitle[index].stuRect.nLeft = Integer.parseInt(leftTextField.getText());
			cfg.stuCustomTitle[index].stuRect.nRight = Integer.parseInt(rightTextField.getText());
			cfg.stuCustomTitle[index].stuRect.nTop = Integer.parseInt(topTextField.getText());
			cfg.stuCustomTitle[index].stuRect.nBottom = Integer.parseInt(bottomTextField.getText());
			
			// 对齐方式
			cfg.stuCustomTitle[index].emTextAlign =	alignComboBox.getSelectedIndex();
			
			if (!SetDevConfig(NetSDKLib.CFG_CMD_VIDEOWIDGET, cfg , loginHandle, channel))
			{
				System.err.println("Failed to SetConfig");
				return false;
			}
			
			return true;
		}
		
		private boolean SetDevConfig(String strCmd ,  SdkStructure cmdObject , LLong hHandle , int nChn  )
	    {
	        boolean result = false;
	    	int nBufferLen = 100*1024;
	        byte szBuffer[] = new byte[nBufferLen];
	        for(int i=0; i<nBufferLen; i++)szBuffer[i]=0;
	        IntByReference error = new IntByReference(0);
	        IntByReference restart = new IntByReference(0);
	        
			cmdObject.write();		
			result = configApi.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(),szBuffer, nBufferLen);
			if (!result) {
				System.out.println("Packet " + strCmd + " Config Failed!");
				cmdObject.read();
				return result;
			}

			result = netsdkApi.CLIENT_SetNewDevConfig(hHandle,strCmd , nChn , szBuffer, nBufferLen, error, restart, 3000);
			if (!result) {
				System.out.printf("Set %s Config Failed! Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
			}
			
			cmdObject.read();
			
	        return result;
	    }
		
		private  boolean GetDevConfig(String strCmd ,  SdkStructure cmdObject , LLong hHandle , int nChn)
	    {
	    	IntByReference error = new IntByReference(0);
	    	int nBufferLen = 100*1024;
	        byte[] strBuffer = new byte[nBufferLen];
	       
	        boolean result = netsdkApi.CLIENT_GetNewDevConfig( hHandle, strCmd , nChn, strBuffer, nBufferLen, error,3000);
	        if (!result) {
	        	System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
	        	return false;
	        }

			cmdObject.write();
			result = configApi.CLIENT_ParseData(strCmd, strBuffer, cmdObject.getPointer(), cmdObject.size(), null);
			cmdObject.read();
	        
	        return true;
	    }
	}
	
	// 开始实时监视支持设置码流回调
    private boolean startRealPlay() {	
		int channel = channelComboBox.getSelectedIndex();
		realPlayHandle = netsdkApi.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(realPlayWindow), 0);		
		return ( 0!= realPlayHandle.longValue()) ? true : false;
    }
	
	// 结束实时监视
	private boolean stopRealPlay() {
		if (realPlayHandle == null || realPlayHandle.longValue() == 0) {
			return false;
		}
		
		return netsdkApi.CLIENT_StopRealPlayEx(realPlayHandle);
	}
	
	/**
	 * 播放面板
	 */
	private class RealPlayPanel extends JPanel {	
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean isSaving = false;
		JTextField fileNameField = new JTextField(20);
		
		public RealPlayPanel() {
			setLayout(new BorderLayout());
			setBorderEx(this, "播放窗口", 1);			
			
			realPlayWindow = new Panel();
			realPlayWindow.setBackground(new Color(153, 240, 255));
			add(realPlayWindow, BorderLayout.CENTER);
			
			startSaveVedioBtn = new JButton("开始录像");
			stopSaveVedioBtn = new JButton("停止录像");
			
			JPanel savePanel =  new JPanel();
			savePanel.setLayout(new FlowLayout());
			savePanel.add(new JLabel("录像名", JLabel.CENTER));
			savePanel.add(fileNameField);
			savePanel.add(startSaveVedioBtn);
			savePanel.add(stopSaveVedioBtn);
			
			add(savePanel, BorderLayout.SOUTH);
			
			startSaveVedioBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (fileNameField.getText().length() <= 0) {
						System.err.println("请输入文件名");
						return;
					}
					
					if (realPlayHandle.longValue() == 0) {
						System.err.println("请先开始预览");
						return;
					}
					
					if (!isSaving) {
						File path = new File(".");			
						String davPath = path.getAbsoluteFile().getParent();
						System.out.println("保存路径 " + davPath + fileNameField.getText()+".dav");
						netsdkApi.CLIENT_SaveRealData(realPlayHandle, fileNameField.getText()+".dav");
						isSaving = true;
					}
				}
			});
			
			stopSaveVedioBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (isSaving) {
						netsdkApi.CLIENT_StopSaveRealData(realPlayHandle);
						isSaving = false;
					}
				}
			}); 
		}
	}	
	
	/**
	 * 显示接口失败错误码
	 */
	public void showLastError(String interfaceStr) {
		System.out.printf("%s is Failed, Error - 0x%x\n", interfaceStr, netsdkApi.CLIENT_GetLastError());
	}
	
	/**
	 * 登录设备设备错误状态
	 */
	private String loginErrorCode(int err) {
		String msg;
		switch(err) {
		case LastError.NET_USER_FLASEPWD_TRYTIME: msg = "输入密码错误超过限制次数"; break;
		case LastError.NET_LOGIN_ERROR_PASSWORD:  msg = "密码不正确"; break;
		case LastError.NET_LOGIN_ERROR_USER: msg = "帐户不存在";break;
		case LastError.NET_LOGIN_ERROR_TIMEOUT: msg = "等待登录返回超时";break;
		case LastError.NET_LOGIN_ERROR_RELOGGIN: msg = "帐号已登录";break;
		case LastError.NET_LOGIN_ERROR_LOCKED:msg = "帐号已被锁定";break;
		case LastError.NET_LOGIN_ERROR_BLACKLIST:msg = "帐号已被列为禁止名单";break;
		case LastError.NET_LOGIN_ERROR_BUSY:msg = "资源不足,系统忙";break;
		case LastError.NET_LOGIN_ERROR_CONNECT:msg = "登录设备超时,请检查网络并重试";break;
		case LastError.NET_LOGIN_ERROR_NETWORK: msg = "网络连接失败";break;
		    default:
		    	msg = "请参考 NetSDKLib.java ";
		}
		
		return msg;
	}
		
	/**
	 * 登录按钮
	 */
	private boolean LoginBtnPerformed() {
		address = ipTextArea.getText();
		port = Integer.parseInt(portTextArea.getText());
		username = nameTextArea.getText();
		password = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + address + "\n端口号：" + port 
					+ "\n用户名：" + username + "\n密码：" + password);
		// 登录设备
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(address, port.intValue(), username , password , 0, null, deviceinfo, nError);
        if(loginHandle.longValue() == 0) {
            int errCode = netsdkApi.CLIENT_GetLastError(); 
            System.err.println("Device["+address+"] Port["+port+"] Failed. Error["+ loginErrorCode(errCode)+"]");
            return false;
        }
        
        System.out.println("Login Success ["+ address +"]");

        return true;
	}
		
	/**
	 * 登出按钮
	 */
	private boolean LogoutBtnPerformed() {
		if (loginHandle.longValue() == 0) {
			return false;
		}
		
		netsdkApi.CLIENT_Logout(loginHandle);
		System.out.println("Logout Success [ " + address +" ]");
		loginHandle.setValue(0);

		return true;
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
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\sdk.log";
			
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
			
			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
			// 接口设置的登录设备超时时间和尝试次数意义相同
			// 此操作为可选操作
			NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
			netParam.nConnectTime = 5000; // 登录时尝试建立链接的超时时间
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
	
	//////////////////////// Components of OSDFrame ///////////////
	/**
	 * 登录界面
	 */
	private LoginPanel loginPanel; 
	
	private JButton loginBtn;
	private JButton logoutBtn;
	private JButton startPlayBtn;
	private JButton stopPlayBtn;
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;
	private JLabel ipLabel;
	private JLabel portLabel;
	private JTextField ipTextArea;
	private JTextField portTextArea;
	private JLabel channelLabel;
	private JComboBox channelComboBox;
	private JButton startSaveVedioBtn;
	private JButton stopSaveVedioBtn;
	
	/*
	 * 控制界面
	 */
	private ControlPanel controlPanel;
	
	/**
	 * 播放界面
	 */
	private RealPlayPanel realPlayPanel;
	private Panel realPlayWindow;
		
	public static void main(String[] args) {
		OsdConfig osd = new OsdConfig();
		osd.setVisible(true);
	}
}
