package com.susan.training.persistence.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 MyBatis Mapper，并让 JPA 继续使用 Spring Boot 自动配置。
 */
@Configuration
@MapperScan("com.susan.training.persistence.mapper")
public class PersistenceConfiguration {
}
