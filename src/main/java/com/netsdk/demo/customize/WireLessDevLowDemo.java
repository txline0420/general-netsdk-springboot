package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_TIME;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import static com.netsdk.lib.NetSDKLib.*;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220629025 海外无线网关三期SDK配套开发
 * @date 2022/7/19 10:01
 */
public class WireLessDevLowDemo extends Initialization {


    /**
     * 订阅
     * @return
     */
    public void startListen() {
        // 设置报警回调函数
        netSdk.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);

        // 订阅报警
        boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
        if (!bRet) {
            System.err.println("订阅报警失败! LastError = 0x%x\n" + netSdk.CLIENT_GetLastError());
        }
        else {
            System.out.println("订阅报警成功.");
        }
    }

    /**
     * 取消订阅
     * @return
     */
    public void stopListen() {
        // 停止订阅报警
        boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
        if (bRet) {
            System.out.println("取消订阅报警信息.");
        }
    }

    /**
     * 报警信息回调函数原形,建议写成单例模式
     */
    private static class fAlarmDataCB implements fMessCallBack{
        private fAlarmDataCB(){}

        private static class fAlarmDataCBHolder {
            private static fAlarmDataCB callback = new fAlarmDataCB();
        }

        public static fAlarmDataCB getCallBack() {
            return fAlarmDataCBHolder.callback;
        }

        public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser){
//	  		System.out.printf("command = %x\n", lCommand);
            switch (lCommand)
            {
                case NetSDKLib.NET_ALARM_AREAARM_MODECHANGE: {  // 区域防区模式改变(对应结构体ALARM_AREAARM_MODECHANGE_INFO)

                    System.out.println(" 区域防区模式改变");
                    ALARM_AREAARM_MODECHANGE_INFO msg = new ALARM_AREAARM_MODECHANGE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    /**
                     区域编号
                     */
                    int nAreaIndex
                            = msg.nAreaIndex;
                    int nEventID
                            = msg.nEventID;

                    NET_TIME_EX utc
                            = msg.UTC;
                    int emTriggerMode
                            = msg.emTriggerMode;

                    int emUser
                            = msg.emUser;

                    int nID
                            = msg.nID;
                    System.out.println(" nAreaIndex:"+nAreaIndex);
                    System.out.println(" nEventID:"+nEventID);
                    System.out.println(" utc:"+utc);
                    System.out.println(" emTriggerMode:"+emTriggerMode);
                    System.out.println(" emUser:"+emUser);
                    System.out.println(" nID:"+nID);

                    break;

                }
                case NetSDKLib.NET_ALARM_ALARM_EX2: {  // 本地报警事件(对应结构体ALARM_ALARM_INFO_EX2,对NET_ALARM_ALARM_EX升级)
                    System.out.println(" 本地报警事件");
                    ALARM_ALARM_INFO_EX2 msg = new ALARM_ALARM_INFO_EX2();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("nChannelID" + msg.nChannelID);

                    System.out.println("nAction" + msg.nAction);

                    System.out.println("stuTime" + msg.stuTime.toStringTime());

                    System.out.println("emSenseType" + msg.emSenseType);

                    System.out.println("emDefenceAreaType" + msg.emDefenceAreaType);

                    System.out.println("nEventID" + msg.nEventID);

                    System.out.println("szName" +new String(msg.szName) );


                    break;
                }

                case NetSDKLib.NET_ALARM_WIRELESSDEV_LOWPOWER:{ // 获取无线设备低电量上报事件(对应结构体ALARM_WIRELESSDEV_LOWPOWER_INFO)
                    System.out.println(" 获取无线设备低电量上报事件");
                    ALARM_WIRELESSDEV_LOWPOWER_INFO msg=new ALARM_WIRELESSDEV_LOWPOWER_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    int emResult
                            = msg.emResult;
                    System.out.println(" emResult:"+emResult);

                    NET_TIME stuTime
                            = msg.stuTime;
                    System.out.println(" stuTime:"+stuTime.toStringTime());

                    int emType
                            = msg.emType;
                    System.out.println(" emType:"+emType);
                    break;
                }
                case NetSDKLib.NET_ALARM_MODULE_LOST:{  // 扩展模块掉线事件(对应结构体 ALARM_MODULE_LOST_INFO)
                    System.out.println(" 扩展模块掉线事件");
                    ALARM_MODULE_LOST_INFO msg=new ALARM_MODULE_LOST_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件上报时间
                    NetSDKLib.NET_TIME stuTime=msg.stuTime;

                    System.out.println(" stuTime:"+stuTime.toStringTime());

                 // 扩展模块接的总线的序号(从0开始)
                    int nSequence
                            = msg.nSequence;
                    System.out.println(" nSequence:"+nSequence);

                    // 总线类型, 参考枚举  NET_BUS_TYPE
                    int emBusType =  msg.emBusType;
                    System.out.println(" emBusType:"+emBusType);
                    // 掉线的扩展模块数目
                    int  nAddr =msg.nAddr;
                    System.out.println(" nAddr:"+nAddr);

                    // 掉线的扩展模块的序号(从0开始)
                    int[] anAddr
                            = msg.anAddr;

                    for(int i=0;i<nAddr;i++){
                        System.out.println(anAddr[i]);
                    }
                    //// 设备类型 "SmartLock",是级联设备,当设备类型"AlarmDefence"接口序号为报警序号
                    System.out.println(" szDevType:"+new String(msg.szDevType));


                    // 在线情况   默认0。   0-不在线;  1-在线
                    System.out.println(" bOnline:"+msg.bOnline);

                    break;
                }
                case  NET_ALARM_SENSOR_ABNORMAL:{// 探测器异常报警(对应结构体 ALARM_SENSOR_ABNORMAL_INFO)

                    System.out.println(" 探测器异常报警");
                    ALARM_SENSOR_ABNORMAL_INFO msg=new ALARM_SENSOR_ABNORMAL_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    int nAction
                            = msg.nAction;
                    System.out.println(" nAction:"+nAction);

                   int nChannelID= msg.nChannelID;
                    System.out.println(" nChannelID:"+nChannelID);

                  NetSDKLib.NET_TIME_EX  stuTime= msg.stuTime;
                    System.out.println(" stuTime:"+stuTime.toStringTime());

                    System.out.println(" emStatus:"+msg.emStatus);
                    break;
                }

                case  NET_ALARM_RCEMERGENCY_CALL:{// 紧急呼叫报警事件(对应结构体 ALARM_RCEMERGENCY_CALL_INFO)

                    System.out.println(" 紧急呼叫报警事件");
                    ALARM_RCEMERGENCY_CALL_INFO msg=new ALARM_RCEMERGENCY_CALL_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println(" nAction:" + msg.nAction );
                    System.out.println(" emType:" + msg.emType );
                    System.out.println(" Happend:" + msg.stuTime.toStringTime() );
                    System.out.println(" emMode:" + msg.emMode);
                    System.out.println(" dwID:" + msg.dwID );

                    if (msg.emType == 1) {
                        System.out.println("火警");
                    } else if (msg.emType == 2) {
                        System.out.println("胁迫");
                    } else if (msg.emType == 3) {
                        System.out.println("匪警");
                    } else if (msg.emType == 4) {
                        System.out.println("医疗");

                    } else if (msg.emType == 5) {
                        System.out.println("紧急");
                    }
                    break;
                }
                case NET_ALARM_BATTERYLOWPOWER:{ // 电池电量低报警(对应结构体 ALARM_BATTERYLOWPOWER_INFO)
                    System.out.println(" 电池电量低报警");
                    ALARM_BATTERYLOWPOWER_INFO msg=new ALARM_BATTERYLOWPOWER_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    //0:开始1:停止
                    int nAction = msg.nAction;
                    System.out.println(" nAction:" + nAction );
                   	//剩余电量百分比,单位%
                    int nBatteryLeft = msg.nBatteryLeft;
                    System.out.println(" nBatteryLeft:" + nBatteryLeft );

                    //事件发生时间

                    NetSDKLib.NET_TIME stTime = msg.stTime;
                    System.out.println(" stTime:" + stTime.toStringTime() );

                    int nChannelID = msg.nChannelID;//通道号,标识子设备电池,从0开始
                    System.out.println(" nChannelID:" + nChannelID );

                    break;
                }
                case  NET_ALARM_POWERFAULT: {  // 电源故障事件(对应结构体ALARM_POWERFAULT_INFO)
                    System.out.println(" 电源故障事件");
                    ALARM_POWERFAULT_INFO msg=new ALARM_POWERFAULT_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    int emPowerType = msg.emPowerType;// 电源类型，详见EM_POWER_TYPE
                    System.out.println(" emPowerType:" + emPowerType );

                   int  emPowerFaultEvent= msg. emPowerFaultEvent;  // 电源故障事件，详见EM_POWERFAULT_EVENT_TYPE
                    System.out.println(" emPowerFaultEvent:" + emPowerFaultEvent );
                    // 报警事件发生的时间
                    NetSDKLib.NET_TIME stuTime
                            = msg.stuTime;
                    System.out.println(" stuTime:" + stuTime.toStringTime() );
                    // 0:开始 1:停止
                    int nAction = msg.nAction;
                    System.out.println(" nAction:" + nAction );
                    break;
                }
                case NET_ALARM_CHASSISINTRUDED: {//  机箱入侵(防拆)报警事件(对应结构体ALARM_CHASSISINTRUDED_INFO)

                    System.out.println(" 机箱入侵(防拆)报警事件");
                    ALARM_CHASSISINTRUDED_INFO msg=new ALARM_CHASSISINTRUDED_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    // 0:开始 1:停止
                    int nAction = msg.nAction;
                    System.out.println(" nAction:" + nAction );
                    // 报警事件发生的时间
                    NetSDKLib.NET_TIME stuTime
                            = msg.stuTime;
                    System.out.println(" stuTime:" + stuTime );

                    int nChannelID = msg.nChannelID;//通道号
                    System.out.println(" nChannelID:" + nChannelID );

                    byte[] szReaderID = msg.szReaderID;
                    System.out.println(" szReaderID:" + new String(szReaderID) );

                    // 事件ID
                    System.out.println(" nEventID:" + msg.nEventID );

                    // 无线设备序列号
                    System.out.println(" szSN:" + new String(msg.szSN) );


                    int bRealUTC = msg.bRealUTC;
                    System.out.println(" bRealUTC:" + bRealUTC );


                    System.out.println(" RealUTC:" + msg.RealUTC.toString() );

                    //设备类型,参考EM_ALARM_CHASSISINTRUDED_DEV_TYPE
                    System.out.println(" emDevType:" + msg.emDevType );
                    break;
                }
                case NET_ALARM_AREAALARM: {// 区域报警(对应结构体ALARM_AREAALARM_INFO)
                    System.out.println(" 区域报警");
                    ALARM_AREAALARM_INFO msg=new ALARM_AREAALARM_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    int nAreaIndex = msg.nAreaIndex;
                    System.out.println(" nAreaIndex:" + nAreaIndex );


                    System.out.println(" nEventID:" +msg.nEventID );


                    System.out.println(" UTC:" +msg.UTC.toString() );

                    System.out.println(" szName:" + new String(msg.szName) );

                    System.out.println(" emDefenceAreaType:" + msg.emDefenceAreaType );

                    System.out.println(" nIndex:" + msg.nIndex );

                    System.out.println(" emTrigerType:" + msg.emTrigerType );

                    break;
                }
                case NET_ALARM_RF_JAMMING:{// RF干扰事件(对应结构体 NET_ALARM_RF_JAMMING_INFO)
                    System.out.println(" RF干扰事件");
                    NET_ALARM_RF_JAMMING_INFO msg=new NET_ALARM_RF_JAMMING_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println(" nAction:" + msg.nAction );


                    System.out.println(" nChannel:" + msg.nChannel );

                    System.out.println(" stuUTC:" + msg.stuUTC );


                    System.out.println(" szDeviceName:" + new String(msg.szDeviceName) );

                    break;

                }
                case NET_ALARM_ARMING_FAILURE:{ // 布防失败事件(对应结构体 NET_ALARM_ARMING_FAILURE_INFO)

                    System.out.println(" 布防失败事件");
                    NET_ALARM_ARMING_FAILURE_INFO msg=new NET_ALARM_ARMING_FAILURE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );
                    // 通道号
                    System.out.println(" nChannel:" + msg.nChannel );

                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString() );
                        // 布撤防模式
                    System.out.println(" emMode:" + msg.emMode );


                    break;

                }
                case NET_ALARM_USER_MODIFIED: {  // 用户信息被修改(增加、删除、修改)事件(对应结构体 NET_ALARM_USER_MODIFIED_INFO)

                    System.out.println(" 用户信息被修改(增加、删除、修改)事件");
                    NET_ALARM_USER_MODIFIED_INFO msg=new NET_ALARM_USER_MODIFIED_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );
                    // 通道号
                    System.out.println(" nChannel:" + msg.nChannel );

                    // 用户名称
                    System.out.println(" szUser:" + new String(msg.szUser) );

                    // 操作类型
                    System.out.println(" emOpType:" + msg.emOpType );

                    // 用户类型
                    System.out.println(" emUserType:" + msg.emUserType );


                    break;
                }
                case NET_ALARM_MANUAL_TEST:{    		// 手动测试事件(对应结构体 NET_ALARM_MANUAL_TEST_INFO)

                    System.out.println(" 手动测试事件");
                    NET_ALARM_MANUAL_TEST_INFO msg=new NET_ALARM_MANUAL_TEST_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );
                    // 通道号
                    System.out.println(" nChannel:" + msg.nChannel );
                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString() );
                    // 配件序列号
                    System.out.println(" szSN:" + new String(msg.szSN) );
                    // 配件名称
                    System.out.println(" szName:" + new String(msg.szName) );
                    // 配件所属区域名称
                    System.out.println(" szAreaName:" + new String(msg.szAreaName) );

                    break;
                }
                case NET_ALARM_ATS_FAULT: { 		                // 报警传输系统故障事件(对应结构体 NET_ALARM_ATS_FAULT_INFO)
                    System.out.println(" 报警传输系统故障事件");
                    NET_ALARM_ATS_FAULT_INFO msg=new NET_ALARM_ATS_FAULT_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );
                    // 通道号
                    System.out.println(" nChannel:" + msg.nChannel );
                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString() );

                        break;
                }
                case  NET_ALARM_ARC_OFFLINE:{			// 报警接收中心离线事件(对应结构体 NET_ALARM_ARC_OFFLINE_INFO)

                    System.out.println(" 报警接收中心离线事件");
                    NET_ALARM_ARC_OFFLINE_INFO msg=new  NET_ALARM_ARC_OFFLINE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );
                    // 通道号
                    System.out.println(" nChannel:" + msg.nChannel );
                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString() );

                    // ARC通讯异常描述信息
                    System.out.println(" szDetail:" + new String(msg.szDetail) );


                    break;
                }
                case NET_ALARM_WIFI_FAILURE:{		// wifi故障事件(对应结构体 NET_ALARM_WIFI_FAILURE_INFO)
                    System.out.println(" wifi故障事件");
                    NET_ALARM_WIFI_FAILURE_INFO msg=new  NET_ALARM_WIFI_FAILURE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );

                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString());
                    // 配件序列号
                    System.out.println(" szSN:" + new String(msg.szSN) );

                    // 配件名称
                    System.out.println(" szName:" + new String(msg.szName) );

                    // wifi故障错误码 1:未知错误;2:无效的网络名称;3:无效的网络口令;4:网络故障
                    System.out.println(" nErrorCode:" + msg.nErrorCode );

                    // 所属区域信息个数
                    System.out.println(" nAreaInfoNum:" + msg.nAreaInfoNum );

                    NET_EVENT_AREAR_INFO[] stuAreaInfo = msg.stuAreaInfo;

                    for(int i=0;i<msg.nAreaInfoNum;i++){
                        NET_EVENT_AREAR_INFO info = stuAreaInfo[i];
                        System.out.println(" nIndex:" +info.nIndex );

                        System.out.println(" szName:" +  new String(info.szName) );

                    }

                    break;
                }
                case NET_ALARM_OVER_TEMPERATURE: { // 超温报警事件(对应结构体 NET_ALARM_OVER_TEMPERATURE_INFO)

                    System.out.println(" 超温报警事件");
                    NET_ALARM_OVER_TEMPERATURE_INFO msg=new  NET_ALARM_OVER_TEMPERATURE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    // 事件动作 0:脉冲
                    System.out.println(" nAction:" + msg.nAction );

                    // 事件发生的时间,标准的（不带时区偏差的）UTC时间
                    System.out.println(" stuUTC:" + msg.stuUTC.toString() );
                    // 配件序列号
                    System.out.println(" szSN:" + new String(msg.szSN) );

                    // 配件名称
                    System.out.println(" szSN:" + new String(msg.szName) );

                    // 超温类型 0:温度恢复正常;1:温度超过下限;2:温度超过上限
                    System.out.println(" nTemperatureType:" + msg.nTemperatureType );

                  	// 所属区域信息个数
                    System.out.println(" nAreaInfoNum:" + msg.nAreaInfoNum );

                    NET_EVENT_AREAR_INFO[] stuAreaInfo = msg.stuAreaInfo;

                    for(int i=0;i<msg.nAreaInfoNum;i++){
                        NET_EVENT_AREAR_INFO info = stuAreaInfo[i];

                        System.out.println(" nIndex:" +info.nIndex );

                        System.out.println(" szName:" +  new String(info.szName) );

                    }


                    break;
                }
                default:
                    break;
            }
            return true;
        }
    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "startListen" , "startListen")));
        menu.addItem((new CaseMenu.Item(this , "stopListen" , "stopListen")));
        menu.run();
    }

    public static void main(String[] args) {
        WireLessDevLowDemo wireLessDevLowDemo=new WireLessDevLowDemo();

        InitTest("172.3.3.135",37777,"admin","admin123");
        wireLessDevLowDemo.RunTest();
        LoginOut();

    }
}
