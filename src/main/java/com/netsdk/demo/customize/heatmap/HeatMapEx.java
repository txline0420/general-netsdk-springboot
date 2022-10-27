package com.netsdk.demo.customize.heatmap;

import com.netsdk.demo.util.DateChooserJButton;
import com.netsdk.lib.HeatMapLib;
import com.netsdk.lib.HeatMapLib.HEATMAP_IMAGE_IN;
import com.netsdk.lib.HeatMapLib.HEATMAP_IMAGE_Out;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Vector;


/**
 * 生成热度图Demo，有界面的，通过调用转换库
 */
class HeatMapExFrame extends JFrame{
	private static final long serialVersionUID = 1L;
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	static HeatMapLib HeatMapSdk   = HeatMapLib.HEATMAP_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.23.8.94";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin111";
	
	//设备信息
	private NET_DEVICEINFO_Ex m_stDeviceInfo = new NET_DEVICEINFO_Ex();
	
	private static LLong m_hLoginHandle = new LLong(0);   //登陆句柄
	
	private boolean bInit    = false;
	private boolean bLogopen = false;
	
	private DisConnect disConnect       = new DisConnect();    //设备断线通知回调
	private HaveReConnect haveReConnect = new HaveReConnect(); //网络连接恢复
	
	public static GRAY_MAP grayData = null;	
	
	// 通道
	private Vector<String> chnlist = new Vector<String>(); 
		
	//设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class DisConnect implements fDisConnect {
		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
		}
	}
			
	//网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
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
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
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
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		NetSdk.CLIENT_SetNetworkParam(netParam);	
		
		//设置抓图回调函数， 图片主要在m_SnapReceiveCB中返回
		NetSdk.CLIENT_SetSnapRevCallBack(fSnapReceiveCB.getInstance(), null);
		
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
	
	public HeatMapExFrame() {
		init();
	    setTitle("获取热度图");
	    setSize(700, 580);
	    setLayout(new BorderLayout());
	    setLocationRelativeTo(null);
	 
	    loginPanel = new LoginPanel();
	    heatMapPanel = new HeatMapPanel();
	    
	    add(loginPanel, BorderLayout.NORTH);
	    add(heatMapPanel, BorderLayout.CENTER);
	    
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
		    Dimension dimension = new Dimension();
		    dimension.height = 60;
		    setPreferredSize(dimension);
		    
			loginBtn = new JButton("登录");
			logoutBtn = new JButton("登出");
			nameLabel = new JLabel("用户名");
			passwordLabel = new JLabel("密码");
			nameTextArea = new JTextField(m_strUser, 8);
			passwordTextArea = new JPasswordField(m_strPassword, 8);
			ipLabel = new JLabel("设备地址");
			portLabel = new JLabel("端口号");
			ipTextArea = new JTextField(m_strIp, 8);
			portTextArea = new JTextField(m_nPort.toString(), 5);
    
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
	
	// 热度图获取
	private class HeatMapPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		public HeatMapPanel() {
			setLayout(new BorderLayout());
		    setBorderEx(this, "热度图获取", 2);
		    
		    JPanel panel1 = new JPanel();
		    showHeatMapPanel = new PaintPanel();
		    
		    add(panel1, BorderLayout.NORTH);
		    add(showHeatMapPanel, BorderLayout.CENTER);
		    
		    ////////////////////////// 
		    startTimeLabel = new JLabel("开始时间:");
		    endTimeLabel = new JLabel("结束时间:");
		    startTimeBtn = new DateChooserJButton("2018-11-14 00:00:00");
		    endTimeBtn = new DateChooserJButton();
		    snapPicBtn = new JButton("获取热度图");
		    chnLabel = new JLabel("通道号:");
		    chnComoBox = new JComboBox(chnlist);
		    planIdLabel = new JLabel("计划ID:");
		    String[] planIdStr = {"1", "2", "3", "4"};
		    planIdComoBox = new JComboBox(planIdStr);
		    resultLabel = new JLabel("获取热度图状态 ：", JLabel.CENTER);
		    
		    chnComoBox.setPreferredSize(new Dimension(80, 23));
		    planIdComoBox.setPreferredSize(new Dimension(80, 23));
		    resultLabel.setPreferredSize(new Dimension(200, 23));
		    
		    Dimension dimension = new Dimension();
		    dimension.height = 90;
		    panel1.setPreferredSize(dimension);
		    
		    panel1.setLayout(new GridLayout(3, 1));
		    
		    JPanel panel2 = new JPanel();
		    JPanel panel3 = new JPanel();
		    
		    panel1.add(panel2);
		    panel1.add(panel3);
		    panel1.add(resultLabel);
		    
		    // 
		    panel2.setLayout(new FlowLayout());
		    panel3.setLayout(new FlowLayout());
		    
		    panel2.add(startTimeLabel);
		    panel2.add(startTimeBtn);
		    panel2.add(endTimeLabel);
		    panel2.add(endTimeBtn);
		    
		    panel3.add(chnLabel);
		    panel3.add(chnComoBox);
		    panel3.add(planIdLabel);
		    panel3.add(planIdComoBox);
		    panel3.add(snapPicBtn);
		    
		    startTimeBtn.setEnabled(false);
		    endTimeBtn.setEnabled(false);
		    snapPicBtn.setEnabled(false);
		    chnComoBox.setEnabled(false);
		    planIdComoBox.setEnabled(false);
		    
		    snapPicBtn.addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {	
					SwingUtilities.invokeLater(new Runnable() {		
						@Override
						public void run() {
							showHeatMapPanel.setOpaque(true);
							showHeatMapPanel.repaint();
							resultLabel.setText("获取热度图状态 ：正在抓图!");
						}
					});
					
					new Thread(new Runnable() {				
						@Override
						public void run() {
							// 抓图
							snapPicture(chnComoBox.getSelectedIndex());
						}
					}).start();			
				}
			});
		}	
	}
	
	////////////////////////////////////// 接口实现 ///////////////////////////////////////////////
	
	
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
			System.err.println("Login Device[%s] Port[%d]Failed." + ToolKits.getErrorCode());
		} else {
			System.out.println("Login Success [ " + m_strIp + " ]");
			logoutBtn.setEnabled(true);
			loginBtn.setEnabled(false);	
		    startTimeBtn.setEnabled(true);
		    endTimeBtn.setEnabled(true);
		    snapPicBtn.setEnabled(true);
		    chnComoBox.setEnabled(true);
		    planIdComoBox.setEnabled(true);
		    
		    for(int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
		    	chnlist.add("通道号 " + i);
		    }
		    
		    if(m_stDeviceInfo.byChanNum > 0) {
		    	chnComoBox.setSelectedIndex(0);
		    }
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
			    snapPicBtn.setEnabled(false);
			    chnComoBox.setEnabled(false);
			    planIdComoBox.setEnabled(false);
				showHeatMapPanel.setOpaque(true);
				showHeatMapPanel.repaint(); 
			    chnlist.clear();
			    resultLabel.setText("获取热度图状态 ：");
			}
		}
	}
	
	// 远程抓图
	public void snapPicture(int chn)
	{
		// 发送抓图命令给前端设备，抓图的信息
		SNAP_PARAMS stuSnapParams = new SNAP_PARAMS() ;
		stuSnapParams.Channel = chn;  //抓图通道
		stuSnapParams.mode = 0;     //表示请求一帧 
		stuSnapParams.Quality = 3;
		stuSnapParams.InterSnap = 5;
		stuSnapParams.CmdSerial = 100; // 请求序列号，有效值范围 0~65535，超过范围会被截断为  
		
		IntByReference reserved = new IntByReference(0);
		
		if (false == NetSdk.CLIENT_SnapPictureEx(m_hLoginHandle, stuSnapParams, reserved)) { 
			System.err.println("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode());
			return;
		} else { 
			System.out.println("CLIENT_SnapPictureEx success"); 
		}
	}
	

	public static class fSnapReceiveCB implements fSnapRev{
		private fSnapReceiveCB() {}
		
		private static class fSnapReceiveCBHolder {
			private static fSnapReceiveCB instance = new fSnapReceiveCB();
		}
		
		private static fSnapReceiveCB getInstance() {
			return fSnapReceiveCBHolder.instance;
		}
		public void invoke( LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser)
		{		
			////////////////////// 读取抓图的数据(格式JPG) ///////////////////////////
			synchronized (this) {
				SwingUtilities.invokeLater(new Runnable() {			
					@Override
					public void run() {
						resultLabel.setText("获取热度图状态 ：正在获取背景图数据!");
					}
				});
			}	
			
			// jpg格式的Buf
			byte[] buf = pBuf.getByteArray(0, RevLen);
			
			ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
			try {
				BufferedImage bufferedImage = ImageIO.read(byteArrInput);
				if(bufferedImage == null) {
					return;
				} 

				ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
				try {
					// 此方式转换出来的位深度为 24
					ImageIO.write(bufferedImage, "bmp", byteArrOutput);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				////////////////// 获取背景图的缓存、宽高 /////////////
				byte[] buffer = byteArrOutput.toByteArray();   // bmp格式的Buf
				int width = bufferedImage.getWidth();
				int height = bufferedImage.getHeight();
							
				new MyThread(buffer, width, height).start();
				
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	
	private static class MyThread extends Thread {
		byte[] bBackBuf;;
		int nBackWidth; 
		int nBackHeight;
		public MyThread(byte[] buffer, int width, int height) {
			this.bBackBuf = buffer;
			this.nBackWidth = width;
			this.nBackHeight = height;
		}

		@Override
		public void run() {
			synchronized (this) {
				SwingUtilities.invokeLater(new Runnable() {			
					@Override
					public void run() {
						resultLabel.setText("获取热度图状态 ：正在查询灰度图数据!");
					}
				});
			}
			
			////////////////// 查询灰度图数据  //////////////////////////
			if(queryHeatMap(chnComoBox.getSelectedIndex(), 
							planIdComoBox.getSelectedIndex() + 1, 
							startTimeBtn.getText(), 
							endTimeBtn.getText())) {
				
				synchronized (this) {
					SwingUtilities.invokeLater(new Runnable() {			
						@Override
						public void run() {
							resultLabel.setText("获取热度图状态 ：正在生成热度图!");
						}
					});
				}
				
				/**
				 *  入参
				 */
				HEATMAP_IMAGE_IN stIn = new HEATMAP_IMAGE_IN();
				/////////////////////// 灰度图, 不带头  ////////////////////////
				// 获取到的灰度数据的位深度是8， 位深度为8时，需要加 1024
		
				// 灰度图大小，不带头
				int nGrayLen = 0;
				
				// 位深度
				int nBit = grayData.getGrayBufLen() / ( grayData.getGrayWidth() * grayData.getGrayHeight() ) * 8;
				
				if(nBit == 8) {
					nGrayLen = grayData.getGrayBufLen() * 8/8;
				} else {
					nGrayLen = grayData.getGrayWidth() * grayData.getGrayHeight() * nBit / 8;
				}	
				byte[] grayBuf = new byte[nGrayLen];
				// 申请内存
				stIn.stuGrayBmpInfo.pBuffer = new Memory(nGrayLen);
				stIn.stuGrayBmpInfo.pBuffer.clear(nGrayLen);	
				
				// 灰度图赋值
				stIn.stuGrayBmpInfo.pBuffer = grayData.getGratBufData();				
				stIn.stuGrayBmpInfo.nWidth = grayData.getGrayWidth();
				stIn.stuGrayBmpInfo.nHeight = grayData.getGrayHeight();
				stIn.stuGrayBmpInfo.nBitCount = nBit;
				stIn.stuGrayBmpInfo.nDirection = 0;
				
				////////////////////////// 背景图, 带头  //////////////////////////////
				// 转换出来的背景图位深度是24
				// 54是头
				int nBackPicLen = nBackWidth * nBackHeight * 24/8 + 54;   
				
				stIn.stuBkBmpInfo.pBuffer = new Memory(nBackPicLen);
				stIn.stuBkBmpInfo.pBuffer.clear(nBackPicLen);		
				
				stIn.stuBkBmpInfo.pBuffer.write(0, bBackBuf, 0, nBackPicLen);
				stIn.stuBkBmpInfo.nWidth = nBackWidth;
				stIn.stuBkBmpInfo.nHeight = nBackHeight;
				stIn.stuBkBmpInfo.nBitCount = 24;
				stIn.stuBkBmpInfo.nDirection = 1;
				
				/**
				 *  出参
				 */
				//////////////////////// 热度图, 带头  //////////////////////////			
				HEATMAP_IMAGE_Out stOut = new HEATMAP_IMAGE_Out();
				stOut.pBuffer = new Memory(nBackPicLen);
				stOut.pBuffer.clear(nBackPicLen);
				
				stOut.nPicSize = nBackPicLen;  // 热度图的大小与背景图相同
				stOut.fOpacity = 0.3F;         // 透明度
				
				// 生成热度图
				if(HeatMapSdk.CreateHeatMap(stIn, stOut)) {		
					File currentPath = new File(".");
					String strFileSnapPic = currentPath.getAbsoluteFile().getParent() + "\\" + "HeatMap.jpg"; 

					byte[] buf = stOut.pBuffer.getByteArray(0, nBackPicLen);				
					ByteArrayInputStream bInputStream = new ByteArrayInputStream(buf);
					
					try {
						BufferedImage bufferedImage = ImageIO.read(bInputStream);
						if(bufferedImage == null) {
							System.err.println("bufferedImage == null");
							return;
						}					
						
						// 面板显示热度图
						showHeatMapPanel.setImage(bufferedImage);
						showHeatMapPanel.setOpaque(false);
						showHeatMapPanel.repaint();
						
						// 写文件，生成图片
						ImageIO.write(bufferedImage, "jpg", new File(strFileSnapPic));
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					synchronized (this) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								resultLabel.setText("获取热度图状态 ：已生成热度图!");
							}
						});
					}
				} else {
					System.err.println("失败！！！");
				}
			}
		}	
	}
	
	// 查询热度图
	public static boolean queryHeatMap(int chn, int planId, String startTime, String endTime) {
		// 开始时间
		String[] startStr = startTime.split(" ");
		String[] startTimeStr1 = startStr[0].split("-");
		String[] startTimeStr2 = startStr[1].split(":");
		
		// 结束时间
		String[] endStr = endTime.split(" ");
		String[] endTimeStr1 = endStr[0].split("-");
		String[] endTimeStr2 = endStr[1].split(":");
		
		
		NET_QUERY_HEAT_MAP heatMap = new NET_QUERY_HEAT_MAP();
		heatMap.stuIn.nChannel = chn; // 通道
		
//		heatMap.stuIn.nPlanID = planId;  // 计划ID
		
		// 开始时间
		heatMap.stuIn.stuBegin.dwYear = Integer.parseInt(startTimeStr1[0]);
		heatMap.stuIn.stuBegin.dwMonth = Integer.parseInt(startTimeStr1[1]);
		heatMap.stuIn.stuBegin.dwDay = Integer.parseInt(startTimeStr1[2]);
		heatMap.stuIn.stuBegin.dwHour = Integer.parseInt(startTimeStr2[0]);
		heatMap.stuIn.stuBegin.dwMinute = Integer.parseInt(startTimeStr2[1]);
		heatMap.stuIn.stuBegin.dwSecond = Integer.parseInt(startTimeStr2[2]);

		// 结束时间
		heatMap.stuIn.stuEnd.dwYear = Integer.parseInt(endTimeStr1[0]);
		heatMap.stuIn.stuEnd.dwMonth = Integer.parseInt(endTimeStr1[1]);
		heatMap.stuIn.stuEnd.dwDay = Integer.parseInt(endTimeStr1[2]);
		heatMap.stuIn.stuEnd.dwHour = Integer.parseInt(endTimeStr2[0]);
		heatMap.stuIn.stuEnd.dwMinute = Integer.parseInt(endTimeStr2[1]);
		heatMap.stuIn.stuEnd.dwSecond = Integer.parseInt(endTimeStr2[2]);	
		
		// 指针申请内存
		int size = 4000 * 4000;
		heatMap.stuOut.pBufData = new Memory(size);
		heatMap.stuOut.pBufData.clear(size);	
		
		heatMap.stuOut.nBufLen = size;
		
		heatMap.stuOut.emDataType = 1; // 1-灰度数据   2-原始数据
		
		IntByReference pRetLen = new IntByReference(0);
		
		heatMap.write();
		boolean bRet = NetSdk.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_GET_HEAT_MAP, 
												   heatMap.getPointer(), heatMap.size(), pRetLen, 5000);
		heatMap.read();
		if(bRet) {
			grayData = new GRAY_MAP();
			grayData.setGratBufData(heatMap.stuOut.pBufData);
			grayData.setGrayBufLen(heatMap.stuOut.nBufRet);
			grayData.setGrayWidth(heatMap.stuOut.nWidth);
			grayData.setGrayHeight(heatMap.stuOut.nHeight);
		} else {
			JOptionPane.showMessageDialog(null, "查到热度图失败， 错误码 ：" + ToolKits.getErrorCode());
			System.err.println("Query HeatMap Failed!" + ToolKits.getErrorCode());
			return false;
		}
		return true;
	}
	
	
	// 灰度图数据
	static class GRAY_MAP {
		public Pointer  pGratBufData;
		public int		nGrayBufLen; 
		public int 		nGrayWidth;
		public int 		nGrayHeight;
		
		public Pointer getGratBufData() {
			return pGratBufData;
		}
		public void setGratBufData(Pointer pGratBufData) {
			this.pGratBufData = pGratBufData;
		}
		public int getGrayBufLen() {
			return nGrayBufLen;
		}
		public void setGrayBufLen(int nGrayBufLen) {
			this.nGrayBufLen = nGrayBufLen;
		}
		public int getGrayWidth() {
			return nGrayWidth;
		}
		public void setGrayWidth(int nGrayWidth) {
			this.nGrayWidth = nGrayWidth;
		}
		public int getGrayHeight() {
			return nGrayHeight;
		}
		public void setGrayHeight(int nGrayHeight) {
			this.nGrayHeight = nGrayHeight;
		}

		
	}

	
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
	
    private HeatMapPanel heatMapPanel;
    private static DateChooserJButton startTimeBtn;
    private static DateChooserJButton endTimeBtn;
    private JLabel startTimeLabel;
    private JLabel endTimeLabel;
    private JButton snapPicBtn;
    private JLabel chnLabel;
    private static JComboBox chnComoBox;
    private JLabel planIdLabel;
    private static JComboBox planIdComoBox;
    private static JLabel resultLabel;
    private static PaintPanel showHeatMapPanel;
}


public class HeatMapEx {
	public static void main(String[] args) {	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				HeatMapExFrame demo = new HeatMapExFrame();	
				demo.setVisible(true);		
			}
		});		
	}
}

