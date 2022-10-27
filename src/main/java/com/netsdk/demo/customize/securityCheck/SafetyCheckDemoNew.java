package com.netsdk.demo.customize.securityCheck;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.securityCheck.AnalyzerDataCallBack;
import com.netsdk.lib.callback.securityCheck.NotifyPopulationStatisticsInfoCallBack;
import com.netsdk.lib.structure.NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_IN_GET_POPULATION_STATISTICS;
import com.netsdk.lib.structure.NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_POPULATION_STATISTICS;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/7/20 19:20
 */
public class SafetyCheckDemoNew extends Initialization{

    /**
     10.35.232.160 admin admin123
     */
    private static String ipAddr 				    = "10.35.232.160";
    private static int    port 				    = 37777;
    private static String user 			    = "admin";
    private static String password 		    = "admin123";

  //  NetSDKLib netSdk= Initialization.netSdk;

    // 订阅安检人数句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

    // 订阅安检人数句柄
    private static NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    public void AttachPopulationStatistics(){

        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        DetachPopulationStatistics();
        //入参
        NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO inParam=new NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO();

        Pointer user=new Memory(1024);
        inParam.dwUser=user;
        inParam.cbNotifyPopulationStatisticsInfo = NotifyPopulationStatisticsInfoCallBack.getInstance();

        Pointer pInParam=new Memory(inParam.size());

        ToolKits.SetStructDataToPointer(inParam, pInParam, 0);

        //出参
        NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO outParam=new NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO();

        Pointer pOutParam=new Memory(outParam.size());

        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
////CLIENT_NET_API LLONG CALL_METHOD CLIENT_AttachPopulationStatistics(LLONG lLoginID, NET_IN_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstInParam, NET_OUT_ATTACH_GATE_POPULATION_STATISTICS_INFO* pstOutParam , int nWaitTime);
        AttachHandle= netSdk.CLIENT_AttachPopulationStatistics(loginHandle,pInParam,pOutParam,3000);

        if (AttachHandle.longValue() != 0) {
            System.out.println("CLIENT_AttachPopulationStatistics Success");
        } else {
            System.out.println("CLIENT_AttachPopulationStatistics Failed!LastError = %s\n"+ToolKits.getErrorCode());
        }
    }

    // 取消订阅安检门人数变化信息
    public void DetachPopulationStatistics(){
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_DetachPopulationStatistics(AttachHandle);
        }

    }
    // 获取安检门人数统计信息
    //CLIENT_NET_API BOOL CALL_METHOD CLIENT_GetPopulationStatistics(LLONG lLoginID, const NET_IN_GET_POPULATION_STATISTICS *pInParam, NET_OUT_GET_POPULATION_STATISTICS *pOutParam, int nWaitTime);
    //public boolean CLIENT_GetPopulationStatistics(NetSDKLib.LLong lLoginID, Pointer pstInParam, Pointer pstOutParam, int nWaitTime);

    public void  GetPopulationStatistics(){

        //入参
        NET_IN_GET_POPULATION_STATISTICS inParam=new NET_IN_GET_POPULATION_STATISTICS();

        Pointer pInParam=new Memory(inParam.size());

        ToolKits.SetStructDataToPointer(inParam, pInParam, 0);

        //出参
        NET_OUT_GET_POPULATION_STATISTICS msg=new NET_OUT_GET_POPULATION_STATISTICS();

        Pointer pOutParam=new Memory(msg.size());

        ToolKits.SetStructDataToPointer(msg, pOutParam, 0);

        //CLIENT_NET_API BOOL CALL_METHOD CLIENT_GetPopulationStatistics(LLONG lLoginID, const NET_IN_GET_POPULATION_STATISTICS *pInParam, NET_OUT_GET_POPULATION_STATISTICS *pOutParam, int nWaitTime);

        boolean DoFind = netSdk.CLIENT_GetPopulationStatistics(loginHandle, pInParam, pOutParam, 3000);

        if(!DoFind){
            System.err.printf("Do Find PopulationStatistics.Error[%s]\n", ToolKits.getErrorCode());
            return;
        }

        ToolKits.GetPointerData(pOutParam, msg);

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
    /**
     * 订阅智能分析数据
     */
    public void realLoadPicture() {
        int bNeedPicture = 1; // 是否需要图片
        int ChannelId = 0;   // -1代表全通道

        m_hAttachHandle =  netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_SECURITYGATE_PERSONALARM,
                bNeedPicture , AnalyzerDataCallBack.getInstance() , null , null);
        if(m_hAttachHandle.longValue() != 0) {
            System.out.println("智能订阅成功.");
        } else {
            System.err.println("智能订阅失败." + ToolKits.getErrorCode());
            return;
        }
    }
    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (m_hAttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(m_hAttachHandle);
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "订阅安检门人数变化信息" , "AttachPopulationStatistics"));
        menu.addItem(new CaseMenu.Item(this , "取消订阅安检门人数变化信息" , "DetachPopulationStatistics"));
        menu.addItem((new CaseMenu.Item(this , "获取安检门人数统计信息" , "GetPopulationStatistics")));
        menu.addItem((new CaseMenu.Item(this , "订阅智能分析数据" , "realLoadPicture")));
        menu.addItem((new CaseMenu.Item(this , "取消实时智能分析数据" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        SafetyCheckDemoNew scd=new SafetyCheckDemoNew();
         scd.RunTest();
         LoginOut();
    }
}
