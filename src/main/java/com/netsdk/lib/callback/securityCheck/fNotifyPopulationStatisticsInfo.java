package com.netsdk.lib.callback.securityCheck;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.SDKCallback;
import com.sun.jna.Pointer;


/**
 * @author ： 291189
 * @since ： Created in 2021/7/5
// 接口 CLIENT_AttachPopulationStatistics 回调函数
// pstuPopulationStatisticsInfos 人数变化信息
 */
public interface fNotifyPopulationStatisticsInfo extends SDKCallback {

    void invoke(
            NetSDKLib.LLong lPopulationStatisticsHandle,
            Pointer pstuPopulationStatisticsInfos,
            Pointer dwUser);
    //typedef int (CALLBACK *fNotifyPopulationStatisticsInfo)(LLONG lPopulationStatisticsHandle, NET_POPULATION_STATISTICS_INFO* pstuPopulationStatisticsInfos, LDWORD dwUser);
}
