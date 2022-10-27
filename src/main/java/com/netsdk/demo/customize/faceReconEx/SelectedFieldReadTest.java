package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;

import java.util.Map;

import static com.netsdk.demo.util.StructFieldChooser.*;

/**
 * @author 47040
 * @since Created in 2020/11/16 16:09
 */
public class SelectedFieldReadTest {

    private static final Map<?, ?> structConfig = GetStructConfig(SelectedFieldReadTest.class, "FaceRecon.json");


    public static void main(String[] args) {

        SelectFieldWithConfig();

    }

    private static void SelectFieldWithConfig() {
        long a = System.currentTimeMillis();
        NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO testStruct0 = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();
        long b = System.currentTimeMillis();
        System.out.println(b - a);

        NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO struct = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();

        struct.szName[1] = 'a';

        struct.UTC = new NetSDKLib.NET_TIME_EX();
        struct.UTC.setTime(2020, 11, 16, 20, 30, 30);

        struct.stuObject = new NetSDKLib.NET_MSG_OBJECT();
        struct.stuObject.stPicInfo = new NetSDKLib.NET_PIC_INFO();
        struct.stuObject.stPicInfo.dwFileLenth = 1000;
        struct.stuObject.stPicInfo.dwOffSet = 2000;

        struct.nCandidateNum = 2;
        struct.stuCandidates[0] = new NetSDKLib.CANDIDATE_INFO();
        struct.stuCandidates[0].stPersonInfo = new NetSDKLib.FACERECOGNITION_PERSON_INFO();
        struct.stuCandidates[0].stPersonInfo.wFacePicNum = 1;
        struct.stuCandidates[0].stPersonInfo.szFacePicInfo[0] = new NetSDKLib.NET_PIC_INFO();
        struct.stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwFileLenth = 100;
        struct.stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwOffSet = 200;
        struct.stuCandidates[0].bySimilarity = 80;
        struct.stuCandidates[1] = new NetSDKLib.CANDIDATE_INFO();
        struct.stuCandidates[1].stPersonInfo = new NetSDKLib.FACERECOGNITION_PERSON_INFO();
        struct.stuCandidates[1].stPersonInfo.wFacePicNum = 1;
        struct.stuCandidates[1].stPersonInfo.szFacePicInfo[0] = new NetSDKLib.NET_PIC_INFO();
        struct.stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwFileLenth = 150;
        struct.stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwOffSet = 250;
        struct.stuCandidates[1].bySimilarity = 85;

        struct.write();

        NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO testStruct = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();

        long startTime = System.currentTimeMillis();

        ReadAllSelectedFields(structConfig, testStruct, struct.getPointer());

        long endTime = System.currentTimeMillis();
        long readAllCost = (endTime - startTime);
        System.out.println("Read All Cost Time:" + readAllCost);

        startTime = System.currentTimeMillis();

        testStruct.szName[1] = (Byte) GetSelectedSingleFieldValue("szName[1]", testStruct, struct.getPointer());
        testStruct.UTC = (NetSDKLib.NET_TIME_EX) GetSelectedSingleFieldValue("UTC", testStruct, struct.getPointer());
        testStruct.stuObject.stPicInfo.dwFileLenth = (Integer) GetSelectedSingleFieldValue("stuObject.stPicInfo.dwFileLenth", testStruct, struct.getPointer());
        testStruct.stuObject.stPicInfo.dwOffSet = (Integer) GetSelectedSingleFieldValue("stuObject.stPicInfo.dwOffSet", testStruct, struct.getPointer());

        testStruct.nCandidateNum = (Integer) GetSelectedSingleFieldValue("nCandidateNum", testStruct, struct.getPointer());
        testStruct.stuCandidates[0].stPersonInfo.wFacePicNum = (Short) GetSelectedSingleFieldValue("stuCandidates[0].stPersonInfo.wFacePicNum", testStruct, struct.getPointer());
        ;
        testStruct.stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwFileLenth = (Integer) GetSelectedSingleFieldValue("stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwFileLenth", testStruct, struct.getPointer());
        testStruct.stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwOffSet = (Integer) GetSelectedSingleFieldValue("stuCandidates[0].stPersonInfo.szFacePicInfo[0].dwOffSet", testStruct, struct.getPointer());
        testStruct.stuCandidates[0].bySimilarity = (Byte) GetSelectedSingleFieldValue("stuCandidates[0].bySimilarity", testStruct, struct.getPointer());
        testStruct.stuCandidates[1].stPersonInfo.wFacePicNum = (Short) GetSelectedSingleFieldValue("stuCandidates[1].stPersonInfo.wFacePicNum", testStruct, struct.getPointer());
        ;
        testStruct.stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwFileLenth = (Integer) GetSelectedSingleFieldValue("stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwFileLenth", testStruct, struct.getPointer());
        testStruct.stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwOffSet = (Integer) GetSelectedSingleFieldValue("stuCandidates[1].stPersonInfo.szFacePicInfo[0].dwOffSet", testStruct, struct.getPointer());
        testStruct.stuCandidates[1].bySimilarity = (Byte) GetSelectedSingleFieldValue("stuCandidates[1].bySimilarity", testStruct, struct.getPointer());

        endTime = System.currentTimeMillis();
        long readSingleCost = (endTime - startTime);
        System.out.println("Read Single Cost Time:" + readSingleCost);

        startTime = System.currentTimeMillis();
        ToolKits.GetPointerDataToStruct(struct.getPointer(), 0, testStruct);
        endTime = System.currentTimeMillis();
        long noHelpCost = (endTime - startTime);
        System.out.println("无优化 Cost Time:" + noHelpCost);
        System.out.println("效率提升: " + (noHelpCost) / Math.min(readAllCost, readSingleCost) + " 倍");
    }
}
