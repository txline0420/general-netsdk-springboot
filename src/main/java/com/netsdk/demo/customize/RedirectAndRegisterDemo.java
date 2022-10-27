package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.fRedirectServerCallBackEx;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.callback.impl.DefaultRedirectServerCallBackEx;
import com.netsdk.lib.callback.impl.DefaultServiceCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.customize.MEDIAFILE_FACERECOGNITION_INFO;
import com.netsdk.module.AutoRegisterModule;
import com.netsdk.module.entity.DeviceInfo;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * @author 47081
 * @version 1.0
 * @description 主动注册重定向及主动注册demo
 * @date 2021/3/13
 */
public class RedirectAndRegisterDemo {
  /** 重定向服务句柄 */
  private long serviceHandler;
  /** 主动注册及重定向模块,基于sdk接口二次封装模块 */
  private AutoRegisterModule autoRegisterModule;

  private long loginHandler;
  private long findHandler;

  public RedirectAndRegisterDemo() {
    autoRegisterModule = new AutoRegisterModule();
  }

  /**
   * 开启重定向服务
   *
   * @param ip 监听的ip,一般为本机ip
   * @param port 监听端口号
   * @param callback 回调,请写成单例模式
   * @param userData 自定义数据,一般为null
   */
  public void startService(
      String ip, short port, fRedirectServerCallBackEx callback, Pointer userData) {
    serviceHandler = autoRegisterModule.startRedirectService(ip, port, callback, userData);
  }

  public void startService() {
    // 设置需要重定向到的服务器ip和端口
    DefaultRedirectServerCallBackEx.getInstance()
        .putDefaultRedirectInfo("10.34.3.115", (short) 9000);
    startService("10.34.3.115", (short) 8000, DefaultRedirectServerCallBackEx.getInstance(), null);
  }

  public void startListen() {
    autoRegisterModule.listenServer(
        "10.34.3.115", (short) 9000, 5000, DefaultServiceCallBack.getInstance(), null);
  }
  /** sdk初始化 */
  public void init() {
    // 回调函数请使用单例模式
    autoRegisterModule.init(
        DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
  }

  public void login(String ip, int port, String username, String password) {
    DeviceInfo info = autoRegisterModule.login(ip, port, username, password);
    loginHandler = info.getLoginHandler();
  }

  /** 查询人脸信息 */
  public void find() {
    // 查询人脸信息
    int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE;
    NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM queryCondition =
        new NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM();

    // 图片类型,0:未知类型,1:人脸全景大图,2:人脸小图
    queryCondition.nFileType = 1; // 有时候设备只存了一种图，填1和2没有区别

    // 通道号从0开始,-1表示查询所有通道
    queryCondition.nChannelId = 0;
    // 人脸检测事件类型 查询所有类型，NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE
    queryCondition.nAlarmType =
        NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE.NET_FACERECOGNITION_ALARM_TYPE_ALL;
    // 开始时间
    queryCondition.stStartTime.dwYear = 2021;
    queryCondition.stStartTime.dwMonth = 3;
    queryCondition.stStartTime.dwDay = 17;
    queryCondition.stStartTime.dwHour = 0;
    queryCondition.stStartTime.dwMinute = 0;
    queryCondition.stStartTime.dwSecond = 0;

    // 结束时间
    queryCondition.stEndTime.dwYear = 2021;
    queryCondition.stEndTime.dwMonth = 3;
    queryCondition.stEndTime.dwDay = 17;
    queryCondition.stEndTime.dwHour = 23;
    queryCondition.stEndTime.dwMinute = 59;
    queryCondition.stEndTime.dwSecond = 59;
    /////////////////////////////////////// 设备准备搜索结果 //////////////////////////////////////////
    queryCondition.write();
    NetSDKLib.LLong lFindHandle =
        autoRegisterModule
            .getNetsdkApi()
            .CLIENT_FindFileEx(
                new NetSDKLib.LLong(loginHandler), type, queryCondition.getPointer(), null, 3000);
    if (lFindHandle.longValue() == 0) {
      System.err.println("FindFileEx Failed!" + ENUMERROR.getErrorMessage());
      return;
    } else {
      System.out.println("FindFileEx success.");
    }
    findHandler = lFindHandle.longValue();
    queryCondition.read();
    if (getTotalFileCount() == 0) {
      return;
    }
    // 获取查询数据
    findNextFile();
  }
  /////////////////////////////////////// 查看共有多少数据    //////////////////////////////////
  private int getTotalFileCount() {
    IntByReference pCount = new IntByReference();
    boolean rt =
        autoRegisterModule
            .getNetsdkApi()
            .CLIENT_GetTotalFileCount(new NetSDKLib.LLong(findHandler), pCount, null, 2000);
    if (!rt) {
      System.err.println("获取搜索句柄：" + findHandler + " 的搜索内容量失败。" + ENUMERROR.getErrorMessage());
      return 0;
    }
    System.out.println("搜索句柄：" + findHandler + " 共获取到：" + pCount.getValue() + " 条数据。");
    return pCount.getValue();
  }
  // 循环获取查询数据
  private void findNextFile() {
    // 一次最多获取条数，不一定会有这么多，数值不宜太大
    int nMaxCount = 10;

    MEDIAFILE_FACERECOGNITION_INFO[] stuMediaFaceDetection =
        new MEDIAFILE_FACERECOGNITION_INFO[nMaxCount];
    for (int i = 0; i < stuMediaFaceDetection.length; ++i) {
      stuMediaFaceDetection[i] = new MEDIAFILE_FACERECOGNITION_INFO();
    }

    int MemorySize = stuMediaFaceDetection[0].size() * nMaxCount;
    Pointer pMediaFileInfo = new Memory(MemorySize);
    pMediaFileInfo.clear(MemorySize);

    ToolKits.SetStructArrToPointerData(stuMediaFaceDetection, pMediaFileInfo);

    // 循环查询
    int nCurCount = 0;
    int nFindCount = 0;
    NetSDKLib.LLong lFindHandler = new NetSDKLib.LLong(findHandler);
    while (true) {
      int nRet =
          autoRegisterModule
              .getNetsdkApi()
              .CLIENT_FindNextFileEx(
                  lFindHandler, nMaxCount, pMediaFileInfo, MemorySize, null, 3000);
      System.out.println("获取到记录数 : " + nRet);

      if (nRet < 0) {
        System.err.println("FindNextFileEx failed!" + ENUMERROR.getErrorMessage());
        break;
      } else if (nRet == 0) {
        break;
      }
      // 从指针中把数据复制出来
      ToolKits.GetPointerDataToStructArr(pMediaFileInfo, stuMediaFaceDetection);
      // 展示数据
      for (int i = 0; i < nRet; i++) {
        nFindCount = i + nCurCount * nMaxCount;
        System.out.println("—————————————————————————————————————————————————");
        System.out.println("[" + nFindCount + "]通道号 :" + stuMediaFaceDetection[i].toString());
      }
      if (nRet < nMaxCount) {
        break;
      } else {
        nCurCount++;
      }
    }
    autoRegisterModule.getNetsdkApi().CLIENT_FindCloseEx(new NetSDKLib.LLong(findHandler));
  }
  /**
   * demo使用说明: demo.startService用于启动重定向服务 demo.startListen用于主动注册监听服务
   * demo启动后,设备请求到重定向服务器,回调中设置该设备需要重定向到的服务器ip和端口即主动注册服务的ip和端口 设备主动注册到该监听服务
   *
   * <p>人脸信息查询用于之前定制的查询接口 出参请使用 com.netsdk.lib.structure.customize.MEDIAFILE_FACERECOGNITION_INFO
   *
   * @param args
   */
  public static void main(String[] args) {
    String ip = "172.12.66.45";
    int port = 37777;
    String username = "admin";
    String password = "admin123";
    RedirectAndRegisterDemo demo = new RedirectAndRegisterDemo();
    demo.init();
    demo.startService();
    demo.startListen();
    demo.login(ip, port, username, password);
    CaseMenu menu = new CaseMenu();
    menu.addItem(new CaseMenu.Item(demo, "人脸信息查询", "find"));
    menu.run();
  }
}
