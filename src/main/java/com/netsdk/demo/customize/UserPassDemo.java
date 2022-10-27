package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_ALARM_USER_PASS_CONFIRM_INFO;
import com.netsdk.lib.structure.NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220518009  ERR220509180 常州开放体育馆--DH-ASGB210Y--对接DH-ASI7215X-V1-TQ进出数据，推送三方
 * @date 2022/5/26 17:01
 */
public class UserPassDemo extends Initialization {

    /**(无图的)
     * 订阅报警信息
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

    /**（无图）
     * 取消订阅报警信息
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
            switch (lCommand)
            {

                case NetSDKLib.NET_ALARM_USER_PASS_CONFIRM: {     //  用户通过闸机进入或离开事件(对应结构体 NET_ALARM_USER_PASS_CONFIRM_INFO)
                    NET_ALARM_USER_PASS_CONFIRM_INFO msg = new NET_ALARM_USER_PASS_CONFIRM_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("用户通过闸机进入或离开事件");
                    //事件动作,1表示持续性事件开始,2表示持续性事件结束;
                    int nAction = msg.nAction;
                    System.out.println("事件动作:"+nAction);

                    int nChannelID = msg.nChannelID;
                    System.out.println("通道号:"+nChannelID);

                    NET_TIME_EX stuTime
                            = msg.stuTime;
                    System.out.println("事件发生的时间:"+stuTime);
                     //stuRealUTC 是否有效，bRealUTC 为 1 时，用 stuRealUTC，否则用 stuTime 字段
                    int bRealUTC = msg.bRealUTC;
                    System.out.println("事件发生的时间:"+bRealUTC);

                    NET_TIME_EX stuRealUTC
                            = msg.stuRealUTC;
                    System.out.println("事件发生的时间(标准UTC时间):"+stuRealUTC);

                    int nCount
                            = msg.nCount;
                    System.out.println("通过人数数量:"+nCount);

                    int emType
                            = msg.emType;
                    System.out.println("进出方向:"+emType);

                    break;
                }

                default:
                    System.out.println("lCommand:"+lCommand);
                    break;
            }
            return true;
        }
    }


    //定期通行人数清除功能配置
    public void userPassDataCountClearInfo(){
        // 通道无关, 通道号填-1
        int nChannelID=-1;
        NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR_INFO msg=new NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR_INFO();
        Pointer pstuConfigInfo=new Memory(msg.size());
        pstuConfigInfo.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg,pstuConfigInfo,0);
        boolean gRet = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR, nChannelID, pstuConfigInfo, msg.size(), 3000, null);
        if (!gRet)
        {
            System.out.println("CLIENT_GetConfig fail,error:"+ToolKits.getErrorCode());

            return;
        }
        ToolKits.GetPointerData(pstuConfigInfo, msg);
        System.out.println("使能:"+msg.bEnable);
        System.out.println("定期人数清除周期:"+msg.emPeriod);

        msg.bEnable=1;
        msg.emPeriod=2;

        Pointer pstuOutInfo=new Memory(msg.size());
        pstuOutInfo.clear(msg.size());

        ToolKits.SetStructDataToPointer(msg, pstuOutInfo, 0);
        boolean sRet=netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_USER_PASS_DATA_COUNT_CLEAR, nChannelID, pstuOutInfo, msg.size(), 3000, null, null);
        if(!sRet){

            System.out.println("CLIENT_SetConfig fail,error:"+ToolKits.getErrorCode());

            return;
        }else{
            System.out.printf("CLIENT_SetConfig success\n");
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "userPassDataCountClearInfo" , "userPassDataCountClearInfo")));
        menu.addItem((new CaseMenu.Item(this , "startListen" , "startListen")));
        menu.addItem((new CaseMenu.Item(this , "stopListen" , "stopListen")));
        menu.run();
    }

    public static void main(String[] args) {

        UserPassDemo userPassDemo=new UserPassDemo();
        InitTest("172.10.3.52",37777,"admin","admin123");
        userPassDemo.RunTest();
        LoginOut();

    }
}
