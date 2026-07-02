package com.mzy.xyswzlsys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mzy.xyswzlsys.mapper")
public class XyswzlSysApplication {

    public static void main(String[] args) {
        SpringApplication.run(XyswzlSysApplication.class, args);
    }

}
