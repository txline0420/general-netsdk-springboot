package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_SERVER_OPTION;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.module.ConfigModule;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;

/**
 * 第三方配置 获取/下发
 *
 * @author 47040
 * @version 1.0.0
 * @since Created in 2021/3/9 13:50
 */
public class ThirdPartyConfiguration {

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
    /////////////////////////////////// 第三方配置 获取/下发 /////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取 公安一所平台接入配置(国标服务端)
     */
    public void getVspGaysServerConfig() {
        NET_CFG_VSP_GAYS_SERVER_INFO config = new NET_CFG_VSP_GAYS_SERVER_INFO();
        config.nSipServerInfoNum = 5;  // 获取配置的数量 填最大值就行了
        for (int i = 0; i < config.nSipServerInfoNum; i++) {
            int nChannelNum = initModule.deviceInfo.getByChanNum();

            config.stuSipServerInfo[i].nChannelInfoNum = nChannelNum;         // 这里填设备通道数就行
            config.stuSipServerInfo[i].pstuChannelInfo = new Memory((new NET_CHANNEL_INFO().size()) * nChannelNum);

            config.stuSipServerInfo[i].nAlarmInfoNum = nChannelNum;           // 这里也填设备通道数就行
            config.stuSipServerInfo[i].pstuAlarmInfo = new Memory((new NET_ALARM_INFO().size()) * nChannelNum);

            config.stuSipServerInfo[i].nAudioOutputChnInfoNum = nChannelNum;  // 这里还是填设备通道数
            config.stuSipServerInfo[i].pstuAudioOutputChnInfo = new Memory((new NET_AUDIO_OUTPUT_CHANNEL_INFO().size()) * nChannelNum);

        }

        config = (NET_CFG_VSP_GAYS_SERVER_INFO)
                configModule.getConfig(
                        loginHandler,                                         // 登录句柄
                        NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VSP_GAYS_SERVER,   // 枚举->公安一所平台接入配置(国标服务端)
                        config,                                               // 配置结构体
                        -1                                            // 通道号 固定-1
                );
        if (config == null) {
            System.err.println("获取公安一所平台接入配置(国标服务端)失败:" + ToolKits.getErrorCode());
            return;
        }

        // 打印展示
        StringBuilder info = new StringBuilder().append("---------- 设备平台接入配置信息 ----------").append("\n");
        int retCount = config.nRetSipServerInfoNum;
        info.append(String.format("( 共包含%d个配置 )", retCount)).append("\n");
        for (int i = 0; i < retCount; i++) {

            String serverOption = EM_SERVER_OPTION.getEnum(config.stuSipServerInfo[i].emServerOption).getNote();
            String sipSvrId = new String(config.stuSipServerInfo[i].szSipSvrId, encode).trim();
            String domain = new String(config.stuSipServerInfo[i].szDomain, encode).trim();
            String sipSvrIp = new String(config.stuSipServerInfo[i].szSipSvrIp, encode).trim();
            String deviceId = new String(config.stuSipServerInfo[i].szDeviceId, encode).trim();
            String password = new String(config.stuSipServerInfo[i].szPassword, encode).trim();
            String localSipPort = String.valueOf(config.stuSipServerInfo[i].nLocalSipPort);
            String sipSvrPort = String.valueOf(config.stuSipServerInfo[i].nSipSvrPort);
            String sipRegExpires = String.valueOf(config.stuSipServerInfo[i].nSipRegExpires);
            String regInterval = String.valueOf(config.stuSipServerInfo[i].nRegInterval);
            String keepAliveCircle = String.valueOf(config.stuSipServerInfo[i].nKeepAliveCircle);
            String maxTimeoutTimes = String.valueOf(config.stuSipServerInfo[i].nMaxTimeoutTimes);
            String civilCode = new String(config.stuSipServerInfo[i].szCivilCode, encode).trim();
            String interVideoID = new String(config.stuSipServerInfo[i].szIntervideoID).trim();

            info.append(String.format(">>>>> 第【%d】配置详情 <<<<<", i + 1)).append("\n")
                    .append(String.format("    %s: ", "启动选项")).append(serverOption).append("\n")
                    .append(String.format("    %s: ", "SIP服务器编号")).append(sipSvrId).append("\n")
                    .append(String.format("    %s: ", "SIP域")).append(domain).append("\n")
                    .append(String.format("    %s: ", "SIP服务器 IP")).append(sipSvrIp).append("\n")
                    .append(String.format("    %s: ", "设备编号")).append(deviceId).append("\n")
                    .append(String.format("    %s: ", "注册密码")).append(password).append("\n")
                    .append(String.format("    %s: ", "本地SIP服务端口")).append(localSipPort).append("\n")
                    .append(String.format("    %s: ", "SIP服务器端口")).append(sipSvrPort).append("\n")
                    .append(String.format("    %s: ", "注册有效期 单位秒")).append(sipRegExpires).append("\n")
                    .append(String.format("    %s: ", "注册失败后重注册间隔 单位秒")).append(regInterval).append("\n")
                    .append(String.format("    %s: ", "心跳周期 单位秒")).append(keepAliveCircle).append("\n")
                    .append(String.format("    %s: ", "最大心跳超时次数")).append(maxTimeoutTimes).append("\n")
                    .append(String.format("    %s: ", "行政区划代码")).append(civilCode).append("\n")
                    .append(String.format("    %s: ", "接入模块识别码")).append(interVideoID).append("\n");

            info.append("    ------ 通道信息 ------").append("\n");

            int retChannelInfoCount = config.stuSipServerInfo[i].nRetChannelInfoNum;
            if (retChannelInfoCount != 0) {
                NET_CHANNEL_INFO[] channelInfos = new NET_CHANNEL_INFO[retChannelInfoCount];
                for (int j = 0; j < retChannelInfoCount; j++) {
                    channelInfos[j] = new NET_CHANNEL_INFO();
                }
                Pointer pChannelInfo = config.stuSipServerInfo[i].pstuChannelInfo;
                ToolKits.GetPointerDataToStructArr(pChannelInfo, channelInfos);

                for (int j = 0; j < retChannelInfoCount; j++) {
                    String channelID = new String(channelInfos[j].szID, encode);
                    String alarmLevel = String.valueOf(channelInfos[j].nAlarmLevel);

                    info.append(String.format("    >>> 第【%d】个通道信息详情 <<<", j + 1)).append("\n")
                            .append(String.format("    %s: ", "通道编号")).append(channelID).append("\n")
                            .append(String.format("    %s: ", "报警级别 范围 [1,6]")).append(alarmLevel).append("\n");
                }
            } else {
                info.append("    不存在通道信息").append("\n");
            }

            info.append("    ------ 报警通道信息 ------").append("\n");

            int retAlarmInfoCount = config.stuSipServerInfo[i].nRetAlarmInfoNum;
            if (retAlarmInfoCount != 0) {
                NET_ALARM_INFO[] alarmInfos = new NET_ALARM_INFO[retAlarmInfoCount];
                for (int j = 0; j < retAlarmInfoCount; j++) {
                    alarmInfos[j] = new NET_ALARM_INFO();
                }
                Pointer pAlarmInfo = config.stuSipServerInfo[i].pstuAlarmInfo;
                ToolKits.GetPointerDataToStructArr(pAlarmInfo, alarmInfos);

                for (int j = 0; j < retAlarmInfoCount; j++) {
                    String channelID = new String(alarmInfos[j].szID, encode);
                    String alarmLevel = String.valueOf(alarmInfos[j].nAlarmLevel);

                    info.append(String.format("    >>> 第【%d】个报警通道信息详情 <<<", j + 1)).append("\n")
                            .append(String.format("    %s: ", "通道编号")).append(channelID).append("\n")
                            .append(String.format("    %s: ", "报警级别 范围 [1,6]")).append(alarmLevel).append("\n");
                }
            } else {
                info.append("    不存在报警通道信息").append("\n");
            }

            info.append("    ------ 音频输出通道信息 ------").append("\n");

            int retAudioOutputChnInfoCount = config.stuSipServerInfo[i].nRetAudioOutputChnInfoNum;
            if (retAudioOutputChnInfoCount != 0) {
                NET_AUDIO_OUTPUT_CHANNEL_INFO[] audioChannelInfos = new NET_AUDIO_OUTPUT_CHANNEL_INFO[retAudioOutputChnInfoCount];
                for (int j = 0; j < retAudioOutputChnInfoCount; j++) {
                    audioChannelInfos[j] = new NET_AUDIO_OUTPUT_CHANNEL_INFO();
                }
                Pointer pAudioOutputChnInfo = config.stuSipServerInfo[i].pstuAudioOutputChnInfo;
                ToolKits.GetPointerDataToStructArr(pAudioOutputChnInfo, audioChannelInfos);

                for (int j = 0; j < retAudioOutputChnInfoCount; j++) {
                    String channelID = new String(audioChannelInfos[j].szID, encode);

                    info.append(String.format("    >>> 第【%d】个音频输出通道信息详情 <<<", j + 1)).append("\n")
                            .append(String.format("    %s: ", "通道编号")).append(channelID).append("\n");
                }
            } else {
                info.append("    不存在音频输出通道信息").append("\n");
            }
        }
        System.out.println(info.toString());
    }

    /**
     * 下发 公安一所平台接入配置(国标服务端)
     * Demo 里修改了配置组的第一个配置
     */
    public void setVspGaysServerConfig() {

        // 我们推荐修改配置的方式都是 获取->修改->下发 这样的流程
        // 配置下发载体的结构体越复杂越得这么做，特别是很多早期的结构体，往往承担着复数个配置的收发任务，缺少关键字段容易引起设备异常

        //////////////////////////////////////// 获取配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        NET_CFG_VSP_GAYS_SERVER_INFO config = new NET_CFG_VSP_GAYS_SERVER_INFO();
        config.nSipServerInfoNum = 5;  // 获取配置的数量 填最大值就行了
        int nChannelNum = initModule.deviceInfo.getByChanNum();

        for (int i = 0; i < config.nSipServerInfoNum; i++) {

            config.stuSipServerInfo[i].nChannelInfoNum = nChannelNum;         // 这里填设备通道数就行
            config.stuSipServerInfo[i].pstuChannelInfo = new Memory((new NET_CHANNEL_INFO().size()) * nChannelNum);

            config.stuSipServerInfo[i].nAlarmInfoNum = nChannelNum;           // 这里也填设备通道数就行
            config.stuSipServerInfo[i].pstuAlarmInfo = new Memory((new NET_ALARM_INFO().size()) * nChannelNum);

            config.stuSipServerInfo[i].nAudioOutputChnInfoNum = nChannelNum;  // 这里还是填设备通道数
            config.stuSipServerInfo[i].pstuAudioOutputChnInfo = new Memory((new NET_AUDIO_OUTPUT_CHANNEL_INFO().size()) * nChannelNum);

        }
        config = (NET_CFG_VSP_GAYS_SERVER_INFO) configModule.getConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VSP_GAYS_SERVER, config, -1);
        if (config == null) {
            System.err.println("获取公安一所平台接入配置(国标服务端)失败:" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("获取公安一所平台接入配置(国标服务端)成功");

        //////////////////////////////////////// 修改配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        // Demo这里只修改第1个配置 其他的配置原样下发
        config.nSipServerInfoNum = config.nRetSipServerInfoNum;       // 下发配置数量 设定成和接收时的一样

        NET_SIP_SERVER_INFO sipServerInfo = config.stuSipServerInfo[0];

        sipServerInfo.emServerOption = EM_SERVER_OPTION.EM_SERVER_OPTION_GB28181.getValue();  // 启用 28181 接入方式

        byte[] bSipSvrId = "34020000002000000001".getBytes(encode);                           // SIP服务器编号
        System.arraycopy(bSipSvrId, 0, sipServerInfo.szSipSvrId, 0, bSipSvrId.length);

        byte[] bDomain = "3402000000".getBytes(encode);                                       // SIP域
        System.arraycopy(bDomain, 0, sipServerInfo.szDomain, 0, bDomain.length);

        byte[] bSipSvrIp = "192.168.1.112".getBytes(encode);                                  // SIP服务器 IP
        System.arraycopy(bSipSvrIp, 0, sipServerInfo.szSipSvrIp, 0, bSipSvrIp.length);

        byte[] bDeviceId = "34020000001320000001".getBytes(encode);                           // 设备编号
        System.arraycopy(bDeviceId, 0, sipServerInfo.szDeviceId, 0, bDeviceId.length);

        ///// 注意: sdk虽然会发送修改后的密码，但可能该设置是无效的 这点请和产品、设备研发提前确认下 不同型号设备在实现上可能不同
        ///// 因为配置是明文发送的 sdk拿到的密码大多时候是 ****** 这样的字符串 看不到真实密码 当然密码也确实不能暴露在TCP报文中
        byte[] bPassword = "helloWorld".getBytes(encode);                                     // 注册密码
        System.arraycopy(bPassword, 0, sipServerInfo.szPassword, 0, bPassword.length);

        sipServerInfo.nLocalSipPort = 5060;                                                   // 本地SIP服务端口
        sipServerInfo.nSipSvrPort = 5060;                                                     // SIP服务器端口
        sipServerInfo.nSipRegExpires = 3600;                                                  // 注册有效期 单位秒
        sipServerInfo.nRegInterval = 60;                                                      // 注册失败后重注册间隔 单位秒
        sipServerInfo.nKeepAliveCircle = 60;                                                  // 心跳周期 单位秒
        sipServerInfo.nMaxTimeoutTimes = 3;                                                   // 最大心跳超时次数

        byte[] bCivilCode = "340200".getBytes(encode);                                        // 行政区划代码
        System.arraycopy(bCivilCode, 0, sipServerInfo.szCivilCode, 0, bCivilCode.length);

        byte[] bInterVideoID = "000001019".getBytes(encode);                                  // 接入模块识别码
        System.arraycopy(bInterVideoID, 0, sipServerInfo.szIntervideoID, 0, bInterVideoID.length);

        // Demo这里通道相关的信息都只修改第1个

        // 通道信息
        sipServerInfo.nChannelInfoNum = sipServerInfo.nRetChannelInfoNum;  // 下发配置数量 设定成和接收的一样
        NET_CHANNEL_INFO[] channelInfos = new NET_CHANNEL_INFO[sipServerInfo.nRetChannelInfoNum];  // 创建和数量相同的结构体组
        for (int i = 0; i < sipServerInfo.nRetChannelInfoNum; i++) {
            channelInfos[i] = new NET_CHANNEL_INFO();    // 结构体组必须先分配内存
        }
        if (sipServerInfo.nRetChannelInfoNum < nChannelNum) {
            // 如果获取配置时 实际获取量比设定的小 那直接 GetPointerDataToStructArr 全拷贝就会把指针数据拷贝到未知内存
            // 解决的办法是 新建一个和获取量大小相同的新指针 复制数据后替换掉原来的
            int size = (channelInfos[0].size()) * sipServerInfo.nRetChannelInfoNum;
            Pointer newStuChannelInfo = new Memory(size);
            newStuChannelInfo.write(0, sipServerInfo.pstuChannelInfo.getByteArray(0, size), 0, size); // 复制数据
            sipServerInfo.pstuChannelInfo = newStuChannelInfo;  // 替换原指针
        }
        ToolKits.GetPointerDataToStructArr(sipServerInfo.pstuChannelInfo, channelInfos); // 把指针数据拷贝到结构体组里

        // 现在修改它的第1个数据
        byte[] bChannelID = "34020000001310000001".getBytes(encode);
        System.arraycopy(bChannelID, 0, channelInfos[0].szID, 0, bChannelID.length);
        channelInfos[0].nAlarmLevel = 1;

        ToolKits.SetStructArrToPointerData(channelInfos, sipServerInfo.pstuChannelInfo); // 再把修改了的数据拷贝回去

        // 报警通道信息
        sipServerInfo.nAlarmInfoNum = sipServerInfo.nRetAlarmInfoNum;  // 下发配置数量 设定成和接收的一样;
        NET_ALARM_INFO[] alarmInfos = new NET_ALARM_INFO[sipServerInfo.nRetAlarmInfoNum];
        for (int i = 0; i < sipServerInfo.nRetAlarmInfoNum; i++) {
            alarmInfos[i] = new NET_ALARM_INFO();    // 结构体组必须先分配内存
        }
        if (sipServerInfo.nRetAlarmInfoNum < nChannelNum) {
            // 如果获取配置时 实际获取量比设定的小 那直接 GetPointerDataToStructArr 全拷贝就会把指针数据拷贝到未知内存
            // 解决的办法是 新建一个和获取量大小相同的新指针 复制数据后替换掉原来的
            int size = (alarmInfos[0].size()) * sipServerInfo.nRetAlarmInfoNum;
            Pointer newStuAlarmInfo = new Memory(size);
            newStuAlarmInfo.write(0, sipServerInfo.pstuAlarmInfo.getByteArray(0, size), 0, size); // 复制数据
            sipServerInfo.pstuAlarmInfo = newStuAlarmInfo;  // 替换原指针
        }
        ToolKits.GetPointerDataToStructArr(sipServerInfo.pstuAlarmInfo, alarmInfos); // 把指针数据拷贝到结构体组里

        // 现在修改它的第1个数据
        byte[] bAlarmChannelID = "34020000002000000001".getBytes(encode);
        System.arraycopy(bAlarmChannelID, 0, alarmInfos[0].szID, 0, bAlarmChannelID.length);
        alarmInfos[0].nAlarmLevel = 1;

        ToolKits.SetStructArrToPointerData(alarmInfos, sipServerInfo.pstuAlarmInfo); // 再把修改了的数据拷贝回去

        // 音频输出通道信息
        sipServerInfo.nAudioOutputChnInfoNum = sipServerInfo.nRetAudioOutputChnInfoNum; // 下发配置数量 设定成和接收的一样;
        NET_AUDIO_OUTPUT_CHANNEL_INFO[] audioOutputChannelInfos = new NET_AUDIO_OUTPUT_CHANNEL_INFO[sipServerInfo.nRetAudioOutputChnInfoNum];
        for (int i = 0; i < sipServerInfo.nRetAudioOutputChnInfoNum; i++) {
            audioOutputChannelInfos[i] = new NET_AUDIO_OUTPUT_CHANNEL_INFO();    // 结构体组必须先分配内存
        }
        if (sipServerInfo.nRetAudioOutputChnInfoNum < nChannelNum) {
            // 如果获取配置时 实际获取量比设定的小 那直接 GetPointerDataToStructArr 全拷贝就会把指针数据拷贝到未知内存
            // 解决的办法是 新建一个和获取量大小相同的新指针 复制数据后替换掉原来的
            int size = (alarmInfos[0].size()) * sipServerInfo.nRetAudioOutputChnInfoNum;
            Pointer newStuAudioOutputInfo = new Memory(size);
            newStuAudioOutputInfo.write(0, sipServerInfo.pstuAudioOutputChnInfo.getByteArray(0, size), 0, size); // 复制数据
            sipServerInfo.pstuAudioOutputChnInfo = newStuAudioOutputInfo;  // 替换原指针
        }
        ToolKits.GetPointerDataToStructArr(sipServerInfo.pstuAudioOutputChnInfo, audioOutputChannelInfos); // 把指针数据拷贝到结构体组里

        // 现在修改它的第1个数据
        byte[] bAudioOutputChannelID = "34020000002000000001".getBytes(encode);
        System.arraycopy(bAudioOutputChannelID, 0, audioOutputChannelInfos[0].szID, 0, bAudioOutputChannelID.length);

        ToolKits.SetStructArrToPointerData(audioOutputChannelInfos, sipServerInfo.pstuAudioOutputChnInfo); // 再把修改了的数据拷贝回去

        //////////////////////////////////////// 下发配置 /////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////

        boolean ret = configModule.setConfig(
                loginHandler,                                         // 登录句柄
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VSP_GAYS_SERVER,   // 枚举->公安平台接入配置(28181国标服务端)
                config,                                               // 配置结构体
                -1                                            // 通道号 固定-1
        );
        if (!ret) {
            System.err.println("公安一所平台接入配置(国标服务端)下发失败:" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("公安一所平台接入配置(国标服务端)下发成功");
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
        menu.addItem(new CaseMenu.Item(this, "获取 公安一所平台接入配置(国标服务端)", "getVspGaysServerConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发 公安一所平台接入配置(国标服务端)", "setVspGaysServerConfig"));
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
    // private String m_strIp = "172.8.2.7";
    private String m_strIp = "172.23.12.112";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    /////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ThirdPartyConfiguration demo = new ThirdPartyConfiguration();

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
