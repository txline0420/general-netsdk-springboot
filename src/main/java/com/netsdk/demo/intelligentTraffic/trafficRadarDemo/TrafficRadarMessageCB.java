package com.netsdk.demo.intelligentTraffic.trafficRadarDemo;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_DETECT_SENSOR_TYPE;
import com.netsdk.lib.enumeration.EM_TRAFFIC_FLOW_STATUS;
import com.netsdk.lib.enumeration.EM_VEHICLEINOUT_CAR_TYPE;
import com.netsdk.lib.enumeration.EM_VIRTUAL_COIL_OCCUPANCY_STATUS;
import com.netsdk.lib.structure.ALARM_VEHICLE_INOUT_INFO;
import com.netsdk.lib.structure.NET_VEHICLE_OBJECT;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

/**
 * @author ： 47040
 * @since ： Created in 2020/12/14 9:14
 */
public class TrafficRadarMessageCB implements NetSDKLib.fMessCallBackEx1 {

    private static TrafficRadarMessageCB singleInstance;

    public static TrafficRadarMessageCB getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new TrafficRadarMessageCB();
        }
        return singleInstance;
    }

    // 获取此平台的字符串编码
    public static final String encode = TrafficRadarUtils.GetSystemEncode();

    // bAlarmAckFlag : TRUE,该事件为可以进行确认的事件；FALSE,该事件无法进行确认
    // nEventID 用于对 CLIENT_AlarmAck 接口的入参进行赋值,当 bAlarmAckFlag 为 TRUE 时,该数据有效
    // pBuf内存由SDK内部申请释放
    @Override
    public boolean invoke(int lCommand,                 // 事件枚举 配合 pStuEvent dwBufLen 可以获取事件信息
                          NetSDKLib.LLong lLoginID,     // 登录句柄
                          Pointer pStuEvent,            // 事件信息指针
                          int dwBufLen,                 // 事件信息长度
                          String strDeviceIP,           // 设备IP
                          NativeLong nDevicePort,       // 设备TCP端口
                          int bAlarmAckFlag, NativeLong nEventID, Pointer dwUser) {
        try {
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_VEHICLE_INOUT: {   // 车辆出入事件 vehicleInOut
                    ParsingVehicleInOut(pStuEvent);

                    break;
                }
                default:
                    System.out.printf("Get Other Event 0x%x\n", lCommand);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }

    // 车辆进出车道报警 vehicleInOut
    private void ParsingVehicleInOut(Pointer pStuEvent) throws UnsupportedEncodingException {
        System.out.println("\n\n<Alarm Event> [ ALARM VEHICLE INOUT 车辆出入事件 ]");

        ////////////////////////////// <<-----获取事件信息----->> //////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////

        ALARM_VEHICLE_INOUT_INFO stuAlarmInfo = new ALARM_VEHICLE_INOUT_INFO();
        ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);

        int nAction = stuAlarmInfo.nAction;                         // 事件动作,0表示脉冲事件
        int nChannelID = stuAlarmInfo.nChannel;                     // 通道号
        String szName = new String(stuAlarmInfo.szName, encode);    // 事件名称
        String UTC = stuAlarmInfo.UTC.toStringTime();               // 事件发生的时间
        int nEventID = stuAlarmInfo.nEventID;

        int nObjectNum = stuAlarmInfo.nObjectNum;   // 有效物体个数
        NET_VEHICLE_OBJECT[] stObjects = new NET_VEHICLE_OBJECT[nObjectNum];
        for (int i = 0; i < nObjectNum; i++) {
            stObjects[i] = new NET_VEHICLE_OBJECT();
        }
        ToolKits.GetPointerDataToStructArr(stuAlarmInfo.pstObjets, stObjects);

        int nStatNum = stuAlarmInfo.nStatNum;       // 统计有效个数

        // 打印出来看一下
        StringBuilder info = new StringBuilder()
                .append("nAction 事件状态 0->脉冲: ").append(nAction).append("\n")
                .append("nChannelID 通道号 : ").append(nChannelID).append("\n")
                .append("szName 事件名称: ").append(szName).append("\n")
                .append("UTC 发生时间: ").append(UTC).append("\n")
                .append("nEventID 事件ID: ").append(nEventID).append("\n");
        info.append("////// 有效物体个数: ").append(nObjectNum).append("\n");
        for (int i = 0; i < nObjectNum; i++) {
            info.append("// --> 物体序号: ").append(i).append("\n")
                    .append("   nObjectID 物体ID:").append(stObjects[i].nObjectID).append("\n")
                    .append("   nSpeed 车速，单位km/h:").append(stObjects[i].nSpeed).append("\n")
                    .append("   szObjectType 物体类型:").append(new String(stObjects[i].szObjectType, encode)).append("\n")
                    .append("   emSubObject 物体子类别(NetSDKLib.EM_CATEGORY_TYPE):").append(stObjects[i].emSubObject).append("\n")
                    .append("   nLane 物理车道号:").append(stObjects[i].nLane).append("\n")
                    .append("   nRoadwayNumber 自定义车道号:").append(stObjects[i].nRoadwayNumber).append("\n")
                    .append("   emSensorType 传感器类型:").append(EM_DETECT_SENSOR_TYPE.getEnum(stObjects[i].emSensorType).getNote()).append("\n")
                    .append("   nObjectRVID 物体雷达和视频融合ID:").append(stObjects[i].nObjectRVID).append("\n")
                    .append("   nObjectRID 物体的雷达ID:").append(stObjects[i].nObjectRID).append("\n")
                    .append("   szDrivingDirection[0] 行驶方向:").append(new String(stObjects[i].szDrivingDirection[0].info, encode)).append("\n")
                    .append("   szDrivingDirection[1] 上行地点:").append(new String(stObjects[i].szDrivingDirection[1].info, encode)).append("\n")
                    .append("   szDrivingDirection[2] 下行地点:").append(new String(stObjects[i].szDrivingDirection[2].info, encode)).append("\n")
                    .append("   szPlateNumber 车牌号码:").append(new String(stObjects[i].szPlateNumber, encode)).append("\n")
                    .append("   szPlateColor 车牌颜色:").append(new String(stObjects[i].szPlateColor, encode)).append("\n")
                    .append("   dbLongitude 车辆经度:").append(stObjects[i].dbLongitude).append("\n")
                    .append("   dbLatitude 车辆纬度:").append(stObjects[i].dbLatitude).append("\n")
                    .append("   szCarColor 车身颜色:").append(new String(stObjects[i].szCarColor, encode)).append("\n")
                    .append("   emCarType 车辆类型:").append(EM_VEHICLEINOUT_CAR_TYPE.getEnum(stObjects[i].emCarType).getNote()).append("\n")
                    .append("   emVirtualCoilDirection 车辆驶入驶出状态(NetSDKLib.NET_FLOWSTAT_DIRECTION):").append(stObjects[i].emVirtualCoilDirection).append("\n")
                    .append("   dbDistanceToStop 距离停车线距离:").append(stObjects[i].dbDistanceToStop).append("\n")
                    .append("   dbCarX 车道的中心点 X轴方向:").append(stObjects[i].dbCarX).append("\n")
                    .append("   dbCarY 车道的中心点 Y轴方向:").append(stObjects[i].dbCarY).append("\n")
                    .append("   dbCarAngle 车道的中心点 角度:").append(stObjects[i].dbCarAngle).append("\n");
        }
        info.append("////// 统计有效个数: ").append(nStatNum).append("\n");
        for (int i = 0; i < nStatNum; i++) {
            info.append("// --> 统计序号: ").append(i).append("\n")
                    .append("   nLane 物理车道号:").append(stuAlarmInfo.stuStats[i].nLane).append("\n")
                    .append("   nRoadwayNumber 自定义车道号:").append(stuAlarmInfo.stuStats[i].nRoadwayNumber).append("\n")
                    .append("   emStatus 流量状态:").append(EM_TRAFFIC_FLOW_STATUS.getEnum(stuAlarmInfo.stuStats[i].emStatus).getNote()).append("\n")
                    .append("   emHeadCoil 车头虚拟线圈状态:").append(EM_VIRTUAL_COIL_OCCUPANCY_STATUS.getEnum(stuAlarmInfo.stuStats[i].emHeadCoil).getNote()).append("\n")
                    .append("   emTailCoil 车尾虚拟线圈状态:").append(EM_VIRTUAL_COIL_OCCUPANCY_STATUS.getEnum(stuAlarmInfo.stuStats[i].emTailCoil).getNote()).append("\n")
                    .append("   nSpeed 车道平均速度(单位：km/h):").append(stuAlarmInfo.stuStats[i].nSpeed).append("\n")
                    .append("   nQueueLen 排队长度(单位：cm):").append(stuAlarmInfo.stuStats[i].nQueueLen).append("\n")
                    .append("   nCarsInQueue 排队车辆数:").append(stuAlarmInfo.stuStats[i].nCarsInQueue).append("\n")
                    .append("   emSensorType 传感器类型 :").append(EM_DETECT_SENSOR_TYPE.getEnum(stuAlarmInfo.stuStats[i].emSensorType).getNote()).append("\n");
        }
        System.out.println(info.toString());
    }
}
