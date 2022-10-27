package com.netsdk.demo.example;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

class PtzControl{
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.23.1.32";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin";
	
	//视频设置参数
	private int m_nBrightness ;
	private int m_nContrast ;
	private int m_nHue  ;
	private int m_nSaturation ;

	//设备信息
	private NetSDKLib.NET_DEVICEINFO m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO();
	
	private static LLong m_hLoginHandle = new LLong(0);   //登陆句柄
	private LLong   m_hPlayHandle = new LLong(0);  //预览句柄    
	private boolean m_hVideo; //视频参数

	
	//////////////////SDK相关信息///////////////////////////
	//NetSDK 库初始化
	private class SDKEnvironment {
		
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
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\PtzControl" + System.currentTimeMillis() + ".log";
			
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
	}
		
	private SDKEnvironment sdkEnv;
	public PtzControl() {	
		sdkEnv = new SDKEnvironment();
	    sdkEnv.init();   //SDK 库初始化
	    
	    SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    mainFrame = new JFrame("带云台控制的JNADEMO");
			    mainFrame.setSize(800, 600);
			    mainFrame.setLayout(new BorderLayout());
			    mainFrame.setLocationRelativeTo(null);
				mainFrame.setVisible(true);
		        	    
			    loginPanel = new LoginPanel();
			    realPlayPanel = new RealPlayPanel();
			    ptzControlPanel = new PtzControlPanel();

			    mainFrame.add(loginPanel, BorderLayout.NORTH);
			    mainFrame.add(realPlayPanel, BorderLayout.CENTER);
			    mainFrame.add(ptzControlPanel, BorderLayout.EAST);
			    
			    mainFrame.addWindowListener(new WindowAdapter() {
			    	public void windowClosing(WindowEvent e) {
			    		System.out.println("Window Closing");
			    		//登出
			    		logoutButtonPerformed(null);
			    		
			    		sdkEnv.cleanup();
			    		mainFrame.dispose();	
			    		System.exit(0);
			    	}
			    });
			}
	    });
	}
	
	/////////////////面板///////////////////
	//////////////////////////////////////
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
		    
		    //登录按钮，监听事件
		    loginBtn.addActionListener(new ActionListener() {
		    	public void actionPerformed(ActionEvent e) {
		    		loginButtonPerformed(e);
		    		play();
		    		getVideoEffect(null);
		    	}
		    });
			
		    //登出按钮，监听事件
		    logoutBtn.addActionListener(new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		logoutButtonPerformed(e);
		    	}
		    });
		}
	}
		
	//预览面板
	private class RealPlayPanel extends PaintPanel {
		private static final long serialVersionUID = 1L;
		public RealPlayPanel() {
			setBorderEx(this, "实时预览", 4);
			setLayout(new BorderLayout());
			Dimension dim = getPreferredSize();
			setPreferredSize(dim);
			
			realPlayWindow = new Panel();
			add(realPlayWindow, BorderLayout.CENTER);
		}
	}
	
	//云台控制
	public class PtzControlPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public PtzControlPanel() {
			setBorderEx(this, "云台控制", 2);
			setLayout(new BorderLayout());
			Dimension dim = getPreferredSize();
			dim.height =180;
			dim.width = 280;
			setPreferredSize(dim);
			
			modifyPanel = new ModifyPanel();  //变倍、变焦控制
			dirControlPanel = new DirControlPanel();  //方向控制		
			sliderPanel = new SliderPanel();  //亮度、对比度、饱和度、色度设置		
		    saveRealDataPanel = new SaveRealDataPanel();
		    ptzCtrPanel = new JPanel();
				
			add(modifyPanel, BorderLayout.NORTH);
			add(ptzCtrPanel, BorderLayout.CENTER);
			add(saveRealDataPanel, BorderLayout.SOUTH);
			
			ptzCtrPanel.setLayout(new GridLayout(2, 1));
			ptzCtrPanel.add(dirControlPanel);
			ptzCtrPanel.add(sliderPanel);
		}
	}
	
	//变倍、变焦控制面板
	public class ModifyPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public ModifyPanel() {
			setBorderEx(this, "变倍、变焦设置", 2);
			setLayout(new FlowLayout());
			Dimension dim = getPreferredSize();
			dim.height =100;
			setPreferredSize(dim);
			
			zoomAdd = new JButton("变倍+");
			zoomDec = new JButton("变倍-");
			spaceLabel1 = new JLabel("         ");
			spaceLabel2 = new JLabel("         ");
			focusAdd = new JButton("变焦+");
			focusDec = new JButton("变焦-");
			
			add(spaceLabel1);
			add(zoomAdd);
			add(zoomDec);
			add(spaceLabel2);
			add(focusAdd);
			add(focusDec);
			
			// 变倍+
			zoomAdd.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlZoomAddStart(0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlZoomAddEnd(0);
				}
			});
			
			// 变倍-
			zoomDec.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlZoomDecStart(0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlZoomDecEnd(0);
				}
			});
			
			// 变焦+
			focusAdd.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlFocusAddStart(0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlFocusAddEnd(0);
				}
			});
			
			// 变焦-
			focusDec.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlFocusDecStart(0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlFocusDecEnd(0);
				}
			});		
		}
	}
		
	//方向控制面板
	public class DirControlPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public DirControlPanel() {
			setBorderEx(this, "方向控制", 2);
			setLayout(new GridLayout(3, 1));
			
			//将DirControlPanel分为三部分
			JPanel dirControlPanel1 = new JPanel();	
			JPanel dirControlPanel2 = new JPanel();
			JPanel dirControlPanel3 = new JPanel();
			
			dirControlPanel1.setLayout(new FlowLayout());
			dirControlPanel2.setLayout(new FlowLayout());
			dirControlPanel3.setLayout(new FlowLayout());
			
			up = new JButton("上");
			leftUp = new JButton("左上");
			rightUp = new JButton("右上");
			down = new JButton("下");
			leftDown = new JButton("左下");
			rightDown = new JButton("右下");
			left = new JButton("左");
			right = new JButton("右");
			String[] speed = {"速率" + " 1",
							  "速率" + " 2",
							  "速率" + " 3",
							  "速率" + " 4",
							  "速率" + " 5",
							  "速率" + " 6",
							  "速率" + " 7",
							  "速率" + " 8"};
	
			speedComboBox = new JComboBox(speed);
			speedComboBox.setSelectedIndex(4);
			speedComboBox.setPreferredSize(new Dimension(74, 25)); 	
			
			dirControlPanel1.add(leftUp);
			dirControlPanel1.add(up);	
			dirControlPanel1.add(rightUp);
			dirControlPanel2.add(left);
			dirControlPanel2.add(speedComboBox);
			dirControlPanel2.add(right);	
			
			dirControlPanel3.add(leftDown);
			dirControlPanel3.add(down);
			dirControlPanel3.add(rightDown);
			
			add(dirControlPanel1);
			add(dirControlPanel2);
			add(dirControlPanel3);
			
			// 向上
			up.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlUpStart(0, 0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlUpEnd(0);
				}
			});

			
			// 向下
			down.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlDownStart(0, 0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlDownEnd(0);
				}
			});

			
			// 向左
			left.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlLeftStart(0, 0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlLeftEnd(0);
				}
			});
			
			// 向右
			right.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlRightStart(0, 0, speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlRightEnd(0);
				}
			});
			
			// 向左上
			leftUp.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlLeftUpStart(0, speedComboBox.getSelectedIndex(), 
											 speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlLeftUpEnd(0);
				}
			});
			
			// 向右上
			rightUp.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlRightUpStart(0, speedComboBox.getSelectedIndex(), 
											  speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlRightUpEnd(0);
				}
			});
			
			// 向左下
			leftDown.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlLeftDownStart(0, speedComboBox.getSelectedIndex(), 
											   speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlLeftDownEnd(0);
				}
			});
			  
			// 向右下
			rightDown.addMouseListener(new MouseListener() {			
				@Override
				public void mouseExited(MouseEvent e) {		
				}	
				@Override
				public void mouseEntered(MouseEvent e) {
				}			
				@Override
				public void mouseClicked(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					ptzControlRightDownStart(0, speedComboBox.getSelectedIndex(), 
												speedComboBox.getSelectedIndex());	
					
				}
				@Override
				public void mouseReleased(MouseEvent e) {	
					ptzControlRightDownEnd(0);
				}
			});
		}
	}
		
	//亮度、对比度、饱和度、色度设置  进度条方法
	public class SliderPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public SliderPanel() {
			setLayout(new GridLayout(4, 1));
			setBorderEx(this, "画面颜色属性", 4);
			
			brightnessLabel = new JLabel("亮度    (0)");		
			contrastLabel = new JLabel("对比度(0)");				
			hueLabel = new JLabel("色度    (0)");	
			saturationLabel = new JLabel("饱和度(0)");				
	
			//设置滑动块
			brightJSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
			contrastJSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
			hueJSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
			saturationJSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);			
			
			add(brightnessLabel); 
			add(brightJSlider);
			add(contrastLabel);   
			add(contrastJSlider);
			add(hueLabel);           
			add(hueJSlider);        
			add(saturationLabel);
			add(saturationJSlider);	
			
			//亮度滑块监听
			brightJSlider.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
				}
				public void mouseEntered(MouseEvent e) {
				}
				public void mouseExited(MouseEvent e) {
				}
				public void mousePressed(MouseEvent e) {
				}
				public void mouseReleased(MouseEvent e) {
					setVideoEffect(e);
				}				
			});
			//对比度滑块监听
			contrastJSlider.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
				}
				public void mouseEntered(MouseEvent e) {
				}
				public void mouseExited(MouseEvent e) {
				}
				public void mousePressed(MouseEvent e) {
				}
				public void mouseReleased(MouseEvent e) {
					setVideoEffect(e);
				}				
			});
			//色度滑块监听
			hueJSlider.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
				}
				public void mouseEntered(MouseEvent e) {
				}
				public void mouseExited(MouseEvent e) {
				}
				public void mousePressed(MouseEvent e) {
				}
				public void mouseReleased(MouseEvent e) {
					setVideoEffect(e);
				}			
			});
			//饱和度滑块监听
			saturationJSlider.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
				}
				public void mouseEntered(MouseEvent e) {
				}
				public void mouseExited(MouseEvent e) {
				}
				public void mousePressed(MouseEvent e) {
				}
				public void mouseReleased(MouseEvent e) {
					setVideoEffect(e);
				}				
			});
		}
	}
	
	/**
	 * 控制面板-保存录像操作
	 */
	public class SaveRealDataPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public SaveRealDataPanel() {
			setBorderEx(this, "保存实时监视", 4);
			setLayout(new BorderLayout());
			
			pathText = new JTextField();
			saveRealDataBtn = new JButton("保存录像");
			stopSaveRealDataBtn = new JButton("停止保存");
			JPanel panel = new JPanel();
			
			add(pathText, BorderLayout.NORTH);
			add(panel, BorderLayout.SOUTH);
			
			panel.setLayout(new GridLayout(2, 1));
			panel.add(saveRealDataBtn);
			panel.add(stopSaveRealDataBtn);
			
			saveRealDataBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					saveRealData();
				}		
			});
			
			stopSaveRealDataBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					stopSaveRealData();
				}		
			});
		}
	}

		
	////////////////////事件执行//////////////////////
	///////////////////////////////////////////////
	//登录按钮事件
	private void loginButtonPerformed(ActionEvent e) {
		m_strIp = ipTextArea.getText();
		m_nPort = Integer.parseInt(portTextArea.getText());
		m_strUser = nameTextArea.getText();
		m_strPassword = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + m_strIp + "\n端口号：" + m_nPort 
				+ "\n用户名：" + m_strUser + "\n密码：" + m_strPassword);
		
		IntByReference nError = new IntByReference(0);
		m_hLoginHandle = NetSdk.CLIENT_LoginEx(m_strIp, m_nPort.intValue(), m_strUser, m_strPassword, 0, null, m_stDeviceInfo, nError);
		if(m_hLoginHandle.longValue() == 0) {
			int error = 0;
			error = NetSdk.CLIENT_GetLastError();
			System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[0x%x]\n", m_strIp, m_nPort, error);
			JOptionPane.showMessageDialog(mainFrame, "登录失败，错误码 ：" + String.format("[0x%x]", error));	
		} else {
			System.out.println("Login Success [ " + m_strIp + " ]");
			JOptionPane.showMessageDialog(mainFrame, "登录成功");
			logoutBtn.setEnabled(true);
			loginBtn.setEnabled(false);	
		}	
	}
	//登出按钮事件
	private void logoutButtonPerformed(ActionEvent e) {
		if(m_hLoginHandle.longValue() != 0) {
			System.out.println("Logout Button Action");
			
			//结束接口调用
			stopPlay();
			stopSaveRealData();
		
			if(NetSdk.CLIENT_Logout(m_hLoginHandle)) {
				System.out.println("Logout Success [ " + m_strIp + " ]");
				m_hLoginHandle.setValue(0);
				logoutBtn.setEnabled(false);
				loginBtn.setEnabled(true);					
			}
		}
	}
	
	
    /////////实时预览///////////
	private void  play() {
	    int channel = 0; //预览通道号，设备有多通道的情况，可手动更改
	    int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; //实时预览
	
	    m_hPlayHandle = NetSdk.CLIENT_RealPlayEx(m_hLoginHandle, channel, Native.getComponentPointer(realPlayWindow), playType);
	
	    if(m_hPlayHandle.longValue() ==0) {
		    int error = NetSdk.CLIENT_GetLastError();
	  	    System.err.println("开始实时监视失败，错误码" + String.format("[0x%x]", error));
	    } else {
	  	    System.out.println("Success to start realplay");
		    realPlayWindow.setVisible(true);
		    realPlayPanel.setOpaque(false);
	  	    realPlayPanel.repaint();	 
	    }
	} 
	
	/********************************************************************************
	 * 									云台功能                  							*
	 ********************************************************************************/
	/**
	 * 向上
	 */
	public static boolean ptzControlUpStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
									NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL, 
									lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlUpEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
									 NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL, 
									 0, 0, 0, 1);
	}
	
	/**
	 * 向下
	 */
	public static boolean ptzControlDownStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL, 
											lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlDownEnd(int nChannelID) {
		return 	NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											 NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL, 
											 0, 0, 0, 1);
	}
	
	/**
	 * 向左
	 */
	public static boolean ptzControlLeftStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL, 
											lParam1, lParam2, 0, 0);	
	}
	public static boolean ptzControlLeftEnd(int nChannelID) {
		return	NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											 NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL, 
											 0, 0, 0, 1);	
	}
	
	/**
	 * 向右
	 */
	public static boolean ptzControlRightStart(int nChannelID, int lParam1,int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL, 
											lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlRightEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											 NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL, 
											 0, 0, 0, 1);		
	}
	
	/**
	 * 向左上
	 */
	public static boolean ptzControlLeftUpStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP, 
											lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlLeftUpEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											 NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP, 
											 0, 0, 0, 1);	
	}
	
	/**
	 * 向右上
	 */
	public static boolean ptzControlRightUpStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTTOP, 
											lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlRightUpEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
											 NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTTOP, 
											 0, 0, 0, 1);	
	}

	/**
	 * 向左下
	 */
	public static boolean ptzControlLeftDownStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
													NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN, 
													lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlLeftDownEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
										 NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN, 
										 0, 0, 0, 1);
	}
	
	/**
	 * 向右下
	 */
	public static boolean ptzControlRightDownStart(int nChannelID, int lParam1, int lParam2) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
													NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN, 
													lParam1, lParam2, 0, 0);
	}
	public static boolean ptzControlRightDownEnd(int nChannelID) {
		return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
										 NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN, 
										 0, 0, 0, 1);
	}
	
    /**
     * 变倍+
     */
    public static boolean ptzControlZoomAddStart(int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
							        	   NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_ADD_CONTROL, 
							        	   0, lParam2, 0, 0);
    }
    public static boolean ptzControlZoomAddEnd(int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
					            		    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_ADD_CONTROL, 
					            		    0, 0, 0, 1);
    }

    /**
     * 变倍-
     */
    public static boolean ptzControlZoomDecStart(int nChannelID, int lParam2) {
       return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
							        	    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_DEC_CONTROL, 
							        	    0, lParam2, 0, 0);
    }
    public static boolean ptzControlZoomDecEnd(int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
					            		     NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_DEC_CONTROL, 
					            		     0, 0, 0, 1);
    }

    /**
     * 变焦+
     */
    public static boolean ptzControlFocusAddStart(int nChannelID, int lParam2) {
    	return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
									        	    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_ADD_CONTROL, 
									        	    0, lParam2, 0, 0);
    }
    public static boolean ptzControlFocusAddEnd(int nChannelID) {
    	return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
				            		     NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_ADD_CONTROL, 
				            		     0, 0, 0, 1);
    }

    /**
     * 变焦-
     */
    public static boolean ptzControlFocusDecStart(int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
							        	    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_DEC_CONTROL, 
							        	    0, lParam2, 0, 0);
    }
    public static boolean ptzControlFocusDecEnd(int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
					            		     NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_DEC_CONTROL, 
					            		     0, 0, 0, 1);
    }

    /**
     * 光圈+
     */
    public static boolean ptzControlIrisAddStart(int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
							        	    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_ADD_CONTROL, 
							        	    0, lParam2, 0, 0);
    }
    public static boolean ptzControlIrisAddEnd(int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
					            		     NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_ADD_CONTROL, 
					            		     0, 0, 0, 1);
    }

    /**
     * 光圈-
     */
    public static boolean ptzControlIrisDecStart(int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
									        	    NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_DEC_CONTROL, 
									        	    0, lParam2, 0, 0);
    }
    public static boolean ptzControlIrisDecEnd(int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID, 
					            		     NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_DEC_CONTROL, 
					            		     0, 0, 0, 1);
    }
	
	//获取亮度、对比度、饱和度、色度参数   
	public void getVideoEffect(MouseEvent e){
		byte[] param1 = {0};
		byte[] param2 = {0};
		byte[] param3 = {0};
		byte[] param4 = {0};
			
		m_hVideo = NetSdk.CLIENT_ClientGetVideoEffect(m_hPlayHandle, param1, param2, param3, param4);
		if(m_hVideo) {
		   System.out.println("Get： param1= " + (int)(param1[0]&0xff) 
				   + ",  param2= " + (int)(param2[0]&0xff) 
				   + ",  param3= " + (int)(param3[0]&0xff)  
				   + ",  param4= " + (int)(param4[0]&0xff));
		   
			brightnessLabel.setText("亮度    (" + String.valueOf((int)(param1[0]&0xff)) + ")");		
			contrastLabel.setText("对比度(" + String.valueOf((int)(param2[0]&0xff)) + ")");				
			hueLabel.setText("色度    (" + String.valueOf((int)(param3[0]&0xff)) + ")");	
			saturationLabel.setText("饱和度(" + String.valueOf((int)(param4[0]&0xff)) + ")");	
			
			brightJSlider.setValue((int)(param1[0]&0xff));
			contrastJSlider.setValue((int)(param2[0]&0xff));
			hueJSlider.setValue((int)(param3[0]&0xff));
			saturationJSlider.setValue((int)(param4[0]&0xff));	
		} else {
		   System.err.println("GetVideoEffect Failed!");
		}
	}
	
	//设置亮度、对比度、饱和度、色度参数   
	private void setVideoEffect(MouseEvent e) {				
		m_nBrightness = brightJSlider.getValue();
		m_nContrast = contrastJSlider.getValue();
		m_nHue = hueJSlider.getValue();
		m_nSaturation = saturationJSlider.getValue();
		
		m_hVideo = NetSdk.CLIENT_ClientSetVideoEffect(m_hPlayHandle, 
													(byte)m_nBrightness, 
													(byte)m_nContrast, 
													(byte)m_nHue, 
													(byte)m_nSaturation);
		if(!m_hVideo) {	
			 System.err.println("Modify Failed!");
		} else {		
			 System.out.println("Set: m_nBrightness=" + m_nBrightness + ",  m_nContrast=" + m_nContrast + ",  m_nHue=" + m_nHue + ",  m_nSaturation=" + m_nSaturation);			 
		
			//从滑动块获取参数，并在文本中显示
			brightnessLabel.setText(String.valueOf("亮度    (" + m_nBrightness + ")"));
			contrastLabel.setText(String.valueOf("对比度(" + m_nContrast + ")"));
			hueLabel.setText(String.valueOf("色度    (" + m_nHue + ")"));
			saturationLabel.setText(String.valueOf("饱和度(" + m_nSaturation + ")"));
		}	
	}

	//结束调用
	private void stopPlay() {
		System.out.println("Stop Tasks!");
		if(m_hPlayHandle.longValue() ==0) {
			System.err.println("Please make sure the RealPlay Handle is valid!");
			return;
		}
		if(!NetSdk.CLIENT_StopRealPlayEx(m_hPlayHandle)) {
			System.err.println("StopRealPlay Failed");
			return;
		} else {
			System.out.println("StopRealPlay Succeed!");
			m_hPlayHandle.setValue(0);	
			realPlayWindow.repaint();
		}	
	}
	
	/**
	 * 保存录像
	 */
	private void saveRealData() {
		if (m_hLoginHandle.longValue() == 0) {			
			System.err.printf("Please Login First");
        	JOptionPane.showMessageDialog(mainFrame, "请先登录");		
        	return;			
		}
		
		if(m_hPlayHandle.longValue() != 0) {
			File currentPath = new File(".");
			String pchFileName = currentPath.getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".dav"; // 默认保存路径
			pathText.setText(pchFileName);
			boolean m_hSaveRealData = NetSdk.CLIENT_SaveRealData(m_hPlayHandle, pchFileName);
			if(m_hSaveRealData) {
				System.out.println("Saving RealData....");
			} else {
				System.err.println("Saving RealData failed!" + NetSdk.CLIENT_GetLastError());
			}
		} else {
			System.err.println("Please make sure the RealPlay Handle is valid!");
		}
	}
	
	/**
	 * 停止保存录像
	 */
	private void stopSaveRealData() {
		if (m_hLoginHandle.longValue() == 0) {			
			System.err.printf("Please Login First");
        	JOptionPane.showMessageDialog(mainFrame, "请先登录");		
        	return;			
		}
		
		if(m_hPlayHandle.longValue() != 0) {			
			boolean bRet = NetSdk.CLIENT_StopSaveRealData(m_hPlayHandle);
			if(bRet) {
				System.out.println("Stop SaveRealData....");
			} else {
				System.err.println("Stop SaveRealData failed!" + NetSdk.CLIENT_GetLastError());
			}
		} 
	}
	
	/////////////////////组件//////////////////////////
	//////////////////////////////////////////////////
	//带背景的面板组件
	private class PaintPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private Image image; //背景图片	
		
		public PaintPanel() {
			super();
			setOpaque(true); //非透明
			setLayout(null);
			setBackground(new Color(153, 240, 255));
			setForeground(new Color(0, 0, 0));
		}
		
		//设置图片的方法
		public void setImage(Image image) {
			this.image = image;
		}
		
		protected void paintComponent(Graphics g) {    //重写绘制组件外观
			if(image != null) {
				g.drawImage(image, 0, 0, getWidth(), getHeight(), this);//绘制图片与组件大小相同
			}
			super.paintComponent(g); // 执行超类方法
		}	
	}
		
	private JFrame mainFrame;
	
	//登录组件
	private LoginPanel loginPanel;
	
	//实时预览
	private RealPlayPanel realPlayPanel;
	private Panel realPlayWindow;
	
	//云台控制
	private PtzControlPanel ptzControlPanel;
	private ModifyPanel modifyPanel;
	private DirControlPanel dirControlPanel;
	private SliderPanel sliderPanel;
	
	private JButton loginBtn;
	private JButton logoutBtn;
	private JButton zoomAdd;
	private JButton zoomDec;
	private JButton focusAdd;
	private JButton focusDec;
	private JButton up;
	private JButton leftUp;
	private JButton rightUp;
	private JButton down;
	private JButton leftDown;
	private JButton rightDown;
	private JButton left;
	private JButton right;
	
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JLabel ipLabel;
	private JLabel portLabel;
	private JLabel brightnessLabel;
	private JLabel contrastLabel;
	private JLabel saturationLabel;
	private JLabel hueLabel;
	private JLabel spaceLabel1;
	private JLabel spaceLabel2;
    
    private JComboBox speedComboBox;
	
	private JTextField ipTextArea;
	private JTextField portTextArea;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;	
	
	private JSlider brightJSlider;
	private JSlider contrastJSlider;
	private JSlider hueJSlider;
	private JSlider saturationJSlider;
	
	private SaveRealDataPanel saveRealDataPanel;
	private JTextField pathText;
	private JButton saveRealDataBtn;
	private JButton stopSaveRealDataBtn;
	private JPanel ptzCtrPanel;
	
	public static void main(String[] args) {	
		new PtzControl();
	}
}
