package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.pojo.RealPreviewChannel;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_LOGIC_CHANNEL;
import com.netsdk.lib.enumeration.NET_ENUM_INPUT_CHANNEL_MEDIA;
import com.netsdk.lib.structure.*;

import static com.netsdk.demo.customize.courseRecord.modules.CourseChannelModule.*;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/30 10:21
 */
public class DemoConsoleDeviceStatus {

    CourseRecordLogon courseRecordLogon = new CourseRecordLogon();
    TestCourseRecordStatus courseRecordStatus = new TestCourseRecordStatus();

    /**
     * 获取软件版本
     */
    public void TestQueryDevDeviceVersionState() {
        courseRecordStatus.QueryDevDeviceVersionStateTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 查询设备在线状态
     */
    public void TestQueryOnlineState() {
        courseRecordStatus.QueryOnlineStateTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 查看设备存储设备状态
     */
    public void TestQueryHardDiskState() {
        courseRecordStatus.QueryHardDiskStateTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 获取录播主机所有配置的前端摄像头连接状态
     */
    public void TestQueryCameraState() {
        courseRecordStatus.QueryCameraStateTest(courseRecordLogon.m_hLoginHandle, courseRecordLogon.deviceInfo.byChanNum);
    }

    /**
     * 获取录播主机当前 逻辑-真实 通道配置
     */
    public void TestGetRealPreviewChannel() {
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetRealPreviewChannels(courseRecordLogon.m_hLoginHandle);
        System.out.println(realPreviewChannel.toString());
    }

    /**
     * 获取录播主机默认 逻辑-真实 通道配置
     */
    public void TestGetDefaultRealChannel() {
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetDefaultRealChannels(courseRecordLogon.m_hLoginHandle);
        System.out.println(realPreviewChannel.toString());
    }

    /**
     * 查询真实通道当前绑定在哪个逻辑通道上
     */
    public void TestGetLogicChannel() {
        /**
         * 这里的用例是: 获取 真实通道 52 (默认映射合成通道) 的逻辑通道号
         */
        NET_IN_GET_COURSE_LOGIC_CHANNEL stuLogicIn = new NET_IN_GET_COURSE_LOGIC_CHANNEL();
        stuLogicIn.nChannelNum = 1;
        stuLogicIn.nChannel[0] = 52;  // 真实通道

        NET_OUT_GET_COURSE_LOGIC_CHANNEL stuLogicOut = new NET_OUT_GET_COURSE_LOGIC_CHANNEL();

        boolean ret = GetLogicChannel(courseRecordLogon.m_hLoginHandle, stuLogicIn, stuLogicOut, 3000);
        if (!ret) {
            System.err.println("获取逻辑通道失败! " + ToolKits.getErrorCode());
            return;
        }

        int retNum = stuLogicOut.nChannelCount;

        for (int i = 0; i < retNum; i++) {
            System.out.println("编号(sdk) 52 通道的逻辑通道是: " + stuLogicOut.stuChannelInfo[i].emLogicChannel);
        }
    }

    /**
     * 绑定 逻辑-真实 通道关系
     */
    public void TestSetBlindRealChannel() {
        /**
         * 这里用例是： 把真实通道 8 (网页上是 9) 和 “学生特写” 逻辑通道绑定
         */
        NET_IN_SET_BLIND_REAL_CHANNEL stuBlindIn = new NET_IN_SET_BLIND_REAL_CHANNEL();
        stuBlindIn.nChannelNum = 1;
        stuBlindIn.nChannel[0] = 8;    // 真实通道
        stuBlindIn.stuChannelInfo[0].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFEATURE.getValue(); // 逻辑通道3

        NET_OUT_SET_BLIND_REAL_CHANNEL stuBlindOut = new NET_OUT_SET_BLIND_REAL_CHANNEL();

        boolean ret = SetBlindRealChannel(courseRecordLogon.m_hLoginHandle, stuBlindIn, stuBlindOut, 3000);
        if (!ret) {
            System.err.println("绑定 逻辑-真实 通道关系失败! ");
            return;
        }
        System.out.println("绑定 逻辑-真实 通道关系成功!");
    }

    /**
     * 获取资源通道媒体介质类型
     */
    public void TestGetInputChannelMedia() {
        /**
         * 这里的用例是: 查看 真实通道 0 的媒体介质类型
         * 现在的设备 0, 1 (网页上是 1, 2) 真实通道是媒体输入
         */
        NET_IN_GET_INPUT_CHANNEL_MEDIA stuChannelMediaIn = new NET_IN_GET_INPUT_CHANNEL_MEDIA();
        stuChannelMediaIn.nChannelNum = 1;
        stuChannelMediaIn.nChannel[0] = 0;  // 真实通道 0
        NET_OUT_GET_INPUT_CHANNEL_MEDIA stuChannelMediaOut = new NET_OUT_GET_INPUT_CHANNEL_MEDIA();
        boolean ret = GetInputChannelMedia(courseRecordLogon.m_hLoginHandle, stuChannelMediaIn, stuChannelMediaOut, 3000);
        if (!ret) {
            System.err.println("获取资源通道媒体介质类型失败! " + ToolKits.getErrorCode());
            return;
        }

        int retNum = stuChannelMediaOut.nChannelNum;

        for (int i = 0; i < retNum; i++) {
            System.out.println("PPT通道的媒体介质类型是: " + NET_ENUM_INPUT_CHANNEL_MEDIA.getNoteByValue(stuChannelMediaOut.emInputMedia[i]));
        }
    }


    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        CourseRecordInit.Init();                  // 初始化SDK库
        courseRecordLogon.m_strIpAddr = m_strIpAddr;
        courseRecordLogon.m_nPort = m_nPort;
        courseRecordLogon.m_strUser = m_strUser;
        courseRecordLogon.m_strPassword = m_strPassword;
        courseRecordLogon.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取软件版本", "TestQueryDevDeviceVersionState"));
        menu.addItem(new CaseMenu.Item(this, "查询设备在线状态", "TestQueryOnlineState"));
        menu.addItem(new CaseMenu.Item(this, "查看设备存储设备状态", "TestQueryHardDiskState"));
        menu.addItem(new CaseMenu.Item(this, "获取前端摄像头连接状态", "TestQueryCameraState"));

        menu.addItem(new CaseMenu.Item(this, "获取录播主机当前 逻辑-真实 通道配置", "TestGetRealPreviewChannel"));
        menu.addItem(new CaseMenu.Item(this, "获取录播主机默认 逻辑-真实 通道配置", "TestGetDefaultRealChannel"));
        menu.addItem(new CaseMenu.Item(this, "获取资源通道媒体介质类型", "TestGetInputChannelMedia"));
        menu.addItem(new CaseMenu.Item(this, "获取真实通道绑定的逻辑通道", "TestGetLogicChannel"));
        menu.addItem(new CaseMenu.Item(this, "绑定 逻辑-真实 通道关系", "TestSetBlindRealChannel"));

        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        courseRecordLogon.logOut();  // 退出
        System.out.println("See You...");
        CourseRecordInit.CleanAndExit();  // 清理资源并退出
    }


    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_strIpAddr = "172.8.1.31";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        DemoConsoleDeviceStatus demo = new DemoConsoleDeviceStatus();

        if (args.length == 4) {
            demo.m_strIpAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
