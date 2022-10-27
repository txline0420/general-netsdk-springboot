package com.netsdk.lib.callback.securityCheck;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_SECURITYGATE_ALARM_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.sun.jna.Pointer;


/**
 * @author 291189
 * @version 1.0
 * @description 安全门报警订阅回调函数实现类
 * @date 2021/6/29
 */
public class SecurityGateAttachAlarmStatisticsCallBack implements fSecurityGateAttachAlarmStatistics {

    private SecurityGateAttachAlarmStatisticsCallBack() {
    }

    private static class CallBackHolder {
        private static SecurityGateAttachAlarmStatisticsCallBack instance = new SecurityGateAttachAlarmStatisticsCallBack();
    }

    public static SecurityGateAttachAlarmStatisticsCallBack getInstance() {
        return CallBackHolder.instance;
    }


    @Override
    public void invoke(NetSDKLib.LLong lAttachHandle, Pointer pInfo, Pointer dwUser) {

        NET_SECURITYGATE_ALARM_STATISTICS_INFO net_securitygate_alarm_statistics_info=new NET_SECURITYGATE_ALARM_STATISTICS_INFO();

        ToolKits.GetPointerData(pInfo, net_securitygate_alarm_statistics_info);

        byte[] szUUID = net_securitygate_alarm_statistics_info.szUUID;

        System.out.println("uuid:"+new String(szUUID));

        int nAlarmIn = net_securitygate_alarm_statistics_info.nAlarmIn;

        System.out.println("nAlarmIn:"+nAlarmIn);

        int nAlarmOut = net_securitygate_alarm_statistics_info.nAlarmOut;

        System.out.println("nAlarmOut:"+nAlarmOut);

        int nPassIn = net_securitygate_alarm_statistics_info.nPassIn;

        System.out.println("nPassIn:"+nPassIn);

        NET_TIME_EX stuBeginTime = net_securitygate_alarm_statistics_info.stuBeginTime;

        System.out.println("stuBeginTime:"+stuBeginTime);

        NET_TIME_EX stuEndTime = net_securitygate_alarm_statistics_info.stuEndTime;

        System.out.println("stuEndTime:"+stuEndTime);

        int nPassOut = net_securitygate_alarm_statistics_info.nPassOut;

        System.out.println("nPassOut:"+nPassOut);

        int nStatisticsInfoID = net_securitygate_alarm_statistics_info.nStatisticsInfoID;

        System.out.println("nStatisticsInfoID:"+nStatisticsInfoID);


    }
}
