package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.CFG_PTZ_ALL_AUTOMOVE_INFO;
import com.netsdk.lib.structure.CFG_PTZ_AUTOMOVE_INFO;
import com.netsdk.lib.structure.CFG_PTZ_PER_AUTOMOVE_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;


/**
 * @创建人 291189
 * @创建时间 2021/5/28
 * @描述
 */
public class PtzAutoMovementConfig extends Initialization {

    int channa=2;

    /**
     * 云台定时动作配置
     */
    public void  setPtzAutoMovementConfig(){

        String cfgCmdPtzAutoMovement
                = NetSDKLib.CFG_CMD_PTZ_AUTO_MOVEMENT;
        CFG_PTZ_AUTOMOVE_INFO  cfgPtzAutomoveInfo=new CFG_PTZ_AUTOMOVE_INFO();

        cfgPtzAutomoveInfo. pstPTZAutoConfig=new Memory(new CFG_PTZ_ALL_AUTOMOVE_INFO().size());

        boolean isScuess
                = ToolKits.GetDevConfig(loginHandle, channa,cfgCmdPtzAutoMovement , cfgPtzAutomoveInfo);

        if (!isScuess) {
            System.err.println("Get PtzAutoMovementConfig Failed!" + ToolKits.getErrorCode());
            return;
        }

        int nReturnPTZNum
                = cfgPtzAutomoveInfo.nReturnPTZNum;
        System.out.println("设备返回的云台个数:"+nReturnPTZNum);


        //云台的配置信息
        Pointer pstPTZAutoConfig
                = cfgPtzAutomoveInfo.pstPTZAutoConfig;
        //pointer 对应的结构体
        CFG_PTZ_ALL_AUTOMOVE_INFO cfgInfo=new CFG_PTZ_ALL_AUTOMOVE_INFO();

        ToolKits.GetPointerData(pstPTZAutoConfig,cfgInfo);//pointer 转结构体

        int nCfgNum = cfgInfo.nCfgNum;
        System.out.println("获取到的配置个数:"+nCfgNum);

        //配置信息
        CFG_PTZ_PER_AUTOMOVE_INFO[] stPTZPerInfo
                = cfgInfo.stPTZPerInfo;

        for(int i=0;i<nCfgNum;i++){
            CFG_PTZ_PER_AUTOMOVE_INFO  info  = stPTZPerInfo[i];
            System.out.println("定时动作开关标志:"+info.bEnable);
            System.out.println("定时功能:"+info.emFuncType);

            info.bEnable=1;

        }

        cfgPtzAutomoveInfo.nMaxPTZNum=1;//结构体申请的云台个数;(对于多通道查询，申请不小于设备通数，对于单通道查询，一个就够了)需赋值

        Pointer dInfo=new Memory(cfgInfo.size());//结构体转换成pointer对象

        ToolKits.SetStructDataToPointer(cfgInfo, dInfo, 0);

        cfgPtzAutomoveInfo.pstPTZAutoConfig=dInfo;


        try {
            isScuess = ToolKits.SetDevConfig( loginHandle,channa,cfgCmdPtzAutoMovement,cfgPtzAutomoveInfo);

            if (!isScuess) {
                System.err.println("set PtzAutoMovementConfig Failed!" + ToolKits.getErrorCode());

            }else {
                System.out.println("set PtzAutoMovementConfig success!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "云台配置", "setPtzAutoMovementConfig"));
        menu.run();
    }


    public static void main(String[] args) {

        Initialization.InitTest("172.32.1.110", 37777, "admin", "admin110");

        new PtzAutoMovementConfig().RunTest();
        Initialization.LoginOut();

    }

}
