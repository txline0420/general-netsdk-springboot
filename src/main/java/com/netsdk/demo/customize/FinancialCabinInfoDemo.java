package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_FINANCIAL_CABIN_INFO;
import com.netsdk.lib.structure.NET_IN_GET_FINANCIAL_CABIN_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_FINANCIAL_CABIN_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description  FinancialCabinetManager.getCabinInfo: CLIENT_GetFinancialCabinInfo（该接口SDK之前已经支持）
 * @date 2022/8/1 14:07
 */
public class FinancialCabinInfoDemo extends Initialization {


    public void GetFinancialCabinInfo(){

        NET_IN_GET_FINANCIAL_CABIN_INFO input=new NET_IN_GET_FINANCIAL_CABIN_INFO();
                        input.nIndexNum=1;
                        input.nIndex[0]=0;

        Pointer pointerInput
                = new Memory(input.size());
        pointerInput.clear(input.size());

        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_GET_FINANCIAL_CABIN_INFO outPut=new NET_OUT_GET_FINANCIAL_CABIN_INFO();

        Pointer pointerOutPut
                = new Memory(outPut.size());
        pointerOutPut.clear(outPut.size());

        ToolKits.SetStructDataToPointer(outPut,pointerOutPut,0);

        boolean b
                = netSdk.CLIENT_GetFinancialCabinInfo(loginHandle, pointerInput, pointerOutPut, 3000);



        if(b){

            System.out.printf(" CLIENT_GetFinancialCabinInfo Success\n");

            ToolKits.GetPointerDataToStruct(pointerOutPut,0,outPut);

            Native.free(Pointer.nativeValue(pointerInput));
            Pointer.nativeValue(pointerInput, 0);

            Native.free(Pointer.nativeValue(pointerOutPut));
            Pointer.nativeValue(pointerOutPut, 0);

            int nInfoNum = outPut.nInfoNum;

            System.out.println("nInfoNum:"+nInfoNum);

            NET_FINANCIAL_CABIN_INFO[] stuInfo = outPut.stuInfo;


            for(int i=0;i<nInfoNum;i++){
                float fAmount = stuInfo[i].fAmount;

                System.out.println("fAmount:"+fAmount);
                System.out.println("szRFIDNo:"+new String(stuInfo[i].szRFIDNo));

                System.out.println("szBindRFIDNo:"+new String(stuInfo[i].szBindRFIDNo));
                System.out.println("szType:"+  new String(stuInfo[i].szType));

                System.out.println("szDoorState:"+  new String(stuInfo[i].szDoorState));

            }

        }else {
            Native.free(Pointer.nativeValue(pointerInput));//清理内存
            Pointer.nativeValue(pointerInput, 0);

            Native.free(Pointer.nativeValue(pointerOutPut));
            Pointer.nativeValue(pointerOutPut, 0);

            System.out.printf("CLIENT_GetFinancialCabinInfo Failed!LastError = %s\n",
                    ToolKits.getErrorCode());
        }



    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "GetFinancialCabinInfo" , "GetFinancialCabinInfo")));
        menu.run();
    }

    public static void main(String[] args) {
        FinancialCabinInfoDemo financialCabinInfoDemo=new FinancialCabinInfoDemo();
        InitTest("172.8.2.15",37777,"admin","admin123");
        financialCabinInfoDemo.RunTest();
        LoginOut();

    }

}
