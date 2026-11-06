package com.susan.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口。
 *
 * <p>启动类放在根包 com.susan.training 下，确保各模块的 Bean 都能被扫描到。</p>
 */
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        // 启动 Spring 容器和嵌入式 Tomcat。
        SpringApplication.run(DemoApplication.class, args);
    }
}
