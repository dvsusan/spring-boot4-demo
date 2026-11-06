package com.susan.training.web;

import com.susan.training.common.ApiResponse;
import com.susan.training.config.DemoEnvironmentProperties;
import com.susan.starter.greeting.GreetingService;
import com.susan.training.lifecycle.BeanLifecycleDemo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 工程基建章节使用的系统能力示例。
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final Environment environment;
    private final DemoEnvironmentProperties properties;
    private final BeanLifecycleDemo lifecycleDemo;
    private final ObjectProvider<GreetingService> greetingServiceProvider;

    public SystemController(
            Environment environment,
            DemoEnvironmentProperties properties,
            BeanLifecycleDemo lifecycleDemo,
            ObjectProvider<GreetingService> greetingServiceProvider
    ) {
        this.environment = environment;
        this.properties = properties;
        this.lifecycleDemo = lifecycleDemo;
        this.greetingServiceProvider = greetingServiceProvider;
    }

    /**
     * 展示当前运行时版本和激活的 Profile。
     */
    @GetMapping("/info")
    public ApiResponse<SystemInfo> info(
            @Value("${spring.application.name:unknown}") String applicationName
    ) {
        return ApiResponse.success(new SystemInfo(
                applicationName,
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                Arrays.asList(environment.getActiveProfiles())
        ));
    }

    /**
     * 调用自定义 Starter 提供的问候服务。
     */
    @GetMapping("/greeting")
    public ApiResponse<String> greeting(@RequestParam(defaultValue = "Spring Boot") String name) {
        GreetingService greetingService = greetingServiceProvider.getIfAvailable();
        if (greetingService == null) {
            return ApiResponse.failure("GREETING_DISABLED", "自定义 Starter 已关闭");
        }
        return ApiResponse.success(greetingService.greet(name));
    }

    /**
     * 展示当前环境的配置值。
     */
    @GetMapping("/config")
    public ApiResponse<ConfigInfo> config() {
        return ApiResponse.success(new ConfigInfo(
                properties.getLabel(),
                properties.getMaxPageSize(),
                properties.getRegion(),
                environment.getProperty("server.port", "8080")
        ));
    }

    /**
     * 展示 Bean 生命周期状态。
     */
    @GetMapping("/bean")
    public ApiResponse<String> beanStatus() {
        return ApiResponse.success(lifecycleDemo.status());
    }

    /**
     * 在虚拟线程中执行一次短任务，帮助读者确认线程类型。
     */
    @GetMapping("/virtual-thread")
    public ApiResponse<VirtualThreadInfo> virtualThread() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<VirtualThreadInfo> future = executor.submit(() -> {
                Thread.sleep(20);
                Thread current = Thread.currentThread();
                return new VirtualThreadInfo(current.isVirtual(), current.toString());
            });
            return ApiResponse.success(future.get());
        }
    }

    public record SystemInfo(
            String applicationName,
            String javaVersion,
            String javaVendor,
            java.util.List<String> activeProfiles
    ) {
    }

    public record ConfigInfo(
            String environmentLabel,
            int maxPageSize,
            String region,
            String port
    ) {
    }

    public record VirtualThreadInfo(
            boolean virtual,
            String threadName
    ) {
    }
}
