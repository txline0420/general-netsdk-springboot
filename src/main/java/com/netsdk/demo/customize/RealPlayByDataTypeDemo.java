package com.netsdk.demo.customize;

import com.netsdk.demo.BaseDemo;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.callback.impl.DefaultRealPlayCallback;
import com.netsdk.lib.enumeration.EM_AUDIO_DATA_TYPE;
import com.netsdk.lib.enumeration.EM_REAL_DATA_TYPE;
import com.netsdk.module.PlayModule;

import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.UUID;

/**
 * @author 47081
 * @version 1.0
 * @description 转码流demo
 * @date 2021/3/2
 */
public class RealPlayByDataTypeDemo extends BaseDemo {
  private final PlayModule playModule;
  private long realPlay;

  /**
   * 工程路径下生成一个随机文件名称
   *
   * @param postfix
   * @return
   */
  private String createFileName(String postfix) {
    String absolutePath = this.getClass().getClassLoader().getResource("").getPath();
    String uuid = UUID.randomUUID().toString().substring(0, 4).replace(".", "").replace("-", "");
    SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String date = simpleDate.format(new java.util.Date()).replace(" ", "_").replace(":", "-");
    if (!(absolutePath.endsWith("/") || absolutePath.endsWith("\\"))) {
      absolutePath += "/";
    }
    return absolutePath + uuid + "-" + date + "." + postfix;
  }

  public RealPlayByDataTypeDemo() {
    playModule = new PlayModule();
  }

  public void detach() {
    if (playModule.stopRealPlayByDataType(realPlay)) {
      realPlay = 0;
    }
  }

  public void attachDav() {
    if (realPlay != 0) {
      detach();
    }
    playModule.realPlayByDataType(
        getLoginHandler(),
        0,
        EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_PRIVATE,
        EM_AUDIO_DATA_TYPE.EM_AUDIO_DATA_TYPE_AAC,
        DefaultRealPlayCallback.getInstance(),
        null,
        createFileName("dav"),
        3000);
  }

  public void attachFlv() {
    if (realPlay != 0) {
      detach();
    }
    realPlay =
        playModule.realPlayByDataType(
            getLoginHandler(),
            0,
            EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_FLV_STREAM,
            EM_AUDIO_DATA_TYPE.EM_AUDIO_DATA_TYPE_AAC,
            DefaultRealPlayCallback.getInstance(),
            null,
            createFileName("flv"),
            3000);
    System.out.println(realPlay);
  }

  public static void main(String[] args) {
    String ip = "172.23.12.231";
    int port = 37777;
    String username = "admin";
    String password = "admin1234";
    Scanner scanner = new Scanner(System.in);
    String defaultConfig = "ip:%s,port:%d,username:%s,password:%s,需要修改吗?(y/n)";
    defaultConfig = String.format(defaultConfig, ip, port, username, password);
    System.out.println(defaultConfig);
    String answer = "";
    do {
      answer = scanner.nextLine();
      if ("y".equalsIgnoreCase(answer) || "yes".equalsIgnoreCase(answer)) {
        System.out.println("please input ip");
        ip = scanner.nextLine().trim();
        System.out.println("please input port:");
        port = Integer.parseInt(scanner.nextLine());
        System.out.println("please input username:");
        username = scanner.nextLine().trim();
        System.out.println("please input password:");
        password = scanner.nextLine().trim();
        break;
      } else if ("n".equalsIgnoreCase(answer) || "no".equalsIgnoreCase(answer)) {
        break;
      }
      System.out.println("please input the right word.y/yes/n/no,try again.");
    } while (!(answer.equalsIgnoreCase("y")
        || answer.equalsIgnoreCase("yes")
        || answer.equalsIgnoreCase("no")
        || answer.equalsIgnoreCase("n")));

    RealPlayByDataTypeDemo demo = new RealPlayByDataTypeDemo();
    demo.addItem(new CaseMenu.Item(demo, "转码flv", "attachFlv"));
    demo.addItem(new CaseMenu.Item(demo, "私有流", "attachDav"));
    demo.addItem(new CaseMenu.Item(demo, "停止拉流", "detach"));
    // sdk初始化
    demo.init();
    // 登录设备
    System.out.println(demo.createFileName("flv"));
    if (demo.login(ip, port, username, password)) {
      // 登录成功后
      demo.run();
    }
    // 登出设备
    demo.logout();
    // 测试结束,释放sdk资源
    demo.clean();
  }
}
