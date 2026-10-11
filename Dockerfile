FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# 先复制 pom.xml 并下载依赖（利用 Docker 层缓存）
COPY pom.xml .
RUN mvn dependency:resolve -B

# 复制源码和测试
COPY src/ src/

# 编译 + 运行单测（surefire）和集成测试（failsafe）+ 打包
RUN mvn verify -B
