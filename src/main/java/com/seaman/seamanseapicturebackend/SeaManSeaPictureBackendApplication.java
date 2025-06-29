package com.seaman.seamanseapicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.seaman.seamanseapicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)//暴露代理类
public class SeaManSeaPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeaManSeaPictureBackendApplication.class, args);
    }

}
