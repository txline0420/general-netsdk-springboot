package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_MAIN_PAGE_STATE;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_CFG_BSCCUSTOM;
import com.netsdk.module.ConfigModule;

import java.nio.charset.Charset;

/**
 * 楼宇门禁产品 定制配置
 *
 * @author 47040
 * @since Created in 2021/4/26 16:47
 */
public class BSCCustomConfig {

    // netsdk 接口
    private final NetSDKLib netSdkApi = NetSDKLib.NETSDK_INSTANCE;

    // 引入sdk初始化和登录模块
    private final ConfigInitAndLogon initModule = new ConfigInitAndLogon(netSdkApi);

    // 二次封装模块,包含一些设备配置的通用接口
    private final ConfigModule configModule = new ConfigModule(netSdkApi);

    // 多平台 编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode());

    // 登录句柄 存储时句柄时可以直接使用long 传入netsdk时需要包装为LLong类
    private long loginHandler;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// BSC Custom 配置 获取/下发 ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * BSCCustom 配置获取
     */
    public void getBSCCustomConfig() {
        NET_CFG_BSCCUSTOM config = new NET_CFG_BSCCUSTOM();
        config = (NET_CFG_BSCCUSTOM)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BSCCUSTOM,         // 枚举->门禁定制配置汇总
                        config,                                               // 配置结构体
                        -1,                                           // 通道号 无效 填-1
                        5000                                         // 超时时间(毫秒)
                );
        if (config == null) {
            System.err.println("获取门禁定制配置汇总失败:" + ToolKits.getErrorCode());
            return;
        }

        // 打印看下原先的配置
        StringBuilder info = new StringBuilder().append("---------- 用户门禁定制配置汇总信息 ----------").append("\n");

        int samePersonInterval = config.nSamePersonInterval; // 间隔时间
        String mainPicState = EM_MAIN_PAGE_STATE.getNoteByValue(config.emMainPageState); // 设备界面图片所表示的状态

        info.append(String.format("%s: ", "相同人员开门间隔时间(0指不限) 单位秒")).append(samePersonInterval).append("\n")
                .append(String.format("%s: ", "设备界面图片所表示的状态")).append(mainPicState);
        System.out.println(info.toString());
    }

    //

    /**
     * BSCCustom 配置下发
     */
    public void setBSCCustomConfig() {

        // 我们推荐修改配置的方式都是 获取->修改->下发 这样的流程

        //////////////////////////////////////// 获取配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        NET_CFG_BSCCUSTOM config = new NET_CFG_BSCCUSTOM();
        config = (NET_CFG_BSCCUSTOM)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BSCCUSTOM,         // 枚举->门禁定制配置汇总
                        config,                                               // 配置结构体
                        -1,                                           // 通道号 无效 填-1
                        5000                                         // 超时时间(毫秒)
                );
        if (config == null) {
            System.err.println("获取门禁定制配置汇总失败:" + ToolKits.getErrorCode());
            return;
        }
        //////////////////////////////////////// 修改配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        config.nSamePersonInterval = 60;    // 修改间隔时间 以 60s为例
        // 设备界面图片所表示的状态 以紧急疏散为例
        config.emMainPageState = EM_MAIN_PAGE_STATE.EM_MAIN_PAGE_STATE_URGENTEVACUATE.getValue();

        //////////////////////////////////////// 下发配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        boolean ret = configModule.setConfig(
                loginHandler,                                         // 登录句柄
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BSCCUSTOM,         // 枚举->门禁定制配置汇总
                config,                                               // 配置结构体
                -1,                                           // 通道号 无效 填-1
                5000                                         // 超时时间
        );
        if (!ret) {
            System.err.println("配置门禁定制配置汇总失败:" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("配置门禁定制配置汇总发送成功");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////// 简易控制台 ////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {
        initModule.init(initModule.defaultDisconnectCB, initModule.defaultReconnectCB);                // 初始化SDK库
        loginHandler = initModule.loginWithHighSecurity(m_strIp, m_nPort, m_strUser, m_strPassword);   // 高安全登录
        if (loginHandler == 0) {
            System.err.println("登录失败, 请检查接口参数, See You..");
            initModule.cleanAndExit();
        }
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取 门禁定制配置 BSCCustomCfg", "getBSCCustomConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发 门禁定制配置 BSCCustomCfg", "setBSCCustomConfig"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        initModule.logout(loginHandler);   // 登出
        System.out.println("See You...");
        initModule.cleanAndExit();         // 清理资源并退出
    }

    //////////////////// 配置登陆地址，端口，用户名，密码 ///////////////////////
     private String m_strIp = "172.5.3.159";
    // private String m_strIp = "10.34.3.126";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    /////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        BSCCustomConfig demo = new BSCCustomConfig();

        if (args.length == 4) {
            demo.m_strIp = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }

}
