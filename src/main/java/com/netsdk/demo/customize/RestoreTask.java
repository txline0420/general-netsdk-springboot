package com.netsdk.demo.customize;

import com.alibaba.fastjson.JSONObject;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.ENUM_RECORDBACKUP_FILE_TYPE;
import com.netsdk.lib.structure.NET_TIME;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Scanner;

import static java.lang.System.exit;

public class RestoreTask {

    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 登陆句柄
    private LLong m_hLoginHandle = new LLong(0);

    // 开始录像备份恢复句柄
    private LLong m_hRestoreID = new LLong(0);

    // 任务ID
    private final int[] taskID = new int[1024];

    // 设备信息扩展
    private final NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();

    // 跨平台编码
    private static final Charset sdkEncode = Charset.forName(Utils.getPlatformEncode());

    // 设备初始化
    private void Init() {
        // 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);
        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);
        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)) {
            System.err.println("Open SDK Log Failed!!!");
        }
    }

    // 登陆设备
    public void Login() {
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        m_hLoginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, null, deviceInfo, nError);
        if (m_hLoginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        } else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
    }

    // 登出设备
    public void LoginOut() {
        System.out.println("End Test");
        if (m_hLoginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(m_hLoginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        exit(0);
    }

    /**
     * 设备断线回调
     */
    private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

        private DisConnectCallBack() {
        }

        private static class CallBackHolder {
            private static final DisConnectCallBack instance = new DisConnectCallBack();
        }

        public static DisConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
        }
    }

    /**
     * 设备重连回调
     */
    private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
        private HaveReConnectCallBack() {
        }

        private static class CallBackHolder {
            private static final HaveReConnectCallBack instance = new HaveReConnectCallBack();
        }

        public static HaveReConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }

    }

    /**
     * 开始录像备份恢复
     */
    public void StartRecordBackupRestore() {
        m_hRestoreID = netSdk.CLIENT_StartRecordBackupRestore(m_hLoginHandle);
        if (m_hRestoreID.longValue() != 0) {
            System.out.println("StartRecordBackupRestore success");
        } else {
            System.err.printf("StartRecordBackupRestore false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    /**
     * 停止录像备份恢复
     */
    public void StopRecordBackupRestore() {
        netSdk.CLIENT_StopRecordBackupRestore(m_hRestoreID);
    }

    public static class Channels extends SdkStructure {
        public int[] channels;
    }

    /**
     * 添加录像备份恢复任务
     */
    public void AddRecordBackupRestoreTask() throws UnsupportedEncodingException {
        // 入参
        NET_IN_ADD_REC_BAK_RST_TASK pInParam = new NET_IN_ADD_REC_BAK_RST_TASK();
        String DeviceID = "172.12.3.20:37777:admin:admin456:12";
        pInParam.pszDeviceID = new NativeString(DeviceID).getPointer();
        pInParam.pnChannels = new Memory(4);
        pInParam.nChannelCount = 1;
        Channels nc = new Channels();
        nc.channels = new int[pInParam.nChannelCount];
        nc.channels[0] = 30;
        ToolKits.SetStructDataToPointer(nc, pInParam.pnChannels, 0);
        pInParam.stuStartTime.setTime(2020, 3, 26, 16, 16, 16);
        pInParam.stuEndTime.setTime(2020, 3, 26, 18, 18, 18);
        boolean ret = netSdk.CLIENT_AddRecordBackupRestoreTask(m_hRestoreID, pInParam, 3000);

        if (ret) {
            System.out.println("AddRecordBackupRestoreTask success");
        } else {
            System.err.printf("AddRecordBackupRestoreTask false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    /**
     * 添加远程录像备份任务 21.05.27 新增
     * 现在只支持 主码流 录像备份
     * 其他类型 Task 不会建立成功
     * <p>
     * Demo 这里采用以下用例：
     * 只发起一个任务：
     * 1) 任务一:
     * 目标设备地址: 172.23.12.138,37777,admin,admin123
     * 通道(存储设备-IPC) 0-0,
     * 时间:2021-05-28 12:05:00 至 2021-05-28 12:10:00
     * 类型与码流: 普通录像 主码流
     */
    public void AddRemoteRecordBackupRestoreTask() {

        NET_IN_ADD_REC_BAK_RST_REMOTE_TASK stuIn = new NET_IN_ADD_REC_BAK_RST_REMOTE_TASK();
        stuIn.nTaskCount = 1;  // 1 个任务

        // 初始化任务数组
        NET_RECORDBACKUP_REMOTE_TASK[] remoteTasks = new NET_RECORDBACKUP_REMOTE_TASK[stuIn.nTaskCount];
        for (int i = 0; i < remoteTasks.length; i++) {
            remoteTasks[i] = new NET_RECORDBACKUP_REMOTE_TASK();
        }

        ////////// 任务一 //////////
        ///////////////////////////

        // 需要备份录像的目标设备的登录参数
        String deviceIP = "172.23.12.138"; // IP
        System.arraycopy(deviceIP.getBytes(), 0, remoteTasks[0].szDeviceIP, 0, deviceIP.length());
        remoteTasks[0].nPort = 37777;      // port
        String username = "admin";         // username
        System.arraycopy(username.getBytes(), 0, remoteTasks[0].szUserName, 0, username.length());
        String password = "admin123";      // password
        System.arraycopy(password.getBytes(), 0, remoteTasks[0].szPassword, 0, password.length());

        // 指定目标设备的备份通道 (存储设备)1通道 对应的 (IPC)0通道
        remoteTasks[0].nChannelCount = 1;
        // 通道组1: (存储设备)0通道 对应的 (IPC)0通道
        remoteTasks[0].nChannels[0] = 0;
        remoteTasks[0].nRemoteChannels[0] = 0;

        // 指定备份时间 2021-05-28 12:05:00 至 2021-05-28 12:10:00
        remoteTasks[0].stuStartTime = new NET_TIME(2021, 5, 28, 12, 5, 0);
        remoteTasks[0].stuEndTime = new NET_TIME(2021, 5, 28, 12, 10, 0);

        // 指定录像类型 普通
        remoteTasks[0].emFileType = ENUM_RECORDBACKUP_FILE_TYPE.ENUM_RECORDBACKUP_FILE_COMMON.getValue();

        // 指定码流 主码流
        remoteTasks[0].emStreamType = NetSDKLib.NET_STREAM_TYPE.NET_EM_STREAM_MAIN;

        //////// 继续组装参数 ///////
        ///////////////////////////

        Pointer pTasks = new Memory(remoteTasks[0].size() * remoteTasks.length);
        ToolKits.SetStructArrToPointerData(remoteTasks, pTasks);
        stuIn.pStuTask = pTasks;

        NET_OUT_ADD_REC_BAK_RST_REMOTE_TASK stuOut = new NET_OUT_ADD_REC_BAK_RST_REMOTE_TASK();

        NET_RECORDBACKUP_TASKID_INFO[] taskIds = new NET_RECORDBACKUP_TASKID_INFO[stuIn.nTaskCount];   // 数组长度和任务数一致
        for (int i = 0; i < taskIds.length; i++) {
            taskIds[i] = new NET_RECORDBACKUP_TASKID_INFO();
        }
        Pointer pTaskIds = new Memory(taskIds[0].size() * taskIds.length);
        ToolKits.SetStructArrToPointerData(taskIds, pTaskIds);
        stuOut.pStuID = pTaskIds;

        stuIn.write();
        stuOut.write();
        boolean ret = netSdk.CLIENT_AddRecordBackupRestoreRemoteTask(m_hRestoreID, stuIn.getPointer(), stuOut.getPointer(), 10000);
        if (!ret) {
            System.err.println("远程备份录像任务命令发送失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        stuOut.read();
        ToolKits.GetPointerDataToStructArr(stuOut.pStuID, taskIds);
        System.out.println("远程备份录像任务命令发送成功");

        // 打印下返回数据
        StringBuilder info = new StringBuilder().append("添加成功的备份任务TaskID列表:\n");
        for (int i = 0; i < 1; i++) {
            info.append("  Task ").append(i + 1).append(" ID:\n");
            for (int j = 0; j < 1 && j < taskIds[i].nTaskIDCount; j++) {
                info.append("    通道组 ").append(j + 1).append(" :").append(taskIds[i].nTaskIDs[j]).append("\n");
            }
        }
        System.out.println(info.toString());
    }

    public static class TaskId extends SdkStructure {
        public int[] pnTaskIDs;
    }

    /**
     * 删除录像备份恢复任务
     * Demo 这里需要输入具体的 TaskID
     * 且正在备份中的 TaskID 删除无效
     */
    public void RemoveRecordBackupRestoreTask() {
        System.out.println("请输入TaskID 进行中的Task删除无效");
        Scanner sc = new Scanner(System.in);
        int taskId = sc.nextInt();

        // 入参
        NET_IN_REMOVE_REC_BAK_RST_TASK pInParam = new NET_IN_REMOVE_REC_BAK_RST_TASK();
        pInParam.nTaskCount = 1;
        pInParam.pnTaskIDs = new Memory(pInParam.nTaskCount * 4);
        TaskId nt = new TaskId();
        nt.pnTaskIDs = new int[pInParam.nTaskCount];
        nt.pnTaskIDs[0] = taskId;
        ToolKits.SetStructDataToPointer(nt, pInParam.pnTaskIDs, 0);
        pInParam.write();
        boolean ret = netSdk.CLIENT_RemoveRecordBackupRestoreTask(m_hRestoreID, pInParam.getPointer(), 3000);

        if (ret) {
            System.out.println("RemoveRecordBackupRestoreTask success");
        } else {
            System.err.printf("RemoveRecordBackupRestoreTask false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    /**
     * 获取录像备份恢复任务信息
     */
    public void QueryRecordBackupRestoreTask() {
        // 入参
        NET_IN_QUERY_REC_BAK_RST_TASK pInParam = new NET_IN_QUERY_REC_BAK_RST_TASK();

        // 出参
        NET_OUT_QUERY_REC_BAK_RST_TASK pOutParam = new NET_OUT_QUERY_REC_BAK_RST_TASK();
        pOutParam.nMaxCount = 100;
        NET_REC_BAK_RST_TASK[] Tasks = new NET_REC_BAK_RST_TASK[pOutParam.nMaxCount];
        for (int i = 0; i < pOutParam.nMaxCount; i++) {
            Tasks[i] = new NET_REC_BAK_RST_TASK();
        }
        pOutParam.pTasks = new Memory(Tasks[0].size() * pOutParam.nMaxCount);
        ToolKits.SetStructArrToPointerData(Tasks, pOutParam.pTasks);

        pInParam.write();
        pOutParam.write();
        boolean ret = netSdk.CLIENT_QueryRecordBackupRestoreTask(m_hRestoreID, pInParam.getPointer(), pOutParam.getPointer(), 3000);

        if (!ret) {
            System.err.printf("QueryRecordBackupRestoreTask false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
        pOutParam.read();
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks,Tasks);

        int maxCount = Math.min(pOutParam.nMaxCount, pOutParam.nReturnCount);
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks, Tasks);
        if (maxCount == 0) {
            System.out.println("没有发现任务；请添加");
            return;
        }
        for (int i = 0; i < maxCount; i++) {
            String szDevice = new String(Tasks[i].szDeviceID, sdkEncode).trim();
            taskID[i] = Tasks[i].nTaskID;
            System.out.println("任务ID: " + taskID[i] + " 设备ID: " + szDevice
                    + " 通道号: " + Tasks[i].nChannelID + " 开始时间: " + Tasks[i].stuStartTime
                    + " 结束时间: " + Tasks[i].stuEndTime + "当前备份状态(0 等待 1 进行中 2 结束 3 失败): " + Tasks[i].nState);
        }
    }

    // 文件流转 byte[] 数组
    public static void getFileByBytes(byte[] bytes, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {  // 判断文件目录是否存在
                if (!dir.mkdirs()) System.err.println("目录创建失败");
            }
            file = new File(filePath + "\\" + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) bos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // byte[]数组转文件流
    public static byte[] toByteArray(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (in != null) in.close();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导入配置文件
     */
    public void ImportConfigFileJson() throws IOException {
        byte[] bytes = toByteArray("D:\\123.txt");   // 配置文件读取地址
        int nSendBufLen = bytes.length;
        Pointer pSendBuf = new Memory(nSendBufLen);
        System.arraycopy(bytes, 0, pSendBuf.getByteArray(0, nSendBufLen), 0, bytes.length);
        boolean ret = netSdk.CLIENT_ImportConfigFileJson(m_hLoginHandle, pSendBuf, nSendBufLen, null, 3000);
        if (ret) {
            System.out.println("ImportConfigFileJson success");
        } else {
            System.err.printf("ImportConfigFileJson false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    /**
     * 导出配置文件
     */
    public void ExportConfigFileJson() {
        int maxLen = 10 * 1024 * 1024;
        Pointer pOutBuffer = new Memory(maxLen);
        IntByReference nRetLen = new IntByReference(0);
        boolean ret = netSdk.CLIENT_ExportConfigFileJson(m_hLoginHandle, pOutBuffer, maxLen, nRetLen, null, 3000);
        if (ret) {
            getFileByBytes(pOutBuffer.getByteArray(0, maxLen), "D:", "345.txt");  // 配置文件写入地址
            System.out.println("ImportConfigFileJson success");
        } else {
            System.err.printf("ImportConfigFileJson false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    /**
     * web信息上传接口 json透传
     */
    public void TransmitInfoForWeb() {
        String request = "{\"method\":\"configManager.getConfig\",\"params\":{\"name\":\"VSP_LXSJ\"}}";
        int dwInBufferSize = request.length();
        Pointer szInBuffer = new Memory(dwInBufferSize);
        //System.arraycopy(request.getBytes(), 0, szInBuffer.getByteArray(0, dwInBufferSize), 0, request.getBytes().length);
        szInBuffer.write(0, request.getBytes(), 0, request.getBytes().length);
        int dwOutBufferSize = 10 * 1024 * 1024;
        Pointer szOutBuffer = new Memory(dwOutBufferSize);
        boolean ret = netSdk.CLIENT_TransmitInfoForWeb(m_hLoginHandle, szInBuffer, dwInBufferSize, szOutBuffer, dwOutBufferSize, null, 3000);
        if (ret) {
            System.out.println("TransmitInfoForWeb success");
        } else {
            System.err.printf("TransmitInfoForWeb false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }

    // 定时录像配置下发
    public void record() {
        String szCommand = NetSDKLib.CFG_CMD_RECORD;
        NetSDKLib.CFG_RECORD_INFO recordModes = new NetSDKLib.CFG_RECORD_INFO();
        //获取配置信息
        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, 0, szCommand, recordModes);
        if (!ret) {
            System.out.println("GetDevConfig false");
            return;
        }
        System.out.println("预录时间" + recordModes.nPreRecTime);

        //设置参数
        recordModes.nChannelID = 0;//通道
        recordModes.nStreamType = 0;//码流类型
        recordModes.stuTimeSection[0].stuTimeSection[0].setStartTime(10, 10, 10);//开始时间
        recordModes.stuTimeSection[0].stuTimeSection[0].setEndTime(12, 12, 12);//结束时间
        recordModes.nPreRecTime = 60;//预录时间
        boolean bRet = ToolKits.SetDevConfig(m_hLoginHandle, -1, szCommand, recordModes);
        if (bRet) {
            System.out.println("SetDevConfig success");
        } else {
            System.out.println("SetDevConfig false");
        }
    }
    
    
    /**
     * 透传接口实现获取配置
     */
    public void getConfig() {
    	JSONObject params = new JSONObject();
    	params.put("name", "VSP_LXSJ");
    	
    	JSONObject JSONObject = new JSONObject();
    	JSONObject.put("method", "configManager.getConfig");
    	JSONObject.put("params", params);
    	String request = JSONObject.toString();
    	System.out.println("request:"+request);
		NET_IN_TRANSMIT_INFO pIn =new NET_IN_TRANSMIT_INFO();
		pIn.emType = 0;
		pIn.emEncryptType = 0;
		String json = request;			
		
		pIn.dwInJsonBufferSize = json.getBytes().length;
		pIn.szInJsonBuffer=json;
		
		 Pointer pInParam =new Memory(pIn.size());
	        ToolKits.SetStructDataToPointer(pIn, pInParam, 0);
		
		NET_OUT_TRANSMIT_INFO pOut =new NET_OUT_TRANSMIT_INFO();
		pOut.szOutBuffer =new Memory(1024*10);
		pOut.dwOutBufferSize = 1024*10;
		Pointer poutParam =new Memory(pOut.size());
        ToolKits.SetStructDataToPointer(pOut, poutParam, 0);
		
		boolean ret = netSdk.CLIENT_TransmitInfoForWebEx(m_hLoginHandle, pInParam, poutParam, 3000);

		if (!ret) {
			System.err.printf("transmitInfoForWebEX Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
		} else {
			System.out.println("transmitInfoForWebEX Succeed!");

			ToolKits.GetPointerDataToStruct(poutParam,0,pOut);
			
			System.out.println("dwOutBinLen:"+pOut.dwOutBinLen);
			System.out.println("dwOutJsonLen:"+pOut.dwOutJsonLen);
			System.out.println("dwOutBufferSize:"+pOut.dwOutBufferSize);
			
			byte[] str = pOut.szOutBuffer.getByteArray(0, 1000);//解析字节长度根据实际返回字符创结果定义
			System.out.println("Str:"+new String(str));
		}
    }
    

    ////////////////////////////////////////////////////////////////
    public String m_strIp = "172.24.0.53";
    public int m_nPort = 37777;
    public String m_strUser = "admin";
    public String m_strPassword = "admin123";
    ////////////////////////////////////////////////////////////////

    public void InitTest() {
        Init();  // 初始化
        Login(); // 登录
    }

    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        // 录像备份
        menu.addItem((new CaseMenu.Item(this, "开始录像备份恢复", "StartRecordBackupRestore")));
        menu.addItem((new CaseMenu.Item(this, "停止录像备份恢复", "StopRecordBackupRestore")));
        // 录像备份/恢复任务
        menu.addItem((new CaseMenu.Item(this, "添加录像备份恢复任务", "AddRecordBackupRestoreTask")));
        menu.addItem((new CaseMenu.Item(this, "添加远程录像备份恢复任务(21.05.28新增)", "AddRemoteRecordBackupRestoreTask")));
        menu.addItem((new CaseMenu.Item(this, "删除录像备份恢复任务(21.05.28修改)", "RemoveRecordBackupRestoreTask")));
        menu.addItem((new CaseMenu.Item(this, "获取录像备份恢复任务信息(21.05.28修改)", "QueryRecordBackupRestoreTask")));
        // 配置文件 导入/导出
        menu.addItem((new CaseMenu.Item(this, "导入配置文件", "ImportConfigFileJson")));
        menu.addItem((new CaseMenu.Item(this, "导出配置文件", "ExportConfigFileJson")));
        // Web信息透传
        menu.addItem((new CaseMenu.Item(this, "web信息上传接口", "TransmitInfoForWeb")));
        
        //透传接口实现获取配置
        menu.addItem((new CaseMenu.Item(this, "透传接口实现获取配置", "getConfig")));

        menu.run();
    }

    public void EndTest() {
        LoginOut();      // 登出
        exit(0);  // 退出
    }

    public static void main(String[] args) {
        System.out.println("请先执行\"开始录像备份恢复\", 再执行录像备份相关接口, 并在备份结束后调用\"停止录像备份恢复\"");

        RestoreTask XM = new RestoreTask();
        if (args.length == 4) {
            XM.m_strIp = args[0];
            XM.m_nPort = Integer.parseInt(args[1]);
            XM.m_strUser = args[2];
            XM.m_strPassword = args[3];
        }
        XM.InitTest();
        XM.RunTest();
        XM.EndTest();
    }
}
