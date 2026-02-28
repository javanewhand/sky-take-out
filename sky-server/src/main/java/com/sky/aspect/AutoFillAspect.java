package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
* 自定义切面类
* */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /*
    * 指定切入点
    * */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) &&@annotation(com.sky.annotation.AutoFill)")
    public  void autoFillPointCut(){}

/*
* 前置通知，在通知中进行公共字段的赋值*/
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("公共字段自动填充");

        //获取到当前被拦截的数据库方法的操作类型
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autofill = methodSignature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解对象
        OperationType operationType = autofill.value();//获得数据库操作类型

        //获取到当前被拦截方法的实体对象--参数
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }
        Object entity = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long CurrentId = BaseContext.getCurrentId();

        //根据当前不同的类型，为对应的属性通过反射赋值
        if (operationType == OperationType.INSERT) {
            //为四个公共字段赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);


                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,CurrentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,CurrentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if (operationType == OperationType.UPDATE) {
            //为两个公共字段赋值
            try {
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);

                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,CurrentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
