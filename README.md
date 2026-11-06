# Spring Boot 4.0 企业级工程示例

这是一个可以直接运行、测试和扩展的 Spring Boot 4.0 多模块工程，配套《Spring Boot 4.0 企业级项目开发完整实践指南》。

项目目前覆盖模块边界、自定义 Starter、Profile 配置、Bean 生命周期、虚拟线程、MDC 请求日志、统一响应、参数校验和异常处理，适合作为 Spring Boot 4 企业项目的工程基建骨架。

## 技术栈

- Java 21
- Spring Boot 4.0.3
- Spring Framework 7.0.5
- Maven 3.9+
- Bean Validation
- Actuator
- Logback、MDC
- JUnit 5、AssertJ

## 模块说明

| 模块 | 职责 | 允许依赖 |
|---|---|---|
| `demo-common` | 公共响应结构 | JDK |
| `demo-greeting-starter` | 自定义自动配置与配置属性 | Spring Boot Autoconfigure |
| `demo-order` | 订单业务与异常 | `demo-common` |
| `demo-web` | REST 接口、配置、生命周期、日志和启动 | `demo-order`、`demo-greeting-starter` |

依赖方向固定为：

```text
demo-web -> demo-order -> demo-common
demo-web -> demo-greeting-starter
```

业务模块 `demo-order` 不依赖 Web，也不依赖 Spring 容器。它会由 `demo-web` 中的配置类装配为 Spring Bean。

## 环境要求

```bash
java -version
mvn -version
```

建议使用 JDK 21 LTS 和 Maven 3.9.x。项目通过 `java.version=21` 编译。

## 构建与测试

```bash
mvn clean verify
```

如果只想快速构建：

```bash
mvn clean package -DskipTests
```

## 启动项目

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar
```

默认端口是 `8080`。端口被占用时可以临时指定：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar --server.port=18080
```

默认激活 `dev` Profile，也可以切换生产配置：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

## 订单 API

### 创建订单

```bash
curl -i -X POST "http://localhost:8080/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C1001","amount":99.00}'
```

返回 HTTP 201：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "orderNo": "SO685E417C3C4F",
    "customerId": "C1001",
    "amount": 99.0,
    "status": "CREATED"
  },
  "timestamp": "2026-11-06T01:59:50.000Z"
}
```

订单号和时间戳每次都会变化。

### 查询订单

```bash
curl "http://localhost:8080/api/v1/orders/SO685E417C3C4F"
```

### 参数校验

```bash
curl -i -X POST "http://localhost:8080/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"","amount":0}'
```

返回 HTTP 400 和业务错误码 `INVALID_ARGUMENT`。

### 订单不存在

```bash
curl -i "http://localhost:8080/api/v1/orders/NOT-EXISTS"
```

返回 HTTP 404 和业务错误码 `ORDER_NOT_FOUND`。

### 健康检查

```bash
curl "http://localhost:8080/actuator/health"
```

## 工程基建 API

### 查看运行环境

```bash
curl "http://localhost:8080/api/v1/system/info"
```

### 验证虚拟线程

```bash
curl "http://localhost:8080/api/v1/system/virtual-thread"
```

### 自定义 Starter 问候服务

```bash
curl "http://localhost:8080/api/v1/system/greeting?name=小明"
```

关闭 Starter：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --demo.greeting.enabled=false
```

### 查看最终配置

```bash
curl "http://localhost:8080/api/v1/system/config"
```

使用环境变量覆盖部署区域：

```bash
DEMO_REGION=cn-east java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar
```

### 查看 Bean 生命周期状态

```bash
curl "http://localhost:8080/api/v1/system/bean"
```

每个请求的响应头都会包含 `X-Trace-Id`，控制台日志可使用同一值检索。

## 项目结构

```text
.
├── demo-common
├── demo-greeting-starter
├── demo-order
├── demo-web
├── .github/workflows/build.yml
├── LICENSE
└── pom.xml
```

## 设计说明

- `demo-order` 中的 `OrderService` 是普通 Java 类，不使用 `@Service`。
- `demo-web` 中的 `OrderConfiguration` 负责创建 `OrderService` Bean。
- `demo-greeting-starter` 通过 AutoConfiguration.imports 注册自动配置。
- `DemoEnvironmentProperties` 集中绑定环境相关配置。
- `BeanLifecycleDemo` 演示构造器、`@PostConstruct` 和 `@PreDestroy`。
- `TraceIdFilter` 为请求写入 MDC，并在请求结束时清理。
- Controller 只处理 HTTP 协议，不承载订单业务规则。
- HTTP 请求对象和业务命令分开，避免业务层依赖 `jakarta.validation`。
- 金额使用 `BigDecimal`，不使用 `double`。

## 当前限制

这是教学项目，不是完整生产系统：

- 订单保存在内存中，重启后丢失。
- 订单号使用 UUID 片段，不代表生产订单号方案。
- 没有接入数据库、Redis、消息队列和认证授权。
- 没有实现分页、幂等和多实例一致性问题。

## 配套教程

工程基建系列已经覆盖：

1. Spring Boot 4.0 新特性
2. 企业级开发环境搭建
3. 多模块项目搭建
4. 自动配置与自定义 Starter
5. Profile 与配置管理
6. Bean 生命周期与依赖注入
7. Logback、MDC 与日志

后续教程会在这个骨架上继续接入 MyBatis、JPA、数据库迁移、缓存、消息队列和安全能力。

## 参与贡献

欢迎提交 Issue 或 Pull Request。提交代码前请先执行：

```bash
mvn clean verify
```

## License

[MIT](LICENSE)
# spring-boot4-demo
