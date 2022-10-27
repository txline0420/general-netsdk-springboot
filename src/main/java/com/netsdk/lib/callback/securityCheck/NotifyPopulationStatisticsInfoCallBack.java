package com.netsdk.lib.callback.securityCheck;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_POPULATION_STATISTICS_INFO;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description    人数变化信息回调函数
 * @date 2021/7/5
 */
public class NotifyPopulationStatisticsInfoCallBack implements fNotifyPopulationStatisticsInfo{


    public static NotifyPopulationStatisticsInfoCallBack singleton;

    private NotifyPopulationStatisticsInfoCallBack(){
    }

    public static NotifyPopulationStatisticsInfoCallBack getInstance(){
        if(singleton==null){

            synchronized(NotifyPopulationStatisticsInfoCallBack.class){

                if(singleton==null){

                    singleton=new NotifyPopulationStatisticsInfoCallBack();

                    return singleton;

                }
            }
        }
        return singleton;
    }



    @Override
    public void invoke(NetSDKLib.LLong lPopulationStatisticsHandle, Pointer pstuPopulationStatisticsInfos, Pointer dwUser) {

   //     NET_POPULATION_STATISTICS_INFO*

        NET_POPULATION_STATISTICS_INFO     msg  = new NET_POPULATION_STATISTICS_INFO();

        ToolKits.GetPointerData(pstuPopulationStatisticsInfos, msg);

        int nPassPopulation = msg.nPassPopulation; // 正向通过人数

        System.out.println("正向通过人数:"+nPassPopulation);

        int nMetalAlarmPopulation = msg.nMetalAlarmPopulation;

        System.out.println("正向触发金属报警人数:"+nMetalAlarmPopulation);

        int nReversePassPopulation = msg.nReversePassPopulation;

        System.out.println("反向通过人数:"+nReversePassPopulation);

        int nReverseMetalAlarmPopulation = msg.nReverseMetalAlarmPopulation;

        System.out.println("反向触发金属报警人数:"+nReverseMetalAlarmPopulation);

        long nTempNormalPopulation = msg.nTempNormalPopulation;

        System.out.println("体温正常人数:"+nTempNormalPopulation);

        long nTempAlarmPopulation = msg.nTempAlarmPopulation;

        System.out.println("体温异常人数:"+nTempAlarmPopulation);
    }
}
