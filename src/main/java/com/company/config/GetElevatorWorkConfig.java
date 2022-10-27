package com.company.config;

import com.netsdk.demo.customize.GetElevatorWorkDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created By TangXiangLin on 2022-10-27 18:03
 */
@Configuration
public class GetElevatorWorkConfig {
    private static final Logger logger = LoggerFactory.getLogger(GetElevatorWorkConfig.class);

    @Bean
    public GetElevatorWorkDemo getElevatorWorkDemo(){
        logger.info("GetElevatorWorkDemo init ..");
        GetElevatorWorkDemo demo = new GetElevatorWorkDemo();
        demo.getElevatorWorkInfo();
        logger.info("GetElevatorWorkDemo init success: [{}]",demo.hashCode());
        return demo;
    }
}
