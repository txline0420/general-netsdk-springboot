package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2022/6/23 10:34
 */
public class DynamicLockDemo extends Initialization {

    /**
     * 根据中心公钥获取锁具随机公钥
     */
    public void  GetDynamicLockRandomPublicKey(){

        NET_IN_GET_DYNAMIC_LOCK_RANDOM_PUBLICKEY_INFO input=new NET_IN_GET_DYNAMIC_LOCK_RANDOM_PUBLICKEY_INFO();
/**
 密码锁ID
 */
        String szID="XXiD1";
        ToolKits.StringToByteArr(szID,input.szID);
/**
 中心公钥
 */
        String szCenterPublicKey="xxx1";
        ToolKits.StringToByteArr(szCenterPublicKey,input.szCenterPublicKey);

        Pointer pointerInput=new Pointer(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_GET_DYNAMIC_LOCK_RANDOM_PUBLICKEY_INFO outPut=new NET_OUT_GET_DYNAMIC_LOCK_RANDOM_PUBLICKEY_INFO();
        Pointer pointerOutput=new Pointer(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b = netSdk.CLIENT_GetDynamicLockRandomPublicKey(loginHandle, pointerInput, pointerOutput, 3000);

        if (!b) {
            printlns("CLIENT_GetDynamicLockRandomPublicKey false Last Error:"+ netSdk.CLIENT_GetLastError());
            return;
        }else {
            printlns("CLIENT_GetDynamicLockRandomPublicKey success");
            ToolKits.GetPointerData(pointerOutput, outPut);
            //动态密码锁错误码 {@link com.netsdk.lib.enumeration.EM_DYNAMIC_LOCK_ERRORCODE}
            int emErrorCode = outPut.emErrorCode;

            printlns("emErrorCode:"+emErrorCode);

            byte[] szRandomPublicKey = outPut.szRandomPublicKey;

            try {
                printlns("szRandomPublicKey:"+new String(szRandomPublicKey,encode));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置通讯秘钥
     */
    public void  SetDynamicLockCommunicationKey(){

        NET_IN_SET_DYNAMIC_LOCK_COMMUNICATIONKEY_INFO input=new NET_IN_SET_DYNAMIC_LOCK_COMMUNICATIONKEY_INFO();
                        /**
                         *密码锁ID
                         */
                        String szID="XXiD1";
                        ToolKits.StringToByteArr(szID,input.szID);
                        /**
                         *通讯密钥
                         */
                        String szCommuKey="xxx1";
                        ToolKits.StringToByteArr(szCommuKey,input.szCommuKey);

                        /**
                         通讯密钥校验数据
                         */
                       String szKeyVerify="xxx2";
                       ToolKits.StringToByteArr(szKeyVerify,input.szKeyVerify);

                       /**
                         原通讯密钥校验数据
                         */
                        String szOldKeyVerify="xxx3";
                        ToolKits.StringToByteArr(szOldKeyVerify,input.szOldKeyVerify);

                        /**
                         加密主机私钥签名
                         */
                       String szPrivateSigniture="xxx4";
                       ToolKits.StringToByteArr(szPrivateSigniture,input.szPrivateSigniture);

                        Pointer pointerInput=new Pointer(input.size());
                        pointerInput.clear(input.size());
                        ToolKits.SetStructDataToPointer(input,pointerInput,0);


        NET_OUT_SET_DYNAMIC_LOCK_COMMUNICATIONKEY_INFO outPut=new NET_OUT_SET_DYNAMIC_LOCK_COMMUNICATIONKEY_INFO();
        Pointer pointerOutput=new Pointer(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b
                = netSdk.CLIENT_SetDynamicLockCommunicationKey(loginHandle, pointerInput, pointerOutput, 3000);

        if (!b) {
            printlns("CLIENT_SetDynamicLockCommunicationKey false Last Error:"+ netSdk.CLIENT_GetLastError());
            return;
        }else {
            printlns("CLIENT_SetDynamicLockCommunicationKey success");
            ToolKits.GetPointerData(pointerOutput, outPut);
            //动态密码锁错误码 {@link com.netsdk.lib.enumeration.EM_DYNAMIC_LOCK_ERRORCODE}
            int emErrorCode = outPut.emErrorCode;
            printlns("emErrorCode:"+emErrorCode);

        }

    }

    /**
     * 设置开锁密钥
     */

    public void SetDynamicLockOpenKey(){

        NET_IN_SET_DYNAMIC_LOCK_OPENKEY_INFO input=new NET_IN_SET_DYNAMIC_LOCK_OPENKEY_INFO();

        /**
         *密码锁ID
         */
        String szID="XXiD1";
        ToolKits.StringToByteArr(szID,input.szID);
        /**
         *开锁密钥
         */
        String szOpenKey="xxx1";
        ToolKits.StringToByteArr(szOpenKey,input.szOpenKey);

        /**
         开锁密钥校验
         */
        String szKeyVerify="xxx2";
        ToolKits.StringToByteArr(szKeyVerify,input.szKeyVerify);

        /**
         开锁密钥版本
         */
        String szKeyVersion="xxx3";
        ToolKits.StringToByteArr(szKeyVersion,input.szKeyVersion);

        Pointer pointerInput=new Pointer(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_SET_DYNAMIC_LOCK_OPENKEY_INFO outPut=new NET_OUT_SET_DYNAMIC_LOCK_OPENKEY_INFO();
        Pointer pointerOutput=new Pointer(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b
                = netSdk.CLIENT_SetDynamicLockOpenKey(loginHandle, pointerInput, pointerOutput, 3000);

        if (!b) {
            printlns("CLIENT_SetDynamicLockOpenKey false Last Error:"+ netSdk.CLIENT_GetLastError());
            return;
        }else {
            printlns("CLIENT_SetDynamicLockOpenKey success");
            ToolKits.GetPointerData(pointerOutput, outPut);
            //动态密码锁错误码 {@link com.netsdk.lib.enumeration.EM_DYNAMIC_LOCK_ERRORCODE}
            int emErrorCode = outPut.emErrorCode;
            printlns("emErrorCode:"+emErrorCode);
        }
    }

    /**
     * 设置临时身份码
     */

    public void SetDynamicLockTempUserID(){

        NET_IN_SET_DYNAMIC_LOCK_TEMP_USERID_INFO input=new NET_IN_SET_DYNAMIC_LOCK_TEMP_USERID_INFO();
        /**
         密码锁ID
         */
        String szID="xxx2";
        ToolKits.StringToByteArr(szID,input.szID);

        /**
         临时身份码
         */
        String szTmpUserID="xxx3";
        ToolKits.StringToByteArr(szTmpUserID,input.szTmpUserID);

        Pointer pointerInput=new Pointer(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_SET_DYNAMIC_LOCK_TEMP_USERID_INFO outPut=new NET_OUT_SET_DYNAMIC_LOCK_TEMP_USERID_INFO();
        Pointer pointerOutput=new Pointer(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);


        boolean b
                = netSdk.CLIENT_SetDynamicLockTempUserID(loginHandle, pointerInput, pointerOutput, 3000);
        if (!b) {
            printlns("CLIENT_SetDynamicLockTempUserID false Last Error:"+ netSdk.CLIENT_GetLastError());
            return;
        }else {
            printlns("CLIENT_SetDynamicLockTempUserID success");
            ToolKits.GetPointerData(pointerOutput, outPut);
            //动态密码锁错误码 {@link com.netsdk.lib.enumeration.EM_DYNAMIC_LOCK_ERRORCODE}
            int emErrorCode = outPut.emErrorCode;
            printlns("emErrorCode:"+emErrorCode);
        }
    }


    /**
     * 设置开锁码
     */

    public void  SetDynamicLockOpenCode(){
        NET_IN_SET_DYNAMIC_LOCK_OPEN_CODE_INFO input=new NET_IN_SET_DYNAMIC_LOCK_OPEN_CODE_INFO();
        /**
         密码锁ID
         */
        String szID="xxx2";
        ToolKits.StringToByteArr(szID,input.szID);

        /**
         开锁密钥
         */
        String szOpenCode="xxx3";
        ToolKits.StringToByteArr(szOpenCode,input.szOpenCode);

        Pointer pointerInput=new Pointer(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_SET_DYNAMIC_LOCK_OPEN_CODE_INFO outPut=new NET_OUT_SET_DYNAMIC_LOCK_OPEN_CODE_INFO();

        Pointer pointerOutput=new Pointer(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b
                = netSdk.CLIENT_SetDynamicLockOpenCode(loginHandle, pointerInput, pointerOutput, 3000);

        if (!b) {
            printlns("CLIENT_SetDynamicLockOpenCode false Last Error:"+ netSdk.CLIENT_GetLastError());
            return;
        }else {
            printlns("CLIENT_SetDynamicLockOpenCode success");
            ToolKits.GetPointerData(pointerOutput, outPut);
            //动态密码锁错误码 {@link com.netsdk.lib.enumeration.EM_DYNAMIC_LOCK_ERRORCODE}
            int emErrorCode = outPut.emErrorCode;
            printlns("emErrorCode:"+emErrorCode);
        }

    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "根据中心公钥获取锁具随机公钥" , "GetDynamicLockRandomPublicKey")));
        menu.addItem((new CaseMenu.Item(this , "设置通讯秘钥" , "SetDynamicLockCommunicationKey")));
        menu.addItem((new CaseMenu.Item(this , "设置开锁密钥" , "SetDynamicLockOpenKey")));
        menu.addItem((new CaseMenu.Item(this , "设置临时身份码" , "SetDynamicLockTempUserID")));
        menu.addItem((new CaseMenu.Item(this , "设置开锁码" , "SetDynamicLockOpenCode")));

        menu.run();
    }

    public static void main(String[] args) {
        DynamicLockDemo dynamicLockDemo=new DynamicLockDemo();
        InitTest("192.168.3.110",37777,"admin","admin123");
        dynamicLockDemo.RunTest();
        LoginOut();

    }

}
