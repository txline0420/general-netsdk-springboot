package com.netsdk.demo.customize.analyseTaskDemo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

import static com.netsdk.lib.enumeration.EM_EVENT_IVS.EVENT_IVS_FEATURE_ABSTRACT;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/17 14:37
 */
public class AnalyseTask {

    /****************************** static *************************************
     * *************************************************************************
     * *************************************************************************/

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数需要是静态的，防止被系统回收
    private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调
    private static NetSDKLib.fAnalyseTaskResultCallBack analyseTaskResultCB = DefaultAnalyseTaskResultCallBack.getSingleInstance(); // 智能分析回调

    /**
     * Init boolean. sdk 初始化
     *
     * @return the boolean
     */
    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        AnalyseTask.enableLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000;                        //登录请求响应超时时间设置为3S
        int tryTimes = 1;                           //登录时尝试建立链接 1 次
        netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);
        // 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间
        netsdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 打开 sdk log
     */
    private static void enableLog() {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("sdklog/");
        if (!path.exists()) path.mkdir();

        // 这里的log保存地址依据实际情况自己调整
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Failed to open NetSDK log");
    }

    /**
     * Cleanup. 清除 sdk 环境
     */
    public static void cleanup() {
        if (bLogOpen) {
            netsdk.CLIENT_LogClose();
        }
        if (bInit) {
            netsdk.CLIENT_Cleanup();
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netsdk.CLIENT_Cleanup();
        System.exit(0);
    }

    /************************************ 登录信息 *********************************************
     ******************************************************************************************
     ******************************************************************************************/

    public void setLogonInfo(String addr, String port, String username, String pwd) {
        m_strIpAddr = addr.trim();
        m_nPort = Integer.parseInt(port.trim());
        m_strUser = username;
        m_strPassword = pwd;
    }

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄
    private NetSDKLib.LLong m_attachHandle = new NetSDKLib.LLong(0); // 订阅句柄

    /**
     * login. TCP 登录
     */
    public void login() {

        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登录
        IntByReference nError = new IntByReference(0);
        m_hLoginHandle = netsdk.CLIENT_LoginEx2(m_strIpAddr, m_nPort, m_strUser, m_strPassword,
                nSpecCap, null, deviceInfo, nError);
        if (m_hLoginHandle.longValue() != 0) {
            System.out.printf("login Device[%s] Success!\n", m_strIpAddr);
        } else {
            System.err.printf("login Device[%s] Fail.Error[%s]\n", m_strIpAddr, "Test Error Code.");
            logOut();
        }
    }

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIpAddr.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netsdk.CLIENT_GetLastError());
        } else {
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
        }
    }


    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("LogOut Success");
        }
    }

    /******************************************** 获取设备能力集 ********************************************
     ******************************************************************************************************
     ******************************************************************************************************/
    /**
     * 设备智能分析服务能力集查询
     */
    public void GetAnalyseCaps() {
        // 查询结构体入参
        NetSDKLib.NET_ANALYSE_CAPS_ALGORITHM stuCapsOutParam = new NetSDKLib.NET_ANALYSE_CAPS_ALGORITHM();

        int emCapsType = NetSDKLib.EM_ANALYSE_CAPS_TYPE.EM_ANALYSE_CAPS_ALGORITHM;    // 智能分析服务能力类型 1

        stuCapsOutParam.write();   // 写入内存
        boolean ret = netsdk.CLIENT_GetAnalyseCaps(m_hLoginHandle, emCapsType, stuCapsOutParam.getPointer(), 3000);
        stuCapsOutParam.read();    // 从内存重新读取

        if (!ret) {
            System.err.printf("failed to get analyse caps. ErrorCode=%s\n", ToolKits.getErrorCode());
            return;
        }

        System.out.println("当前设备支持特征向量个数：" + stuCapsOutParam.nAlgorithmNum);

        for (int i = 0; i < stuCapsOutParam.nAlgorithmNum; i++) {
            String info = "第" + (i + 1) + "个\n" +
                    "场景类型：" + stuCapsOutParam.stuAlgorithmInfos[i].emClassType + "\n" +   // 枚举，参考 EM_SCENE_CLASS_TYPE
                    "特征向量版本号：" + new String(stuCapsOutParam.stuAlgorithmInfos[i].szVersion) + "\n" +
                    "算法厂商：" + stuCapsOutParam.stuAlgorithmInfos[i].emAlgorithmVendor;
            System.out.println(info);
        }
    }

    /******************************************** 添加、查询、删除智能事件 ********************************************
     ******************************************************************************************************
     ******************************************************************************************************/

    // 任务ID
    private int myTaskID = 0;

    /**
     * 添加智能分析任务测试
     */
    public void AddAnalyseTask() {
        // 图片智能分析入参
        NetSDKLib.NET_PUSH_PICFILE_INFO stuPushPicFileInfo = new NetSDKLib.NET_PUSH_PICFILE_INFO();
        stuPushPicFileInfo.emStartRule = NetSDKLib.EM_ANALYSE_TASK_START_RULE.EM_ANALYSE_TASK_START_NOW; // 立刻启动

        // 设置图片分析规则
        NetSDKLib.NET_ANALYSE_RULE stuRule = new NetSDKLib.NET_ANALYSE_RULE();
        stuRule.nRuleCount = 1;     // 以一条规则为例
        stuRule.stuRuleInfos[0].emClassType = NetSDKLib.EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_FEATURE_ABSTRACT; // 特征提取大类
        stuRule.stuRuleInfos[0].dwRuleType = EVENT_IVS_FEATURE_ABSTRACT.getId();   // 特征提取

        /* 特征提取事件 (对应 DEV_EVENT_FEATURE_ABSTRACT_INFO) */
        NetSDKLib.NET_FEATURE_ABSTRACT_RULE_INFO sRuleInfo = new NetSDKLib.NET_FEATURE_ABSTRACT_RULE_INFO();
        sRuleInfo.emAbstractType = NetSDKLib.EM_FEATURE_ABSTRACT_TYPE.EM_FEATURE_ABSTRACT_FACE;  // 特征提取的类型：人脸
        sRuleInfo.nFeature = 1;     // 提取数量
        /* 设定提取版本号 */
        byte[] version001 = "1003001009002".getBytes();         // 这里使用的是能力集获取时得到的版本号
        // byte[] version001 = "1003001003002".getBytes();
        System.arraycopy(version001, 0, sRuleInfo.szFeatureVersions[0].szFeatureVersion, 0, version001.length);

        // 存入指针
        stuRule.stuRuleInfos[0].pReserved = sRuleInfo.getPointer();
        sRuleInfo.write();  // 结构体写入内存

        stuPushPicFileInfo.stuRuleInfo = stuRule;   // 赋规则
        stuPushPicFileInfo.write(); // 结构体写入内存

        // 出参
        NetSDKLib.NET_OUT_ADD_ANALYSE_TASK stuOutParam = new NetSDKLib.NET_OUT_ADD_ANALYSE_TASK();

        boolean success = netsdk.CLIENT_AddAnalyseTask(m_hLoginHandle, NetSDKLib.EM_DATA_SOURCE_TYPE.EM_DATA_SOURCE_PUSH_PICFILE,
                stuPushPicFileInfo.getPointer(), stuOutParam, 15000);  // 超时时间设长一点
        if (!success) {
            System.err.printf("Failed to add task! ErrorCode=%s\n", ToolKits.getErrorCode());
        } else {
            System.out.println("添加任务成功, 任务ID:" + stuOutParam.nTaskID);
            this.myTaskID = stuOutParam.nTaskID;
        }
    }

    /**
     * 查询智能分析任务
     */
    public void FindAnalyseTask() {
        // 入参
        NetSDKLib.NET_IN_FIND_ANALYSE_TASK stuInParam = new NetSDKLib.NET_IN_FIND_ANALYSE_TASK();
        // 出参
        NetSDKLib.NET_OUT_FIND_ANALYSE_TASK stuOutParam = new NetSDKLib.NET_OUT_FIND_ANALYSE_TASK();

        if (netsdk.CLIENT_FindAnalyseTask(m_hLoginHandle, stuInParam, stuOutParam, 5000)) {
            System.out.println("FindAnalyseTask Succeed!" + "智能分析任务个数" + stuOutParam.nTaskNum);
            // ID和状态 可以从 stuTaskInfos 中获取
            for (int i = 0; i < stuOutParam.nTaskNum; i++) {   // 状态值参考 EM_ANALYSE_STATE
                System.out.printf("任务%d: %d, 状态：%d\n", (i + 1), stuOutParam.stuTaskInfos[i].nTaskID, stuOutParam.stuTaskInfos[i].emAnalyseState);
            }
        } else {
            System.err.printf("FindAnalyseTask Failed!Last Error:%s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 删除智能分析任务
     */
    public void RemoveAnalyseTask() {
        // 入参
        NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK pInParam = new NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK();
        pInParam.nTaskID = this.myTaskID;  // demo里删除的是刚刚创建的任务，正常应该是查询后获取任务列表再选择需要删除的ID

        // 出参
        NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK pOutParam = new NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK();

        if (netsdk.CLIENT_RemoveAnalyseTask(m_hLoginHandle, pInParam, pOutParam, 5000)) {
            System.out.println("RemoveAnalyseTask Succeed! ");
        } else {
            System.err.printf("RemoveAnalyseTask Failed!Last Error: %s\n", ToolKits.getErrorCode());
        }
        // 删除后再查询一下看看是否删除
    }

    /******************************************** 任务订阅与退订 ********************************************
     ******************************************************************************************************
     ******************************************************************************************************/

    public void SetDemoTaskID() {
        System.out.println("请输入TaskID");
        Scanner sc = new Scanner(System.in);

        this.myTaskID = sc.nextInt();
    }

    /**
     * 订阅智能事件
     */
    public void AttachAnalyseTaskResult() {

        this.DetachAnalyseTaskResult();  // 安全起见，先执行一次退订
        System.out.println("myTaskID:"+myTaskID);
        // 入参
        NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT stuInAttach = new NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT();
        stuInAttach.cbAnalyseTaskResult = AnalyseTask.analyseTaskResultCB;
        stuInAttach.nTaskIdNum = 1;             // 目前设备只支持按一个任务订阅
        stuInAttach.nTaskIDs[0] = myTaskID;     // 必须指定一个有效的 TaskID 才能订阅成功
        stuInAttach.stuFilter.nEventNum = 1;    // 只获取 EVENT_IVS_FEATURE_ABSTRACT 特征提取事件
        stuInAttach.stuFilter.dwAlarmTypes[0] = EVENT_IVS_FEATURE_ABSTRACT.getId();

        m_attachHandle = netsdk.CLIENT_AttachAnalyseTaskResult(m_hLoginHandle, stuInAttach, 5000);
        System.out.println(m_attachHandle.longValue());
        if (m_attachHandle.longValue() != 0) {
            System.out.println("AttachAnalyseTaskResult Succeed!");
        } else {
            System.err.printf("AttachAnalyseTaskResult Failed!Last Error: %s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 退订智能事件
     */
    public void DetachAnalyseTaskResult() {
        if (0 != m_attachHandle.longValue()) {
            boolean ret = netsdk.CLIENT_DetachAnalyseTaskResult(m_attachHandle);
            if (!ret) {
                System.err.printf("DetachAnalyseTaskResult Failed!Last Error: %s\n", ToolKits.getErrorCode());
            } else {
                System.out.println("DetachAnalyseTaskResult Succeed!");
                m_attachHandle.setValue(0);
            }
        }
    }

    /******************************************** 主动推送图片 ********************************************
     ******************************************************************************************************
     ******************************************************************************************************/
    // 测试用的图片
    private final String testPicPath1 = "D:/1.jpg";
    private final String testPicPath2 = "D:/2.jpg";

    /**
     * 主动推送图片
     */
    public void PushAnalysePictureFile() {
        //入参
        NetSDKLib.NET_IN_PUSH_ANALYSE_PICTURE_FILE stuInParam = new NetSDKLib.NET_IN_PUSH_ANALYSE_PICTURE_FILE();
        stuInParam.nTaskID = myTaskID;
        stuInParam.nPicNum = 2;    // 以两张图为例

        // ftp://用户名:密码@hostname:端口号/文件路径
        // byte[] fileUrl = "ftp://username:password@hostname:port/filepath".getBytes();
        // System.arraycopy(fileUrl, 0, stuInParam.stuPushPicInfos[0].szUrl, 0, fileUrl.length);

        // 加载本地图片到缓冲区, 这里以两张图为例
        int totalLen = 0;  // 这是总的图片缓冲区长度

        byte[] testFileBuffer1 = ToolKits.readPictureToByteArray(testPicPath1);
        byte[] testFileBuffer2 = ToolKits.readPictureToByteArray(testPicPath2);

        // 文件id
        byte[] fileId1 = "file001".getBytes();   // 这个ID是图片的标识符，设备分析完毕返回的数据也以这个ID区分
        System.arraycopy(fileId1, 0, stuInParam.stuPushPicInfos[0].szFileID, 0, fileId1.length);

        if (testFileBuffer1 != null) {
            stuInParam.stuPushPicInfos[0].nLength = testFileBuffer1.length;
            stuInParam.stuPushPicInfos[0].nOffset = totalLen; // 偏移量，有多张图时这里要特别注意
            totalLen = totalLen + stuInParam.stuPushPicInfos[0].nLength;  // 总的长度是各个图的累加
        }

        byte[] fileId2 = "file002".getBytes();   // 这个ID是图片的标识符，设备分析完毕返回的数据也以这个ID区分
        System.arraycopy(fileId2, 0, stuInParam.stuPushPicInfos[1].szFileID, 0, fileId2.length);
        if (testFileBuffer2 != null) {
            stuInParam.stuPushPicInfos[1].nLength = testFileBuffer2.length;
            stuInParam.stuPushPicInfos[1].nOffset = totalLen; // 偏移量，有多张图时这里要特别注意
            totalLen = totalLen + stuInParam.stuPushPicInfos[1].nLength;  // 总的长度是各个图的累加
        }

        stuInParam.nBinBufLen = totalLen;
        stuInParam.pBinBuf = new Memory(totalLen); // 分配内存
        stuInParam.pBinBuf.clear(totalLen);        // 清理内存

        if (testFileBuffer1 != null)         // 第一张图写入缓存
            stuInParam.pBinBuf.write(stuInParam.stuPushPicInfos[0].nOffset, testFileBuffer1, 0, stuInParam.stuPushPicInfos[0].nLength);
        if (testFileBuffer2 != null)         // 第二张图写入缓存
            stuInParam.pBinBuf.write(stuInParam.stuPushPicInfos[1].nOffset, testFileBuffer2, 0, stuInParam.stuPushPicInfos[1].nLength);

        // 出参
        NetSDKLib.NET_OUT_PUSH_ANALYSE_PICTURE_FILE stuOutParam = new NetSDKLib.NET_OUT_PUSH_ANALYSE_PICTURE_FILE();

        if (netsdk.CLIENT_PushAnalysePictureFile(m_hLoginHandle, stuInParam, stuOutParam, 5000)) {
            System.out.println("PushAnalysePictureFile Succeed!");
        } else {
            System.err.printf("PushAnalysePictureFile Failed!Last Error: %s\n", ToolKits.getErrorCode());
        }
    }

    /******************************************** 简易控制台 ***********************************************
     ******************************************************************************************************
     ******************************************************************************************************/

    // 初始化测试
    public void InitTest() {

        AnalyseTask.Init(); // 初始化SDK库
        this.loginWithHighLevel();   // 高安全登录

        if (m_hLoginHandle.longValue() != 0) {
            System.out.printf("Login Device [%s:%d] Success. \n", m_strIpAddr, m_nPort);
        } else {
            System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", m_strIpAddr, m_nPort, netsdk.CLIENT_GetLastError());
            EndTest();
        }
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "分析能力查询", "GetAnalyseCaps"));
        menu.addItem(new CaseMenu.Item(this, "添加分析任务", "AddAnalyseTask"));
        menu.addItem(new CaseMenu.Item(this, "查询分析任务", "FindAnalyseTask"));
        menu.addItem(new CaseMenu.Item(this, "删除分析任务", "RemoveAnalyseTask"));
        menu.addItem(new CaseMenu.Item(this, "选择TaskID", "SetDemoTaskID"));
        menu.addItem(new CaseMenu.Item(this, "订阅分析事件", "AttachAnalyseTaskResult"));
        menu.addItem(new CaseMenu.Item(this, "退订分析事件", "DetachAnalyseTaskResult"));
        menu.addItem(new CaseMenu.Item(this, "主动推送图片", "PushAnalysePictureFile"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.DetachAnalyseTaskResult();    // 退订
        this.logOut();  // 退出
        System.out.println("See You...");

        AnalyseTask.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_strIpAddr = "172.12.245.69";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        /**
         * Demo 的使用逻辑：
         * 1） 先填写设备登录信息： addr, port, username, password。 Demo启动后会先初始化SDK然后登录
         * 2） 获取设备分析能力：得到设备现在的特征比对版本号
         *
         * 3） 查询一下设备现存的任务，设备会返回现存任务的 TaskId
         * 4） 如果没有任务，则添加分析任务，设备会返回新添加任务的 TaskId
         * 5） 如果任务不需要了，可以删除任务（指定 TaskId）
         *
         * 6） 选择 TaskID 这个选项是为了方便 demo 使用，手动指定 TaskId
         *
         * 7） 准备好任务后即可订阅任务，相对应的可以退订任务，注意订阅和退订必须一一对应。
         * 8） 订阅时用到的 “特征比对版本号” 就是获取设备分析能力时拿到的内容。
         *
         * 9） 订阅任务后，即可推送图片，图片可以一次推送多张，demo里以两张作示例
         * 10）推送后设备回很快返回分析结果，包括角度、质量等信息
         *
         * 11） 退出时，Demo 会退订、登出并清理资源。
         * 12） 具体的配置内容请参看 demo内的函数注释
         */
        AnalyseTask demo = new AnalyseTask();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
