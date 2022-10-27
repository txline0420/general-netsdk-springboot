package com.netsdk.demo.customize;


import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220629200 远州+netsdk+java封装第二期
 * @date 2022/7/4 10:45
 */
public class PollingConfigDemo extends Initialization {


    /**
     * 平台下发轮询配置，规则和场景字段见附件
     */

    public void setPollingConfig(){

        NET_IN_SET_POLLING_CONFIG input=new NET_IN_SET_POLLING_CONFIG();

        input.nConfigCnt=1;

        NET_SET_POLLING_CONFIG_INFO info=new NET_SET_POLLING_CONFIG_INFO();
                    info.bEnable=1;

        NET_SET_POLLING_CONFIG_INFO  setInfo=new NET_SET_POLLING_CONFIG_INFO();

            setInfo.bEnable=1;

            setInfo.nChannel=0;

        /**
         规则配置个数
         */
        setInfo. nRulelTypeCnt=2;
/**
 * 规则配置
 */
        ToolKits.StringToByteArray("ObjectRemoval",    setInfo.szRulelType[0].arr);
        ToolKits.StringToByteArray("ObjectPlacement",    setInfo.szRulelType[1].arr);
/**
 全局配置列表
 */
        ToolKits.StringToByteArray("ObjectMonitor",    setInfo.szGlobalTypeList[0].arr);
/**
 全局配置列表个数
 */
        setInfo.nGlobalTypeListNum=1;

        input.stuConfigInfos[0]=setInfo;

        Pointer pointerInput
                = new Memory(input.size());

        pointerInput.clear(input.size());

        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_SET_POLLING_CONFIG outPut=new NET_OUT_SET_POLLING_CONFIG();

        Pointer pointerOutput
                = new Memory(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b
                = netSdk.CLIENT_SetPollingConfig(loginHandle, pointerInput, pointerOutput, 3000);

        if(b){
            System.out.println("CLIENT_SetPollingConfig success");
        }else {
            System.out.println("CLIENT_SetPollingConfig fail:"+ToolKits.getErrorCode());

        }

    }

    /**
     * 按通道获取设备智能业务的运行状态
     */
    public    void  getChannelState(){
        NET_IN_GET_CHANNEL_STATE input=new NET_IN_GET_CHANNEL_STATE();
        input.nChannelNum=1;
        input.nChannel[0]=0;
        Pointer pointerInput
                = new Memory(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_GET_CHANNEL_STATE outPut=new NET_OUT_GET_CHANNEL_STATE();

        outPut.nMaxStateNum=2;

        NET_CHANNEL_STATE_INFO[]  infos=new NET_CHANNEL_STATE_INFO[outPut.nMaxStateNum];

                for(int i=0;i<infos.length;i++){
                    infos[i]=new NET_CHANNEL_STATE_INFO();
                }


            outPut.pstuState
                = new Memory(infos[0].size() * infos.length);

             outPut.pstuState.clear(infos[0].size() * infos.length);

                ToolKits.SetStructArrToPointerData(infos,outPut.pstuState);



        Pointer pointerOutput
                = new Memory(outPut.size());
        pointerOutput.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointerOutput,0);

        boolean b
                = netSdk.CLIENT_GetChannelState(loginHandle, pointerInput, pointerOutput, 3000);

        if(b){
            System.out.println("CLIENT_GetChannelState success");

            ToolKits.GetPointerData(pointerOutput,outPut);

            int nMaxStateNum
                    = outPut.nMaxStateNum;
            //用户申请智能业务状态信息最大个数
            System.out.println("用户申请智能业务状态信息最大个数:"+nMaxStateNum);

            //
            System.out.println("智能业务状态信息实际个数:"+outPut.nStateNum);


                NET_CHANNEL_STATE_INFO[]  stateInfos=
                        new NET_CHANNEL_STATE_INFO[outPut.nMaxStateNum];
                for(int i=0;i<stateInfos.length;i++){
                    stateInfos[i]=new NET_CHANNEL_STATE_INFO();
                }

                ToolKits.GetPointerDataToStructArr(outPut.pstuState,stateInfos);

            for(int i=0;i<outPut.nStateNum;i++){

                NET_CHANNEL_STATE_INFO info
                        = stateInfos[i];

                /**
                 通道号
                 */
                int nChannel
                        = info.nChannel;
                System.out.println("通道号:"+nChannel);
                /**
                 已开启的智能规则信息个数
                 */

                int nIntelliInfoNum
                        = info.nIntelliInfoNum;
                System.out.println("已开启的智能规则信息个数:"+nIntelliInfoNum);

                NET_INTELLI_INFO[] stuIntelliInfo
                        = info.stuIntelliInfo;


                for(int j=0;j<nIntelliInfoNum;j++){

                    NET_INTELLI_INFO net_intelli_info
                            = stuIntelliInfo[j];

                    int nTypeNum
                            = net_intelli_info.nTypeNum;
                    System.out.println("智能规则类型个数:"+nTypeNum);

                    /**
                     智能规则类型
                     */
                    Byte64Arr[] szType
                            = net_intelli_info.szType;

                    for(int m=0;m<nTypeNum;m++){
                        Byte64Arr byte64Arr
                                = szType[m];
                        //智能规则类型
                        try {
                            System.out.println("智能规则类型:"+new String(byte64Arr.arr,encode));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                    }
                    /**
                     智能场景类型
                     */

                    byte[] szClass
                            = net_intelli_info.szClass;
                    try {
                        System.out.println("智能场景类型:"+new String(szClass,encode));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }

            }



        }else {
            System.out.println("CLIENT_GetChannelState fail:"+ToolKits.getErrorCode());

        }


    }

    public static void main(String[] args) {
        PollingConfigDemo pollingConfigDemo=new PollingConfigDemo();
        InitTest("172.12.1.51",37777,"admin","admin123");

        Scanner sc=new Scanner(System.in);


        while (true){
            System.out.println("0 退出");
            System.out.println("1 平台下发轮询配置");
            System.out.println("2 按通道获取设备智能业务的运行状态");
            int nextInt
                    = sc.nextInt();

            if(nextInt==0){
                break;
            }else if(nextInt==1){
                pollingConfigDemo.setPollingConfig();
            }else if(nextInt==2){
                pollingConfigDemo.getChannelState();
            }
        }

        LoginOut();

    }
}
