# Bus Gallery

> 一套面向客运 / 公交车辆资料的全栈解决方案，包含车辆档案管理、图片管理、品牌/车型/公司/地区维度查询及上传建档功能。项目采用 Spring Boot + MyBatis + MySQL + MinIO（后端）和 Vite + Vue 3 + Element Plus（前端），并通过 Docker Compose 提供一键化部署。

---

## 功能亮点

- **车辆档案管理**：支持按地区、公司、品牌、车型分页查询与详情查看。
- **图片上传与存储**：前端支持拖拽上传，后端结合 MinIO 统一管理图片资源。
- **数据字典维护**：品牌、车型、公司、地区等基础信息的增删改查。
- **RESTful API**：统一的 DTO + Service + Mapper 架构，方便扩展。
- **Docker Compose 启动**：内置 MySQL、MinIO、后端、前端容器，一条命令即可运行。

---

## 目录结构

```
bus-gallery/
├─ docker/
│  ├─ init/          # MySQL 初始化脚本（init.sql）
│  ├─ nginx/         # 前端 Nginx 配置
│  └─ src/           # Dockerfile 等构建所需上下文
├─ backend/
│  └─ com.busgallery/
│     └─ busgallery/
│        ├─ BusGalleryApplication.java          # Spring Boot 入口
│        ├─ config/                             # Swagger、MinIO、CORS、Web MVC配置
│        ├─ controller/                         # Region / Company / Brand / Model / Vehicle / Image / Upload 控制器
│        ├─ dto/                                # 请求/响应 DTO
│        ├─ entity/                             # JPA / MyBatis 实体
│        ├─ repository/                         # Spring Data Repository
│        ├─ service/                            # Service 接口与实现（含 Storage 子模块）
│        ├─ mapper/                             # MapStruct / MyBatis Mapper
│        ├─ exception/                          # 全局异常处理
│        └─ util/                               # 常用工具类
├─ bus-gallery-frontend/
│  ├─ package.json
│  ├─ vite.config.js
│  ├─ .env.development / .env.production
│  └─ src/                                      # Vue 3 应用（视图、组件、API 封装、状态管理等）
└─ docker-compose.yml
```

---

## 运行要求

- Docker 20+
- Docker Compose v2+（或 `docker compose` CLI）
- 若需源码开发：JDK 17、Node.js 18、pnpm/npm 等

---

## 快速启动（推荐）

1. **克隆仓库**

```bash
   git clone https://github.com/ldrwhc/bus_gallery.git
   cd bus_gallery/docker
```

2. **（可选）检查/修改配置**

   - 修改 `docker-compose.yml` 中的数据库、MinIO帐号或端口映射。
   - 如需自定义前端访问域名，可调整 `docker/nginx` 下配置。

3. **一键启动**

```bash
docker compose up -d
```

4. **访问**
   - 前端：`http://localhost/`
   - 后端 API：`http://localhost:8080`（示例：`/api/vehicles`）
   - MySQL：`localhost:13306`（root / 123456）
   - MinIO 控制台：`http://localhost:9001`（admin / 12345678）
   
5. **停止并清理**

```bash
docker compose down
# 如果需要清空数据卷：
docker compose down -v
```

---

## 服务说明（docker-compose）

| 服务       | 说明                              | 关键环境变量/端口 |
|------------|-----------------------------------|-------------------|
| `mysql`    | 车辆库数据存储（初始化 init.sql）  | `13306:3306`，root/123456 |
| `minio`    | 图片对象存储                      | `9000` / `9001`，admin/12345678 |
| `backend`  | Spring Boot 后端 API              | `8080:8080`，使用上面 MySQL/MinIO |
| `frontend` | Vue 3 前端（Nginx 托管打包结果）  | `80:80` |

---

## 开发模式（可选）

### 后端本地运行

```bash
cd backend
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev
```

确保本地 MySQL / MinIO 配置与 `application-dev.yml` 一致，或通过 `SPRING_PROFILES_ACTIVE`、`DB_URL` 等环境变量覆盖。

### 前端本地运行

```bash
cd bus-gallery-frontend
npm install   # 或 pnpm install / yarn
npm run dev   # 默认 http://localhost:5173
```

如需连接 Docker Compose 中的后端，请在 `.env.development` 中将 `VITE_API_BASE_URL` 指向 `http://localhost:8080`.

---

## 贡献指南

1. Fork 本项目，创建新分支（命名建议 `feature/xxx` 或 `fix/xxx`）。
2. 提交前运行对应的单元测试 / lint。
3. 提交 PR 时请说明变更内容与动机。

欢迎提交 Issue 或 PR 反馈问题、贡献功能！

---

## 许可证

本项目采用 [MIT License](./LICENSE)（如无 LICENSE 文件可自行选择合适的开源许可）。使用前请阅读协议条款。

---
