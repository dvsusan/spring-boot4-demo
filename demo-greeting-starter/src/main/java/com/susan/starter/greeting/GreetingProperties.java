package com.susan.starter.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义 Starter 的可配置项。
 *
 * <p>属性前缀是 demo.greeting，使用方只需在 application.yml 中配置即可，
 * 不需要修改 Starter 内部代码。</p>
 */
@ConfigurationProperties(prefix = "demo.greeting")
public class GreetingProperties {
    // 默认开启，方便引入 Starter 后立即看到效果。
    private boolean enabled = true;

    // %s 是调用方传入的名字，由 GreetingService 负责格式化。
    private String message = "你好，%s";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
