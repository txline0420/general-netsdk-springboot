package com.netsdk.lib.callback.securityCheck;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_PACKAGE_STATISTICS_BYGRADE;
import com.netsdk.lib.structure.NET_IN_PACKAGE_STATISTICS_BYTYPE;
import com.netsdk.lib.structure.NET_IN_XRAY_PACKAGE_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.sun.jna.Pointer;
/**
 * @author 291189
 * @version 1.0
 * @description 安检机包裹数量统计信息订阅回调函数实现类
 * @date 2021/7/1
 */
public class PackageInformationCallBack implements fXRayAttachPackageStatistics{

    private PackageInformationCallBack() {
    }

    private static class CallBackHolder {
        private static PackageInformationCallBack instance = new PackageInformationCallBack();
    }

    public static PackageInformationCallBack getInstance() {
        return CallBackHolder.instance;
    }

    @Override
    public void invoke(NetSDKLib.LLong lAttachHandle, Pointer pInfo, Pointer dwUser) {
        NET_IN_XRAY_PACKAGE_STATISTICS_INFO msg=new NET_IN_XRAY_PACKAGE_STATISTICS_INFO();

        ToolKits.GetPointerData(pInfo, msg);
        // UUID
        byte[] szUUID = msg.szUUID;
        System.out.println("uuid:"+new String(szUUID));
        // 统计信息ID
        int nStatisticsInfoID = msg.nStatisticsInfoID;
        System.out.println("nStatisticsInfoID:"+nStatisticsInfoID);

        //开始时间
        NET_TIME_EX stuBeginTime = msg.stuBeginTime;
        System.out.println("stuBeginTime:"+stuBeginTime);
        // 结束时间
        NET_TIME_EX stuEndTime = msg.stuEndTime;
        System.out.println("stuEndTime:"+stuEndTime);
        // 包裹总数
        int nTotalCount = msg.nTotalCount;
        System.out.println("nTotalCount:"+nTotalCount);
        // 按危险等级统计的信息的数量
        int nStatisticsInfoByGradeNum = msg.nStatisticsInfoByGradeNum;
        System.out.println("nStatisticsInfoByGradeNum:"+nStatisticsInfoByGradeNum);

        // 按危险等级统计的信息
        NET_IN_PACKAGE_STATISTICS_BYGRADE[] stuStatisticsInfoByGrade =
                msg.stuStatisticsInfoByGrade;
            for(int i=0;i<stuStatisticsInfoByGrade.length;i++){
                NET_IN_PACKAGE_STATISTICS_BYGRADE stuStatisticsInfoGrade = stuStatisticsInfoByGrade[i];
                System.out.println("【stuStatisticsInfoGrade】:"+"emGrade="+stuStatisticsInfoGrade.emGrade+",nCount="+stuStatisticsInfoGrade.nCount);
            }
        //按危险类型统计的信息的数量
        int nStatisticsInfoByTypeNum = msg.nStatisticsInfoByTypeNum;
        System.out.println("nStatisticsInfoByTypeNum:"+nStatisticsInfoByTypeNum);

        // 按危险类型统计的信息
        NET_IN_PACKAGE_STATISTICS_BYTYPE[] stuStatisticsInfoByType = msg.stuStatisticsInfoByType;
        for(int i=0;i<stuStatisticsInfoByType.length;i++){
            NET_IN_PACKAGE_STATISTICS_BYTYPE stuStatisticsInfoType = stuStatisticsInfoByType[i];
            System.out.println("【stuStatisticsInfoType】:"+"emGrade="+stuStatisticsInfoType.emType+",nCount="+stuStatisticsInfoType.nCount);
        }


    }
}
