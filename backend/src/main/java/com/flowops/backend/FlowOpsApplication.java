package com.flowops.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.flowops.backend","com.flowops.common"})
public class FlowOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowOpsApplication.class, args);
    }

}
