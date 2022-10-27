package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_PERSON_INFO_INPUT_RESULT;
import com.netsdk.lib.structure.NET_OUT_PERSON_INFO_INPUT_RESULT;
import com.netsdk.lib.utils.Initialization;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220107147 电业局阿拉善向德（乌兰布和）220千伏输变电工程智慧工地4.3寸单屏台式指纹人证终端DH-ASHZ230A-W sdk设备返回接口
 * @date 2022/1/11 14:05
 */
public class PersonInfoInputResultDemo extends Initialization {

    // 下发人员信息录入结果
    public void SetPersonInfoInputResult(){

        NET_IN_PERSON_INFO_INPUT_RESULT input=new NET_IN_PERSON_INFO_INPUT_RESULT();

                input.nChannelID=0;

              byte[]		szCitizenID=new byte[64];

            String Id="123";
            ToolKits.StringToByteArray(Id,szCitizenID);

            input.szCitizenID=szCitizenID;

            input.nResult=0;

        NET_OUT_PERSON_INFO_INPUT_RESULT out=new NET_OUT_PERSON_INFO_INPUT_RESULT();


        input.write();
        out.write();
        boolean isSuccess
                = netSdk.CLIENT_SetPersonInfoInputResult(loginHandle, input.getPointer(), out.getPointer(), 3000);

        if(isSuccess){
            System.out.println("CLIENT_SetPersonInfoInputResult success");
        }else {
            System.out.println("CLIENT_SetPersonInfoInputResult fail "+ ToolKits.getErrorCode());
        }

    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "SetPersonInfoInputResult" , "SetPersonInfoInputResult")));

        menu.run();
    }

    public static void main(String[] args) {
        PersonInfoInputResultDemo personInfoInputResultDemo=new PersonInfoInputResultDemo();
        InitTest("172.10.3.154",37777,"admin","admin123");
        personInfoInputResultDemo.RunTest();
        LoginOut();
    }
}
