package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.ALARM_REGION_PARKING_NO_ENTRY_RECORD_INFO;
import com.netsdk.lib.structure.ALARM_REGION_PARKING_TIMEOUT_INFO;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/21 16:14
 */
public class ParkingMessageCallBackEx1 implements NetSDKLib.fMessCallBackEx1 {

    private static ParkingMessageCallBackEx1 singleInstance;

    public static ParkingMessageCallBackEx1 getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new ParkingMessageCallBackEx1();
        }
        return singleInstance;
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
    public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, int bAlarmAckFlag, NativeLong nEventID, Pointer dwUser) {
        switch (lCommand) {
            case NetSDKLib.NET_ALARM_TRAFFIC_PARKING_TIMEOUT: {   ///<————停车超时时间 TrafficParkingTimeout

                ParsingTrafficParkingTimeout(pStuEvent);
                break;
            }
            case NetSDKLib.NET_ALARM_TRAFFIC_SUSPICIOUSCAR: {   ///<————可疑车辆检测 TrafficSuspiciousCar

                ParsingTrafficSuspiciousCar(pStuEvent);
                break;
            }
            case NetSDKLib.NET_ALARM_PARKING_LOT_STATUS_DETECTION: {    ///<————车位检测事件 ParkingLotStatusDetection

                ParsingParkingLotStatusDetectionEvent(pStuEvent);
                break;
            }
            case NetSDKLib.NET_ALARM_REGION_PARKING_TIMEOUT: {      ///<————区间车位停车超时 RegionParkingTimeout

                ParsingAlarmRegionParkingTimeoutEvent(pStuEvent);
                break;
            }
            case NetSDKLib.NET_ALARM_REGION_PARKING_NO_ENTRY_RECORD: {  ///<————区间车位停车无入场信息事件 RegionParkingNoEntryRecord

                ParsingRegionParkingNoEntryRecord(pStuEvent);
                break;
            }
            default:
                System.out.printf("Get Other Event 0x%x\n",lCommand);
                break;
        }
        return true;
    }

    // 区间车位停车无入场信息事件 RegionParkingNoEntryRecord
    private void ParsingRegionParkingNoEntryRecord(Pointer pStuEvent) {

        ALARM_REGION_PARKING_NO_ENTRY_RECORD_INFO stuAlarmInfo = new ALARM_REGION_PARKING_NO_ENTRY_RECORD_INFO();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);

        int nAction = stuAlarmInfo.nAction;
        String UTC = stuAlarmInfo.UTC.toString();
        int nChannelID = stuAlarmInfo.nChannelID;
        String nCarOutParkingSpaceTime = stuAlarmInfo.stuCarOutParkingSpaceTime.toString();

        String szPlateNumber = null;
        String szInParkRegionInfo = null;
        try {
            szPlateNumber = new String(stuAlarmInfo.szPlateNumber, encode).trim();
            szInParkRegionInfo = new String(stuAlarmInfo.szInParkRegionInfo, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringBuilder info = new StringBuilder()
                .append("<<------区间车位停车无入场信息事件------>>").append("\n")
                .append("nAction 事件状态 0:脉冲: ").append(nAction).append("\n")
                .append("UTC 发生时间: ").append(UTC).append("\n")
                .append("nChannelID 通道号: ").append(nChannelID).append("\n")
                .append("nCarOutParkingSpaceTime 车辆驶出区域停车位时间(单位：秒): ").append(nCarOutParkingSpaceTime).append("\n")
                .append("szPlateNumber 车牌号: ").append(szPlateNumber).append("\n")
                .append("szInParkRegionInfo 停车区间信息: ").append(szInParkRegionInfo);
        System.out.println(info.toString());
    }

    // 区间车位停车超时 RegionParkingTimeout
    private void ParsingAlarmRegionParkingTimeoutEvent(Pointer pStuEvent) {

        ALARM_REGION_PARKING_TIMEOUT_INFO stuAlarmInfo = new ALARM_REGION_PARKING_TIMEOUT_INFO();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);

        int nAction = stuAlarmInfo.nAction;
        String UTC = stuAlarmInfo.UTC.toString();
        int nChannelID = stuAlarmInfo.nChannelID;
        String nCarInParkingLotTime = stuAlarmInfo.stuCarInParkingLotTime.toString();
        String szPlateNumber = null;
        String szInParkRegionInfo = null;
        try {
            szPlateNumber = new String(stuAlarmInfo.szPlateNumber, encode).trim();
            szInParkRegionInfo = new String(stuAlarmInfo.szInParkRegionInfo, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringBuilder info = new StringBuilder()
                .append("<<------区间车位停车超时事件------>>").append("\n")
                .append("nAction 事件状态 0:脉冲: ").append(nAction).append("\n")
                .append("UTC 发生时间: ").append(UTC).append("\n")
                .append("nChannelID 通道号: ").append(nChannelID).append("\n")
                .append("nCarInParkingLotTime 车辆驶入停车场时间(单位：秒): ").append(nCarInParkingLotTime).append("\n")
                .append("szPlateNumber 车牌号: ").append(szPlateNumber).append("\n")
                .append("szInParkRegionInfo 停车区间信息: ").append(szInParkRegionInfo);
        System.out.println(info.toString());
    }

    // 停车超时事件 TrafficParkingTimeout
    private void ParsingTrafficParkingTimeout(Pointer pStuEvent) {
        System.out.println("<Event> TRAFFIC [ PARKING TIMEOUT ]");

        // 能展示事件内容：【事件名称】，事件发生时间，车牌号，【车位号】，驶入时间，停车时长

        NetSDKLib.ALARM_TRAFFIC_PARKING_TIMEOUT_INFO stuParkingTimeoutInfo = new NetSDKLib.ALARM_TRAFFIC_PARKING_TIMEOUT_INFO();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuParkingTimeoutInfo);

        String timeoutEventTime = stuParkingTimeoutInfo.UTC.toString();
        String timeoutInPartTime = stuParkingTimeoutInfo.stuInParkTime.toString();
        String timeoutOutPartTime = stuParkingTimeoutInfo.stuOutParkTime.toString();
        int timeoutParkingTime = stuParkingTimeoutInfo.nParkingTime;
        String timeoutPlatNumber = null;
        try {
            timeoutPlatNumber = new String(stuParkingTimeoutInfo.stuTrafficCar.szPlateNumber, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder timeoutInfo = new StringBuilder()
                .append("<<------停车超时事件------>>").append("\n")
                .append("发生时间: ").append(timeoutEventTime).append("\n")
                .append("进场时间: ").append(timeoutInPartTime).append("\n")
                .append("出场时间: ").append(timeoutOutPartTime).append("\n")
                .append("停车时长: ").append(timeoutParkingTime).append("\n")
                .append("车牌名: ").append(timeoutPlatNumber);
        System.out.println(timeoutInfo.toString());
    }

    // 可疑车辆检测 TrafficSuspiciousCar
    private void ParsingTrafficSuspiciousCar(Pointer pStuEvent) {
        System.out.println("<Event> TRAFFIC [ SUSPICIOUS CAR ]");

        // 能展示报警内容：【事件名称】，【时间】，车辆信息（车位号、车牌）。

        NetSDKLib.ALARM_TRAFFIC_SUSPICIOUSCAR_INFO stuSuspiciousInfo = new NetSDKLib.ALARM_TRAFFIC_SUSPICIOUSCAR_INFO();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuSuspiciousInfo);

        String suspiciousMasterCar = null;    // 车主姓名
        String suspiciousPlateNumber = null;  // 车牌号码
        try {
            suspiciousMasterCar = new String(stuSuspiciousInfo.stuCarInfo.szMasterOfCar, encode).trim();     // 车主姓名
            suspiciousPlateNumber = new String(stuSuspiciousInfo.stuCarInfo.szPlateNumber, encode).trim();   // 车牌号码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder suspiciousMainInfo = new StringBuilder()
                .append("<<------嫌疑车辆上报事件------>>").append("\n")
                .append("车主姓名: ").append(suspiciousMasterCar).append("\n")
                .append("车牌号码: ").append(suspiciousPlateNumber);
        System.out.println(suspiciousMainInfo.toString());
    }

    // 车位检测事件 ParkingLotStatusDetection
    private void ParsingParkingLotStatusDetectionEvent(Pointer pStuEvent) {
        System.out.println("<Event> TRAFFIC [ PARKING LOT STATUS DETECTION ]");

        // 能展示巡检事件内容 + ParkingStatus下的所有信息

        /////////////// 停车位状态主要信息
        NetSDKLib.ALARM_PARKING_LOT_STATUS_DETECTION stuPLSDetection = new NetSDKLib.ALARM_PARKING_LOT_STATUS_DETECTION();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuPLSDetection);

        String plsSzName = new String(stuPLSDetection.szName).trim();
        String plsTime = stuPLSDetection.UTC.toString();
        int plsStatusNum = stuPLSDetection.nParkingStatusNum;
        StringBuilder plsDetectMainInfo = new StringBuilder()
                .append("<<------停车位状态检测事件------>>").append("\n")
                .append("事件名: ").append(plsSzName).append("\n")
                .append("发生时间: ").append(plsTime).append("\n")
                .append("室外停车位个数: ").append(plsStatusNum);
        System.out.println(plsDetectMainInfo.toString());

        ////////////// 每个停车位信息
        StringBuilder plsDetectDetailInfo = new StringBuilder()
                .append("<<------每个停车位信息------>>");
        for (int i = 0; i < stuPLSDetection.nParkingStatusNum; i++) {
            try {
                plsDetectDetailInfo.append("\n").append("第 ").append(i+1).append(" 个停车位信息")
                        .append("\n").append("车位名称: ").append(new String(stuPLSDetection.stuParkingStatus[i].szName, encode).trim())
                        .append("\n").append("车位ID: ").append(stuPLSDetection.stuParkingStatus[i].nID)
                        .append("\n").append("车位内已停车位数量: ").append(stuPLSDetection.stuParkingStatus[i].nParkedNumber)
                        .append("\n").append("相对上次上报的变化状态: ").append(stuPLSDetection.stuParkingStatus[i].emChangeStatus);  // 参考 EM_PARKING_NUMBER_CHANGE_STATUS
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(plsDetectDetailInfo.toString());
    }
}
