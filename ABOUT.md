# About 模块对照文档

本文件用于和 `frontend/src/views/About.vue` 同步，重点描述“组件 -> 接口 -> 服务 -> Redis/RabbitMQ”的真实执行细节，避免页面说明和代码实现漂移。

---

## 模块2：Web 接口文档（细化版）

### 2.1 评论链路（查询/发布/删除）

前端组件与子区域：
- `frontend/src/views/VehicleDetail.vue`
  - 电脑端：右侧 `comment-column` 默认显示评论区（含发布框和删除按钮）。
  - 移动端：底部 `mobile-comments` 显示评论列表与输入框。
  - 轮询：`COMMENT_POLL_INTERVAL_MS = 8000`，拉取最新评论。
  - 本地缓存：Vuex `commentCache`（30s TTL）先回显，再按需刷新。
- `frontend/src/components/Gallery/VehicleDetailModal.vue`
  - 电脑端：`comment-drawer` 固定在详情右侧。
  - 移动端：`mobile-comments` 在详情内容下方。
  - 与详情页共享同一套评论增删逻辑和权限判断。

接口与后端组件：
- 查询：`GET /api/vehicles/{vehicleId}/comments`
- 发布：`POST /api/vehicles/{vehicleId}/comments`
- 删除：`DELETE /api/vehicles/{vehicleId}/comments/{commentId}`
- Controller：`CommentController`
- Service：`VehicleCommentServiceImpl`
- Mapper：`VehicleCommentMapper`（`created_at DESC, id DESC`）

同步链路时序（发布评论）：
1. 前端 `submitComment()` -> Vuex `addVehicleComment` -> 调用 POST 接口。
2. `VehicleCommentServiceImpl.addComment()` 在事务内写 MySQL `vehicle_comment`。
3. 同步 `INCR bg:comments:ver:{vehicleId}`，让旧分页缓存自然失效。
4. 事务提交后，`BusEventPublisher` 通过 `afterCommit` 发布 `comment.created`。
5. 前端收到成功后先做本地乐观更新，再由轮询/刷新与服务端最终态对齐。

同步链路时序（删除评论）：
1. 前端 `canDeleteComment()` 先做 UI 级判断（本人或 `STATION`）。
2. 调用 DELETE 接口，后端再次强校验权限（防越权）。
3. 删除成功后同样 `INCR bg:comments:ver:{vehicleId}`。
4. 前端从本地列表移除该评论，后续轮询会再次校准总数。

权限边界：
- 评论作者可删本人评论。
- 站长（`STATION`）可删任意评论。
- 非本人且非站长：后端返回未授权错误。

Redis 交互：
- 版本键：`bg:comments:ver:{vehicleId}`
- 列表键：`bg:comments:list:v{ver}:vid{vehicleId}:p{page}:s{size}`
- 计数键：`bg:comments:count:v{ver}:vid{vehicleId}`

RabbitMQ 异步副作用（仅发布评论）：
- 路由键：`comment.created`
- 队列：`busgallery.comment.created.queue`
- 副作用：通知、敏感词复审、热度统计、推荐信号、快照预热
- 失败策略：best-effort，异常记录日志但不反抛，不回滚主事务

并发与性能注意点：
- 热门车辆评论分页会频繁回源：依赖版本键 + 短 TTL 缓存抗压。
- 删除/发布风暴会导致版本快速增长：旧键会自然过期，不做全量扫描删除。
- 轮询过密会放大读流量：当前 8s，后续可升级为长连接或增量拉取。

---

### 2.2 收藏链路（切换与一致性）

前端组件与触发点：
- `VehicleDetail.vue` / `VehicleDetailModal.vue`
  - 收藏按钮触发 `toggleFavorite`。
  - 本地有去抖与请求中保护，避免连点放大写压力。
  - 点击后会再拉 `GET /api/favorites/{vehicleId}/summary` 校准最终态。
- `frontend/src/views/UserProfile.vue`
  - 从收藏夹进入详情时强制 `loadVehicleDetail(force=true)`，避免旧快照导致 `liked` 假阴性。

接口与后端组件：
- `POST /api/favorites/{vehicleId}/toggle`
- `GET /api/favorites/{vehicleId}/summary`
- `GET /api/favorites`
- Service：`FavoriteServiceImpl`

同步链路时序（toggle）：
1. `FavoriteServiceImpl.toggle()` 在事务内 insert/delete `vehicle_favorite`。
2. 实时计算总数和 topUsers，返回 `FavoriteSummary`。
3. 覆盖写 Redis：
   - `bg:fav:summary:{vehicleId}`
   - `bg:fav:liked:{vehicleId}:{userId}`
4. 事务提交后发布 `favorite.toggled`。

RabbitMQ 异步副作用（收藏切换）：
- 路由键：`favorite.toggled`
- 队列：`busgallery.favorite.toggled.queue`
- 副作用：榜单聚合（ZSet）、推荐信号更新、通知
- 失败策略：best-effort，消费者吞掉异常防止重试雪崩

一致性策略：
- 主链路以内存+Redis 返回“最终态”，写后读一致。
- 前端从多个入口打开详情都会主动拉 summary 再覆盖 UI。
- Redis 异常时，接口仍可回源 MySQL 保持主流程可用。

并发与性能注意点：
- 热点车辆高频收藏会争用同一车聚合键：可按需引入批量聚合任务。
- `GET /api/favorites` 当前会组装车辆详情，收藏量极大时建议分页化或快照化。

---

### 2.3 站长后台评论治理

前端组件：
- `frontend/src/views/AdminDashboard.vue` 评论管理区块。
- 支持分页加载、删除确认、删除后自动刷新当前页。

接口：
- `GET /api/admin/comments?page=1&size=20`
- `DELETE /api/admin/comments/{commentId}`

后端组件与复用关系：
- `AdminController.listComments/deleteComment`
- 删除逻辑复用 `VehicleCommentService.deleteComment(...)`
- 复用价值：权限模型、缓存失效策略、后续审计扩展点一致

分页与排序：
- 后台评论列表按 `created_at DESC, id DESC` 返回。
- 建议保留 `created_at` 索引，避免大表分页退化。

---

## 模块3：中间件视角（细化版）

### 3.1 Redis（谁写、何时写、写失败怎么办）

评论相关：
- 写入时机：评论发布/删除后立即 `INCR bg:comments:ver:{vehicleId}`。
- 读取时机：评论列表和计数先读版本，再读对应分页键。
- 失败兜底：Redis 读写异常时直接回源 MySQL，主链路不中断。

收藏相关：
- 写入时机：toggle 事务内计算完 summary 后覆盖写 summary/liked。
- 读取时机：summary 接口优先读缓存，miss 再回源并回填。
- 失败兜底：缓存写失败不影响 toggle 成功返回。

访问量去重相关：
- 键：`bg:vehicle:view:dedupe:{vehicleId}:{fingerprintMd5}`
- TTL：20s
- 作用：同一短时间内重复点击只计一次浏览，避免刷热度。

### 3.2 RabbitMQ（交换机、队列、DLQ、容错）

交换机与队列：
- 业务交换机：`busgallery.events.exchange`（Topic）
- 路由键：
  - `comment.created`
  - `favorite.toggled`
- 业务队列：
  - `busgallery.comment.created.queue`
  - `busgallery.favorite.toggled.queue`
- 死信队列：
  - `busgallery.comment.created.dlq`
  - `busgallery.favorite.toggled.dlq`

发布时机：
- 通过 `BusEventPublisher.publishAfterCommit(...)` 在事务提交后再发消息，避免“数据库回滚但消息已发”的不一致。

消费容错：
- `CommentCreatedEventConsumer` / `FavoriteToggledEventConsumer` 捕获异常并记录日志，不向上抛出。
- 副作用服务内部每个 action 都是 best-effort，单个失败不影响其他 action。

---

## 前端交互布局补充

- 电脑端：
  - 评论区默认显示在详情右侧。
  - 详情与评论之间有灰色竖线分隔。
  - 移除冗余评论展开按钮，减少一次点击路径。
- 移动端：
  - 评论区放在详情下方，输入框固定底部区域。
- 删除按钮显示：
  - 本人评论显示删除按钮。
  - 站长可见全部评论删除按钮。

---

## 变更同步规则

以下任一变更发生时，必须同步更新 `frontend/src/views/About.vue` 与本文件：
- 评论/收藏接口、权限、缓存键或 TTL 策略
- `comment.created` / `favorite.toggled` 事件名、队列名、消费副作用
- 后台评论管理能力（分页、排序、删除行为）
- 详情页评论布局规则（PC/移动端）

---

## 2026-03 Image Policy Update

- Upload now writes three image objects to MinIO: `original`, `display` (watermarked HD), and `thumbnail`.
- `image.url` is used as the controlled HD image for vehicle detail and review pages.
- `image.thumbnailUrl` is used by non-detail browsing pages and list cards.
- All image URLs are short-lived signed URLs via `/api/images/access/{token}`.
- Historical data can be repaired with `ImageDisplayBackfillRunner` using `busgallery.image-display-backfill.*` configs.

## 2026-03 Auth Context Update

- Request context no longer stores full `UserSession` directly.
- `AuthTokenInterceptor` now maps Redis session payload into `AuthPrincipal`.
- `AuthContextHolder` keeps `ThreadLocal<AuthPrincipal>` only.
- Service signatures related to auth/review/upload/submission are migrated from `UserSession` to `AuthPrincipal`.
