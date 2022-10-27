package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_DATA_SOURCE_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_DIALRECOGNITION;
import static com.netsdk.lib.NetSDKLib.EVENT_IVS_ELECTRICFAULT_DETECT;


/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2022/6/28 15:20
 */
public class AnalyseTaskDemo extends Initialization {


    /**
     * 添加智能分析任务测试
     */
        int myTaskID=0;
    public void AddAnalyseTaskLEFTDETECTION() {

        /**
         主动推送图片文件，添加任务时无规则和图片信息，通过推送图片接口，每张图片中带有不同的规则信息（目前能源场景中使用）, 对应 NET_PUSH_PICFILE_BYRULE_INFO
         */
        int emDataSourceType = EM_DATA_SOURCE_TYPE.EM_DATA_SOURCE_PUSH_PICFILE_BYRULE.getType();

        // 入参结构体
        NET_PUSH_PICFILE_BYRULE_INFO msg = new NET_PUSH_PICFILE_BYRULE_INFO();
        msg.write();

        NetSDKLib.NET_OUT_ADD_ANALYSE_TASK pOutParam = new NetSDKLib.NET_OUT_ADD_ANALYSE_TASK();

        if (netSdk.CLIENT_AddAnalyseTask(loginHandle, emDataSourceType, msg.getPointer(), pOutParam, 5000)) {
            myTaskID = pOutParam.nTaskID;
            System.out.println("AddAnalyseTask Succeed! " + "任务ID:" + myTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
        } else {
            System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }

    /**
     * 查询智能分析任务
     */
    public void FindAnalyseTask() {
        // 入参
        NetSDKLib.NET_IN_FIND_ANALYSE_TASK stuInParam = new NetSDKLib.NET_IN_FIND_ANALYSE_TASK();
        // 出参
        NetSDKLib.NET_OUT_FIND_ANALYSE_TASK stuOutParam = new NetSDKLib.NET_OUT_FIND_ANALYSE_TASK();

        if (netSdk.CLIENT_FindAnalyseTask(loginHandle, stuInParam, stuOutParam, 5000)) {
            System.out.println("FindAnalyseTask Succeed!" + "智能分析任务个数" + stuOutParam.nTaskNum);
            // ID和状态 可以从 stuTaskInfos 中获取
            for (int i = 0; i < stuOutParam.nTaskNum; i++) {   // 状态值参考 EM_ANALYSE_STATE

                myTaskID=stuOutParam.stuTaskInfos[i].nTaskID;
                System.out.printf("任务%d: %d, 状态：%d\n", (i + 1), stuOutParam.stuTaskInfos[i].nTaskID, stuOutParam.stuTaskInfos[i].emAnalyseState);
            }
        } else {
            System.err.printf("FindAnalyseTask Failed!Last Error:%s\n", ToolKits.getErrorCode());
        }
    }


    /**
     * 删除智能分析任务
     */
    public void RemoveAnalyseTask() {
        // 入参
        NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK pInParam = new NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK();
        pInParam.nTaskID = this.myTaskID;  // demo里删除的是刚刚创建的任务，正常应该是查询后获取任务列表再选择需要删除的ID

        // 出参
        NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK pOutParam = new NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK();

        if (netSdk.CLIENT_RemoveAnalyseTask(loginHandle, pInParam, pOutParam, 5000)) {
            System.out.println("RemoveAnalyseTask Succeed! ");
        } else {
            System.err.printf("RemoveAnalyseTask Failed!Last Error: %s\n", ToolKits.getErrorCode());
        }
        // 删除后再查询一下看看是否删除
    }

    /**
     * 推送智能分析图片文件和规则信息
     */
    public void  PushAnalysePictureFileByRule(){

        //4、推送智能分析图片文件和规则信息 CLIENT_PushAnalysePictureFileByRule
        // EVENT_IVS_DIALRECOGNITION 仪表检测 NET_IVS_DIALRECOGNITION_RULE_INFO
        // EVENT_IVS_ELECTRICFAULT_DETECT
        // 仪表类缺陷检测 NET_IVS_ELECTRICFAULT_DETECT_RULE_INFO
        NET_IN_PUSH_ANALYSE_PICTURE_FILE_BYRULE input=new NET_IN_PUSH_ANALYSE_PICTURE_FILE_BYRULE();

        input.nTaskID=myTaskID;
            System.out.println("myTaskID:"+myTaskID);
        input.nPicNum=1;


        String path="D:\\480_2020-10-10_09-34-03_near_with_correct.jpg";

        byte[] bytes = ToolKits.readPictureToByteArray(path);


        System.out.println("bytes:"+bytes.length);


        input.nBinBufLen=bytes.length;

        input.pBinBuf=new Memory(bytes.length);

        input.pBinBuf.clear(bytes.length);

        input.pBinBuf.write(0,bytes  , 0, bytes.length);

        NET_PUSH_PICTURE_BYRULE_INFO[]  info=new NET_PUSH_PICTURE_BYRULE_INFO[input.nPicNum];
        for(int i=0;i<info.length;i++){
            info[i]=new NET_PUSH_PICTURE_BYRULE_INFO();
        }

        ToolKits.StringToByteArray("file-12598",info[0].szFileID);

        info[0].nOffset=0;
        info[0].nLength=bytes.length;

        NetSDKLib.NET_ANALYSE_RULE stuRule = new  NetSDKLib.NET_ANALYSE_RULE();

        stuRule.nRuleCount=1;

        NetSDKLib.NET_ANALYSE_RULE_INFO stuRuleInfo
                = stuRule.stuRuleInfos[0];

       // EVENT_IVS_DIALRECOGNITION(仪表检测事件)对应结构体NET_IVS_DIALRECOGNITION_RULE_INFO
        // EVENT_IVS_ELECTRICFAULT_DETECT(仪表类缺陷检测事件)对应结构体NET_IVS_ELECTRICFAULT_DETECT_RULE_INFO


     //   int type=EVENT_IVS_ELECTRICFAULT_DETECT;


       int    type = EVENT_IVS_DIALRECOGNITION;



        stuRuleInfo.dwRuleType=type;
        if(type==EVENT_IVS_ELECTRICFAULT_DETECT){
            NET_IVS_ELECTRICFAULT_DETECT_RULE_INFO  msg=new NET_IVS_ELECTRICFAULT_DETECT_RULE_INFO();
/**
 仪表类型 {@link com.netsdk.lib.enumeration.EM_DIALDETECT_TYPE}
 */
            msg. nDetectRegionNum=3;

            msg.stuDetectRegion[0].nX=0;

            msg.stuDetectRegion[0].nY = 0;

            msg.stuDetectRegion[1].nX = 8192;
            msg.stuDetectRegion[1].nY = 0;
            msg.stuDetectRegion[2].nX = 8192;
            msg.stuDetectRegion[2].nY = 8192;
            msg.stuDetectRegion[3].nX = 0;
            msg.stuDetectRegion[3].nY = 8192;


            msg.bSizeFileter=1;

            msg.stuSizeFileter.bFilterMinSizeEnable = 1;
            msg.stuSizeFileter.stuFilterMinSize.nWide = 0;
            msg.stuSizeFileter.stuFilterMinSize.nHeight = 0;

            msg.stuSizeFileter.bFilterMaxSizeEnable = 1;
            msg.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
            msg.stuSizeFileter.stuFilterMaxSize.nHeight = 8191;

            msg.stuSizeFileter.bFilterTypeEnable = 1;
            msg.stuSizeFileter.bFilterType = 0;


            stuRuleInfo.pReserved=new Memory(msg.size());

            stuRuleInfo.pReserved.clear(msg.size());

            ToolKits.SetStructDataToPointer(msg, stuRuleInfo.pReserved,0);
        }else if(type==EVENT_IVS_DIALRECOGNITION){

            NET_IVS_DIALRECOGNITION_RULE_INFO msg=new NET_IVS_DIALRECOGNITION_RULE_INFO();
/**
 仪表类型 {@link com.netsdk.lib.enumeration.EM_DIALDETECT_TYPE}
 */
            msg.emType =3;

            msg.nDetectRegionNum = 4;

            msg.stuDetectRegion[0].nX = 0;
            msg.stuDetectRegion[0].nY = 0;
            msg.stuDetectRegion[1].nX = 8192;
            msg.stuDetectRegion[1].nY = 0;
            msg.stuDetectRegion[2].nX = 8192;
            msg.stuDetectRegion[2].nY = 8192;
            msg.stuDetectRegion[3].nX = 0;
            msg.stuDetectRegion[3].nY = 8192;

            msg.bSizeFileter = 1;

            msg.stuSizeFileter.bFilterMinSizeEnable = 1;
            msg.stuSizeFileter.stuFilterMinSize.nWide = 0;
            msg.stuSizeFileter.stuFilterMinSize.nHeight = 0;

            msg.stuSizeFileter.bFilterMaxSizeEnable = 1;
            msg.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
            msg.stuSizeFileter.stuFilterMaxSize.nHeight = 8191;

            msg.stuSizeFileter.bFilterTypeEnable = 1;
            msg.stuSizeFileter.bFilterType = 0;

            stuRuleInfo.pReserved=new Memory(msg.size());

            stuRuleInfo.pReserved.clear(msg.size());

            ToolKits.SetStructDataToPointer(msg, stuRuleInfo.pReserved,0);
        }


        info[0].stuRuleInfo=stuRule;

        input.pstuPushPicByRuleInfos=  new Memory(info[0].size()*(info.length));

        input.pstuPushPicByRuleInfos.clear(info[0].size()*(info.length));

        ToolKits.SetStructArrToPointerData(info,input.pstuPushPicByRuleInfos);


        Pointer pointerInput = new Memory(input.size());
        pointerInput.clear(input.size());
        ToolKits.SetStructDataToPointer(input,pointerInput,0);

        NET_OUT_PUSH_ANALYSE_PICTURE_FILE_BYRULE outPut=new NET_OUT_PUSH_ANALYSE_PICTURE_FILE_BYRULE();
        Pointer pointeroutPut = new Memory(outPut.size());
        pointeroutPut.clear(outPut.size());
        ToolKits.SetStructDataToPointer(outPut,pointeroutPut,0);

        boolean b
                = netSdk.CLIENT_PushAnalysePictureFileByRule(loginHandle, pointerInput, pointeroutPut, 5000);

        if(b){
            System.out.println("CLIENT_PushAnalysePictureFileByRule Succeed! ");
        }else {
            System.err.printf("CLIENT_PushAnalysePictureFileByRule Failed!Last Error: %s\n", ToolKits.getErrorCode());

        }

    }


    /**
     * 智能分析结果 句柄
     */
    private NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

    public void AttachAnalyseTaskResult() {
        // 入参
        NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT pInParam = new NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT();
        pInParam.cbAnalyseTaskResult = CbfAnalyseTaskResultCallBack.getInstance();

        pInParam.nTaskIdNum = 1;
        pInParam.nTaskIDs[0] = myTaskID;



        AttachHandle = netSdk.CLIENT_AttachAnalyseTaskResult(loginHandle, pInParam, 5000);
        if (AttachHandle.longValue() != 0) {
            System.out.println("AttachAnalyseTaskResult Succeed!"+AttachHandle.longValue());
        } else {
            System.err.printf("AttachAnalyseTaskResult Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }

    /**
     * 取消订阅智能分析结果
     */


    /**
     * 智能分析退订
     *
     * @return 是否成功退订
     */
    public void detachAnalyseTaskResult() {
        System.out.println("detachAnalyseTaskResult:"+AttachHandle.longValue());

        boolean result = netSdk.CLIENT_DetachAnalyseTaskResult(AttachHandle);
        if (!result) {
            System.out.println("detach analyseTask result failed. error is " + ENUMERROR.getErrorMessage());
        }else {
            System.out.println("detachAnalyseTaskResult Succeed");

        }

    }

    /**
     * 智能分析结果订阅函数原型
     */
    private static class CbfAnalyseTaskResultCallBack implements NetSDKLib.fAnalyseTaskResultCallBack {

        private final File picturePath;

        private CbfAnalyseTaskResultCallBack() {

            picturePath = new File("./AnalyzerPicture/result/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }

        }

        private static class CallBackHolder {
            private static CbfAnalyseTaskResultCallBack instance = new CbfAnalyseTaskResultCallBack();
        }

        public static CbfAnalyseTaskResultCallBack getInstance() {
            return CallBackHolder.instance;
        }

        @Override
        public int invoke(NetSDKLib.LLong lAttachHandle, Pointer pstAnalyseTaskResult, Pointer pBuf, int dwBufSize,
                          Pointer dwUser) {
            NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO task = new NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO();
            ToolKits.GetPointerData(pstAnalyseTaskResult, task);

            System.out.println(System.currentTimeMillis() + "进入回调-----------------------------------");
            for (int i = 0; i < task.nTaskResultNum; i++) {
                for (int j = 0; j < task.stuTaskResultInfos[0].nEventCount; j++) {
                    switch (task.stuTaskResultInfos[i].stuEventInfos[i].emEventType) {
                        case NetSDKLib.EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_DIALRECOGNITION: {
                            System.out.println(" 仪表检测事件");
                            DEV_EVENT_DIALRECOGNITION_INFO
                                    msg = new DEV_EVENT_DIALRECOGNITION_INFO();

                            ToolKits.GetPointerData(task.stuTaskResultInfos[0].stuEventInfos[j].pstEventInfo, msg);


                            int emType
                                    = msg.emType;
                            System.out.println("emType:" + emType);

                            byte[] szTaskID = msg.szTaskID;

                            System.out.println("szTaskID:" + new String(szTaskID));
                            System.out.println("nPresetID:" + msg.nPresetID);

                            System.out.println("nChannelID:" + msg.nChannelID);

                            System.out.println("nRetImageInfoNum:" + msg.nRetImageInfoNum);

                            NET_IMAGE_INFO[] stuImgaeInfo
                                    = msg.stuImgaeInfo;

                            for (int m = 0; m < msg.nRetImageInfoNum; m++) {
                                NET_IMAGE_INFO net_image_info
                                        = stuImgaeInfo[m];
                                /**
                                 图片类型 {@link com.netsdk.lib.enumeration.EM_PIC_TYPE}
                                 */
                                System.out.println("emPicType:" + net_image_info.emPicType);
                                System.out.println("net_image_info.length:"+net_image_info.nLength);
                                System.out.println("net_image_info.szFilePath:"+new String( net_image_info.szFilePath));

                                //图片
                                if (net_image_info != null && net_image_info.nLength > 0) {
                                    String picture = picturePath + "/" + System.currentTimeMillis() + "_stuImgaeInfo.jpg";
                                    ToolKits.savePicture(pBuf, net_image_info.nOffset, net_image_info.nLength, picture);
                                }

                            }

                            System.out.println("szDialResult:" + new String(msg.szDialResult));

                            //原始图片
                            if (msg.nOriginalImageLength > 0) {
                                String picture = picturePath + "/" + System.currentTimeMillis() + "_OriginalImage.jpg";
                                ToolKits.savePicture(pBuf, msg.nOriginalImageOffset, msg.nOriginalImageLength, picture);
                            }

                            /**
                             告警类型：0-该字段无效;1-数值异常;2-定时上报
                             */
                            System.out.println("nAlarmType:" + msg.nAlarmType);


                        /**
                         * EM_ANALYSE_EVENT_DIALRECOGNITION 仪表检测事件 DEV_EVENT_DIALRECOGNITION_INFO
                         * EM_ANALYSE_EVENT_ELECTRICFAULT_DETECT
                         * 仪表类缺陷检测事件 DEV_EVENT_ELECTRICFAULTDETECT_INFO
                         *
                         */
                            break;
                        }
                        case NetSDKLib.EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_ELECTRICFAULT_DETECT: { //

                            System.out.println(" 仪表类缺陷检测事件");

                            DEV_EVENT_ELECTRICFAULTDETECT_INFO msg = new DEV_EVENT_ELECTRICFAULTDETECT_INFO();

                            ToolKits.GetPointerData(task.stuTaskResultInfos[0].stuEventInfos[j].pstEventInfo, msg);
                                /**
                                 智能事件所属大类 {@link com.netsdk.lib.enumeration.EM_CLASS_TYPE}
                                 */
                            System.out.println("emClassType:" + msg.emClassType);


                            System.out.println("szName:"+new String(msg.szName));

                            NetSDKLib.SCENE_IMAGE_INFO stuSceneImageInfo
                                    = msg.stuSceneImageInfo;
                            //大图
                            if (stuSceneImageInfo != null && stuSceneImageInfo.nLength > 0) {
                                String picture = picturePath + "/" + System.currentTimeMillis() + "_stuSceneImageInfo.jpg";
                                ToolKits.savePicture(pBuf, stuSceneImageInfo.nOffSet, stuSceneImageInfo.nLength, picture);
                            }


                            int nDialDetectNum
                                    = msg.nDialDetectNum;

                            System.out.println("表盘检测结果个数: "+nDialDetectNum);

                            NET_DIAL_DETECT[] stuDialDetectInfo
                                    = msg.stuDialDetectInfo;

                            for (int m=0;m<nDialDetectNum;m++){
                                NET_DIAL_DETECT detect
                                        = stuDialDetectInfo[m];

                                /**
                                 表盘状态 {@link com.netsdk.lib.enumeration.EM_DIAL_STATE}
                                 */

                                int emDialState
                                        = detect.emDialState;
                            System.out.println("表盘状态:"+emDialState);

                                NET_RECT stuBoundingBox
                                        = detect.stuBoundingBox;

                                System.out.println("stuBoundingBox: "+stuBoundingBox);
                            }





                            break;
                        }

                    }
                }
            }

            return 0;
        }
    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "添加智能分析任务测试" , "AddAnalyseTaskLEFTDETECTION")));

        menu.addItem((new CaseMenu.Item(this , "查询智能分析任务" , "FindAnalyseTask")));

        menu.addItem((new CaseMenu.Item(this , "删除智能分析任务" , "RemoveAnalyseTask")));

        menu.addItem((new CaseMenu.Item(this , "推送智能分析图片文件和规则信息" , "PushAnalysePictureFileByRule")));

        menu.addItem((new CaseMenu.Item(this , "智能分析结果" , "AttachAnalyseTaskResult")));

        menu.addItem((new CaseMenu.Item(this , "取消智能分析订阅" , "detachAnalyseTaskResult")));

        menu.run();
    }

    public static void main(String[] args) {
        AnalyseTaskDemo analyseTaskDemo=new AnalyseTaskDemo();
        InitTest("171.35.0.46",37777,"admin","admin123");
        analyseTaskDemo.RunTest();
        LoginOut();
    }
}
