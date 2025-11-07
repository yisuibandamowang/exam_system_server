package com.atguigu.exam.exceptionhandlers;

import com.atguigu.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //定义异常处理类型
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace();
        log.error("异常信息：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

}

