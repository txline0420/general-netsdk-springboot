package com.netsdk.demo.customize;

import com.netsdk.demo.BaseDemo;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.callback.impl.DefaultAnalyseTaskResultCallBack;
import com.netsdk.lib.enumeration.EM_DATA_SOURCE_TYPE;
import com.netsdk.module.AnalyseTaskModule;
import com.netsdk.module.entity.AddAnalyseTaskResult;
import com.netsdk.module.entity.PushAnalysePictureInfo;
import com.sun.jna.Memory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_FACEANALYSIS;

/**
 * @author 47081
 * @version 1.0
 * @description 图片智能分析demo, 1:1,1:n
 * @date 2020/10/21
 */
public class PictureAnalyseDemo extends BaseDemo {
    private final AnalyseTaskModule analyseTaskModule;
    //任务id
    private int taskId;

    public PictureAnalyseDemo() {
        super();
        analyseTaskModule = new AnalyseTaskModule(getNetSdkApi());
    }

    /**
     * 1:1,图片比对
     */
    public void matchPicture() {
        try {
            int result = analyseTaskModule.matchImage(getLoginHandler(), new FileInputStream(new File("D:/shanyu.jpg")), new FileInputStream(new File("D:/test.jpg")), 3000);
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加智能分析任务
     */
    public void addTask() {
        NetSDKLib.NET_PUSH_PICFILE_INFO inParam = new NetSDKLib.NET_PUSH_PICFILE_INFO();
        //立刻启动
        inParam.emStartRule = NetSDKLib.EM_ANALYSE_TASK_START_RULE.EM_ANALYSE_TASK_START_NOW;
        //配置分析规则
        //分析规则条数
        inParam.stuRuleInfo.nRuleCount = 1;
        //大类类型,人脸检测/人脸识别
        inParam.stuRuleInfo.stuRuleInfos[0].emClassType = NetSDKLib.EM_SCENE_CLASS_TYPE.EM_SENCE_CLASS_FACERECOGNITION;
        //规则类型,人脸分析EVENT_IVS_FACEANALYSIS
        inParam.stuRuleInfo.stuRuleInfos[0].dwRuleType = EVENT_IVS_FACEANALYSIS;

        //检测物体类型个数
        inParam.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
        //检测物体类型列表,检测人脸
        inParam.stuRuleInfo.stuRuleInfos[0].emObjectTypes[0] = NetSDKLib.EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMANFACE;
        //规则配置
        NetSDKLib.NET_FACEANALYSIS_RULE_INFO info = new NetSDKLib.NET_FACEANALYSIS_RULE_INFO();

        //检测区顶点数
        info.nDetectRegionPoint = 4;
        info.stuDetectRegion[0].nX = 0;
        info.stuDetectRegion[0].nY = 0;
        info.stuDetectRegion[1].nX = 0;
        info.stuDetectRegion[1].nY = 8192;
        info.stuDetectRegion[2].nX = 8192;
        info.stuDetectRegion[2].nY = 8192;
        info.stuDetectRegion[3].nX = 8192;
        info.stuDetectRegion[3].nY = 0;
        //灵敏度,1-10
        info.nSensitivity = 2;
        //布控组
        info.nLinkGroupNum = 1;
        //布控组启用
        info.stuLinkGroup[0].bEnable = true;
        //人脸库id
        System.arraycopy("1".getBytes(Charset.forName(Utils.getPlatformEncode())), 0, info.stuLinkGroup[0].szGroupID, 0, "1".getBytes(Charset.forName(Utils.getPlatformEncode())).length);
        //陌生人布控模式,未启用
        info.stuStrangerMode.bEnable = false;
        //启用相似度
        info.stuLinkGroup[0].bShowPlate = true;
        info.stuLinkGroup[0].bySimilarity = 20;
        //尺寸过滤
        info.bSizeFileter = false;
        //是否开启人脸属性识别
        info.bFeatureEnable = false;
        info.nFaceFeatureNum = 0;
        info.bFeatureFilter = false;
        info.nMinQuality = 50;
        inParam.stuRuleInfo.stuRuleInfos[0].pReserved = new Memory(info.size());
        ToolKits.SetStructDataToPointer(info, inParam.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
        AddAnalyseTaskResult result = analyseTaskModule.addAnalyseTask(getLoginHandler(), EM_DATA_SOURCE_TYPE.EM_DATA_SOURCE_PUSH_PICFILE, inParam, 3000);
        if (result.isResult()) {
            taskId = result.getTaskId();
        }
    }

    public void attachAnalyseTask() {
        setAttachHandler(analyseTaskModule.attachAnalyseTaskResult(getLoginHandler(), new int[]{taskId}, new int[]{}, true, DefaultAnalyseTaskResultCallBack.getSingleInstance(), 3000, null));
    }

    public void startAnalyseTask() {
        analyseTaskModule.startAnalyseTask(getLoginHandler(), taskId, 3000);
    }

    /**
     * 主动推送图片
     */
    public void pushPicture() {
        List<PushAnalysePictureInfo> infos = new ArrayList<>();
        //推送的图片最好是jg,100k以下
        infos.add(new PushAnalysePictureInfo("D:/liyang(3).jpg", "test"));
        infos.add(new PushAnalysePictureInfo("D:/test.jpg", "test1"));
        infos.add(new PushAnalysePictureInfo("D:/shanyu.jpg", "test2"));
        analyseTaskModule.pushAnalysePicture(getLoginHandler(), taskId, 1, infos, 3000);
    }

    public void dettachAnalyseTask() {
        analyseTaskModule.detachAnalyseTaskResult(getLoginHandler());
    }

    public void removeTask() {
        analyseTaskModule.removeAnalyseTask(getLoginHandler(), taskId, 3000);
    }

    /**
     * demo调用顺序
     * 1.添加人脸识别任务
     * 2.订阅智能分析任务
     * 3.推送图片
     * 等待回调事件上报
     * 4.取消订阅
     * 5.删除任务
     * 6.1:1
     *
     * @param args
     */
    public static void main(String[] args) {
        String ip = "172.12.245.69";
        int port = 37777;
        String username = "admin";
        String password = "admin123";
        PictureAnalyseDemo demo = new PictureAnalyseDemo();
        demo.init();

        if (demo.login(ip, port, username, password)) {
            //登录成功后
            demo.addItem(new CaseMenu.Item(demo, "1:1", "matchPicture"));
            demo.addItem(new CaseMenu.Item(demo, "添加人脸识别任务", "addTask"));
            demo.addItem(new CaseMenu.Item(demo, "订阅智能分析任务", "attachAnalyseTask"));
            //demo.addItem(new CaseMenu.Item(demo, "启动智能分析任务", "startAnalyseTask"));
            demo.addItem(new CaseMenu.Item(demo, "推送图片", "pushPicture"));
            demo.addItem(new CaseMenu.Item(demo, "取消订阅", "dettachAnalyseTask"));
            demo.addItem(new CaseMenu.Item(demo, "移除任务", "removeTask"));
            demo.run();
            demo.logout();
        }
        demo.clean();
    }

}
