package com.netsdk.demo.customize;

import com.netsdk.demo.util.Sdk;
import com.netsdk.lib.LastError;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.fServiceCallBack;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AutoRegisterFrame extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Sdk sdk = Sdk.environment();
	
	private static List<Device> deviceList = new ArrayList<Device>();
	private static Map<String, DeviceLoginInfo> deviceMap = new HashMap<String, DeviceLoginInfo>();
	private AutoRegisterObserver observer;
	//private String currentNode;

	private DefaultMutableTreeNode root;
	private DefaultTreeModel treeModel;
    private JTree tree;
    private ListenPanel listenPanel; 
    private JScrollPane treePanel;
    private JPopupMenu popupMenu;
    Device curretnDevice = null;
	
	public AutoRegisterFrame() throws UnknownHostException{
		
		CreateMainFrame();
	}
	
	public void start()
	{
		sdk.init(); //函数本身没有做异常处理
		sdk.setLogPath(".");
		sdk.openLog();
		
		loadDeviceInfo();
		
		observer = new AutoRegisterObserver();
		AutoRegisterSubject.getInstance().addObserver(observer);
	}
	
	private void loadDeviceInfo() {
		deviceMap.clear();
		DataOperate dp = new ExcelDate();
		dp.open("./device.xlsx");
		dp.read();
//		System.out.println(System.getProperty("user.dir"));
//		dp.print();
		dp.close();
	}
	
	public void clean()
	{
		for (Device device : deviceList)
			device.logout();
			
		if (listenPanel !=null)
			listenPanel.service.stop();
		
		sdk.cleanup();
	}
	
	//////////////////////////界面//////////////////////////////////////
	public void CreateMainFrame() throws UnknownHostException{
    	
    	root = new DefaultMutableTreeNode("设备列表");
    	
    	treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        add(tree);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treePanel = new JScrollPane();
        treePanel.getViewport().add(tree);
        this.getContentPane().add(treePanel,BorderLayout.CENTER);

        listenPanel = new ListenPanel();
        this.getContentPane().add(listenPanel,BorderLayout.NORTH);
       
        popupMenu = new JPopupMenu();
        
        popupMenu.add(new AbstractAction("登陆") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				
				loadDeviceInfo();
	
				DeviceLoginInfo deviceLoginInfo = deviceMap.get(curretnDevice.ip);
				if (deviceLoginInfo != null) {
					curretnDevice.setLoginInfo(deviceLoginInfo.username, deviceLoginInfo.password);
					curretnDevice.login();
				}
				else {
					curretnDevice.setStatus(Device.DeviceStatus.DEVICE_NOTFIND);
				}
				treeModel.reload();
			}
		});
           
        addEvent();
        
        setSize(new Dimension(450,700));
        setTitle("主动注册");
        setVisible(true);
    }

	//事件处理
	private void addEvent(){
		
		// 登录按钮. 监听事件
		listenPanel.listenBtn.addActionListener(new ActionListener() {		
			public void actionPerformed(ActionEvent e) {
				listenPanel.listenButtonPerformed();
				//setTreeNode();  // test
			}
		});
		
		// 停止侦听事件
		listenPanel.stopListenBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listenPanel.stopListenButtonPerformed();
			}
		});
		
		//未登录设备鼠标右键事件
        tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
				Object object = null;
				DefaultMutableTreeNode treeNode = null;
				curretnDevice = null;
				if (treePath != null) {
					object = treePath.getLastPathComponent();
					if (object instanceof DefaultMutableTreeNode) {
						treeNode = (DefaultMutableTreeNode)object;
						object = treeNode.getUserObject();
						if (object instanceof Device) {
							curretnDevice = (Device)object;
						}
					}
				}
				 
				if (e.isMetaDown() && curretnDevice != null && !curretnDevice.IsLogined()) {
					popupMenu.show(tree, e.getX(), e.getY());
				}
			}
		});
        
     	addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("主动登陆窗口关闭");			
				clean();
				System.exit(0);
			}
		});
    }
	
	/**
	 * 监听面板
	 */
	private class ListenPanel extends JPanel {
		/**
		 * 
		 */
		
		private  AutoRegisterService service;
		private static final long serialVersionUID = 1L;
		
		private JButton listenBtn;
		private JButton stopListenBtn;
		private JLabel ipLabel;
		private JLabel portLabel;
		private JTextField ipTextArea;
		private JTextField portTextArea;

		public ListenPanel() throws UnknownHostException {
			setLayout(new FlowLayout());
			setBorderEx(this, "侦听", 2);
			
			InetAddress address = InetAddress.getLocalHost();
			String ip = address.getHostAddress();
			int nPort = 1001;
			service = new AutoRegisterService(ip, nPort);
			
			listenBtn = new JButton("启动侦听");
			stopListenBtn = new JButton("停止侦听");
			ipLabel = new JLabel("IP地址");
			portLabel = new JLabel("端口号");
			
			ipTextArea = new JTextField(ip, 8);
			portTextArea = new JTextField(new Integer(nPort).toString(), 4);
			
			add(ipLabel);
			add(ipTextArea);
			add(portLabel);
			add(portTextArea);		
			add(listenBtn);
			add(stopListenBtn);	
			
			ipTextArea.setEditable(false);
			stopListenBtn.setEnabled(false);
		}
		
		public void listenButtonPerformed() {
			String ip = ipTextArea.getText();
			int nPort = Integer.parseInt(portTextArea.getText());
			
			service.setIpAndPort(ip, nPort);
			service.start();
			if (service.isListening()){
				//JOptionPane.showMessageDialog(listenPanel, "侦听成功");
				stopListenBtn.setEnabled(true);
				listenBtn.setEnabled(false);
			}
			else {
				JOptionPane.showMessageDialog(listenPanel, "启动侦听失败");
			}
		}
		
		public void stopListenButtonPerformed() {
			service.stop();
			if (!service.isListening()){
				//JOptionPane.showMessageDialog(listenPanel, "停止侦听成功");
				listenBtn.setEnabled(true);
				stopListenBtn.setEnabled(false);
				
			}
			else {
				JOptionPane.showMessageDialog(listenPanel, "停止侦听失败");
			}
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

	/////////////////////////////// 主动注册业务处理 /////////////////////
	@SuppressWarnings("static-access")
	private class AutoRegisterService {
		
		private boolean listenSuccess;
		private LLong handle;
		private String ip;
		private int port;
		
		public AutoRegisterService(String ip, int port) {
			this.ip = ip;
			this.port = port;
			
			handle = null;
			listenSuccess = false;
		}
		
		public boolean isListening()
		{
			return listenSuccess;
		}
		
		public void setIpAndPort(String ip, int port)
		{
			this.ip = ip;
			this.port = port;
		}
		
		public void start() {
			
			if (listenSuccess)
				return;
			
			handle = sdk.netsdkApi.CLIENT_ListenServer(ip, port, 1000, AutoRegisterSubject.getInstance(), null);
			if (0 == handle.longValue()) {
				System.err.println("Failed to start server. " + String.format("0x%x", sdk.netsdkApi.CLIENT_GetLastError()));
			}
			else {
				listenSuccess = true;
				
			}
		}
		
		public void stop() {
			
			if (listenSuccess)
				if (sdk.netsdkApi.CLIENT_StopListenServer(handle)){
					listenSuccess = false;
				}
				else {
					System.err.println("Failed to stop server. " + String.format("0x%x", sdk.netsdkApi.CLIENT_GetLastError()));
				}
		}
		
	}
	
	/* 观察者模式 */
	private static class AutoRegisterSubject extends  Observable implements fServiceCallBack {
		
		private AutoRegisterSubject() {
		}
		private static AutoRegisterSubject autoRegisterSubject = new AutoRegisterSubject();
		
		public static AutoRegisterSubject getInstance() {
			return autoRegisterSubject;
		} 
		
		@Override
		public int invoke(LLong lHandle, final String pIp, final int wPort,
				int lCommand, Pointer pParam, int dwParamLen,
				Pointer dwUserData) {
			
			
				// 将 pParam 转化为序列号
				byte[] buf = new byte[dwParamLen];
				pParam.read(0, buf, 0, dwParamLen);
				String serial = new String(buf).trim();
				
				setChanged();
				notifyObservers(new Device(pIp, wPort, serial));
			
			return 0;
		}
	}
	
	private class AutoRegisterObserver implements Observer {
		
		@Override
		public void update(Observable o, Object arg)
		{
			new Thread(new DataManageRunnable(arg)).start();
		}
	}
	
	private class DataManageRunnable implements Runnable{
		
		private Device device;
		
		public DataManageRunnable(Object obj) {
			device = (Device)obj;
		}
		
		public void run() {
			
			DeviceLoginInfo deviceLoginInfo = deviceMap.get(device.ip);
			
			if (deviceLoginInfo != null) {
				device.setLoginInfo(deviceLoginInfo.username, deviceLoginInfo.password);
				device.login();
			}
			else {
				device.setStatus(Device.DeviceStatus.DEVICE_NOTFIND);
			}
			
			synchronized (deviceList) {
				deviceList.add(device);
			}
			synchronized (root) {
				addDeviceNode(device);
			}
			treeModel.reload();
		}
	}

	@SuppressWarnings( value = {"static-access", "unused"})
	private static class Device {
		
		final static int tcpSpecCap = 2;	// 主动注册方式
		
		public LLong handle; 	// 设备句柄, 标识唯一的设备
		//private boolean loginSuccess; // 登录状态
		private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo; //设备信息
		private boolean pwdPlaintext;
		
		private String ip; 			// 设备地址
		private int port; 			// 设备端口
		private String username; 	// 用户名
		private String password; 	// 密码
		private String serial;		// 设备序列号
		
		enum DeviceStatus {DEVICE_INIT,DEVICE_NOTFIND,DEVICE_LOGINFAIL,DEVICE_LOGINED, DEVICE_LOGOUT};
		private DeviceStatus status;
		
		public Device(String ip, int port, String serial) {
			this(ip, port, "", "", serial);
		}
		
		public Device(String ip, int port, String username,
				String password, String serial) {
			
			this.ip = ip;
			this.port = port;
			this.username = username;
			this.password = password;
			
			this.serial = serial;
			
			deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
			
			handle = null;
			//loginSuccess = false;
			pwdPlaintext = false;
			status = DeviceStatus.DEVICE_INIT;
		}
		
		public void setLoginInfo(String username, String password){
			this.username = username;
			this.password = password;
		}
		
		public void setStatus(DeviceStatus status){
			this.status = status;
		}

		@Override
		public String toString() {
			return ip;
		}
		
		public boolean IsLogined()
		{
			return status == DeviceStatus.DEVICE_LOGINED;
		}
		
		public void login() {
			
			if (status == DeviceStatus.DEVICE_LOGINED)
				return;
			
			IntByReference nError = new IntByReference(0);
			// 将 序列号 转化为 pointer 类型
			com.netsdk.lib.NativeString sdkSerial = new com.netsdk.lib.NativeString(serial);
			handle = sdk.netsdkApi.CLIENT_LoginEx2(ip, port, username, password, tcpSpecCap, sdkSerial.getPointer(), deviceinfo, nError);
	        if(handle.longValue() == 0)
	        {
	        	status = DeviceStatus.DEVICE_LOGINFAIL;
	            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%x]\n" , ip , port , sdk.netsdkApi.CLIENT_GetLastError());
	        }
	        else
	        {
	        	//System.out.println("Login Success [ " + ip +" ]");
	        	status = DeviceStatus.DEVICE_LOGINED;
	        }
		}
		
		public void logout() 
		{
			if	(status == DeviceStatus.DEVICE_LOGINED) {
				if (sdk.netsdkApi.CLIENT_Logout(handle)) {
					status = DeviceStatus.DEVICE_LOGOUT;
				}
				else {
					System.err.printf("Loginout Device[%s] Port[%d]Failed. Last Error[%x]\n" , ip , port , sdk.netsdkApi.CLIENT_GetLastError());
				}
			}
		}
		
		private String GetloginErrorMsg() {
			int err = sdk.netsdkApi.CLIENT_GetLastError();
			String msg = "";
			switch(err) {
				case LastError.NET_USER_FLASEPWD_TRYTIME: 
					msg = "输入密码错误超过限制次数"; break;
				case LastError.NET_LOGIN_ERROR_PASSWORD:  
					msg = "密码不正确"; break;
				case LastError.NET_LOGIN_ERROR_USER: 
					msg = "帐户不存在";break;
				case LastError.NET_LOGIN_ERROR_TIMEOUT: 
					msg = "等待登录返回超时";break;
				case LastError.NET_LOGIN_ERROR_RELOGGIN: 
					msg = "帐号已登录";break;
				case LastError.NET_LOGIN_ERROR_LOCKED:
					msg = "帐号已被锁定";break;
				case LastError.NET_LOGIN_ERROR_BLACKLIST:
					msg = "帐号已被列为禁止名单";break;
				case LastError.NET_LOGIN_ERROR_BUSY:
					msg = "资源不足,系统忙";break;
				case LastError.NET_LOGIN_ERROR_CONNECT:
					msg = "登录设备超时,请检查网络并重试";break;
				case LastError.NET_LOGIN_ERROR_NETWORK: 
					msg = "网络连接失败";break;
				default:
				    msg = "请参考 NetSDKLib.java ";
			}
			
			return msg;
		}
		
		private class Port{
			@Override
			public String toString() {
				
				return "[端口]" + port;
			}
		}
		
		private class Serial{
			
			@Override
			public String toString() {
				
				return "[设备序列号]" + serial;
			}
		}
		
		private class UserName{
					
			@Override
			public String toString() {	
				return "[用户名]" + username;
			}
		}

		private class PassWord{
			
			@Override
			public String toString() {
				if (!pwdPlaintext) {
					return "[密码]" + "******";
				}
				return "[密码]" + password;
			}
		}
		
		private class LoginState{
			
			@Override
			public String toString() {
				String deviceStatus = "设备状态未知";
				switch (status) {
				case DEVICE_INIT:
				case DEVICE_LOGOUT:
					deviceStatus = "设备未登陆";
					break;
				case DEVICE_LOGINFAIL:
					deviceStatus = "[" + GetloginErrorMsg() + "]";
					break;
				case DEVICE_LOGINED:
					deviceStatus = "设备已登陆";
					break;
				case DEVICE_NOTFIND:
					deviceStatus = "设备不匹配";
					break;
				default:
					break;
				}
				
				return deviceStatus;
			}
		}
	}
	
	private void addDeviceNode(Device device) {
		DefaultMutableTreeNode deviceNode = new DefaultMutableTreeNode(device);
		deviceNode.add(new DefaultMutableTreeNode(device.new Port()));
		deviceNode.add(new DefaultMutableTreeNode(device.new Serial()));
		deviceNode.add(new DefaultMutableTreeNode(device.new LoginState()));
		//deviceNode.add(new DefaultMutableTreeNode(device.new PassWord()));
		root.add(deviceNode);
	}
	
	///////////////////////////// 加载数据 //////////////////////////////////////
	/*数据保存在DeviceLoginInfo对象中*/
	private static class DeviceLoginInfo {
		
		public String ip; 			// 设备地址
		public int port; 			// 设备端口
		public String username; 	// 用户名
		public String password; 	// 密码
		
		public DeviceLoginInfo(String ip, int port, String username, String password) {
			
			this.ip = ip;
			this.port = port;
			this.username = username;
			this.password = password;
		}
		
		public String toString() {
			return "ip:" + ip + " port:" + port + " username:" + username + " password:" + password;
		}
	}
	
	private interface DataOperate {
		public boolean open(String filename);
		public boolean read();
		public boolean write();
		public boolean close();
		public void print();
	}
	
	private class ExcelDate implements DataOperate{ // 可使用Apache POI库来简化代码
		ZipFile xlsxFile;
		DocumentBuilderFactory dbf;
		//Vector<String> sharedStrings;
		String sharedStrings[];
		public boolean open(String filename)
		{
			try {
				xlsxFile = new ZipFile(new File(filename));
				dbf = DocumentBuilderFactory.newInstance();
			} catch (Exception e) {
	            e.printStackTrace();
	            return false;
	        }
			return true;
		}
		
		public boolean read(){
			
			getSharedStrings();
			try {
				
				ZipEntry workbookXML = xlsxFile.getEntry("xl/workbook.xml");
	            InputStream workbookXMLIS = xlsxFile.getInputStream(workbookXML);
	            Document doc = dbf.newDocumentBuilder().parse(workbookXMLIS);
	     
	            NodeList sheetList = doc.getElementsByTagName("sheet");

	            for (int i = 0; i < sheetList.getLength(); i++) {
	                Element element = (Element) sheetList.item(i);
	                //System.out.println(element.getAttribute("name"));// 输出sheet节点的name属性的值
	                ZipEntry sheetXML = xlsxFile.getEntry("xl/worksheets/"
	                					+ element.getAttribute("name").toLowerCase() + ".xml");
	                InputStream sheetXMLIS = xlsxFile.getInputStream(sheetXML);
	                Document sheetdoc = dbf.newDocumentBuilder().parse(sheetXMLIS);
	                NodeList rowdata = sheetdoc.getElementsByTagName("row"); // 得到每个行
	                for (int j = 1; j < rowdata.getLength(); j++) { //跳过第一行
	                    
	                	String ip = null, port = null, username = null, password = null;
	                    Element row = (Element) rowdata.item(j);
	                    
	                    NodeList columndata = row.getElementsByTagName("c"); // 根据行得到每个行中的列
	                    for (int k = 0; k < columndata.getLength(); k++) {  
	                        Element column = (Element) columndata.item(k);
	                        NodeList values = column.getElementsByTagName("v");
	                        Element value = (Element) values.item(0);
	                        String rowName = column.getAttribute("r");
	                        String temp;
	                        if (column.getAttribute("t") != null
	                                & column.getAttribute("t").equals("s")) {
	                            // 如果是共享字符串则在sharedstring.xml里查找该列的值
	                        	temp = sharedStrings[Integer.parseInt(value.getTextContent())];
	                        } else {
	                            if (value != null) {
	                                temp = value.getTextContent();
	                            }else {
	                                System.out.println("i : " + i + "   j : " + j + "  null");
	                                return false;
	                            }
	                        }
	                        
	                    	if (rowName != null) {
                        		if (rowName.contains("A")) {
                        			ip = temp;
								}else if (rowName.contains("B")) {
									port = temp;
								}else if (rowName.contains("C")) {
									username = temp;
								}else if (rowName.contains("D")) {
									password = temp;
								}
							}
                        	else {
								return false;
							}
	                    }
	                    
	                    deviceMap.put(ip, new DeviceLoginInfo(ip, Integer.parseInt(port), username, password));
	                }
	            }
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		
		}
		
		private boolean getSharedStrings()
		{
			// 读取sharedStrings.xml
			try {
				
				ZipEntry sharedStringXML = xlsxFile.getEntry("xl/sharedStrings.xml");
	            InputStream sharedStringXMLIS = xlsxFile.getInputStream(sharedStringXML);
	            Document document = dbf.newDocumentBuilder().parse(sharedStringXMLIS);
	            NodeList str = document.getElementsByTagName("t");
	            sharedStrings = new String[str.getLength()];
	            for (int i = 0; i < str.getLength(); i++) {
	                Element element = (Element) str.item(i); 
	                //System.out.println(element.getTextContent());
	                sharedStrings[i] = element.getTextContent();
	            }
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
            
			return true;
		}
		
		public boolean write(){ return true;}
		public boolean close(){
			try {
				xlsxFile.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		public void print() {
			if(xlsxFile == null) {
				System.err.println("xlsxFile is null!");
			} else {
				System.out.println("xlsxFile is not null!");
			}
			
			for (DeviceLoginInfo deviceLoginInfo : deviceMap.values())
				System.out.println(deviceLoginInfo);
		}
	}
	
	@SuppressWarnings("unused")
	private class XMLDate implements DataOperate{
		Document document;
		public boolean open(String filename){
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
				document = documentBuilder.parse(new FileInputStream(filename));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		public boolean read(){
			try {
				
				NodeList deviceNodeList = document.getElementsByTagName("device");
				
				for (int i = 0; i < deviceNodeList.getLength(); i++)
				{
					Node node = deviceNodeList.item(i);
					NodeList childNodeList =  node.getChildNodes();
					
					String ip = null, port = null, username = null, password = null;
					ip = ((Element)node).getAttribute("ip");
					
					for (int j = 0; j < childNodeList.getLength(); j++) {
						Node childNode = childNodeList.item(j);
						
						if (childNode.getNodeName().equals("port")) {
							port = childNode.getTextContent();
						}
						else if (childNode.getNodeName().equals("username")) {
							username = childNode.getTextContent();
						}
						else if (childNode.getNodeName().equals("password")) {
							password = childNode.getTextContent();
						}
					}
					deviceMap.put(ip, new DeviceLoginInfo(ip, Integer.parseInt(port), username, password));
				}
				
			
				
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		public boolean write(){ return true;}
		public boolean close(){ return true;}
		
		public void print() {
			for (DeviceLoginInfo deviceLoginInfo : deviceMap.values())
				System.out.println(deviceLoginInfo);
		}
	}
	
	public static void main(String []args) throws UnknownHostException
	{
		AutoRegisterFrame frame = new AutoRegisterFrame();
		frame.start();
	}
	
}
