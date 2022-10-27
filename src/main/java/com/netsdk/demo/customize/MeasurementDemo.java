package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_CAR_MEASUREMENT_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.NetSDKLib.RESERVED_TYPE_FOR_PATH;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220606098 常州迈普治超--NETSDK--定制java版本SDK
 * @date 2022/6/7 9:18
 */
public class MeasurementDemo extends Initialization {

    private static String ipAddr = "172.24.1.224";
    private static int port = 37777;
    private static String user = "admin";
    private static String password = "admin123";

    static int channel=0;
    NetSDKLib.LLong   attachHandle=new NetSDKLib.LLong(0);


    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if (attachHandle.longValue() != 0) {
            this.DetachEventRealLoadPic();
        }


        NetSDKLib.RESERVED_PARA  para=new NetSDKLib.RESERVED_PARA();
        para.dwType=RESERVED_TYPE_FOR_PATH;
        NetSDKLib.NET_RESERVED_PATH path=new NetSDKLib.NET_RESERVED_PATH();
        path.nMaxPathNum=2;
        path.emPictruePaths[0]=1;
        path.emPictruePaths[1]=2;
        Pointer  pathPointer= new Memory(path.size());

        pathPointer.clear(path.size());

        ToolKits.SetStructDataToPointer(path,pathPointer,0);

        para.pData=pathPointer;


        Pointer pointerPara=new Memory(para.size());

        pointerPara.clear(para.size());


        ToolKits.SetStructDataToPointer(para,pointerPara,0);


        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_TRAFFIC_CAR_MEASUREMENT , bNeedPicture,
                AnalyzerDataCB.getInstance(), null, pointerPara);
        if (attachHandle.longValue() != 0) {

            ToolKits.GetPointerDataToStruct(pointerPara,0,para);

            Pointer pData = para.pData;
            ToolKits.GetPointerDataToStruct(pData,0,path);

            System.out.println("nMaxPathNum:"+path.nMaxPathNum);
            int[] emPictruePaths
                    = path.emPictruePaths;
            for (int i=0;i<path.nMaxPathNum;i++){
                System.out.println(i+" :"+emPictruePaths[i]);
            }

            System.out.println("Chn"+channel+" CLIENT_RealLoadPictureEx Success");
        } else {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

        return attachHandle;
    }

    /**
     * 报警事件（智能）回调  该回调使用时，其创建对象建议为持久化对象，避免被Gc回收，可使用静态方式
     */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private final File picturePath;
        private static AnalyzerDataCB instance;

        @Override
        public void finalize() throws Throwable {
            super.finalize();
            System.out.println("AnalyzerDataCB 对象已经被回收");
        }

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
                case NetSDKLib.EVENT_IVS_TRAFFIC_CAR_MEASUREMENT: {

                    DEV_EVENT_TRAFFIC_CAR_MEASUREMENT_INFO  msg=new DEV_EVENT_TRAFFIC_CAR_MEASUREMENT_INFO();
                    ToolKits.GetPointerDataToStruct(pAlarmInfo,0,msg);
                    //车牌号码
                    byte[] szPlateNumber
                            = msg.stTrafficCar.szPlateNumber;
                    try {
                        System.out.println("车牌号码 szPlateNumber:"+new String(szPlateNumber,encode));

                        byte bPicEnble
                                = msg.stuObject.bPicEnble;
                        NetSDKLib.NET_PIC_INFO stPicInfo
                                = msg.stuObject.stPicInfo;
                        if (bPicEnble== 1&&stPicInfo.dwFileLenth>0) {

                            String sencePicture = picturePath + "\\" + System.currentTimeMillis() + "license_Plate.jpg";
                            ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, sencePicture);
                        }

                        byte[] szObjectSubType
                                = msg.stuVehicle.szObjectSubType;

                        System.out.println("物体子类别 szObjectSubType:"+new String(szObjectSubType,encode));
                        int nGroupId
                                = msg.stuFileInfo.nGroupId;
                        System.out.println("同一组抓拍文件的唯一标识 nGroupId:"+nGroupId);

                        NetSDKLib.EVENT_COMM_INFO stuCommInfo
                                = msg.stuCommInfo;
                        Pointer pszFTPPath
                                = stuCommInfo.pszFTPPath;
                        String FTPPath
                                = ToolKits.GetPointerDataToGBKString(pszFTPPath);
                        Pointer pszVideoPath
                                = stuCommInfo.pszVideoPath;
                        String videoPath
                                = ToolKits.GetPointerDataToGBKString(pszVideoPath);
                        System.out.println("FTPPath:"+FTPPath);
                        System.out.println("videoPath:"+videoPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    break;
                } default:
                    System.out.println("其他事件："+dwAlarmType);
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
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        MeasurementDemo measurementDemo=new MeasurementDemo();
        measurementDemo.RunTest();
        LoginOut();
    }

}
