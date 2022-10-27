package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_RECORD_BACKUP_FIND_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;

import static com.netsdk.lib.NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_EXAM;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220427035山西理化生考场项目中，共三个需求
 *     & GIP220601015  中信银行理财数据迁移项目EVS去掉给NVR校时功能
 * @date 2022/5/16 20:07
 */
public class FindRecordBackupRestoreTaskInfosDemo extends Initialization {

// 跨平台编码
private static final Charset sdkEncode = Charset.forName(Utils.getPlatformEncode());
    // 任务ID
    private final int[] taskID = new int[1024];

    /**
     * 根据查询条件返回录像备份任务的信息表
     */
    public void findTaskInfos() {
        // 入参
        NET_IN_FIND_REC_BAK_RST_TASK pInParam = new NET_IN_FIND_REC_BAK_RST_TASK();
        // 根据条件查询备份任务的查询方式
        pInParam.emFindType = EM_RECORD_BACKUP_FIND_TYPE.EM_RECORD_BACKUP_FIND_TYPE_BY_TASKID.getValue();
        if(pInParam.emFindType == 1) {

            // 按照任务号查询
            pInParam.dwTaskID =1897909;
        }else if(pInParam.emFindType == 2) {
            // 按照通道和录制时间段查询
            // 通道号
            pInParam.nLocalChannelID = 3;
            //录制时间段
            pInParam.stuStartTime.setTime(2020, 10, 1, 0, 0, 0);
            pInParam.stuEndTime.setTime(2023, 10, 1, 23, 0, 0);
        }else if(pInParam.emFindType==3){
            //按照主动注册的设备ID查
            byte[] szDeviceID = pInParam.szDeviceID;
            String deviceID="evs52172121184";
            ToolKits.StringToByteArr(deviceID,szDeviceID);

        }
        pInParam.write();

        // 出参
        NET_OUT_FIND_REC_BAK_RST_TASK pOutParam = new NET_OUT_FIND_REC_BAK_RST_TASK();
        pOutParam.nMaxCount = 100;
        NetSDKLib.NET_REC_BAK_RST_TASK[] Tasks = new NetSDKLib.NET_REC_BAK_RST_TASK[pOutParam.nMaxCount];
        for (int i = 0; i < pOutParam.nMaxCount; i++) {
            Tasks[i] = new NetSDKLib.NET_REC_BAK_RST_TASK();
        }
        pOutParam.pTasks = new Memory(Tasks[0].size() * pOutParam.nMaxCount);
        pOutParam.pTasks.clear(Tasks[0].size() * pOutParam.nMaxCount);
        ToolKits.SetStructArrToPointerData(Tasks, pOutParam.pTasks);


        pOutParam.write();
        boolean ret = netSdk.CLIENT_FindRecordBackupRestoreTaskInfos(loginHandle, pInParam.getPointer(), pOutParam.getPointer(), 3000);

        if (!ret) {
            System.err.printf("findTaskInfosByTime false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }else {
            System.out.println("findTaskInfosByTime success");
        }
        pOutParam.read();
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks,Tasks);

        int nRetCount = Math.min(pOutParam.nMaxCount, pOutParam.nRetCount);
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks, Tasks);
        if (nRetCount == 0) {
            System.out.println("没有发现任务；请添加");
            return;
        }
        for (int i = 0; i < nRetCount; i++) {
            String szDevice = new String(Tasks[i].szDeviceID, sdkEncode).trim();
            taskID[i] = Tasks[i].nTaskID;
            System.out.println("任务ID: " + taskID[i] + " 设备ID: " + szDevice
                    + " 通道号: " + Tasks[i].nChannelID + " 录像开始时间: " + Tasks[i].stuStartTime.toStringTime()
                    + " 录像结束时间: " + Tasks[i].stuEndTime.toStringTime()
                    +" 任务开始时间, nState为1、2、3的情况下该时间点有效： "+Tasks[i].stuTaskStartTime.toStringTime()
                    +" 任务结束时间, nState为2、3的情况下该时间点有效： "+Tasks[i].stuTaskEndTime.toStringTime()
                     +"备份源通道:"+Tasks[i].nRemoteChannel);

            System.out.println("当前备份状态(0 等待 1 进行中 2 完成 3 失败):" + Tasks[i].nState);
            System.out.println(" 失败的原因, 当nState字段为3的情况下有效,参考EM_RECORD_BACKUP_FAIL_REASON:"+Tasks[i].emFailReason);
        }
    }
   // 按查询条件查询文件
    public  void  FindFileEx(){

        //对应 NET_MEDIAFILE_EXAM_PARAM 和 NET_MEDIAFILE_EXAM_INFO
        NET_MEDIAFILE_EXAM_PARAM netMediafileExamParam=new NET_MEDIAFILE_EXAM_PARAM();

        String name="张三";
        ToolKits.StringToByteArr(name, netMediafileExamParam.szName);

        Pointer  pointer = new Memory(netMediafileExamParam.size());
        pointer.clear(netMediafileExamParam.size());
        ToolKits.SetStructDataToPointer(netMediafileExamParam,pointer,0);

        NetSDKLib.LLong lLong
                = netSdk.CLIENT_FindFileEx(loginHandle, NET_FILE_QUERY_EXAM, pointer, null, 3000);
        if(lLong.longValue()!=0){
            System.out.println("CLIENT_FindFileEx  success lLong:"+lLong);
        }else {
            System.err.printf("CLIENT_FindFileEx false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        }
    }


        public void SetExamRecordingPlans(){
            NET_IN_SET_EXAM_RECORDING_PLANS input=new NET_IN_SET_EXAM_RECORDING_PLANS();

            input.nPlansNum=1;


            NET_EXAM_RECORDING_PLAN_INFO[]  info=new NET_EXAM_RECORDING_PLAN_INFO[2];
                for(int i=0;i<info.length;i++){
                    info[i]=new NET_EXAM_RECORDING_PLAN_INFO();

                    String name="张三";
                    ToolKits.StringToByteArr(name, info[i].szName);
                    String szNumber="878789";
                    ToolKits.StringToByteArr(szNumber, info[i].szNumber);

                    info[i].nCameraIPNum=2;

                    String ip1="172.12.10.37";
                    String ip2="10.33.12.205";

                    System.arraycopy(ip1.getBytes(), 0,  info[i].szCameraIP, 0, ip1.getBytes().length);

                    System.arraycopy(ip2.getBytes(), 0,  info[i].szCameraIP, 32, ip2.getBytes().length);
                    info[i].stuStartTime.setTime(2022, 5, 21, 9, 0, 0);
                    info[i].stuEndTime.setTime(2022, 5, 21, 16, 30, 0);

                }


            Pointer pointerInfo = new Memory(new NET_EXAM_RECORDING_PLAN_INFO().size() * info.length);

                pointerInfo.clear(new NET_EXAM_RECORDING_PLAN_INFO().size() * info.length);
                    ToolKits.SetStructArrToPointerData(info,pointerInfo);

                    input.pstuPlans=pointerInfo;


            Pointer pointerInput = new Memory(input.size());
            pointerInput.clear(input.size());
            ToolKits.SetStructDataToPointer(input,pointerInput,0);



            NET_OUT_SET_EXAM_RECORDING_PLANS outPut=new NET_OUT_SET_EXAM_RECORDING_PLANS();

            Pointer pointerOut = new Memory(outPut.size());
            pointerOut.clear(outPut.size());
            ToolKits.SetStructDataToPointer(outPut,pointerOut,0);


            boolean b = netSdk.CLIENT_SetExamRecordingPlans(loginHandle, pointerInput, pointerOut, 3000);
            if (!b) {
                System.err.printf("CLIENT_SetExamRecordingPlans false Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
                return;
            }else {
                System.out.println("CLIENT_SetExamRecordingPlans success");
            }

        }
    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "findTaskInfos" , "findTaskInfos")));
        menu.addItem((new CaseMenu.Item(this , "FindFileEx" , "FindFileEx")));
        menu.addItem((new CaseMenu.Item(this , "SetExamRecordingPlans" , "SetExamRecordingPlans")));

        menu.run();
    }

    public static void main(String[] args) {
        FindRecordBackupRestoreTaskInfosDemo findRecordBackupRestoreTaskInfosDemo=new FindRecordBackupRestoreTaskInfosDemo();
        InitTest("172.12.5.248",37777,"admin","admin123");
        findRecordBackupRestoreTaskInfosDemo.RunTest();
        LoginOut();

    }
}
