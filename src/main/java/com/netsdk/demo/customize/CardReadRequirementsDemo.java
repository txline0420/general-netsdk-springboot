package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_CFG_CARD_MNG_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.util.Scanner;

import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CARD_MNG;

/**
 * @author 291189
 * @version 1.0
 * @description GIP211129033 暨南大学门禁系统改造工程项目+DH-ASI8214Y-V4+读卡需求
 * @date 2021/12/9 16:30
 */
public class CardReadRequirementsDemo extends Initialization {
    int channel= -1;

    //支持兼容cpu卡和ic卡功能切换
    public void cardMng(){
        //获取
        int type= NET_EM_CFG_CARD_MNG;
        //入参
        NET_CFG_CARD_MNG_INFO msg=new NET_CFG_CARD_MNG_INFO();

        int dwOutBufferSize=msg.size();

        Pointer szOutBuffer =new Memory(dwOutBufferSize);

        ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);

        boolean ret=netSdk.CLIENT_GetConfig(loginHandle, type,channel , szOutBuffer, dwOutBufferSize, 3000, null);

        if(!ret) {
            System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
            return;
        }

        ToolKits.GetPointerData(szOutBuffer, msg);

        int nType = msg.nType;
        System.out.println("nType:"+nType);

        Scanner scanner=new Scanner(System.in);
        System.out.println("输入类型 CPU卡 = 1, IC卡 = 2,");

        nType=scanner.nextInt();

        msg.nType=nType;

        ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);

        ret= netSdk.CLIENT_SetConfig(loginHandle, type, channel, szOutBuffer, msg.size(), 3000, new IntByReference(0),null);

        if(!ret) {
            System.err.printf("CLIENT_SetConfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
            return;
        }
        ToolKits.GetPointerData(szOutBuffer, msg);
         nType = msg.nType;
        System.out.println("nType:"+nType);

    }


    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "支持兼容cpu卡和ic卡功能切换", "cardMng"));
        menu.run();
    }

    public static void main(String[] args) {
        Initialization.InitTest("172.10.9.15", 37777, "admin", "admin123");

        new CardReadRequirementsDemo().RunTest();
        Initialization.LoginOut();
    }

}
