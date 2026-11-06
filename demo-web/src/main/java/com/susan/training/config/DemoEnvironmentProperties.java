package com.susan.training.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 演示不同环境的业务配置。
 */
@ConfigurationProperties(prefix = "demo.environment")
public class DemoEnvironmentProperties {
    // 当前环境名称，例如 dev、prod。
    private String label = "local";

    // 示例分页上限，用来演示配置项如何进入业务代码。
    private int maxPageSize = 100;

    // 部署区域既可由配置文件提供，也可由环境变量 DEMO_REGION 覆盖。
    private String region = "local";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
