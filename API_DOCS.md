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
      "url": "/api/images/access/<display-token>",
      "thumbnailUrl": "/api/images/access/<thumb-token>"
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
          "url": "/api/images/access/<display-token>",
          "thumbnailUrl": "/api/images/access/<thumb-token>"
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

组件与调用位置（前端）：
- `frontend/src/views/VehicleDetail.vue`
- `frontend/src/components/Gallery/VehicleDetailModal.vue`
- 电脑端默认显示在详情右侧；移动端显示在详情下方。

组件内触发细节：
- 详情页与弹窗都支持发布和删除评论，权限按钮由 `canDeleteComment` 控制（本人或站长可见）。
- 评论列表在前端有本地缓存（Vuex `commentCache`）和轮询刷新（8s），先回显后校准。
- 删除成功后前端先本地移除，再由下一次轮询与后端总数对齐。

### GET `/api/vehicles/{vehicleId}/comments`

响应：
```json
{
  "records": [
    {
      "id": 1,
      "vehicleId": 3,
      "userId": 9,
      "username": "uploader_a",
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
{
  "id": 1,
  "vehicleId": 3,
  "userId": 9,
  "username": "uploader_a",
  "displayName": "ldr",
  "content": "好车！",
  "createdAt": "2026-03-17T08:45:45"
}
```

### DELETE `/api/vehicles/{vehicleId}/comments/{commentId}`

说明：
- 需要登录（`@RequireLogin`）。
- 允许删除的角色/用户：
- 评论作者本人。
- 站长（`STATION`）。
- 其他用户删除将返回权限错误（`A0401` 或业务错误码，具体取决于全局异常映射）。

响应：
- `204 No Content`（或空 body 200，按网关/框架设置）

写入后行为（后端）：
- MySQL：删除 `vehicle_comment` 记录。
- Redis：`INCR bg:comments:ver:{vehicleId}`，使 `list/count` 旧缓存自然失效。
- MQ：当前删除评论不发事件（如后续需要可新增 `comment.deleted` 事件用于审计/推荐回滚）。

发布评论主链路时序（同步）：
1. `CommentController` 调用 `VehicleCommentServiceImpl.addComment()`。
2. 事务内写 MySQL 评论记录。
3. 同步 `INCR bg:comments:ver:{vehicleId}`。
4. 返回最新评论 DTO 给前端。

发布评论副作用时序（异步）：
1. `BusEventPublisher` 在事务 `afterCommit` 后发布 `comment.created`。
2. 消息进入 `busgallery.comment.created.queue`（路由键：`comment.created`）。
3. `CommentCreatedEventConsumer` 调用 `CommentSideEffectService` 执行副作用。

评论发布异步副作用（`comment.created`）：
- 通知（日志占位，便于后续接短信/站内信）。
- 敏感词复审（写 `bg:comment:moderation:flag:{commentId}`）。
- 热度统计（`bg:vehicle:heat:engagement`）。
- 推荐信号（`bg:vehicle:recommend:comment-signal:{vehicleId}`）。
- 快照预热（按车牌触发 SnapshotService）。
- 失败策略：best-effort，异常仅告警不反抛，不影响主请求成功返回。

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

组件内触发细节：
- 详情页/弹窗收藏按钮都会先更新本地态，再调用 `/summary` 校准最终态。
- 从个人收藏页点开详情会强制刷新详情数据，避免旧快照导致 `liked` 错判。

收藏状态一致性说明：
- 前端在详情/弹窗会优先展示缓存态，再主动请求 `/summary` 刷新最终态。
- 当用户从“个人收藏”进入详情时，前端会强制刷新详情，避免旧快照导致 `liked` 显示错误。

收藏切换主链路时序（同步）：
1. `FavoriteServiceImpl.toggle()` 在事务内执行 insert/delete。
2. 实时计算 `total/topUsers`。
3. 覆盖写 `bg:fav:summary:{vehicleId}` 和 `bg:fav:liked:{vehicleId}:{userId}`。
4. 直接返回最终态 `FavoriteSummary`。

收藏切换副作用时序（异步）：
1. 事务提交后发布 `favorite.toggled`。
2. 消息进入 `busgallery.favorite.toggled.queue`（路由键：`favorite.toggled`）。
3. 消费器执行榜单聚合、推荐更新、通知。

收藏切换异步副作用（`favorite.toggled`）：
- 榜单/热度聚合（`bg:vehicle:heat:favorites`）。
- 推荐信号更新（`bg:vehicle:recommend:favorite-signal:{vehicleId}`）。
- 通知（日志占位）。
- 失败策略：best-effort，异常仅告警不反抛，避免 Rabbit listener 重试耗尽影响可用性。

---

## 8. 图片 Images

字段语义（当前版本）：
- `url`：受控高清图（压缩 + 强水印），用于车辆详情页与审核页。
- `thumbnailUrl`：缩略图，用于普通浏览页和列表卡片。
- 两者都必须通过签名访问接口读取，不是长期公开直链。

### GET `/api/images/{id}`

响应：
```json
{
  "id": 101,
  "url": "/api/images/access/<display-token>",
  "thumbnailUrl": "/api/images/access/<thumb-token>"
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

## 13. 后台 Admin（评论管理）

> 需要站长权限（`STATION`）+ 登录态。

### GET `/api/admin/comments?page=1&size=20`

响应：
```json
{
  "records": [
    {
      "id": 1001,
      "vehicleId": 18,
      "userId": 9,
      "username": "user@example.com",
      "displayName": "昵称",
      "content": "这车坐起来很稳",
      "createdAt": "2026-03-25T20:55:10"
    }
  ],
  "total": 128,
  "page": 1,
  "size": 20
}
```

说明：
- 排序规则：`created_at DESC, id DESC`（最新评论优先）。
- 参数保护：`page>=1`、`size>=1`，越界会被后端归一化。

### DELETE `/api/admin/comments/{commentId}`

说明：
- 站长删除任意评论。
- 内部复用评论服务删除逻辑，会同步触发评论缓存版本失效（后续读取自动回源重建）。

响应：`204 No Content`（或空 body 200）

实现细节：
- `AdminController.deleteComment()` 先查评论是否存在，再复用 `VehicleCommentService.deleteComment(...)`。
- 这样前台/后台共享一套删除逻辑：权限模型统一、缓存版本失效统一、后续审计扩展点统一。
- 后台删除后同样触发 `bg:comments:ver:{vehicleId}` 递增，详情页评论列表会在下一次读取时自动切换版本。

---

## 14. Swagger

- 在线：`http://localhost:8080/swagger-ui/index.html`
- 静态导出：`docs/swagger/`
