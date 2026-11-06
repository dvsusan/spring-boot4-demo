package com.susan.training.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 Web 层使用的配置属性。
 */
@Configuration
@EnableConfigurationProperties(DemoEnvironmentProperties.class)
public class WebPropertiesConfiguration {
}
