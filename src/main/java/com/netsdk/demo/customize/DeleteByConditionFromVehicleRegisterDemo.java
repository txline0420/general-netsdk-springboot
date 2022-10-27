package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_COLOR_RGBA;
import com.netsdk.lib.structure.NET_DELETE_VEHICLE_CONDITION_INFO;
import com.netsdk.lib.structure.NET_IN_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB;
import com.netsdk.lib.structure.NET_OUT_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB;
import com.netsdk.lib.utils.Initialization;

/**
 * @author 291189
 * @version 1.0
 * @description GIP220224022 上海市全区道口动态管控项目SDK定制（Java）
 * @date 2022/2/25 14:54
 */
public class DeleteByConditionFromVehicleRegisterDemo extends Initialization {

    public void DeleteByCondition(){

        NET_IN_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB input=new NET_IN_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB();

        byte[]					szGroupID=new byte[64];

        String szGroupId="1";

        byte[] bytes = szGroupId.getBytes();
        System.arraycopy(bytes,0,szGroupID,0,bytes.length);
        //   车辆组标识
        input.szGroupID=szGroupID;

        //删除车辆的过滤条件

         NET_DELETE_VEHICLE_CONDITION_INFO stuDelCondition=new NET_DELETE_VEHICLE_CONDITION_INFO();

         byte[]					szPlateCountry=new byte[4];

         String country="CN";

         byte[] countryByte = country.getBytes();

        System.arraycopy(countryByte,0,szPlateCountry,0,countryByte.length);

        //CN
        stuDelCondition.szPlateCountry=szPlateCountry;

        //车牌信息 不能填写
/**
 车色 第一个元素表示红色分量值； 第二个元素表示绿色分量值； 第三个元素表示蓝色分量值；
 注意：第四个元素不再表示透明度分量，而用来表示车色字段是否是一个有效的过滤条件，0 - 无效的过滤条件，非0 - 有效的过滤条件
 */


        NET_COLOR_RGBA stuVehicleColor =new NET_COLOR_RGBA();
        stuVehicleColor.nRed=0;
        stuVehicleColor.nGreen=0;
        stuVehicleColor.nBlue=0;
        stuVehicleColor.nAlpha=0;//此处c 库有个转换 o 转换成 255 ；255 转换成 0
        stuDelCondition.stuVehicleColor=stuVehicleColor;


        input.stuDelCondition=stuDelCondition;

        NET_OUT_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB output=new NET_OUT_DEL_BY_CONDITION_FROM_VEHICLE_REG_DB();


        input.write();
        output.write();


        boolean b
                = netSdk.CLIENT_DeleteByConditionFromVehicleRegisterDB(loginHandle, input.getPointer(), output.getPointer(), 3000);

        if(b){
            System.out.println(" CLIENT_DeleteByConditionFromVehicleRegisterDB  success");
        }else {
            System.out.println(" CLIENT_DeleteByConditionFromVehicleRegisterDB  fail "+ ToolKits.getErrorCode());
        }
                input.clear();
                output.clear();


    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "DeleteByCondition" , "DeleteByCondition")));

        menu.run();
    }

    public static void main(String[] args) {
        DeleteByConditionFromVehicleRegisterDemo deleteByConditionFromVehicleRegisterDemo=new DeleteByConditionFromVehicleRegisterDemo();
        InitTest("20.2.36.54",37777,"admin","admin123");
        deleteByConditionFromVehicleRegisterDemo.RunTest();
        LoginOut();
    }
}
