package com.netsdk.demo.thermalImaging;

import com.netsdk.demo.event.DeviceModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.MessCallBack;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/7 9:56
 */
public class ThermalRuleConfig {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

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
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
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

    //////////////////////////////////////// 订阅事件/退订 ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private static int queryChannel = 1;

    private static NetSDKLib.CFG_RADIOMETRY_RULE_INFO config = new NetSDKLib.CFG_RADIOMETRY_RULE_INFO();

    public void SelectChannel(){

        Scanner sc = new Scanner(System.in);
        System.out.println("请输入热成像通道");
        queryChannel = sc.nextInt();

    }


    public void TestSetThermometryRule() {
        /**
         * 本示例，我会配置 5 个种类不同的规则配置
         * 配置内所有的参数都会填写，且尽可能的和网页匹配
         */

        // 配置前最好获取一下旧的配置，如果只是修改就不需要大费周章的所有参数都填一遍了
        String command = NetSDKLib.CFG_CMD_THERMOMETRY_RULE;

        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, queryChannel, command, config);

        if (!ret) {
            System.out.println("获取热成像测温规则配置失败：" + ToolKits.getErrorCode());
            return;
        }

        // 当然本Demo会把所有参数都填一遍以供参考

        config.nCount = 5;
        // 配置1，点规则配置
        this.SetThermometrySpotRule(0);
        this.SetThermometryLineRule(1);
        this.SetThermometryPolygonRule(2);
        this.SetThermometryRectangleRule(3);
        this.SetThermometryEllipseRule(4);

        boolean ret2 = ToolKits.SetDevConfig(m_hLoginHandle, queryChannel, command, config);
        if (!ret2) {
            System.err.println("设置热成像测温规则失败!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("设置热成像测温规则成功!");

    }

    // 点规则配置
    private void SetThermometrySpotRule(int i) {
        /**
         * 预置点编号: 0
         * 规则编号: 1
         * 规则名称: Spot1
         * 使能: 是
         * 测温模式: 点
         * 模式子类型: 多边形
         * 温度采样周期(秒): 3
         * //--->共有: 1 个测温点，测温点坐标详细:
         * 坐标[0]: [1459,0963]
         * //--->本地参数配置:
         * 是否启用本地配置: 是
         * 目标辐射系数: 0.97
         * 目标距离: 2
         * 目标反射温度: 25
         * //--->共有: 1 测温点报警设置:
         * 配置[0]:
         * 是否启用: 是
         * 报警唯一编号: 0
         * 测温报警结果类型: 具体值
         * 报警条件: 低于
         * 报警阈值温度: 20.0
         * 温度误差: 0.1
         * 阈值温度持续时间(秒): 30
         */

        // 通用配置
        config.stRule[i].nPresetId = 0;   // 枪机没有预置点的说法，固定为0
        config.stRule[i].nRuleId = i + 1;     // 规则编号,从 1 开始
        System.arraycopy("Spot1".getBytes(), 0, config.stRule[i].szName, 0, "Spot1".length());  // 规则名称
        config.stRule[i].bEnable = 1; // 0 false,1 true
        config.stRule[i].nMeterType = NetSDKLib.NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_SPOT;  // 点模式
        config.stRule[i].emAreaSubType = NetSDKLib.EM_CFG_AREA_SUBTYPE.EM_CFG_AREA_SUBTYPE_POLYGON;      // 点模式属于多边形
        config.stRule[i].nSamplePeriod = 3; //温度采样周期(秒)

        // 规则坐标点配置
        config.stRule[i].nCoordinateCnt = 1; // 点模式只有一个坐标
        config.stRule[i].stCoordinates[0].nX = 1459;
        config.stRule[i].stCoordinates[0].nY = 963;

        // 报警相关配置(本地参数)
        config.stRule[i].stLocalParameters.bEnable = 1;    // 0 false,1 true  不需要启用的话这里至 0
        config.stRule[i].stLocalParameters.fObjectEmissivity = 0.97f;   // 目标辐射系数
        config.stRule[i].stLocalParameters.nObjectDistance = 2; // 目标距离
        config.stRule[i].stLocalParameters.nRefalectedTemp = 25; // 目标反射温度

        // 报警相关配置(测温点报警) Demo里只配置一条
        config.stRule[i].nAlarmSettingCnt = 1;
        config.stRule[i].stAlarmSetting[0].bEnable = 1; // 0 false,1 true  不需要启用的话这里至 0
        // 点测温：具体值，
        // 线测温：最大, 最小, 平均
        // 区域测温：最大, 最小, 平均, 标准, 中间, ISO
        config.stRule[i].stAlarmSetting[0].nResultType = NetSDKLib.CFG_STATISTIC_TYPE.CFG_STATISTIC_TYPE_VAL; // 点测温固定具体值
        config.stRule[i].stAlarmSetting[0].nAlarmCondition = NetSDKLib.CFG_COMPARE_RESULT.CFG_COMPARE_RESULT_BELOW; //低于
        config.stRule[i].stAlarmSetting[0].fThreshold = 20.0f;  // 报警阈值温度
        config.stRule[i].stAlarmSetting[0].fHysteresis = 0.1f;  // 温度误差
        config.stRule[i].stAlarmSetting[0].nDuration = 30;      // 阈值温度持续时间(秒)
    }

    // 线规则配置
    private void SetThermometryLineRule(int i) {
        /**
         * 预置点编号: 0
         * 规则编号: 2
         * 规则名称: Line1
         * 使能: 是
         * 测温模式: 线
         * 模式子类型: 多边形
         * 温度采样周期(秒): 3
         * //--->共有: 2 个测温点，测温点坐标详细:
         * 坐标[0]: [3071,0963]
         * 坐标[1]: [6351,0938]
         * //--->本地参数配置:
         * 是否启用本地配置: 是
         * 目标辐射系数: 0.97
         * 目标距离: 2
         * 目标反射温度: 25
         * //--->共有: 1 测温点报警设置:
         * 配置[0]:
         * 是否启用: 是
         * 报警唯一编号: 0
         * 测温报警结果类型: 最大
         * 报警条件: 低于
         * 报警阈值温度: 20.0
         * 温度误差: 0.1
         * 阈值温度持续时间(秒): 30
         */

        // 通用配置
        config.stRule[i].nPresetId = 0;   // 枪机没有预置点的说法，固定为0
        config.stRule[i].nRuleId = i + 1;     // 规则编号,从 1 开始
        System.arraycopy("Line1".getBytes(), 0, config.stRule[i].szName, 0, "Line1".length());  // 规则名称
        config.stRule[i].bEnable = 1; // 0 false,1 true
        config.stRule[i].nMeterType = NetSDKLib.NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_LINE;  // 线模式
        config.stRule[i].emAreaSubType = NetSDKLib.EM_CFG_AREA_SUBTYPE.EM_CFG_AREA_SUBTYPE_POLYGON;      // 线模式属于多边形
        config.stRule[i].nSamplePeriod = 3; //温度采样周期(秒)

        // 规则坐标点配置 [3071,0963] [6351,0938]
        config.stRule[i].nCoordinateCnt = 2; // 线模式有两个坐标
        config.stRule[i].stCoordinates[0].nX = 3071;
        config.stRule[i].stCoordinates[0].nY = 963;
        config.stRule[i].stCoordinates[1].nX = 6351;
        config.stRule[i].stCoordinates[1].nY = 938;

        // 报警相关配置(本地参数)
        config.stRule[i].stLocalParameters.bEnable = 1;    // 0 false,1 true  不需要启用的话这里至 0
        config.stRule[i].stLocalParameters.fObjectEmissivity = 0.97f;   // 目标辐射系数
        config.stRule[i].stLocalParameters.nObjectDistance = 2; // 目标距离
        config.stRule[i].stLocalParameters.nRefalectedTemp = 25; // 目标反射温度

        // 报警相关配置(测温点报警) Demo里只配置一条
        config.stRule[i].nAlarmSettingCnt = 1;
        config.stRule[i].stAlarmSetting[0].bEnable = 1; // 0 false,1 true  不需要启用的话这里至 0
        // 点测温：具体值，
        // 线测温：最大, 最小, 平均
        // 区域测温：最大, 最小, 平均, 标准, 中间, ISO
        config.stRule[i].stAlarmSetting[0].nResultType = NetSDKLib.CFG_STATISTIC_TYPE.CFG_STATISTIC_TYPE_MAX; // 最大
        config.stRule[i].stAlarmSetting[0].nAlarmCondition = NetSDKLib.CFG_COMPARE_RESULT.CFG_COMPARE_RESULT_BELOW; // 低于
        config.stRule[i].stAlarmSetting[0].fThreshold = 20.0f;  // 报警阈值温度
        config.stRule[i].stAlarmSetting[0].fHysteresis = 0.1f;  // 温度误差
        config.stRule[i].stAlarmSetting[0].nDuration = 30;      // 阈值温度持续时间(秒)
    }

    // 多边形规则配置
    private void SetThermometryPolygonRule(int i) {
        /**
         * 预置点编号: 0
         * 规则编号: 3
         * 规则名称: Polygon1
         * 使能: 是
         * 测温模式: 区域
         * 模式子类型: 多边形
         * 温度采样周期(秒): 3
         * //--->共有: 5 个测温点，测温点坐标详细:
         * 坐标[0]: [1459,1978]
         * 坐标[1]: [6067,1978]
         * 坐标[2]: [6636,2941]
         * 坐标[3]: [2938,3727]
         * 坐标[4]: [0834,2738]
         * //--->本地参数配置:
         * 是否启用本地配置: 是
         * 目标辐射系数: 0.97
         * 目标距离: 2
         * 目标反射温度: 25
         * //--->共有: 1 测温点报警设置:
         * 配置[0]:
         * 是否启用: 是
         * 报警唯一编号: 0
         * 测温报警结果类型: 最大
         * 报警条件: 低于
         * 报警阈值温度: 20.0
         * 温度误差: 0.1
         * 阈值温度持续时间(秒): 30
         */

        // 通用配置
        config.stRule[i].nPresetId = 0;   // 枪机没有预置点的说法，固定为0
        config.stRule[i].nRuleId = i + 1;     // 规则编号,从 1 开始
        System.arraycopy("Polygon1".getBytes(), 0, config.stRule[i].szName, 0, "Polygon1".length());  // 规则名称
        config.stRule[i].bEnable = 1; // 0 false,1 true
        config.stRule[i].nMeterType = NetSDKLib.NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_AREA;  // 区域模式
        config.stRule[i].emAreaSubType = NetSDKLib.EM_CFG_AREA_SUBTYPE.EM_CFG_AREA_SUBTYPE_POLYGON;      // 多边形模式
        config.stRule[i].nSamplePeriod = 3; //温度采样周期(秒)

        // 规则坐标点配置 [1459,1978] [6067,1978] [6636,2941] [2938,3727] [0834,2738]
        config.stRule[i].nCoordinateCnt = 5; // 五个坐标
        config.stRule[i].stCoordinates[0].nX = 1459;
        config.stRule[i].stCoordinates[0].nY = 1978;
        config.stRule[i].stCoordinates[1].nX = 6067;
        config.stRule[i].stCoordinates[1].nY = 1978;
        config.stRule[i].stCoordinates[2].nX = 6636;
        config.stRule[i].stCoordinates[2].nY = 2941;
        config.stRule[i].stCoordinates[3].nX = 2938;
        config.stRule[i].stCoordinates[3].nY = 3727;
        config.stRule[i].stCoordinates[4].nX = 834;
        config.stRule[i].stCoordinates[4].nY = 2738;

        // 报警相关配置(本地参数)
        config.stRule[i].stLocalParameters.bEnable = 1;    // 0 false,1 true  不需要启用的话这里至 0
        config.stRule[i].stLocalParameters.fObjectEmissivity = 0.97f;   // 目标辐射系数
        config.stRule[i].stLocalParameters.nObjectDistance = 2; // 目标距离
        config.stRule[i].stLocalParameters.nRefalectedTemp = 25; // 目标反射温度

        // 报警相关配置(测温点报警) Demo里只配置一条
        config.stRule[i].nAlarmSettingCnt = 1;
        config.stRule[i].stAlarmSetting[0].bEnable = 1; // 0 false,1 true  不需要启用的话这里至 0
        // 点测温：具体值，
        // 线测温：最大, 最小, 平均
        // 区域测温：最大, 最小, 平均, 标准, 中间, ISO
        config.stRule[i].stAlarmSetting[0].nResultType = NetSDKLib.CFG_STATISTIC_TYPE.CFG_STATISTIC_TYPE_MAX; // 最大
        config.stRule[i].stAlarmSetting[0].nAlarmCondition = NetSDKLib.CFG_COMPARE_RESULT.CFG_COMPARE_RESULT_BELOW; // 低于
        config.stRule[i].stAlarmSetting[0].fThreshold = 20.0f;  // 报警阈值温度
        config.stRule[i].stAlarmSetting[0].fHysteresis = 0.1f;  // 温度误差
        config.stRule[i].stAlarmSetting[0].nDuration = 30;      // 阈值温度持续时间(秒)
    }

    // 方形规则配置
    private void SetThermometryRectangleRule(int i) {
        /**
         * 预置点编号: 0
         * 规则编号: 4
         * 规则名称: Rectangle1
         * 使能: 是
         * 测温模式: 区域
         * 模式子类型: 矩形
         * 温度采样周期(秒): 3
         * //--->共有: 4 个测温点，测温点坐标详细:
         * 坐标[0]: [0891,4361]
         * 坐标[1]: [2616,4361]
         * 坐标[2]: [2616,6720]
         * 坐标[3]: [0891,6720]
         * //--->本地参数配置:
         * 是否启用本地配置: 是
         * 目标辐射系数: 0.97
         * 目标距离: 2
         * 目标反射温度: 25
         * //--->共有: 1 测温点报警设置:
         * 配置[0]:
         * 是否启用: 是
         * 报警唯一编号: 0
         * 测温报警结果类型: 最大
         * 报警条件: 低于
         * 报警阈值温度: 20.0
         * 温度误差: 0.1
         * 阈值温度持续时间(秒): 30
         */

        // 通用配置
        config.stRule[i].nPresetId = 0;   // 枪机没有预置点的说法，固定为0
        config.stRule[i].nRuleId = i + 1;     // 规则编号,从 1 开始
        System.arraycopy("Rectangle1".getBytes(), 0, config.stRule[i].szName, 0, "Rectangle1".length());  // 规则名称
        config.stRule[i].bEnable = 1; // 0 false,1 true
        config.stRule[i].nMeterType = NetSDKLib.NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_AREA;  // 区域模式
        config.stRule[i].emAreaSubType = NetSDKLib.EM_CFG_AREA_SUBTYPE.EM_CFG_AREA_SUBTYPE_RECT;      // 方形模式
        config.stRule[i].nSamplePeriod = 3; //温度采样周期(秒)

        // 规则坐标点配置 [0891,4361] [2616,4361] [2616,6720] [0891,6720]
        config.stRule[i].nCoordinateCnt = 4; // 五个坐标
        config.stRule[i].stCoordinates[0].nX = 891;
        config.stRule[i].stCoordinates[0].nY = 4361;
        config.stRule[i].stCoordinates[1].nX = 2616;
        config.stRule[i].stCoordinates[1].nY = 4361;
        config.stRule[i].stCoordinates[2].nX = 2616;
        config.stRule[i].stCoordinates[2].nY = 6720;
        config.stRule[i].stCoordinates[3].nX = 891;
        config.stRule[i].stCoordinates[3].nY = 6720;

        // 报警相关配置(本地参数)
        config.stRule[i].stLocalParameters.bEnable = 1;    // 0 false,1 true  不需要启用的话这里至 0
        config.stRule[i].stLocalParameters.fObjectEmissivity = 0.97f;   // 目标辐射系数
        config.stRule[i].stLocalParameters.nObjectDistance = 2; // 目标距离
        config.stRule[i].stLocalParameters.nRefalectedTemp = 25; // 目标反射温度

        // 报警相关配置(测温点报警) Demo里只配置一条
        config.stRule[i].nAlarmSettingCnt = 1;
        config.stRule[i].stAlarmSetting[0].bEnable = 1; // 0 false,1 true  不需要启用的话这里至 0
        // 点测温：具体值，
        // 线测温：最大, 最小, 平均
        // 区域测温：最大, 最小, 平均, 标准, 中间, ISO
        config.stRule[i].stAlarmSetting[0].nResultType = NetSDKLib.CFG_STATISTIC_TYPE.CFG_STATISTIC_TYPE_MAX; // 最大
        config.stRule[i].stAlarmSetting[0].nAlarmCondition = NetSDKLib.CFG_COMPARE_RESULT.CFG_COMPARE_RESULT_BELOW; // 低于
        config.stRule[i].stAlarmSetting[0].fThreshold = 20.0f;  // 报警阈值温度
        config.stRule[i].stAlarmSetting[0].fHysteresis = 0.1f;  // 温度误差
        config.stRule[i].stAlarmSetting[0].nDuration = 30;      // 阈值温度持续时间(秒)
    }

    // 椭圆形规则配置
    private void SetThermometryEllipseRule(int i) {
        /**
         * 预置点编号: 0
         * 规则编号: 5
         * 规则名称: Ellipse1
         * 使能: 是
         * 测温模式: 区域
         * 模式子类型: 椭圆
         * 温度采样周期(秒): 3
         * //--->共有: 4 个测温点，测温点坐标详细:
         * 坐标[0]: [3697,4463]
         * 坐标[1]: [7072,4463]
         * 坐标[2]: [7072,6441]
         * 坐标[3]: [3697,6441]
         * //--->本地参数配置:
         * 是否启用本地配置: 是
         * 目标辐射系数: 0.97
         * 目标距离: 2
         * 目标反射温度: 25
         * //--->共有: 1 测温点报警设置:
         * 配置[0]:
         * 是否启用: 是
         * 报警唯一编号: 0
         * 测温报警结果类型: 最大
         * 报警条件: 低于
         * 报警阈值温度: 20.0
         * 温度误差: 0.1
         * 阈值温度持续时间(秒): 30
         */

        // 通用配置
        config.stRule[i].nPresetId = 0;   // 枪机没有预置点的说法，固定为0
        config.stRule[i].nRuleId = i + 1;     // 规则编号,从 1 开始
        System.arraycopy("Ellipse1".getBytes(), 0, config.stRule[i].szName, 0, "Ellipse1".length());  // 规则名称
        config.stRule[i].bEnable = 1; // 0 false,1 true
        config.stRule[i].nMeterType = NetSDKLib.NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_AREA;  // 区域模式
        config.stRule[i].emAreaSubType = NetSDKLib.EM_CFG_AREA_SUBTYPE.EM_CFG_AREA_SUBTYPE_ELLIPSE;      // 椭圆形模式
        config.stRule[i].nSamplePeriod = 3; //温度采样周期(秒)

        // 规则坐标点配置 [3697,4463] [7072,4463] [7072,6441] [3697,6441]
        config.stRule[i].nCoordinateCnt = 4; // 五个坐标
        config.stRule[i].stCoordinates[0].nX = 3697;
        config.stRule[i].stCoordinates[0].nY = 4463;
        config.stRule[i].stCoordinates[1].nX = 7072;
        config.stRule[i].stCoordinates[1].nY = 4463;
        config.stRule[i].stCoordinates[2].nX = 7072;
        config.stRule[i].stCoordinates[2].nY = 6441;
        config.stRule[i].stCoordinates[3].nX = 3697;
        config.stRule[i].stCoordinates[3].nY = 6441;

        // 报警相关配置(本地参数)
        config.stRule[i].stLocalParameters.bEnable = 1;    // 0 false,1 true  不需要启用的话这里至 0
        config.stRule[i].stLocalParameters.fObjectEmissivity = 0.97f;   // 目标辐射系数
        config.stRule[i].stLocalParameters.nObjectDistance = 2; // 目标距离
        config.stRule[i].stLocalParameters.nRefalectedTemp = 25; // 目标反射温度

        // 报警相关配置(测温点报警) Demo里只配置一条
        config.stRule[i].nAlarmSettingCnt = 1;
        config.stRule[i].stAlarmSetting[0].bEnable = 1; // 0 false,1 true  不需要启用的话这里至 0
        // 点测温：具体值，
        // 线测温：最大, 最小, 平均
        // 区域测温：最大, 最小, 平均, 标准, 中间, ISO
        config.stRule[i].stAlarmSetting[0].nResultType = NetSDKLib.CFG_STATISTIC_TYPE.CFG_STATISTIC_TYPE_MAX; // 最大
        config.stRule[i].stAlarmSetting[0].nAlarmCondition = NetSDKLib.CFG_COMPARE_RESULT.CFG_COMPARE_RESULT_BELOW; // 低于
        config.stRule[i].stAlarmSetting[0].fThreshold = 20.0f;  // 报警阈值温度
        config.stRule[i].stAlarmSetting[0].fHysteresis = 0.1f;  // 温度误差
        config.stRule[i].stAlarmSetting[0].nDuration = 30;      // 阈值温度持续时间(秒)
    }

    /**
     * 热成像测温规则配置
     */
    public void GetThermometryRule() {

        String command = NetSDKLib.CFG_CMD_THERMOMETRY_RULE;

        boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, queryChannel, command, config);

        if (!ret) {
            System.out.println("获取热成像测温规则配置失败：" + ToolKits.getErrorCode());
            return;
        }

        StringBuilder builder = new StringBuilder().append("\n<<-----当前热成像配置----->>");
        for (int i = 0; i < config.nCount; ++i) {

            try {  // 预置点是球机的概念(指球机运动到的某一个精确点的编号)，BF系列是枪机没有这个设定，所以固定是 0
                builder.append("\n").append("///----->规则配置[").append(i).append("]: ").append("\n")
                        .append("预置点编号: ").append(config.stRule[i].nPresetId).append("\n")
                        .append("规则编号: ").append(config.stRule[i].nRuleId).append("\n")
                        .append("规则名称: ").append(new String(config.stRule[i].szName, encode).trim()).append("\n")
                        .append("使能: ").append(config.stRule[i].bEnable == 0 ? "否" : "是").append("\n")
                        .append("测温模式: ").append(ThermalUtils.ArrMeterType.getNoteByValue(config.stRule[i].nMeterType)).append("\n")
                        .append("模式子类型: ").append(ThermalUtils.AreaSubtype.getNoteByValue(config.stRule[i].emAreaSubType)).append("\n")
                        .append("温度采样周期(秒): ").append(config.stRule[i].nSamplePeriod).append("\n");

                // 规则坐标点配置
                builder.append("//--->共有: ").append(config.stRule[i].nCoordinateCnt).append(" 个测温点，测温点坐标详细: ").append("\n");
                for (int j = 0; j < config.stRule[i].nCoordinateCnt; j++) {
                    builder.append("坐标[").append(j).append("]: ")
                            .append(String.format("[%04d,%04d]", config.stRule[i].stCoordinates[j].nX, config.stRule[i].stCoordinates[j].nY)).append("\n");
                }
                // 报警相关配置(本地参数)
                builder.append("//--->本地参数配置: ").append("\n")
                        .append("是否启用本地配置: ").append(config.stRule[i].stLocalParameters.bEnable == 0 ? "否" : "是").append("\n")
                        .append("目标辐射系数: ").append(config.stRule[i].stLocalParameters.fObjectEmissivity).append("\n")
                        .append("目标距离: ").append(config.stRule[i].stLocalParameters.nObjectDistance).append("\n")
                        .append("目标反射温度: ").append(config.stRule[i].stLocalParameters.nRefalectedTemp).append("\n");

                // 报警相关配置(测温点报警)
                builder.append("//--->共有: ").append(config.stRule[i].nAlarmSettingCnt).append(" 测温点报警设置: ").append("\n");
                for (int j = 0; j < config.stRule[i].nAlarmSettingCnt; j++) {
                    builder.append("配置[").append(j).append("]: ").append("\n")
                            .append("是否启用: ").append(config.stRule[i].stAlarmSetting[j].bEnable == 0 ? "否" : "是").append("\n")
                            .append("报警唯一编号: ").append(config.stRule[i].stAlarmSetting[j].nId).append("\n")
                            .append("测温报警结果类型: ").append(ThermalUtils.AlarmType.getNoteByValue(config.stRule[i].stAlarmSetting[j].nResultType)).append("\n")
                            .append("报警条件: ").append(ThermalUtils.AlarmResultType.getNoteByValue(config.stRule[i].stAlarmSetting[j].nAlarmCondition)).append("\n")
                            .append("报警阈值温度: ").append(config.stRule[i].stAlarmSetting[j].fThreshold).append("\n")
                            .append("温度误差: ").append(config.stRule[i].stAlarmSetting[j].fHysteresis).append("\n")
                            .append("阈值温度持续时间(秒): ").append(config.stRule[i].stAlarmSetting[j].nDuration).append("\n");
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(builder.toString());
    }

    ////////////////////////////////////// 测温事件报警 /////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////

    DeviceModule moudle = new DeviceModule();

    /**
     * 监听事件
     */
    public void StartListen() {
        moudle.startListen(m_hLoginHandle);
    }

    /**
     * 停止监听事件
     */
    public void StopListen() {
        moudle.stopListen(m_hLoginHandle);
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        ThermalUtils.Init();         // 初始化SDK库
        DeviceModule.setDVRMessCallBack(MessCallBack.getInstance());  // 设置报警回调
        this.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "选择通道", "SelectChannel"));
        menu.addItem(new CaseMenu.Item(this, "获取配置", "GetThermometryRule"));
        menu.addItem(new CaseMenu.Item(this, "测试设置配置", "TestSetThermometryRule"));
        menu.addItem(new CaseMenu.Item(this, "测温点报警订阅", "StartListen"));
        menu.addItem(new CaseMenu.Item(this, "测温点报警退订", "StopListen"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.StopListen();
        this.logOut();  // 退出
        System.out.println("See You...");

        ThermalUtils.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_strIpAddr = "172.32.100.63";  // 相机
    private String m_strIpAddr = "172.11.2.5";  // NVR
//    private String m_strIpAddr = "172.32.101.197";  // NVR
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";     // 模拟器
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ThermalRuleConfig demo = new ThermalRuleConfig();

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
