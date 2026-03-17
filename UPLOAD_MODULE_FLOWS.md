# 文件上传模块 — 流程图与业务流程

## 目录

- [1. 模块概览](#1-模块概览)
- [2. 系统架构图](#2-系统架构图)
- [3. 主上传流程（当前活跃路径）](#3-主上传流程当前活跃路径)
- [4. 前端交互流程](#4-前端交互流程)
- [5. 后端处理流程](#5-后端处理流程)
- [6. 图片存储流程](#6-图片存储流程)
- [7. 车辆建档流程](#7-车辆建档流程)
- [8. 认证鉴权流程](#8-认证鉴权流程)
- [9. 数据校验流程](#9-数据校验流程)
- [10. 异常处理流程](#10-异常处理流程)
- [11. 旧版两步上传流程（Legacy）](#11-旧版两步上传流程legacy)
- [12. 关键文件索引](#12-关键文件索引)
- [13. 配置参数一览](#13-配置参数一览)

---

## 1. 模块概览

文件上传模块是"公交车辆图库"系统的核心功能，允许已认证用户上传公交车辆照片并同时填写车辆档案信息。系统采用**单次 multipart 请求**将图片文件与车辆元数据一并提交，后端完成图片存储（MinIO）、EXIF 提取、车辆建档（MySQL）等一系列操作。

```
技术栈：
├── 前端：Vue 3 + Element Plus + Axios
├── 后端：Spring Boot 3.2.5 + MyBatis + JPA
├── 存储：MinIO（对象存储）
├── 缓存：Redis（会话管理）
├── 数据库：MySQL 8
└── 网关：Nginx（反向代理）
```

---

## 2. 系统架构图

```mermaid
graph TB
    subgraph 浏览器
        A[用户浏览器]
    end

    subgraph Nginx网关
        B[Nginx<br/>client_max_body_size: 50MB]
    end

    subgraph 后端服务
        C[Spring Boot<br/>UploadController]
        D[ImageService]
        E[VehicleService]
        F[AuthTokenInterceptor]
    end

    subgraph 数据层
        G[(MySQL<br/>image / vehicle / vehicle_image)]
        H[(Redis<br/>用户会话)]
        I[(MinIO<br/>bus-gallery bucket)]
    end

    A -->|POST /api/upload<br/>multipart/form-data| B
    B -->|代理转发| C
    C --> F
    F -->|验证Token| H
    C --> D
    C --> E
    D -->|存储图片| I
    D -->|写入图片记录| G
    E -->|写入车辆记录| G
    B -->|GET /bus-gallery/*<br/>图片访问| I
```

---

## 3. 主上传流程（当前活跃路径）

完整的端到端上传流程，从用户操作到数据持久化：

```mermaid
flowchart TD
    Start([用户进入上传页面]) --> AuthCheck{已登录?}
    AuthCheck -->|否| Redirect[重定向到登录页]
    AuthCheck -->|是| ShowForm[展示上传表单]

    ShowForm --> SelectFile[选择/拖拽图片文件]
    SelectFile --> FillForm[填写车辆信息<br/>车牌号、型号、公司、地区等]
    FillForm --> ClickSubmit[点击提交]

    ClickSubmit --> FrontValidate{前端校验}
    FrontValidate -->|失败| ShowError1[显示错误提示]
    ShowError1 --> FillForm

    FrontValidate -->|通过| BuildFormData[构建 FormData<br/>file + payload JSON Blob]
    BuildFormData --> InjectToken[Axios 注入 Bearer Token]
    InjectToken --> SendRequest[POST /api/upload<br/>multipart/form-data]

    SendRequest --> NginxProxy[Nginx 代理转发]
    NginxProxy --> AuthIntercept{认证拦截器<br/>Token 有效?}
    AuthIntercept -->|无效| Return401[返回 401 未授权]
    Return401 --> ShowError2[前端显示错误]

    AuthIntercept -->|有效| BackValidate{后端参数校验}
    BackValidate -->|失败| ReturnError[返回错误信息]
    ReturnError --> ShowError2

    BackValidate -->|通过| UploadImage[上传图片到 MinIO]
    UploadImage --> ExtractExif[提取 EXIF 元数据]
    ExtractExif --> SaveImage[保存图片记录到 MySQL]
    SaveImage --> CreateVehicle[创建车辆档案<br/>自动创建品牌/型号/公司/地区]
    CreateVehicle --> LinkImage[关联图片与车辆<br/>vehicle_image 表]
    LinkImage --> ReturnDetail[返回车辆详情 JSON]

    ReturnDetail --> ShowSuccess[显示"上传成功，车辆已建档"]
    ShowSuccess --> ResetForm[重置表单]
    ShowSuccess --> EmitEvent[触发 uploaded 事件]

    UploadImage -->|失败| ThrowEx[抛出异常<br/>图片上传失败]
    ThrowEx --> GlobalHandler[全局异常处理器]
    GlobalHandler --> ShowError2

    style Start fill:#e1f5fe
    style ShowSuccess fill:#c8e6c9
    style Return401 fill:#ffcdd2
    style ReturnError fill:#ffcdd2
    style ThrowEx fill:#ffcdd2
```

---

## 4. 前端交互流程

```mermaid
flowchart TD
    subgraph 路由层
        R1[router/index.js] -->|/upload| R2{requiresAuth}
        R2 -->|未登录| R3[重定向 /login]
        R2 -->|已登录| R4[加载 Upload.vue]
    end

    subgraph 视图层
        R4 --> V1[Upload.vue<br/>页面容器]
        V1 --> V2[ImageUpload.vue<br/>上传组件]
    end

    subgraph 组件交互
        V2 --> U1[el-upload 组件<br/>拖拽/点击选择文件]
        V2 --> U2[表单字段<br/>车牌号/型号/公司/地区/...]
        V2 --> U3[自动补全下拉<br/>品牌/型号/公司/地区]
        U3 -->|远程搜索| API1[调用搜索 API]
    end

    subgraph 提交流程
        U1 --> S1[handleFileChange<br/>暂存文件引用]
        U2 --> S2[收集表单数据]
        S1 --> S3[handleSubmit]
        S2 --> S3
        S3 --> S4{前端校验<br/>文件+必填字段}
        S4 -->|失败| S5[ElMessage.warning]
        S4 -->|通过| S6[uploadVehicleWithImage<br/>api/vehicles.js]
        S6 --> S7[构建 FormData]
        S7 --> S8[axiosInstance.post<br/>/api/upload]
    end

    subgraph 响应处理
        S8 -->|成功| H1[ElMessage.success<br/>上传成功，车辆已建档]
        H1 --> H2[emit uploaded 事件]
        H1 --> H3[resetForm 重置表单]
        S8 -->|失败| H4[ElMessage.error<br/>上传失败，请稍后重试]
    end
```

---

## 5. 后端处理流程

```mermaid
flowchart TD
    subgraph UploadController
        A1[接收 multipart 请求<br/>@RequestPart file<br/>@RequestPart payload] --> A2[payload.validate]
        A2 -->|校验失败| A3[抛出 IllegalArgumentException]
        A2 -->|校验通过| A4[构建 Image 元数据<br/>设置上传者信息]
        A4 --> A5[调用 imageService.uploadAndSave]
    end

    subgraph ImageServiceImpl
        A5 --> B1[校验文件非空]
        B1 --> B2[读取文件字节 byte数组]
        B2 --> B3[ExifExtractor.extract<br/>提取 EXIF 信息]
        B3 --> B4[ExifUtils.toJson<br/>序列化 EXIF 为 JSON]
        B4 --> B5[buildObjectName<br/>images/YYYYMMDD/uuid.ext]
        B5 --> B6[storageService.upload<br/>上传到 MinIO]
        B6 --> B7[设置 Image 实体属性<br/>url/objectName/size/mime/exif]
        B7 --> B8[imageMapper.insert<br/>写入数据库]
        B8 --> B9[返回 Image 实体]
    end

    subgraph 回到 UploadController
        B9 --> C1[转换 payload 为<br/>Vehicle + VehicleConfig]
        C1 --> C2[调用 vehicleService.create]
    end

    subgraph VehicleServiceImpl
        C2 --> D1[ensureRegionExists<br/>查找或创建地区]
        D1 --> D2[ensureModelExists<br/>查找或创建品牌+型号]
        D2 --> D3[ensureCompanyExists<br/>查找或创建公司]
        D3 --> D4[vehicleMapper.insert<br/>插入车辆记录]
        D4 --> D5[vehicleConfigMapper.upsert<br/>插入车辆配置]
        D5 --> D6[saveVehicleImages<br/>关联图片到车辆]
        D6 --> D7[返回 Vehicle]
    end

    subgraph 组装响应
        D7 --> E1[查询完整车辆详情]
        E1 --> E2[assembleDetail<br/>组装 VehicleDetailResponse]
        E2 --> E3[返回 JSON 响应]
    end

    A3 --> F1[GlobalExceptionHandler<br/>返回错误响应]
```

---

## 6. 图片存储流程

```mermaid
flowchart LR
    subgraph 文件处理
        A[MultipartFile] --> B[读取 byte 数组]
        B --> C[提取 EXIF]
        B --> D[生成对名<br/>images/YYYYMMDD/uuid.ext]
    end

    subgraph MinIO 存储
        D --> E[MinioStorageService.upload]
        E --> F[确保 bucket 存在<br/>bus-gallery]
        F --> G[PutObjectArgs<br/>上传对象]
        G --> H[构建访问 URL]
    end

    subgraph URL 生成策略
        H --> I{cdnHost 已配置?}
        I -->|是| J[cdnHost/objectName<br/>例: http://localhost/bus-gallery/images/20260316/xxx.jpg]
        I -->|否| K[endpoint/bucket/objectName<br/>例: http://minio:9000/bus-gallery/images/20260316/xxx.jpg]
    end

    subgraph 数据持久化
        J --> L[StorageObject<br/>objectName + url + thumbnailUrl]
        K --> L
        L --> M[Image 实体写入 MySQL]
    end
```

```mermaid
graph LR
    subgraph MinIO Bucket: bus-gallery
        direction TB
        R[images/] --> D1[20260101/]
        R --> D2[20260316/]
        D1 --> F1[a1b2c3d4.jpg]
        D1 --> F2[e5f6g7h8.png]
        D2 --> F3[i9j0k1l2.webp]
    end
```

---

## 7. 车辆建档流程

```mermaid
flowchart TD
    A[接收车辆信息 + 图片ID] --> B{地区已存在?}
    B -->|是| B1[使用已有 regionId]
    B -->|否| B2[创建省份+城市记录]
    B2 --> B1

    B1 --> C{品牌已存在?}
    C -->|是| C1[使用已有 brandId]
    C -->|否| C2[创建品牌记录]
    C2 --> C1

    C1 --> D{型号已存在?}
    D -->|是| D1[使用已有 modelId]
    D -->|否| D2[创建型号记录<br/>关联品牌]
    D2 --> D1

    D1 --> E{公司已存在?}
    E -->|是| E1[使用已有 companyId]
    E -->|否| E2[创建公司记录<br/>关联地区]
    E2 --> E1

    E1 --> F[插入 vehicle 记录<br/>车牌号/型号/公司/地区/日期等]
    F --> G[插入/更新 vehicle_config<br/>燃料类型/发动机/电机/悬挂等]
    G --> H[插入 vehicle_image 关联<br/>绑定图片到车辆]
    H --> I[返回完整车辆对象]

    style B2 fill:#fff9c4
    style C2 fill:#fff9c4
    style D2 fill:#fff9c4
    style E2 fill:#fff9c4
```

---

## 8. 认证鉴权流程

```mermaid
sequenceDiagram
    participant 浏览器
    participant Axios拦截器
    participant Nginx
    participant AuthInterceptor
    participant Redis
    participant UploadController

    浏览器->>Axios拦截器: 发起上传请求
    Axios拦截器->>Axios拦截器: 从 localStorage 读取 token
    Axios拦截器->>Nginx: POST /api/upload<br/>Authorization: Bearer {token}

    Nginx->>AuthInterceptor: 转发请求

    AuthInterceptor->>AuthInterceptor: 提取 Authorization 头
    alt Token 缺失
        AuthInterceptor-->>浏览器: 401 未授权
    end

    AuthInterceptor->>Redis: 查询 session:{token}
    alt Session 不存在或过期
        AuthInterceptor-->>浏览器: 401 会话已过期
    end

    Redis-->>AuthInterceptor: 返回 UserSession
    AuthInterceptor->>AuthInterceptor: 检查 @RequireLogin 注解
    AuthInterceptor->>UploadController: 放行请求<br/>注入用户信息到 request attribute
    UploadController-->>浏览器: 处理结果
```

---

## 9. 数据校验流程

```mermaid
flowchart TD
    subgraph 前端校验 — ImageUpload.vue
        FA{已登录?} -->|否| FA1[提示请先登录]
        FA -->|是| FB{文件已选择?}
        FB -->|否| FB1[提示请选择图片]
        FB -->|是| FC{车牌号非空?}
        FC -->|否| FC1[提示请填写车牌号]
        FC -->|是| FD{型号已填?}
        FD -->|否| FD1[提示请选择型号]
        FD -->|是| FE{公司已填?}
        FE -->|否| FE1[提示请选择公司]
        FE -->|是| FF{地区已填?}
        FF -->|否| FF1[提示请选择地区]
        FF -->|是| FG[校验通过 ✓<br/>发送请求]
    end

    subgraph Nginx 层
        FG --> NA{请求体 ≤ 50MB?}
        NA -->|否| NA1[413 Request Entity Too Large]
        NA -->|是| NB[转发到后端]
    end

    subgraph Spring Boot 层
        NB --> SA{文件 ≤ 10MB?}
        SA -->|否| SA1[MaxUploadSizeExceededException]
        SA -->|是| SB[进入 Controller]
    end

    subgraph UploadController.validate
        SB --> BA{plateNumber 非空?}
        BA -->|否| BA1[IllegalArgumentException]
        BA -->|是| BB{modelId 或 modelName?}
        BB -->|否| BB1[IllegalArgumentException]
        BB -->|是| BC{companyId 或 companyName?}
        BC -->|否| BC1[IllegalArgumentException]
        BC -->|是| BD{regionId 或 regionCity?}
        BD -->|否| BD1[IllegalArgumentException]
        BD -->|是| BE[校验通过 ✓<br/>进入业务逻辑]
    end

    subgraph ImageServiceImpl
        BE --> CA{文件非 null 且非空?}
        CA -->|否| CA1[RuntimeException]
        CA -->|是| CB[校验通过 ✓<br/>开始处理]
    end
```

---

## 10. 异常处理流程

```mermaid
flowchart TD
    subgraph 异常来源
        E1[文件校验失败<br/>ImageServiceImpl]
        E2[参数校验失败<br/>UploadController]
        E3[MinIO 上传失败<br/>MinioStorageService]
        E4[数据库操作失败<br/>MyBatis]
        E5[认证失败<br/>AuthInterceptor]
        E6[文件过大<br/>Spring Multipart]
    end

    subgraph 异常类型
        E1 --> T1[RuntimeException<br/>上传文件至 MinIO 失败]
        E2 --> T2[IllegalArgumentException]
        E3 --> T1
        E4 --> T3[DataAccessException]
        E5 --> T4[BizException<br/>UNAUTHORIZED]
        E6 --> T5[MaxUploadSizeExceededException]
    end

    subgraph GlobalExceptionHandler
        T1 --> G1[handleException<br/>返回 code + message]
        T2 --> G1
        T3 --> G1
        T4 --> G2[handleBizException<br/>返回业务错误码]
        T5 --> G1
    end

    subgraph 事务回滚
        E3 -.->|@Transactional| R1[ImageServiceImpl<br/>DB 记录回滚]
        E4 -.->|@Transactional| R2[VehicleServiceImpl<br/>DB 记录回滚]
    end

    subgraph 前端处理
        G1 --> F1[Axios 拦截器<br/>提取 error.message]
        G2 --> F1
        F1 --> F2[ElMessage.error<br/>显示错误信息]
    end

    subgraph ⚠️ 已知问题
        R1 -.-> W1[MinIO 对象已上传<br/>但 DB 回滚后成为孤儿文件]
    end

    style W1 fill:#fff3e0,stroke:#ff9800
```

---

## 11. 旧版两步上传流程（Legacy）

系统中保留了两个旧版上传组件，采用两步分离式上传：

```mermaid
sequenceDiagram
    participant 用户
    participant ImageUploader.vue
    participant ImageController
    participant VehicleController
    participant MinIO
    participant MySQL

    用户->>ImageUploader.vue: 选择图片 + 填写信息

    Note over ImageUploader.vue: 第一步：上传图片
    ImageUploader.vue->>ImageController: POST /api/images/upload<br/>multipart: file + uploadUser
    ImageController->>MinIO: 存储图片
    ImageController->>MySQL: 保存 Image 记录
    ImageController-->>ImageUploader.vue: 返回 Image (含 id)

    Note over ImageUploader.vue: 第二步：创建车辆
    ImageUploader.vue->>VehicleController: POST /api/vehicles<br/>JSON: 车辆信息 + imageIds
    VehicleController->>MySQL: 创建车辆 + 关联图片
    VehicleController-->>ImageUploader.vue: 返回车辆详情

    ImageUploader.vue->>用户: 显示上传成功
```

| 版本 | 文件 | 状态 |
|------|------|------|
| v1 (最早) | `components/ImageUploader.txt` | 已归档，使用 el-autocomplete + el-upload |
| v2 | `components/ImageUploader.vue` | 保留，自定义下拉组件 |
| v3 (当前) | `components/Upload/ImageUpload.vue` | **活跃使用**，单请求上传 |

---

## 12. 关键文件索引

### 前端

| 文件 | 职责 |
|------|------|
| `frontend/src/views/Upload.vue` | 上传页面路由视图 |
| `frontend/src/components/Upload/ImageUpload.vue` | 主上传组件（表单 + 拖拽上传） |
| `frontend/src/api/vehicles.js` | `uploadVehicleWithImage()` — 构建 FormData 并发送 |
| `frontend/src/api/images.js` | `uploadImage()` — 旧版图片上传 API |
| `frontend/src/api/axiosInstance.js` | Axios 实例，Token 注入 + 响应拦截 |
| `frontend/src/router/index.js` | 路由定义，`/upload` 需要认证 |

### 后端

| 文件 | 职责 |
|------|------|
| `controller/UploadController.java` | `POST /api/upload` — 主上传入口 |
| `controller/ImageController.java` | `POST /api/images/upload` — 旧版图片上传 |
| `controller/VehicleController.java` | `POST /api/vehicles` — 旧版车辆创建 |
| `service/impl/ImageServiceImpl.java` | 图片处理核心：EXIF 提取 + MinIO 上传 + DB 持久化 |
| `service/impl/VehicleServiceImpl.java` | 车辆建档：自动创建关联实体 + 事务管理 |
| `service/storage/MinioStorageService.java` | MinIO 对象存储操作 |
| `service/storage/StorageProperties.java` | MinIO 配置属性 |
| `entity/Image.java` | 图片实体 |
| `util/ExifExtractor.java` | EXIF 元数据提取 |
| `util/ExifUtils.java` | EXIF JSON 序列化 |
| `auth/AuthTokenInterceptor.java` | 认证拦截器 |
| `exception/GlobalExceptionHandler.java` | 全局异常处理 |

### 基础设施

| 文件 | 职责 |
|------|------|
| `docker/docker-compose.yml` | 服务编排（MySQL + MinIO + Redis + 应用） |
| `docker/nginx/default.conf` | Nginx 反向代理 + 静态资源 + 上传限制 |
| `backend/src/main/resources/application.yml` | Spring Boot 配置（上传限制 + MinIO + DB） |

---

## 13. 配置参数一览

| 参数 | 值 | 位置 | 说明 |
|------|-----|------|------|
| 前端文件大小提示 | 20 MB | `ImageUpload.vue` | ⚠️ 仅 UI 提示，未强制校验 |
| Spring 最大文件大小 | 10 MB | `application.yml` | 实际生效的后端限制 |
| Spring 最大请求大小 | 10 MB | `application.yml` | 含文件+表单的总请求限制 |
| Nginx 最大请求体 | 50 MB | `nginx/default.conf` | 网关层限制 |
| 允许文件类型 | JPG, PNG, WebP | `ImageUpload.vue` | ⚠️ 仅 UI 提示，后端未校验 |
| 单次上传文件数 | 1 | `ImageUpload.vue` | el-upload `:limit="1"` |
| MinIO Bucket | `bus-gallery` | `application.yml` | 对象存储桶名 |
| 图片路径模式 | `images/YYYYMMDD/<uuid>.<ext>` | `ImageServiceImpl` | 按日期分区存储 |
| 会话有效期 | 86400s (24h) | `application.yml` | Redis 会话 TTL |

> ⚠️ **已知不一致**：前端提示最大 20MB，但后端 Spring 限制为 10MB，可能导致用户上传 10-20MB 文件时收到意外错误。建议统一为相同值。
