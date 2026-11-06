# Spring Boot 4.0 企业级工程示例

GitHub：[dvsusan/spring-boot4-demo](https://github.com/dvsusan/spring-boot4-demo)

这是一个可以直接运行、测试和扩展的 Spring Boot 4.0 多模块工程，配套《Spring Boot 4.0 企业级项目开发完整实践指南》。

项目目前覆盖模块边界、自定义 Starter、Profile 配置、Bean 生命周期、REST API、OpenAPI、实时消息、PostgreSQL、MyBatis、JPA、事务、Flyway、多租户、分片、Redis、分布式锁、RocketMQ、Outbox、Quartz、Elasticsearch、MinIO、Excel 批处理和企业安全。

## 技术栈

- Java 21
- Spring Boot 4.0.3
- Spring Framework 7.0.5
- Maven 3.9+
- Bean Validation
- Actuator
- Springdoc OpenAPI 3
- RestClient、HTTP Interface
- SSE、WebSocket
- MyBatis 4
- Spring Data JPA、Hibernate 7
- PostgreSQL 17、Flyway 11
- Redis、Redisson
- RocketMQ 4.x
- Quartz
- Elasticsearch 8
- MinIO
- Apache POI 5
- Spring Security 7
- JWT、OAuth2 Resource Server、OIDC Client
- Logback、MDC
- JUnit 5、AssertJ

## 模块说明

| 模块 | 职责 | 允许依赖 |
|---|---|---|
| `demo-common` | 公共响应结构 | JDK |
| `demo-greeting-starter` | 自定义自动配置与配置属性 | Spring Boot Autoconfigure |
| `demo-order` | 订单业务与异常 | `demo-common` |
| `demo-persistence` | JPA、MyBatis、PostgreSQL、Flyway、租户与分片 | `demo-order` |
| `demo-integration` | Redis、锁、RocketMQ、Outbox、Quartz、搜索、对象存储和 Excel 批处理 | `demo-order`、`demo-persistence` |
| `demo-web` | REST 接口、配置、生命周期、日志和启动 | `demo-order`、`demo-persistence`、`demo-integration`、`demo-greeting-starter` |

依赖方向固定为：

```text
demo-web -> demo-order -> demo-common
demo-web -> demo-persistence -> demo-order
demo-web -> demo-integration -> demo-persistence
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

## PostgreSQL 数据库

默认 Profile 使用内存 H2，保证 Web 章节无需数据库即可启动。数据库章节使用 PostgreSQL：

```bash
docker compose up -d
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=db
```

默认连接：

```text
jdbc:postgresql://localhost:5432/spring_boot4
```

可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖。Flyway 会自动执行 `db/migration` 下的版本脚本。

如果暂时没有 PostgreSQL，也可以用 H2 验证 JPA、MyBatis 分页和事务主流程：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=db-h2
```

原生嵌入式 PostgreSQL 测试默认跳过；允许启动 PostgreSQL 进程的环境可执行：

```bash
RUN_POSTGRES_TESTS=true mvn -pl demo-web -am test
```

设置库存并创建数据库订单：

```bash
curl -X PUT \
  "http://localhost:8080/api/v1/db/inventory/P1001?quantity=20"

curl -X POST "http://localhost:8080/api/v1/db/orders" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{
    "customerNo":"C1001",
    "customerName":"小明",
    "items":[{
      "productId":"P1001",
      "productName":"机械键盘",
      "quantity":2,
      "unitPrice":199.00
    }],
    "metadata":{"channel":"web"}
  }'
```

MyBatis 分页：

```bash
curl "http://localhost:8080/api/v1/db/orders?customerNo=C1001&page=1&size=10" \
  -H "X-Tenant-Id: demo"
```

PostgreSQL JSONB 与全文检索：

```bash
curl "http://localhost:8080/api/v1/db/orders/search?keyword=机械键盘&channel=web" \
  -H "X-Tenant-Id: demo"
```

Schema-per-tenant：

```bash
curl -X PUT \
  "http://localhost:8080/api/v1/db/tenant-notes/theme?value=blue" \
  -H "X-Tenant-Id: tenant_acme"
```

分片：

```bash
curl -X POST \
  "http://localhost:8080/api/v1/db/shards/orders?customerNo=C6001&amount=199.00"
```

## MinIO 文件与 Excel 批处理

第五章的能力默认关闭。启动应用时加上 `chapter5` Profile，下面两条命令也可以替换成对应启动参数：

```bash
docker compose up -d minio

java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=chapter5 \
  --server.port=18100
```

上传 PDF、PNG、JPEG、WebP 或文本文件：

```bash
curl -F file=@manual.pdf \
  "http://localhost:18100/api/v1/chapter5/files"
```

响应里的 `presignedUrl` 只在一段时间内有效。不要把它写进数据库，也不要替代业务权限校验。

流式导出 Excel：

```bash
curl -o orders.xlsx \
  "http://localhost:18100/api/v1/chapter5/excel/export?rows=2000"
```

异步导入并查询任务：

```bash
curl -F file=@orders.xlsx \
  "http://localhost:18100/api/v1/chapter5/excel/import"

curl \
  "http://localhost:18100/api/v1/chapter5/excel/jobs/<jobId>"
```

当前任务状态保存在进程内存中，重启后会丢失。生产环境应把任务状态写入数据库，并使用对象存储或消息队列承接跨实例任务。

## 企业安全

第六章默认关闭，使用 `chapter6` Profile 启动：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=chapter6 \
  --server.port=18106
```

登录并访问 JWT 身份接口：

```bash
curl -X POST "http://localhost:18106/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

curl "http://localhost:18106/api/v1/security/me" \
  -H "Authorization: Bearer <accessToken>"
```

教学账号：

| 用户 | 密码 | 角色 | 数据范围 |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | 本租户全部数据 |
| `analyst` | `analyst123` | ANALYST | 本部门 |
| `clerk` | `clerk123` | CLERK | 本人 |

数据权限：

```bash
curl "http://localhost:18106/api/v1/security/data/orders" \
  -H "Authorization: Bearer <accessToken>" \
  -H "X-Tenant-Id: tenant_acme"
```

企业 OIDC 单点登录默认不启用。配置企业 Client ID、Secret 和 Issuer 后，增加
`--demo.chapter6.oauth2.enabled=true`。回调地址为
`http://localhost:18106/login/oauth2/code/corp-oidc`。

支付接口需要 JWT、`Idempotency-Key` 和 HMAC 请求签名。签名规范、脚本和完整验证步骤见第 36 篇教程。

JWT 撤销表、刷新令牌、请求 nonce 和幂等响应当前保存在进程内存中，只适合单实例教学。生产环境应迁移到 Redis 或数据库，并增加审计与定期清理。

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

### 按客户查询

```bash
curl "http://localhost:8080/api/v1/orders?customerId=C1001&limit=20"
```

### 更新金额

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/orders/SO685E417C3C4F" \
  -H "Content-Type: application/json" \
  -d '{"amount":88.00}'
```

### API 版本

Header 版本：

```bash
curl "http://localhost:8080/api/orders/SO685E417C3C4F" \
  -H "X-Api-Version: 2"
```

媒体类型版本：

```bash
curl "http://localhost:8080/api/orders/SO685E417C3C4F" \
  -H "Accept: application/vnd.susan.order.v3+json"
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

## OpenAPI 与实时事件

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI YAML：

```bash
curl "http://localhost:8080/v3/api-docs.yaml"
```

重新生成 TypeScript 客户端：

```bash
./scripts/generate-order-client.sh
```

SSE 订阅：

```bash
curl -N "http://localhost:8080/api/v1/events/orders?customerId=C1001"
```

WebSocket 地址：

```text
ws://localhost:8080/ws/orders?customerId=C1001
```

## 缓存、消息与搜索

外部能力默认关闭。需要联调第 22–28 篇时启动 Docker 服务：

```bash
docker compose up -d redis rocketmq-namesrv rocketmq-broker elasticsearch
```

使用完整章节配置启动：

```bash
java -jar demo-web/target/demo-web-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=chapter4
```

只启用某个能力时，可以分别使用：

```text
--demo.chapter4.redis.enabled=true
--demo.chapter4.redisson.enabled=true
--demo.chapter4.rocketmq.enabled=true
--demo.chapter4.jobs.enabled=true
--demo.chapter4.elasticsearch.enabled=true
```

Redis 读穿缓存：

```bash
curl "http://localhost:8080/api/v1/chapter4/cache/orders/SO1001"
```

Redisson 锁：

```bash
curl -X POST \
  "http://localhost:8080/api/v1/chapter4/locks/orders/SO1001"
```

RocketMQ 消息：

```bash
curl -X POST \
  "http://localhost:8080/api/v1/chapter4/messages/orders/SO1001"
```

Outbox：

```bash
curl -X POST "http://localhost:8080/api/v1/chapter4/outbox" \
  --data-urlencode "eventId=evt-1001" \
  --data-urlencode "aggregateId=SO1001" \
  --data-urlencode "payload={\"orderNo\":\"SO1001\"}"

curl "http://localhost:8080/api/v1/chapter4/outbox"
```

Elasticsearch 索引与读取：

```bash
curl -X POST \
  "http://localhost:8080/api/v1/chapter4/search/orders/SO1001"

curl \
  "http://localhost:8080/api/v1/chapter4/search/orders/SO1001"
```

## 项目结构

```text
.
├── demo-common
├── demo-greeting-starter
├── demo-order
├── demo-persistence
├── demo-integration
├── demo-web
├── openapi/order-api.yaml
├── scripts/generate-order-client.sh
├── docker-compose.yml
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
- API 同时展示 URL、Header 和厂商媒体类型三种版本方案。
- 全局异常处理器把业务异常、参数错误和协议错误映射为稳定错误码。
- 创建和更新复用请求对象，通过 Validation Groups 选择不同校验规则。
- `OrderApiClient` 使用 `@HttpExchange` 声明远端接口，由 `RestClient` 执行。
- SSE 和 WebSocket 共用 `OrderEventBroker`，进程内实现仅适用于单实例教学。
- JPA 负责订单聚合写入，MyBatis 负责联表分页和 PostgreSQL 高级查询。
- 订单写入与库存扣减在同一事务中，失败审计使用 `REQUIRES_NEW`。
- Flyway 管理结构，Hibernate 使用 `ddl-auto=validate` 校验映射。
- 动态数据源按租户路由到独立 PostgreSQL Schema。
- 分片示例使用订单号哈希路由两个物理分片表。
- 外部中间件 Bean 全部按配置开关创建，未开启时返回 `FEATURE_DISABLED`。
- Redis 缓存使用 JSON 和 TTL，分布式锁只在显式开启 Redisson 时创建。
- Outbox 与业务共用关系数据库；Quartz 周期扫描待投递消息。
- Elasticsearch 是可重建的搜索读模型，不作为订单事实来源。

## 当前限制

这是教学项目，不是完整生产系统：

- 旧版 `/api/v1/orders` 仍保存在内存中；数据库接口使用 `/api/v1/db/orders`。
- 订单号使用 UUID 片段，不代表生产订单号方案。
- Redis、RocketMQ 和 Elasticsearch 需要本地 Docker；默认 Profile 不连接这些服务。
- 认证授权尚未接入。
- 查询只有数量上限，没有实现游标分页、幂等和多实例事件广播。

## 配套教程

工程基建系列已经覆盖：

1. Spring Boot 4.0 新特性
2. 企业级开发环境搭建
3. 多模块项目搭建
4. 自动配置与自定义 Starter
5. Profile 与配置管理
6. Bean 生命周期与依赖注入
7. Logback、MDC 与日志
8. Spring MVC 接口设计
9. REST API 版本管理
10. 全局异常处理与错误码
11. 参数校验、分组校验与国际化
12. OpenAPI 与客户端代码生成
13. RestClient 与 HTTP Interface
14. SSE 与 WebSocket 实时消息推送
15. MyBatis 多表查询与分页
16. Spring Data JPA 实体与关联
17. 事务传播、隔离与失效场景
18. Flyway 数据库迁移
19. 动态数据源与多租户
20. PostgreSQL JSONB、索引与全文检索
21. 分库分表、路由与跨库查询
22. Redis 缓存设计与序列化
23. 缓存双写、失效与延迟删除
24. Redisson 分布式锁
25. RocketMQ 消息发送与消费
26. 事务消息与 Outbox
27. Quartz、XXL-JOB 与延时任务
28. Elasticsearch 搜索与聚合
29. MinIO 文件上传、预览与权限控制
30. Excel 百万级导入导出与异步处理
31. Spring Security 7 认证、授权与过滤器链
32. JWT 登录、刷新令牌与退出登录
33. OAuth2 与 SSO 单点登录
34. 多租户 RBAC 权限系统
35. 行级、列级与部门数据权限
36. 接口幂等、签名与防重放

后续教程会继续接入可观测性和部署能力。

## 参与贡献

欢迎提交 Issue 或 Pull Request。提交代码前请先执行：

```bash
mvn clean verify
```

## License

[MIT](LICENSE)
# spring-boot4-demo
