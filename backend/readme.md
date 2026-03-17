# Bus Gallery Backend

后端基于 Spring Boot + MyBatis + MySQL + Redis + MinIO，提供车辆档案、图片、评论、收藏、快照、上传等接口，并支持 Redis 缓存与缩略图生成。

---

## 技术栈

- Java 17
- Spring Boot 3
- MyBatis + MySQL 8
- Redis（会话、快照、列表缓存）
- MinIO（对象存储）

---

## 运行方式

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

打包运行：

```bash
mvn clean package
java -jar target/bus-gallery-backend-0.0.1-SNAPSHOT.jar
```

---

## 关键设计点（面试高频）

1. Redis 列表缓存 + 版本号一致性
- `/api/vehicles` 缓存进 Redis（TTL 60s）
- key 组合：筛选参数 + 游标 + `bg:vehicle:page:version`
- 新增/更新/删除车辆时版本号自增，自动失效旧缓存

2. 车辆详情快照（big key）
- `/api/snapshots/plate/{plate}` 直接返回车牌级详情快照
- Redis key：`bg:snapshot:plate:{plate}:latest` + `...:v{version}`
- 快照内容包含变体、评论、收藏摘要、推荐

3. 幂等上传
- `/api/upload` 支持 `Idempotency-Key`
- 避免重复提交造成脏数据

4. 缩略图生成与重建
- 上传时自动生成缩略图
- 历史数据可通过 `rebuild.thumbnails=true` 重建

5. 统一错误码 + 全局异常
- ErrorCode 枚举统一返回
- GlobalExceptionHandler 保证响应格式一致

6. 会话与鉴权
- Redis 存储 session
- `AuthTokenInterceptor` 读取 token 并校验
- `@RequireLogin` 注解统一鉴权入口

---

## 面试追问点（可直接回答）

- 你如何保证缓存一致性：版本号失效 + TTL
- big key 带来的代价：大 key 删除、持久化开销，用 TTL + lazyfree 缓解
- 上传为什么要幂等：避免重复提交产生脏数据
- 为什么不用偏移分页：大数据量下偏移会变慢

---

## Swagger 文档

- 在线：`http://localhost:8080/swagger-ui/index.html`
- 静态导出见：`docs/swagger/`

---

## 缩略图重建任务

```bash
java -jar target/bus-gallery-backend-0.0.1-SNAPSHOT.jar --rebuild.thumbnails=true
```

---

完整接口说明请见项目根目录：`API_DOCS.md`
