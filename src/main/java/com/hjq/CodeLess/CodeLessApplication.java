package com.hjq.CodeLess;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hjq.CodeLess.mapper")
public class CodeLessApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.hjq.CodeLess.CodeLessApplication.class, args);
    }

}
