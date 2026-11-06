package com.susan.training.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 定义 OpenAPI 文档的标题、版本和服务入口。
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI orderPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 4 订单履约平台 API")
                        .version("v1")
                        .description("课程示例接口，覆盖订单、版本管理和实时事件")
                        .contact(new Contact().name("苏三")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("本地开发环境")));
    }
}
