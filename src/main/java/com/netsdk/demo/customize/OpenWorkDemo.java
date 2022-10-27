package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_GET_CUSTOMINFO_CAPS;
import com.netsdk.lib.structure.NET_OUT_GET_CUSTOMINFO_CAPS;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.NetSDKLib.RESERVED_TYPE_FOR_COMMON;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220328022 开包台接入安检机sdk配套开发
 * @date 2022/5/11 19:10
 */
public class OpenWorkDemo extends Initialization {


    int channel=0;
    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);

    public void GetCustomInfoCaps(){
        NET_IN_GET_CUSTOMINFO_CAPS  input=new NET_IN_GET_CUSTOMINFO_CAPS();
        Pointer inputPointer = new Memory(input.size());
        inputPointer.clear(input.size());
        ToolKits.SetStructDataToPointer(input,inputPointer,0);

        NET_OUT_GET_CUSTOMINFO_CAPS outPut=new NET_OUT_GET_CUSTOMINFO_CAPS();
        Pointer outputPointer = new Memory(outPut.size());
        outputPointer.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,outputPointer,0);

        boolean isOk
                = netSdk.CLIENT_GetCustomInfoCaps(loginHandle, inputPointer, outputPointer, 3000);
        if (isOk){
            System.out.println("CLIENT_GetCustomInfoCaps Success");
            ToolKits.GetPointerData(outputPointer,outPut);
            int bSupportXRaySubscribeRule = outPut.bSupportXRaySubscribeRule;
            System.out.println("bSupportXRaySubscribeRule :"+bSupportXRaySubscribeRule);
        }else{
            System.out.println("CLIENT_GetCustomInfoCaps Failed!LastError"+ ToolKits.getErrorCode());
        }
    }

    /**
     * 订阅智能任务
     */
    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if(attachHandle.longValue()!=0){
            this.DetachEventRealLoadPic();
        }

        NetSDKLib.RESERVED_PARA  para=new NetSDKLib.RESERVED_PARA();
        para.dwType=RESERVED_TYPE_FOR_COMMON;
        NetSDKLib.NET_RESERVED_COMMON common=new NetSDKLib.NET_RESERVED_COMMON();

        common.bFlagCustomInfo=1;

        String param="PackageTakeRule=1";

        System.arraycopy(param.getBytes(), 0, common.szCustomInfo, 0, param.getBytes().length);

        Pointer  comPointer= new Memory(common.size());

        comPointer.clear(common.size());

        ToolKits.SetStructDataToPointer(common,comPointer,0);

        para.pData=comPointer;

        Pointer pointerPara=new Memory(para.size());

        pointerPara.clear(para.size());

        ToolKits.SetStructDataToPointer(para,pointerPara,0);

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
                AnalyzerDataCB.getInstance(), null, pointerPara);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
            ToolKits.GetPointerData(pointerPara,para);

            Pointer pData = para.pData;

            ToolKits.GetPointerData(pData,common);

            System.out.println("b:"+common.bFlagCustomInfo);

            System.out.println(new String(common.szCustomInfo));

        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
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

        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }


      System.out.println("dwAlarmType:"+dwAlarmType);
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
        menu.addItem((new CaseMenu.Item(this , "GetCustomInfoCaps" , "GetCustomInfoCaps")));
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));

        menu.run();
    }

    public static void main(String[] args) {
        OpenWorkDemo openWorkDemo=new OpenWorkDemo();
        InitTest("10.35.232.160",37777,"admin","admin123");
        openWorkDemo.RunTest();
        LoginOut();

    }

}
