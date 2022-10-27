package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.LastError;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.PlaySDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;


public class AutoPlayPushTestDemo extends Initialization {

    //智能报警句柄
    private NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0);

    private static int index = -1;

    JWindow wnd;


    private final NetSDKLib.NET_TIME m_startTime = new NetSDKLib.NET_TIME(); // 开始时间
    private final NetSDKLib.NET_TIME m_stopTime = new NetSDKLib.NET_TIME();  // 结束时间
    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    // The constant play sdk
    public static final PlaySDKLib playsdk = PlaySDKLib.PLAYSDK_INSTANCE;


    private static final PlayBackWithDataOnly.DownLoadPosCallBack m_PlayBackDownLoadPos = new PlayBackWithDataOnly.DownLoadPosCallBack(); // 回放数据下载进度

    private static final PlayBackWithDataOnly.DataCallBack m_dataCallBack = new PlayBackWithDataOnly.DataCallBack(); // 回放数据回调

    private static final PlayBackWithDataOnly.PlayDecCallBack m_playDecCallBack = new PlayBackWithDataOnly.PlayDecCallBack();  // Play SDK 回放

    private static final ExecutorService dataParseService = Executors.newFixedThreadPool(5);

    private static final int channel = 10;   // NetSDK 对应的 设备通道

    private static final int port = 10;     // PlaySDK 解码端口
    private static final LinkedBlockingDeque<ByteArrayOutputStream> queue = new LinkedBlockingDeque<>();

    public void PushTest(){

            wnd = new JWindow();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenSize.height /= 2;
            screenSize.width /= 2;
            wnd.setSize(screenSize);

            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int w = wnd.getSize().width;
            int h = wnd.getSize().height;
            int x = (dim.width - w) / 2;
            int y = (dim.height - h) / 2;
            wnd.setLocation(x, y);

        Timer timer = new Timer();//创建Timer类对象
        Timer timer2 = new Timer();
        TimerTask playback = new TimerTask() {
            @Override
            public void run() {
                System.out.println("触发回放预览，时间为" + new Date());//输出当前的时间
                StartPlayBack();
            }
        };
        TimerTask stopplayback = new TimerTask() {
            @Override
            public void run() {
                System.out.println("触发停止回放预览，时间为" + new Date());//输出当前的时间
                StopPlayBack();
            }
        };
        TimerTask realplay = new TimerTask() {
            @Override
            public void run() {
                System.out.println("触发实时预览，时间为" + new Date());//输出当前的时间
                Realplay();
            }
        };
        TimerTask stoprealplay = new TimerTask() {
            @Override
            public void run() {
                System.out.println("触发停止实时预览，时间为" + new Date());//输出当前的时间
                StopRealPlay();
            }
        };
        timer.schedule(realplay, 0, 1000*600);
        timer.schedule(stoprealplay, 1000*150, 1000*600);
        timer.schedule(playback, 1000*300, 1000*600);
        timer.schedule(stopplayback, 1000*450, 1000*600);

    }

    public void SetStartTime(){
        Scanner sc = new Scanner(System.in);
        System.out.print("year:");
        String tmp = sc.nextLine();
        int year = Integer.parseInt(tmp);
        System.out.print("month:");
        tmp = sc.nextLine();
        int month = Integer.parseInt(tmp);
        System.out.print("day:");
        tmp = sc.nextLine();
        int day = Integer.parseInt(tmp);
        System.out.print("hour:");
        tmp = sc.nextLine();
        int hour = Integer.parseInt(tmp);
        System.out.print("minute:");
        tmp = sc.nextLine();
        int minute = Integer.parseInt(tmp);
        System.out.print("second:");
        tmp = sc.nextLine();
        int second = Integer.parseInt(tmp);
        m_startTime.setTime(year,month,day,hour,minute,second);
    }

    public void SetStopTime(){
        Scanner sc = new Scanner(System.in);
        System.out.print("year:");
        String tmp = sc.nextLine();
        int year = Integer.parseInt(tmp);
        System.out.print("month:");
        tmp = sc.nextLine();
        int month = Integer.parseInt(tmp);
        System.out.print("day:");
        tmp = sc.nextLine();
        int day = Integer.parseInt(tmp);
        System.out.print("hour:");
        tmp = sc.nextLine();
        int hour = Integer.parseInt(tmp);
        System.out.print("minute:");
        tmp = sc.nextLine();
        int minute = Integer.parseInt(tmp);
        System.out.print("second:");
        tmp = sc.nextLine();
        int second = Integer.parseInt(tmp);
        m_stopTime.setTime(year,month,day,hour,minute,second);
    }

    /**
     * 开启回放
     */
    public void StartPlayBack() {


        // 配置 PlaySDK
        playsdk.PLAY_OpenStream(port, null, 0, 4 * 1024 * 1024);
        playsdk.PLAY_SetDecCallBackEx(port, m_playDecCallBack, null);
        playsdk.PLAY_Play(port, null);

        // 设置回放时的码流类型
        IntByReference steamType = new IntByReference(0);           // 0-主辅码流,1-主码流,2-辅码流
        int emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE;

        boolean bret = netsdk.CLIENT_SetDeviceMode(loginHandle, emType, steamType.getPointer());
        if (!bret) {
            System.err.println("Set Stream Type Failed" + ToolKits.getErrorCode());
        }

        // 设置回放时的录像文件类型
        IntByReference emFileType = new IntByReference(0); // 所有录像 NET_RECORD_TYPE
        emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_TYPE;
        bret = netsdk.CLIENT_SetDeviceMode(loginHandle, emType, emFileType.getPointer());
        if (!bret) {
            System.err.println("Set Record Type Failed " + ToolKits.getErrorCode());
        }

        m_hPlayHandle = netsdk.CLIENT_PlayBackByTimeEx(loginHandle, channel, m_startTime, m_stopTime,
                null, m_PlayBackDownLoadPos, null, m_dataCallBack, null);

        if (m_hPlayHandle.longValue() == 0) {
            int error = netsdk.CLIENT_GetLastError();
            System.err.println("PlayBackByTimeEx Failed " + ToolKits.getErrorCode());
            switch (error) {
                case LastError.NET_NO_RECORD_FOUND:
                    System.out.println("查找不到录像");
                    break;
                default:
                    System.out.println("开启失败");
                    break;
            }
        } else {
            System.out.println("PlayBackByTimeEx Succeed.");
        }

        // 启动新线程 顺序向PlaySDK写入数据 线程在句柄清空后关闭
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (m_hPlayHandle.longValue() != 0) {
                    try {
                        ByteArrayOutputStream stream = queue.take();
                        final byte[] data = stream.toByteArray();
                        playsdk.PLAY_InputData(port, data, data.length);
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 停止回放
     */
    public void StopPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        if (!netsdk.CLIENT_StopPlayBack(m_hPlayHandle)) {
            System.err.println("StopPlayBack Failed");
            return;
        }

        // 关闭PlaySDK解码端口
        playsdk.PLAY_Stop(port);
        playsdk.PLAY_CloseStream(port);

        System.out.println("StopPlayBack Succeed.");

        m_hPlayHandle.setValue(0);
    }

    public void Realplay(){
        if (loginHandle.longValue() == 0) {
            System.err.println("Please login first");
        }
        wnd.setVisible(true);
        int channel = 0; // 预览通道号， 设备有多通道的情况，可手动更改
        int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览

        m_hPlayHandle = netSdk.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(wnd), playType);
        if (m_hPlayHandle.longValue() == 0) {
            int error = netSdk.CLIENT_GetLastError();
            System.err.println("开始实时监视失败，错误码：" + String.format("[0x%x]", error));
        }
        else {
            System.out.println("Success to start realplay");
            netSdk.CLIENT_SetRealDataCallBackEx(m_hPlayHandle, CommonWithCallBack.RealDataCallBack.getInstance(), null, 0);
        }
    }

    public void StopRealPlay(){
        wnd.setVisible(false);
        if(netSdk.CLIENT_StopRealPlayEx(m_hPlayHandle)){
            System.out.println("StopRealPlay success");
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "PushTest" , "PushTest")));
        menu.addItem((new CaseMenu.Item(this , "SetStartTime" , "SetStartTime")));
        menu.addItem((new CaseMenu.Item(this , "SetStopTime" , "SetStopTime")));
//        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));

        menu.run();
    }
    public static void main(String[] args) {
        AutoPlayPushTestDemo AutoPlayPushTestDemo=new AutoPlayPushTestDemo();
        Scanner sc = new Scanner(System.in);
        System.out.print("ip:");
        String ip = sc.nextLine();
        System.out.print("port:");
        String tmp = sc.nextLine();
        int port = Integer.parseInt(tmp);
        System.out.print("username:");
        String username = sc.nextLine();
        System.out.print("password:");
        String pwd = sc.nextLine();
        InitTest(ip,port,username,pwd);
        AutoPlayPushTestDemo.RunTest();
        LoginOut();

    }
}
