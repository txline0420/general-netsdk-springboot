package com.netsdk.demo.customize.surfaceEventDemo;

import com.netsdk.demo.customize.surfaceEventDemo.frame.SurfaceMainFrame;
import com.netsdk.demo.customize.surfaceEventDemo.module.AttachModule;
import com.netsdk.demo.customize.surfaceEventDemo.module.LogonModule;
import com.netsdk.demo.customize.surfaceEventDemo.module.RealPlayModule;
import com.netsdk.demo.customize.surfaceEventDemo.module.SdkUtilModule;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author 47040
 * @since Created in 2021/5/11 9:30
 */
public class SurfaceFrameDemo {

    private final SurfaceMainFrame frame;

    private final LogonModule logonModule = new LogonModule();

    private final RealPlayModule realPlayModule = new RealPlayModule();

    private final AttachModule attachModule = new AttachModule();

    public SurfaceFrameDemo() {

        // sdk 初始化
        SdkUtilModule.init();

        frame = new SurfaceMainFrame(m_strIp, m_nPort, m_strUser, m_strPassword);
        // 主界面退出 事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Window Closing");
                realPlayModule.stopPlay(m_hPlayHandle);  // 停止播放
                logonModule.logout(m_hLoginHandle);      // 设备登出
                SdkUtilModule.cleanup();                 // 清理 SDK 资源
                frame.dispose();
                System.exit(0);
            }
        });

        /////////////// 登录相关 事件 ///////////////
        ///////////////////////////////////////////

        // 登入按钮 事件
        frame.getLoginPanel().getLoginBtn().addActionListener(e -> {
            m_strIp = frame.getLoginPanel().getIpAddress();
            m_nPort = frame.getLoginPanel().getPort();
            m_strUser = frame.getLoginPanel().getUsername();
            m_strPassword = frame.getLoginPanel().getPassword();
            // 登录
            m_hLoginHandle = logonModule.login(m_strIp, m_nPort, m_strUser, m_strPassword, m_stDeviceInfo);
            if (m_hLoginHandle.longValue() == 0) {
                JOptionPane.showMessageDialog(frame, "登录失败，错误码 ：" + String.format("[%s]", ToolKits.getErrorCode()));
                return;
            }

            // 初始化订阅句柄Map
            attachHandelMap.clear();
            for (int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
                attachHandelMap.put(i, new NetSDKLib.LLong(0));
            }

            frame.getLoginPanel().getLoginBtn().setEnabled(false); // 更新登录按钮状态
            frame.getLoginPanel().getLogoutBtn().setEnabled(true); // 更新登出按钮状态
            frame.getControlPanel().setEnabled(true);              // 启用播放控制组件
            frame.getControlPanel().resetChannelComboBox(m_stDeviceInfo.byChanNum); // 更新通道下拉框
        });
        // 登出按钮 事件
        frame.getLoginPanel().getLogoutBtn().addActionListener(e -> {
            realPlayModule.stopPlay(m_hPlayHandle);                 // 停止播放
            frame.getRealPlayPanel().getCanvasPanel().repaint();    // 刷新画布
            frame.getControlPanel().setEnabled(false);              // 禁用播放控制组件
            logonModule.logout(m_hLoginHandle);                     // 登出设备
            if (m_hLoginHandle.longValue() == 0) {          // 登出成功则更新 登录/登出 按钮状态

                // 清空订阅句柄Map
                attachHandelMap.clear();

                frame.getLoginPanel().getLoginBtn().setEnabled(true);
                frame.getLoginPanel().getLogoutBtn().setEnabled(false);
            }
        });

        /////////////// 播放控制 事件 ///////////////
        ///////////////////////////////////////////
        // 通道号切换 切换通道下拉框选项时触发
        frame.getControlPanel().getChannelComboBox().addActionListener(e -> {
            channel = ((JComboBox<?>) e.getSource()).getSelectedIndex();
            System.out.println("selected channel changed , now it is : " + channel);
        });
        // 开始播放 事件
        frame.getControlPanel().getPlayBtn().addActionListener(e -> {
            // 开始预览
            m_hPlayHandle = realPlayModule.realPlay(m_hLoginHandle, channel, playType,
                    Native.getComponentPointer(frame.getRealPlayPanel().getCanvasPanel()));
            if (m_hPlayHandle.longValue() == 0) {
                JOptionPane.showMessageDialog(frame, "播放失败，错误码 ：" + String.format("[%s]", ToolKits.getErrorCode()));
            }
        });
        // 停止播放 事件
        frame.getControlPanel().getStopPlayBtn().addActionListener(e -> {
            realPlayModule.stopPlay(m_hPlayHandle);               // 停止播放
            frame.getRealPlayPanel().getCanvasPanel().repaint();  // 刷新画布
        });

        ////////////// 智能事件订阅 事件//////////////
        ////////////////////////////////////////////
        // 订阅
        frame.getControlPanel().getAttachBtn().addActionListener(e -> {

            for (int channel : attachHandelMap.keySet()) {
                NetSDKLib.LLong m_hAttachHandel = attachHandelMap.get(channel);

                attachModule.DetachEventRealLoadPic(m_hAttachHandel);
                // 需求要求直接订阅全通道
                m_hAttachHandel = attachModule.AttachEventRealLoadPic(m_hLoginHandle, channel, SurfaceAnalyzerDataCallBack.getInstance());
                if (m_hAttachHandel.longValue() == 0) {
                    for (int chl : attachHandelMap.keySet()) {
                        attachModule.DetachEventRealLoadPic(attachHandelMap.get(chl));
                    }
                    JOptionPane.showMessageDialog(frame, "订阅失败，错误码 ：" + String.format("[%s]", ToolKits.getErrorCode()));
                    return;
                }
                attachHandelMap.put(channel, m_hAttachHandel);
            }

            frame.getControlPanel().getAttachBtn().setEnabled(false);
            frame.getControlPanel().getDetachBtn().setEnabled(true);

            taskIsOpen = true;
            new Thread(this::eventListTask).start();
            new Thread(this::eventInfoListTask).start();
            new Thread(this::eventInfoGroupTask).start();

            JOptionPane.showMessageDialog(frame, "全通道订阅成功");
        });
        // 退订
        frame.getControlPanel().getDetachBtn().addActionListener(e -> {

            for (int channel : attachHandelMap.keySet()) {
                attachModule.DetachEventRealLoadPic(attachHandelMap.get(channel));
            }
            frame.getControlPanel().getAttachBtn().setEnabled(true);
            frame.getControlPanel().getDetachBtn().setEnabled(false);

            int count = 100;  // 等待队列数据清空，但最多等待10s
            while (count > 0) {
                try {
                    if (eventList.size() == 0 && eventInfoList.size() == 0 && eventGroupList.size() == 0)
                        break;
                    count--;
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
            taskIsOpen = false;
        });
        // 清空
        frame.getControlPanel().getClearBtn().addActionListener(e -> {
            frame.getGroupListPanel().clearTableContent();
            frame.getEventPicPanel().clearPic();
            frame.getEventGroupPanel().clearAll();
            EventResource.clearAll();
        });
    }

    // 设备信息
    private final NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 登录句柄
    private static NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);

    // 播放句柄
    private static NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0);

    // 订阅句柄组 key-通道 value-句柄
    private static final Map<Integer, NetSDKLib.LLong> attachHandelMap = new ConcurrentHashMap<>();

    // 预览通道号
    int channel = 0;

    // 预览类型
    int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay;   // 实时预览模式

    // 来自回调函数的推送事件队列
    private final LinkedBlockingQueue<SurfaceEventInfo> eventList = EventResource.getSurfaceEventQueue();

    // 用于表格展示的队列
    private final LinkedBlockingQueue<List<SurfaceEventInfo>> eventInfoList = EventResource.getSurfaceEventListQueue();

    // 用于事件组展示的队列
    private final LinkedBlockingQueue<List<SurfaceEventInfo>> eventGroupList = EventResource.getSurfaceEventGroupQueue();

    // 判断Task是否执行的开关
    private Boolean taskIsOpen = false;

    // 获取数据并传递到各个队列中
    public void eventListTask() {
        // 为了控制获取事件的顺序，这里我起了个死循环强制线性处理
        // 这么做只是为了Demo设计简便，异步顺序控制有很多更好的方法
        while (taskIsOpen) {
            try {
                // 稍微延迟一下，避免循环的太快
                Thread.sleep(10);
                // 阻塞获取
                SurfaceEventInfo surfaceEventInfo = eventList.poll(50, TimeUnit.MILLISECONDS);
                if (surfaceEventInfo == null) continue;

                // 放入表格展示的队列
                EventResource.AddEventListEle(surfaceEventInfo);
                // 放入缓存并检查 事件组
                EventResource.AddCacheAndCheckGroupEvents(surfaceEventInfo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 表格和事件图片展示任务
    private void eventInfoListTask() {
        while (taskIsOpen) {
            try {
                // 稍微延迟一下，避免循环的太快
                Thread.sleep(10);
                // 阻塞获取
                List<SurfaceEventInfo> eventInfos = eventInfoList.poll(50, TimeUnit.MILLISECONDS);
                if (eventInfos == null) continue;

                String[][] data = new String[eventInfos.size()][5];
                for (int i = 0; i < eventInfos.size(); i++) {
                    data[i][0] = String.valueOf(eventInfos.get(i).getEventID());
                    data[i][1] = String.valueOf(eventInfos.get(i).getChannel());
                    data[i][2] = eventInfos.get(i).getEventName();
                    data[i][3] = eventInfos.get(i).getUTC();
                    data[i][4] = eventInfos.get(i).getBriefInfo();
                }
                // 更新表格
                frame.getGroupListPanel().updateTableContent(data);
                // 更新图片 来源是最上面的(最新)的事件
                SurfaceEventInfo eventInfo = eventInfos.get(0);
                if (eventInfo.getImagesData() != null) {
                    frame.getEventPicPanel().showPicOnCanvas(eventInfo.getImagesData());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 事件组内容展示任务
    private void eventInfoGroupTask() {
        while (taskIsOpen) {
            try {
                // 稍微延迟一下，避免循环的太快
                Thread.sleep(10);
                // 阻塞获取
                List<SurfaceEventInfo> groupList = eventGroupList.poll(50, TimeUnit.MILLISECONDS);
                if (groupList == null) continue;

                // 先清空
                frame.getEventGroupPanel().clearAll();
                for (int i = 0; i < groupList.size(); i++) {
                    SurfaceEventInfo eventInfo = groupList.get(i);
                    String detailInfo = eventInfo.getDetailInfo();
                    frame.getEventGroupPanel().getGroupPanels().get(i).setDetail(detailInfo);
                    if (eventInfo.getImagesData() != null)
                        frame.getEventGroupPanel().getGroupPanels().get(i).showPicOnCanvas(eventInfo.getImagesData());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    ////////////////// 登陆参数 //////////////////
    /////////////////////////////////////////////
    private String m_strIp = "10.18.128.96";
    private Integer m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin654321";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SurfaceFrameDemo::new);
    }
}
