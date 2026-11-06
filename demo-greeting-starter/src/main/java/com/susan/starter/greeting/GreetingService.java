package com.susan.starter.greeting;

/**
 * 自定义 Starter 暴露给业务使用的服务。
 */
public class GreetingService {
    private final GreetingProperties properties;

    public GreetingService(GreetingProperties properties) {
        this.properties = properties;
    }

    /**
     * 根据配置模板生成问候语。
     */
    public String greet(String name) {
        return properties.getMessage().formatted(name);
    }
}
