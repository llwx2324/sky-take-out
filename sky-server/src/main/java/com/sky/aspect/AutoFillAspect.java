package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 切面类,用于实现公共字段的自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFillBefore(JoinPoint joinPoint){
        log.info("公共字段自动填充...");
        //获取当前执行的方法对象
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        //获取方法上的注解对象
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        //根据注解的属性值,判断是新增还是修改
        OperationType operationType = autoFill.value();
        //获取当前操作的数据对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }
        Object object = args[0];
        //获取当前登录用户id
        Long currentId = BaseContext.getCurrentId();
        //获取当前时间
        LocalDateTime now = LocalDateTime.now();
        //如果是新增,填充4个字段
        //如果是修改,填充2个字段
        if(operationType == OperationType.INSERT){
            try {
                Method setCreateUser = object.getClass().getMethod("setCreateUser", Long.class);
                Method setCreateTime = object.getClass().getMethod("setCreateTime", LocalDateTime.class);
                Method setUpdateUser = object.getClass().getMethod("setUpdateUser", Long.class);
                Method setUpdateTime = object.getClass().getMethod("setUpdateTime", LocalDateTime.class);
                setCreateUser.invoke(object, currentId);
                setCreateTime.invoke(object, now);
                setUpdateUser.invoke(object, currentId);
                setUpdateTime.invoke(object, now);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(operationType == OperationType.UPDATE){
            try {
                Method setUpdateUser = object.getClass().getMethod("setUpdateUser", Long.class);
                Method setUpdateTime = object.getClass().getMethod("setUpdateTime", LocalDateTime.class);
                setUpdateUser.invoke(object, currentId);
                setUpdateTime.invoke(object, now);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
