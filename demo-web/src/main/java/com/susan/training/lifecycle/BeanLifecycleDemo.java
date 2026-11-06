package com.susan.training.lifecycle;

import com.susan.training.config.DemoEnvironmentProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 演示 Spring Bean 的创建、初始化和销毁阶段。
 *
 * <p>构造器执行时依赖已经注入，但容器还没有完成全部初始化；
 * @PostConstruct 适合做本对象内部的准备工作。</p>
 */
@Component
public class BeanLifecycleDemo {
    private static final Logger log = LoggerFactory.getLogger(BeanLifecycleDemo.class);

    private final DemoEnvironmentProperties environmentProperties;
    private volatile String status = "CREATED";

    public BeanLifecycleDemo(DemoEnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
        log.info("BeanLifecycleDemo 构造器执行，environment={}",
                environmentProperties.getLabel());
    }

    @PostConstruct
    public void init() {
        status = "READY";
        log.info("BeanLifecycleDemo 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        status = "STOPPED";
        log.info("BeanLifecycleDemo 即将销毁");
    }

    public String status() {
        return status;
    }
}
