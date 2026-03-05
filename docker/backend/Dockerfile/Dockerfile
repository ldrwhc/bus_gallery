# Stage 0: 构建
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# ========== 关键修改1：配置Maven国内镜像（阿里云） ==========
# 临时创建settings.xml，使用阿里云Maven镜像源
RUN mkdir -p /root/.m2 \
    && echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > /root/.m2/settings.xml \
    && echo "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"" >> /root/.m2/settings.xml \
    && echo "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" >> /root/.m2/settings.xml \
    && echo "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd\">" >> /root/.m2/settings.xml \
    && echo "    <mirrors>" >> /root/.m2/settings.xml \
    && echo "        <mirror>" >> /root/.m2/settings.xml \
    && echo "            <id>aliyunmaven</id>" >> /root/.m2/settings.xml \
    && echo "            <mirrorOf>central</mirrorOf>" >> /root/.m2/settings.xml \
    && echo "            <url>https://maven.aliyun.com/repository/public</url>" >> /root/.m2/settings.xml \
    && echo "        </mirror>" >> /root/.m2/settings.xml \
    && echo "    </mirrors>" >> /root/.m2/settings.xml \
    && echo "</settings>" >> /root/.m2/settings.xml

COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B clean package -DskipTests

# Stage 1: 运行
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ========== 关键修改2：替换Alpine国内源（阿里云） ==========
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

COPY --from=build /app/target/bus-gallery-backend-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]