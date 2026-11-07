# 构建阶段固定 Maven 与 JDK 主版本，避免依赖开发者机器环境。
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY demo-common/pom.xml demo-common/pom.xml
COPY demo-greeting-starter/pom.xml demo-greeting-starter/pom.xml
COPY demo-order/pom.xml demo-order/pom.xml
COPY demo-persistence/pom.xml demo-persistence/pom.xml
COPY demo-integration/pom.xml demo-integration/pom.xml
COPY demo-web/pom.xml demo-web/pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -B -DskipTests package

# 运行镜像只保留 JRE 和可执行 JAR。
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/demo-web/target/demo-web-1.0.0-SNAPSHOT.jar app.jar
RUN chown -R spring:spring /app

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
