package com.abc.wechat.log;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class LogAspect {
    private final static Logger logger = LoggerFactory.getLogger(LogAspect.class);
    ThreadLocal<Long> startTime = new ThreadLocal<>();


    @Pointcut("execution(* com.abc.wechat.service.Impl.ChatRecordsServiceImpl.upload(..))")
    public void uploadLog(){}

    @Before("uploadLog()")
    public void doBefore( ) throws Throwable{
        startTime.set(System.currentTimeMillis());
    }

    @AfterReturning(value = "uploadLog()", returning = "res")
    public void doAfterReturning(Object res){
        if(null == res) {logger.info("----->没有更多新消息");};
        List<Integer>list =  (List<Integer>)res;
        long endTime = System.currentTimeMillis();
        logger.info("成功上传消息{}条, 失败{}条, 耗时{}ms",list.get(0), list.get(1), endTime - startTime.get());
    }

}
