package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.util.CaseMenu;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/28 9:59
 */
public class DemoConsoleCommonConfig {

    CourseRecordLogon courseRecordLogon = new CourseRecordLogon();
    TestCourseRecordRecord courseRecordRecords = new TestCourseRecordRecord();
    TestCourseRecordConfig courseRecordConfigTest = new TestCourseRecordConfig();

    /**
     * 获取教室录像信息
     */
    public void TestGetOperateCourseRecordInfo() {
        courseRecordRecords.GetOperateCourseRecordInfoTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 设置教室录像信息
     */
    public void TestSetOperateCourseRecordInfo() {
        /**
         * 这里的用例是: 先获取教室录像信息, 再设置 合成通道(逻辑通道 0) 为录像状态
         */
        courseRecordRecords.SetOperateCourseRecordInfoTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 将录像信息更新到 time 时的信息
     */
    public void TestUpdateOperateCourseRecordInfo() {
        /**
         * 这里的用例是: 录像信息跟新到 2020/9/28 0:0:0 时的信息
         */
        courseRecordRecords.UpdateOperateCourseRecordInfoTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 控制组合通道与逻辑通道 (锁定/解锁)
     */
    public void TestLockControlOperateCourseCompositeChannel() {
        /**
         * 这里的用例是: 教师特写逻辑 5 (锁定/解锁  取反)
         */
        courseRecordRecords.LockControlOperateCourseCompositeChannelTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 获取组合通道与逻辑通道的锁定信息
     */
    public void TestGetLockInfoOperateCourseCompositeChannel() {
        /**
         * 这里的用例是:  获取 教师特写逻辑 5 的 锁定状态
         */
        courseRecordRecords.GetLockInfoOperateCourseCompositeChannelTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 获取组合通道信息
     */
    public void TestGetInfoOperateCourseCompositeChannel() {
        courseRecordRecords.GetInfoOperateCourseCompositeChannelTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 设置组合通道信息
     */
    public void TestSetInfoOperateCourseCompositeChannel() {
        /**
         * 这里的用例是: 先获取到原先的组合通道信息，再把课程名称修改为 "手动课程",用户自定义 编号 -4 模式
         */
        courseRecordRecords.SetInfoOperateCourseCompositeChannelTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 将组合通道信息更新到 time 时的信息
     */
    public void TestUpdateInfoOperateCourseCompositeChannel() {
        /**
         * 这里的用例是: 将组合通道信息更新到 2020/09/30 10:0:0 时的信息
         */
        courseRecordRecords.UpdateInfoOperateCourseCompositeChannelTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 设置课程录像模式 注意如果重复设置相同的录像模式，设备将会返回失败
     */
    public void TestSetCourseRecordMode() {
        /**
         * 这里的用例是: 设置当前教室为 课程普通模式
         */
        courseRecordRecords.SetCourseRecordModeTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 获取课程录像模式
     */
    public void TestGetCourseRecordMode() {
        courseRecordRecords.GetCourseRecordModeTest(courseRecordLogon.m_hLoginHandle);
    }

    /**
     * 获取录播默认配置
     */
    public void TestGetSteamConfigTest() {

        courseRecordConfigTest.GetSteamConfigTest(courseRecordLogon.m_hLoginHandle);

    }

    /**
     * 修改录播默认配置
     */
    public void TestSetSteamConfigTest() {
        /**
         * 这里的用例是：修改合成通道模式为多画面模式
         */
        courseRecordConfigTest.SetSteamConfigTest(courseRecordLogon.m_hLoginHandle);

    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        CourseRecordInit.Init();                 // 初始化SDK库
        courseRecordLogon.m_strIpAddr = m_strIpAddr;
        courseRecordLogon.m_nPort = m_nPort;
        courseRecordLogon.m_strUser = m_strUser;
        courseRecordLogon.m_strPassword = m_strPassword;
        courseRecordLogon.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取教室录像信息测试", "TestGetOperateCourseRecordInfo"));
        menu.addItem(new CaseMenu.Item(this, "设置教室录像信息测试", "TestSetOperateCourseRecordInfo"));
        menu.addItem(new CaseMenu.Item(this, "将录像信息更新到time时的信息测试", "TestUpdateOperateCourseRecordInfo"));

        menu.addItem(new CaseMenu.Item(this, "获取组合通道信息测试", "TestGetInfoOperateCourseCompositeChannel"));
        menu.addItem(new CaseMenu.Item(this, "设置组合通道信息测试", "TestSetInfoOperateCourseCompositeChannel"));
        menu.addItem(new CaseMenu.Item(this, "将组合通道信息更新到 time 时的信息测试", "TestUpdateInfoOperateCourseCompositeChannel"));

        menu.addItem(new CaseMenu.Item(this, "获取组合通道与逻辑通道的锁定信息测试", "TestGetLockInfoOperateCourseCompositeChannel"));
        menu.addItem(new CaseMenu.Item(this, "控制组合通道与逻辑通道(锁定/解锁)测试", "TestLockControlOperateCourseCompositeChannel"));

        // 默认配置接口设备暂时未支持，有需要请联系区域给设备提需求
        // menu.addItem(new CaseMenu.Item(this, "获取录播默认配置测试", "TestGetSteamConfigTest"));
        // menu.addItem(new CaseMenu.Item(this, "修改录播默认配置测试", "TestSetSteamConfigTest"));

        menu.addItem(new CaseMenu.Item(this, "获取课程录像模式测试", "TestGetCourseRecordMode"));
        menu.addItem(new CaseMenu.Item(this, "设置课程录像模式测试", "TestSetCourseRecordMode"));
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
        DemoConsoleCommonConfig demo = new DemoConsoleCommonConfig();

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
