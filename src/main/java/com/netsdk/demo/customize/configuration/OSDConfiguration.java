package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_OSD_USER_DEF_TITLE;
import com.netsdk.lib.structure.NET_USER_DEF_TITLE_INFO;
import com.netsdk.module.ConfigModule;

import java.nio.charset.Charset;

/**
 * OSD 相关配置 获取/下发
 *
 * @author 47040
 * @version 1.0.0
 * @since Created in 2021/3/9 10:22
 */
public class OSDConfiguration {

    // netsdk 接口
    private final NetSDKLib netSdkApi = NetSDKLib.NETSDK_INSTANCE;

    private final ConfigInitAndLogon initModule = new ConfigInitAndLogon(netSdkApi);

    // 多平台 编码
    private Charset encode = Charset.forName(Utils.getPlatformEncode());

    /**
     * 二次封装模块,包含一些设备配置的接口
     */
    private final ConfigModule configModule = new ConfigModule(netSdkApi);

    /**
     * 登录句柄值
     */
    private long loginHandler;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// OSD 配置 获取/下发 //////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取 用户自定义OSD标题配置
     */
    public void getUserDefineTitleConfig() {
        NET_OSD_USER_DEF_TITLE config = new NET_OSD_USER_DEF_TITLE();
        config = (NET_OSD_USER_DEF_TITLE)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_DEF_TITLE,    // 枚举->用户自定义OSD标题
                        config,                                               // 配置结构体
                        0                                             // 通道号
                );
        if (config == null) {
            System.err.println("获取自定义OSD标题配置失败:" + ToolKits.getErrorCode());
            return;
        }

        // 打印看下原先的配置
        StringBuilder info = new StringBuilder().append("---------- 用户自定义OSD标题配置信息 ----------").append("\n");

        int userTitleCount = config.nUserDefTitleNum;
        if (userTitleCount > 0) {
            info.append(String.format("( 共包含%d个配置 )", userTitleCount)).append("\n");
            for (int i = 0; i < userTitleCount; i++) {

                String text = new String(config.stuUserDefTitle[i].szText, encode);
                String bEncodeBlend = (config.stuUserDefTitle[i].bEncodeBlend == 0) ? "未使能" : "使能";
                String bPreviewBlend = (config.stuUserDefTitle[i].bPreviewBlend == 0) ? "未使能" : "使能";
                String rect = config.stuUserDefTitle[i].stuRect.toString();
                String frontColor = config.stuUserDefTitle[i].stuFrontColor.toString();
                String backColor = config.stuUserDefTitle[i].stuBackColor.toString();
                String emTextAlign = String.valueOf(config.stuUserDefTitle[i].emTextAlign);

                info.append(String.format(">>>>> 第【%d】配置详情 <<<<<", i + 1)).append("\n")
                        .append(String.format("    %s: ", "标题内容")).append(text).append("\n")
                        .append(String.format("    %s: ", "叠加到编码视频使能")).append(bEncodeBlend).append("\n")
                        .append(String.format("    %s: ", "叠加到预览视频使能")).append(bPreviewBlend).append("\n")
                        .append(String.format("    %s: ", "标题所在区域")).append(rect).append("\n")
                        .append(String.format("    %s: ", "前景色")).append(frontColor).append("\n")
                        .append(String.format("    %s: ", "背景色")).append(backColor).append("\n")
                        .append(String.format("    %s: ", "文本对齐方式 参考 EM_TITLE_TEXT_ALIGNTYPE")).append(emTextAlign).append("\n");
            }
        }
        System.out.println(info.toString());
    }

    /**
     * 下发 用户自定义OSD标题配置
     */
    public void setUserDefineTitleConfig() {

        // 我们推荐修改配置的方式都是 获取->修改->下发 这样的流程
        // OSD标题配置经我测试 必须重置后才能再次配置 否则不会生效 这个和设备型号与设备软件版本都有关 请以实际测试为准

        //////////////////////////////////////// 重置配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        this.OSDTitleResetConfig();

        //////////////////////////////////////// 获取配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        NET_OSD_USER_DEF_TITLE config = new NET_OSD_USER_DEF_TITLE();
        config = (NET_OSD_USER_DEF_TITLE)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_DEF_TITLE,    // 枚举->用户自定义OSD标题
                        config,                                               // 配置结构体
                        0                                             // 通道号
                );
        if (config == null) {
            System.err.println("获取自定义OSD标题配置失败:" + ToolKits.getErrorCode());
            return;
        }
        if (config.nUserDefTitleNum == 0) {
            System.err.println("不存在自定义OSD标题配置 设备异常");
            return;
        }
        System.out.println("获取自定义OSD标题配置成功");

        //////////////////////////////////////// 修改配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////
        // 这里我以修改两个配置为例

        // 第一个配置
        NET_USER_DEF_TITLE_INFO titleInfo = config.stuUserDefTitle[0];

        byte[] text = "我是用户自定义标题".getBytes(encode);
        // 给 byte[] 参数赋值请务必使用 System.arraycopy, 以防止破坏数据的原始长度
        System.arraycopy(text, 0, titleInfo.szText, 0, text.length);

        titleInfo.bEncodeBlend = 1;     // 叠加到编码视频
        titleInfo.bPreviewBlend = 1;    // 叠加到预览视频

        // 配置 OSD 的左顶点边距 (8192x8192相对坐标)
        titleInfo.stuRect.left = 500;
        titleInfo.stuRect.top = 500;

        titleInfo.stuFrontColor.setRGBA(240, 248, 255, 200);   // 前景色 爱丽丝蓝
        titleInfo.stuBackColor.setRGBA(199, 21, 133, 200);     // 背景色 紫罗兰红

        titleInfo.emTextAlign = NetSDKLib.EM_TITLE_TEXT_ALIGNTYPE.EM_TEXT_ALIGNTYPE_LEFT; // 左对齐

        // 第二个配置
        NET_USER_DEF_TITLE_INFO titleInfo2 = config.stuUserDefTitle[1];

        byte[] text2 = "我也是用户自定义标题".getBytes(encode);
        // 给 byte[] 参数赋值请务必使用 System.arraycopy, 以防止破坏数据的原始长度
        System.arraycopy(text2, 0, titleInfo2.szText, 0, text2.length);

        titleInfo2.bEncodeBlend = 1;     // 叠加到编码视频
        titleInfo2.bPreviewBlend = 1;    // 叠加到预览视频

        // 配置 OSD 的左顶点边距 (8192x8192相对坐标)
        // 大部分设备从第二个配置开始， Rect 会自动匹配第一个配置的对齐方式，所以这里即使配置了也不会生效 可以不用填
        // titleInfo2.stuRect.left = 1500;
        // titleInfo2.stuRect.top = 1500;

        // 颜色也会自动和第一个配置的颜色保持一致，可以不用填
        // titleInfo2.stuFrontColor.setRGBA(100, 100, 100, 100);  // 前景色 爱丽丝蓝
        // titleInfo2.stuBackColor.setRGBA(0, 100, 100, 100);     // 背景色 宝石绿

        // 对齐方式会自动匹配第一个配置里的对齐方式，可以不用填
        // titleInfo2.emTextAlign = NetSDKLib.EM_TITLE_TEXT_ALIGNTYPE.EM_TEXT_ALIGNTYPE_LEFT; // 左对齐

        //////////////////////////////////////// 下发配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        boolean ret = configModule.setConfig(
                loginHandler,                                         // 登录句柄
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_DEF_TITLE,    // 枚举->用户自定义OSD标题
                config,                                               // 配置结构体
                0,                                            // 通道号
                5000
        );
        if (!ret) {
            System.err.println("配置OSD自定义标题失败:" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("配置OSD自定义标题成功");
    }

    public void OSDTitleResetConfig() {

        //////////////////////////////////////// 获取配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        NET_OSD_USER_DEF_TITLE config = new NET_OSD_USER_DEF_TITLE();
        config = (NET_OSD_USER_DEF_TITLE)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_DEF_TITLE,    // 枚举->用户自定义OSD标题
                        config,                                               // 配置结构体
                        0                                             // 通道号
                );
        if (config == null) {
            System.err.println("获取自定义OSD标题配置失败:" + ToolKits.getErrorCode());
            return;
        }
        if (config.nUserDefTitleNum == 0) {
            System.err.println("不存在自定义OSD标题配置 设备异常");
            return;
        }
        System.out.println("获取自定义OSD标题配置成功");

        //////////////////////////////////////// 初始化配置 ///////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        for (int i = 0; i < config.nUserDefTitleNum; i++) {

            config.stuUserDefTitle[i].szText = new byte[1024];
            config.stuUserDefTitle[i].bEncodeBlend = 0;
            config.stuUserDefTitle[i].bPreviewBlend = 0;
            config.stuUserDefTitle[i].stuRect.left = 148;
            config.stuUserDefTitle[i].stuRect.top = 352;
            config.stuUserDefTitle[i].stuRect.right = 1773;
            config.stuUserDefTitle[i].stuRect.bottom = 769;
            config.stuUserDefTitle[i].stuFrontColor.setRGBA(255, 255, 255, 0);
            config.stuUserDefTitle[i].stuBackColor.setRGBA(0, 0, 0, 128);
            config.stuUserDefTitle[i].emTextAlign = 0;
        }
        //////////////////////////////////////// 下发配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        boolean ret = configModule.setConfig(
                loginHandler,                                         // 登录句柄
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_DEF_TITLE,    // 枚举->用户自定义OSD标题
                config,                                               // 配置结构体
                0,                                            // 通道号
                5000
        );
        if (!ret) {
            System.err.println("重置OSD自定义标题失败:" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("重置OSD自定义标题成功");
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
        menu.addItem(new CaseMenu.Item(this, "获取 用户自定义OSD标题配置", "getUserDefineTitleConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发 用户自定义OSD标题配置", "setUserDefineTitleConfig"));
        menu.addItem(new CaseMenu.Item(this, "重置 用户自定义OSD标题配置", "OSDTitleResetConfig"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        initModule.logout(loginHandler);        // 登出
        System.out.println("See You...");
        initModule.cleanAndExit();              // 清理资源并退出
    }

    //////////////////// 配置登陆地址，端口，用户名，密码 ///////////////////////
//    private String m_strIp = "172.8.1.230";
    private String m_strIp = "172.23.12.112";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    /////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        OSDConfiguration demo = new OSDConfiguration();

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
