package com.susan.training.advanced;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 第七章异步与 AI 示例配置。
 */
@ConfigurationProperties(prefix = "demo.chapter7")
public class Chapter7Properties {
    private boolean enabled;
    private final Async async = new Async();
    private final Ai ai = new Ai();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Async getAsync() {
        return async;
    }

    public Ai getAi() {
        return ai;
    }

    public static class Async {
        private Duration timeout = Duration.ofMillis(500);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Ai {
        private boolean remoteEnabled;
        private String model = "gpt-4.1-mini";

        public boolean isRemoteEnabled() {
            return remoteEnabled;
        }

        public void setRemoteEnabled(boolean remoteEnabled) {
            this.remoteEnabled = remoteEnabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
