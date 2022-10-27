package com.netsdk.demo.customize;

import com.netsdk.demo.util.DateChooserJButtonEx;
import com.netsdk.demo.util.PaintJPanel;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * 人脸操作
 */
class FaceRecognitionFrame extends Frame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib netsdkApi        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configsdkApi     = NetSDKLib.CONFIG_INSTANCE;
	
//	private static final String encode = "GBK";		// win
	
	private static final String encode = "UTF-8"; 	// linux

	//登陆参数
	private String m_strIp         = "172.12.5.53";
	private int m_nPort        	   = 37777;
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin123";

	//设备信息
	private static NET_DEVICEINFO_Ex m_stDeviceInfo = new NET_DEVICEINFO_Ex(); // 对应CLIENT_LoginEx2
	private static LLong m_hLoginHandle = new LLong(0);     // 登陆句柄
	private LLong m_hAttachHandle = new LLong(0);    // 智能订阅句柄
	private LLong m_hRealPlayHandle = new LLong(0);  // 实时预句柄
	
	// 用于布控
	private ArrayList<JTextField> putDispositionArrayList = new ArrayList<JTextField>();  // 保存布控通道文本
	private HashMap<JTextField, JTextField> putDispositionHashMap = new HashMap<JTextField, JTextField>();   // Key：布控通道文本     Value：布控相似度
	
	// 用于撤控
	private ArrayList<JCheckBox> deltDispositionArrayList = new ArrayList<JCheckBox>();
	
	// 通道
	private Vector<String> chnlist = new Vector<String>(); 
	
	// 用于保存对比图的图片缓存，用于多张图片显示
	private ArrayList<BufferedImage> arrayListBuffer = new ArrayList<BufferedImage>();
    
    // 用于保存对比图的人脸库id、名称、人员名称、相似度
    private static String[] candidateStr = new String[4];
    
    // 用于以图搜图
    private ArrayList<PaintJPanel> searchByPicPanelList = new ArrayList<PaintJPanel>();    // 用于保存图片显示面板
    private ArrayList<JTextField> searchByPicTextFieldlList = null; 					   // 用于保存具体的信息文本，每添加一个面板，需要重新new
    private HashMap<PaintJPanel, ArrayList<JTextField>> searchByPicTextFieldMap = new HashMap<PaintJPanel, ArrayList<JTextField>>(); // Key：图片显示面板   Value:具体信息文本列表
	private ArrayList<JPanel> searchShowPanelList = new ArrayList<JPanel>();               // 用于保存展示面板
    // 用于以图搜图
	private static int nToken = 0;  // 查询令牌
	private static LLong attachFaceHandle = new LLong(0); // 人脸订阅句柄
	private static LLong findHandle = new LLong(0); 				// 查询句柄
    
	// 全景大图、人脸图、对比图
	private BufferedImage globalBufferedImage = null;
	private BufferedImage personBufferedImage = null;
	private BufferedImage candidateBufferedImage = null;
	
	private static PutDispositionFrame putDispositionFrame = null;      // 布控窗口
	private static DelDispositionFrame delDispositionFrame = null;      // 撤控窗口
	private static FaceServerAddFrame faceServerAddFrame = null;        // 人员添加窗口
	private static FaceServerModifyFrame faceServerModifyFrame = null;  // 人员修改窗口
	private static FaceServerShowFrame faceServerShowFrame = null;      // 人员列表显示窗口
	private static FaceEventFrame faceEventFrame = null;				// 人脸识别事件窗口
	private static SearchByPictureFrame searchByPictureFrame = null;		// 以图搜图窗口
	
	Object[][] groupData = null;    // 人脸库列表
	Object[][] serverData = null;   // 人员信息列表
	
	private String[] groupName = {"序号", "人脸库名称", "人脸库ID", "人脸库相似度", "当前组绑定到视视通道号"};
	private String[] serverName = {"序号", "人脸库名称", "UID", "姓名", "性别", "证件类型", "证件编号", "生日", "省份", "城市", "图片地址"};
	
	String[] privinceStr = {"安徽省","北京市","重庆市","福建省","甘肃省","广东省","广西壮族自治区","贵州省",
			"海南省","河北省","河南省","黑龙江省","湖北省","湖南省","吉林省","江苏省","江西省","辽宁省",
			"内蒙古自治区","宁夏回族自治区","青海省","山东省","山西省","陕西省","上海市","四川省","天津市",
			"新疆维吾尔自治区","西藏自治区","云南省","浙江省","香港特别行政区","澳门特别行政区","台湾省"};
	
	String[] faceSexStr = {"未知", "男", "女"};
	String[] idStr = {"未知", "身份证", "护照",};
	
	boolean bRealLoadFlag = false;
	private Component  target     = this;
	
	private String selectImagePath = "";   // 图片路径
	private int nPicBufLen = 0;			   // 图片大小
	private int width = 0;				   // 图片宽
	private int height = 0;				   // 图片高
	Memory memory = null;				   // 存储
	
	//////////////////SDK相关信息///////////////////////////
	//NetSDK 库初始化	
	private boolean bInit    = false;
	private boolean bLogopen = false;   
	
	private DisConnect disConnect       = new DisConnect();    //设备断线通知回调
	private HaveReConnect haveReConnect = new HaveReConnect(); //网络连接恢复
		
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
	
	// 登陆前，设备初始化
	public boolean init() {	
		bInit = netsdkApi.CLIENT_Init(disConnect, null);
		if(!bInit) {
			System.out.println("Initialize SDK failed");
			return false;
		}
		
		//打开日志，可选
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
		File path = new File(".");		
		String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\FaceRecognition_" + System.currentTimeMillis() + ".log";			
		
		setLog.bSetFilePath = 1; 
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy    = 0;
		bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
		if(!bLogopen ) {
			System.err.println("Failed to open NetSDK log");
		}
		
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
	    
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		return true;
	}
	
	// 关闭工程前，清除环境，释放内存
	public void cleanup() {
		if(bLogopen) {
			netsdkApi.CLIENT_LogClose();
		}
		
		if(bInit) {
			netsdkApi.CLIENT_Cleanup();
		}
	}
	

	public FaceRecognitionFrame() {	
		init();
	    setTitle("FaceRecognition");
	    setSize(950, 700);
	    setLayout(new BorderLayout());
	    setLocationRelativeTo(null);
	    setVisible(true);
        	    
	    loginPanel = new LoginPanel();  			// 登陆面板
	    FaceRecognitionOperatePanel faceRecognitionOperatePanel = new FaceRecognitionOperatePanel();  // 人脸操作面板
	    ListPanel listPanel = new ListPanel();      // 列表面板
	     
	    add(loginPanel, BorderLayout.NORTH);
	    add(faceRecognitionOperatePanel, BorderLayout.WEST);
	    add(listPanel, BorderLayout.CENTER);
	    
	    addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
	    		System.out.println("Window Closing");
	    		//登出
	    		logout();    
	    		cleanup();
	    		dispose();		
	    		if(putDispositionFrame != null) {
	        		putDispositionFrame.dispose();
	    		}
	    		
	    		if(delDispositionFrame != null) {
	    	 		delDispositionFrame.dispose();
	    		}
	    		
	       		if(faceServerAddFrame != null) {
	       	   		faceServerAddFrame.dispose();
	    		}
	       		
		 		if(faceServerModifyFrame != null) {
		 			faceServerModifyFrame.dispose();	
			    }
		 		
		 		if(faceServerShowFrame != null) {
		 			faceServerShowFrame.dispose();
				}
		 		
		 		if(faceEventFrame != null) {
		 			faceEventFrame.dispose();
				}
		 		
		 		if(searchByPictureFrame != null) {
		 			searchByPictureFrame.dispose();
		 		}
	    	}
	    });
	}
	
	/***********************************************************************************************
	 * 									界面布局面板												   *
	 ***********************************************************************************************/
	//设置边框
	private void setBorderEx(JComponent object, String title, int width) {
	    Border innerBorder = BorderFactory.createTitledBorder(title);
	    Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);
	    object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));	 
	}
	
	//登录面板
	public class LoginPanel extends JPanel {
		/**
		 * 
		 */
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
			portTextArea = new JTextField(String.valueOf(m_nPort), 4);
			
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
		    		if(login()) {
//		    			attachDeviceState(m_hLoginHandle);  // 监听设备状态
//		    			getDeviceInfo(m_hLoginHandle);      // 查询前端的设备信息
		    			findGroupInfo("");
		    			loginBtn.setEnabled(false);
		    			logoutBtn.setEnabled(true);
		    			addGroupBtn.setEnabled(true);
		    			modifyGroupBtn.setEnabled(true);
		    			delGroupBtn.setEnabled(true);
//		    			delAllGrouopBtn.setEnabled(true);  此功能注释，以防删除所有人脸库
		    			addFaceDBBtn.setEnabled(true);
		    			modifyFaceDBBtn.setEnabled(true);
		    			delFaceDBBtn.setEnabled(true);
		    			realoadPictureBtn.setEnabled(true);
		    			searchPicBtn.setEnabled(true);
		    			putDispositionBtn.setEnabled(true);
		    			delDispositionBtn.setEnabled(true);
		    			
//		    			findFile();
		    		}
		    	}
		    });
			
		    //登出按钮，监听事件
		    logoutBtn.addActionListener(new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		if(logout()) {
		    			clearGroupList();
		    			clearServerList();
		    			groupNameText.setText("");
		    			loginBtn.setEnabled(true);
		    			logoutBtn.setEnabled(false);
		    			addGroupBtn.setEnabled(false);
		    			modifyGroupBtn.setEnabled(false);
		    			delGroupBtn.setEnabled(false);
		    			delAllGrouopBtn.setEnabled(false);
		    			addFaceDBBtn.setEnabled(false);
		    			modifyFaceDBBtn.setEnabled(false);
		    			delFaceDBBtn.setEnabled(false);
		    			realoadPictureBtn.setEnabled(false);
		    			searchPicBtn.setEnabled(false);
		    			putDispositionBtn.setEnabled(false);
		    			delDispositionBtn.setEnabled(false);
		    		}
		    	}
		    });
		}
	}
		
	// 人脸操作面板
	public class FaceRecognitionOperatePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FaceRecognitionOperatePanel() {
			setBorderEx(this, "人脸操作", 4);
			Dimension dim = this.getPreferredSize();
			dim.width = 260;
			this.setPreferredSize(dim);	
			setLayout(new FlowLayout());
			
			FaceGroupPanel singlePanel = new FaceGroupPanel();     // 人脸库操作面板
			FaceServerPanel batchPanel = new FaceServerPanel();    // 人员信息操作面板
			FaceEventPanel faceEventPanel = new FaceEventPanel();  // 人脸识别事件面板
			
            add(singlePanel);
            add(batchPanel);
            add(faceEventPanel);
		}
	}
	
	// 人脸库操作面板
	public class FaceGroupPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FaceGroupPanel(){
			setBorderEx(this, "人脸库操作", 4);
			Dimension dim = this.getPreferredSize();
			dim.height = 120;
			dim.width = 245;
			this.setPreferredSize(dim);
			setLayout(new GridLayout(4,2));
			
			groupNameLabel = new JLabel("人脸库名称 : ");
			groupNameText = new JTextField("", 8);		
			addGroupBtn = new JButton("添加人脸库");
			modifyGroupBtn = new JButton("修改人脸库");
			delGroupBtn = new JButton("删除人脸库");
			delAllGrouopBtn = new JButton("全部删除");
			putDispositionBtn = new JButton("人脸库布控");
			delDispositionBtn = new JButton("人脸库撤控");
			
			add(groupNameLabel);
			add(groupNameText);
			add(addGroupBtn);
			add(modifyGroupBtn);
			add(delGroupBtn);
//			add(delAllGrouopBtn);
			add(putDispositionBtn);
			add(delDispositionBtn);
			
			addGroupBtn.setEnabled(false);
			modifyGroupBtn.setEnabled(false);
			delGroupBtn.setEnabled(false);
			delAllGrouopBtn.setEnabled(false);
			putDispositionBtn.setEnabled(false);
			delDispositionBtn.setEnabled(false);
			
			// 添加人脸库
			addGroupBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					addFaceRecognitionGroup(new String(groupNameText.getText()));	
					findGroupInfo("");
				}
			});
			
			// 修改人脸库
			modifyGroupBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int rowSelect = groupTable.getSelectedRow();	
					
					if(rowSelect < 0) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要修改的人脸库！");
						return;
					} 

					if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)).equals("") 
							|| groupDefaultModel.getValueAt(rowSelect, 2) == null) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
						return;
					} else {
						modifyFaceRecognitionGroup(groupNameText.getText(), String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
					}				
					findGroupInfo("");
				}
			});
			
			// 删除人脸库
			delGroupBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int rowSelect = groupTable.getSelectedRow();
					if(rowSelect < 0) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要删除的人脸库！");
						return;
					} 
					
					if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)).equals("")  
							|| groupDefaultModel.getValueAt(rowSelect, 2) == null) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
						return;
					} else {
						deleteFaceRecognitionGroup(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
					}				
					findGroupInfo("");
				}
			});
			
			// 删除所有人脸库 
			delAllGrouopBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					deleteFaceRecognitionGroup("");				
					findGroupInfo("");
				}
			});
			
			// 以人脸库的角度进行布控
			putDispositionBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int rowSelect = groupTable.getSelectedRow();	
					
					if(rowSelect < 0) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要布控的人脸库！");
						return;
					} 

					if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)).equals("")  
							|| groupDefaultModel.getValueAt(rowSelect, 2) == null) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
						return;
					} else {					
						putDispositionFrame = new PutDispositionFrame(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
						putDispositionFrame.setVisible(true);
					}	
				}
			});
			
			// 以人脸库的角度进行撤控
			delDispositionBtn.addActionListener(new ActionListener() {		
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int rowSelect = groupTable.getSelectedRow();	
					
					if(rowSelect < 0) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要撤控的人脸库！");
						return;
					} 

					if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)).equals("")  
							|| groupDefaultModel.getValueAt(rowSelect, 2) == null) {
						JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
						return;
					} else {		
						delDispositionFrame = new DelDispositionFrame(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
						delDispositionFrame.setVisible(true);
					}	
				}
			});
		}
	}
	
	// 人员信息操作面板
	public class FaceServerPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FaceServerPanel() {
			setBorderEx(this, "人员信息操作", 4);
			Dimension dim = this.getPreferredSize();
			dim.height = 100;
			dim.width = 245;
			this.setPreferredSize(dim);		
			setLayout(new GridLayout(3,2));
			
			addFaceDBBtn = new JButton("添加人员信息");
			modifyFaceDBBtn = new JButton("修改人员信息");
			delFaceDBBtn = new JButton("删除人员信息");
		
			add(addFaceDBBtn);
			add(modifyFaceDBBtn);
			add(delFaceDBBtn);
			
			addFaceDBBtn.setEnabled(false);
			modifyFaceDBBtn.setEnabled(false);
			delFaceDBBtn.setEnabled(false);
			
			addFaceDBBtn.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent arg0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							int rowSelectGroup = groupTable.getSelectedRow();
							if(rowSelectGroup < 0) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要添加的人脸库！");
								return;
							} 

							if(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)).equals("") 
									|| groupDefaultModel.getValueAt(rowSelectGroup, 2) == null) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
								return;
							} 

							faceServerAddFrame = new FaceServerAddFrame();
							faceServerAddFrame.setVisible(true);
						}
					});		
				}
			});
			
			modifyFaceDBBtn.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent arg0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							int rowSelectGroup = groupTable.getSelectedRow();
							if(rowSelectGroup < 0) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要修改的人脸库！");
								return;
							} 
								
							if(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)).equals("") 
									|| groupDefaultModel.getValueAt(rowSelectGroup, 2) == null) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
								return;
							} 
							
							int rowSelectServer = serverTable.getSelectedRow();
							if(rowSelectServer < 0) {
								JOptionPane.showMessageDialog(null, "请在人员列表选择需要修改的人员信息！");
								return;
							} 
 
							if(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)).equals("") 
									|| serverDefaultModel.getValueAt(rowSelectServer, 2) == null) {
								JOptionPane.showMessageDialog(null, "请在人员信息列表选择存在的人员信息！");
								return;
							} 									
							
							faceServerModifyFrame = new FaceServerModifyFrame();
							faceServerModifyFrame.setVisible(true);

							faceServerModifyFrame.faceNameTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 3)));
							faceServerModifyFrame.faceSexComboBox.setSelectedIndex(getSexInt(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 4))));
							faceServerModifyFrame.faceIdTypeComboBox.setSelectedIndex(getIDInt(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 5))));						
							faceServerModifyFrame.faceIdIndexTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 6)));
							if(!String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 7)).equals("0-0-0")) {
								faceServerModifyFrame.faceBirthdayBtn.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 7)));
							}
							faceServerModifyFrame.facePrivinceComboBox.setSelectedIndex(getProvinceIndex(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 8))));
							faceServerModifyFrame.faceAddressTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 9)));
							
							// 先下载图片
							if(!downloadRemoteFile(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 10)))) {
								memory = null;
							}
							
							if(readPictureMemory(faceServerModifyFrame.addImagePanel)) {
								memory = null;
							}
						}
					});		
				}
			});
			
			delFaceDBBtn.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent arg0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							int rowSelectGroup = groupTable.getSelectedRow();
							if(rowSelectGroup < 0) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择需要删除的人脸库！");
								return;
							} 
 
							if(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)).equals("") 
									|| groupDefaultModel.getValueAt(rowSelectGroup, 2) == null) {
								JOptionPane.showMessageDialog(null, "请在人脸库列表选择存在的人脸库！");
								return;
							} 
							
							int rowSelectServer = serverTable.getSelectedRow();
							if(rowSelectServer < 0) {
								JOptionPane.showMessageDialog(null, "请在人员列表选择需要删除的人员信息！");
								return;
							}  
							
							if(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)).equals("") 
									|| serverDefaultModel.getValueAt(rowSelectServer, 2) == null) {
								JOptionPane.showMessageDialog(null, "请在人员信息列表选择存在的人员信息！");
								return;
							} 
							delFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)), String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)));
							findFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)));
						}
					});		
				}
			});
		}
	}
	
	// 人脸识别事件以及以图搜图面板
	public class FaceEventPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FaceEventPanel() {
			setBorderEx(this, "人脸识别事件", 4);
			Dimension dim = this.getPreferredSize();
			dim.height = 330;
			dim.width = 245;
			this.setPreferredSize(dim);	
			setLayout(new GridLayout(11, 1));

			realoadPictureBtn = new JButton("人脸识别事件订阅");
			searchPicBtn = new JButton("以图搜图");
			
			add(realoadPictureBtn);
			add(searchPicBtn);
			
			realoadPictureBtn.setEnabled(false);
			searchPicBtn.setEnabled(false);
			
			realoadPictureBtn.addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					faceEventFrame = new FaceEventFrame();
					faceEventFrame.setVisible(true);
				}
			});
			
			searchPicBtn.addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					searchByPictureFrame = new SearchByPictureFrame();
					searchByPictureFrame.setVisible(true);
				}
			});
		}
	}
	
	// 人脸库列表以及人员信息列表
	public class ListPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ListPanel() {
			setBorderEx(this, "人脸库以及人员信息列表" , 4);	
			setLayout(new GridLayout(2, 1));
			
			GroupListPanel groupListPanel = new GroupListPanel();     // 人脸库列表
			ServerListPanel serverListPanel = new ServerListPanel();  // 人员信息列表
			
			add(groupListPanel);
			add(serverListPanel);
		}
	}
	
	// 人脸库列表
	public class GroupListPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public GroupListPanel() {
			setBorderEx(this, "人脸库列表", 4);
			setLayout(new BorderLayout());	
			
			groupData = new Object[20][5];
			groupDefaultModel = new DefaultTableModel(groupData, groupName);
			groupTable = new JTable(groupDefaultModel) {   // 列表不可编辑
				private static final long serialVersionUID = 1L;
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			
			groupDefaultModel = (DefaultTableModel)groupTable.getModel();
			
			groupTable.getColumnModel().getColumn(0).setPreferredWidth(20);
			groupTable.getColumnModel().getColumn(1).setPreferredWidth(20);
			groupTable.getColumnModel().getColumn(2).setPreferredWidth(20);
			groupTable.getColumnModel().getColumn(3).setPreferredWidth(20);
			
			groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行
			
			// 列表显示居中
			DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
			dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
			groupTable.setDefaultRenderer(Object.class, dCellRenderer);	
			
			JScrollPane scrollPane = new JScrollPane(groupTable);
			add(scrollPane, BorderLayout.CENTER);	
			
			groupTable.addMouseListener(new MouseListener() {	
				@Override
				public void mouseReleased(MouseEvent arg0) {

				}
				
				@Override
				public void mousePressed(MouseEvent arg0) {

				}
				
				@Override
				public void mouseExited(MouseEvent arg0) {

				}
				
				@Override
				public void mouseEntered(MouseEvent arg0) {
					
				}
				
				@Override
				public void mouseClicked(MouseEvent arg0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {								
							int rowSelect = groupTable.getSelectedRow();	
							
							if(rowSelect < 0) {
								return;
							}
							if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 1)).equals("") 
									|| groupDefaultModel.getValueAt(rowSelect, 1) == null) {
								groupNameText.setText("");
								clearServerList();
							} else {
								groupNameText.setText(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 1)));	
								findFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
							}			
						}
					});		
				}
			});
		}
	}
	
	// 人员信息列表
	public class ServerListPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ServerListPanel() {
			setBorderEx(this, "人员信息列表", 4);
			setLayout(new BorderLayout());
			
			textFieldFeature = new JTextField("", 20);
			serverData = new Object[2000][11];
			serverDefaultModel = new DefaultTableModel(serverData, serverName);
			serverTable = new JTable(serverDefaultModel) {   // 列表不可编辑
				private static final long serialVersionUID = 1L;
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			
			serverDefaultModel = (DefaultTableModel)serverTable.getModel();
			
			serverTable.getColumnModel().getColumn(0).setPreferredWidth(50);
			serverTable.getColumnModel().getColumn(1).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(2).setPreferredWidth(50);
			serverTable.getColumnModel().getColumn(3).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(4).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(5).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(6).setPreferredWidth(140);
			serverTable.getColumnModel().getColumn(7).setPreferredWidth(100);
			serverTable.getColumnModel().getColumn(8).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(9).setPreferredWidth(70);
			serverTable.getColumnModel().getColumn(10).setPreferredWidth(250);
			
			serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行
			
			// 列表显示居中
			DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
			dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
			serverTable.setDefaultRenderer(Object.class, dCellRenderer);	
			
			serverTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane scrollPane = new JScrollPane(serverTable);
			add(textFieldFeature, BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);
			
			textFieldFeature.setEditable(false);

			serverTable.addMouseListener(new MouseListener() {	
				@Override
				public void mouseReleased(MouseEvent arg0) {

				}
				
				@Override
				public void mousePressed(MouseEvent arg0) {

				}
				
				@Override
				public void mouseExited(MouseEvent arg0) {

				}
				
				@Override
				public void mouseEntered(MouseEvent arg0) {
					
				}
				
				@Override
				public void mouseClicked(MouseEvent arg0) {
					
					if(m_hLoginHandle.longValue() == 0) {
						return;
					}
					
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {		
							int rowSelect = groupTable.getSelectedRow();	
							int rowSelectServer = serverTable.getSelectedRow();	
		
							if(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)).equals("") 
									|| groupDefaultModel.getValueAt(rowSelect, 2) == null) {
								return;
							}
							
							if(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)).equals("") 
									|| serverDefaultModel.getValueAt(rowSelectServer, 2) == null) {
								return;
							}
							getFaceRecognitionFeatureState(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)), String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)));
						}
					});
					
					if(arg0.getClickCount() < 2) {
						return;
					}
					
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {		
							int rowSelectServer = serverTable.getSelectedRow();	
		
							if(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)).equals("") 
									|| serverDefaultModel.getValueAt(rowSelectServer, 2) == null) {
								return;
							} 
							faceServerShowFrame = new FaceServerShowFrame();
							faceServerShowFrame.setVisible(true);						
							
							faceServerShowFrame.faceNameTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 3)));
							faceServerShowFrame.faceSexComboBox.setSelectedIndex(getSexInt(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 4))));
							faceServerShowFrame.faceIdTypeComboBox.setSelectedIndex(getIDInt(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 5))));						
							faceServerShowFrame.faceIdIndexTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 6)));
							faceServerShowFrame.faceBirthdayBtn.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 7)));
							faceServerShowFrame.facePrivinceComboBox.setSelectedIndex(getProvinceIndex(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 8))));
							faceServerShowFrame.faceAddressTextField.setText(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 9)));
							
							// 先下载图片
							if(!downloadRemoteFile(String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 10)))) {
								return;
							}			
							
							selectImagePath = "./face.jpg";  // 下载的图片路径
							
							// 读取图片
							BufferedImage bufferedImage = null;
							if(selectImagePath == null || selectImagePath.equals("")) {
								System.err.println("selectImagePath == null || selectImagePath");
								return;
							}
							
							File file = new File(selectImagePath);
							if(!file.exists()) {
								System.err.println(file);
								return;
							}
							
							try {
								bufferedImage = ImageIO.read(file);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							// 人员面板信息展示
							faceServerShowFrame.addImagePanel.setOpaque(false);
							faceServerShowFrame.addImagePanel.setImage(bufferedImage);
							faceServerShowFrame.addImagePanel.repaint();
							
							file.delete();   // 删除下载的图片
						}
					});		
				}
			});
		}
	}
	
	/**
	 * 用于添加人员信息
	 *
	 */
	public class FaceServerAddFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FaceServerAddFrame(){
			setTitle("人员信息添加");
			setSize(600, 600);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			memory = null;
			
			FaceServerAddPanel faceServerAddPanel = new FaceServerAddPanel();
            add(faceServerAddPanel, BorderLayout.CENTER);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class FaceServerAddPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FaceServerAddPanel() {
				setBorderEx(this, "", 4);
				Dimension dim = this.getPreferredSize();
				dim.height = 400;
				dim.width = 180;
		
				setLayout(new BorderLayout());
				
				JPanel imagePanel = new JPanel();
				JPanel serverInfoPanel = new JPanel();
				serverInfoPanel.setPreferredSize(dim);
				
				add(imagePanel, BorderLayout.CENTER);
				add(serverInfoPanel, BorderLayout.EAST);
				
				/////////// 添加的人脸图片面板 //////////////////
				imagePanel.setLayout(new BorderLayout());
				addImagePanel = new PaintJPanel();   // 添加的人员信息图片显示
				selectImageBtn = new JButton("选择添加的人脸图片");
				imagePanel.add(addImagePanel, BorderLayout.CENTER);
				imagePanel.add(selectImageBtn, BorderLayout.SOUTH);
				
				////////// 添加的人脸信息面板 /////////////////
				serverInfoPanel.setLayout(new FlowLayout());
				faceNameLabel = new JLabel("姓名 : ");
				faceNameTextField = new JTextField("", 9);
				faceSexLabel = new JLabel("性别 : ");
				
				faceSexComboBox = new JComboBox(faceSexStr);
				
				faceBirthdayLabel = new JLabel("生日 : ");
				faceBirthdayBtn = new DateChooserJButtonEx(); 
				faceCountryLabel = new JLabel("国际 : ");
				faceCountryTextField = new JTextField("中国", 9);
				facePrivinceLabel = new JLabel("省份 : ");
				facePrivinceComboBox = new JComboBox(privinceStr);
				
				faceAddressLabel = new JLabel("地址 : ");
				faceAddressTextField = new JTextField("", 9);

				faceIdTypeLabel = new JLabel("证件类型 : ");
				faceIdTypeComboBox = new JComboBox(idStr);
				faceIdIndexLabel = new JLabel("证件编号 : ");
				faceIdIndexTextField = new JTextField("", 8);
				
				addFaceDBBtn = new JButton("添加");
				cancelFaceDBBtn = new JButton("取消");
	
				serverInfoPanel.add(faceNameLabel);
				serverInfoPanel.add(faceNameTextField);
				serverInfoPanel.add(faceSexLabel);
				serverInfoPanel.add(faceSexComboBox);
				serverInfoPanel.add(faceBirthdayLabel);
				serverInfoPanel.add(faceBirthdayBtn);
				serverInfoPanel.add(faceCountryLabel);
				serverInfoPanel.add(faceCountryTextField);
				serverInfoPanel.add(facePrivinceLabel);
				serverInfoPanel.add(facePrivinceComboBox);
				serverInfoPanel.add(faceAddressLabel);
				serverInfoPanel.add(faceAddressTextField);		
				serverInfoPanel.add(faceIdTypeLabel);
				serverInfoPanel.add(faceIdTypeComboBox);
				serverInfoPanel.add(faceIdIndexLabel);
				serverInfoPanel.add(faceIdIndexTextField);
				serverInfoPanel.add(addFaceDBBtn);
				serverInfoPanel.add(cancelFaceDBBtn);
				
				faceCountryTextField.setEnabled(false);
				
				faceSexComboBox.setPreferredSize(new Dimension(100, 20));
				faceBirthdayBtn.setPreferredSize(new Dimension(100, 20));
				facePrivinceComboBox.setPreferredSize(new Dimension(100, 20));
				faceIdTypeComboBox.setPreferredSize(new Dimension(70, 20));
				addFaceDBBtn.setPreferredSize(new Dimension(70, 20));
				cancelFaceDBBtn.setPreferredSize(new Dimension(70, 20));
				
				// 选择图片，获取图片的信息
				selectImageBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(!selectPicture(addImagePanel)) {
							memory = null;
						}	
					}
				});
				
				// 添加人员信息
				addFaceDBBtn.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent arg0) {
						String[] birthday = faceBirthdayBtn.getText().split("-");
						int rowSelect = groupTable.getSelectedRow();			
						dispose();
						addFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)), nPicBufLen, width, height, memory, faceNameTextField.getText(), 
											(byte)(faceSexComboBox.getSelectedIndex()), birthday, privinceStr[facePrivinceComboBox.getSelectedIndex()], 
											faceAddressTextField.getText(), faceIdIndexTextField.getText(), (byte)faceIdTypeComboBox.getSelectedIndex());
						findFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelect, 2)));
					}
				});
				
				// 取消，关闭
				cancelFaceDBBtn.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dispose();		
					}
				});
			}		 
		}
		
		// 添加人员信息窗口的组件
		private PaintJPanel addImagePanel;
		private JButton selectImageBtn;
		
		private JLabel faceNameLabel;
		private JTextField faceNameTextField;
		private JLabel faceSexLabel;
		private JComboBox faceSexComboBox;
		private JLabel faceBirthdayLabel;
		private DateChooserJButtonEx faceBirthdayBtn; 
		private JLabel faceCountryLabel;
		private JTextField faceCountryTextField;
		private JLabel facePrivinceLabel;
		private JComboBox facePrivinceComboBox;
		private JLabel faceAddressLabel;
		private JTextField faceAddressTextField;
		private JLabel faceIdTypeLabel;
		private JComboBox faceIdTypeComboBox;
		private JLabel faceIdIndexLabel;
		private JTextField faceIdIndexTextField;
		private JButton addFaceDBBtn;
		private JButton cancelFaceDBBtn;
	}
	
	/**
	 * 用于修改人员信息窗口	
	 */
	public class FaceServerModifyFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FaceServerModifyFrame(){
			setTitle("人员信息修改");
			setSize(600, 600);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			FaceServerModifyPanel faceServerModifyPanel = new FaceServerModifyPanel();
            add(faceServerModifyPanel, BorderLayout.CENTER);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class FaceServerModifyPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FaceServerModifyPanel() {
				setBorderEx(this, "", 4);
				Dimension dim = this.getPreferredSize();
				dim.height = 400;
				dim.width = 180;
		
				setLayout(new BorderLayout());
				
				JPanel imagePanel = new JPanel();
				JPanel serverInfoPanel = new JPanel();
				serverInfoPanel.setPreferredSize(dim);
				
				add(imagePanel, BorderLayout.CENTER);
				add(serverInfoPanel, BorderLayout.EAST);
				
				/////////// 修改的人脸图片面板 //////////////////
				imagePanel.setLayout(new BorderLayout());
				addImagePanel = new PaintJPanel();   // 修改的人员信息图片显示
				selectImageBtn = new JButton("选择添加的人脸图片");
				imagePanel.add(addImagePanel, BorderLayout.CENTER);
				imagePanel.add(selectImageBtn, BorderLayout.SOUTH);
				
				////////// 修改的人脸信息面板 /////////////////
				serverInfoPanel.setLayout(new FlowLayout());
				faceNameLabel = new JLabel("姓名 : ");
				faceNameTextField = new JTextField("", 9);
				faceSexLabel = new JLabel("性别 : ");
				
				faceSexComboBox = new JComboBox(faceSexStr);
				
				faceBirthdayLabel = new JLabel("生日 : ");
				faceBirthdayBtn = new DateChooserJButtonEx(); 
				faceCountryLabel = new JLabel("国际 : ");
				faceCountryTextField = new JTextField("中国", 9);
				facePrivinceLabel = new JLabel("省份 : ");
				facePrivinceComboBox = new JComboBox(privinceStr);
				
				faceAddressLabel = new JLabel("地址 : ");
				faceAddressTextField = new JTextField("", 9);

				faceIdTypeLabel = new JLabel("证件类型 : ");
				faceIdTypeComboBox = new JComboBox(idStr);
				faceIdIndexLabel = new JLabel("证件编号 : ");
				faceIdIndexTextField = new JTextField("", 8);
				
				addFaceDBBtn = new JButton("修改");
				cancelFaceDBBtn = new JButton("取消");
	
				serverInfoPanel.add(faceNameLabel);
				serverInfoPanel.add(faceNameTextField);
				serverInfoPanel.add(faceSexLabel);
				serverInfoPanel.add(faceSexComboBox);
				serverInfoPanel.add(faceBirthdayLabel);
				serverInfoPanel.add(faceBirthdayBtn);
				serverInfoPanel.add(faceCountryLabel);
				serverInfoPanel.add(faceCountryTextField);
				serverInfoPanel.add(facePrivinceLabel);
				serverInfoPanel.add(facePrivinceComboBox);
				serverInfoPanel.add(faceAddressLabel);
				serverInfoPanel.add(faceAddressTextField);		
				serverInfoPanel.add(faceIdTypeLabel);
				serverInfoPanel.add(faceIdTypeComboBox);
				serverInfoPanel.add(faceIdIndexLabel);
				serverInfoPanel.add(faceIdIndexTextField);
				serverInfoPanel.add(addFaceDBBtn);
				serverInfoPanel.add(cancelFaceDBBtn);
				
				faceCountryTextField.setEnabled(false);
				
				faceSexComboBox.setPreferredSize(new Dimension(100, 20));
				faceBirthdayBtn.setPreferredSize(new Dimension(100, 20));
				facePrivinceComboBox.setPreferredSize(new Dimension(100, 20));
				faceIdTypeComboBox.setPreferredSize(new Dimension(70, 20));
				addFaceDBBtn.setPreferredSize(new Dimension(70, 20));
				cancelFaceDBBtn.setPreferredSize(new Dimension(70, 20));
				
				// 选择图片，获取图片的信息
				selectImageBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(!selectPicture(addImagePanel)) {
							memory = null;
						}	
					}
				});
				
				// 修改人员信息
				addFaceDBBtn.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent arg0) {
						String[] birthday = faceBirthdayBtn.getText().split("-");
						int rowSelectGroup = groupTable.getSelectedRow();
						int rowSelectServer = serverTable.getSelectedRow();
						dispose();
						modifyFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)), String.valueOf(serverDefaultModel.getValueAt(rowSelectServer, 2)), 
								nPicBufLen, width, height, memory, faceNameTextField.getText(), (byte)(faceSexComboBox.getSelectedIndex()), birthday, 
								privinceStr[facePrivinceComboBox.getSelectedIndex()], faceAddressTextField.getText(), faceIdIndexTextField.getText(), (byte)faceIdTypeComboBox.getSelectedIndex());
						findFaceRecognitionDB(String.valueOf(groupDefaultModel.getValueAt(rowSelectGroup, 2)));
					}
				});
				
				// 取消，关闭
				cancelFaceDBBtn.addActionListener(new ActionListener() {			
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dispose();		
					}
				});
			}		 
		}
		
		// 修改人员信息窗口的组件
		private PaintJPanel addImagePanel;
		private JButton selectImageBtn;
		
		private JLabel faceNameLabel;
		private JTextField faceNameTextField;
		private JLabel faceSexLabel;
		private JComboBox faceSexComboBox;
		private JLabel faceBirthdayLabel;
		private DateChooserJButtonEx faceBirthdayBtn; 
		private JLabel faceCountryLabel;
		private JTextField faceCountryTextField;
		private JLabel facePrivinceLabel;
		private JComboBox facePrivinceComboBox;
		private JLabel faceAddressLabel;
		private JTextField faceAddressTextField;
		private JLabel faceIdTypeLabel;
		private JComboBox faceIdTypeComboBox;
		private JLabel faceIdIndexLabel;
		private JTextField faceIdIndexTextField;
		private JButton addFaceDBBtn;
		private JButton cancelFaceDBBtn;
	}
	
	/**
	 * 用于人员信息展示窗口	
	 */
	public class FaceServerShowFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FaceServerShowFrame(){
			setTitle("人员信息展示");
			setSize(600, 600);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			FaceServerShowPanel faceServerShowPanel = new FaceServerShowPanel();
            add(faceServerShowPanel, BorderLayout.CENTER);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class FaceServerShowPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FaceServerShowPanel() {
				setBorderEx(this, "", 4);
				Dimension dim = this.getPreferredSize();
				dim.height = 400;
				dim.width = 180;
		
				setLayout(new BorderLayout());
				
				JPanel imagePanel = new JPanel();
				JPanel serverInfoPanel = new JPanel();
				serverInfoPanel.setPreferredSize(dim);
				
				add(imagePanel, BorderLayout.CENTER);
				add(serverInfoPanel, BorderLayout.EAST);
				
				/////////// 展示的人脸图片面板 //////////////////
				imagePanel.setLayout(new BorderLayout());
				addImagePanel = new PaintJPanel();   // 展示的人员信息图片显示
				imagePanel.add(addImagePanel, BorderLayout.CENTER);
				
				////////// 展示的人脸信息面板 /////////////////
				serverInfoPanel.setLayout(new FlowLayout());
				faceNameLabel = new JLabel("姓名 : ");
				faceNameTextField = new JTextField("", 9);
				faceSexLabel = new JLabel("性别 : ");
				
				faceSexComboBox = new JComboBox(faceSexStr);
				
				faceBirthdayLabel = new JLabel("生日 : ");
				faceBirthdayBtn = new DateChooserJButtonEx(); 
				faceCountryLabel = new JLabel("国际 : ");
				faceCountryTextField = new JTextField("中国", 9);
				facePrivinceLabel = new JLabel("省份 : ");
				
				String[] privinceStr = {"安徽省","北京市","重庆市","福建省","甘肃省","广东省","广西壮族自治区","贵州省",
						"海南省","河北省","河南省","黑龙江省","湖北省","湖南省","吉林省","江苏省","江西省","辽宁省",
						"内蒙古自治区","宁夏回族自治区","青海省","山东省","山西省","陕西省","上海市","四川省","天津市",
						"新疆维吾尔自治区","西藏自治区","云南省","浙江省","香港特别行政区","澳门特别行政区","台湾省"};
				facePrivinceComboBox = new JComboBox(privinceStr);
				
				faceAddressLabel = new JLabel("地址 : ");
				faceAddressTextField = new JTextField("", 9);

				faceIdTypeLabel = new JLabel("证件类型 : ");
				faceIdTypeComboBox = new JComboBox(idStr);
				faceIdIndexLabel = new JLabel("证件编号 : ");
				faceIdIndexTextField = new JTextField("", 8);
	
				serverInfoPanel.add(faceNameLabel);
				serverInfoPanel.add(faceNameTextField);
				serverInfoPanel.add(faceSexLabel);
				serverInfoPanel.add(faceSexComboBox);
				serverInfoPanel.add(faceBirthdayLabel);
				serverInfoPanel.add(faceBirthdayBtn);
				serverInfoPanel.add(faceCountryLabel);
				serverInfoPanel.add(faceCountryTextField);
				serverInfoPanel.add(facePrivinceLabel);
				serverInfoPanel.add(facePrivinceComboBox);
				serverInfoPanel.add(faceAddressLabel);
				serverInfoPanel.add(faceAddressTextField);		
				serverInfoPanel.add(faceIdTypeLabel);
				serverInfoPanel.add(faceIdTypeComboBox);
				serverInfoPanel.add(faceIdIndexLabel);
				serverInfoPanel.add(faceIdIndexTextField);
				
				faceSexComboBox.setPreferredSize(new Dimension(100, 20));
				faceBirthdayBtn.setPreferredSize(new Dimension(100, 20));
				facePrivinceComboBox.setPreferredSize(new Dimension(100, 20));
				faceIdTypeComboBox.setPreferredSize(new Dimension(70, 20));
				
				faceNameTextField.setEnabled(false);
				faceSexComboBox.setEnabled(false);
				faceBirthdayBtn.setEnabled(false);
				faceCountryTextField.setEnabled(false);
				facePrivinceComboBox.setEnabled(false);
				faceAddressTextField.setEnabled(false);
				faceIdTypeComboBox.setEnabled(false);
			}		 
		}
		
		// 展示人员信息窗口的组件
		private PaintJPanel addImagePanel;
		
		private JLabel faceNameLabel;
		private JTextField faceNameTextField;
		private JLabel faceSexLabel;
		private JComboBox faceSexComboBox;
		private JLabel faceBirthdayLabel;
		private DateChooserJButtonEx faceBirthdayBtn; 
		private JLabel faceCountryLabel;
		private JTextField faceCountryTextField;
		private JLabel facePrivinceLabel;
		private JComboBox facePrivinceComboBox;
		private JLabel faceAddressLabel;
		private JTextField faceAddressTextField;
		private JLabel faceIdTypeLabel;
		private JComboBox faceIdTypeComboBox;
		private JLabel faceIdIndexLabel;
		private JTextField faceIdIndexTextField;
	}
	
	/**
	 * 用于人脸识别事件
	 *
	 */
	public class FaceEventFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FaceEventFrame(){
			setTitle("人脸识别事件处理");
			setSize(900, 800);
			setLocationRelativeTo(null);
			setLayout(new BorderLayout());
			chnlist.clear();

			FaceRecognitionEventPanel faceRecognitionEventPanel = new FaceRecognitionEventPanel();
            add(faceRecognitionEventPanel, BorderLayout.CENTER);
            
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					renderPrivateData(m_hRealPlayHandle, 0);	
					stopRealPlay(m_hRealPlayHandle);
					detach(m_hAttachHandle);
					dispose();
				}
			});
		}
		public class FaceRecognitionEventPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public FaceRecognitionEventPanel() {
				setBorderEx(this, "", 4);
				setLayout(new BorderLayout());
				
				JPanel functionJpanel = new JPanel();   // 通道、预览、订阅		
				JPanel panel = new JPanel();	

				add(functionJpanel, BorderLayout.NORTH);
				add(panel, BorderLayout.CENTER);
				
				/////// 通道、预览、订阅按钮
				for(int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
					chnlist.add("通道 " + i);
				}
				chnComboBox = new JComboBox(chnlist);
				realplayBtn = new JButton("开始预览");
				stopRealplayBtn = new JButton("停止预览");
				attachBtn = new JButton("开始订阅");
				detachBtn = new JButton("停止订阅");
				openRenderPrivateDataBtn = new JButton("打开跟踪框");
				closeRenderPrivateDataBtn = new JButton("关闭跟踪框");

				functionJpanel.setLayout(new FlowLayout());
				functionJpanel.add(chnComboBox);
				functionJpanel.add(realplayBtn);
				functionJpanel.add(stopRealplayBtn);
				functionJpanel.add(attachBtn);
				functionJpanel.add(detachBtn);
				functionJpanel.add(openRenderPrivateDataBtn);
				functionJpanel.add(closeRenderPrivateDataBtn);
				
				chnComboBox.setPreferredSize(new Dimension(100, 25));
				
				//////  预览、图片面板
				JPanel panel1 = new JPanel();
				JPanel panel2 = new JPanel();
				JPanel panel3 = new JPanel();
				JPanel panel4 = new JPanel();
				
				panel1.setBorder(new EmptyBorder(5, 5, 5, 5));
				panel2.setBorder(new EmptyBorder(5, 5, 5, 5));
				panel3.setBorder(new EmptyBorder(5, 5, 5, 5));
				panel4.setBorder(new EmptyBorder(5, 5, 5, 5));
				
				panel.setLayout(new GridLayout(2, 2));
				panel.add(panel1);
				panel.add(panel2);
				panel.add(panel3);
				panel.add(panel4);
				
				// 预览
				JLabel realplaylabel = new JLabel("实时预览");
				realplayPanel = new Panel();
				realplayPanel.setBackground(new Color(153, 240, 255));
				panel1.setLayout(new BorderLayout());
				panel1.add(realplaylabel, BorderLayout.NORTH);
				panel1.add(realplayPanel, BorderLayout.CENTER);
		
				// 全景图
				JLabel globalPiclabel = new JLabel("全景图");
				globalPicPanel = new PaintJPanel();
				panel2.setLayout(new BorderLayout());
				panel2.add(globalPiclabel, BorderLayout.NORTH);
				panel2.add(globalPicPanel, BorderLayout.CENTER);
				
				// 人脸图
				JLabel personPiclabel = new JLabel("人脸图");
				personPicPanel = new PaintJPanel();
				JPanel facedataPanel = new JPanel();
				panel3.setLayout(new BorderLayout());
				panel3.add(personPiclabel, BorderLayout.NORTH);
				panel3.add(personPicPanel, BorderLayout.CENTER);
				panel3.add(facedataPanel, BorderLayout.EAST);
				
				JLabel sexLabel = new JLabel("性别", JLabel.CENTER);
				JLabel ageLabel = new JLabel("年龄", JLabel.CENTER);
				JLabel eyeLabel = new JLabel("眼睛", JLabel.CENTER);
				JLabel unknowLabel = new JLabel("", JLabel.CENTER);
				JLabel mouthLabel = new JLabel("嘴", JLabel.CENTER);
				JLabel maskLabel = new JLabel("口罩", JLabel.CENTER);
				JLabel beardLabel = new JLabel("胡子", JLabel.CENTER);
				sexTextField = new JTextField("", 7);
				ageTextField = new JTextField("", 7);
				eyeTextField = new JTextField("", 7);
				raceTextField = new JTextField("", 7);
				mouthTextField = new JTextField("", 7);
				maskTextField = new JTextField("", 7);
				beardTextField = new JTextField("", 7);
				
				sexTextField.setHorizontalAlignment(JTextField.CENTER);
				ageTextField.setHorizontalAlignment(JTextField.CENTER);
				eyeTextField.setHorizontalAlignment(JTextField.CENTER);
				raceTextField.setHorizontalAlignment(JTextField.CENTER);
				mouthTextField.setHorizontalAlignment(JTextField.CENTER);
				maskTextField.setHorizontalAlignment(JTextField.CENTER);
				beardTextField.setHorizontalAlignment(JTextField.CENTER);
				
				facedataPanel.setLayout(new GridLayout(10, 2));
				facedataPanel.add(sexLabel);
				facedataPanel.add(sexTextField);
				facedataPanel.add(ageLabel);
				facedataPanel.add(ageTextField);
				facedataPanel.add(eyeLabel);
				facedataPanel.add(eyeTextField);
				facedataPanel.add(unknowLabel);
				facedataPanel.add(raceTextField);
				facedataPanel.add(mouthLabel);
				facedataPanel.add(mouthTextField);
				facedataPanel.add(maskLabel);
				facedataPanel.add(maskTextField);
				facedataPanel.add(beardLabel);
				facedataPanel.add(beardTextField);
	
				// 对比图
				JLabel candidatelabel = new JLabel("对比图");
				candidatePanel = new PaintJPanel();
				JPanel candidatedataPanel = new JPanel();
				panel4.setLayout(new BorderLayout());
				panel4.add(candidatelabel, BorderLayout.NORTH);
				panel4.add(candidatePanel, BorderLayout.CENTER);
				panel4.add(candidatedataPanel, BorderLayout.EAST);

				JLabel groupIdLabel = new JLabel("人脸库ID", JLabel.CENTER);
				JLabel groupNameLabel = new JLabel("人脸库名称", JLabel.CENTER);
				JLabel personNameLabel = new JLabel("人员名称", JLabel.CENTER);
				JLabel similaryLabel = new JLabel("相似度", JLabel.CENTER);
				groupIdTextField = new JTextField("", 7);
				groupNameTextField = new JTextField("", 7);
				personNameTextField = new JTextField("", 7);
				similaryTextField = new JTextField("", 7);
				JLabel nullLabel1 = new JLabel();
				JLabel nullLabel2 = new JLabel();
				JLabel nullLabel3 = new JLabel();

				
				groupIdTextField.setHorizontalAlignment(JTextField.CENTER);
				groupNameTextField.setHorizontalAlignment(JTextField.CENTER);
				personNameTextField.setHorizontalAlignment(JTextField.CENTER);
				similaryTextField.setHorizontalAlignment(JTextField.CENTER);
				
				candidatedataPanel.setLayout(new GridLayout(9, 2));
				candidatedataPanel.add(groupIdLabel);
				candidatedataPanel.add(groupIdTextField);
				candidatedataPanel.add(groupNameLabel);
				candidatedataPanel.add(groupNameTextField);
				candidatedataPanel.add(personNameLabel);
				candidatedataPanel.add(personNameTextField);
				candidatedataPanel.add(similaryLabel);
				candidatedataPanel.add(similaryTextField);
				candidatedataPanel.add(nullLabel1);
				candidatedataPanel.add(nullLabel2);
				candidatedataPanel.add(nullLabel3);
				
				stopRealplayBtn.setEnabled(false);
				detachBtn.setEnabled(false);
				closeRenderPrivateDataBtn.setEnabled(false);
				
				// 预览
				realplayBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(startRealPlay(chnComboBox.getSelectedIndex())) {
							realplayBtn.setEnabled(false);
							stopRealplayBtn.setEnabled(true);
						}
					}
				});
				
				// 停止预览
				stopRealplayBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						// 停止实时预览后，会关闭跟踪的功能
						renderPrivateData(m_hRealPlayHandle, 0);	
						openRenderPrivateDataBtn.setEnabled(true);
						closeRenderPrivateDataBtn.setEnabled(false);
						
						stopRealPlay(m_hRealPlayHandle);
						realplayPanel.repaint();
						realplayBtn.setEnabled(true);
						stopRealplayBtn.setEnabled(false);		
					}
				});
				
				// 订阅
				attachBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(attach(chnComboBox.getSelectedIndex())) {
							attachBtn.setEnabled(false);
							detachBtn.setEnabled(true);
						}
					}
				});
				
				// 停止订阅
				detachBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						detach(m_hAttachHandle);
						attachBtn.setEnabled(true);
						detachBtn.setEnabled(false);
					}
				});
				
				// 打开跟踪框
				openRenderPrivateDataBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(!renderPrivateData(m_hRealPlayHandle, 1)) {			
							JOptionPane.showMessageDialog(null, "请确认是否打开实时预览！");
						} else {
							openRenderPrivateDataBtn.setEnabled(false);
							closeRenderPrivateDataBtn.setEnabled(true);
						}
					}
				});
				
				// 关闭跟踪框
				closeRenderPrivateDataBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						renderPrivateData(m_hRealPlayHandle, 0);	
						openRenderPrivateDataBtn.setEnabled(true);
						closeRenderPrivateDataBtn.setEnabled(false);
					}
				});
			}
		}
	}
	
	/**
	 * 以图搜图窗口
	 */
	public class SearchByPictureFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SearchByPictureFrame(){
			setTitle("以图搜图");
			setSize(1165, 500);
			setLocationRelativeTo(null);
			setLayout(new BorderLayout());

			searchByPicPanelList.clear();
			if(searchByPicTextFieldlList != null) {
				searchByPicTextFieldlList.clear();
			}
			searchByPicTextFieldMap.clear();
			searchShowPanelList.clear();
			chnlist.clear();
			memory = null;
			
			if(attachFaceHandle.longValue() != 0) {
				netsdkApi.CLIENT_DetachFaceFindState(attachFaceHandle);
			}	
			
			SearchByPicturePanel searchByPicturePanel = new SearchByPicturePanel();
            add(searchByPicturePanel, BorderLayout.CENTER);
            
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class SearchByPicturePanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public SearchByPicturePanel() {
				setBorderEx(this, "", 4);
				setLayout(new BorderLayout());
				
				JPanel panel = new JPanel();  // 查找跟展示面板
				JPanel progressPanel = new JPanel();
				
				add(panel, BorderLayout.CENTER);
				add(progressPanel, BorderLayout.SOUTH);
					
				//////// 进度面板
				progressBar = new JProgressBar(0, 100);
				progressBar.setPreferredSize(new Dimension(100, 20));
				progressBar.setStringPainted(true);
				progressPanel.setLayout(new BorderLayout());
				progressPanel.add(progressBar, BorderLayout.CENTER);
				
				////////////// 查找与展示面板添加
				panel.setLayout(new BorderLayout());	
				
				JPanel searchPanel = new JPanel();
				showPanel = new JPanel();
				
				JScrollPane showScrollPane = new JScrollPane(showPanel);
				showScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				
				showPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
				searchPanel.setPreferredSize(new Dimension(220, 20));
				
				panel.add(searchPanel, BorderLayout.WEST);
				panel.add(showScrollPane, BorderLayout.CENTER);
				
				showPanel.setLayout(new FlowLayout());
				showPanel.add(addSearchByPicShowPanel());
				showPanel.add(addSearchByPicShowPanel());
				
				////////////// 查找面板
				JLabel searchLabel = new JLabel("选择条件以图搜图");
				searchByPaintJPanel = new PaintJPanel();
				JPanel conditionPanel = new JPanel();
				
				searchPanel.setLayout(new BorderLayout());
				
				searchPanel.add(searchLabel, BorderLayout.NORTH);
				searchPanel.add(searchByPaintJPanel, BorderLayout.CENTER);
				searchPanel.add(conditionPanel, BorderLayout.SOUTH);
				
				JLabel startTimeLabel = new JLabel("开始时间: ", JLabel.CENTER);
				JLabel endTimeLabel = new JLabel("结束时间: ", JLabel.CENTER);
				JLabel channelLabel = new JLabel("通道号: ", JLabel.CENTER);
				JLabel similaryLabel = new JLabel("相似度: ", JLabel.CENTER);
				startTimeBtn = new DateChooserJButtonEx();
				endTimeBtn = new DateChooserJButtonEx();
				for(int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
					chnlist.add("通道 " + i);
				}
				channelSearchCheckBox = new JComboBox(chnlist);
				similarySearchTextField = new JTextField();			
				JButton openPicBtn = new JButton("添加图片");
				searchByPicBtu = new JButton("查找");
				
				conditionPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
				conditionPanel.setPreferredSize(new Dimension(20, 150));
				conditionPanel.setLayout(new FlowLayout());
				
				startTimeBtn.setPreferredSize(new Dimension(130, 24));
				endTimeBtn.setPreferredSize(new Dimension(130, 24));
				channelSearchCheckBox.setPreferredSize(new Dimension(140, 24));
				similarySearchTextField.setPreferredSize(new Dimension(140, 24));
				openPicBtn.setPreferredSize(new Dimension(100, 24));
				searchByPicBtu.setPreferredSize(new Dimension(90, 24));
				
				conditionPanel.add(startTimeLabel);
				conditionPanel.add(startTimeBtn);
				conditionPanel.add(endTimeLabel);
				conditionPanel.add(endTimeBtn);
				conditionPanel.add(channelLabel);
				conditionPanel.add(channelSearchCheckBox);
				conditionPanel.add(similaryLabel);
				conditionPanel.add(similarySearchTextField);
				conditionPanel.add(openPicBtn);
				conditionPanel.add(searchByPicBtu);

				
				openPicBtn.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {	
						if(!selectPicture(searchByPaintJPanel)) {
							memory = null;
						}
					}
				});
				
				searchByPicBtu.addActionListener(new ActionListener() {					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						progressBar.setValue(0);

						for(int i = 0; i < searchShowPanelList.size(); i++) {
							 showPanel.remove(searchShowPanelList.get(i));
						}
						
						searchByPicPanelList.clear();
						searchByPicTextFieldMap.clear();
						searchShowPanelList.clear();
						
						showPanel.setPreferredSize(new Dimension(2 * 445, 380));
						
					    showPanel.add(addSearchByPicShowPanel()); 
						showPanel.add(addSearchByPicShowPanel());
		               
	                	showPanel.updateUI();
	                	
	                	if(memory == null) {
	                		JOptionPane.showMessageDialog(null, "请先添加一张图片！！！");
	                		return;
	                	}
						searchByPicture(startTimeBtn.getText(), 
										endTimeBtn.getText(), 
										channelSearchCheckBox.getSelectedIndex(), 
										similarySearchTextField.getText(), 
										memory, nPicBufLen);
						
					     searchByPicBtu.setEnabled(false);
						
					}
				});
				
			}
		}
	}
	
	public JPanel addSearchByPicShowPanel() {			
		JPanel panel = new JPanel();
		
		panel.setBorder(new LineBorder(new Color(255, 0, 0)));
		panel.setPreferredSize(new Dimension(440, 370));  // 设置高
		
		JLabel showLabel = new JLabel("查询到的图片");
		PaintJPanel showPaintJPanel = new PaintJPanel();
		JPanel showInfoJPanel = new JPanel();
		
		panel.setLayout(new BorderLayout());
		
		panel.add(showLabel, BorderLayout.NORTH);
		panel.add(showPaintJPanel, BorderLayout.CENTER);
		panel.add(showInfoJPanel, BorderLayout.EAST);
		
		JLabel timeLabel = new JLabel(" 时间:  ");
		JLabel sexLabel = new JLabel(" 性别:  ");
		JLabel ageLabel = new JLabel(" 年龄:  ");
		JLabel unknowLabel = new JLabel(" 未知:  ");
		JLabel eyeLabel = new JLabel(" 眼睛:  ");
		JLabel mouthLabel = new JLabel(" 嘴巴:  ");
		JLabel maskLabel = new JLabel(" 口罩:  ");
		JLabel beardLabel = new JLabel(" 胡子:  ");
		JLabel glassesLabel = new JLabel(" 眼镜:  ");
		JLabel similaryLabel = new JLabel("相似度:");
		JLabel nameLabel = new JLabel(" 姓名:  ");
		JLabel birthdayLabel = new JLabel(" 生日:  ");
		JLabel cardTypeLabel = new JLabel("证件类型:");
		JLabel cardIdLabel = new JLabel("证件编号:");
		
		JTextField timeByPicTextField = new JTextField("", 12);
		JTextField sexByPicTextField = new JTextField("", 12);
		JTextField ageByPicTextField = new JTextField("", 12);
		JTextField unknowByPicTextField = new JTextField("", 12);
		JTextField eyeByPicTextField = new JTextField("", 12);
		JTextField mouthByPicTextField = new JTextField("", 12);
		JTextField maskByPicTextField = new JTextField("", 12);
		JTextField beardByPicTextField = new JTextField("", 12);
		JTextField glassesByPicTextField = new JTextField("", 12);
		JTextField similaryByPicTextField = new JTextField("", 12);
		JTextField nameByPicTextField = new JTextField("", 12);
		JTextField birthdayByPicTextField = new JTextField("", 12);
		JTextField cardTypeByPicTextField = new JTextField("", 11);
		JTextField cardIdByPicTextField = new JTextField("", 11);
		
		timeByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		sexByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		ageByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		unknowByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		eyeByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		mouthByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		maskByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		beardByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		glassesByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		similaryByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		nameByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		birthdayByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		cardTypeByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		cardIdByPicTextField.setHorizontalAlignment(JTextField.CENTER);
		
		timeByPicTextField.setEnabled(false);
		sexByPicTextField.setEnabled(false);
		ageByPicTextField.setEnabled(false);
		unknowByPicTextField.setEnabled(false);
		eyeByPicTextField.setEnabled(false);
		mouthByPicTextField.setEnabled(false);
		maskByPicTextField.setEnabled(false);
		beardByPicTextField.setEnabled(false);
		glassesByPicTextField.setEnabled(false);
		similaryByPicTextField.setEnabled(false);
		nameByPicTextField.setEnabled(false);
		birthdayByPicTextField.setEnabled(false);
		cardTypeByPicTextField.setEnabled(false);
		cardIdByPicTextField.setEnabled(false);
		
		showInfoJPanel.setPreferredSize(new Dimension(220, 350));   // 设置宽
		showInfoJPanel.setLayout(new FlowLayout());
		
		showInfoJPanel.add(timeLabel);
		showInfoJPanel.add(timeByPicTextField);
		showInfoJPanel.add(sexLabel);
		showInfoJPanel.add(sexByPicTextField);
		showInfoJPanel.add(ageLabel);
		showInfoJPanel.add(ageByPicTextField);
		showInfoJPanel.add(unknowLabel);
		showInfoJPanel.add(unknowByPicTextField);
		showInfoJPanel.add(eyeLabel);
		showInfoJPanel.add(eyeByPicTextField);
		showInfoJPanel.add(mouthLabel);
		showInfoJPanel.add(mouthByPicTextField);
		showInfoJPanel.add(maskLabel);
		showInfoJPanel.add(maskByPicTextField);
		showInfoJPanel.add(beardLabel);
		showInfoJPanel.add(beardByPicTextField);		
		showInfoJPanel.add(glassesLabel);
		showInfoJPanel.add(glassesByPicTextField);		
		showInfoJPanel.add(similaryLabel);
		showInfoJPanel.add(similaryByPicTextField);			
		showInfoJPanel.add(nameLabel);
		showInfoJPanel.add(nameByPicTextField);
		showInfoJPanel.add(birthdayLabel);
		showInfoJPanel.add(birthdayByPicTextField);
		showInfoJPanel.add(cardTypeLabel);
		showInfoJPanel.add(cardTypeByPicTextField);
		showInfoJPanel.add(cardIdLabel);
		showInfoJPanel.add(cardIdByPicTextField);
		
		searchByPicTextFieldlList = new ArrayList<JTextField>();
		
		searchByPicTextFieldlList.add(timeByPicTextField);
		searchByPicTextFieldlList.add(sexByPicTextField);
		searchByPicTextFieldlList.add(ageByPicTextField);
		searchByPicTextFieldlList.add(unknowByPicTextField);
		searchByPicTextFieldlList.add(eyeByPicTextField);
		searchByPicTextFieldlList.add(mouthByPicTextField);
		searchByPicTextFieldlList.add(maskByPicTextField);
		searchByPicTextFieldlList.add(beardByPicTextField);
		searchByPicTextFieldlList.add(glassesByPicTextField);
		searchByPicTextFieldlList.add(similaryByPicTextField);	
		searchByPicTextFieldlList.add(nameByPicTextField);
		searchByPicTextFieldlList.add(birthdayByPicTextField);
		searchByPicTextFieldlList.add(cardTypeByPicTextField);
		searchByPicTextFieldlList.add(cardIdByPicTextField);

		searchByPicPanelList.add(showPaintJPanel);
		searchByPicTextFieldMap.put(showPaintJPanel, searchByPicTextFieldlList);
		
		searchShowPanelList.add(panel);
		return panel;
	}
	
	/**
	 * 用于布控
	 *
	 */
	public class PutDispositionFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public PutDispositionFrame(String groupId){
			setTitle("人脸库布控");
			setSize(300, 600);
			setLocationRelativeTo(null);
			setLayout(new BorderLayout());
			
			putDispositionArrayList.clear();
			putDispositionHashMap.clear();

			PutDispositionPanel putDispositionPanel = new PutDispositionPanel(groupId);
            add(putDispositionPanel, BorderLayout.CENTER);
            
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class PutDispositionPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public PutDispositionPanel(final String groupId) {
				setBorderEx(this, "", 4);
				setLayout(new BorderLayout());
				
				JPanel panel1 = new JPanel();		
				final JPanel panel2 = new JPanel();	
				Dimension dimension = new Dimension();
				dimension.height = 2000;
				panel2.setPreferredSize(dimension);
				
				JScrollPane scrollPane = new JScrollPane(panel2);

				add(panel1, BorderLayout.NORTH);
				add(scrollPane, BorderLayout.CENTER);
				
				JButton addChnSimilaryBtn = new JButton("+");
				JButton confirmBtn = new JButton("布控");
				JButton cancelBtn = new JButton("取消");
				
				panel1.setLayout(new FlowLayout());
				panel2.setLayout(new FlowLayout());
				
				panel1.add(addChnSimilaryBtn);
				panel1.add(cancelBtn);
				panel1.add(confirmBtn);
				
				NET_FACERECONGNITION_GROUP_INFO[] groupInfo = findSingleGroupInfo(groupId);
				
				final int count = groupInfo[0].nRetChnCount > groupInfo[0].nRetSimilarityCount? groupInfo[0].nRetChnCount : groupInfo[0].nRetSimilarityCount;
				
				for(int j = 0; j < count; j++) {				
					JPanel jPanel = addPanel(); 
					panel2.add(jPanel);
					
					putDispositionArrayList.get(j).setText(String.valueOf(groupInfo[0].nChannel[j]));
					putDispositionHashMap.get(putDispositionArrayList.get(j)).setText(String.valueOf(groupInfo[0].nSimilarity[j]));
				}
				
				addChnSimilaryBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						SwingUtilities.invokeLater(new Runnable() {					
							@Override
							public void run() {
								JPanel jPanel = addPanel();
								panel2.add(jPanel);	
								panel2.validate();
								panel2.repaint();
							}
						});

					}
				});
				
				confirmBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(putFaceRecognitionDisposition(groupId)) {
							JOptionPane.showMessageDialog(null, "布控成功！");
						}
						findGroupInfo("");
						dispose();
					}
				});
				
				cancelBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
			}
	
			public JPanel addPanel() {
				JPanel panel = new JPanel();
				panel.setLayout(new FlowLayout());
				
				JLabel chnLabel = new JLabel("视频通道:");
				JLabel similaryLabel = new JLabel("相似度:");
				JTextField chnTextField = new JTextField("", 4);
				JTextField similaryTextField = new JTextField("", 4);
				
				panel.add(chnLabel);
				panel.add(chnTextField);
				panel.add(similaryLabel);
				panel.add(similaryTextField);
				
				putDispositionArrayList.add(chnTextField);
				putDispositionHashMap.put(chnTextField, similaryTextField);

				return panel;
			}
		}

	}
	
	/**
	 * 用于撤控
	 *
	 */
	public class DelDispositionFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public DelDispositionFrame(String groupId){
			setTitle("人脸库撤控");
			setSize(240, 600);
			setLocationRelativeTo(null);
			setLayout(new BorderLayout());
			
			deltDispositionArrayList.clear();

			DelDispositionPanel delDispositionPanel = new DelDispositionPanel(groupId);
            add(delDispositionPanel, BorderLayout.CENTER);
            
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class DelDispositionPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public DelDispositionPanel(final String groupId) {
				setBorderEx(this, "", 4);
				setLayout(new BorderLayout());
				
				JPanel panel1 = new JPanel();		
				final JPanel panel2 = new JPanel();	
				Dimension dimension = new Dimension();
				dimension.height = 2000;
				panel2.setPreferredSize(dimension);
				
				JScrollPane scrollPane = new JScrollPane(panel2);

				add(panel1, BorderLayout.NORTH);
				add(scrollPane, BorderLayout.CENTER);
				
				JButton confirmBtn = new JButton("撤控");
				JButton cancelBtn = new JButton("取消");
				
				panel1.setLayout(new FlowLayout());
				panel2.setLayout(new FlowLayout());
				
				panel1.add(cancelBtn);
				panel1.add(confirmBtn);
				
				NET_FACERECONGNITION_GROUP_INFO[] groupInfo = findSingleGroupInfo(groupId);
				for(int j = 0; j < groupInfo[0].nRetChnCount; j++) {				
					JCheckBox checkBox = addCheckBox(); 
					panel2.add(checkBox);
					checkBox.setText("已布控的通道号" + String.valueOf(groupInfo[0].nChannel[j]));
				}

				
				confirmBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(delFaceRecognitionDisposition(groupId)) {
							JOptionPane.showMessageDialog(null, "撤控成功！");
						}
						findGroupInfo("");
						dispose();
					}
				});
				
				cancelBtn.addActionListener(new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
			}
	
			public JCheckBox addCheckBox() {
				JCheckBox chnCheckBox = new JCheckBox();
				deltDispositionArrayList.add(chnCheckBox);
				
				return chnCheckBox;
			}
		}

	}
	
	/***********************************************************************************************
	 * 									具体功能的接口实现										   *
	 ***********************************************************************************************/
	// 登录按钮事件
	private boolean login() {
		m_strIp = ipTextArea.getText();
		m_nPort = Integer.parseInt(portTextArea.getText());	
		m_strUser = nameTextArea.getText();
		m_strPassword = new String(passwordTextArea.getPassword());
		
		System.out.println("设备地址：" + m_strIp + "\n端口号：" + m_nPort 
				+ "\n用户名：" + m_strUser + "\n密码：" + m_strPassword);
		
		int nSpecCap = EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP; //=0
		IntByReference nError = new IntByReference(0);
		m_hLoginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser, m_strPassword, nSpecCap, null, m_stDeviceInfo, nError);
		if(m_hLoginHandle.longValue() == 0) {
			System.err.printf("Login Device[%s] Port[%d]Failed.\n", m_strIp, m_nPort, ToolKits.getErrorCode());
			JOptionPane.showMessageDialog(this, "Login Failed，Error ：" + ToolKits.getErrorCode());	
			return false;
		} else {
			System.out.println("Login Success [ " + m_strIp + " ]");
		}
		return true;
	}
	
	// 登出按钮事件
	private boolean logout() {
		if(m_hLoginHandle.longValue() != 0) {
			System.out.println("Logout Button Action");
		
			if(netsdkApi.CLIENT_Logout(m_hLoginHandle)) {
				System.out.println("Logout Success [ " + m_strIp + " ]");
				m_hLoginHandle.setValue(0);	
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 开始预览
	 * @param channel 通道号
	 */
	public boolean startRealPlay(int channel) {
		System.out.println(channel);
		int playType = NET_RealPlayType.NET_RType_Realplay; // 实时预览
		
		m_hRealPlayHandle = netsdkApi.CLIENT_RealPlayEx(m_hLoginHandle, channel, Native.getComponentPointer(realplayPanel), playType);
		if (m_hRealPlayHandle.longValue() == 0) {
			System.err.println("开始实时监视失败，错误码：" + ToolKits.getErrorCode());
			return false;
		} else {
			System.out.println("Success to start realplay");
    	}
		return true;
	}
	
	/**
	 * 停止预览
	 * @param m_hRealPlayHandle 实时预览句柄
	 */
	public void stopRealPlay(LLong m_hRealPlayHandle) {
		if(m_hRealPlayHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopRealPlayEx(m_hRealPlayHandle);
		}
	}
    
	/**
	 * 添加人脸库
	 * @param groupName 需要添加的人脸库名称
	 */
	public boolean addFaceRecognitionGroup(String groupName) {
		if(groupName.equals("") || groupName == null) {
			JOptionPane.showMessageDialog(this, "请输入需要添加的人脸库名称！");
			return false;
		}

		NET_ADD_FACERECONGNITION_GROUP_INFO addGroupInfo = new NET_ADD_FACERECONGNITION_GROUP_INFO();
		try {
			System.arraycopy(groupName.getBytes(encode), 
							 0, 
							 addGroupInfo.stuGroupInfo.szGroupName, 
							 0, 
							 groupName.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}   
		
		// 入参
		NET_IN_OPERATE_FACERECONGNITION_GROUP stIn = new NET_IN_OPERATE_FACERECONGNITION_GROUP();
		stIn.emOperateType = EM_OPERATE_FACERECONGNITION_GROUP_TYPE.NET_FACERECONGNITION_GROUP_ADD; // 添加人员组信息
		stIn.pOPerateInfo = addGroupInfo.getPointer();		
		
		//出参
		NET_OUT_OPERATE_FACERECONGNITION_GROUP stOut = new NET_OUT_OPERATE_FACERECONGNITION_GROUP();
		
		addGroupInfo.write();
		boolean bRet = netsdkApi.CLIENT_OperateFaceRecognitionGroup(m_hLoginHandle, stIn, stOut, 4000);
		addGroupInfo.read();
		
		if(bRet) {
			System.out.println("人员组ID : " + new String(stOut.szGroupId).trim());  // 新增记录的人员组ID,唯一标识一组人员
			JOptionPane.showMessageDialog(this, "添加人脸库成功");
		} else {
			JOptionPane.showMessageDialog(this, "添加人脸库失败：" + ToolKits.getErrorCode());	
			return false;
		}

		return true;
	}
	
	/**
	 * 修改人脸库
	 * @param groupName 修改后的人脸库名称
	 * @param groupId 需要修改的人脸库ID
	 */
	public boolean modifyFaceRecognitionGroup(String groupName, String groupId) {
		if(groupName.equals("") || groupName == null) {
			JOptionPane.showMessageDialog(this, "请输入修改后的人脸库名称！");
			return false;
		}
		
		NET_MODIFY_FACERECONGNITION_GROUP_INFO modifyGroupInfo = new NET_MODIFY_FACERECONGNITION_GROUP_INFO();
		try {
			System.arraycopy(groupName.getBytes(encode), 
							 0, 
							 modifyGroupInfo.stuGroupInfo.szGroupName, 
							 0, 
							 groupName.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  
		System.arraycopy(groupId.getBytes(), 
						 0, 
						 modifyGroupInfo.stuGroupInfo.szGroupId, 
						 0, 
						 groupId.getBytes().length);    // 给人员组ID赋值，要用数组拷贝
		
		// 入参
		NET_IN_OPERATE_FACERECONGNITION_GROUP stIn = new NET_IN_OPERATE_FACERECONGNITION_GROUP();
		stIn.emOperateType = EM_OPERATE_FACERECONGNITION_GROUP_TYPE.NET_FACERECONGNITION_GROUP_MODIFY; // 修改人员组信息
		stIn.pOPerateInfo = modifyGroupInfo.getPointer();		
		
		//出参
		NET_OUT_OPERATE_FACERECONGNITION_GROUP stOut = new NET_OUT_OPERATE_FACERECONGNITION_GROUP();
		
		modifyGroupInfo.write();
		boolean bRet = netsdkApi.CLIENT_OperateFaceRecognitionGroup(m_hLoginHandle, stIn, stOut, 4000);
		modifyGroupInfo.read();
		
		if(bRet) {
			JOptionPane.showMessageDialog(this, "修改人脸库成功");
		} else {
			JOptionPane.showMessageDialog(this, "修改人脸库失败：" + ToolKits.getErrorCode());	
			return false;
		}
		
		return true;
	}
	
	/**
	 * 删除人脸库
	 * @param groupId 需要删除的人脸库ID; 为空表示删除所有的人脸库
	 */
	public boolean deleteFaceRecognitionGroup(String groupId) {
		NET_DELETE_FACERECONGNITION_GROUP_INFO deleteGroupInfo = new NET_DELETE_FACERECONGNITION_GROUP_INFO();
		System.arraycopy(groupId.getBytes(), 
						 0, 
						 deleteGroupInfo.szGroupId, 
						 0, 
						 groupId.getBytes().length);    // 给人员组ID赋值，要用数组拷贝
		
		// 入参
		NET_IN_OPERATE_FACERECONGNITION_GROUP stIn = new NET_IN_OPERATE_FACERECONGNITION_GROUP();
		stIn.emOperateType = EM_OPERATE_FACERECONGNITION_GROUP_TYPE.NET_FACERECONGNITION_GROUP_DELETE; // 删除人员组信息
		stIn.pOPerateInfo = deleteGroupInfo.getPointer();		
		
		//出参
		NET_OUT_OPERATE_FACERECONGNITION_GROUP stOut = new NET_OUT_OPERATE_FACERECONGNITION_GROUP();
		
		deleteGroupInfo.write();
		boolean bRet = netsdkApi.CLIENT_OperateFaceRecognitionGroup(m_hLoginHandle, stIn, stOut, 4000);
		deleteGroupInfo.read();
		
		if(bRet) {
			JOptionPane.showMessageDialog(this, "删除人脸库成功!");
		} else {
			JOptionPane.showMessageDialog(this, "删除人脸库失败：" + ToolKits.getErrorCode());	
			return false;
		}

		return true;
	}
	
	/**
	 * 以人脸库的角度进行布控
	 * @param groupId 人脸库ID
	 */
	public boolean putFaceRecognitionDisposition(String groupId) {
		// 入参
		NET_IN_FACE_RECOGNITION_PUT_DISPOSITION_INFO stIn = new NET_IN_FACE_RECOGNITION_PUT_DISPOSITION_INFO();
		// 人脸库ID
		System.arraycopy(groupId.getBytes(), 0, stIn.szGroupId, 0, groupId.getBytes().length);
			
		int j = 0;
		
		for(int i = 0; i < putDispositionArrayList.size(); i++) {
			if(!putDispositionArrayList.get(i).getText().equals("") || !putDispositionHashMap.get(putDispositionArrayList.get(i)).getText().equals("")) {
				if(!putDispositionArrayList.get(i).getText().equals("")) {
					stIn.stuDispositionChnInfo[j].nChannelID = Integer.parseInt(putDispositionArrayList.get(i).getText());
				}
				if(!putDispositionHashMap.get(putDispositionArrayList.get(i)).getText().equals("")) {
					stIn.stuDispositionChnInfo[j].nSimilary = Integer.parseInt(putDispositionHashMap.get(putDispositionArrayList.get(i)).getText());
				}
	
				j++;
			}
		}
		stIn.nDispositionChnNum = j;        // 布控视频通道个数
		
		// 出参
		NET_OUT_FACE_RECOGNITION_PUT_DISPOSITION_INFO stOut = new NET_OUT_FACE_RECOGNITION_PUT_DISPOSITION_INFO();
		
		if(netsdkApi.CLIENT_FaceRecognitionPutDisposition(m_hLoginHandle, stIn, stOut, 4000)) {
//			System.out.println("通道布控结果个数:" + stOut.nReportCnt);
			
			for(int i = 0; i < stOut.nReportCnt; i++) {
				if(stOut.bReport[i] == 1) {
					System.out.println("通道布控结果 : 成功。");
				} else {
					System.err.println("通道布控结果 : 失败。");			
				}
			}	
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * 以人脸库的角度进行撤控
	 * @param groupId 人脸库ID
	 */
	public boolean delFaceRecognitionDisposition(String groupId) {
		// 入参
		NET_IN_FACE_RECOGNITION_DEL_DISPOSITION_INFO stIn = new NET_IN_FACE_RECOGNITION_DEL_DISPOSITION_INFO();
		// 人脸库ID
		System.arraycopy(groupId.getBytes(), 0, stIn.szGroupId, 0, groupId.getBytes().length);
		
		int j = 0;
		
		for(int i = 0; i < deltDispositionArrayList.size(); i++) {
			if(deltDispositionArrayList.get(i).isSelected()) {
				String chnString = deltDispositionArrayList.get(i).getText().substring(7);
				stIn.nDispositionChn[j] = Integer.parseInt(chnString);  // 撤控视频通道列表
				
				j++;
			}
		}
		stIn.nDispositionChnNum = j;        // 撤控视频通道个数
		
		// 出参
		NET_OUT_FACE_RECOGNITION_DEL_DISPOSITION_INFO stOut = new NET_OUT_FACE_RECOGNITION_DEL_DISPOSITION_INFO();
		
		if(deltDispositionArrayList.size() > 0 && netsdkApi.CLIENT_FaceRecognitionDelDisposition(m_hLoginHandle, stIn, stOut, 4000)) {
//			System.out.println("通道撤控结果个数:" + stOut.nReportCnt);
			
			for(int i = 0; i < stOut.nReportCnt; i++) {
				if(stOut.bReport[i] == 1) {
					System.out.println("通道撤控结果 : 成功。");
				} else {
					System.err.println("通道撤控结果 : 失败。");
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * 查询单个人脸库人员组信息，用于布控 和 撤控
	 * @param groupId 需要查找的人脸库ID; 
	 */
	public NET_FACERECONGNITION_GROUP_INFO[] findSingleGroupInfo(String groupId) {
		// 入参
		NET_IN_FIND_GROUP_INFO stIn = new NET_IN_FIND_GROUP_INFO();
		System.arraycopy(groupId.getBytes(), 0, stIn.szGroupId, 0, groupId.getBytes().length);   

		// 出参
		int max = 20;
		NET_FACERECONGNITION_GROUP_INFO[] groupInfo = new NET_FACERECONGNITION_GROUP_INFO[max];
		for(int i = 0; i < max; i++) {
			groupInfo[i] = new NET_FACERECONGNITION_GROUP_INFO();
		}
		
		NET_OUT_FIND_GROUP_INFO stOut = new NET_OUT_FIND_GROUP_INFO();   
		stOut.pGroupInfos = new Memory(groupInfo[0].size() * 20);     // Pointer初始化
		stOut.pGroupInfos.clear(groupInfo[0].size() * 20);
		stOut.nMaxGroupNum = max;
		
		ToolKits.SetStructArrToPointerData(groupInfo, stOut.pGroupInfos);  // 将数组内存拷贝给Pointer

		if(netsdkApi.CLIENT_FindGroupInfo(m_hLoginHandle, stIn, stOut, 4000)) {
			ToolKits.GetPointerDataToStructArr(stOut.pGroupInfos, groupInfo);     // 将Pointer的值输出到 数组 NET_FACERECONGNITION_GROUP_INFO
		} else {
			System.err.println("查询人员信息失败" + ToolKits.getErrorCode());
		}
		
		return groupInfo;
	}
	
	/**
	 * 查询人脸库人员组信息
	 * @param groupId 需要查找的人脸库ID; 为空表示查找所有的人脸库
	 */
	public boolean findGroupInfo(String groupId) {
		clearGroupList();
		
		// 入参
		NET_IN_FIND_GROUP_INFO stIn = new NET_IN_FIND_GROUP_INFO();
		System.arraycopy(groupId.getBytes(), 0, stIn.szGroupId, 0, groupId.getBytes().length);   

		// 出参
		int max = 20;
		NET_FACERECONGNITION_GROUP_INFO[] groupInfo = new NET_FACERECONGNITION_GROUP_INFO[max];
		for(int i = 0; i < max; i++) {
			groupInfo[i] = new NET_FACERECONGNITION_GROUP_INFO();
		}
		
		NET_OUT_FIND_GROUP_INFO stOut = new NET_OUT_FIND_GROUP_INFO();   
		stOut.pGroupInfos = new Memory(groupInfo[0].size() * 20);     // Pointer初始化
		stOut.pGroupInfos.clear(groupInfo[0].size() * 20);
		stOut.nMaxGroupNum = max;
		
		ToolKits.SetStructArrToPointerData(groupInfo, stOut.pGroupInfos);  // 将数组内存拷贝给Pointer

		if(netsdkApi.CLIENT_FindGroupInfo(m_hLoginHandle, stIn, stOut, 4000)) {
			ToolKits.GetPointerDataToStructArr(stOut.pGroupInfos, groupInfo);     // 将Pointer的值输出到 数组 NET_FACERECONGNITION_GROUP_INFO
			for(int i = 0; i < stOut.nRetGroupNum; i++) {
				groupDefaultModel.setValueAt(String.valueOf(i + 1), i, 0);
				
				// 人脸库名称
				try {
					groupDefaultModel.setValueAt(new String(groupInfo[i].szGroupName, encode).trim(), i, 1);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
				// 人脸库ID
				groupDefaultModel.setValueAt(new String(groupInfo[i].szGroupId).trim(), i, 2);
				
				// 相似度
				String nSimilarityCount = "";
				for(int j = 0; j < groupInfo[i].nRetSimilarityCount; j++) {
					nSimilarityCount = nSimilarityCount + "  " + String.valueOf(groupInfo[i].nSimilarity[j]);
				}			
				groupDefaultModel.setValueAt(nSimilarityCount, i, 3);
				
				
				String nChannelCount = "";
				for(int j = 0; j < groupInfo[i].nRetChnCount; j++) {
					nChannelCount = nChannelCount + "  " + String.valueOf(groupInfo[i].nChannel[j]);
				}
				groupDefaultModel.setValueAt(nChannelCount, i, 4);
			}
		} else {
			System.err.println("查询人员信息失败" + ToolKits.getErrorCode());
			return false;
		}
		
		return true;
	}
	
	/**
	 * 清除人脸库列表
	 */
	public void clearGroupList() {
		for(int i = 0; i < 20; i++) {
			for(int j = 0; j < 5; j++) {
				groupDefaultModel.setValueAt("", i, j);
			}
		}
	}
	
	/**
	 * 清除人员信息列表
	 */
	public void clearServerList() {
		for(int i = 0; i < 2000; i++) {
			for(int j = 0; j < 11; j++) {
				serverDefaultModel.setValueAt("", i, j);
			}
		}
	}
	
	/**
	 * 添加人员信息(即注册人脸)
	 * @param groupId 组ID(人脸库ID) 
	 * @param nPicBufLen 图片大小
	 * @param width 图片宽
	 * @param height 图片高 
	 * @param memory  保存图片的缓存
	 * @param personName 人员名称
	 * @param bySex 性别
	 * @param birthday 生日(年月日数组)
	 * @param province 省份
	 * @param id 证件编号
	 * @param byIdType 证件类型
	 */
	public boolean addFaceRecognitionDB(String groupId, int nPicBufLen, int width, int height, Memory memory, String personName, byte bySex, 
										String[] birthday, String province, String city, String id, byte byIdType) {
		// 入参
		NET_IN_OPERATE_FACERECONGNITIONDB stuIn  = new NET_IN_OPERATE_FACERECONGNITIONDB();
		stuIn.emOperateType = EM_OPERATE_FACERECONGNITIONDB_TYPE.NET_FACERECONGNITIONDB_ADD;
		
		///////// 使用人员扩展信息 //////////
		stuIn.bUsePersonInfoEx = 1;   
		
		// 组ID设置
		System.arraycopy(groupId.getBytes(), 0, stuIn.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);
		
		// 生日设置
		stuIn.stPersonInfoEx.wYear = (short)Integer.parseInt(birthday[0]);
		stuIn.stPersonInfoEx.byMonth = (byte)Integer.parseInt(birthday[1]);
		stuIn.stPersonInfoEx.byDay = (byte)Integer.parseInt(birthday[2]);
		
		// 性别,1-男,2-女,作为查询条件时,此参数填0,则表示此参数无效	
		stuIn.stPersonInfoEx.bySex = bySex;	
		
		// 人员名字	
		try {
			System.arraycopy(personName.getBytes(encode), 0, stuIn.stPersonInfoEx.szPersonName, 0, personName.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		
		// 证件类型
		stuIn.stPersonInfoEx.byIDType = byIdType;  
		
		// 人员唯一标识(身份证号码,工号,或其他编号)
		System.arraycopy(id.getBytes(), 0, stuIn.stPersonInfoEx.szID, 0, id.getBytes().length); 					  
		
		// 国际,符合ISO3166规范
		System.arraycopy("CN".getBytes(), 0, stuIn.stPersonInfoEx.szCountry, 0, "CN".getBytes().length);	
		
		// 省份
		try {
			System.arraycopy(province.getBytes(encode), 0, stuIn.stPersonInfoEx.szProvince, 0, province.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  
		
		 // 城市
		try {
			System.arraycopy(city.getBytes(encode), 0, stuIn.stPersonInfoEx.szCity, 0, city.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  

		// 图片张数、大小、宽、高、缓存设置
		if(memory != null) {
			stuIn.stPersonInfoEx.wFacePicNum = 1; // 图片张数
			stuIn.stPersonInfoEx.szFacePicInfo[0].dwFileLenth = nPicBufLen;
			stuIn.stPersonInfoEx.szFacePicInfo[0].dwOffSet = 0;
			stuIn.stPersonInfoEx.szFacePicInfo[0].wWidth = (short)width;
			stuIn.stPersonInfoEx.szFacePicInfo[0].wHeight = (short)height;
			
			stuIn.nBufferLen = nPicBufLen;
			stuIn.pBuffer = memory;
		}
		
		// 出参
		NET_OUT_OPERATE_FACERECONGNITIONDB stuOut = new NET_OUT_OPERATE_FACERECONGNITIONDB() ;	
		
		stuIn.write();
	    if(netsdkApi.CLIENT_OperateFaceRecognitionDB(m_hLoginHandle, stuIn, stuOut, 3000)) {
			JOptionPane.showMessageDialog(this, "添加人员信息成功!");
		} else {
	    	JOptionPane.showMessageDialog(this, "添加人员信息失败!");
	    	return false;
	    }
	    stuIn.read();

		return true;
	}
	
	/**
	 * 修改人员信息(即注册人脸)
	 * @param groupId 组ID(人脸库ID) 
	 * @param szUID 人员唯一标识符
	 * @param nPicBufLen 图片大小
	 * @param width 图片宽
	 * @param height 图片高 
	 * @param memory  保存图片的缓存
	 * @param personName 人员名称
	 * @param bySex 性别
	 * @param birthday 生日(年月日数组)
	 * @param province 省份
	 * @param id 证件编号
	 * @param byIdType 证件类型
	 */
	public boolean modifyFaceRecognitionDB(String groupId, String szUID, int nPicBufLen, int width, int height, Memory memory, String personName, byte bySex, 
										String[] birthday, String province, String city, String id, byte byIdType) {
		// 入参
		NET_IN_OPERATE_FACERECONGNITIONDB stuIn  = new NET_IN_OPERATE_FACERECONGNITIONDB();
		stuIn.emOperateType = EM_OPERATE_FACERECONGNITIONDB_TYPE.NET_FACERECONGNITIONDB_MODIFY;
		
		///////// 使用人员扩展信息  ////////
		stuIn.bUsePersonInfoEx = 1;	
		
		// 组ID设置
		System.arraycopy(groupId.getBytes(), 0, stuIn.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);
		
		// UID设置
		System.arraycopy(szUID.getBytes(), 0, stuIn.stPersonInfoEx.szUID, 0, szUID.getBytes().length); 
		
		// 生日设置
		stuIn.stPersonInfoEx.wYear = (short)Integer.parseInt(birthday[0]);
		stuIn.stPersonInfoEx.byMonth = (byte)Integer.parseInt(birthday[1]);
		stuIn.stPersonInfoEx.byDay = (byte)Integer.parseInt(birthday[2]);
		
		// 性别,1-男,2-女,作为查询条件时,此参数填0,则表示此参数无效	
		stuIn.stPersonInfoEx.bySex = bySex;	
		
		// 人员名字	
		try {
			System.arraycopy(personName.getBytes(encode), 0, stuIn.stPersonInfoEx.szPersonName, 0, personName.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		
		// 证件类型
		stuIn.stPersonInfoEx.byIDType = byIdType;  
		
		// 人员唯一标识(身份证号码,工号,或其他编号)
		System.arraycopy(id.getBytes(), 0, stuIn.stPersonInfoEx.szID, 0, id.getBytes().length); 					  
		
		// 国际,符合ISO3166规范
		System.arraycopy("CN".getBytes(), 0, stuIn.stPersonInfoEx.szCountry, 0, "CN".getBytes().length); 		
		
		 // 省份
		try {
			System.arraycopy(province.getBytes(encode), 0, stuIn.stPersonInfoEx.szProvince, 0, province.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  
		
		// 城市
		try {
			System.arraycopy(city.getBytes(encode), 0, stuIn.stPersonInfoEx.szCity, 0, city.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 

		// 图片张数、大小、宽、高、缓存设置
		if(memory != null) {
			stuIn.stPersonInfoEx.wFacePicNum = 1; // 图片张数
			stuIn.stPersonInfoEx.szFacePicInfo[0].dwFileLenth = nPicBufLen;
			stuIn.stPersonInfoEx.szFacePicInfo[0].dwOffSet = 0;
			stuIn.stPersonInfoEx.szFacePicInfo[0].wWidth = (short)width;
			stuIn.stPersonInfoEx.szFacePicInfo[0].wHeight = (short)height;
			
			stuIn.nBufferLen = nPicBufLen;
			stuIn.pBuffer = memory;
		}

		// 出参
		NET_OUT_OPERATE_FACERECONGNITIONDB stuOut = new NET_OUT_OPERATE_FACERECONGNITIONDB() ;	
		
		stuIn.write();
	    if(netsdkApi.CLIENT_OperateFaceRecognitionDB(m_hLoginHandle, stuIn, stuOut, 3000)) {
			JOptionPane.showMessageDialog(this, "修改人员信息成功!");
		} else {
	    	JOptionPane.showMessageDialog(this, "修改人员信息失败!");
	    	return false;
	    }
	    stuIn.read();

		return true;
	}
	
	/**
	 * 下载图片
	 * @param szFileName 需要下载的文件名
	 */
	public boolean downloadRemoteFile(String szFileName) {
		// 入参
		NET_IN_DOWNLOAD_REMOTE_FILE stIn = new NET_IN_DOWNLOAD_REMOTE_FILE();
		stIn.pszFileName = new NativeString(szFileName).getPointer();
		stIn.pszFileDst = new NativeString("./face.jpg").getPointer();

		// 出参
		NET_OUT_DOWNLOAD_REMOTE_FILE stOut = new NET_OUT_DOWNLOAD_REMOTE_FILE();
		
		if(netsdkApi.CLIENT_DownloadRemoteFile(m_hLoginHandle, stIn, stOut, 5000)) {
			System.out.println("下载图片成功!");
		} else {
			System.err.println("下载图片失败!" +  + netsdkApi.CLIENT_GetLastError());
			return false;
		}
		return true;
	}
	
	/**
	 * 删除人员信息(即删除人脸)
	 * @param groupId 人脸库ID
	 * @param sUID  人员唯一标识符
	 */
	public boolean delFaceRecognitionDB(String groupId, String sUID) {    
		// 入参
		NET_IN_OPERATE_FACERECONGNITIONDB stuIn  = new NET_IN_OPERATE_FACERECONGNITIONDB();
		stuIn.emOperateType = EM_OPERATE_FACERECONGNITIONDB_TYPE.NET_FACERECONGNITIONDB_DELETE;
	
		//////// 使用人员扩展信息  //////////
		stuIn.bUsePersonInfoEx = 1;	
		
		// GroupID 赋值
		System.arraycopy(groupId.getBytes(), 0, stuIn.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);

		// UID赋值
		System.arraycopy(sUID.getBytes(), 0, stuIn.stPersonInfoEx.szUID, 0, sUID.getBytes().length);

		// 出参
		NET_OUT_OPERATE_FACERECONGNITIONDB stuOut = new NET_OUT_OPERATE_FACERECONGNITIONDB() ;	

	    if(netsdkApi.CLIENT_OperateFaceRecognitionDB(m_hLoginHandle, stuIn, stuOut, 3000)) {
			JOptionPane.showMessageDialog(this, "删除人员信息成功!");
		} else {
	    	JOptionPane.showMessageDialog(this, "删除人员信息失败!");
	    	System.err.println(netsdkApi.CLIENT_GetLastError());
	    	return false;
	    }
	    
		return true;
	}
	
	/**
	 * 人员信息查找
	 * @param groupId   当前查找的人脸库的id库的id
	 */
	public void findFaceRecognitionDB(String groupId) {
		clearServerList();
		
		// IVVS设备，查询条件只有  stInStartFind.stPerson 里的参数有效
		NET_IN_STARTFIND_FACERECONGNITION stInStartFind = new NET_IN_STARTFIND_FACERECONGNITION();
		// 人员信息查询条件是否有效, 并使用扩展结构体
		stInStartFind.bPersonExEnable = 1;   
		
		System.arraycopy(groupId.getBytes(), 0, stInStartFind.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);   // 人员组ID
		
	    //设置过滤条件
		stInStartFind.stFilterInfo.nGroupIdNum = 1;   // 人员组数
		System.arraycopy(groupId.getBytes(), 0, stInStartFind.stFilterInfo.szGroupIdArr[0].szGroupId, 0, groupId.getBytes().length);  // 人员组ID
		stInStartFind.stFilterInfo.nRangeNum = 1;
		stInStartFind.stFilterInfo.szRange[0] = EM_FACE_DB_TYPE.NET_FACE_DB_TYPE_BLACKLIST;
		stInStartFind.stFilterInfo.emFaceType = EM_FACERECOGNITION_FACE_TYPE.EM_FACERECOGNITION_FACE_TYPE_ALL;

		stInStartFind.nChannelID = -1;
	    
	    //让设备根据查询条件整理结果集
	    NET_OUT_STARTFIND_FACERECONGNITION stOutParam = new NET_OUT_STARTFIND_FACERECONGNITION();
	    stInStartFind.write();
	    stOutParam.write();
	    if(netsdkApi.CLIENT_StartFindFaceRecognition(m_hLoginHandle, stInStartFind,  stOutParam, 2000))
	    {
	    	System.out.printf("Handle Token = %d\n" , stOutParam.nToken);
	        
	    	int doNextCount = 0;	// 查询次数
	    	int count = 10;    		// 每次查询的个数
	        //分页查找数据
	        NET_IN_DOFIND_FACERECONGNITION  stFindIn = new NET_IN_DOFIND_FACERECONGNITION();
	        stFindIn.lFindHandle = stOutParam.lFindHandle;
	        stFindIn.nCount      = count;  // 当前想查询的记录条数
	      	stFindIn.nBeginNum   = 0;  //每次递增
	        
	        NET_OUT_DOFIND_FACERECONGNITION stFindOut = new NET_OUT_DOFIND_FACERECONGNITION();;
	        stFindOut.bUseCandidatesEx = 1;   // 使用候选对象扩展结构体
	        
	        do 
	        {
	        	stFindIn.write();
	        	stFindOut.write();
	            if(netsdkApi.CLIENT_DoFindFaceRecognition(stFindIn, stFindOut, 1000))
	            {
	                System.out.printf("Record Number [%d]\n" , stFindOut.nCadidateExNum);
	      
	        		for(int i = 0; i < stFindOut.nCadidateExNum; i++)
	                {                         
	        			int index = i + doNextCount * count;    // 查询的总个数 - 1, 从0开始
	                	
	                    serverDefaultModel.setValueAt(String.valueOf(index + 1), index, 0);  

	        			try {
	        				serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szGroupName, encode).trim(), index, 1);
	        			} catch (UnsupportedEncodingException e1) {
	        				e1.printStackTrace();
	        			}
	        	
	                	serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szUID).trim(), index, 2);
	                    
	                	// 人员名称
	                    try {
	                    	serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szPersonName, encode).trim(), index, 3);
	        			} catch (UnsupportedEncodingException e) {
	        				e.printStackTrace();
	        			}

	                    // 性别
	                    serverDefaultModel.setValueAt(new String(faceSexStr[stFindOut.stuCandidatesEx[i].stPersonInfo.bySex]).trim(), index, 4);	  
	                	
	                    // 证件类型
	                    serverDefaultModel.setValueAt(new String(idStr[stFindOut.stuCandidatesEx[i].stPersonInfo.byIDType]).trim(), index, 5);
	                   
	                    // 证件编码
	                    serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szID).trim(), index, 6);
	                   
	                    // 生日
	                    serverDefaultModel.setValueAt(String.valueOf(stFindOut.stuCandidatesEx[i].stPersonInfo.wYear) + "-" + 
	        				                   String.valueOf(0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byMonth) + "-" + 
	        				                   String.valueOf(0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byDay), index, 7);
	                    
	                    try {
	                    	serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szProvince, encode).trim(), index, 8);  // 省份
	                    	serverDefaultModel.setValueAt(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szCity, encode).trim(), index, 9);		 // 城市
	        			} catch (UnsupportedEncodingException e) {
	        				e.printStackTrace();
	        			}
	                    
	                    // 图片地址
	                    for (int j = 0; j < stFindOut.stuCandidatesEx[i].stPersonInfo.wFacePicNum; j ++)
	                    {
	                    	serverDefaultModel.setValueAt(GetPointerDataToString(stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].pszFilePath), index, 10);
	                    }         
	                }                          
	            }
	            else
	            {
	            	System.out.printf("CLIENT_DoFindFaceRecognition Failed!LastError = %x\n" , netsdkApi.CLIENT_GetLastError());
	            	break;
	            }

	            if( stFindOut.nCadidateNum < stFindIn.nCount)
	            {
	            	System.out.printf("No More Record, Find End!\n");
	                break;
	            } else {
		            stFindIn.nBeginNum += count;
		            doNextCount++;
	            }
	        }while (true);
	          
	        netsdkApi.CLIENT_StopFindFaceRecognition(stOutParam.lFindHandle);	        
	    }
	    else
	    {
	        System.out.println("CLIENT_StartFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
	    }
	}
	
	
	public String GetPointerDataToString(Pointer pointer) {	
		String str = "";
		if(pointer == null) {
			return str;
		}		
		
		int length = 0;
		while (length < 2048) {
			if (pointer.getByte(length) == '\0') {
				break;
			}
			++length;
		}
		
		byte[] buffer = pointer.getByteArray(0, length);
		try {
			str = new String(buffer, encode).trim();
		} catch (UnsupportedEncodingException e) {
			return str;
		}

		return str;
	}
	
	
		
	/**
	 * 以图搜图
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 * @param chn 通道号
	 * @param similary 相似度
	 * @param memory 图片缓存
	 * @param nPicLen 图片大小
	 */
	public void searchByPicture(String startTime, String endTime, int chn, String similary, Memory memory, int nPicLen) {	
		String[] startTimeStr = startTime.split("-");
		String[] endTimeStr = endTime.split("-");
		
		// IVSS设备，查询条件只有  stInStartFind.stPerson 里的参数有效
		NET_IN_STARTFIND_FACERECONGNITION stInStartFind = new NET_IN_STARTFIND_FACERECONGNITION();

		// 通道号
		stInStartFind.nChannelID = chn;
		stInStartFind.bPersonExEnable = 1;  	// 人员信息查询条件是否有效, 并使用扩展结构体

		// 图片信息
		if(memory != null && nPicLen > 0) {
			stInStartFind.pBuffer = memory;
			stInStartFind.nBufferLen = nPicLen;
			stInStartFind.stPersonInfoEx.wFacePicNum = 1;
			stInStartFind.stPersonInfoEx.szFacePicInfo[0].dwOffSet = 0;
			stInStartFind.stPersonInfoEx.szFacePicInfo[0].dwFileLenth = nPicLen;
		}

		// 相似度
		if(!similary.equals("")) {
			stInStartFind.stMatchOptions.nSimilarity = Integer.parseInt(similary);
		}

		stInStartFind.stFilterInfo.nGroupIdNum = 0;
		stInStartFind.stFilterInfo.nRangeNum = 1;
		stInStartFind.stFilterInfo.szRange[0] = EM_FACE_DB_TYPE.NET_FACE_DB_TYPE_HISTORY;
		stInStartFind.stFilterInfo.stStartTime.dwYear = Integer.parseInt(startTimeStr[0]);
		stInStartFind.stFilterInfo.stStartTime.dwMonth = Integer.parseInt(startTimeStr[1]);
		stInStartFind.stFilterInfo.stStartTime.dwDay = Integer.parseInt(startTimeStr[2]);
		stInStartFind.stFilterInfo.stEndTime.dwYear = Integer.parseInt(endTimeStr[0]);
		stInStartFind.stFilterInfo.stEndTime.dwMonth = Integer.parseInt(endTimeStr[1]);
		stInStartFind.stFilterInfo.stEndTime.dwDay = Integer.parseInt(endTimeStr[2]);
		stInStartFind.stFilterInfo.emFaceType = EM_FACERECOGNITION_FACE_TYPE.EM_FACERECOGNITION_FACE_TYPE_ALL;

	    //让设备根据查询条件整理结果集
	    NET_OUT_STARTFIND_FACERECONGNITION stOutParam = new NET_OUT_STARTFIND_FACERECONGNITION();
	    stInStartFind.write();
	    stOutParam.write();
	    if(netsdkApi.CLIENT_StartFindFaceRecognition(m_hLoginHandle, stInStartFind,  stOutParam, 2000))
	    {        
	    	findHandle = stOutParam.lFindHandle;

	    	if(stOutParam.nTotalCount == -1) {   // -1表示总条数未生成,要推迟获取, 使用CLIENT_AttachFaceFindState接口状态
	        	nToken = stOutParam.nToken;
	    		// 入参
	    		NET_IN_FACE_FIND_STATE pstInParam = new NET_IN_FACE_FIND_STATE();
	    		pstInParam.nTokenNum = 1;   
	    		pstInParam.nTokens = new IntByReference(nToken);  // 查询令牌
	    		pstInParam.cbFaceFindState = faceFindStateCb;
	    		
	    		// 出参
	    		NET_OUT_FACE_FIND_STATE pstOutParam = new NET_OUT_FACE_FIND_STATE();
	    		
	    		pstInParam.write();
	    		attachFaceHandle = netsdkApi.CLIENT_AttachFaceFindState(m_hLoginHandle, pstInParam, pstOutParam, 4000);
	    		pstInParam.read();
	    		if(attachFaceHandle.longValue() != 0) {
	    			System.out.println("AttachFaceFindState Succeed!");
	    		}
	    			 
	    	} else {
	    		progressBar.setValue(100);
	    		doFindSearchByPicture(findHandle, 10);
	    	}
	    }
	    else
	    {
	        System.out.println("CLIENT_StartFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
	        searchByPicBtu.setEnabled(true);
	    }
	}
	
	/**
	 * 以图搜图
	 * @param longHandle  CLIENT_StartFindFaceRecognition 接口返回的查询句柄
	 * @param count  查询的个数
	 */
	public void doFindSearchByPicture(LLong longHandle, int count) {
		int doNextCount = 0;
    	//分页查找数据
        NET_IN_DOFIND_FACERECONGNITION  stFindIn = new NET_IN_DOFIND_FACERECONGNITION();
        stFindIn.lFindHandle = longHandle;
        stFindIn.nCount      = 10;  // 当前想查询的记录条数
        stFindIn.nBeginNum   = 0 ;     //每次递增
        
        NET_OUT_DOFIND_FACERECONGNITION stFindOut = new NET_OUT_DOFIND_FACERECONGNITION();;
        stFindOut.bUseCandidatesEx = 1;				// 是否使用候选对象扩展结构体
        
        for(int i = 0; i < count; i++) {
        	stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[0].nFilePathLen = 256;
        	stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[0].pszFilePath = new Memory(256);
        }
        
        do 
        {
        	stFindIn.write();
        	stFindOut.write();
            if(netsdkApi.CLIENT_DoFindFaceRecognition(stFindIn, stFindOut, 1000))
            {
                System.out.printf("Record Number [%d]\n" , stFindOut.nCadidateExNum);  

                if(stFindOut.nCadidateExNum == 0) {
                	JOptionPane.showMessageDialog(null, "没查到相关信息！");
                	break;
                }
                
                if(stFindOut.nCadidateExNum > 2) {
                	showPanel.setPreferredSize(new Dimension(stFindOut.nCadidateExNum * 448, 380));
                	showPanel.updateUI();
                	
                	for(int i = 0; i < stFindOut.nCadidateExNum - 2; i++) {
                		showPanel.add(addSearchByPicShowPanel());        
                	}
                }		
                
                for(int i = 0; i < stFindOut.nCadidateExNum; i++) {
                	int index = i + doNextCount * count;    // 查询的总个数 - 1, 从0开始
                	 
                	PaintJPanel showPanel = searchByPicPanelList.get(i);    // 获取图片显示面板
                	ArrayList<JTextField> textFieldsList = searchByPicTextFieldMap.get(showPanel);  // 获取文本列表
                	
                	// 时间
                	textFieldsList.get(0).setText(stFindOut.stuCandidatesEx[i].stTime.toStringTime());  
                	
                	// 性别
                 	textFieldsList.get(1).setText(faceSexStr[0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.bySex]);  
                 	
                 	// 年龄
                	textFieldsList.get(2).setText(String.valueOf(0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byAge));  


                 	textFieldsList.get(3).setText(getRC(0));
                 	
                 	// 眼睛
                 	textFieldsList.get(4).setText(getEyeState(stFindOut.stuCandidatesEx[i].stPersonInfo.emEye));
                 	
                 	// 嘴巴
                 	textFieldsList.get(5).setText(getMouthState(stFindOut.stuCandidatesEx[i].stPersonInfo.emMouth));
                 	
                 	// 口罩
                 	textFieldsList.get(6).setText(getMaskState(stFindOut.stuCandidatesEx[i].stPersonInfo.emMask));
                 	
                 	// 胡子
                 	textFieldsList.get(7).setText(getBeardState(stFindOut.stuCandidatesEx[i].stPersonInfo.emBeard));
                 	
                 	// 眼镜
                 	textFieldsList.get(8).setText(getGlasses(stFindOut.stuCandidatesEx[i].stPersonInfo.byGlasses));
                 	
                	// 相似度
                	textFieldsList.get(9).setText(String.valueOf(stFindOut.stuCandidatesEx[i].bySimilarity));  
                	
                	// 姓名
                	try {
						textFieldsList.get(10).setText(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szPersonName, encode).trim());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} 
                	
                	// 生日
                	textFieldsList.get(11).setText((stFindOut.stuCandidatesEx[i].stPersonInfo.wYear) + "-" + 
						                		   (0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byMonth) + "-" +
						                		   (0xff & stFindOut.stuCandidatesEx[i].stPersonInfo.byDay));
                	
                	// 证件类型
                	textFieldsList.get(12).setText(idStr[stFindOut.stuCandidatesEx[i].stPersonInfo.byIDType]);
                	
                	// 证件编号
                	textFieldsList.get(13).setText(new String(stFindOut.stuCandidatesEx[i].stPersonInfo.szID).trim());
                	
                	if(stFindOut.stuCandidatesEx[i].stPersonInfo.wFacePicNum > 0) {
	                	// 图片路径
	                	String picPath = stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[0].pszFilePath.getString(0);

	                	// 下载图片
	                	downloadRemoteFile(picPath);
	                	                	
	                	File file = new File("./face.jpg");							
	        			BufferedImage bufferedImage = null;
	        			
	        			if(file.isFile() && !picPath.isEmpty()) {
		        			try {
		        				bufferedImage = ImageIO.read(file);
		        				if(bufferedImage != null) {			       
		        	   				showPanel.setOpaque(false);
			        				showPanel.setImage(bufferedImage);
			        				showPanel.repaint();
		        				}
		        			} catch (IOException e2) {
		        				e2.printStackTrace();
		        			}
		        			file.delete();
	        			}
                	}    
                }
            }
            else
            {
            	System.out.println("CLIENT_DoFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
            	break;
            }

            if( stFindOut.nCadidateNum < stFindIn.nCount)
            {
            	System.out.printf("No More Record, Find End!\n");
                break;
            } else {
	            stFindIn.nBeginNum += count;
	            doNextCount++;
            }
        }while (true);
          
        netsdkApi.CLIENT_StopFindFaceRecognition(longHandle);
        searchByPicBtu.setEnabled(true);
	}
	
	/**
	 * 订阅人脸回调，在以图搜图时使用
	 */
	fFaceFindStateCb faceFindStateCb = new fFaceFindStateCb();
	public class fFaceFindStateCb implements fFaceFindState/*, StdCallCallback*/ {
		@Override
		public void invoke(LLong lLoginID, LLong lAttachHandle,
				Pointer pstStates, int nStateNum, Pointer dwUser) {
			if(nStateNum < 1) {
				return;
			}
			NET_CB_FACE_FIND_STATE[] msg = new NET_CB_FACE_FIND_STATE[nStateNum];
			for(int i = 0; i < nStateNum; i++) {
				msg[i] = new NET_CB_FACE_FIND_STATE();
			}
			ToolKits.GetPointerDataToStructArr(pstStates, msg);
			
			for(int i = 0; i < nStateNum; i++) {
				if(nToken == msg[i].nToken) {
					
					// 刷新UI时，将以图搜图的信息抛出去
		            EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		            if (eventQueue != null) {
		        	   eventQueue.postEvent(new SearchByPictureEvent(target, msg[i].nCurrentCount, msg[i].nProgress));
		            }
				}
			}
		}	
	}
	
	/**
	 * 以图搜图刷新UI
	 */
	class SearchByPictureEvent extends AWTEvent {
		private static final long serialVersionUID = 1L;
		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
		
		private int m_Count;
		private int nProgress;
		
		public SearchByPictureEvent(Object target, int m_Count, int nProgress) {
			super(target,EVENT_ID);

			this.m_Count = m_Count;
			this.nProgress = nProgress;
		}
		
		public int getCount() {
			return m_Count;
		}
		
		public int getProgress() {
			return nProgress;
		}
	}

	
	/**
	 * 获取性别索引
	 * @param szSex 下拉框显示的男女
	 * @return type
	 */
	public int getSexInt(String szSex) {
		int type = 0;
		if(szSex.equals("男")) {
			type = 1;
		} else if(szSex.equals("女")) {
			type = 2;
		} 
		
		return type;
	}
	
	/**
	 * 获取证件类型索引
	 * @param idType 下拉框显示的证件类型
	 * @return type
	 */
	public int getIDInt(String idType) {
		int type = 0;
		if(idType.equals("身份证")) {
			type = 1;
		} else if(idType.equals("护照")){
			type = 2;
		} 
		
		return type;
	}
	
	/**
	 * 获取省份索引
	 * @param province 下拉框显示的省份
	 * @return index
	 */
	public int getProvinceIndex(String province) {
		int index = 0;
		for(int i = 0; i < 34; i++) {
			if(province.equals(privinceStr[i])) {
				index = i;
				break;
			}
		}
		return index;
	}
	

	/**
	 * 人脸识别事件订阅
	 * @param channel 通道号
	 * @return true:成功    false:失败
	 */
	public boolean attach(int channel) {
		int bNeedPicture = 1; // 是否需要图片
	
		m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyseCb, null, null);
        if(m_hAttachHandle.longValue() == 0) {
        	System.err.println("CLIENT_RealLoadPictureEx Failed, Error:" + ToolKits.getErrorCode());
        	return false;
        } 

		return true;
	}
	
	/**
	 * 停止订阅
	 * @param m_hAttachHandle 智能订阅句柄
	 */
	public void detach(LLong m_hAttachHandle) {
		if(m_hAttachHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
		}
	}
	
	/**
	 * 智能订阅事件回调
	 */
	private AnalyzerDataCB analyseCb = new AnalyzerDataCB();
    public class AnalyzerDataCB implements fAnalyzerDataCallBack/*, StdCallCallback*/ {
        private int bGlobalScenePic;					//全景图是否存在, 类型为BOOL, 取值为0或者1
        private NET_PIC_INFO stuGlobalScenePicInfo;     //全景图片信息
        

        private NET_PIC_INFO stPicInfo;	  			    // 人脸图
        private NET_FACE_DATA stuFaceData;			    // 人脸数据
        
        private int nCandidateNumEx;				    // 当前人脸匹配到的候选对象数量
        private CANDIDATE_INFOEX[] stuCandidatesEx;     // 当前人脸匹配到的候选对象信息扩展 
        
		@Override
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
				Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) { 
	        
		   // 获取相关事件信息
		   getObjectInfo(dwAlarmType, pAlarmInfo);
		   
		   if(dwAlarmType == NetSDKLib.EVENT_IVS_FACERECOGNITION) {   // 人脸识别
			   // 保存图片
			   savePicture(pBuffer, dwBufSize, bGlobalScenePic, stuGlobalScenePicInfo, stPicInfo, nCandidateNumEx, stuCandidatesEx);
				
			   // 刷新UI时，将人脸识别事件抛出处理
	           EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
	           if (eventQueue != null) {
	        	   eventQueue.postEvent(new FaceRecognitionEvent(target, 
										        			     globalBufferedImage, 
										        			     personBufferedImage,
										        			     stuFaceData,
										        			     arrayListBuffer, 
										        			     nCandidateNumEx,
										        			     stuCandidatesEx));
	           }
		   } else if(dwAlarmType == NetSDKLib.EVENT_IVS_FACEDETECT) {  // 人脸检测
			   // 保存图片
			   savePicture(pBuffer, dwBufSize, stPicInfo);
		   }
			
			return 0;
		}
		
		/**
		 * 获取相关事件信息
		 * @param dwAlarmType 事件类型
		 * @param pAlarmInfo 事件信息指针
		 */
		public void getObjectInfo(int dwAlarmType, Pointer pAlarmInfo) {
			if(pAlarmInfo == null) {
				return;
			}
			
			switch(dwAlarmType)
            {
				case NetSDKLib.EVENT_IVS_FACERECOGNITION:  ///< 人脸识别事件
				{
                	DEV_EVENT_FACERECOGNITION_INFO msg = new DEV_EVENT_FACERECOGNITION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);  
               
                    bGlobalScenePic = msg.bGlobalScenePic;
                    stuGlobalScenePicInfo = msg.stuGlobalScenePicInfo;
                    stuFaceData = msg.stuFaceData;
                    stPicInfo = msg.stuObject.stPicInfo;
                    nCandidateNumEx = msg.nRetCandidatesExNum;
                    stuCandidatesEx = msg.stuCandidatesEx;     
                             
					break;
				} 
				case NetSDKLib.EVENT_IVS_FACEDETECT:   ///< 人脸检测
				{
					DEV_EVENT_FACEDETECT_INFO msg = new DEV_EVENT_FACEDETECT_INFO(); 
				    ToolKits.GetPointerData(pAlarmInfo, msg); 
				      
				    stPicInfo = msg.stuObject.stPicInfo;  // 检测到的人脸
				    
				    System.out.println("age:" + msg.nAge);
				    System.out.println("sex:" + faceSexStr[msg.emSex]);
				    System.out.println("dwOffSet:" + stPicInfo.dwOffSet);
				    System.out.println("dwFileLenth:" + stPicInfo.dwFileLenth);
				    
				    break;
				}
				default:
					break;
            }
		}
		
		/**
		 * 保存人脸识别事件图片
		 * @param pBuffer 抓拍图片信息
		 * @param dwBufSize 抓拍图片大小
		 */
		public void savePicture(Pointer pBuffer, int dwBufSize, 
								int bGlobalScenePic, NET_PIC_INFO stuGlobalScenePicInfo, 
								NET_PIC_INFO stPicInfo,
								int nCandidateNum, CANDIDATE_INFOEX[] stuCandidatesEx) {
			File path = new File("./FaceRegonition/");
            if (!path.exists()) {
                path.mkdir();
            }

            if (pBuffer == null || dwBufSize <= 0) {
            	return;
            }
            
			/////////////// 保存全景图 ///////////////////
            if(bGlobalScenePic == 1 && stuGlobalScenePicInfo != null) {
    			String strGlobalPicPathName = path + "\\" + System.currentTimeMillis() + "Global.jpg"; 
    	    	byte[] bufferGlobal = pBuffer.getByteArray(stuGlobalScenePicInfo.dwOffSet, stuGlobalScenePicInfo.dwFileLenth);
    			ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(bufferGlobal);
    			
    			try {
    				globalBufferedImage = ImageIO.read(byteArrInputGlobal);
    				if(globalBufferedImage == null) {
    					return;
    				}
    				ImageIO.write(globalBufferedImage, "jpg", new File(strGlobalPicPathName));
    			} catch (IOException e2) {
    				e2.printStackTrace();
    			}
            }

            /////////////// 保存人脸图 /////////////////////////
            if(stPicInfo != null) {
            	String strPersonPicPathName = path + "\\" + System.currentTimeMillis() + "Person.jpg"; 
    	    	byte[] bufferPerson = pBuffer.getByteArray(stPicInfo.dwOffSet, stPicInfo.dwFileLenth);
    			ByteArrayInputStream byteArrInputPerson = new ByteArrayInputStream(bufferPerson);
    			
    			try {
    				personBufferedImage = ImageIO.read(byteArrInputPerson);
    				if(personBufferedImage == null) {
    					return;
    				}
    				ImageIO.write(personBufferedImage, "jpg", new File(strPersonPicPathName));
    			} catch (IOException e2) {
    				e2.printStackTrace();
    			}
            }
            
            ///////////// 保存对比图 //////////////////////
            arrayListBuffer.clear();
            if(nCandidateNum > 0 && stuCandidatesEx != null) {
            	for(int i = 0; i < nCandidateNum; i++) {
                	String strCandidatePicPathName = path + "\\" + System.currentTimeMillis() + "Candidate.jpg"; 
                	// 多张对比图
                	for(int j = 0; j < stuCandidatesEx[i].stPersonInfo.wFacePicNum; j++) {
                		byte[] bufferCandidate = pBuffer.getByteArray(stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].dwOffSet, stuCandidatesEx[i].stPersonInfo.szFacePicInfo[j].dwFileLenth);
            			ByteArrayInputStream byteArrInputCandidate = new ByteArrayInputStream(bufferCandidate);
            			
            			try {
            				candidateBufferedImage = ImageIO.read(byteArrInputCandidate);
            				if(candidateBufferedImage == null) {
            					return;
            				}
            				ImageIO.write(candidateBufferedImage, "jpg", new File(strCandidatePicPathName));
            			} catch (IOException e2) {
            				e2.printStackTrace();
            			}
           				arrayListBuffer.add(candidateBufferedImage);
                	}
            	}
            }
		}
		
		/**
		 * 保存人脸检测事件图片
		 * @param pBuffer 抓拍图片信息
		 * @param dwBufSize 抓拍图片大小
		 */
		public void savePicture(Pointer pBuffer, int dwBufSize, NET_PIC_INFO stPicInfo) {
			File path = new File("./FaceDetected/");
	        if (!path.exists()) {
	            path.mkdir();
	        }

	        if (pBuffer == null || dwBufSize <= 0) {
	        	return;
	        }
	        
			/////////////// 保存全景图 ///////////////////
			String strGlobalPicPathName = path + "\\" + System.currentTimeMillis() + "Global.jpg"; 
	    	byte[] bufferGlobal = pBuffer.getByteArray(0, dwBufSize);
			ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(bufferGlobal);
			
			try {
				globalBufferedImage = ImageIO.read(byteArrInputGlobal);
				if(globalBufferedImage == null) {
					return;
				}
				ImageIO.write(globalBufferedImage, "jpg", new File(strGlobalPicPathName));
			} catch (IOException e2) {
				e2.printStackTrace();
			}

	        /////////////// 保存人脸图 /////////////////////////
	        if(stPicInfo != null) {
	        	String strPersonPicPathName = path + "\\" + System.currentTimeMillis() + "Person.jpg"; 
		    	byte[] bufferPerson = pBuffer.getByteArray(stPicInfo.dwOffSet, stPicInfo.dwFileLenth);
				ByteArrayInputStream byteArrInputPerson = new ByteArrayInputStream(bufferPerson);
				
				try {
					personBufferedImage = ImageIO.read(byteArrInputPerson);
					if(personBufferedImage == null) {
						return;
					}
					ImageIO.write(personBufferedImage, "jpg", new File(strPersonPicPathName));
				} catch (IOException e2) {
					e2.printStackTrace();
				}
	        }
		}
    }	

    
    /**
     * 刷新UI
     */
	class FaceRecognitionEvent extends AWTEvent {
		private static final long serialVersionUID = 1L;
		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
		
		private BufferedImage globalImage = null;
		private BufferedImage personImage = null;
		private NET_FACE_DATA stuFaceData;
		private ArrayList<BufferedImage> arrayList = null;
		private int nCandidateNum;
		private ArrayList<String[]> candidateList;

		public FaceRecognitionEvent(Object target, 
							BufferedImage globalImage, 
							BufferedImage personImage, 
							NET_FACE_DATA stuFaceData,
							ArrayList<BufferedImage> arrayList,
							int nCandidateNum,
							CANDIDATE_INFOEX[] stuCandidatesEx) {
			super(target,EVENT_ID);
			this.globalImage = globalImage;
			this.personImage = personImage;
			this.stuFaceData = stuFaceData;
			this.arrayList = arrayList;
			this.nCandidateNum = nCandidateNum;
			this.candidateList = new ArrayList<String[]>();

			this.candidateList.clear();
			for(int i = 0; i < nCandidateNum; i++) {
				try {
					candidateStr[0] = new String(stuCandidatesEx[i].stPersonInfo.szGroupID, encode).trim();
					candidateStr[1] = new String(stuCandidatesEx[i].stPersonInfo.szGroupName, encode).trim();
					candidateStr[2] = new String(stuCandidatesEx[i].stPersonInfo.szPersonName, encode).trim();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				candidateStr[3] = String.valueOf(0xff & stuCandidatesEx[i].bySimilarity);
				
				this.candidateList.add(candidateStr);
			}
		}
		
		public BufferedImage getGlobalBufferImage() {
			return globalImage;
		}
		
		public BufferedImage getPersonBufferImage() {
			return personImage;
		}
		
		public NET_FACE_DATA getFaceData() {
			return stuFaceData;
		}
		
		public ArrayList<BufferedImage> getCandidateBufferImageArrayList() {
			return arrayList;
		}
		
		public int getCandidateNum() {
			return nCandidateNum;
		}
		
		public  ArrayList<String[]> getCandidateList() {
			return candidateList;
		}
	}
	
	@Override
    protected void processEvent(AWTEvent event) {
		if(event instanceof SearchByPictureEvent) {  // 以图搜图处理
			SearchByPictureEvent ev = (SearchByPictureEvent) event;
			
			int n_Count = ev.getCount();
			int nProgress = ev.getProgress();
			
			progressBar.setValue(nProgress);
			
			if(nProgress == 100) {
				netsdkApi.CLIENT_DetachFaceFindState(attachFaceHandle);
	    		doFindSearchByPicture(findHandle, n_Count);
			}		
		} else if (event instanceof FaceRecognitionEvent) {  // 人脸识别事件处理
        	
			FaceRecognitionEvent ev = (FaceRecognitionEvent) event;
        	
        	BufferedImage globalBufferedImage =  ev.getGlobalBufferImage();
    		BufferedImage personBufferedImage = ev.getPersonBufferImage();
    		NET_FACE_DATA stuFaceData = ev.getFaceData();
    		ArrayList<BufferedImage> arrayList = ev.getCandidateBufferImageArrayList();
    		int nCandidateNum = ev.getCandidateNum();
    		ArrayList<String[]> candidateList = ev.getCandidateList();

        	/////////////////////// 全景图界面显示
        	if(globalBufferedImage == null) {
        		globalPicPanel.setOpaque(true);  // 不透明
        		globalPicPanel.repaint();
        	} else {
        		globalPicPanel.setOpaque(false);  // 透明
        		globalPicPanel.setImage(globalBufferedImage);
        		globalPicPanel.repaint(); 	
        	}
        	
        	//////////////////////// 人脸图界面显示
        	if(personBufferedImage == null) {
        		personPicPanel.setOpaque(true);  // 不透明
        		personPicPanel.repaint();
        	} else {
        		personPicPanel.setOpaque(false);  // 透明
        		personPicPanel.setImage(personBufferedImage);
        		personPicPanel.repaint(); 	
        	}

			if(stuFaceData != null) {
				sexTextField.setText(faceSexStr[stuFaceData.emSex]);  // 性别,1-男,2-女
	        	
	        	// 年龄
	        	if(stuFaceData.nAge == -1) {
	        		ageTextField.setText("");
	        	} else {
	        		ageTextField.setText(String.valueOf(stuFaceData.nAge));
	        	}
	        	eyeTextField.setText(getEyeState(stuFaceData.emEye));   	// 眼睛状态
	        	raceTextField.setText(getRC(0));     
				mouthTextField.setText(getMouthState(stuFaceData.emMouth)); // 嘴巴状态
				maskTextField.setText(getMaskState(stuFaceData.emMask));  	// 口罩
				beardTextField.setText(getBeardState(stuFaceData.emBeard)); // 胡子状态
			}	
			
			//////////////////// 对比图,   如果有多张对比图，界面只显示一个
			if(nCandidateNum <= 0) {
				return;
			}
			
        	if(arrayList.size() <= 0) {
        		candidatePanel.setOpaque(true);  // 不透明
        		candidatePanel.repaint();
        	} else {
        		candidatePanel.setOpaque(false);  // 透明
        		candidatePanel.setImage(arrayList.get(0));    
        		candidatePanel.repaint(); 	
        	}
        	
        	if(candidateList.size() > 0 && candidateList != null) {
        		groupIdTextField.setText(candidateList.get(0)[0]);
	    		groupNameTextField.setText(candidateList.get(0)[1]);	
	    		personNameTextField.setText(candidateList.get(0)[2]);
	    		similaryTextField.setText(candidateList.get(0)[3]);
	    	}
        } else {
            super.processEvent( event );   
        }
    } 
	
	/**
	 * 胡子状态
	 * @param beard
	 * @return
	 */
	public String getBeardState(int beard) {
		String str = "未识别";	
		switch (beard) {
			case 0:
				str = "未知";
				break;
			case 1:
				str = "未识别";	
					break;
			case 2:
				str = "没胡子";	
				break;
			case 3:
				str = "有胡子";	
				break;
			default:
				break;
		}	
		return str;
	}
	
	/**
	 * 嘴巴状态
	 * @param mouth
	 * @return
	 */
	public String getMouthState(int mouth) {
		String str = "未识别";	
		switch (mouth) {
			case 0:
				str = "未知";
				break;
			case 1:
				str = "未识别";	
					break;
			case 2:
				str = "闭嘴";	
				break;
			case 3:
				str = "张嘴";	
				break;
			default:
				break;
		}	
		return str;
	}
	
	/**
	 * @param RC
	 * @return
	 */
	public String getRC(int RC) {
		String str = "未识别";	
		switch (RC) {
			case 0:
				str = "未知";
				break;
			case 1:
				str = "未识别";	
					break;
			case 2:
				str = "2";
				break;
			case 3:
				str = "3";
				break;
			case 4:
				str = "4";
				break;
			default:
				break;
		}	
		return str;
	}
	
	/**
	 * 眼睛状态
	 * @param eye
	 * @return
	 */
	public String getEyeState(int eye) {
		String str = "未识别";	
		switch (eye) {
			case 0:
				str = "未知";
				break;
			case 1:
				str = "未识别";	
					break;
			case 2:
				str = "闭眼";	
				break;
			case 3:
				str = "睁眼";	
				break;
			default:
				break;
		}	
		return str;
	}
	
	/**
	 * 获取人脸表情
	 */
	public String getFaceFeature(int type) {
		String featureStr = "";
		switch (type) {
			case 0:
				featureStr = "未知";
				break;
			case 1:
				featureStr = "戴眼镜";
				break;
			case 2:
				featureStr = "微笑";
				break;
			case 3:
				featureStr = "愤怒";
				break;
			case 4:
				featureStr = "悲伤";
				break;
			case 5:
				featureStr = "厌恶";
				break;
			case 6:
				featureStr = "害怕";
				break;
			case 7:
				featureStr = "惊讶";
				break;
			case 8:
				featureStr = "正常";
				break;
			case 9:
				featureStr = "大笑";
				break;
			default:
				break;
		}
		return featureStr;
	}
	
	/**
	 * 获取口罩状态
	 */
	public String getMaskState(int type) {
		String maskStateStr = "";
		switch (type) {
			case 0:
				maskStateStr = "未知";
				break;
			case 1:
				maskStateStr = "未识别";
				break;
			case 2:
				maskStateStr = "没戴口罩";
				break;
			case 3:
				maskStateStr = "戴口罩";
				break;
			default:
				break;
		}
		return maskStateStr;
	}
	
	/**
	 * 获取眼镜状态
	 */
	public String getGlasses(int byGlasses) {
		String glassesStr = "";
		switch (byGlasses) {
			case 0:
				glassesStr = "未知";
				break;
			case 1:
				glassesStr = "不戴眼镜";
				break;
			case 2:
				glassesStr = "戴眼镜";
				break;
			default:
				break;
		}
		return glassesStr;
	}
	
	/**
	 * 选择一张本地图片，并在面板上显示，获取图片缓存和大小
	 * @param panel 显示图片的面板
	 * @return
	 */
	public boolean selectPicture(PaintJPanel panel) {
		// 读取图片
		JFileChooser jfc = new JFileChooser("d:/");
		jfc.setMultiSelectionEnabled(false);   // 不可以拖选多个文件
		jfc.setAcceptAllFileFilterUsed(true);  // 打开显示所有

        if(jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        	selectImagePath = jfc.getSelectedFile().getAbsolutePath();

        	BufferedImage bufferedImage = null;
			if(selectImagePath == null || selectImagePath.equals("")) {
				return false;
			}
			
			File file = new File(selectImagePath);
			if(!file.exists()) {
				return false;
			}
			try {
				bufferedImage = ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(bufferedImage == null) {
				panel.setOpaque(true);
				panel.repaint();
				return false;
			} else {
				panel.setOpaque(false);
				panel.setImage(bufferedImage);
				panel.repaint();
			}
			
		    // 当前接口的文件大小为int，这里进行强制转换，
		    // 意味着只能支持2G以下的文件
			nPicBufLen = (int)ToolKits.GetFileSize(selectImagePath);   // 图片大小
			
			// 读取文件大小失败
			if (nPicBufLen <= 0) {
				JOptionPane.showMessageDialog(null, "读取图片大小失败，请重新选择！");
	            return false;
			}

			memory = new Memory(nPicBufLen);   // 申请缓存
			memory.clear();
			
			if (!ToolKits.ReadAllFileToMemory(selectImagePath, memory)) {
	        	System.out.printf("read all file from %s to memory failed!!!\n");
	            return false;
			}
		}
        return true;
	}
	
	/**
	 * 根据图片地址，先下载图片，再读取缓存和大小，并在面板上显示出来
	 * @param picPath 图片地址
	 * @param panel 显示图片的面板
	 * @return
	 */
	public boolean readPictureMemory(PaintJPanel panel) {	
		selectImagePath = "./face.jpg";  // 下载的图片路径
		
		// 读取图片
		BufferedImage bufferedImage = null;
		if(selectImagePath == null || selectImagePath.equals("")) {
			System.err.println("selectImagePath == null || selectImagePath");
			return false;
		}
		
		File file = new File(selectImagePath);
		if(!file.exists()) {
			return false;
		}
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	    // 当前接口的文件大小为int，这里进行强制转换，
	    // 意味着只能支持2G以下的文件
		nPicBufLen = (int)ToolKits.GetFileSize(selectImagePath);
		
		// 读取文件大小失败
		if (nPicBufLen <= 0) {
			JOptionPane.showMessageDialog(null, "读取图片大小失败，请重新选择！");
            return false;
		}
		
		memory = new Memory(nPicBufLen);
		memory.clear();
		
		if (!ToolKits.ReadAllFileToMemory(selectImagePath, memory)) {
        	System.err.printf("read all file from %s to memory failed!!!\n");
            return false;
		}
		
		file.delete();   // 删除下载的图片
		
		if(bufferedImage == null) {
			panel.setOpaque(true);
			panel.repaint();
		} else {
			panel.setOpaque(false);
			panel.setImage(bufferedImage);
			panel.repaint();
		}

		return true;
	}
	
	/**
	 * 显示/关闭规则库
	 * @param RealPlayHandle  实时预览余静
	 * @param bTrue    1 打开, 0 关闭
	 * @return
	 */
	public boolean renderPrivateData(LLong RealPlayHandle, int bTrue) {
		return netsdkApi.CLIENT_RenderPrivateData(m_hRealPlayHandle, bTrue);
	}
	
	/**
	 * 是否从人脸图片计算了特征值
	 * @param groupId   当前查找的人脸库的id库的id
	 * @param uid   当前查找的人员信息的uid
	 */
	public void getFaceRecognitionFeatureState(String groupId, String uid) {
		System.out.println(groupId + " / " + uid);
		// IVVS设备，查询条件只有  stInStartFind.stPerson 里的参数有效
		NET_IN_STARTFIND_FACERECONGNITION stInStartFind = new NET_IN_STARTFIND_FACERECONGNITION();
		// 人员信息查询条件是否有效, 并使用扩展结构体
		stInStartFind.bPersonExEnable = 1;   
		
		System.arraycopy(groupId.getBytes(), 0, stInStartFind.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);   // 人员组ID
		System.arraycopy(uid.getBytes(), 0, stInStartFind.stPersonInfoEx.szUID, 0, uid.getBytes().length);   // UID
	    
		//设置过滤条件
		stInStartFind.stFilterInfo.nGroupIdNum = 1;   // 人员组数
		System.arraycopy(groupId.getBytes(), 0, stInStartFind.stFilterInfo.szGroupIdArr[0].szGroupId, 0, groupId.getBytes().length);  // 人员组ID
		stInStartFind.stFilterInfo.nRangeNum = 1;
		stInStartFind.stFilterInfo.szRange[0] = EM_FACE_DB_TYPE.NET_FACE_DB_TYPE_BLACKLIST;
		stInStartFind.stFilterInfo.emFaceType = EM_FACERECOGNITION_FACE_TYPE.EM_FACERECOGNITION_FACE_TYPE_ALL;

		stInStartFind.nChannelID = -1;
	    
	    //让设备根据查询条件整理结果集
	    NET_OUT_STARTFIND_FACERECONGNITION stOutParam = new NET_OUT_STARTFIND_FACERECONGNITION();
	    stInStartFind.write();
	    stOutParam.write();
	    if(netsdkApi.CLIENT_StartFindFaceRecognition(m_hLoginHandle, stInStartFind,  stOutParam, 2000))
	    {        
	    	int doNextCount = 0;	// 查询次数
	    	int count = 1;    		// 每次查询的个数
	        //分页查找数据
	        NET_IN_DOFIND_FACERECONGNITION  stFindIn = new NET_IN_DOFIND_FACERECONGNITION();
	        stFindIn.lFindHandle = stOutParam.lFindHandle;
	        stFindIn.nCount      = count;  // 当前想查询的记录条数
	        stFindIn.nBeginNum   = 0 ;  //每次递增
	        
	        NET_OUT_DOFIND_FACERECONGNITION stFindOut = new NET_OUT_DOFIND_FACERECONGNITION();;
	        stFindOut.bUseCandidatesEx = 1;   // 使用候选对象扩展结构体
	        

        	stFindIn.write();
        	stFindOut.write();
            if(netsdkApi.CLIENT_DoFindFaceRecognition(stFindIn, stFindOut, 1000)) {
    			System.out.println("nFeatureState: " + stFindOut.stuCandidatesEx[0].stPersonInfo.emFeatureState);
    			String strFeatureState = "是否从人脸图片计算了特征值: ";
    			switch (stFindOut.stuCandidatesEx[0].stPersonInfo.emFeatureState) {
				case EM_PERSON_FEATURE_STATE.EM_PERSON_FEATURE_UNKNOWN:
					strFeatureState += "未知";
					break;
				case EM_PERSON_FEATURE_STATE.EM_PERSON_FEATURE_FAIL:
					strFeatureState += "建模失败";
					break;
				case EM_PERSON_FEATURE_STATE.EM_PERSON_FEATURE_USEFUL:
					strFeatureState += "有可用的特征值";
					break;
				case EM_PERSON_FEATURE_STATE.EM_PERSON_FEATURE_CALCULATING:
					strFeatureState += "正在计算特征值";
					break;
				case EM_PERSON_FEATURE_STATE.EM_PERSON_FEATURE_UNUSEFUL:
					strFeatureState += "已建模但需要重新建模";
					break;
				default:
					strFeatureState += "未知";
					break;
				}
    			textFieldFeature.setText(strFeatureState);                        
            } else {
            	System.out.printf("CLIENT_DoFindFaceRecognition Failed!LastError = %x\n" , netsdkApi.CLIENT_GetLastError());
            }
	          
	        netsdkApi.CLIENT_StopFindFaceRecognition(stOutParam.lFindHandle);	        
	    } else {
	        System.out.println("CLIENT_StartFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
	    }
	}
	
	/**
	 * 查询对比数据
	 */
	public void findFile() {
		int type = EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE;
		
		/**
		 *  查询条件
		 */
		MEDIAFILE_FACERECOGNITION_PARAM findContion = new MEDIAFILE_FACERECOGNITION_PARAM();
		
		// 开始时间
		findContion.stStartTime.dwYear = 2018;
		findContion.stStartTime.dwMonth = 6;
		findContion.stStartTime.dwDay = 1;
		findContion.stStartTime.dwHour = 13;
		findContion.stStartTime.dwMinute = 0;
		findContion.stStartTime.dwSecond = 0;
		
		// 结束时间
		findContion.stEndTime.dwYear = 2018;
		findContion.stEndTime.dwMonth = 6;
		findContion.stEndTime.dwDay = 1;
		findContion.stEndTime.dwHour = 14;
		findContion.stEndTime.dwMinute = 59;
		findContion.stEndTime.dwSecond = 59;
		
		/**
		 * 以下注释的查询条件参数，目前设备不支持，后续会逐渐增加
		 */
//		// 地点,支持模糊匹配 
//		String machineAddress = "";
//		System.arraycopy(machineAddress.getBytes(), 0, findContion.szMachineAddress, 0, machineAddress.getBytes().length);
//		
//		// 待查询报警类型
//		findContion.nAlarmType = EM_FACERECOGNITION_ALARM_TYPE.NET_FACERECOGNITION_ALARM_TYPE_ALL;
		
		// 通道号
		findContion.nChannelId = 0;
		
//		// 人员组数 
//		findContion.nGroupIdNum = 1;  
//		
//		// 人员组ID(人脸库ID)
//		String groupId = "";
//		System.arraycopy(groupId.getBytes(), 0, findContion.szGroupIdArr[0].szGroupId, 0, groupId.getBytes().length);
//		
//		// 人员信息扩展是否有效
//		findContion.abPersonInfoEx = 1;     
//		
//		// 人员组ID(人脸库ID)
//		System.arraycopy(groupId.getBytes(), 0, findContion.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);
		
		findContion.write();
		LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(m_hLoginHandle, type, findContion.getPointer(), null, 3000);
		if(lFindHandle.longValue() == 0) {
			System.err.println("FindFileEx Failed!" + netsdkApi.CLIENT_GetLastError());
			return;
		}
		findContion.read();
		
		
		int nMaxConut = 10;
		MEDIAFILE_FACERECOGNITION_INFO[] faceRecognitionInfo = new MEDIAFILE_FACERECOGNITION_INFO[nMaxConut];
		for (int i = 0; i < faceRecognitionInfo.length; ++i) {
			faceRecognitionInfo[i] = new MEDIAFILE_FACERECOGNITION_INFO();
			faceRecognitionInfo[i].bUseCandidatesEx = 1;
		}
		
		int MemorySize = faceRecognitionInfo[0].size() * nMaxConut;
		Pointer pointer = new Memory(MemorySize);
		pointer.clear(MemorySize);
		
		ToolKits.SetStructArrToPointerData(faceRecognitionInfo, pointer);
		
		//循环查询
		int nCurCount = 0;
		int nFindCount = 0;
		while(true) {
			int nRetCount = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, pointer, MemorySize, null, 3000);
			ToolKits.GetPointerDataToStructArr(pointer, faceRecognitionInfo);
			
			if (nRetCount <= 0) {
				System.err.println("FindNextFileEx failed!" + netsdkApi.CLIENT_GetLastError());
                break;
			} 
	
			for (int i = 0; i < nRetCount; i++) {
				nFindCount = i + nCurCount * nMaxConut;
				System.out.println("[" + nFindCount + "]通道号 :" + faceRecognitionInfo[i].nChannelId);
				System.out.println("[" + nFindCount + "]报警发生时间 :" + faceRecognitionInfo[i].stTime.toStringTime());
	
				// 人脸图
				System.out.println("[" + nFindCount + "]人脸图路径 :" + new String(faceRecognitionInfo[i].stObjectPic.szFilePath).trim());
				
				// 对比图
				System.out.println("[" + nFindCount + "]匹配到的候选对象数量 :" + faceRecognitionInfo[i].nCandidateNum);
				for(int j = 0; j < faceRecognitionInfo[i].nCandidateNum; j++) {  
					for(int k = 0; k < faceRecognitionInfo[i].stuCandidatesPic[j].nFileCount; k++) {
						System.out.println("[" + nFindCount + "]对比图路径 :" + new String(faceRecognitionInfo[i].stuCandidatesPic[j].stFiles[k].szFilePath).trim());
					}	
				}	

				// 对比信息   
				System.out.println("[" + nFindCount + "]匹配到的候选对象数量 :" + faceRecognitionInfo[i].nCandidateExNum);
				for(int j = 0; j < faceRecognitionInfo[i].nCandidateExNum; j++) {  
					System.out.println("[" + nFindCount + "]人员唯一标识符 :" + new String(faceRecognitionInfo[i].stuCandidatesEx[j].stPersonInfo.szUID).trim());
					
					// 以下参数，设备有些功能没有解析，如果想要知道   对比图的人员信息，可以根据上面获取的 szUID，用 findFaceRecognitionDB() 来查询人员信息。
					// findFaceRecognitionDB() 此示例的方法是根据 GroupId来查询的，这里的查询，GroupId不填，根据 szUID 来查询
					System.out.println("[" + nFindCount + "]姓名 :" + new String(faceRecognitionInfo[i].stuCandidatesEx[j].stPersonInfo.szPersonName).trim());
					System.out.println("[" + nFindCount + "]相似度 :" + faceRecognitionInfo[i].stuCandidatesEx[j].bySimilarity);
					System.out.println("[" + nFindCount + "]年龄 :" + faceRecognitionInfo[i].stuCandidatesEx[j].stPersonInfo.byAge);
					System.out.println("[" + nFindCount + "]人脸库名称 :" + new String(faceRecognitionInfo[i].stuCandidatesEx[j].stPersonInfo.szGroupName).trim());
					System.out.println("[" + nFindCount + "]人脸库ID :" + new String(faceRecognitionInfo[i].stuCandidatesEx[j].stPersonInfo.szGroupID).trim());
				}
				
				System.out.println();
			}
			
			if(nRetCount < nMaxConut) {
				break;
			} else {
				nCurCount++;
			}
		} 
		
		netsdkApi.CLIENT_FindCloseEx(lFindHandle);	
	}
	
	/////////////////////组件//////////////////////////
	//////////////////////////////////////////////////

	// 登录组件
	private LoginPanel loginPanel;
	private JButton loginBtn;
	private JButton logoutBtn;
	
    private JLabel ipLabel;
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JLabel portLabel;
	
    private JTextField ipTextArea;
	private JTextField portTextArea;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;	
	
	// 人脸库组件
	private JLabel groupNameLabel;
	private JTextField groupNameText;		
	private JButton addGroupBtn;
	private JButton modifyGroupBtn;
	private JButton delGroupBtn;
	private JButton delAllGrouopBtn;
	private JButton putDispositionBtn;
	private JButton delDispositionBtn;
	
	// 人脸识别事件
	private JButton realoadPictureBtn;
	private JButton searchPicBtn;
	private JButton openRenderPrivateDataBtn;
	private JButton closeRenderPrivateDataBtn;
	
	// 人员信息组件
	private JButton addFaceDBBtn;
	private JButton modifyFaceDBBtn;
	private JButton delFaceDBBtn;
	
	// 人脸库列表组件
	private DefaultTableModel groupDefaultModel;
	private JTable groupTable;
	
	// 人员信息列表组件
	private DefaultTableModel serverDefaultModel;
	private JTable serverTable;
	
	// 人脸识别面板控件
	private Panel realplayPanel;
	private PaintJPanel globalPicPanel;
	private PaintJPanel personPicPanel;
	private PaintJPanel candidatePanel;
	
	// 以图搜图控件
	private DateChooserJButtonEx startTimeBtn;
	private DateChooserJButtonEx endTimeBtn;
	private JComboBox channelSearchCheckBox;
	private JTextField similarySearchTextField;
	private PaintJPanel searchByPaintJPanel;
	private JProgressBar progressBar;
	private JButton searchByPicBtu;
	private JPanel showPanel;
	
	private JComboBox chnComboBox;
	private JButton realplayBtn;
	private JButton stopRealplayBtn;
	private JButton attachBtn;
	private JButton detachBtn;
	private JTextField sexTextField;
	private JTextField ageTextField;
	private JTextField eyeTextField;
	private JTextField raceTextField;
	private JTextField mouthTextField;
	private JTextField maskTextField;
	private JTextField beardTextField;
	private JTextField groupIdTextField;
	private JTextField groupNameTextField;
	private JTextField personNameTextField;
	private JTextField similaryTextField;
	private JTextField textFieldFeature;
	
}
public class FaceRecognition {  
	public static void main(String[] args) {	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				FaceRecognitionFrame demo = new FaceRecognitionFrame();
				demo.setVisible(true);
			}
		});		
	}
}
