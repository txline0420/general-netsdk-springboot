package com.netsdk.demo.customize.composeDemo;

import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.util.Arrays;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.*;

public class ComposeAndPicInPicDemo {

	// 初始化配置 合成通道
    private static CFG_COMPOSE_CHANNEL composeConfig = new CFG_COMPOSE_CHANNEL();
    // 枚举指令 合成通道
    private static final String composeCommand = CFG_CMD_COMPOSE_CHANNEL;

    // 初始化配置 画中画
    private static CFG_PICINPIC_INFO picInPicConfig = new CFG_PICINPIC_INFO();

    // 枚举指令 画中画
    private static final String picInPicCommand = CFG_CMD_PICINPIC;

    /******************************合成通道**************************************/

    public void ComposeChannelDemo(int channel, int command) {

        // 配置的枚举值
        if (GetComposedConfig(channel)) return;
        System.out.println("【设备原有的合并通道配置：】");
        PrintComposeConfig(composeConfig);

        // 发包的格式如下：合并屏幕只支持这几种模式
        switch (command) {
            case 1:
                ComposeChannelOne();
                break;
            case 2:
                ComposeChannelTwo();
                break;
            case 3:
                ComposeChannelThree();
                break;
            case 4:
                ComposeChannelFour();
                break;
            case 5:
                ComposeChannelFive();
                break;
            case 6:
                ComposeChannelSix();
                break;
            case 8:
                ComposeChannelEight();
                break;
            default:
                System.out.println("【不支持这种分割模式】");
        }
        // ComposeChannelFive();

        // 发送新的配置
        boolean ret2 = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, channel, composeCommand, composeConfig);
        if (!ret2) System.out.println("【写入合并通道设置失败】");
        System.out.println("【写入合并通道设置成功，现在的配置是：】");
        GetComposedConfig(channel);
        PrintComposeConfig(composeConfig);
        
        // 最后还要配置 PicInPic 的配置为 null 否则会有bug
        PicInPicNull();
    }

    // 单通道例子
    private void ComposeChannelOne() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_1;              // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4};                   // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 1;                                    // 配置窗口的数量
    }

    // 二通道例子
    private void ComposeChannelTwo() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_2;             // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 8};               // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 2;                                    // 配置窗口的数量
    }

    // 三通道例子
    private void ComposeChannelThree() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_3;              // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 8, 9};             // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 3;                                    // 配置窗口的数量
    }


    // 四通道例子
    private void ComposeChannelFour() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_4;              // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 8, 9, 10};         // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 4;                                    // 配置窗口的数量
    }


    // 五通道例子
    private void ComposeChannelFive() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_5;              // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 8, 9, 10, 11};     // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 5;                                    // 配置窗口的数量
    }

    // 六通道例子
    private void ComposeChannelSix() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_6;     // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 7, 8, 9, 10, 11};   // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 6;                                     // 配置窗口的数量
    }

    // 八通道例子
    private void ComposeChannelEight() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_8;           // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{4, 5, 6, 7, 8, 9, 10, 11};   // 第一个是主窗口
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 8;
    }

    // 获取合成通道配置
    private boolean GetComposedConfig(int channel) {
        // 获取设备当前配置
        boolean ret = ToolKits.GetDevConfig(ComposeLogon.m_hLoginHandle, channel, composeCommand, composeConfig);
        if (!ret) {
            System.err.printf("Request for composeConfig failed, \nErrCode = %x\n", ComposeLogon.netsdk.CLIENT_GetLastError());
            return true;
        }
        return false;
    }

    private void PrintComposeConfig(CFG_COMPOSE_CHANNEL config) {
        System.out.println("分割模式: " + config.emSplitMode);
        System.out.println("合并的窗口通道：" + Arrays.toString(config.nChannelCombination));
        System.out.println("分割窗口数量：" + config.nChannelCount);
    }
    
    private void PicInPicNull(){
    	CFG_SPLIT_INFO pSplits = new CFG_SPLIT_INFO();
        pSplits.pSplitChannels = Pointer.NULL;
        pSplits.emSplitMode = 1;
        pSplits.nReturnChannels = 1;
        pSplits.nMaxChannels = 1;
    	CFG_PICINPIC_INFO msg = new CFG_PICINPIC_INFO();
        msg.nReturnSplit = 1;
        msg.nMaxSplit = 1;
        msg.pSplits = new Memory(pSplits.size());
        ToolKits.SetStructDataToPointer(pSplits, msg.pSplits, 0);
        
      //赋值
        boolean result
                = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, 0, picInPicCommand, msg);
        if (result) {
            System.out.println("PicInPic Config Set Null Success.");
        } else {
            System.err.println("PicInPic Config Set Null failed.");
        }
    }

    /*************************************画中画*********************************************/

 // 合成通道 画中画模式
    private void ComposeChannelPicInPic() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_PIP1;           // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{8, 4};                   // 第一个是主窗口，然后是子画面
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 2;                                             // 配置窗口的数量
    }

    // 合成通道 画中画模式2
    private void ComposeChannelPicInPic2() {
        composeConfig.emSplitMode = CFG_SPLITMODE.SPLITMODE_PIP1;           // 分割模式，必须从 NetSDKLib.CFG_SPLITMODE 中选出
        composeConfig.nChannelCombination = new int[]{8, 4, 5};             // 第一个是主窗口，然后是子画面
        // 配置哪几个通道需要被合并窗口
        composeConfig.nChannelCount = 3;                                     // 配置窗口的数量
    }

    // 一个大画面，2个小画面
    private void PicInPicDemo2(){
        // 先设置为画中画模式
        GetComposedConfig(0);
        ComposeChannelPicInPic2();
        boolean ret = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, 0, composeCommand, composeConfig);
        if (!ret) {
            System.out.println("【写入画中画模式设置失败】");
            return;
        }

        // 写入配置
        CFG_SMALLPIC_INFO pPicInfo1 = new CFG_SMALLPIC_INFO();
        pPicInfo1.nChannelID = 4;
        pPicInfo1.stuPosition.nLeft = 6150;
        pPicInfo1.stuPosition.nTop = 6152;
        pPicInfo1.stuPosition.nRight = 8179;
        pPicInfo1.stuPosition.nBottom = 8184;

        CFG_SMALLPIC_INFO pPicInfo2 = new CFG_SMALLPIC_INFO();
        pPicInfo2.nChannelID = 5;
        pPicInfo2.stuPosition.nLeft = 6150;
        pPicInfo2.stuPosition.nTop = 2152;
        pPicInfo2.stuPosition.nRight = 8179;
        pPicInfo2.stuPosition.nBottom = 4184;

        CFG_SMALLPIC_INFO[] pPicInfos = new CFG_SMALLPIC_INFO[2];
        pPicInfos[0] = pPicInfo1;
        pPicInfos[1] = pPicInfo2;

        CFG_SPLIT_CHANNEL_INFO pSplitChannels = new CFG_SPLIT_CHANNEL_INFO();
        pSplitChannels.bEnable = 1;
        pSplitChannels.nChannelID = 8;
        pSplitChannels.nReturnSmallChannels = 2;     // 注意这里要修改，对应多少个子画面
        pSplitChannels.nMaxSmallChannels = 2;        // 注意这里要修改
        pSplitChannels.pPicInfo = new Memory(pPicInfos[0].size()*pPicInfos.length);
        ToolKits.SetStructArrToPointerData(pPicInfos, pSplitChannels.pPicInfo);

        CFG_SPLIT_INFO pSplits = new CFG_SPLIT_INFO();
        pSplits.pSplitChannels = new Memory(pSplitChannels.size());
        pSplits.emSplitMode = 1;
        pSplits.nReturnChannels = 1;
        pSplits.nMaxChannels = 1;
        ToolKits.SetStructDataToPointer(pSplitChannels, pSplits.pSplitChannels, 0);

        CFG_PICINPIC_INFO msg = new CFG_PICINPIC_INFO();
        msg.nReturnSplit = 1;
        msg.nMaxSplit = 1;
        msg.pSplits = new Memory(pSplits.size());
        ToolKits.SetStructDataToPointer(pSplits, msg.pSplits, 0);

        //赋值
        boolean result
                = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, 0, picInPicCommand, msg);
        if (result) {
            System.out.println("CLIENT_SetConfig success");
        } else {
            System.err.println("CLIENT_SetConfig failed");
        }

    }


    // 画中画模式 demo，1个大画面1个小画面
    private void PicInPicDemo() {

        // 先设置为画中画模式
        GetComposedConfig(0);
        ComposeChannelPicInPic();
        boolean ret = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, 0, composeCommand, composeConfig);
        if (!ret) {
            System.out.println("【写入画中画模式设置失败】");
            return;
        }

        // 获取配置
        // GetPicInPicConfig(0);

        // 写入配置
        CFG_SMALLPIC_INFO pPicInfo1 = new CFG_SMALLPIC_INFO();
        pPicInfo1.nChannelID = 4;
        pPicInfo1.stuPosition.nLeft = 6150;
        pPicInfo1.stuPosition.nTop = 6152;
        pPicInfo1.stuPosition.nRight = 8179;
        pPicInfo1.stuPosition.nBottom = 8184;
        CFG_SMALLPIC_INFO[] pPicInfos = new CFG_SMALLPIC_INFO[1];
        pPicInfos[0] = pPicInfo1;

        CFG_SPLIT_CHANNEL_INFO pSplitChannels = new CFG_SPLIT_CHANNEL_INFO();
        pSplitChannels.bEnable = 1;
        pSplitChannels.nChannelID = 8;
        pSplitChannels.nReturnSmallChannels = 1;
        pSplitChannels.nMaxSmallChannels = 1;
        pSplitChannels.pPicInfo = new Memory(pPicInfos[0].size()*pPicInfos.length);
        ToolKits.SetStructArrToPointerData(pPicInfos, pSplitChannels.pPicInfo);

        CFG_SPLIT_INFO pSplits = new CFG_SPLIT_INFO();
        pSplits.pSplitChannels = new Memory(pSplitChannels.size());
        pSplits.emSplitMode = 1;
        pSplits.nReturnChannels = 1;
        pSplits.nMaxChannels = 1;
        ToolKits.SetStructDataToPointer(pSplitChannels, pSplits.pSplitChannels, 0);

        CFG_PICINPIC_INFO msg = new CFG_PICINPIC_INFO();
        msg.nReturnSplit = 1;
        msg.nMaxSplit = 1;
        msg.pSplits = new Memory(pSplits.size());
        ToolKits.SetStructDataToPointer(pSplits, msg.pSplits, 0);

        //赋值
        boolean result
                = ToolKits.SetDevConfig(ComposeLogon.m_hLoginHandle, 0, picInPicCommand, msg);
        if (result) {
            System.out.println("CLIENT_SetConfig success");
        } else {
            System.err.println("CLIENT_SetConfig field");
        }
    }

    private boolean GetPicInPicConfig(int channel) {
        // 获取设备当前配置
        boolean ret = ToolKits.GetDevConfig(ComposeLogon.m_hLoginHandle, channel, picInPicCommand, picInPicConfig);
        if (!ret) {
            System.err.printf("Request for composeConfig failed, \nErrCode = %x\n", ComposeLogon.netsdk.CLIENT_GetLastError());
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        // 登陆初始化
        ComposeLogon.init(ComposeLogon.DisConnectCallBack.getInstance(),
                ComposeLogon.HaveReConnectCallBack.getInstance());

        // 设备登陆，如果不支持高安全，请使用普通的TCP登陆函数 login(), 句柄 m_hLoginHandle 也要相应的换掉
        ComposeLogon.LoginWithHighLevel();

        ComposeAndPicInPicDemo demo = new ComposeAndPicInPicDemo();

        int channel = 0;   // 合成视频的通道号，一般都是 0

        //********************简易控制台菜单********************************
        Scanner sc = new Scanner(System.in);
        System.out.println("00:退出,\n" +
                "11 n: 发送配置(n为例子选取，可以取1，2，3，4，5，6，8)\n" +
                "12 : 画中画模式demo(1个大画面+1个小画面)\n" +
                "13 : 画中画模式demo(1个大画面+2个小画面)\n" +
                "21 : 获取合成通道配置\n" +
                "22 : 获取画中画配置");

        command:
        while (true) {
            String input = sc.next();

            if ("00".equals(input)) {
                break command;
            } else if ("11".equals(input)) {
                demo.ComposeChannelDemo(0, sc.nextInt());
            } else if ("12".equals(input)) {
                demo.PicInPicDemo();
            } else if ("13".equals(input)) {
                demo.PicInPicDemo2();
            } else if ("21".equals(input)) {
                demo.GetComposedConfig(0);
                demo.PrintComposeConfig(composeConfig);
            } else if ("22".equals(input)) {// 分割模式下获取该配置会失败，需在画中画模式中使用
                demo.GetPicInPicConfig(0);
            } else {
                System.out.println("No such command");
            }
        }

        // 退出登陆
        ComposeLogon.logOut();
        // 清理资源并退出程序
        ComposeLogon.cleanAndExit();
    }
}
