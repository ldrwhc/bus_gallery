# Bus Gallery API 文档

本文基于当前后端实现整理接口清单，并补充请求/响应示例，便于联调和面试讲解。

## 1. 基础信息

- Base URL：`/api`
- 认证方式：`Authorization: Bearer <token>` 或 `?token=<token>`
- 统一错误响应：

```json
{
  "code": "A0401",
  "message": "Unauthorized or session expired"
}
```

## 2. 错误码

| code | 含义 |
|------|------|
| 00000 | success |
| A0400 | 参数错误 |
| A0401 | 未登录 / 会话失效 |
| A0404 | 资源不存在 |
| A0429 | 幂等请求重复 |
| A0500 | 业务错误 |
| B0500 | 系统错误 |
| B0501 | 存储服务异常 |

---

## 3. 认证 Auth

### POST `/api/auth/register`

请求：
```json
{
  "username": "user@example.com",
  "displayName": "昵称",
  "password": "******",
  "confirmPassword": "******"
}
```

响应：
```json
{
  "token": "abcd1234",
  "profile": {
    "id": 1,
    "username": "user@example.com",
    "displayName": "昵称"
  }
}
```

### POST `/api/auth/login`

请求：
```json
{
  "username": "user@example.com",
  "password": "******"
}
```

响应：
```json
{
  "token": "abcd1234",
  "profile": {
    "id": 1,
    "username": "user@example.com",
    "displayName": "昵称"
  }
}
```

### POST `/api/auth/logout`

响应：`204 No Content`

---

## 4. 用户 User

### GET `/api/users/me`

响应：
```json
{
  "id": 1,
  "username": "user@example.com",
  "displayName": "昵称",
  "uploads": 12
}
```

### GET `/api/users/me/images`

响应：
```json
{
  "records": [
    {
      "id": 10,
      "vehicleId": 3,
      "url": "http://localhost/bus-gallery/images/xxx.jpg",
      "thumbnailUrl": "http://localhost/bus-gallery/images/xxx_thumb.jpg"
    }
  ],
  "total": 12,
  "page": 1,
  "size": 12
}
```

### GET `/api/users/{userId}`

响应同 `me`

### GET `/api/users/{userId}/images`

响应同 `me/images`

---

## 5. 车辆 Vehicles

### GET `/api/vehicles`

请求参数示例：
```
/api/vehicles?size=12&regionId=1&companyId=3&keyword=京A
```

响应：
```json
{
  "records": [
    {
      "vehicle": {
        "id": 5,
        "plateNumber": "京A 12345",
        "launchDate": "2015-03-01"
      },
      "images": [
        {
          "id": 101,
          "url": "http://localhost/bus-gallery/images/xxx.jpg",
          "thumbnailUrl": "http://localhost/bus-gallery/images/xxx_thumb.jpg"
        }
      ]
    }
  ],
  "total": 120,
  "page": 1,
  "size": 12,
  "nextLaunch": "2015-03-01",
  "nextId": 5
}
```

### GET `/api/vehicles/{id}`

响应：
```json
{
  "vehicle": { "id": 5, "plateNumber": "京A 12345" },
  "vehicleConfig": { "fuelType": "diesel" },
  "images": [ { "id": 101, "url": "..." } ]
}
```

### GET `/api/vehicles/plate/{plateNumber}`

响应：
```json
{
  "plateNumber": "京A12345",
  "variants": [ { "vehicle": { "id": 5 }, "images": [] } ]
}
```

### POST `/api/vehicles`

请求：
```json
{
  "plateNumber": "京A 12345",
  "brandId": 1,
  "modelId": 2,
  "companyId": 3,
  "regionId": 10,
  "launchDate": "2026-03-01",
  "config": { "fuelType": "diesel" },
  "imageIds": [1,2,3]
}
```

响应：车辆详情（同 GET /api/vehicles/{id}）

---

## 6. 评论 Comments

### GET `/api/vehicles/{vehicleId}/comments`

响应：
```json
{
  "records": [
    {
      "id": 1,
      "vehicleId": 3,
      "displayName": "ldr",
      "content": "好车！",
      "createdAt": "2026-03-17T08:45:45"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

### POST `/api/vehicles/{vehicleId}/comments`

请求：
```json
{ "content": "好车！" }
```

响应：
```json
{ "id": 1, "vehicleId": 3, "content": "好车！" }
```

---

## 7. 收藏 Favorites

### POST `/api/favorites/{vehicleId}/toggle`

响应：
```json
{
  "liked": true,
  "total": 1,
  "topUsers": [
    { "id": 1, "username": "user@example.com", "displayName": "昵称", "isSelf": true }
  ]
}
```

### GET `/api/favorites/{vehicleId}/summary`

响应同上

### GET `/api/favorites`

响应：车辆详情列表

---

## 8. 图片 Images

### GET `/api/images/{id}`

响应：
```json
{
  "id": 101,
  "url": "http://localhost/bus-gallery/images/xxx.jpg",
  "thumbnailUrl": "http://localhost/bus-gallery/images/xxx_thumb.jpg"
}
```

### POST `/api/images/upload`

`multipart/form-data`：`file` + `uploadUser`

响应：图片元数据

---

## 9. 上传 Upload

### POST `/api/upload`

`multipart/form-data`：
- `file`
- `payload`（JSON）
- Header: `Idempotency-Key`

响应：车辆详情

---

## 10. 目录 Catalog

### GET `/api/catalog/regions`
### GET `/api/catalog/companies`
### GET `/api/catalog/brands`
### GET `/api/catalog/models`

响应为聚合目录结构（含缩略图/统计信息）

---

## 11. 品牌 / 公司 / 车型 / 地区

详细接口参见：`BrandController` / `CompanyController` / `ModelController` / `RegionController`

---

## 12. 快照 Snapshots

### GET `/api/snapshots/plate/{plateNumber}`

响应：
```json
{
  "plateNumber": "京A12345",
  "version": 1773737660016,
  "variants": [ { "vehicle": { "id": 5 }, "images": [] } ],
  "comments": [],
  "recommendations": [],
  "favoriteSummary": { "liked": false, "total": 0, "topUsers": [] }
}
```

### GET `/api/snapshots/hot`

响应：快照列表

---

## 13. Swagger

- 在线：`http://localhost:8080/swagger-ui/index.html`
- 静态导出：`docs/swagger/`
