package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import static com.netsdk.lib.NetSDKLib.*;

/**
 * @author 291189
 * @version 1.0
 * @description  ERR220722146 IVS-VQ8000 视频质量诊断Java Demo实现
 * @date 2022/8/3 11:35
 */
public class VideoDiagnosisDemo extends Initialization {

    private static LLong m_lDiagnosisHandle = new LLong(0);

    /**
     * 视频诊断订阅，当诊断完成后，会收到事件，一个计划只收到一个事件
     */
    public void StartVideoDiagnosis() {
        /*
         * 入参
         */
        NET_IN_VIDEODIAGNOSIS stIn = new NET_IN_VIDEODIAGNOSIS();
        stIn.nDiagnosisID = -1;
        stIn.dwWaitTime = 5000;
        stIn.cbVideoDiagnosis = RealVideoDiagnosis.getInstance();

        /*
         * 出参
         */
        NET_OUT_VIDEODIAGNOSIS stOut = new NET_OUT_VIDEODIAGNOSIS();

        if(netSdk.CLIENT_StartVideoDiagnosis(loginHandle, stIn, stOut)) {
            m_lDiagnosisHandle = stOut.lDiagnosisHandle;
            System.out.println("视频诊断订阅成功！");
        } else {
            System.err.println("订阅失败, " + ToolKits.getErrorCode());
        }
    }

    // 取消订阅
    public void StopVideoDiagnosis() {
        if(m_lDiagnosisHandle.longValue() != 0) {
            if(netSdk.CLIENT_StopVideoDiagnosis(m_lDiagnosisHandle)) {
                System.out.println("取消视频诊断订阅！");
                m_lDiagnosisHandle.setValue(0);
            }
        }
    }


    private static class RealVideoDiagnosis implements fRealVideoDiagnosis {
        private RealVideoDiagnosis() {}

        private static class RealVideoDiagnosisHolder {
            private static RealVideoDiagnosis instance = new RealVideoDiagnosis();
        }

        private static RealVideoDiagnosis getInstance() {
            return RealVideoDiagnosisHolder.instance;
        }

        @Override
        public int invoke(LLong lDiagnosisHandle,
                          NET_REAL_DIAGNOSIS_RESULT pDiagnosisInfo, Pointer pBuf,
                          int nBufLen, Pointer dwUser) {

            //
            NET_VIDEODIAGNOSIS_COMMON_INFO commons = new NET_VIDEODIAGNOSIS_COMMON_INFO();

            ToolKits.GetPointerData(pDiagnosisInfo.pstDiagnosisCommonInfo, commons);

            int dwBufSize
                    = pDiagnosisInfo.dwBufSize;

            System.out.println("dwBufSize:"+dwBufSize);

            System.out.println("nTypeCount:"+pDiagnosisInfo.nTypeCount);

            Pointer pDiagnosisResult
                    = pDiagnosisInfo.pDiagnosisResult;

            int count=0;

            for(int i=0;i<pDiagnosisInfo.nTypeCount;i++){

                NET_DIAGNOSIS_RESULT_HEADER header=new NET_DIAGNOSIS_RESULT_HEADER();

                ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,header);


                byte[] szDiagnosisType
                        = header.szDiagnosisType;
                int nDiagnosisTypeLen
                        = header.nDiagnosisTypeLen;

                count+=header.dwSize;

                if(NET_DIAGNOSIS_DITHER.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频抖动检测:");
                    NET_VIDEO_DITHER_DETECTIONRESULT result=new NET_VIDEO_DITHER_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;
                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);

                }else if(NET_DIAGNOSIS_STRIATION.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频条纹检测:");
                    NET_VIDEO_STRIATION_DETECTIONRESULT result=new NET_VIDEO_STRIATION_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;
                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );
                }else if(NET_DIAGNOSIS_LOSS.equals(new String(szDiagnosisType).trim())){

                    System.out.println(" 视频丢失检测:");
                    NET_VIDEO_LOSS_DETECTIONRESULT result=new NET_VIDEO_LOSS_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);


                    count+=result.dwSize;

                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );
                }else if(NET_DIAGNOSIS_COVER.equals(new String(szDiagnosisType).trim())){

                    System.out.println(" 视频遮挡检测:");

                    NET_VIDEO_COVER_DETECTIONRESULT  result=new NET_VIDEO_COVER_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;
                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                }else if(NET_DIAGNOSIS_FROZEN.equals(new String(szDiagnosisType).trim())){

                    System.out.println(" 视频冻结检测:");
                    NET_VIDEO_FROZEN_DETECTIONRESULT  result=new NET_VIDEO_FROZEN_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                }else if(NET_DIAGNOSIS_BRIGHTNESS.equals(new String(szDiagnosisType).trim())){

                    System.out.println(" 视频亮度异常检测:");
                    NET_VIDEO_BRIGHTNESS_DETECTIONRESULT     result=new NET_VIDEO_BRIGHTNESS_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );

                }else if(NET_DIAGNOSIS_CONTRAST.equals( new String(szDiagnosisType).trim())){
                    System.out.println(" 视频对比度异常检测:");
                    NET_VIDEO_CONTRAST_DETECTIONRESULT  result=new NET_VIDEO_CONTRAST_DETECTIONRESULT();


                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );
                }else if(NET_DIAGNOSIS_UNBALANCE.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频偏色检测:");
                    NET_VIDEO_UNBALANCE_DETECTIONRESULT result=new NET_VIDEO_UNBALANCE_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );
                }else if(NET_DIAGNOSIS_NOISE.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频噪声检测:");
                    NET_VIDEO_NOISE_DETECTIONRESULT  result=new NET_VIDEO_NOISE_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );


                }else if(NET_DIAGNOSIS_BLUR.equals(new String(szDiagnosisType).trim()) ){
                    System.out.println(" 视频模糊检测:");
                    NET_VIDEO_BLUR_DETECTIONRESULT result=new NET_VIDEO_BLUR_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                    System.out.println(" szPicUrl:"+new String(result.szPicUrl) );

                }else if(NET_DIAGNOSIS_SCENECHANGE.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频场景变化检测:");

                    NET_VIDEO_SCENECHANGE_DETECTIONRESULT result=new NET_VIDEO_SCENECHANGE_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;
                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);
                }else if(NET_DIAGNOSIS_VIDEO_DELAY.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频延时检测:");
                    NET_VIDEO_DELAY_DETECTIONRESUL result=new NET_VIDEO_DELAY_DETECTIONRESUL();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;
                    System.out.println(" nSignalDelay:"+result.nSignalDelay);
                    System.out.println(" nStreamDelay:"+result.nStreamDelay);
                    System.out.println(" nIFrameDelay:"+result.nIFrameDelay);


                }else if(NET_DIAGNOSIS_PTZ_MOVING.equals( new String(szDiagnosisType).trim() )){
                    System.out.println(" 云台移动检测:");

                    NET_PTZ_MOVING_DETECTIONRESULT result=new NET_PTZ_MOVING_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" emPTZMovingUp:"+result.emPTZMovingUp);
                    System.out.println(" emPTZMovingDown:"+result.emPTZMovingDown);
                    System.out.println(" emPTZMovingLeft:"+result.emPTZMovingLeft);
                    System.out.println(" emPTZMovingRight:"+result.emPTZMovingRight);
                    System.out.println(" emPTZMovingZoomWide:"+result.emPTZMovingZoomWide);
                    System.out.println(" emPTZMovingZoomTele:"+result.emPTZMovingZoomTele);

                }else if(NET_DIAGNOSIS_BLACK_WHITE.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 黑白图像检测:");
                    NET_BLACK_WHITE_DETECTIONRESULT result=new NET_BLACK_WHITE_DETECTIONRESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);

                    System.out.println(" szPicUrl:"+ new String(result.szPicUrl) );


                }else if(NET_DIAGNOSIS_DRAMATIC_CHANGE.equals( new String(szDiagnosisType).trim() )){
                    System.out.println(" 场景剧变检测:");
                    NET_DIAGNOSIS_DRAMATIC_DETECTIONRESULT result=new NET_DIAGNOSIS_DRAMATIC_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);


                }else if(NET_DIAGNOSIS_VIDEO_AVAILABILITY.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 视频完好率监测:");

                    NET_VIDEO_AVAILABILITY_DETECTIONRESULT   result=new NET_VIDEO_AVAILABILITY_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;

                    System.out.println(" nSignalDelay:"+result.nSignalDelay);
                    System.out.println(" nStreamDelay:"+result.nStreamDelay);
                    System.out.println(" nIFrameDelay:"+result.nIFrameDelay);

                } else if(NET_DIAGNOSIS_SNOWFLAKE.equals(new String(szDiagnosisType).trim())){
                    System.out.println(" 雪花屏检测:");
                    NET_VIDEO_SNOWFLAKE_DETECTIONRESULT result=new NET_VIDEO_SNOWFLAKE_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);

                    System.out.println(" szPicUrl:"+new String(result.szPicUrl));
                }else if(NET_DIAGNOSIS_VIDEO_ALGORITHMTYPE.equals( new String(szDiagnosisType).trim() )){
                    System.out.println(" 视频算法类型检测:");
                    NET_VIDEO_ALGORITHMTYPE_DETECTIONRESULT result=new NET_VIDEO_ALGORITHMTYPE_DETECTIONRESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);
                    count+=result.dwSize;
                    System.out.println(" bFaceAlgorithm:"+result.bFaceAlgorithm);
                    System.out.println(" nFaceAlgorithmValue:"+result.nFaceAlgorithmValue);
                    System.out.println(" bHumanBodyAlgorithm:"+result.bHumanBodyAlgorithm);

                    System.out.println(" nHumanBodyAlgorithmValue:"+result.nHumanBodyAlgorithmValue);
                    System.out.println(" bVehicleAlgorithm:"+result.bVehicleAlgorithm);

                    System.out.println(" nVehicleAlgorithmValue:"+result.nVehicleAlgorithmValue);

                    System.out.println(" bPlateNumAlgorithm:"+result.bPlateNumAlgorithm);

                    System.out.println(" nPlateNumAlgorithmValue:"+result.nPlateNumAlgorithmValue);

                }else if(NET_DIAGNOSIS_VIDEO_FILCKERING_DETECTION.equals( new String(szDiagnosisType).trim() )){
                    System.out.println(" 视频闪频检测:");
                    NET_VIDEO_FILCKERING_DETECTION_RESULT result=new NET_VIDEO_FILCKERING_DETECTION_RESULT();

                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);

                } else if(NET_DIAGNOSIS_VIDEO_LOSS_FRAME_DETECTION.equals(new String(szDiagnosisType).trim())){
                    NET_VIDEO_LOSS_FRAME_DETECTION_RESULT result=new NET_VIDEO_LOSS_FRAME_DETECTION_RESULT();
                    ToolKits.GetPointerDataToStruct(pDiagnosisResult,count,result);

                    count+=result.dwSize;

                    System.out.println(" nValue:"+result.nValue);
                    System.out.println(" emState:"+result.emState);
                    System.out.println(" nDuration:"+result.nDuration);

                }else {
                    System.out.println("other szDiagnosisType:"+new String(szDiagnosisType));
                    count+=nDiagnosisTypeLen;
                }

            }


            System.out.println("计划名称:" + ToolKits.GetPointerDataToGBKString(commons.stProject.pArray, commons.stProject.dwArrayLen));
            System.out.println("任务名称:" + ToolKits.GetPointerDataToGBKString(commons.stTask.pArray, commons.stTask.dwArrayLen));
            System.out.println("参数表名称:" + ToolKits.GetPointerDataToGBKString(commons.stProfile.pArray, commons.stProfile.dwArrayLen));
            System.out.println("诊断设备ID:" + ToolKits.GetPointerDataToGBKString(commons.stDeviceID.pArray, commons.stDeviceID.dwArrayLen));
            System.out.println("诊断通道:" + commons.nVideoChannelID);
            System.out.println("诊断开始时间:" + commons.stStartTime.toStringTime());
            System.out.println("诊断结束时间:" + commons.stEndTime.toStringTime());
            System.out.println("诊断码流:" + commons.emVideoStream);  // 参考  NET_STREAM_TYPE
            System.out.println("诊断结果类型:" + commons.emResultType);  // 参考  NET_VIDEODIAGNOSIS_RESULT_TYPE
            System.out.println("诊断结果:" + commons.bCollectivityState); // 诊断结果, 1-true, 0-false
            System.out.println("失败原因:" + commons.emFailedCause);  // 参考 NET_VIDEODIAGNOSIS_FAIL_TYPE
            System.out.println("失败原因描述:" + new String(commons.szFailedCode).trim());
            System.out.println("诊断结果存放地址:" + new String(commons.szResultAddress).trim());

            System.out.println("诊断结果存放地址扩展:" + new String(commons.szResultAddressEx).trim());
            for(int i = 0; i < commons.nBackPic; i++) {
                System.out.println("背景图片路径:" + new String(commons.szBackPicAddressArr[i].szBackPicAddress).trim());
            }
            return 0;
        }

    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "StartVideoDiagnosis" , "StartVideoDiagnosis")));
        menu.addItem((new CaseMenu.Item(this , "StopVideoDiagnosis" , "StopVideoDiagnosis")));
        menu.run();
    }

    public static void main(String[] args) {
        VideoDiagnosisDemo videoDiagnosisDemo=new VideoDiagnosisDemo();

        InitTest("172.25.242.120",37777,"admin","dahuacloud123");
        videoDiagnosisDemo.RunTest();
        LoginOut();

    }
}
