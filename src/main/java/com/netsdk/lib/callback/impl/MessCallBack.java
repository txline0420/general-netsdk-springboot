package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.constant.SDK_ALARM_Ex_TYPE;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

public class MessCallBack implements fMessCallBack {

    private MessCallBack() {
    }

    private static class CallBackHolder {
        private static final MessCallBack cb = new MessCallBack();
    }

    public static final MessCallBack getInstance() {
        return CallBackHolder.cb;
    }

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    @Override
    public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
                          int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
                          Pointer dwUser) {
        switch (lCommand) {
            /************************************************************
             *							门禁事件							*
             *************************************************************/
            case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: // 门禁事件
            {
                ALARM_ACCESS_CTL_EVENT_INFO msg = new ALARM_ACCESS_CTL_EVENT_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.println("【门禁事件】 " + new String(msg.szQRCode, "GBK"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            }
            case NetSDKLib.NET_ALARM_ACCESS_CTL_NOT_CLOSE: // 门禁未关事件详细信息
            {
                ALARM_ACCESS_CTL_NOT_CLOSE_INFO msg = new ALARM_ACCESS_CTL_NOT_CLOSE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.printf("【门禁未关事件详细信息】 时间:%s 门通道号:%d 门禁名称:%s 事件动作(0:开始 1:停止):%d \n",
                            msg.stuTime, msg.nDoor, new String(msg.szDoorName, "GBK").trim(), msg.nAction);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            case NetSDKLib.NET_MOTION_ALARM_EX: {
                byte[] alarm = new byte[dwBufLen];
                pStuEvent.read(0, alarm, 0, dwBufLen);
                for (int i = 0; i < dwBufLen; i++) {
                    if (alarm[i] == 1) {
                        System.out.println("通道[" + i + "]" + "动态检测报警:" + "开始");
                    } else {
                        System.out.println("通道[" + i + "]" + "动态检测报警:" + "结束");
                    }
                }
                break;
            }
            case NetSDKLib.NET_ALARM_ACCESS_CTL_BREAK_IN: // 闯入事件详细信息
            {
                ALARM_ACCESS_CTL_BREAK_IN_INFO msg = new ALARM_ACCESS_CTL_BREAK_IN_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.printf("【闯入事件详细信息】 时间:%s 门通道号:%d 门禁名称:%s \n",
                            msg.stuTime, msg.nDoor, new String(msg.szDoorName, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            case NetSDKLib.NET_ALARM_ACCESS_CTL_REPEAT_ENTER: // 反复进入事件详细信息
            {
                ALARM_ACCESS_CTL_REPEAT_ENTER_INFO msg = new ALARM_ACCESS_CTL_REPEAT_ENTER_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.printf("【反复进入事件详细信息】 时间:%s 门通道号:%d 门禁名称:%s 卡号:%s\n",
                            msg.stuTime, msg.nDoor, new String(msg.szDoorName, "GBK").trim(), new String(msg.szCardNo).trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            case NetSDKLib.NET_ALARM_ACCESS_CTL_DURESS: // 胁迫卡刷卡事件详细信息
            {
                ALARM_ACCESS_CTL_DURESS_INFO msg = new ALARM_ACCESS_CTL_DURESS_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.printf("【胁迫卡刷卡事件详细信息】 时间:%s 门通道号:%d 门禁名称:%s 胁迫卡号:%s\n",
                            msg.stuTime, msg.nDoor, new String(msg.szDoorName, "GBK").trim(), new String(msg.szCardNo).trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            case NetSDKLib.NET_ALARM_OPENDOORGROUP: // 多人组合开门事件
            {
                ALARM_OPEN_DOOR_GROUP_INFO msg = new ALARM_OPEN_DOOR_GROUP_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【多人组合开门事件】 时间:%s 门通道号:%d\n", msg.stuTime, msg.nChannelID);
                break;
            }
            case NetSDKLib.NET_ALARM_ACCESS_CTL_STATUS: // 门禁状态事件
            {
                ALARM_ACCESS_CTL_STATUS_INFO msg = new ALARM_ACCESS_CTL_STATUS_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【门禁状态事件】 时间:%s 门通道号:%d 门禁状态:%d\n", msg.stuTime, msg.nDoor, msg.emStatus);
                break;
            }
            case NetSDKLib.NET_ALARM_FINGER_PRINT: // 获取指纹事件
            {
                ALARM_CAPTURE_FINGER_PRINT_INFO msg = new ALARM_CAPTURE_FINGER_PRINT_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【获取指纹事件】 时间:%s 门通道号:%d 采集结果:%d\n", msg.stuTime, msg.nChannelID, msg.bCollectResult);
                break;
            }
            case NetSDKLib.NET_ALARM_QR_CODE_CHECK: // 二维码上报事件
            {
                ALARM_QR_CODE_CHECK_INFO msg = new ALARM_QR_CODE_CHECK_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                try {
                    System.out.printf("【二维码上报事件】 UTC:%s 二维码字符串:%s ",
                            msg.UTC, new String(msg.szQRCode, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            }
            /****************************************************************
             *							报警主机事件							*
             *****************************************************************/
            case NetSDKLib.NET_ALARM_POWERFAULT: // 电源故障事件
            {
                ALARM_POWERFAULT_INFO msg = new ALARM_POWERFAULT_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【电源故障事件】 时间:%s 电源类型:%d 电源故障事件:%d 事件动作(0:开始 1:停止):%d\n",
                        msg.stuTime, msg.emPowerType, msg.emPowerFaultEvent, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_BATTERYLOWPOWER: // 蓄电池低电压事件
            {
                ALARM_BATTERYLOWPOWER_INFO msg = new ALARM_BATTERYLOWPOWER_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【蓄电池低电压事件】 时间:%s 通道号:%d 剩余电量百分比:%d 事件动作(0:开始 1:停止):%d\n",
                        msg.stTime, msg.nChannelID, msg.nBatteryLeft, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_ARMMODE_CHANGE_EVENT: // 设备布防模式变化事件
            {
                ALARM_ARMMODE_CHANGE_INFO msg = new ALARM_ARMMODE_CHANGE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【设备布防模式变化事件】 时间:%s 变化后的状态:%d ID号:%d 用户ID:%d\n",
                        msg.stuTime, msg.bArm, msg.dwID, msg.nUserCode);
                break;
            }
            case NetSDKLib.NET_ALARM_BYPASSMODE_CHANGE_EVENT: // 防区旁路状态变化事件
            {
                ALARM_BYPASSMODE_CHANGE_INFO msg = new ALARM_BYPASSMODE_CHANGE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【防区旁路状态变化事件】 时间:%s 通道号:%d 变化后的模式:%d ID号:%d \n",
                        msg.stuTime, msg.nChannelID, msg.dwID, msg.emMode);
                break;
            }
            case NetSDKLib.NET_ALARM_INPUT_SOURCE_SIGNAL: // 报警输入源信号事件
            {
                ALARM_INPUT_SOURCE_SIGNAL_INFO msg = new ALARM_INPUT_SOURCE_SIGNAL_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【报警输入源信号事件】 时间:%s 通道号:%d 事件动作(0:开始 1:停止):%d \n",
                        msg.stuTime, msg.nChannelID, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_ALARMCLEAR: // 消警事件
            {
                ALARM_ALARMCLEAR_INFO msg = new ALARM_ALARMCLEAR_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【消警事件】 时间:%s 通道号:%d 事件动作(0表示脉冲事件,1表示持续性事件开始,2表示持续性事件结束):%d \n",
                        msg.stuTime, msg.nChannelID, msg.bEventAction);
                break;
            }
            case NetSDKLib.NET_ALARM_SUBSYSTEM_STATE_CHANGE: // 子系统状态改变事件
            {
                ALARM_SUBSYSTEM_STATE_CHANGE_INFO msg = new ALARM_SUBSYSTEM_STATE_CHANGE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【子系统状态改变事件】 时间:%s 子系统序号:%d 变化后的状态:%d \n", msg.stuTime, msg.nChannelID, msg.emState);
                break;
            }
            case NetSDKLib.NET_ALARM_MODULE_LOST: // 扩展模块掉线事件
            {
                ALARM_MODULE_LOST_INFO msg = new ALARM_MODULE_LOST_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【扩展模块掉线事件】 时间:%s 扩展模块接的总线的序号:%d 掉线的扩展模块数目:%d 设备类型:%s 在线情况(0-不在线,1-在线):%d \n",
                        msg.stuTime, msg.nSequence, msg.nAddr, new String(msg.szDevType).trim(), msg.bOnline);
                break;
            }
            case NetSDKLib.NET_ALARM_PSTN_BREAK_LINE: // PSTN掉线事件
            {
                ALARM_PSTN_BREAK_LINE_INFO msg = new ALARM_PSTN_BREAK_LINE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【PSTN掉线事件】 时间:%s 电话线序号:%d 事件动作(0:开始 1:停止):%d \n",
                        msg.stuTime, msg.nChannelID, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_DEFENCE_ARMMODE_CHANGE: // 防区布撤防状态改变事件
            {
                ALARM_DEFENCE_ARMMODECHANGE_INFO msg = new ALARM_DEFENCE_ARMMODECHANGE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【防区布撤防状态改变事件】 时间:%s 布撤防状态:%d 防区号:%d \n",
                        msg.stuTime, msg.emDefenceStatus, msg.nDefenceID);
                break;
            }
            case NetSDKLib.NET_ALARM_SUBSYSTEM_ARMMODE_CHANGE: // 子系统布撤防状态改变事件
            {
                ALARM_SUBSYSTEM_ARMMODECHANGE_INFO msg = new ALARM_SUBSYSTEM_ARMMODECHANGE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【子系统布撤防状态改变事件】 UTC:%s 布撤防状态:%d 子系统编号:%d \n",
                        msg.UTC, msg.emSubsystemMode, msg.nSubSystemID);
                break;
            }
            case NetSDKLib.NET_ALARM_SENSOR_ABNORMAL: // 探测器异常报警
            {
                ALARM_SENSOR_ABNORMAL_INFO msg = new ALARM_SENSOR_ABNORMAL_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【探测器异常报警】 时间:%s 通道号:%d 探测器状态:%d 事件动作(0:开始 1:停止):%d \n",
                        msg.stuTime, msg.nChannelID, msg.emStatus, msg.nAction);
                break;
            }
            /************************************************************
             *							通用事件							*
             *************************************************************/
            case NetSDKLib.NET_ALARM_ALARM_EX2: // 本地报警事件
            {
                ALARM_ALARM_INFO_EX2 msg = new ALARM_ALARM_INFO_EX2();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【本地报警事件】 时间:%s 通道号:%d 事件动作(0:开始 1:停止):%d \n",
                        msg.stuTime, msg.nChannelID, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_CHASSISINTRUDED: // 防拆事件
            {
                ALARM_CHASSISINTRUDED_INFO msg = new ALARM_CHASSISINTRUDED_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【防拆事件】 时间:%s 通道号:%d 事件动作(0:开始 1:停止):%d \n",
                        msg.stuTime, msg.nChannelID, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_RCEMERGENCY_CALL: // 紧急呼叫报警事件
            {
                ALARM_RCEMERGENCY_CALL_INFO msg = new ALARM_RCEMERGENCY_CALL_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【紧急呼叫报警事件】 时间:%s 紧急类型:%d 报警方式:%d 事件动作(-1:未知 0:开始 1:停止):%d \n",
                        msg.stuTime, msg.emType, msg.emMode, msg.nAction);
                break;
            }
            case NetSDKLib.NET_ALARM_CROWD_DETECTION: // 人群密度检测事件
            {
                ALARM_CROWD_DETECTION_INFO msg = new ALARM_CROWD_DETECTION_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【人群密度检测事件】 UTC:%s 通道号:%d 事件动作(0表示脉冲事件,1表示持续性事件开始,2表示持续性事件结束):%d ",
                        msg.UTC, msg.nChannelID, msg.nEventAction);
                break;
            }
            /************************************************************
             *							交通事件							*
             *************************************************************/
            case NetSDKLib.NET_ALARM_TRAFFIC_XINKONG: // 交通态势报警事件
            {
                ALARM_TRAFFIC_XINKONG_INFO msg = new ALARM_TRAFFIC_XINKONG_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                System.out.printf("【交通态势报警事件】 UTC:%s 通道号:%d 物理车道号:%d 车牌号:%s carID:%d carType:%d \n",
                        msg.UTC, msg.nChannelID, msg.stuLaneInfo[0].nLane
                        , new String(msg.stuLaneInfo[0].stuCoilsInfo[0].PlateNum).trim()
                        , msg.stuLaneInfo[0].stuCoilsInfo[0].nCarId
                        , msg.stuLaneInfo[0].stuCoilsInfo[0].emCarType);
                break;
            }
            /************************************************************
             *                      电警红绿灯状态                        *
             ************************************************************/
            case NetSDKLib.NET_ALARM_TRAFFIC_LIGHT_STATE: // 电警红绿灯状态事件
            {
                System.out.println("\n\n<Event> TRAFFIC [ ALARM TRAFFIC LIGHT STATE ]");

                ALARM_TRAFFIC_LIGHT_STATE_INFO msg = new ALARM_TRAFFIC_LIGHT_STATE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);

                StringBuilder builder = new StringBuilder();
                try {
                    builder.append("<<------门禁报警事件主要信息------>>").append("\n")
                            .append("nChannel 通道: ").append(msg.nChannelID).append("\n")
                            .append("nAction 事件状态(0:脉冲 1:开始 2:停止): ").append(msg.nAction).append("\n")
                            .append("szName 事件名称: ").append(new String(msg.szName, encode)).append("\n")
                            .append("PTS 时间戳: ").append(msg.PTS).append("\n")
                            .append("UTC 发生时间: ").append(msg.UTC).append("\n")
                            .append("nEventID 事件ID: ").append(msg.nEventID).append("\n")
                            .append("nSource 源地址: ").append(msg.nSource).append("\n")
                            .append("nFrameSequence 帧序号: ").append(msg.nFrameSequence).append("\n")
                            .append("emLightSource 红绿灯触发源: ").append(EM_TRFAFFIC_LIGHT_SOURCE.getNoteByValue(msg.emLightSource)).append("\n");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                builder.append("///->灯亮的持续时间组，共 ").append(msg.nLightPeriodsNum).append(" 组\n");
                for (int i = 0; i < msg.nLightPeriodsNum; i++) {
                    builder.append("<<-----[").append(i).append("]----->>").append("\n")
                            .append("emType 交通灯类型: ").append(EM_TRFAFFIC_LIGHT_TYPE.getNoteByValue(msg.stuLightPeriods[i].emType)).append("\n")
                            .append("nStraight 直行灯持续时间: ").append(msg.stuLightPeriods[i].nStraight).append("\n")
                            .append("nTurnLeft 左转灯持续时间: ").append(msg.stuLightPeriods[i].nTurnLeft).append("\n")
                            .append("nTurnRight 右转灯持续时间: ").append(msg.stuLightPeriods[i].nTurnRight).append("\n")
                            .append("nUTurn 掉头灯持续时间: ").append(msg.stuLightPeriods[i].nUTurn).append("\n");
                }

                builder.append("///->交通灯状态组，共 ").append(msg.nLightStateNum).append(" 组\n");
                for (int i = 0; i < msg.nLightStateNum; i++) {
                    builder.append("<<-----[").append(i).append("]----->>").append("\n")
                            .append("emStraightLightInfo 直行信号灯状态: ").append(NET_TRAFFIC_LIGHT_STATUS.getNoteByValue(msg.stuLightStates[i].emStraightLightInfo)).append("\n")
                            .append("emTurnLeftLightInfo 左转信号灯状态: ").append(NET_TRAFFIC_LIGHT_STATUS.getNoteByValue(msg.stuLightStates[i].emTurnLeftLightInfo)).append("\n")
                            .append("emTurnRightLightInfo 右转信号灯状态: ").append(NET_TRAFFIC_LIGHT_STATUS.getNoteByValue(msg.stuLightStates[i].emTurnRightLightInfo)).append("\n")
                            .append("emUTurnLightInfo 调头信号灯状态: ").append(NET_TRAFFIC_LIGHT_STATUS.getNoteByValue(msg.stuLightStates[i].emUTurnLightInfo)).append("\n");
                }

                System.out.println(builder.toString());

                break;
            }
            /**
             * 太阳能系统信息上报
             */
            case SDK_ALARM_Ex_TYPE.SDK_ALARM_SOLARCELL_SYSTEM_INFO: {
                /**
                 * 太阳能电池信息
                 */
                ALARM_SOLARCELL_SYSTEM_INFO info = new ALARM_SOLARCELL_SYSTEM_INFO();
                ToolKits.GetPointerData(pStuEvent, info);

                System.out.printf("太阳能电池信息: 脉冲:%d, 事件发生时间:%s,时间戳:%f", info.nAction, info.UTC.toStringTime(), info.PTS);

                System.out.printf("蓄电池信息: 电量百分比:%d,电压:%f,温度:%f,控制温度:%f",
                        info.stuBatteryInfo.nElectricityQuantityPercent,
                        info.stuBatteryInfo.fVoltage,
                        info.stuBatteryInfo.fTemperature,
                        info.stuBatteryInfo.fControllerTemper);

                System.out.printf("太阳能历史数据: 系统运行时间:%d,蓄电池总放电次数:%d,蓄电池总充满电次数:%d",
                        info.stuHistoryInfo.nSystemTotalRunDay,
                        info.stuHistoryInfo.nBatteryOverDischargeCount,
                        info.stuHistoryInfo.nBatteryTotalChargeCount);

                System.out.printf("太阳能板信息: 电压:%f,电流:%f,充电功率:%f",
                        info.stuSolarPanel.fVoltage,
                        info.stuSolarPanel.fElectricCurrent,
                        info.stuSolarPanel.fChargingPower);
                System.out.println("故障信息:");
                for (int i = 0; i < info.nSystemFault; i++) {
                    System.out.println(EM_SOLARCELL_SYSTEM_FAULT_TYPE.getSolarcellFaultType(info.emSystemFault[i]).getInfo());
                }
                break;
            }

            /**
             * 硬盘满警告信息,数据为1个字节，1为有硬盘满报警，0为无报警。
             */
            case SDK_ALARM_Ex_TYPE.SDK_DISKFULL_ALARM_EX: {
                byte[] info = new byte[1];
                pStuEvent.read(0, info, 0, info.length);
                System.out.println(info[0] == 1 ? "硬盘满报警" : "无硬盘报警");
                break;
            }
            /**
             * 硬盘故障
             */
            case SDK_ALARM_Ex_TYPE.SDK_DISKERROR_ALARM_EX: {
                byte[] info = new byte[32];
                pStuEvent.read(0, info, 0, info.length);
                for (int i = 0; i < info.length; i++) {
                    System.out.println("info[" + i + "]" + (info[i] == 1 ? "硬盘错误报警" : "无报警"));
                }
                break;
            }
            /**
             * 无硬盘报警
             */
            case SDK_ALARM_Ex_TYPE.SDK_ALARM_NO_DISK: {
                ALARM_NO_DISK_INFO info = new ALARM_NO_DISK_INFO();
                ToolKits.GetPointerData(pStuEvent, info);
                System.out.println("time:" + info.stuTime.toString() + ",action:" + (info.dwAction == 0 ? "Start" : (info.dwAction == 1 ? "Stop" : "UnKnown")));
                break;
            }
            /************************************************************
             *                      热成像测温点报警                       *
             ************************************************************/
            case NetSDKLib.NET_ALARM_HEATIMG_TEMPER: // 热成像测温点报警
            {
                System.out.println("\n\n<Event> THERMAL [ ALARM HEATIMG TEMPER ]");

                ALARM_HEATIMG_TEMPER_INFO msg = new ALARM_HEATIMG_TEMPER_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);

                StringBuilder builder = new StringBuilder();
                try {
                    builder.append("<<------门禁报警事件主要信息------>>").append("\n")
                            .append("szName 温度异常点名称: ").append(new String(msg.szName, encode)).append("\n")
                            .append("nAlarmId 报警项编号: ").append(msg.nAlarmId).append("\n")
                            .append("nResult 报警结果值: ").append(NET_RADIOMETRY_RESULT.getNoteByValue(msg.nResult)).append("\n")
                            .append("nAlarmContion 报警条件: ").append(NET_RADIOMETRY_ALARMCONTION.getNoteByValue(msg.nAlarmContion)).append("\n")
                            .append("fTemperatureValue 报警温度值: ").append(msg.fTemperatureValue).append("\n")
                            .append("nTemperatureUnit 温度单位: ").append(msg.nTemperatureUnit == 1 ? "摄氏度" : (msg.nTemperatureUnit == 2 ? "华氏度" : "未知")).append("\n")
                            .append("nPresetID 预置点: ").append(msg.nPresetID).append("\n")
                            .append("nChannel 通道号: ").append(msg.nChannel).append("\n")
                            .append("nAction 事件状态: ").append(msg.nAction == 0 ? "开始" : (msg.nAction == 1 ? "停止" : "无意义")).append("\n")
                            .append("stuAlarmCoordinates 报警坐标Ex: ").append("\n");
                    for (int i = 0; i < msg.stuAlarmCoordinates.nPointNum; i++) {
                        builder.append(String.format("[%2d] (%4d, %4d)", i, msg.stuAlarmCoordinates.stuPoints[i].nx, msg.stuAlarmCoordinates.stuPoints[i].ny)).append("\n");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                System.out.println(builder.toString());

                break;
            }
            case NetSDKLib.NET_ALARM_HOTSPOT_WARNING: {
                System.out.println("\n\n<Event> THERMAL [ ALARM_HOTSPOT_WARNING ]");

                ALARM_HOTSPOT_WARNING_INFO msg = new ALARM_HOTSPOT_WARNING_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);

                StringBuilder builder = new StringBuilder();

                builder.append("<<------门禁报警事件主要信息------>>").append("\n")
                        .append("fHotSpotValue 温度值: ").append(msg.nTemperatureUnit).append("\n")
                        .append("nChannel 通道号: ").append(msg.nChannelID).append("\n")
                        .append("nAction 事件状态: ").append(msg.nAction == 0 ? "开始" : (msg.nAction == 1 ? "停止" : "无意义")).append("\n")
                        .append("stuAlarmCoordinates 报警坐标Ex: ").append("\n");


                System.out.println(builder.toString());
            }
            case NetSDKLib.NET_ALARM_COLDSPOT_WARNING: {
                System.out.println("\n\n<Event> THERMAL [ ALARM_COLDSPOT_WARNING ]");

                ALARM_COLDSPOT_WARNING_INFO msg = new ALARM_COLDSPOT_WARNING_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);

                StringBuilder builder = new StringBuilder();

                builder.append("<<------门禁报警事件主要信息------>>").append("\n")
                        .append("fHotSpotValue 温度值: ").append(msg.nTemperatureUnit).append("\n")
                        .append("nChannel 通道号: ").append(msg.nChannelID).append("\n")
                        .append("nAction 事件状态: ").append(msg.nAction == 0 ? "开始" : (msg.nAction == 1 ? "停止" : "无意义")).append("\n")
                        .append("stuAlarmCoordinates 报警坐标Ex: ").append("\n");


                System.out.println(builder.toString());
            }
            default:
                break;
        }

        return true;
    }
}