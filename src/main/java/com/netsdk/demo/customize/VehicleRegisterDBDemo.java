package com.netsdk.demo.customize;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.util.Scanner;


/**
 * @author 291189
 * @version 1.0
 * @description GIP210810006 上海全区治安道口车辆布控需求SDK支撑
 * @date 2021/8/17 10:29
 */
public class VehicleRegisterDBDemo extends Initialization {

    //创建车辆组
    public String CLIENT_CreateGroupForVehicleRegisterDB(){
        //入参
        NET_IN_CREATE_GROUP_FOR_VEHICLE_REG_DB inparam=new NET_IN_CREATE_GROUP_FOR_VEHICLE_REG_DB();

        String groupName="one";
        String GroupDetail="第一个分组";


         System.arraycopy(groupName.getBytes(),0,inparam.szGroupName,0,groupName.getBytes().length);

        System.arraycopy(GroupDetail.getBytes(),0,inparam.szGroupDetail,0,GroupDetail.getBytes().length);
        int pInParamSize=inparam.size();
        Pointer pInParam =new Memory(pInParamSize);
        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

        //出参
        NET_OUT_CREATE_GROUP_FOR_VEHICLE_REG_DB outParam=new NET_OUT_CREATE_GROUP_FOR_VEHICLE_REG_DB();
        int poutParamSize=outParam.size();
        Pointer poutParam =new Memory(poutParamSize);
        ToolKits.SetStructDataToPointer(outParam, poutParam, 0);


        boolean isCreate
                = netSdk.CLIENT_CreateGroupForVehicleRegisterDB(loginHandle, pInParam, poutParam, 3000);
        String id="";
        if(isCreate){

            ToolKits.GetPointerDataToStruct(poutParam,0,outParam);

            byte[] szGroupID = outParam.szGroupID;
            id= new String(szGroupID);
            System.out.println("CLIENT_CreateGroupForVehicleRegisterDB success ");
            System.out.println("szGroupID:"+id);


        }else {
            System.err.println(" CLIENT_CreateGroupForVehicleRegisterDB fail :"+ToolKits.getErrorCode());
        }
        return id;
    }
   // 删除车辆组
    public void CLIENT_DeleteGroupFromVehicleRegisterDB(String id){
        //入参
        NET_IN_DELETE_GROUP_FROM_VEHICLE_REG_DB inparam=new NET_IN_DELETE_GROUP_FROM_VEHICLE_REG_DB();

        String groupID=id;

        System.arraycopy(groupID.getBytes(),0,inparam.szGroupID,0,groupID.getBytes().length);

        int pInParamSize=inparam.size();

        Pointer pInParam =new Memory(pInParamSize);

        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

        //出参
        NET_OUT_DELETE_GROUP_FROM_VEHICLE_REG_DB outParam=new NET_OUT_DELETE_GROUP_FROM_VEHICLE_REG_DB();

        int pOutParamSize=outParam.size();

        Pointer pOutParam =new Memory(pOutParamSize);

        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);

        boolean isDelete = netSdk.CLIENT_DeleteGroupFromVehicleRegisterDB(loginHandle, pInParam, pOutParam, 3000);

        if(isDelete){
            System.out.println(" delete is success");
        }else {
            System.err.println(" delete is fail");
        }
    }

    //向车牌库添加车辆信息
    public void CLIENT_MultiAppendToVehicleRegisterDB(){

        //入参
        NET_IN_MULTI_APPEND_TO_VEHICLE_REG_DB inparam=new NET_IN_MULTI_APPEND_TO_VEHICLE_REG_DB();

        inparam.nVehicleNum=1000; // 车辆个数
       // Memory memory = new Memory(info.size() * inparam.nVehicleNum);

        //设置车辆数组对象。
        NetSDKLib.NET_VEHICLE_INFO[] infos = new  NetSDKLib.NET_VEHICLE_INFO[inparam.nVehicleNum];

            for(int i=0;i<inparam.nVehicleNum;i++){
                NetSDKLib.NET_VEHICLE_INFO info=new NetSDKLib.NET_VEHICLE_INFO();
                info.nUID=11;
                String groupID="6";//车辆所属组ID（注意该groupID需在设备已存在）
                System.arraycopy(groupID.getBytes(),0,info.szGroupID,0,groupID.getBytes().length);
                String   groupName="one";// 车辆所属组名
                System.arraycopy(groupName.getBytes(),0,info.szGroupName,0,groupName.getBytes().length);
                int pla=12345+i;
                String  plateNumber="浙A"+pla;//车牌号码（号码牌不重复）
                System.arraycopy(plateNumber.getBytes(),0,info.szPlateNumber,0,plateNumber.getBytes().length);
                String  plateCountry="CN";
                System.arraycopy(plateCountry.getBytes(),0,info.szPlateCountry,0,plateCountry.getBytes().length);
                info.nPlateType=1; // 车牌类型
                info.nVehicleType=6;  // 车型(轿车、卡车等)
                info.nBrand=10; //车辆车标,需要通过映射表得到真正的车标.同卡口事件的CarLogoIndex
                info.nCarSeries=1005; //车辆子品牌，需要通过映射表得到真正的子品牌,同卡口事件的SubBrand
                info.nCarSeriesModelYearIndex=12; // 车辆品牌年款，需要通过映射表得到真正的年款，同卡口事件的BrandYear 车头年款序号范围1~999；车尾年款序号范围1001~1999；0表示未知；1000预留。

                // 车色 第一个元素表示红色分量值； 第二个元素表示绿色分量值； 第三个元素表示蓝色分量值； 第四个元素表示透明度分量(无意义)
                info.stuVehicleColor.nRed = 128;
                info.stuVehicleColor.nGreen = 128;
                info.stuVehicleColor.nBlue = 128;
                info.stuVehicleColor.nAlpha = 255;

                // 车牌颜色,规则同车色
                info.stuPlateColor.nRed = 128;
                info.stuPlateColor.nGreen = 128;
                info.stuPlateColor.nBlue = 128;
                info.stuPlateColor.nAlpha = 255;

                String  ownerName="TOM";//车主名称
                System.arraycopy(ownerName.getBytes(),0,info.szOwnerName,0,ownerName.getBytes().length);

                info.nSex=1;

                info.nCertificateType = 0;

                long id=123456001+i;

                String personID=id+"";// 人员身份证号码,工号,或其他编号

                System.arraycopy(personID.getBytes(),0,info.szPersonID,0,personID.getBytes().length);

                String ownerCountry="CN";// 车主国籍,2字节,符合ISO3166规范

                System.arraycopy(ownerCountry.getBytes(),0,info.szOwnerCountry,0,ownerCountry.getBytes().length);

                String    province="ZHEJIANG";//省份

                System.arraycopy(province.getBytes(),0,info.szProvince,0,province.getBytes().length);

                String city="HANGZHOU";

                System.arraycopy(city.getBytes(),0,info.szCity,0,city.getBytes().length);

                String  homeAddress="BINANLU1188";

                System.arraycopy(homeAddress.getBytes(),0,info.szHomeAddress,0,homeAddress.getBytes().length);

                long e= 123456782l+i;
                String email= e+"@qq.com";

                System.arraycopy(email.getBytes(),0,info.szEmail,0,email.getBytes().length);

                long p=12345678911l+i;
                String  phoneNo=""+p; // 注册车主电话号码
                System.arraycopy(phoneNo.getBytes(),0,info.szPhoneNo,0,phoneNo.getBytes().length);

                 infos[i]=info;
            }
            //将车辆数组对象赋值。
        inparam.stuVehicleInfo = new Memory(infos[0].size() * inparam.nVehicleNum);

        ToolKits.SetStructArrToPointerData(infos, inparam.stuVehicleInfo);


        inparam.bReplace=0;


        int pInParamSize=inparam.dwSize;
        Pointer pInParam =new Memory(pInParamSize);

        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);



        //出参
        NET_OUT_MULTI_APPEND_TO_VEHICLE_REG_DB outParam=new NET_OUT_MULTI_APPEND_TO_VEHICLE_REG_DB();


        int pOutParamSize=outParam.size();

        Pointer pOutParam =new Memory(pOutParamSize);

        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);


        System.out.println("outParam:"+outParam.dwSize);//4008

        System.out.println("inparam:"+inparam.dwSize);//1352012
     try {
         boolean isAdd
                 = netSdk.CLIENT_MultiAppendToVehicleRegisterDB(loginHandle, pInParam, pOutParam, 3000);
            //     = netSdk.CLIENT_MultiAppendToVehicleRegisterDB(loginHandle, inparam.getPointer(), outParam.getPointer(), 3000);
         if(isAdd){
             System.out.println(" add is success");
             outParam.read();
          //   ToolKits.GetPointerDataToStruct(pOutParam,0,outParam);
             int nErrCodeNum
                     = outParam.nErrCodeNum;

             Pointer ems = outParam.emErrCode;

             int[] emErrCode=new int[1000];

             ems.read(0,emErrCode,0,emErrCode.length);

             for(int i=0;i<nErrCodeNum;i++){
                 System.out.println(emErrCode[i]);
             }
         }else {
             System.err.println(" add is fail:"+ToolKits.getErrorCode());
         }
     }catch (Exception e){
         System.out.println("err:"+ToolKits.getErrorCode());
     }




    }


    /*public  void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "创建车辆组" , "CLIENT_CreateGroupForVehicleRegisterDB"));
        menu.addItem(new CaseMenu.Item(this , "删除车辆组" , "CLIENT_DeleteGroupFromVehicleRegisterDB"));
        menu.run();
    }*/


    public static void main(String[] args) {

        VehicleRegisterDBDemo vc=new VehicleRegisterDBDemo();
        Initialization.InitTest("172.12.234.233", 37777, "admin", "admin111");
        Scanner scanner=new Scanner(System.in);
            String id="";
        while (true){
            System.out.println("0 ,退出");
            System.out.println("1 ,创建车辆组");
            System.out.println("2 ,删除车辆组");
            System.out.println("3 ,向车牌库添加车辆信息");
            int step = scanner.nextInt();
            if(step==0){
                break;
            }else if(step==1){
              id  =  vc.CLIENT_CreateGroupForVehicleRegisterDB();
            }else if(step==2) {
                vc.CLIENT_DeleteGroupFromVehicleRegisterDB(id);
            }else if(step==3){
                vc.CLIENT_MultiAppendToVehicleRegisterDB();
            } else{
                break;
            }

        }
        Initialization.LoginOut();
    }

}
