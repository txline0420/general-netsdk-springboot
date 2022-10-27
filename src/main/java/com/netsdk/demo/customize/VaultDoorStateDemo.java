package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_REMOTE_APPROVAL_ALARM;
import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FINANCIAL_CABINET_CONFIG;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220729030 自研业务库，JAVA版demo
 * @date 2022/8/10 10:23
 */
public class VaultDoorStateDemo extends Initialization {


    int channel=-1;
    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);

    public void GetVaultDoorState(){

        NET_IN_GET_VAULTDOOR_STATE_INFO input=new NET_IN_GET_VAULTDOOR_STATE_INFO();
        Pointer pointerInput = new Memory(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input, pointerInput, 0);

        NET_OUT_GET_VAULTDOOR_STATE_INFO outPut=new NET_OUT_GET_VAULTDOOR_STATE_INFO();
        Pointer pointerOutput = new Memory(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut, pointerOutput, 0);

        boolean b
                = netSdk.CLIENT_GetVaultDoorState(loginHandle, pointerInput, pointerOutput, 5000);

        if(b){
            System.out.printf(" CLIENT_GetVaultDoorState Success\n");

            ToolKits.GetPointerDataToStruct(pointerOutput,0,outPut);
            Native.free(Pointer.nativeValue(pointerInput)); //清理内存
            Pointer.nativeValue(pointerInput, 0);

            Native.free(Pointer.nativeValue(pointerOutput));
            Pointer.nativeValue(pointerOutput, 0);

            System.out.println("emDoorState:"+ outPut.emDoorState);

            System.out.println("emLockState:"+ outPut.emLockState);
        }else {
            Native.free(Pointer.nativeValue(pointerInput)); //清理内存
            Pointer.nativeValue(pointerInput, 0);  //防止gc重复回收

            Native.free(Pointer.nativeValue(pointerOutput));
            Pointer.nativeValue(pointerOutput, 0);

            System.out.printf("CLIENT_GetVaultDoorState Failed!LastError = %s\n",
                    ToolKits.getErrorCode());
        }



    }


    public void GetFinancialCabinetState(){

        NET_IN_GET_CABINET_STATE_INFO input=new NET_IN_GET_CABINET_STATE_INFO();

        Pointer pointerInput = new Memory(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input, pointerInput, 0);

        NET_OUT_GET_CABINET_STATE_INFO outPut=new NET_OUT_GET_CABINET_STATE_INFO();

        Pointer pointerOutput = new Memory(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut, pointerOutput, 0);

        boolean b
                = netSdk.CLIENT_GetFinancialCabinetState(loginHandle, pointerInput, pointerOutput, 5000);

        if(b){
            System.out.printf(" CLIENT_GetFinancialCabinetState Success\n");

            ToolKits.GetPointerDataToStruct(pointerOutput,0,outPut);
            Native.free(Pointer.nativeValue(pointerInput)); //清理内存
            Pointer.nativeValue(pointerInput, 0);   //防止gc重复回收

            Native.free(Pointer.nativeValue(pointerOutput));
            Pointer.nativeValue(pointerOutput, 0);


            System.out.println("fTemperature:"+ outPut.fTemperature);

            System.out.println("fHumidity:"+ outPut.fHumidity);
        }else {
            Native.free(Pointer.nativeValue(pointerInput)); //清理内存
            Pointer.nativeValue(pointerInput, 0);

            Native.free(Pointer.nativeValue(pointerOutput));
            Pointer.nativeValue(pointerOutput, 0);
            System.out.printf("CLIENT_GetFinancialCabinetState Failed!LastError = %s\n",
                    ToolKits.getErrorCode());
        }




    }


    public void financialCabinetConfig(){
        //获取
        int emCfgOpType
                = NET_EM_CFG_FINANCIAL_CABINET_CONFIG;

        NET_CFG_FINANCIAL_CABINET_CONFIG_INFO msg=new NET_CFG_FINANCIAL_CABINET_CONFIG_INFO();

        //入参
        Pointer szOutBuffer =new Memory(msg.size());
        szOutBuffer.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
        boolean b=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, -1, szOutBuffer, msg.size(), 3000, null);

        if(!b){

            Native.free(Pointer.nativeValue(szOutBuffer)); //清理内存
            Pointer.nativeValue(szOutBuffer, 0);

            System.out.printf("CLIENT_GetConfig Failed!LastError = %s\n",
                    ToolKits.getErrorCode());

            return;
        }

            System.out.printf(" CLIENT_GetConfig Success\n");

            ToolKits.GetPointerDataToStruct(szOutBuffer,0,msg);
            Native.free(Pointer.nativeValue(szOutBuffer)); //清理内存
            Pointer.nativeValue(szOutBuffer, 0);

            NET_FINANCIAL_CABINET_MESSAGE stuMobileMessage
                    = msg.stuMobileMessage;


            System.out.println("bEnable:"+stuMobileMessage.bEnable);


            int nPhoneNumberNum
                    = stuMobileMessage.nPhoneNumberNum;

            System.out.println("nPhoneNumberNum:"+nPhoneNumberNum);

            ByteSize16[] szPhoneNumber
                    = stuMobileMessage.szPhoneNumber;

            for(int i=0;i<nPhoneNumberNum;i++){
                ByteSize16 byteSize16
                        = szPhoneNumber[i];

                System.out.println("["+i+"]:"+new String(byteSize16.byte16));
            }

            if(nPhoneNumberNum>0){
            ToolKits.ByteArrZero(szPhoneNumber[0].byte16);

            String phone="18955652222";

            ToolKits.StringToByteArray(phone,szPhoneNumber[0].byte16);
            }
                stuMobileMessage.bEnable=1;
            //下发

        IntByReference restart = new IntByReference(0);
        Pointer szInBuffer =new Memory(msg.size());
        szInBuffer.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
        boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, -1, szInBuffer, msg.size(), 3000, restart, null);

        if(result) {

            Native.free(Pointer.nativeValue(szInBuffer)); //清理内存
            Pointer.nativeValue(szInBuffer, 0);

            System.out.println("CLIENT_SetConfig success");
        }else {
            Native.free(Pointer.nativeValue(szInBuffer)); //清理内存
            Pointer.nativeValue(szInBuffer, 0);
            System.err.println("CLIENT_SetConfig field");
        }
    }

    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if(attachHandle.longValue()!=0){
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, EVENT_IVS_REMOTE_APPROVAL_ALARM, bNeedPicture,
                AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

        return attachHandle;
    }
    /**
     * 报警事件（智能）回调
     */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private final File picturePath;
        private static AnalyzerDataCB instance;

        private AnalyzerDataCB() {
            picturePath = new File("./AnalyzerPicture/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }

        public static AnalyzerDataCB getInstance() {
            if (instance == null) {
                synchronized (AnalyzerDataCB.class) {
                    if (instance == null) {
                        instance = new AnalyzerDataCB();
                    }
                }
            }
            return instance;
        }


        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (dwAlarmType) {
                case EVENT_IVS_REMOTE_APPROVAL_ALARM : {//金融远程审批事件
                            System.out.println(" 金融远程审批事件");
                    NET_DEV_EVENT_REMOTE_APPROVAL_ALARM_INFO msg=new NET_DEV_EVENT_REMOTE_APPROVAL_ALARM_INFO();

                         ToolKits.GetPointerData(pAlarmInfo, msg);
                 //   nChannelID
                            /**
                            通道号
                            */
                            System.out.println("nChannelID:"+msg.nChannelID);
                            /**
                             1:开始 2:停止
                             */
                             System.out.println("nAction:"+msg.nAction);

                    try {
                        /**
                         事件名称
                         */
                        System.out.println("szName UTF-8:"+new String(msg.szName,"UTF-8"));

                        System.out.println("szName GBK:"+new String(msg.szName,"GBK"));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    /**
                     时间戳(单位是毫秒)
                     */
                    System.out.println("PTS:"+msg.PTS);

                    /**
                     事件发生的时间
                     */
                    System.out.println("UTC:"+msg.UTC);

                    /**
                     事件ID
                     */
                    System.out.println("nEventID:"+msg.nEventID);

                    /**
                     审批单号
                     */
                    System.out.println("nApprovalNo:"+msg.nApprovalNo);

                    /**
                     操作类型，-1:未知，0:存箱, 1:取箱
                     */
                    System.out.println("nType:"+msg.nType);
                    /**
                     审批超时时间，单位秒
                     */
                    System.out.println("nTimeout:"+msg.nTimeout);
                        /**
                         存取的舱位下标数量
                         */
                    System.out.println("nCabinListNum:"+msg.nCabinListNum);

                    /**
                     存取的舱位下标数组
                     */
                    int[] nCabinList
                            = msg.nCabinList;

                    for(int i=0;i<msg.nCabinListNum;i++){

                        System.out.println("["+i+"]"+nCabinList[i]);

                    }
                    /**
                     存取人ID
                     */
                    System.out.println("szAccessorID:"+new String(msg.szAccessorID));


                    try {
                        System.out.println("szAccessorName UTF-8:"+new String(msg.szAccessorName,"UTF-8"));

                        System.out.println("szAccessorName GBK:"+new String(msg.szAccessorName,"GBK"));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    /**
                     复核人ID
                     */
                    System.out.println("szReviewerID:"+new String(msg.szReviewerID));



                    try {
                        System.out.println("szReviewerName UTF-8:"+new String(msg.szReviewerName,"UTF-8"));

                        System.out.println("szReviewerName GBK:"+new String(msg.szReviewerName,"GBK"));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                        /**
                         图片信息个数
                         */
                    int nImageInfoNum
                            = msg.nImageInfoNum;

                    System.out.println("nImageInfoNum:"+nImageInfoNum);

                    NET_IMAGE_INFO_EX2[] stuImageInfo
                            = msg.stuImageInfo;

                    for(int i=0;i<nImageInfoNum;i++){
                        NET_IMAGE_INFO_EX2 image
                                = stuImageInfo[i];

                        if (dwBufSize > 0) {
                            String picture = picturePath + "/" + System.currentTimeMillis() + "_info.jpg";
                            ToolKits.savePicture(pBuffer, image.nOffset, image.nLength, picture);
                        }
                        System.out.println("szPath:"+ new String(image.szPath));

                    }

                    break;
                }
                default:
                    System.out.println("其他事件--------------------"+ dwAlarmType);
                    break;
            }
            return 0;
        }
    }


    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (this.attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(this.attachHandle);
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "financialCabinetConfig" , "financialCabinetConfig")));
        menu.addItem((new CaseMenu.Item(this , "GetFinancialCabinetState" , "GetFinancialCabinetState")));
        menu.addItem((new CaseMenu.Item(this , "GetVaultDoorState" , "GetVaultDoorState")));
        menu.run();
    }

    public static void main(String[] args) {
        VaultDoorStateDemo vaultDoorStateDemo=new VaultDoorStateDemo();
        InitTest("172.25.160.51",37777,"admin","admin123");
        vaultDoorStateDemo.RunTest();
        LoginOut();

    }
}
