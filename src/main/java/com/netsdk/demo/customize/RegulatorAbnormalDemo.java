package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.CFG_REGULATOR_DETECT_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description  GIP220708004 成都智元汇平台接入SDK配套开发
 * @date 2022/7/23 10:14
 */
public class RegulatorAbnormalDemo extends Initialization {


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
    private static class fAlarmDataCB implements NetSDKLib.fMessCallBack{
        private fAlarmDataCB(){}

        private static class fAlarmDataCBHolder {
            private static fAlarmDataCB callback = new fAlarmDataCB();
        }

        public static fAlarmDataCB getCallBack() {
            return fAlarmDataCBHolder.callback;
        }

        public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser){
//	  		System.out.printf("command = %x\n", lCommand);
            switch (lCommand)
            {
                case NetSDKLib.NET_ALARM_REGULATOR_ABNORMAL: {  // // 标准黑体源异常报警事件(对应结构体 ALARM_REGULATOR_ABNORMAL_INFO)

                    System.out.println(" 标准黑体源异常报警事件");
                    NetSDKLib.ALARM_REGULATOR_ABNORMAL_INFO msg = new NetSDKLib.ALARM_REGULATOR_ABNORMAL_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println(" nAction:"+ msg.nAction);

                    System.out.println(" nChannelID:"+ msg.nChannelID);

                    System.out.println(" szName:"+ new String(msg.szName));

                    System.out.println(" UTC:"+ msg.UTC.toStringTime());

                    System.out.println(" nEventID:"+ msg.nEventID);

                    System.out.println(" szTypes:"+ new String(msg.szTypes));
                    break;

                }



                default:
                    break;
            }
            return true;
        }
    }
//    CLIENT_GetNewDevConfig和CLIENT_SetNewDevConfig + EVENT_IVS_ANATOMY_TEMP_DETECT + CFG_ANATOMY_TEMP_DETECT_INFO
    //人体温智能检测事件配置
    public void SetTempEnable() {
        int channel =1; // 通道号
        String command = NetSDKLib.CFG_CMD_ANALYSERULE;  //视频分析规则配置(对应 CFG_ANALYSERULES_INFO)

        int ruleCount = 10;  // 事件规则个数
        NetSDKLib.CFG_RULE_INFO[] ruleInfo = new NetSDKLib.CFG_RULE_INFO[ruleCount];
        for(int i = 0; i < ruleCount; i++) {
            ruleInfo[i] = new NetSDKLib.CFG_RULE_INFO();
        }

        NetSDKLib.CFG_ANALYSERULES_INFO analyse = new NetSDKLib.CFG_ANALYSERULES_INFO();
        analyse.nRuleLen = 1024 * 1024 * 40;
        analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
        analyse.pRuleBuf.clear(1024 * 1024 * 40);

        // 获取
        if(ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
            int offset = 0;
            System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);

            int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;

            for(int i = 0; i < count; i++) {
                ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);

                offset += ruleInfo[0].size();   // 智能规则偏移量

                switch (ruleInfo[i].dwRuleType) {
                    case NetSDKLib.EVENT_IVS_ANATOMY_TEMP_DETECT:   // 人体温智能检测事件
                    {
                        System.out.println(" 人体温智能检测事件");
                        NetSDKLib.CFG_ANATOMY_TEMP_DETECT_INFO msg = new NetSDKLib.CFG_ANATOMY_TEMP_DETECT_INFO();
                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
                        try {
                            System.out.println("规则名称 gbk：" + new String(msg.szRuleName,"GBK").trim());
                            System.out.println("规则名称 utf-8：" + new String(msg.szRuleName,"UTF-8").trim());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        System.out.println("使能：" + msg.bRuleEnable);
                        ruleInfo[i].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_ANATOMYTEMP_DETECT;
                        ToolKits.SetStructDataToPointer(ruleInfo[i], analyse.pRuleBuf, offset - ruleInfo[0].size());

                        ToolKits.ByteArrZero(msg.szRuleName);//重新赋值前先将之前的数据清空
                        // 设置使能开
                        System.arraycopy("TEMP".getBytes(), 0, msg.szRuleName, 0, "TEMP".getBytes().length);
                        msg.bRuleEnable = 1;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
                        break;
                    }
                    default:
                        break;
                }

                offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
            }

            // 设置
            if(ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
                System.out.println("设置使能成功!");
            } else {
                System.err.println("设置使能失败!" + ToolKits.getErrorCode());
            }
        } else {
            System.err.println("获取使能失败!" + ToolKits.getErrorCode());
        }
    }


    public  void regulator(){
        int chanl=1;
      CFG_REGULATOR_DETECT_INFO stuPTZ = new CFG_REGULATOR_DETECT_INFO();

        if (!ToolKits.GetDevConfig(loginHandle, chanl, NetSDKLib.CFG_CMD_REGULATOR_DETECT, stuPTZ)) {
            System.err.println("Get CFG_REGULATOR_DETECT_INFO Failed!" + ToolKits.getErrorCode());
            return;
        }

            System.out.println("bEnable:"+stuPTZ.bEnable);
                //灵敏度, 1-100
            System.out.println("nSensitivity:"+stuPTZ.nSensitivity);

            stuPTZ.bEnable = 1;    // 使能
            /**
             灵敏度, 1-100
             */
            stuPTZ.nSensitivity = 80; //
        if (!ToolKits.SetDevConfig(loginHandle, chanl, NetSDKLib.CFG_CMD_REGULATOR_DETECT, stuPTZ)) {
            System.err.println("Set PTZ Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("Set PTZ Success!");
        }
    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "SetTempEnable" , "SetTempEnable")));
        menu.addItem((new CaseMenu.Item(this , "regulator" , "regulator")));
        menu.addItem((new CaseMenu.Item(this , "startListen" , "startListen")));
        menu.addItem((new CaseMenu.Item(this , "stopListen" , "stopListen")));
        menu.run();
    }

    public static void main(String[] args) {
        RegulatorAbnormalDemo regulatorAbnormalDemo=new RegulatorAbnormalDemo();

        InitTest("10.35.232.227",40066,"admin","admin123");
        regulatorAbnormalDemo.RunTest();
        LoginOut();

    }

}
