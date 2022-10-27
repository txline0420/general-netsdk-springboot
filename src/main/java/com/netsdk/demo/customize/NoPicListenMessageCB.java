package com.netsdk.demo.customize;

import com.netsdk.demo.intelligentTraffic.trafficRadarDemo.TrafficRadarUtils;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * @author ： 47040
 * @since ： Created in 2020/12/14 9:14
 */
public class NoPicListenMessageCB implements NetSDKLib.fMessCallBackEx1 {

    private static NoPicListenMessageCB singleInstance;

    public static NoPicListenMessageCB getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new NoPicListenMessageCB();
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
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_TRAFFIC_FLOW_QUEUE: {   // 交通路口排队事件
                    System.out.println("交通路口排队事件, code = " + lCommand);
                    ALARM_TRAFFIC_FLOW_QUEUE_INFO stuAlarmInfo = new ALARM_TRAFFIC_FLOW_QUEUE_INFO();
                    ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);
                    System.out.println(stuAlarmInfo.toString());
                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_FLOW_JUNTION: {   // 交通路口过车事件
                    System.out.println("交通路口过车事件, code = " + lCommand);
                    ALARM_TRAFFIC_FLOW_JUNTION_INFO stuAlarmInfo = new ALARM_TRAFFIC_FLOW_JUNTION_INFO();
                    ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);
                    System.out.println(stuAlarmInfo.toString());
                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_FLOW_VEHICLE_STOP: {   // 交通路口停车事件
                    System.out.println("交通路口停车事件, code = " + lCommand);
                    ALARM_TRAFFIC_FLOW_VEHICLE_STOP_INFO stuAlarmInfo = new ALARM_TRAFFIC_FLOW_VEHICLE_STOP_INFO();
                    ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);
                    System.out.println(stuAlarmInfo.toString());
                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_FLOW_STAT: {   // 交通路口车道统计事件
                    System.out.println("交通路口车道统计事件, code = " + lCommand);
                    ALARM_TRAFFIC_FLOW_STAT_INFO stuAlarmInfo = new ALARM_TRAFFIC_FLOW_STAT_INFO();
                    ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);
                    System.out.println(stuAlarmInfo.toString());
                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_FLOW_STAT_EX: {   // 交通路口车道统计拓展事件
                    System.out.println("交通路口车道统计拓展事件, code = " + lCommand);
                    ALARM_TRAFFIC_FLOW_STAT_EX_INFO stuAlarmInfo = new ALARM_TRAFFIC_FLOW_STAT_EX_INFO();
                    ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuAlarmInfo);
                    TRAFFIC_FLOW_STAT_EX_INFO  stuFlowStatEx = stuAlarmInfo.stuFlowStatEx;
                    TRAFFIC_FLOW_LANE_INFO[]   stuLaneInfo = stuFlowStatEx.stuLaneInfo;
                    int nLaneInfoNum =		stuFlowStatEx.nLaneInfoNum;
                    for (int i = 0; i < nLaneInfoNum; i++) {
                    	System.out.println("车道流量:"+stuLaneInfo[i].nFlow);
					}                                                            
                    System.out.println(stuAlarmInfo.toString());
                    break;
                }
                default:
                    System.out.printf("Get Other Event 0x%x\n", lCommand);
                    break;
            }
        return true;
    }


}
