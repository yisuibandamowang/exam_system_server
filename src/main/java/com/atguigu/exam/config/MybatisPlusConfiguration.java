package com.atguigu.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.atguigu.exam.mapper")
public class MybatisPlusConfiguration {

}