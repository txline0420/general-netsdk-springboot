package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.frame.CourseRecordLogonFrame;

import java.awt.*;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/28 9:58
 */
public class DemoFrameSteamAndCourseManage {

    public CourseRecordLogon courseRecordLogon = new CourseRecordLogon();


    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    public String m_strIpAddr = "172.8.1.31";
    public int m_nPort = 37777;
    public String m_strUser = "admin";
    public String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    /**
     * Launch the application.
     */
    public static void main(String[] args) {

        final DemoFrameSteamAndCourseManage demo = new DemoFrameSteamAndCourseManage();
        demo.courseRecordLogon.m_strIpAddr = demo.m_strIpAddr;
        demo.courseRecordLogon.m_nPort = demo.m_nPort;
        demo.courseRecordLogon.m_strUser = demo.m_strUser;
        demo.courseRecordLogon.m_strPassword = demo.m_strPassword;

        // SDK 初始化
        CourseRecordInit.Init();
        /**
         * 这个 swing 界面 Demo 只提供:
         * 1) 导播拉流
         * 2）课程的增删改查
         */
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CourseRecordLogonFrame frame = new CourseRecordLogonFrame(demo.courseRecordLogon);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
