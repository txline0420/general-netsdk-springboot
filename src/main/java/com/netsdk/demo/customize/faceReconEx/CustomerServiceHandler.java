package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.demo.util.EventTaskHandler;

/**
 * @author ： 47040
 * @since ： Created in 2021/2/5 18:03
 */
public class CustomerServiceHandler implements EventTaskHandler {

    CustomerServiceModel model;

    CustomerServiceHandler(CustomerServiceModel model) {
        this.model = model;
    }

    @Override
    public void eventCallBackProcess() {
        // Todo 自定义业务逻辑
        System.out.println("做了一些其他业务...");
    }
}
