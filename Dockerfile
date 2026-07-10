# ===== 构建阶段 =====
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 创建日志目录
RUN mkdir -p /var/log/student-archives

# 时区
RUN apk add --no-cache tzdata
ENV TZ=Asia/Shanghai

COPY --from=builder /build/target/student-archives-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
