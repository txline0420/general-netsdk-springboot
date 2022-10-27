package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_RADAR_SET_RFID_MODE;
import com.netsdk.lib.structure.NET_OUT_REMOTE_SLEEP;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220315013 4G低功耗开发远程休眠模式SDK开发
 * @date 2022/3/24 13:33
 */
public class RemoteSleepDemo extends Initialization {

    public void RemoteSleep(){

        NET_IN_RADAR_SET_RFID_MODE input=new NET_IN_RADAR_SET_RFID_MODE();
        NET_OUT_REMOTE_SLEEP outPut=new NET_OUT_REMOTE_SLEEP();

        Pointer memory_input
                = new Memory(input.size());
        memory_input.clear(input.size());
        ToolKits.SetStructDataToPointer(input,memory_input,0);
        Pointer memory_out
                = new Memory(outPut.size());
        memory_out.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,memory_out,0);

        boolean isSuccess
                = netSdk.CLIENT_RemoteSleep(loginHandle, memory_input, memory_out, 3000);

        if(isSuccess){
            System.out.println("调用成功");
        }else {
            System.out.printf("调用失败  错误码 [%s] \n",ToolKits.getErrorCode());
        }
    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "RemoteSleep" , "RemoteSleep")));

        menu.run();
    }

    public static void main(String[] args) {
        RemoteSleepDemo remoteSleepDemo=new RemoteSleepDemo();
        InitTest("172.32.0.22",37777,"admin","admin123");
        remoteSleepDemo.RunTest();
        LoginOut();

    }

}
