package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.demo.customize.JordanPSD.module.ConfigModule;
import com.netsdk.demo.customize.JordanPSD.module.DeviceControlModule;
import com.netsdk.demo.customize.JordanPSD.module.LogonModule;
import com.netsdk.demo.customize.JordanPSD.module.SdkUtilModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;
import java.util.Scanner;

import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ENCODE_VIDEO;

/**
 * @author 47040
 * @since Created at 2021/5/25 10:52
 */
public class DemoMPT {

    // 设备信息
    NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 登录句柄
    NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);

    // 跨平台编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode());

    // 当前通道
    int channel = 0;

    public void setCurrChannel() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入通道号");
        channel = sc.nextInt();
        System.out.println("当前通道为:" + channel);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // 需求点一：平台远程设置设备的参数，包括分辨率，帧率，码率
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取远程设备编码配置
     * 二代协议
     * Demo 此处以获取主码流配置为例
     */
    public void getVideoEncode() {
        final int cfgCmd = NET_EM_CFG_ENCODE_VIDEO;

        NET_ENCODE_VIDEO_INFO stuInfo = new NET_ENCODE_VIDEO_INFO();
        stuInfo.emFormatType = NET_EM_FORMAT_TYPE.EM_FORMAT_MAIN_NORMAL.getValue();     // 主码流

        boolean ret = ConfigModule.GetConfig(m_hLoginHandle, cfgCmd, channel, stuInfo, 5000);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        // 打印看一下

        StringBuilder info = new StringBuilder()
                .append(String.format("SN:%s, Chn:%03d, 主码流配置:\n", new String(m_stDeviceInfo.sSerialNumber, encode).trim(), channel));
        info.append(String.format(
                " bEnable:%d, \n" +
                        " emFormatType:%d, emCompression:%d\n" +
                        " nHeight:%d, nWidth:%d \n" +
                        " emBitRateControl:%d, nBitRate:%d\n" +
                        " nFrameRate:%f, nIFrameInterval:%d\n" +
                        " emImageQuality:%d",
                stuInfo.bVideoEnable,
                stuInfo.emFormatType,
                stuInfo.emCompression,
                stuInfo.nHeight,
                stuInfo.nWidth,
                stuInfo.emBitRateControl,
                stuInfo.nBitRate,
                stuInfo.nFrameRate,
                stuInfo.nIFrameInterval,
                stuInfo.emImageQuality));
        System.out.println(info.toString());
    }

    /**
     * 下发远程设备编码配置
     * 二代协议
     * Demo 此处以获取主码流配置为例:
     * 分辨率:1920x1080 码率:2048 帧率:25.0 I帧间隔:50
     * <p>
     * 注意:
     * 1) I帧以 2s 一个为宜 所以 I帧间隔我们建议同步设置为 帧率 x2
     * 2) 修改配置的原则是: 获取->修改->下发 这样可以避免关键字段缺省
     * 3) 并不是任何配置都可以下发成功, 能力集请参考三代协议的接口
     */
    public void setVideoEncode() {

        ////////////////////////////// 获取配置 //////////////////////////////
        /////////////////////////////////////////////////////////////////////

        final int cfgCmd = NET_EM_CFG_ENCODE_VIDEO;

        NET_ENCODE_VIDEO_INFO stuInfo = new NET_ENCODE_VIDEO_INFO();
        stuInfo.emFormatType = NET_EM_FORMAT_TYPE.EM_FORMAT_MAIN_NORMAL.getValue();     // 主码流

        boolean ret = ConfigModule.GetConfig(m_hLoginHandle, cfgCmd, channel, stuInfo, 5000);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        ////////////////////////////// 修改配置 //////////////////////////////
        /////////////////////////////////////////////////////////////////////

        // 分辨率
        stuInfo.nWidth = 1920;
        stuInfo.nHeight = 1080;
        // 码流
        stuInfo.nBitRate = 2048;
        // 帧率和I帧间隔
        stuInfo.nFrameRate = 25.0f;
        stuInfo.nIFrameInterval = 50;

        ////////////////////////////// 下发配置 //////////////////////////////
        /////////////////////////////////////////////////////////////////////

        ret = ConfigModule.SetConfig(m_hLoginHandle, cfgCmd, channel, stuInfo, 5000);
        if (!ret) {
            System.err.println("下发远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("下发远程设备编码配置成功");
    }


    /**
     * 获取远程设备编码配置
     * 三代协议
     * 注：
     * 1) 我们建议把主码流配置看成一个整体, 即主码流组统一做相同配置
     * 2) 辅码流在不同设备上能力集差异较大 我们建议只使用辅码流 1, 即只关注辅码流组的第一个配置
     */
    public void getDeviceEncode() {
        final String cfgCmd = EM_NEW_CONFIG.CFG_CMD_ENCODE.getValue();
        NetSDKLib.CFG_ENCODE_INFO encodeInfo = new NetSDKLib.CFG_ENCODE_INFO();
        encodeInfo.nChannelID = channel;
        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, channel, cfgCmd, encodeInfo);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        // 打印下当前的设备参数: 主/辅 码流的分辨率, 帧率和码率
        StringBuilder info = new StringBuilder()
                .append(String.format("SN:%s, Chn:%03d, Encode Config:\n", new String(m_stDeviceInfo.sSerialNumber, encode).trim(), channel));
        // 主码流
        if (encodeInfo.nValidCountMainStream > 0) {
            info.append("Main Stream 主码流:\n")
                    .append("    Resolution 分辨率: ").append(String.format("( %04d x %04d )\n",
                    encodeInfo.stuMainStream[0].stuVideoFormat.nWidth,
                    encodeInfo.stuMainStream[0].stuVideoFormat.nHeight))
                    .append("    BitRate 视频码流(kbps): ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nBitRate).append("\n")
                    .append("    FrameRate 帧率: ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nFrameRate).append("\n")
                    .append("    IFrameInterval I帧间隔: ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nIFrameInterval).append("\n");
        }

        // 辅码流
        if (encodeInfo.nValidCountExtraStream > 0) {
            info.append("Sub Stream 辅码流(1):\n")
                    .append("    Resolution 分辨率: ").append(String.format("( %04d x %04d )\n",
                    encodeInfo.stuExtraStream[0].stuVideoFormat.nWidth,
                    encodeInfo.stuExtraStream[0].stuVideoFormat.nHeight))
                    .append("    BitRate 视频码流(kbps): ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nBitRate).append("\n")
                    .append("    FrameRate 帧率: ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nFrameRate).append("\n")
                    .append("    IFrameInterval I帧间隔: ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nIFrameInterval).append("\n");
        }

        System.out.println(info.toString());
    }

    /**
     * 设置远程设备编码配置
     * 三代协议
     * 注：
     * 1) 我们建议把主码流配置看成一个整体, 即主码流组统一做相同配置
     * 2) 辅码流在不同设备上差异较大 如果对接设备型号来源复杂的话我们建议只使用辅码流 1, 即只关注辅码流组的第一个配置
     * 3) I帧以 2s 一个为宜 所以 I帧间隔我们建议同步设置为 帧率 x2
     * 4) 修改配置的原则是: 获取->修改->下发 这样可以避免关键字段缺省
     * <p>
     * Demo 这里采用以下用例:
     * 主码流 分辨率:1920x1080 码率:2048 帧率:25.0 I帧间隔:50
     * 辅码流(1) 分辨率:704x576 码率:512 帧率:25.0 I帧间隔:50
     * <p>
     * 注:这个数据不是随便填的 设备端会逐一校验 所以简便的办法是照搬网页端的规则
     * 当然 最好的方法是是获取设备的相关能力集 请参考下一个示例
     */
    public void setDeviceEncode() {

        ////////////////////////////// 获取配置 ///////////////////////////////
        //////////////////////////////////////////////////////////////////////

        String cfgCmd = EM_NEW_CONFIG.CFG_CMD_ENCODE.getValue();
        NetSDKLib.CFG_ENCODE_INFO encodeInfo = new NetSDKLib.CFG_ENCODE_INFO();
        encodeInfo.nChannelID = channel;
        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, channel, cfgCmd, encodeInfo);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }

        ////////////////////////////// 修改配置 ///////////////////////////////
        //////////////////////////////////////////////////////////////////////

        // 主码流组统一做相同配置
        for (int i = 0; i < encodeInfo.nValidCountMainStream; i++) {
            // 分辨率:1920x1080
            encodeInfo.stuMainStream[i].stuVideoFormat.nWidth = 1920;
            encodeInfo.stuMainStream[i].stuVideoFormat.nHeight = 1080;
            // 码流:1280
            encodeInfo.stuMainStream[i].stuVideoFormat.nBitRate = 2048;
            // 帧率:25.0
            encodeInfo.stuMainStream[i].stuVideoFormat.nFrameRate = 25f;
            // I帧间隔 帧率x2
            encodeInfo.stuMainStream[i].stuVideoFormat.nIFrameInterval = 50;
        }

        // 辅码流只使用 辅码流1 也只修改它的配置 其他配置保持不变
        if (encodeInfo.nValidCountExtraStream > 0) {
            // 分辨率:1920x1080
            encodeInfo.stuExtraStream[0].stuVideoFormat.nWidth = 704;
            encodeInfo.stuExtraStream[0].stuVideoFormat.nHeight = 576;
            // 码流:512
            encodeInfo.stuExtraStream[0].stuVideoFormat.nBitRate = 512;
            // 帧率:25.0
            encodeInfo.stuExtraStream[0].stuVideoFormat.nFrameRate = 25.0f;
            // I帧间隔 帧率x2
            encodeInfo.stuExtraStream[0].stuVideoFormat.nIFrameInterval = 50;
        }

        ////////////////////////////// 下发配置 ///////////////////////////////
        //////////////////////////////////////////////////////////////////////

        ret = ToolKits.SetDevConfig(m_hLoginHandle, channel, cfgCmd, encodeInfo);
        if (!ret) {
            System.err.println("下发远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("下发远程设备编码配置成功");
    }

    /**
     * 依据当前配置 获取本编码格式下对应的能力集
     * 三代协议
     * Demo 这里以 主码流 为例
     */
    public void getEncodeCapWithConfig() {

        //////////////////////// 获取配置 /////////////////////////
        //////////////////////////////////////////////////////////
        String cfgCmd = EM_NEW_CONFIG.CFG_CMD_ENCODE.getValue();
        NetSDKLib.CFG_ENCODE_INFO encodeInfo = new NetSDKLib.CFG_ENCODE_INFO();
        encodeInfo.nChannelID = channel;
        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, channel, cfgCmd, encodeInfo);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        ////////////////// 获取能力集 指定本编码格式///////////////////
        ////////////////////////////////////////////////////////////

        int capCmd = GetDevCaps_Type.NET_ENCODE_CFG_CAPS.getType();
        NET_IN_ENCODE_CFG_CAPS stuIn = new NET_IN_ENCODE_CFG_CAPS();
        stuIn.nChannelId = channel;
        stuIn.nStreamType = 0;              // 主码流
        byte[] encodeJson = ConfigModule.PackageData2JsonByte(cfgCmd, 2 * 1024 * 1024, encodeInfo);
        if (encodeJson == null) {
            System.err.println("打包编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        Pointer pEncodeJson = new Memory(encodeJson.length);
        pEncodeJson.clear(encodeJson.length);
        pEncodeJson.write(0, encodeJson, 0, encodeJson.length);
        stuIn.pchEncodeJson = pEncodeJson;

        NET_OUT_ENCODE_CFG_CAPS stuOut = new NET_OUT_ENCODE_CFG_CAPS();

        ret = ConfigModule.GetDeviceCapability(m_hLoginHandle, capCmd, stuIn, stuOut, 5000);
        if (!ret) {
            System.err.println("获取设备编码能力失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        // 打印展示一下 以第一个配置为例
        NET_STREAM_CFG_CAPS mainStreamCaps = stuOut.stuMainFormatCaps[0];

        // 指定编码格式后获取能力集时 abIndivResolution = 0
        StringBuilder info = new StringBuilder()
                .append(String.format("SN:%s, Chn:%03d, 主码流能力集:\n", new String(m_stDeviceInfo.sSerialNumber, encode).trim(), channel));

        info.append("  当前能力集所属编码: ").append(
                CFG_VIDEO_COMPRESSION.getEnum(encodeInfo.stuMainStream[0].stuVideoFormat.emCompression).getNote()).append("\n");

        // 注意: 码流区间并不意味着可以在区间里随便填 必须填 STREAM_RATE_TYPE 内的符合条件的枚举值
        info.append("  码流范围(kbps): ").append(String.format("( %04d ~ %04d )\n",
                mainStreamCaps.nMinBitRateOptions, mainStreamCaps.nMaxBitRateOptions));

        // 大部分设备这个字段都是有值的
        if (mainStreamCaps.nFPSMax != 0)
            info.append(" 帧率最大值: ").append(mainStreamCaps.nFPSMax).append("\n");

        info.append(" 分辨率支持列表: \n");
        for (int i = 0; i < mainStreamCaps.nResolutionTypeNum; i++) {
            info.append("    分辨率: ").append(String.format("( %04d x %04d )\n",
                    mainStreamCaps.stuResolutionTypes[i].snWidth,
                    mainStreamCaps.stuResolutionTypes[i].snHight));
            if (mainStreamCaps.nFPSMax == 0) {
                info.append("      帧率: ").append(mainStreamCaps.nResolutionFPSMax[i]).append("\n");
            }
        }
        System.out.println(info.toString());
    }

    /////////////////////////////////////////////////////////////////////////
    // 需求点二：平台可以远程关闭设备
    /////////////////////////////////////////////////////////////////////////

    /**
     * 手动关机
     * <p>
     * 警告: 关机后 sdk 是无法把设备启动起来的
     * <p>
     * 注: 大部分设备对关机命令都有特殊逻辑
     * 比如很多设备需要连续两次关机命令才会真正关机 而且在上电状态下会直接重启 本质和Reboot命令没有区别
     */
    public void deviceShutdown() {

        Scanner sc = new Scanner(System.in);
        System.err.println("确定要关闭设备吗(y/n)? 关闭后sdk无法主动启动设备");
        String decision = sc.next().trim().toLowerCase();
        if (!decision.equals("y")) return;

        boolean ret = DeviceControlModule.DeviceShutdown(m_hLoginHandle);
        if (!ret) {
            System.err.println("设备关机命令下发失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("设备关机命令下发成功");
    }

    /**
     * 手动重启
     */
    public void deviceReboot() {

        boolean ret = DeviceControlModule.DeviceReboot(m_hLoginHandle);
        if (!ret) {
            System.err.println("设备重启命令下发失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("设备重启命令下发成功");
    }

    /**
     * 远程关机
     */
    public void RemoteShutdown() {
        boolean ret = DeviceControlModule.RemoteShutDown(m_hLoginHandle);
        if (!ret) {
            System.err.println("远程关机失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("远程关机成功");

    }

    /////////////////////////////////////////////////////////////////////////
    // 需求点二：平台可以远程关闭/开启设备录像
    /////////////////////////////////////////////////////////////////////////

    //  Todo 二代协议

    /**
     * 获取录像状态
     * 三代协议: "RecordMode"
     * 0-自动录像，1-手动录像，2-关闭录像
     * 需求里的开启即 1-手动录像 关闭即 2-关闭录像
     * 设置为 0 则使用 自动录像的配置规则 见下一个示例
     */
    public void getRecordMode() {
        String szCommand = NetSDKLib.CFG_CMD_RECORDMODE;   // "RecordMode"
        // m_stDeviceInfo.byChanNum为设备通道数
        NetSDKLib.AV_CFG_RecordMode[] recordModes = new NetSDKLib.AV_CFG_RecordMode[m_stDeviceInfo.byChanNum];
        for (int i = 0; i < recordModes.length; ++i) {
            recordModes[i] = new NetSDKLib.AV_CFG_RecordMode();
        }

        int retCount = ToolKits.GetDevConfig(m_hLoginHandle, -1, szCommand, recordModes);
        StringBuilder info = new StringBuilder()
                .append(String.format("SN:%s, 录像状态:\n", new String(m_stDeviceInfo.sSerialNumber, encode).trim()));
        for (int i = 0; i < retCount; i++) {
            info.append(" 通道:").append(i).append(" 主码流录像模式:")
                    .append(parseRecodeMode(recordModes[i].nMode)).append("; ")
                    .append(" 辅码流录像模式:").append(parseRecodeMode(recordModes[i].nModeExtra1)).append("\n");
        }
        System.out.println(info.toString());
    }

    private String parseRecodeMode(int type) {
        switch (type) {
            case 0:
                return "自动录像";
            case 1:
                return "手动录像";
            case 2:
                return "关闭录像";
            default:
                return "数据错误";
        }
    }


    /**
     * 设置录像状态 手动 开启/停止 录像
     * 三代协议: "RecordMode"
     * Demo 这里不修改内容 原样下发
     */
    public void setRecordMode() {

        //////////////////////// 获取配置 /////////////////////////
        //////////////////////////////////////////////////////////

        String szCommand = NetSDKLib.CFG_CMD_RECORDMODE;   // "RecordMode"
        // m_stDeviceInfo.byChanNum为设备通道数
        NetSDKLib.AV_CFG_RecordMode[] recordModes = new NetSDKLib.AV_CFG_RecordMode[m_stDeviceInfo.byChanNum];
        for (int i = 0; i < recordModes.length; ++i) {
            recordModes[i] = new NetSDKLib.AV_CFG_RecordMode();
        }

        int retCount = ToolKits.GetDevConfig(m_hLoginHandle, -1, szCommand, recordModes);
        if (retCount == -1) {
            System.err.println("获取录像模式失败: " + ENUMERROR.getErrorMessage());
            return;
        }

        //////////////////////// 下发配置 /////////////////////////
        //////////////////////////////////////////////////////////

        boolean bRet = ToolKits.SetDevConfig(m_hLoginHandle, -1, szCommand, recordModes);
        if (!bRet) {
            System.err.println("下发录像模式失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("下发录像模式成功");
    }

    /**
     * 获取手持设备 IR 开关配置
     */
    public void getInfraredSetConfig() {
        String strCmd = NetSDKLib.CFG_CMD_INFRARED_CONFIG;  // 配置类型枚举
        CFG_INFRARED_INFO config = new CFG_INFRARED_INFO();

        boolean bRet = ToolKits.GetDevConfig(m_hLoginHandle, -1, strCmd, config);
        if (!bRet) {
            System.err.println("获取IR开关配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("获取IR开关配置成功");

        // 打印出来看一看
        System.out.println(String.format("当前设备 SN:%s IR开关配置:\n" +
                        "    红外功能模式：%s\n" +
                        "    红外亮度(仅模式为开始时有效): %s",
                new String(m_stDeviceInfo.sSerialNumber, encode).trim(),
                EM_INFRARED_FUNC_MODE.getEnum(config.emInfraredMode).getNote(),
                EM_INFRARED_LIGHT_LEVEL.getEnum(config.emLightLevel).getNote()));
    }

    /**
     * 配置手持设备 IR 开关配置
     */
    public void setInfraredSetConfig() {
        String strCmd = NetSDKLib.CFG_CMD_INFRARED_CONFIG;  // 配置类型枚举
        CFG_INFRARED_INFO config = new CFG_INFRARED_INFO();

        // 先获取配置
        boolean bRet = ToolKits.GetDevConfig(m_hLoginHandle, -1, strCmd, config);
        if (!bRet) {
            System.err.println("获取IR开关配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("获取IR开关配置成功");

        // todo 修改
        config.emLightLevel = EM_INFRARED_LIGHT_LEVEL.EM_INFRARED_LIGHT_LEVEL_MEDIUM.getValue();

        // 下发配置
        bRet = ToolKits.SetDevConfig(m_hLoginHandle, -1, strCmd, config);
        if (!bRet) {
            System.err.println("下发IR开关配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("下发IR开关配置成功");
    }


    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化测试
     */
    public void InitTest() {
        SdkUtilModule.Init();  // 初始化SDK库
        m_hLoginHandle = LogonModule.TcpLoginWithHighSecurity(m_ipAddr, m_nPort, m_username, m_password, m_stDeviceInfo);  // 高安全登录
        if (m_hLoginHandle.intValue() == 0) {
            SdkUtilModule.cleanup();
        }
    }

    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "切换Demo选择通道", "setCurrChannel"));

        menu.addItem(new CaseMenu.Item(this, "获取设备编码配置(二代协议)", "getVideoEncode"));
        menu.addItem(new CaseMenu.Item(this, "下发设备编码配置(二代协议)", "setVideoEncode"));

        menu.addItem(new CaseMenu.Item(this, "获取设备编码配置(三代协议)", "getDeviceEncode"));
        menu.addItem(new CaseMenu.Item(this, "获取当前编码能力集(三代协议)", "getEncodeCapWithConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发设备编码配置(三代协议)", "setDeviceEncode"));

        menu.addItem(new CaseMenu.Item(this, "重启当前设备", "deviceReboot"));
        menu.addItem(new CaseMenu.Item(this, "关闭当前设备", "deviceShutdown"));
        menu.addItem(new CaseMenu.Item(this, "远程关机", "RemoteShutdown"));

        menu.addItem(new CaseMenu.Item(this, "获取录像模式", "getRecordMode"));
        menu.addItem(new CaseMenu.Item(this, "下发录像模式", "setRecordMode"));

        menu.addItem(new CaseMenu.Item(this, "获取手持设备IR(红外)配置", "getInfraredSetConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发手持设备IR(红外)配置", "setInfraredSetConfig"));

        menu.run();
    }

    /**
     * 结束测试
     */
    public void EndTest() {
        System.out.println("End Test");
        LogonModule.logout(m_hLoginHandle);  // 登出
        System.out.println("See You...");
        SdkUtilModule.cleanup();             // 清理资源
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_ipAddr = "172.8.2.99";
    private int m_nPort = 37777;
    private String m_username = "admin";
    private String m_password = "qqqqq1";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        DemoMPT demo = new DemoMPT();
        if (args.length == 4) {
            demo.m_ipAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_username = args[2];
            demo.m_password = args[3];
        }
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
