package com.netsdk.demo.customize.heatmap;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.callback.impl.DefaultVideoStatHeatMapCallBack;
import com.netsdk.lib.enumeration.EM_HEATMAP_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_GET_HEATMAPS_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_HEATMAPS_INFO;
import com.netsdk.module.BaseModule;
import com.netsdk.module.HeatMapModule;
import com.netsdk.module.entity.BmpInfo;
import com.netsdk.module.entity.DeviceInfo;
import com.netsdk.module.entity.HeatMapGrayData;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author 47081
 * @version 1.0
 * @description 热度图数据合成到背景图上
 * @date 2020/9/25
 */
public class HeatMapDemo {

  private NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
  /** 二次封装的基础模块,包含初始化,登录,登出 */
  private BaseModule baseModule;
  /** 二次封装的热度图模块，包含订阅，获取，退订等 */
  private HeatMapModule heatMapModule;

  private long loginHandler;
  private long attachHeatMapHandler;
  /** 这个static只是为了方便在抓图回调里获取灰度图数据 */
  public static int token;

  private boolean isSetSnapCallBack = false;

  public HeatMapDemo() {
    this.baseModule = new BaseModule(netSdk);
    this.heatMapModule = new HeatMapModule(netSdk);
  }

  /**
   * sdk初始化
   *
   * @return
   */
  public boolean init() {
    return baseModule.init(
        DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
  }

  /**
   * 登录设备
   *
   * @param ip 设备ip
   * @param port 设备端口
   * @param userName 用户名
   * @param password 密码
   * @return
   */
  public boolean login(String ip, int port, String userName, String password) {
    DeviceInfo info = baseModule.login(ip, port, userName, password);
    if (info == null || info.getLoginHandler() == 0) {
      loginHandler = 0;
      return false;
    }
    loginHandler = info.getLoginHandler();
    System.out.println("Login success! LoginHandler is " + loginHandler);
    return true;
  }

  /**
   * sdk清理资源
   *
   * @return
   */
  public void clean() {
    baseModule.clean();
  }

  /** 订阅热度图数据 */
  public boolean attachHeatMap() {
    attachHeatMapHandler =
        heatMapModule.attach(
            loginHandler, 0, DefaultVideoStatHeatMapCallBack.getInstance(), null, 3000);
    System.err.println("订阅：" + ToolKits.getErrorCode() + "  错误msg：" + ENUMERROR.getErrorMessage());
    return attachHeatMapHandler != 0;
  }

  /**
   * 退订热度图数据
   *
   * @return
   */
  public boolean detach() {
    return heatMapModule.detach(attachHeatMapHandler);
  }

  /**
   * 获取热度图数据
   *
   * @return
   */
  public boolean get() {
    token =
        heatMapModule.get(
            attachHeatMapHandler,
            0,
            "2020/9/27/19/0/0",
            "2020/9/27/20/00/00",
            EM_HEATMAP_TYPE.EM_HEATMAP_HUMANSTATISTICS,
            3000);
    return token != 0;
  }

  /** 热度图合成到背景图上 */
  public void mergeBmpWithFile() {
    // 读取bmp文件
    try {
      BufferedImage bmp = ImageIO.read(new File("D:/1.bmp"));
      ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
      // 此方式转换出来的位深度是24位
      ImageIO.write(bmp, "bmp", byteArrOutput);
      ////////////////// 获取背景图的缓存、宽高 /////////////
      byte[] buffer = byteArrOutput.toByteArray(); // bmp格式的Buf
      int width = bmp.getWidth();
      int height = bmp.getHeight();
      int bitCount = bmp.getColorModel().getPixelSize();
      // 54是头
      int nBackPicLen = width * height * 24 / 8 + 54;
      BmpInfo backInfo = new BmpInfo(buffer, width, height, bitCount, 1, nBackPicLen);
      HeatMapGrayData grayData = DefaultVideoStatHeatMapCallBack.grayDatas.get(token);
      BmpInfo grayInfo =
          new BmpInfo(
              grayData.getData(),
              grayData.getWidth(),
              grayData.getHeight(),
              grayData.getnBit(),
              0,
              grayData.getLength());
      heatMapModule.createHeatMap(grayInfo, backInfo, "D:/3.bmp", 0.5f);
      byteArrOutput.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** 远程抓图叠加热度图 */
  public void mergeBmpWithSnap() {
    if (!isSetSnapCallBack) {
      // 设置抓图回调
      netSdk.CLIENT_SetSnapRevCallBack(SnapPicHeatMapCallBack.getInstance(), null);
    }
    // 发送抓图命令给前端设备，抓图的信息
    NetSDKLib.SNAP_PARAMS stuSnapParams = new NetSDKLib.SNAP_PARAMS();
    // 抓图通道
    stuSnapParams.Channel = 0;
    // 表示请求一帧
    stuSnapParams.mode = 0;
    stuSnapParams.Quality = 3;
    stuSnapParams.InterSnap = 5;
    // 请求序列号，有效值范围 0~65535，超过范围会被截断为unsigned short
    stuSnapParams.CmdSerial = 100;

    IntByReference reserved = new IntByReference(0);
    // 热度图叠加到背景图在抓图回调SnapPicHeatMapCallBack.java里完成
    if (!netSdk.CLIENT_SnapPictureEx(new NetSDKLib.LLong(loginHandler), stuSnapParams, reserved)) {
      System.out.println("snap picture failed. the error is " + ENUMERROR.getErrorMessage());
    } else {
      System.out.println("snap picture success.");
    }
  }

  /**
   * byte to int
   *
   * @param b bmp
   * @return
   * @throws IOException
   */
  private int byte2Int(byte[] b) throws IOException {
    int num = (b[3] & 0xff << 24) | (b[2] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[0] & 0xff);
    return num;
  }

  /**
   * 登出设备
   *
   * @return
   */
  public boolean logout() {
    boolean result = baseModule.logout(loginHandler);
    if (!result) {
      System.out.println("logout failed. the error is " + ENUMERROR.getErrorMessage());
    }
    return result;
  }

  public void runTest() {
    CaseMenu menu = new CaseMenu();
    menu.addItem(new CaseMenu.Item(this, "订阅热度图", "attachHeatMap"));
    menu.addItem(new CaseMenu.Item(this, "获取热度图数据", "get"));
    menu.addItem(new CaseMenu.Item(this, "热度图叠加背景图", "mergeBmpWithFile"));
    menu.addItem(new CaseMenu.Item(this, "远程抓图叠加热度图", "mergeBmpWithSnap"));
    menu.addItem(new CaseMenu.Item(this, "退订热度图", "detach"));
    menu.addItem(new CaseMenu.Item(this, "热度图温度", "getHeatMapDirectlyTest"));
    menu.run();
  }

  int channel = 0;

  public void getHeatMapDirectlyTest() {
    // 获取热成像温度的入参
    NET_IN_GET_HEATMAPS_INFO inParam = new NET_IN_GET_HEATMAPS_INFO();
    inParam.nChannel = channel;
    NET_OUT_GET_HEATMAPS_INFO outParam = new NET_OUT_GET_HEATMAPS_INFO();
    /** dwMaxDataBufLen: The allocated memory should be larger. */
    outParam.dwMaxDataBufLen = 1024 * 1024;
    outParam.pbDataBuf = new Memory(outParam.dwMaxDataBufLen);
    outParam.pbDataBuf.clear(outParam.dwMaxDataBufLen);
    Pointer pInParam = new Memory(inParam.dwSize);
    ToolKits.SetStructDataToPointer(inParam, pInParam, 0);
    Pointer pOutParam = new Memory(outParam.size());
    ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
    boolean result =
        netSdk.CLIENT_GetHeatMapsDirectly(
            new NetSDKLib.LLong(loginHandler), pInParam, pOutParam, 3000);
    if (result) {
      System.out.println("GetHeatMapsDirectly success！");
      ToolKits.GetPointerData(pOutParam, outParam);
      outParam.read();
      System.out.println(outParam.dwRetDataBufLen);
      System.out.println(outParam);

    } else {
      System.err.println(
          "GetHeatMapsDirectly false!"
              + ENUMERROR.getErrorMessage()
              + "    SDK error type is:"
              + ENUMERROR.getENUMError());
    }
  }

  public static void main(String[] args) {
    String ipsr = "172.11.2.5";
    // 设备ip 172.32.102.46 、172.32.102.57
    String ip = "172.32.102.88";
    int port = 37777;
    String username = "admin";
    String password = "admin123";
    HeatMapDemo demo = new HeatMapDemo();
    demo.init();
    if (demo.login(ip, port, username, password)) {
      /**
       * 热度图调用顺序： 1.订阅数据, 2.获取数据 3. 设备传上来热度图数据,在回调中获取到数据,进行业务处理 4.业务处理完成，退订数据
       *
       * <p>demo使用顺序: main方法启动,登录设备: 输入1订阅热度图数据 输入2获取热度图数据,查看控制台输出是否有热度图数据
       * 输入3可将热度图数据叠加到.bmp文件上(demo中是D:/1.bmp)，输出一个新的bmp文件 输入4可远程抓一张图片,将热度图数据叠加到抓图上,输出一个新的bmp文件 输入5退订
       * 输入6热度图温度 输入0结束demo
       */
      demo.runTest();
      demo.logout();
    }
    demo.clean();
  }
}
