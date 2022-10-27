package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description  流量统计
 * @date 2022/5/7 9:54
 */
public class FluxStatDemo extends Initialization {

    public  static  NetSDKLib.LLong lFindHandle=new NetSDKLib.LLong(0);
        public void  StartFindFluxStat(){
            NET_IN_TRAFFICSTARTFINDSTAT input=new NET_IN_TRAFFICSTARTFINDSTAT();

                NET_TIME stStartTime=new NET_TIME(2022,5,7,0,30,0);

               NET_TIME stEndTime=new NET_TIME(2022,5,7,11,12,0);

                    input.stStartTime=stStartTime;
                    input.stEndTime=stEndTime;
                    input.nWaittime=3000;
            NET_OUT_TRAFFICSTARTFINDSTAT  output=new NET_OUT_TRAFFICSTARTFINDSTAT();

            Pointer pointerInput = new Memory(input.size());
            pointerInput.clear(input.size());
            ToolKits.SetStructDataToPointer(input, pointerInput, 0);

            Pointer pointerOutput = new Memory(output.size());
            pointerOutput.clear(output.size());
            ToolKits.SetStructDataToPointer(output, pointerOutput, 0);


            lFindHandle  = netSdk.CLIENT_StartFindFluxStat(loginHandle, pointerInput, pointerOutput);
            if(lFindHandle.longValue()==0){
                System.out.printf("CLIENT_StartFindFluxStat Failed!LastError = %s\n",
                        ToolKits.getErrorCode());
            }else {
                System.out.println("CLIENT_StartFindFluxStat SUCCESS");
                ToolKits.GetPointerData(pointerOutput,output);
                int dwTotalCount
                        = output.dwTotalCount;
                System.out.println("dwTotalCount: "+dwTotalCount);
            }
        }
    //继续查询流量统计
        public void  DoFindFluxStat(){
            NET_IN_TRAFFICDOFINDSTAT inputTra=new NET_IN_TRAFFICDOFINDSTAT();
          //每次查询的流量统计条数
            inputTra.nCount=3;
            inputTra.nWaittime=3000;

            Pointer pointerInput = new Memory(inputTra.size());
            pointerInput.clear(inputTra.size());
            ToolKits.SetStructDataToPointer(inputTra, pointerInput, 0);


            NET_OUT_TRAFFICDOFINDSTAT outPutTra=new NET_OUT_TRAFFICDOFINDSTAT();

            Pointer pointerOutput = new Memory(outPutTra.size());
            pointerOutput.clear(outPutTra.size());
            ToolKits.SetStructDataToPointer(outPutTra, pointerOutput, 0);

            int isOk
                    = netSdk.CLIENT_DoFindFluxStat(lFindHandle, pointerInput, pointerOutput);
            if(isOk!=1){
                System.out.printf("CLIENT_DoFindFluxStat Failed!LastError = %s\n",
                        ToolKits.getErrorCode());
            }else {
                System.out.println("CLIENT_DoFindFluxStat SUCCESS");
                ToolKits.GetPointerData(pointerOutput,outPutTra);

                DH_TRAFFICFLOWSTAT_OUT stStatInfo
                        = outPutTra.stStatInfo;

                int nStatInfo
                        = stStatInfo.nStatInfo;

                Pointer pStatInfo
                        = stStatInfo.pStatInfo;

                System.out.println("nStatInfo:"+nStatInfo);
                          for(int i=0;i<nStatInfo;i++){
                              DH_TRAFFICFLOWSTAT  dhTrafficflowstats=new DH_TRAFFICFLOWSTAT();
                              ToolKits.GetPointerDataToStruct(pStatInfo,i*dhTrafficflowstats.size(),dhTrafficflowstats);
                              byte[] szMachineName
                                      = dhTrafficflowstats.szMachineName;
                              byte[] szMachineAddress
                                      = dhTrafficflowstats.szMachineAddress;
                              byte[] szDrivingDirection
                                      = dhTrafficflowstats.szDrivingDirection;
                              try {
                                  System.out.println("szMachineName utf-8:"+new String(szMachineName,"utf-8"));
                                  System.out.println("szMachineName gbk:"+new String(szMachineName,"gbk"));

                                  System.out.println("szMachineAddress utf-8:"+new String(szMachineAddress,"utf-8"));
                                  System.out.println("szMachineAddress gbk:"+new String(szMachineAddress,"gbk"));

                                  System.out.println("szDrivingDirection utf-8:"+new String(szDrivingDirection,"utf-8"));
                                  System.out.println("szDrivingDirection gbk:"+new String(szDrivingDirection,"gbk"));

                              } catch (UnsupportedEncodingException e) {
                                  e.printStackTrace();
                              }

                              System.out.println("UTC:"+dhTrafficflowstats.UTC);


                          }
            }

        }
                    public void StopFindFluxStat(){
                        boolean b
                                = netSdk.CLIENT_StopFindFluxStat(lFindHandle);
                        if(b){
                            System.out.println("CLIENT_StopFindFluxStat SUCCESS");
                        }else {
                            System.out.printf("CLIENT_StopFindFluxStat Failed!LastError = %s\n",
                                    ToolKits.getErrorCode());
                        }
                    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "StartFindFluxStat" , "StartFindFluxStat")));
        menu.addItem((new CaseMenu.Item(this , "DoFindFluxStat" , "DoFindFluxStat")));
        menu.addItem((new CaseMenu.Item(this , "StopFindFluxStat" , "StopFindFluxStat")));

        menu.run();
    }

    public static void main(String[] args) {
        FluxStatDemo fluxStatDemo=new FluxStatDemo();
        InitTest("172.24.2.171",37777,"admin","admin123");
        fluxStatDemo.RunTest();
        LoginOut();

    }

}
