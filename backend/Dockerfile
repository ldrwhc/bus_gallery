##############################################
# Stage 1: Build with Maven
##############################################
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/mvkvl/graalvm:native-image-community-21-muslib-maven-3.9.6 AS build
WORKDIR /app

# 配置Maven阿里云镜像源（解决依赖下载慢）
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

# 核心：context是项目根目录，所以COPY backend/xxx 指向根目录的backend文件夹
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline

COPY backend/src ./src
RUN mvn -B clean package -DskipTests

##############################################
# Stage 2: Run with JRE
##############################################
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

# 替换Alpine阿里云软件源
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

COPY --from=build /app/target/bus-gallery-backend-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]