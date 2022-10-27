package com.netsdk.demo.example;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.USER_MANAGE_INFO_EX;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

class UserManagementFrame extends Frame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.23.1.32";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin";
	private String[] name1 = {"Group Name", "Memo"};
	private String[] name2 = {"Group Name", "User Name", "Password", "Memo"};
	
	//设备信息
	private NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();     // 对应CLIENT_LoginEx2
	private NetSDKLib.USER_GROUP_INFO_NEW m_groupInfo = new NetSDKLib.USER_GROUP_INFO_NEW();    //用户组信息
	private NetSDKLib.USER_INFO_NEW m_userInfoNew = new NetSDKLib.USER_INFO_NEW(); 				//用户信息结构体
	private NetSDKLib.USER_MANAGE_INFO_NEW m_userInfo = new NetSDKLib.USER_MANAGE_INFO_NEW();   //用户信息
	
	private LLong m_hLoginHandle = new LLong(0);   //登陆句柄	
	
	private boolean m_hqueryUserInfoNew;
	private boolean m_hoperateUserInfoNew;	
	
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
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\UserTest" + System.currentTimeMillis() + ".log";
			
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
	
	public UserManagementFrame() {	
		sdkEnv = new SDKEnvironment();
	    sdkEnv.init();   //SDK 库初始化
	    setTitle("用户管理的JNADEMO");
	    setSize(800, 600);
	    setLayout(new BorderLayout());
	    setLocationRelativeTo(null);
        	    
	    loginPanel = new LoginPanel();
	    tablePanel = new TablePanel();
	    operatePanel = new OperatePanel();
	     
	    add(loginPanel, BorderLayout.NORTH);
	    add(tablePanel, BorderLayout.CENTER);
	    add(operatePanel, BorderLayout.EAST);
	    addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
	    		System.out.println("Window Closing");
	    		//登出
	    		logoutButtonPerformed(null);   		
	    		sdkEnv.cleanup();
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
		
	//显示面板
	public class TablePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TablePanel() {
			setBorderEx(this, "列表", 2);
			setLayout(new GridLayout(2, 1));
			
			GroupPanel grojp = new GroupPanel();
			UserPanel userjp = new UserPanel();
					
			add(grojp);
			add(userjp);
		}
	}
	public class GroupPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public GroupPanel() {
			setBorderEx(this, "Group info", 4);
			setLayout(new BorderLayout());
			
			data1 = new Object[10][5];		
			groupMod = new DefaultTableModel(data1, name1) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(int row, int col) {
					return false;   // 表格不可编辑
				}
			};
			groupTab = new JTable(groupMod);			
			
			JScrollPane jscrollgro = new JScrollPane(groupTab);
			add(jscrollgro, BorderLayout.CENTER);
		}
	}
	public class UserPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public UserPanel() {
			setBorderEx(this, "User info", 4);
			setLayout(new BorderLayout());
			
			data2 = new Object[15][5];
			userMod = new DefaultTableModel(data2, name2) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(int row, int col) {
					return false;   // 表格不可编辑
				}
			};
			userTab = new JTable(userMod);
			JScrollPane jscrolluser= new JScrollPane(userTab);
			add(jscrolluser, BorderLayout.CENTER);
		}
	}
	
	//设置面板
	private class OperatePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OperatePanel() {
			setBorderEx(this, "设置面板", 4);
			setLayout(new BorderLayout());
		    Dimension dim = getPreferredSize();
			dim.width = 200;
			setPreferredSize(dim);
			
			JPanel jP1 = new JPanel();		
			jP1.setLayout(new GridLayout(10, 1));
			
			operateBtn0 = new JButton("增加用户组");
			operateBtn1 = new JButton("删除用户组");
			cancelgroField = new JTextField("net1", 10);
			operateBtn2 = new JButton("修改用户组");
			operateBtn3 = new JButton("增加用户");
			operateBtn4 = new JButton("删除用户");			
			canceluserField = new JTextField("NetSdk", 10);
			operateBtn5 = new JButton("修改用户");
			operateBtn6 = new JButton("修改密码");
			operateBtn7 = new JButton("查看设备用户信息");
			
			jP1.add(operateBtn7);
			jP1.add(operateBtn0);
			jP1.add(operateBtn1);
			jP1.add(cancelgroField);
			jP1.add(operateBtn2);
			jP1.add(operateBtn3);
			jP1.add(operateBtn4);
			jP1.add(canceluserField);
			jP1.add(operateBtn5);
			jP1.add(operateBtn6);	
			
			add(jP1, BorderLayout.NORTH);
			
			//增加用户组
			operateBtn0.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {								
					new AddGroFrame();
				}
			});
			// 删除用户组
			operateBtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					operateUserInfoNewPerformed11(1);
				}
			});
			// 修改用户组
			operateBtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					new ModifyGroFrame();
				}
			});
			// 增加用户
			operateBtn3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					new AddUserFrame();
				}
			});
			// 删除用户
			operateBtn4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					operateUserInfoNewPerformed31(4);
				}
			});
			// 修改用户
			operateBtn5.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					new ModifyUserFrame();
				}
			});
			// 修改密码
			operateBtn6.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
					new ModifyPasswordFrame();
				}
			});
			operateBtn7.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {			
//					queryUserInfo();
					queryUserInfoEx();
				}
			});
		}
	}

	// 增加用户组窗口
	public class AddGroFrame extends Frame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddGroFrame() {
			setTitle("增加用户组");
			setSize(250, 300);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			AddGroPanel groP = new AddGroPanel();		
			add(groP, BorderLayout.CENTER);	
			
			addWindowListener(new WindowAdapter()  {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});						
		}
		
		public class AddGroPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public AddGroPanel() {
				setBorderEx(this, "", 2);
				setLayout(new BorderLayout());
				
				JPanel groP1 = new JPanel();
				JPanel groP2 = new JPanel();
				
				JLabel groLabel1 = new JLabel("Group Name");
				groTex1 = new JTextField("net1", 8);
				JLabel groLabel2 = new JLabel("Memo");
				groTex2 = new JTextField("net1", 8);
				JButton groBtn1 = new JButton("OK");
				JButton groBtn2 = new JButton("Cancle");
				
				groP1.setLayout(new GridLayout(2, 2));
				groP1.add(groLabel1);
				groP1.add(groTex1);
				groP1.add(groLabel2);
				groP1.add(groTex2);
				
				groP2.setLayout(new FlowLayout());
				groP2.add(groBtn1);
				groP2.add(groBtn2);

				add(groP1, BorderLayout.NORTH);
				add(groP2, BorderLayout.SOUTH);
				
				groBtn1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						operateUserInfoNewPerformed1(0);
						dispose();
					}
				});
				groBtn2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
			}
		}
	}
	
	// 修改用户组
	public class ModifyGroFrame extends Frame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ModifyGroFrame() {
			setTitle("修改用户组");
			setSize(300, 300);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			PrimiaryJPanel primiarygroP = new PrimiaryJPanel();				
			NewJPanel newgroP = new NewJPanel();
								
			add(primiarygroP, BorderLayout.NORTH);
			add(newgroP, BorderLayout.CENTER);	
			
			addWindowListener(new WindowAdapter()  {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});						
		}			
		
		public class PrimiaryJPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public PrimiaryJPanel(){
				setLayout(new GridLayout(2, 2));
				setBorderEx(this, "Primiary Group info", 2);
				JLabel groLabel1 = new JLabel("Group Name");
				groTex13 = new JTextField("net1", 8);
				JLabel groLabel2 = new JLabel("Memo");
				groTex23 = new JTextField("net1", 8);														

				add(groLabel1);
				add(groTex13);
				add(groLabel2);
				add(groTex23);

			}
		}
		
		public class NewJPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public NewJPanel() {
				setLayout(new GridLayout(3, 2 , 3, 40));
				setBorderEx(this, "New Group info", 2);
				JLabel groLabel3 = new JLabel("Group Name");
				groTex33 = new JTextField("net2", 8);
				JLabel groLabel4 = new JLabel("Memo");
				groTex43 = new JTextField("net2", 8);
				
				JButton groBtn1 = new JButton("OK");
				JButton groBtn2 = new JButton("Cancle");					

				add(groLabel3);
				add(groTex33);
				add(groLabel4);
				add(groTex43);
				add(groBtn1);
			    add(groBtn2);
			    groBtn1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						operateUserInfoNewPerformed2(2);
						dispose();
					}
				});
			    groBtn2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
			}
		}
	}
		
	
	// 增加用户
	public class AddUserFrame extends Frame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddUserFrame() {
			setTitle("增加用户");
			setSize(300, 300);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			AddUserPanel userP = new AddUserPanel();		
			add(userP, BorderLayout.CENTER);
			
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dispose();
				}
			});
		}
		
		public class AddUserPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public AddUserPanel() {
				setBorderEx(this, "", 2);
				setLayout(new BorderLayout());
				
				JPanel addUserp1 = new JPanel();
				JPanel addUserp2 = new JPanel();
				
				JLabel userLab1 = new JLabel("User Group");
				userTex1 = new JTextField("net1");
				JLabel userLab2 = new JLabel("UserName");
				userTex4 = new JTextField("NetSdk");
				JLabel userLab3 = new JLabel("Password");
				userTex2 = new JTextField("NetSdk");
				JLabel userLab4 = new JLabel("Memo");
				userTex3 = new JTextField("NetSdk");
				JButton userBtn1 = new JButton("OK");
				JButton userBtn2 = new JButton("Cancle");
				
				addUserp1.setLayout(new GridLayout(4, 2));
				addUserp1.add(userLab1);
				addUserp1.add(userTex1);
				addUserp1.add(userLab2);
				addUserp1.add(userTex4);
				addUserp1.add(userLab3);
				addUserp1.add(userTex2);
				addUserp1.add(userLab4);
				addUserp1.add(userTex3);

				
				addUserp2.setLayout(new FlowLayout());
				addUserp2.add(userBtn1);
				addUserp2.add(userBtn2);
				
				add(addUserp1, BorderLayout.NORTH);
				add(addUserp2, BorderLayout.SOUTH);
			    
				userBtn1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						operateUserInfoNewPerformed3(3);
						dispose();
					}
				});
				userBtn2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
				
			}
		}
	}
	
	// 修改用户
	public class ModifyUserFrame extends Frame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ModifyUserFrame() {
			setTitle("修改用户");
			setSize(300, 400);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new GridLayout(2, 1));
			
			PrimiaryJPanel primiaryUserP = new PrimiaryJPanel();				
			NewJPanel newUserP = new NewJPanel();
								
			add(primiaryUserP);
			add(newUserP);	
			
			addWindowListener(new WindowAdapter()  {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});						
		}			
		
		public class PrimiaryJPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public PrimiaryJPanel(){
				setLayout(new GridLayout(3, 2, 1, 30));
				setBorderEx(this, "Primiary User info", 2);
				
				JLabel groLabel1 = new JLabel("Group Name");
				groTex11 = new JTextField("net1", 8);			
				JLabel groLabel2 = new JLabel("UserName");
				groTex21 = new JTextField("NetSdk", 8);	
				JLabel groLabel3 = new JLabel("Memo");
				groTex31 = new JTextField("NetSdk", 8);	

				add(groLabel1);
				add(groTex11);
				add(groLabel2);
				add(groTex21);
				add(groLabel3);
				add(groTex31);
			}
		}
		
		public class NewJPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public NewJPanel() {
				setLayout(new GridLayout(3, 2, 1, 30));
				setBorderEx(this, "New User info", 2);
				
				JLabel groLabel4 = new JLabel("UserName");
				groTex41 = new JTextField("NetSdk1", 8);
				JLabel groLabel5 = new JLabel("Memo");
				groTex51 = new JTextField("NetSdk1", 8);
				
				JButton groBtn11 = new JButton("OK");
				JButton groBtn21 = new JButton("Cancle");					

				add(groLabel4);
				add(groTex41);
				add(groLabel5);
				add(groTex51);;
				add(groBtn11);
			    add(groBtn21);
			    
			    groBtn11.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						operateUserInfoNewPerformed4(5);
						dispose();
					}
				});
			    groBtn21.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
			}
		}
	}
	
	// 修改密码
	public class ModifyPasswordFrame extends Frame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ModifyPasswordFrame() {
			setTitle("修改用户组");
			setSize(300, 300);
			setLocationRelativeTo(null);
			setVisible(true);
			setLayout(new BorderLayout());
			
			ModifyPasswordFrameJPanel primiarypasswordP = new ModifyPasswordFrameJPanel();				
								
			add(primiarypasswordP, BorderLayout.CENTER);				
			addWindowListener(new WindowAdapter()  {
				public void windowClosing(WindowEvent e){
					dispose();
				}
			});						
		}			
		
		public class ModifyPasswordFrameJPanel extends JPanel{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public ModifyPasswordFrameJPanel(){
				setLayout(new BorderLayout());
				setBorderEx(this, "修改密码", 2);
							
				JPanel pswjp1 = new JPanel();
				JPanel pswjp2 = new JPanel();
				
				JLabel groLabel1 = new JLabel("UserName");
				groTex12 = new JTextField("NetSdk", 8);
				JLabel groLabel2 = new JLabel("Old PSW");
				groTex22 = new JTextField("NetSdk", 8);
				JLabel groLabel3 = new JLabel("New PSW");
				groTex32 = new JTextField("NetSdk1", 8);
				JLabel groLabel4 = new JLabel("PSW Check");
				groTex42 = new JTextField(8);
				JButton groBtn12 = new JButton("OK");
				JButton groBtn22 = new JButton("Cancle");

				pswjp1.setLayout(new GridLayout(4, 2));
				pswjp1.add(groLabel1);
				pswjp1.add(groTex12);
				pswjp1.add(groLabel2);
				pswjp1.add(groTex22);
				pswjp1.add(groLabel3);
				pswjp1.add(groTex32);
				pswjp1.add(groLabel4);
				pswjp1.add(groTex42);
				
				pswjp2.setLayout(new FlowLayout());
				pswjp2.add(groBtn12);
				pswjp2.add(groBtn22);
				
				add(pswjp1, BorderLayout.NORTH);
				add(pswjp2, BorderLayout.SOUTH);
				
			    groBtn12.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						operateUserInfoNewPerformed41(6);
						dispose();
					}
				});
			    groBtn22.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
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
//			JOptionPane.showMessageDialog(this, "登录成功");
			logoutBtn.setEnabled(true);
			loginBtn.setEnabled(false);	
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
			}
		}
	}
	
	//用户查询事件
	private void queryUserInfo() {		
		m_hqueryUserInfoNew = NetSdk.CLIENT_QueryUserInfoNew(m_hLoginHandle, m_userInfo, null, 5000);
		
		if(m_hqueryUserInfoNew) {
			System.out.println("Query Succeed!" + "\n" +"m_userInfo.dwRightNum=" + m_userInfo.dwRightNum);
//			for(int i=0; i<m_userInfo.dwRightNum; i++) {
//				System.out.println(m_userInfo.rightList[i].dwID);
//			}
			
			for(int i = 0; i < m_userInfo.dwGroupNum; i++) {
				System.out.println(new String(m_userInfo.groupList[i].name).trim());
			}
		
		} else {
			System.err.printf("Query Failed!" + NetSdk.CLIENT_GetLastError());
		}		
	}
	
	private void queryUserInfoEx() {
		USER_MANAGE_INFO_EX info = new USER_MANAGE_INFO_EX();
		if(NetSdk.CLIENT_QueryUserInfoEx(m_hLoginHandle, info, 5000)) {
			System.out.println(info.dwGroupNum);
			for(int i = 0; i < info.dwGroupNum; i++) {
				System.out.println(new String(info.groupList[i].name).trim());
			}
		} else {
			System.err.printf("Query Failed!" + NetSdk.CLIENT_GetLastError());
		}	
	}
	
	//增加用户组 按钮事件设置
	private void operateUserInfoNewPerformed1(int nOperateType) {   
        m_groupInfo.dwRightNum = 20;//m_userInfo.dwRightNum; //权限数
//		m_groupInfo.dwID = m_userInfo.dwGroupNum + 1;   //用户组数
		System.arraycopy(groTex1.getText().getBytes(), 0, m_groupInfo.name, 0, groTex1.getText().getBytes().length);	
		System.arraycopy(groTex2.getText().getBytes(), 0, m_groupInfo.memo, 0, groTex2.getText().getBytes().length);
		
		for(int i=0; i<m_groupInfo.dwRightNum; i++) {
			m_groupInfo.rights[i] = i+1;
		}	
		
		m_groupInfo.write();		
    	m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
    							m_groupInfo.getPointer(), null, null, 5000);
    	
    	if(m_hoperateUserInfoNew) {
		    System.out.println("增加用户组成功" + new String(m_groupInfo.name));
		    
			data1[1][0] = groTex1.getText();
			data1[1][1] = groTex2.getText();
			groupMod.setDataVector(data1, name1); //不可少   
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}
    }
	//删除用户组 按钮事件设置
	private void operateUserInfoNewPerformed11(int nOperateType) {   
        m_groupInfo.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_groupInfo.dwID = m_userInfo.dwGroupNum + 1;   //用户组数
		
		System.arraycopy(cancelgroField.getText().getBytes(), 0, m_groupInfo.name, 0, cancelgroField.getText().getBytes().length);
		for(int i=0; i<m_groupInfo.dwRightNum; i++) {
			m_groupInfo.rights[i] = i+1;
		}	
		
		m_groupInfo.write();		
    	m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
    							m_groupInfo.getPointer(), null, null, 5000);
    	
    	if(m_hoperateUserInfoNew) {
    		System.out.println("删除用户组成功" + new String(m_groupInfo.name));
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}
    }
	
	//修改用户组 按钮事件
	private void operateUserInfoNewPerformed2(int nOperateType) {   
		NetSDKLib.USER_GROUP_INFO_NEW m_groupInfoEx = new NetSDKLib.USER_GROUP_INFO_NEW();
		// 新数据
        m_groupInfo.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_groupInfo.dwID = m_userInfo.dwGroupNum + 1;   //用户组数
		System.arraycopy(groTex33.getText().getBytes(), 0, m_groupInfo.name, 0, groTex33.getText().getBytes().length);
		System.arraycopy(groTex43.getText().getBytes(), 0, m_groupInfo.memo, 0, groTex43.getText().getBytes().length);
		for(int i=0; i<m_groupInfo.dwRightNum; i++) {
			m_groupInfo.rights[i] = i+1;
		}
		
		// 原始数据
		m_groupInfoEx.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_groupInfoEx.dwID = m_userInfo.dwGroupNum + 1;   //用户组数
		System.arraycopy(groTex13.getText().getBytes(), 0, m_groupInfoEx.name, 0, groTex13.getText().getBytes().length);
		System.arraycopy(groTex23.getText().getBytes(), 0, m_groupInfoEx.memo, 0, groTex23.getText().getBytes().length);
		for(int i=0; i<m_groupInfoEx.dwRightNum; i++) {
			m_groupInfoEx.rights[i] = i+1;
		}
		
		m_groupInfo.write();		
		m_groupInfoEx.write();
		
    	m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
    			m_groupInfo.getPointer(), m_groupInfoEx.getPointer(), null, 5000);
    	
    	if(m_hoperateUserInfoNew) {		
    		    System.out.println("修改用户组成功" + new String(m_groupInfo.name));
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}
    }
	
	//增加用户 按钮事件
	private void operateUserInfoNewPerformed3(int nOperateType) {
		m_userInfoNew.dwRightNum = 20;//m_userInfo.dwRightNum; //权限数
		m_userInfoNew.dwID = m_userInfo.dwUserNum +1;   //用户ID
		m_userInfoNew.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID
		
		System.arraycopy(userTex1.getText().getBytes(), 0, m_groupInfo.name, 0, userTex1.getText().getBytes().length);	
		System.arraycopy(userTex4.getText().getBytes(), 0, m_userInfoNew.name, 0, userTex4.getText().getBytes().length);
		System.arraycopy(userTex2.getText().getBytes(), 0, m_userInfoNew.passWord, 0, userTex2.getText().getBytes().length);
		System.arraycopy(userTex3.getText().getBytes(), 0, m_userInfoNew.memo, 0, userTex3.getText().getBytes().length);
		
		for(int i=0; i<m_userInfoNew.dwRightNum; i++) {
			m_userInfoNew.rights[i] = i+1;
		}
		m_userInfoNew.dwFouctionMask = 0x00000001;;
		m_userInfoNew.byIsAnonymous = 0;

		m_userInfoNew.write();
		
		m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
				m_userInfoNew.getPointer(), null, null, 5000);
    	
    	if(m_hoperateUserInfoNew) {		
    		 System.out.println("增加用户成功" + new String(m_userInfoNew.name));
 			data2[1][0] = userTex1.getText();
 			data2[1][1] = userTex4.getText();
 			data2[1][2] = userTex2.getText();
 			data2[1][3] = userTex3.getText();
 			userMod.setDataVector(data2, name2); //不可少   
    	}else {
    		 System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}	
	}
	
	// 删除用户 按钮事件
	private void operateUserInfoNewPerformed31(int nOperateType) {
		m_userInfoNew.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_userInfoNew.dwID = m_userInfo.dwUserNum +1;   //用户ID
		m_userInfoNew.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID
		
		System.arraycopy(cancelgroField.getText().getBytes(), 0, m_groupInfo.name, 0, cancelgroField.getText().getBytes().length);	
		System.arraycopy(canceluserField.getText().getBytes(), 0, m_userInfoNew.name, 0, canceluserField.getText().getBytes().length);
		
		for(int i=0; i<m_userInfoNew.dwRightNum; i++) {
			m_userInfoNew.rights[i] = i+1;
		}
		m_userInfoNew.dwFouctionMask = 0x00000001;;
		m_userInfoNew.byIsAnonymous = 0;

		m_userInfoNew.write();
		
		m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
				m_userInfoNew.getPointer(), null, null, 5000);
    	
    	if(m_hoperateUserInfoNew) {		
    		System.out.println("删除用户成功" + new String(m_userInfoNew.name));
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}	
	}

	//修改用户按钮事件
	private void operateUserInfoNewPerformed4(int nOperateType) {
		m_userInfoNew.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_userInfoNew.dwID = m_userInfo.dwUserNum ;   //用户ID
		m_userInfoNew.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID
		System.arraycopy(groTex41.getText().getBytes(), 0, m_userInfoNew.name, 0, groTex41.getText().getBytes().length);
		System.arraycopy(groTex51.getText().getBytes(), 0, m_userInfoNew.memo, 0, groTex51.getText().getBytes().length);

		for(int i=0; i<m_userInfoNew.dwRightNum; i++) {
			m_userInfoNew.rights[i] = i+1;
		}
		m_userInfoNew.dwFouctionMask = 0x00000001;
		m_userInfoNew.byIsAnonymous = 0;

		//原始
		NetSDKLib.USER_INFO_NEW m_userInfoNewEx = new NetSDKLib.USER_INFO_NEW();
		m_userInfoNewEx.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_userInfoNewEx.dwID = m_userInfo.dwUserNum ;   //用户ID
		m_userInfoNewEx.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID
		System.arraycopy(groTex11.getText().getBytes(), 0, m_groupInfo.name, 0, groTex11.getText().getBytes().length);	
		System.arraycopy(groTex21.getText().getBytes(), 0, m_userInfoNewEx.name, 0, groTex21.getText().getBytes().length);
		System.arraycopy(groTex31.getText().getBytes(), 0, m_userInfoNewEx.memo, 0, groTex31.getText().getBytes().length);

		for(int i=0; i<m_userInfoNewEx.dwRightNum; i++) {
			m_userInfoNewEx.rights[i] = i+1;
		}
		m_userInfoNewEx.dwFouctionMask = 0x00000001;
		m_userInfoNewEx.byIsAnonymous = 0;
		m_userInfoNew.write();
		m_userInfoNewEx.write();
		
		m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
				m_userInfoNew.getPointer(), m_userInfoNewEx.getPointer(), null, 5000);
    	
    	if(m_hoperateUserInfoNew) {		
    		    System.out.println("修改用户成功" + new String(m_userInfoNew.name));
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}		
	}
	
	//修改密码 按钮事件
	private void operateUserInfoNewPerformed41(int nOperateType) {
		m_userInfoNew.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_userInfoNew.dwID = m_userInfo.dwUserNum ;   //用户ID
		m_userInfoNew.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID

		System.arraycopy(groTex32.getText().getBytes(), 0, m_userInfoNew.passWord, 0, groTex32.getText().getBytes().length);

		for(int i=0; i<m_userInfoNew.dwRightNum; i++) {
			m_userInfoNew.rights[i] = i+1;
		}
		m_userInfoNew.dwFouctionMask = 0x00000001;
		m_userInfoNew.byIsAnonymous = 0;

		//原始
		NetSDKLib.USER_INFO_NEW m_userInfoNewEx = new NetSDKLib.USER_INFO_NEW();
		m_userInfoNewEx.dwRightNum = 20; //m_userInfo.dwRightNum; //权限数
		m_userInfoNewEx.dwID = m_userInfo.dwUserNum ;   //用户ID
		m_userInfoNewEx.dwGroupID = m_userInfo.dwGroupNum + 1; //用户组ID	
		System.arraycopy(groTex12.getText().getBytes(), 0, m_userInfoNewEx.name, 0, groTex12.getText().getBytes().length);
		System.arraycopy(groTex22.getText().getBytes(), 0, m_userInfoNewEx.passWord, 0, groTex22.getText().getBytes().length);

		for(int i=0; i<m_userInfoNewEx.dwRightNum; i++) {
			m_userInfoNewEx.rights[i] = i+1;
		}
		m_userInfoNewEx.dwFouctionMask = 0x00000001;
		m_userInfoNewEx.byIsAnonymous = 0;
		m_userInfoNew.write();
		m_userInfoNewEx.write();
		
		m_hoperateUserInfoNew = NetSdk.CLIENT_OperateUserInfoNew(m_hLoginHandle, nOperateType, 
				m_userInfoNew.getPointer(), m_userInfoNewEx.getPointer(), null, 5000);
    	
    	if(m_hoperateUserInfoNew) {		  
    		System.out.println("修改密码成功！" + new String(m_userInfoNew.passWord));   	
    	}else {
    		System.err.printf("Failed!" + String.format("0x%x", NetSdk.CLIENT_GetLastError()));
    	}		
	}
	/////////////////////组件//////////////////////////
	//////////////////////////////////////////////////
	//带背景的面板组件
		
	//登录组件
	private LoginPanel loginPanel;
	
	private OperatePanel operatePanel;
	
	//实时预览
	private TablePanel tablePanel;
	
	private JButton loginBtn;
	private JButton logoutBtn;
	private JButton operateBtn0;
	private JButton operateBtn1;
	private JButton operateBtn2;
	private JButton operateBtn3;
	private JButton operateBtn4;
	private JButton operateBtn5;
	private JButton operateBtn6;
	private JButton operateBtn7;
	
	private JTextField groTex1;
	private JTextField groTex2;
	private Object[][] data1;
	private Object[][] data2;
	private DefaultTableModel groupMod;
	private DefaultTableModel userMod;
	private JTable groupTab;
	private JTable userTab;
	private JTextField cancelgroField;
	private JTextField canceluserField;

	private JLabel nameLabel;
	private JLabel passwordLabel;
	private JLabel ipLabel;
	private JLabel portLabel;
	
	private JTextField groTex13;
	private JTextField groTex23;
	private JTextField groTex33;
	private JTextField groTex43;
	
	private JTextField userTex1;
	private JTextField userTex4;
	private JTextField userTex2;
	private JTextField userTex3;
	
	private JTextField groTex11;			
	private JTextField groTex21;	
	private JTextField groTex31 ;	
	private JTextField groTex41;
	private JTextField groTex51;

	private JTextField groTex12;
	private JTextField groTex22;
	private JTextField groTex32;
	private JTextField groTex42;
	
	private JTextField ipTextArea;
	private JTextField portTextArea;
	private JTextField nameTextArea;
	private JPasswordField passwordTextArea;	
	
}

public class UserManagement{
	public static void main(String[] args) {	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UserManagementFrame demo = new UserManagementFrame();
				demo.setVisible(true);
			}
		});		
	}
}

