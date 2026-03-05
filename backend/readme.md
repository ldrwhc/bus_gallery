
# Bus Gallery Backend

基于 Spring Boot + MyBatis + MySQL + MinIO 的公交车图库管理系统后端，提供地区/公司/品牌/型号分类查询、车辆详情管理、图片上传等功能。

## 技术栈

- **Java 17**
- **Spring Boot 3.x**
- **MyBatis + MySQL 8**
- **MinIO 对象存储**（可替换为 OSS/S3/COS 等）
- **Lombok、MapStruct、Springdoc OpenAPI**

## 功能概览

- 地区、公司、品牌、型号、车辆、图片的 CRUD 接口。
- 按地区/公司/品牌/型号分类展示车辆和图片。
- 支持车辆详情：车辆信息、车型配置、图片列表。
- 图片上传至对象存储（MinIO），自动生成访问 URL。
- Swagger/OpenAPI 接口文档。
- 完整的 Docker 化部署方案（MySQL、MinIO、后端、前端）。

## 项目结构（后端部分）

```
bus-gallery/backend
├── src/main/java/com/busgallery/busgallery
│   ├── BusGalleryApplication.java
│   ├── config/             # MinIO、Swagger、跨域等配置
│   ├── controller/         # REST 控制器
│   ├── dto/                # 请求/响应 DTO
│   ├── entity/             # 实体定义
│   ├── exception/          # 全局异常处理
│   ├── mapper/             # MyBatis Mapper 接口
│   ├── service/            # 业务服务 & 存储服务
│   └── util/               # 工具类
├── src/main/resources
│   ├── application.yml     # 配置文件
│   └── mapper/*.xml        # MyBatis XML 映射
└── src/test/java/com/busgallery/busgallery
    ├── BusGalleryApplicationTests.java
    ├── controller/VehicleControllerTest.java
    └── service/VehicleServiceImplTest.java
```

## 配置说明

`application.yml` 核心配置：

```yaml
spring:
  datasource: ...
mybatis:
  mapper-locations: classpath*:mapper/*.xml
minio:
  endpoint: http://minio:9000
  bucket: bus-gallery
  access-key: admin
  secret-key: admin123
```

运行时可通过环境变量覆盖 `DB_URL`、`MINIO_ENDPOINT` 等配置。

## 构建与运行

### 本地 Maven 构建

```bash
cd bus-gallery/backend
mvn clean package
java -jar target/bus-gallery-backend-0.0.1-SNAPSHOT.jar
```

### Docker Compose

在项目根目录执行：

```bash
cd bus-gallery/docker
docker compose up -d
```

将启动以下服务：

- `mysql` – 数据库，初始化脚本位于 `docker/init/init.sql`
- `minio` – 对象存储
- `backend` – Spring Boot 服务（映射 8080 端口）
- `frontend` – Vue 静态站点（映射 80 端口）

## 接口文档

启动后访问 `http://localhost:8080/swagger-ui.html` 或 `http://localhost:8080/swagger-ui/index.html` 查看 API 文档，支持在线调试。

## 测试

```bash
cd bus-gallery/backend
mvn test
```

主要测试：

- `BusGalleryApplicationTests`：Spring 容器启动测试。
- `VehicleServiceImplTest`：Service 层单元测试。
- `VehicleControllerTest`：MockMvc Controller 测试。

## 注意事项

- 首次运行需确保 MySQL 数据库创建成功、执行建表脚本。
- MinIO 需提前配置存储桶，可以通过 `MinioStorageService` 在启动时自动创建。
- 如需调整对象存储为其它服务（OSS/COS/S3等），实现 `StorageService` 接口即可。
- 若不使用 Spring Data JPA，可去除 `repository` 目录及相关依赖。

---

欢迎根据业务需求继续扩展接口和前端展示。