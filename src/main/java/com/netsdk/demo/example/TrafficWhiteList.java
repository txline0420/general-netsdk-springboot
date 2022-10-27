package com.netsdk.demo.example;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * @author 29779
 *
 */
class JNATrafficListFrame extends Frame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.32.5.54";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin123";
	private int nNo = 0;
	private String[] name = {"序号", "车牌号", "车主", "开始时间", "结束时间", "开闸模式"};

	//设备信息
	private NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 对应CLIENT_LoginEx2
	private LLong m_hLoginHandle = new LLong(0);   //登陆句柄
	private NetSDKLib.NET_TRAFFIC_LIST_RECORD pstRecordAdd = new NetSDKLib.NET_TRAFFIC_LIST_RECORD(); // 开闸权限
	
	//////////////////SDK相关信息///////////////////////////
	//NetSDK 库初始化
	public class SDKEnvironment {
		
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
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\TrafficList" + System.currentTimeMillis() + ".log";			
			
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

	public JNATrafficListFrame() {	
		sdkEnv = new SDKEnvironment();
		sdkEnv.init();
	    setTitle("TrafficList");
	    setSize(900, 650);
	    setLayout(new BorderLayout());
	    setLocationRelativeTo(null);
	    setVisible(true);
        	    
	    loginPanel = new LoginPanel();
	    TrafficPanel trafficPanel = new TrafficPanel();
	    QueryViewPanel queryViewPanel = new QueryViewPanel();
	     
	    add(loginPanel, BorderLayout.NORTH);
	    add(trafficPanel, BorderLayout.WEST);
	    add(queryViewPanel, BorderLayout.CENTER);
	    
	    addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
	    		System.out.println("Window Closing");
	    		//登出
	    		logoutButtonPerformed(null);    		
	    		dispose();		
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
		    		new SDKEnvironment().init();
		    		loginButtonPerformed(e);
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
		
	//允许名单操作面板
	public class TrafficPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TrafficPanel() {
			setBorderEx(this, "允许名单操作", 4);
			Dimension dim = this.getPreferredSize();
			dim.width = 300;
			this.setPreferredSize(dim);	
			
			SinglePanel singlePanel = new SinglePanel();
			BatchPanel batchPanel = new BatchPanel();
			setLayout(new BorderLayout());
            add(singlePanel, BorderLayout.NORTH);
            add(batchPanel, BorderLayout.SOUTH);
		}
	}
	
	// 单个上传面板
	public class SinglePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SinglePanel(){
			setBorderEx(this, "单个上传", 4);
			Dimension dim = this.getPreferredSize();
			dim.height = 200;
			this.setPreferredSize(dim);
			setLayout(new GridLayout(4, 2, 30, 20));

			numLabel = new JLabel("输入要查的车牌号：");
			numTextArea = new JTextField("");
			queryBtn = new JButton("查询");
			queryExBtn = new JButton("模糊查询  ");
			addBtn = new JButton("添加");
			deleteBtn = new JButton("删除");
			modifyBtn = new JButton("修改");
			alldeleteBtn = new JButton("全部删除");
			
			queryBtn.setEnabled(false);
			queryExBtn.setEnabled(false);
			addBtn.setEnabled(false);
			deleteBtn.setEnabled(false);
			modifyBtn.setEnabled(false);
			alldeleteBtn.setEnabled(false);
			
			add(numLabel);
			add(numTextArea);
			add(queryBtn);
			add(queryExBtn);
			add(addBtn);
			add(deleteBtn);
			add(modifyBtn);
			add(alldeleteBtn);
			
			queryBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					DefaultTableModel model = (DefaultTableModel)table.getModel();
					model.setRowCount(0);  // 在模糊查询前，清空表格
					data = new Object[200][6];  // 再重设表格，
					query();
				}
			});
			queryExBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DefaultTableModel model = (DefaultTableModel)table.getModel();
					model.setRowCount(0);  // 在模糊查询前，清空表格
					data = new Object[200][6];  // 再重设表格，
					queryEx();
				}
			});
			addBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new AddFrame();
				}					
			});
			deleteBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {		
					int rowCount = table.getSelectedRowCount();
					if(rowCount > 0) {
						deleteOperate();		
						int row = table.getSelectedRow();
						DefaultTableModel model = (DefaultTableModel)table.getModel();
						model.removeRow(row);   // 删除选中的行
						data = new Object[200][6];  // 再重设表格，
					}				
				}
			});
			modifyBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int rowCount = table.getSelectedRowCount();
					if(rowCount > 0) {
						new ModifyFrame();
						int row = table.getSelectedRow(); //获得所选的单行
						nullTextArea31.setText(String.valueOf(model.getValueAt(row, 1)));
						nullTextArea41.setText(String.valueOf(model.getValueAt(row, 2)));
						startTextArea1.setText(String.valueOf(model.getValueAt(row, 3)));
						endTextArea1.setText(String.valueOf(model.getValueAt(row, 4)));
						if((model.getValueAt(row, 5)).equals("授权")) {
							jr1.setSelected(true);
						} else {
							jr1.setSelected(false);
						}
					} 
				}
			});
			alldeleteBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {		
					alldeleteOperate();
				}
			});
		}
	}
	
	// 批量上传面板
	public class BatchPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public BatchPanel() {
			setBorderEx(this, "批量上传", 4);
			Dimension dim = this.getPreferredSize();
			dim.height = 150;
			this.setPreferredSize(dim);
			setLayout(new GridLayout(3, 2, 30, 20));
			
			browseTextArea = new JTextField();
			browseBtn = new JButton("浏览");
			nullLabel1 = new JLabel("");
			upLoadBtn = new JButton("上传");
			nullLabel2 = new JLabel("");		
			browseTextArea.setEditable(false);
			browseBtn.setEnabled(false);
			upLoadBtn.setEnabled(false);

			add(browseTextArea);
			add(browseBtn);
			add(nullLabel1);
			add(upLoadBtn);
			add(nullLabel2);
			
			browseBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					jfc = new JFileChooser();
					jfc.setMultiSelectionEnabled(true); //可以拖选多个文件
					jfc.setAcceptAllFileFilterUsed(false); //关掉显示所有
					//添加过滤器
					jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
						public boolean accept(File f) {
							if(f.getName().endsWith(".CSV")||f.isDirectory()) {
								return true;
							}
							return false;
						}
						public String getDescription() {
							return ".CSV";
						}
					});
			        if(jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			        	System.out.println(jfc.getSelectedFile().getAbsolutePath());
			        	browseTextArea.setText(jfc.getSelectedFile().getAbsolutePath());
			        } 
				}
			});
			
			upLoadBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(browseTextArea.getText().isEmpty()){
						
					}else {
						upLoad();
					}
				}
			});
		}
	}
	
	// 查询显示 面板
	public class QueryViewPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public QueryViewPanel() {
			setBorderEx(this, "查询信息" , 4);	
			setLayout(new BorderLayout());
			
			// 在JTable列表里添加一个模版，信息存在模版里
			data = new Object[200][6];
			model = new DefaultTableModel(data, name);
			table = new JTable(model);
			
			// 设置某列的宽度
			table.getColumnModel().getColumn(0).setPreferredWidth(40);
			table.getColumnModel().getColumn(3).setPreferredWidth(120);
			table.getColumnModel().getColumn(4).setPreferredWidth(120);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行		

			// 建立滑动面板，并插入列表
			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane, BorderLayout.CENTER);
		}
	}

	//添加按钮窗口	
	public class AddFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public AddFrame(){
			setTitle("Dialog");
			setSize(450, 450);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
            DialogPanel dialogPanel = new DialogPanel();
            add(dialogPanel, BorderLayout.CENTER);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class DialogPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public DialogPanel() {
				setBorderEx(this, "添加", 4);
				Dimension dim = this.getPreferredSize();
				dim.height = 400;
				dim.width = 400;
				this.setPreferredSize(dim);
				setLayout(new GridLayout(3, 1));
				JPanel jp11 = new JPanel();
				JPanel jp1 = new JPanel();
				JPanel jp2 = new JPanel();
				JPanel jp3 = new JPanel();
				
				numberLabel = new JLabel("车牌号:");
				//下拉菜单设置选项						
				String[] str = {"京","津","冀","晋","内蒙古","辽","吉","黑","沪","鲁","苏","浙","皖","闽","赣","豫","鄂","湘",
						"粤","桂","琼","渝","川","贵","云","藏","陕","甘","青","宁","新","港","澳","台"};
	            /*ComboBoxModel jComboBoxModel = new DefaultComboBoxModel(str);
				jComboBox.setModel(jComboBoxModel);*/
				jComboBox = new JComboBox(str);	
				jComboBox.setPreferredSize(new Dimension(100, 25));  // 设置宽度
				
				nullTextArea3 = new JTextField(8);
				userLabel = new JLabel("车主:");
				nullTextArea4 = new JTextField(8);
				startTime = new JLabel("开始时间:");
				startTextArea = new JTextField("2016/7/27 7:07:07");       		
				stopTime = new JLabel("结束时间:");
				endTextArea = new JTextField("2016/7/27 7:07:07");				
				
				jr = new JRadioButton("授权");
				jr.setSelected(true);
				okBtn = new JButton("OK");
				cancleBtn = new JButton("Cancle");
				
				jp11.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp11.add(jComboBox);
				jp11.add(nullTextArea3);
				
				jp1.setLayout(new GridLayout(4, 2, 1, 8));
				jp1.add(numberLabel);
				jp1.add(jp11);
				jp1.add(userLabel);
				jp1.add(nullTextArea4);
				jp1.add(startTime);
				jp1.add(startTextArea);
				
				jp1.add(stopTime);
				jp1.add(endTextArea);
				
				jp2.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp2.add(jr);
				
				jp3.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp3.add(okBtn);
				jp3.add(cancleBtn);
								
				add(jp1);
				add(jp2);
				add(jp3);
				
				okBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(jr.isSelected()) {
							pstRecordAdd.stAuthrityTypes[0].emAuthorityType = NetSDKLib.EM_NET_AUTHORITY_TYPE.NET_AUTHORITY_OPEN_GATE;
							pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable = true;	
						} else {
							pstRecordAdd.stAuthrityTypes[0].emAuthorityType = NetSDKLib.EM_NET_AUTHORITY_TYPE.NET_AUTHORITY_OPEN_GATE;
							pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable = false;	
						}
						addOperate();
						dispose();
					}
				});
				
				cancleBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});						
			}		 
		}	 	
	}
	// 修改按钮窗口	
	public class ModifyFrame extends Frame{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public ModifyFrame(){
			setTitle("ModifyPanel");
			setSize(450, 450);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
            ModifyPanel modifyPanel = new ModifyPanel();
            add(modifyPanel, BorderLayout.CENTER);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});
		}
		public class ModifyPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public ModifyPanel() {
				setBorderEx(this, "修改", 4);
				Dimension dim = this.getPreferredSize();
				dim.height = 400;
				dim.width = 400;
				this.setPreferredSize(dim);
				setLayout(new GridLayout(3, 1));
				JPanel jp111 = new JPanel();
				JPanel jp11 = new JPanel();
				JPanel jp21 = new JPanel();
				JPanel jp31 = new JPanel();
				
				numberLabel1 = new JLabel("车牌号:");				
				nullTextArea31 = new JTextField(18);	
				nullTextArea31.setEditable(false);
				userLabel1 = new JLabel("车主:");
				nullTextArea41 = new JTextField(8);
				startTime1 = new JLabel("开始时间:");
				startTextArea1 = new JTextField("2016/7/27 7:07:07");       		
				stopTime1 = new JLabel("结束时间:");
				endTextArea1 = new JTextField("2016/7/27 7:07:07");				
				
				jr1 = new JRadioButton("授权");
				okBtn1 = new JButton("OK");
				cancleBtn1 = new JButton("Cancle");
				
				jp111.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp111.add(nullTextArea31);
				
				jp11.setLayout(new GridLayout(4, 2, 1, 8));
				jp11.add(numberLabel1);
				jp11.add(jp111);
				jp11.add(userLabel1);
				jp11.add(nullTextArea41);
				jp11.add(startTime1);
				jp11.add(startTextArea1);
				
				jp11.add(stopTime1);
				jp11.add(endTextArea1);
				
				jp21.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp21.add(jr1);
				
				jp31.setLayout(new FlowLayout(FlowLayout.CENTER));
				jp31.add(okBtn1);
				jp31.add(cancleBtn1);
								
				add(jp11);
				add(jp21);
				add(jp31);
				
				okBtn1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(jr1.isSelected()) {
							pstRecordAdd.stAuthrityTypes[0].emAuthorityType = NetSDKLib.EM_NET_AUTHORITY_TYPE.NET_AUTHORITY_OPEN_GATE;
							pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable = true;	
						} else {
							pstRecordAdd.stAuthrityTypes[0].emAuthorityType = NetSDKLib.EM_NET_AUTHORITY_TYPE.NET_AUTHORITY_OPEN_GATE;
							pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable = false;	
						}
						modifyOperate();
						dispose();
					}
				});
				
				cancleBtn1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});						
			}		 
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
		
		int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP; //=0
		IntByReference nError = new IntByReference(0);
		m_hLoginHandle = NetSdk.CLIENT_LoginEx2(m_strIp, m_nPort.intValue(), m_strUser, m_strPassword, nSpecCap, null, m_stDeviceInfo, nError);
		if(m_hLoginHandle.longValue() == 0) {
			int error = 0;
			error = NetSdk.CLIENT_GetLastError();
			System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[0x%x]\n", m_strIp, m_nPort, error);
			JOptionPane.showMessageDialog(this, "登录失败，错误码 ：" + String.format("[0x%x]", error));	
		} else {
			System.out.println("Login Success [ " + m_strIp + " ]");
			JOptionPane.showMessageDialog(this, "登录成功");
			logoutBtn.setEnabled(true);
			loginBtn.setEnabled(false);	
			queryBtn.setEnabled(true);
			queryExBtn.setEnabled(true);
			addBtn.setEnabled(true);
			deleteBtn.setEnabled(true);
			modifyBtn.setEnabled(true);
			browseBtn.setEnabled(true);
			upLoadBtn.setEnabled(true);
			alldeleteBtn.setEnabled(true);
		}	
	}
	
	//登出按钮事件
	private void logoutButtonPerformed(ActionEvent e) {
		if(m_hLoginHandle.longValue() != 0) {
			System.out.println("Logout Button Action");
		
			if(NetSdk.CLIENT_Logout(m_hLoginHandle)) {
				System.out.println("Logout Success [ " + m_strIp + " ]");
				m_hLoginHandle.setValue(0);
				logoutBtn.setEnabled(false);
				loginBtn.setEnabled(true);	
				queryBtn.setEnabled(false);
				queryExBtn.setEnabled(false);
				addBtn.setEnabled(false);
				deleteBtn.setEnabled(false);
				modifyBtn.setEnabled(false);
				browseBtn.setEnabled(false);
				upLoadBtn.setEnabled(false);
				
			}
		}
	}
    
	// 查询按钮事件
	private void query() {
		// 开始查询记录
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST; 	
		
		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		System.arraycopy(numTextArea.getText().getBytes(), 0, stuRedListCondition.szPlateNumber, 0, numTextArea.getText().getBytes().length);
		
		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

		if((numTextArea.getText()).equals("")) {
			JOptionPane.showMessageDialog(this, "请输入要查询的数据");
		}else {
			stuRedListCondition.write();
			boolean bRet = NetSdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000);
			stuRedListCondition.read();
			System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);
			if(bRet) {
				int nRecordCount = 10;
				NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
				stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
				stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
				
				NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
				stuFindNextOutParam.nMaxRecordNum = nRecordCount;
				NetSDKLib.NET_TRAFFIC_LIST_RECORD pstRecord = new NetSDKLib.NET_TRAFFIC_LIST_RECORD();
				stuFindNextOutParam.pRecordList = pstRecord.getPointer();

				pstRecord.write();
				boolean zRet = NetSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
				pstRecord.read();
				
				if(zRet) {		
					System.out.println("record are found!");
					
					for(int i=0; i < stuFindNextOutParam.nRetRecordNum; i++) {
						data[i][0] = String.valueOf(i);
						data[i][1] = new String(pstRecord.szPlateNumber).trim();
						data[i][2] = new String(pstRecord.szMasterOfCar).trim();
						data[i][3] = pstRecord.stBeginTime.toStringTime();
						data[i][4] = pstRecord.stCancelTime.toStringTime();
						if(pstRecord.stAuthrityTypes[0].bAuthorityEnable == true) {
							data[i][5] = "授权";
						} else {
							data[i][5] = "不授权";
						}
						model.setDataVector(data, name);
					}				
				} 
				
				NetSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);      
			}else {
				System.err.println("Can Not Find This Record" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));					
			}
		}    		
	}
	
	// 模糊查询按钮事件
	private void queryEx() {
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST; 
		
		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListConditionEx = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListConditionEx.getPointer();
		ToolKits.ByteArrZero(stuRedListConditionEx.szPlateNumberVague);
		System.arraycopy(numTextArea.getText().getBytes(), 0, stuRedListConditionEx.szPlateNumberVague, 0, numTextArea.getText().getBytes().length);
		
		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();
		
		stuRedListConditionEx.write(); 
		boolean bRet = NetSdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 10000);
		stuRedListConditionEx.read();
		
		if(bRet) {
			int doNextCount = 0;
			while(true) {
				int nRecordCount = 10;
				NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
				stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
				stuFindNextInParam.nFileCount = nRecordCount;
				
				NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
				stuFindNextOutParam.nMaxRecordNum = nRecordCount;
				NetSDKLib.NET_TRAFFIC_LIST_RECORD pstRecordEx = new NetSDKLib.NET_TRAFFIC_LIST_RECORD();
				stuFindNextOutParam.pRecordList = new Memory(pstRecordEx.dwSize * nRecordCount);   //分配(stRecordEx.dwSize * nRecordCount)个内存

				// 把内存里的dwSize赋值
				for (int i=0; i<stuFindNextOutParam.nMaxRecordNum; ++i)
				{
					ToolKits.SetStructDataToPointer(pstRecordEx, stuFindNextOutParam.pRecordList, i*pstRecordEx.dwSize);
				}
				
				pstRecordEx.write();
				boolean zRet = NetSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 10000);
				pstRecordEx.read();
				
				if(zRet) {	
					System.out.println("查询到的个数：" + stuFindNextOutParam.nRetRecordNum);
					for(int i=0; i < stuFindNextOutParam.nRetRecordNum; i++) {
						int item = i + doNextCount * nRecordCount;
						data[item][0] = String.valueOf(item);
						ToolKits.GetPointerDataToStruct(stuFindNextOutParam.pRecordList, i*pstRecordEx.dwSize, pstRecordEx);
						data[item][1] = new String(pstRecordEx.szPlateNumber).trim();
						data[item][2] = new String(pstRecordEx.szMasterOfCar).trim();
						data[item][3] = pstRecordEx.stBeginTime.toStringTime();
						data[item][4] = pstRecordEx.stCancelTime.toStringTime();
						if(pstRecordEx.stAuthrityTypes[0].bAuthorityEnable == true) {
							data[item][5] = "授权";
						} else {
							data[item][5] = "不授权";
						}
						model.setDataVector(data, name);
					}
					
					if (stuFindNextOutParam.nRetRecordNum < nRecordCount)
					{
						break;
					} else {
						doNextCount ++;
					}
				} else {
					break;
				}
			}
			NetSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
		}else {
			System.err.println("Can Not Find This Record" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
		}
	}
    
	// 添加按钮事件
	private void addOperate() {
		NetSDKLib.NET_INSERT_RECORD_INFO stInsertInfo = new NetSDKLib.NET_INSERT_RECORD_INFO();  // 添加
		
		NetSDKLib.NET_TRAFFIC_LIST_RECORD.ByReference stRec = new NetSDKLib.NET_TRAFFIC_LIST_RECORD.ByReference();
		stRec.szPlateNumber = (jComboBox.getSelectedItem().toString() + nullTextArea3.getText()).getBytes();
		stRec.szMasterOfCar = nullTextArea4.getText().getBytes();
		String[] start = startTextArea.getText().split(" ");
		String st1 = start[0];
		String st2 = start[1];
		String[] start1 = st1.split("/"); //年月日
		String[] start2 = st2.split(":"); // 时分
		String[] end = endTextArea.getText().split(" ");
		String ed1 = end[0];
		String ed2 = end[1];
		String[] end1 = ed1.split("/"); //年月日
		String[] end2 = ed2.split(":"); // 时分
		stRec.stBeginTime.dwYear = Integer.parseInt(start1[0]);
		stRec.stBeginTime.dwMonth = Integer.parseInt(start1[1]);
		stRec.stBeginTime.dwDay = Integer.parseInt(start1[2]);
		stRec.stBeginTime.dwHour = Integer.parseInt(start2[0]);
		stRec.stBeginTime.dwMinute = Integer.parseInt(start2[1]);
		stRec.stBeginTime.dwSecond = Integer.parseInt(start2[2]);
		stRec.stCancelTime.dwYear = Integer.parseInt(end1[0]);
		stRec.stCancelTime.dwMonth = Integer.parseInt(end1[1]);
		stRec.stCancelTime.dwDay = Integer.parseInt(end1[2]);
		stRec.stCancelTime.dwHour = Integer.parseInt(end2[0]);
		stRec.stCancelTime.dwMinute = Integer.parseInt(end2[1]);
		stRec.stCancelTime.dwSecond = Integer.parseInt(end2[2]);
		stRec.nAuthrityNum = 1;
		stRec.stAuthrityTypes[0].emAuthorityType = pstRecordAdd.stAuthrityTypes[0].emAuthorityType;
		stRec.stAuthrityTypes[0].bAuthorityEnable = pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable;
		
		stInsertInfo.pRecordInfo = stRec;

		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_INSERT;	
		stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;
		stInParam.pstOpreateInfo = stInsertInfo.getPointer();
	
		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();
		stRec.write();
		stInsertInfo.write();
		stInParam.write();

		boolean zRet = NetSdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if(zRet) {
			stInParam.read();
			System.out.println("succeed!");
		} else {
			System.err.println("failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
		}
	}
	
	// 查询之前的记录号
	private void findRecordCount() {
		// 开始查询记录
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST; 
		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		// 获取选中行的车牌号，并赋值
		int row = table.getSelectedRow();
		System.arraycopy(String.valueOf(model.getValueAt(row, 1)).getBytes(), 0, stuRedListCondition.szPlateNumber, 0, String.valueOf(model.getValueAt(row, 1)).getBytes().length);
		
		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();
		
		stuFindInParam.write();
		stuRedListCondition.write();
		boolean bRet = NetSdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000);
		stuRedListCondition.read();
		stuFindInParam.read();
		
		if(bRet){
			int nRecordCount = 1;

			NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
			
			NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			NetSDKLib.NET_TRAFFIC_LIST_RECORD pstRecord = new NetSDKLib.NET_TRAFFIC_LIST_RECORD();
			stuFindNextOutParam.pRecordList = pstRecord.getPointer();
			
			stuFindNextInParam.write();
			stuFindNextOutParam.write();
			pstRecord.write();
			boolean zRet = NetSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
			pstRecord.read();
			stuFindNextInParam.read();
			stuFindNextOutParam.read();
			
			if(zRet) {
				// 获取当前记录号
				nNo = pstRecord.nRecordNo;			
			}
			// 停止查询
			NetSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);      
		} else {
			System.err.println("error occured!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));					
		}
	}
	
	// 删除按钮事件
	private void deleteOperate() {
		findRecordCount();
			
		// 获得之前查询到的记录号后，开始删除数据
		NetSDKLib.NET_REMOVE_RECORD_INFO stRemoveInfo = new NetSDKLib.NET_REMOVE_RECORD_INFO();   
		stRemoveInfo.nRecordNo = nNo;
		
		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_REMOVE;	
		stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;;
		stInParam.pstOpreateInfo = stRemoveInfo.getPointer();
		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();
		
		stInParam.write();
		stRemoveInfo.write();
		boolean zRet = NetSdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if(zRet) {
			System.out.println("succeed!");
			JOptionPane.showMessageDialog(this, "删除成功");
		} else {
			System.err.println("failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
		}	
	}

	// 修改按钮事件
	private void modifyOperate() {
		findRecordCount();
		
		NetSDKLib.NET_TRAFFIC_LIST_RECORD.ByReference stRec = new NetSDKLib.NET_TRAFFIC_LIST_RECORD.ByReference();
		stRec.szPlateNumber =  nullTextArea31.getText().getBytes();
		stRec.szMasterOfCar = nullTextArea41.getText().getBytes();
		String[] start = startTextArea1.getText().split(" ");
		String st1 = start[0];
		String st2 = start[1];
		String[] start1 = st1.split("/"); //年月日
		String[] start2 = st2.split(":"); // 时分
		String[] end = endTextArea1.getText().split(" ");
		String ed1 = end[0];
		String ed2 = end[1];
		String[] end1 = ed1.split("/"); //年月日
		String[] end2 = ed2.split(":"); // 时分
		stRec.stBeginTime.dwYear = Integer.parseInt(start1[0]);
		stRec.stBeginTime.dwMonth = Integer.parseInt(start1[1]);
		stRec.stBeginTime.dwDay = Integer.parseInt(start1[2]);
		stRec.stBeginTime.dwHour = Integer.parseInt(start2[0]);
		stRec.stBeginTime.dwMinute = Integer.parseInt(start2[1]);
		stRec.stBeginTime.dwSecond = Integer.parseInt(start2[2]);
		stRec.stCancelTime.dwYear = Integer.parseInt(end1[0]);
		stRec.stCancelTime.dwMonth = Integer.parseInt(end1[1]);
		stRec.stCancelTime.dwDay = Integer.parseInt(end1[2]);
		stRec.stCancelTime.dwHour = Integer.parseInt(end2[0]);
		stRec.stCancelTime.dwMinute = Integer.parseInt(end2[1]);	
		stRec.stCancelTime.dwSecond = Integer.parseInt(end2[2]);
		stRec.nAuthrityNum = 1;
		stRec.stAuthrityTypes[0].emAuthorityType = pstRecordAdd.stAuthrityTypes[0].emAuthorityType;
		stRec.stAuthrityTypes[0].bAuthorityEnable = pstRecordAdd.stAuthrityTypes[0].bAuthorityEnable;
				
		stRec.nRecordNo = nNo;

		NetSDKLib.NET_UPDATE_RECORD_INFO stUpdateInfo = new NetSDKLib.NET_UPDATE_RECORD_INFO();
		stUpdateInfo.pRecordInfo = stRec;
		
		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_UPDATE;	
		stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;
		stInParam.pstOpreateInfo = stUpdateInfo.getPointer();
		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();
		
		stRec.write();
		stUpdateInfo.write();
		stInParam.write();		
		boolean zRet = NetSdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if(zRet) {
			System.out.println("succeed!");
			JOptionPane.showMessageDialog(this, "修改成功");
		} else {
			System.err.println("failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
		}
	}
	
	// 全部删除
	private void alldeleteOperate() {
		int type = NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR;
		NetSDKLib.NET_CTRL_RECORDSET_PARAM param = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
		param.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;
		param.write();
		boolean zRet = NetSdk.CLIENT_ControlDevice(m_hLoginHandle, type, param.getPointer(), 5000);
		if(zRet) {
			System.out.println("全部删除成功");
		} else {
			System.err.println("全部删除失败");
		}
	}
	
	// 上传按钮事件      注：上传*.CSV的文件，文件的数据会覆盖原数据库的数据，所以可以从数据库导出文件，并在文件里添加数据后，再上传
	private void upLoad() {
		NetSDKLib.NETDEV_BLACKWHITE_LIST_INFO stIn = new NetSDKLib.NETDEV_BLACKWHITE_LIST_INFO();
		Pointer szInBuf = stIn.getPointer();
		int nInBufLen = stIn.size();
		stIn.szFile = jfc.getSelectedFile().getAbsolutePath().getBytes();
		stIn.nFileSize = 1024*32;
		stIn.byFileType = 1;
		stIn.byAction = 1;
		stIn.write();
		LLong zRet = NetSdk.CLIENT_FileTransmit(m_hLoginHandle, NetSDKLib.NET_DEV_BLACKWHITETRANS_START, szInBuf, nInBufLen, null, null, 5000);
		stIn.read();
		if(zRet.longValue() == 0) {
			System.err.println("Start failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
			return;
		}
		
		stIn.write();
		LongByReference handleReference = new LongByReference(zRet.longValue());   //LLong转为Pointer*	
		LLong zRet1 = NetSdk.CLIENT_FileTransmit(m_hLoginHandle, NetSDKLib.NET_DEV_BLACKWHITETRANS_SEND, handleReference.getPointer(), nInBufLen, null, null, 20000);
		stIn.read();
		if(zRet1.longValue() == 0) {
	    	System.err.println("Send failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
	    }else {
	    	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	stIn.write();
	    	LLong zRet2 = NetSdk.CLIENT_FileTransmit(m_hLoginHandle, NetSDKLib.NET_DEV_BLACKWHITETRANS_STOP,  handleReference.getPointer(), nInBufLen, null, null, 5000);
			stIn.read();
	    	if(zRet2.longValue() == 0) {
	    		System.err.println("Stop failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
	    	} else {
	    		System.out.println("上传成功");
				JOptionPane.showMessageDialog(this, "上传成功");
	    	}
	    }
	}
	/////////////////////组件//////////////////////////
	//////////////////////////////////////////////////

	//登录组件
	private LoginPanel loginPanel;
	private JButton loginBtn;
	private JButton logoutBtn;
	
	private JLabel numLabel;
	private JTextField numTextArea;
	private JButton queryBtn;
	private JButton queryExBtn;
	private JButton addBtn;
	private JButton deleteBtn;
	private JButton modifyBtn;

	private JTextField browseTextArea;
	private JButton browseBtn;
	private JLabel nullLabel1;
	private JButton upLoadBtn;
	private JLabel nullLabel2;	
	
	private JComboBox jComboBox;
	private JLabel numberLabel;
	private JTextField nullTextArea3;
	private JLabel userLabel;
	private JTextField nullTextArea4;
	private JLabel startTime;
	private JTextField startTextArea;
	private JLabel stopTime;
	private JTextField endTextArea;
	private JRadioButton jr;
	private JButton okBtn;
	private JButton cancleBtn;
	private JButton alldeleteBtn;
	
	private JLabel numberLabel1;
	private JTextField nullTextArea31;
	private JLabel userLabel1;
	private JTextField nullTextArea41;
	private JLabel startTime1;
	private JTextField startTextArea1;
	private JLabel stopTime1;
	private JTextField endTextArea1;
	private JRadioButton jr1;
	private JButton okBtn1;
	private JButton cancleBtn1;

	private JFileChooser jfc;
    private JLabel ipLabel;
    private JTextField ipTextArea;
	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JLabel portLabel;
	
	private JTextField portTextArea;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;	
    
	private DefaultTableModel model;
	private JTable table;
	private Object[][] data;
}

public class TrafficWhiteList {  
	public static void main(String[] args) {	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JNATrafficListFrame demo = new JNATrafficListFrame();
				demo.setVisible(true);
			}
		});		
	}
}

