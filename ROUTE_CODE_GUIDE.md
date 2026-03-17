# 路由接口业务链路解读

本文按照前端路由与后端接口对应关系，梳理“请求从哪里来、到哪里去、落在哪个 Service/Mapper”。适合快速上手与面试复盘。

---

## 1. 车辆图库 `/gallery`

- 前端路由：`frontend/src/router/index.js`
- 页面：`frontend/src/views/Gallery.vue`
- Vuex：`frontend/src/store/modules/vehicles.js`（`loadVehicleGallery`）
- API：`frontend/src/api/vehicles.js` -> `GET /api/vehicles`
- 后端：`VehicleController.page()`
- Service：`VehicleService.queryPage()`
- Mapper：`VehicleMapper.selectPageByCursor()`
- 缓存：`VehicleController` Redis 缓存 + 版本号

**链路**：
`Gallery.vue` → `vehicles.loadVehicleGallery` → `/api/vehicles` → `VehicleController.page` → `VehicleServiceImpl.queryPage` → `VehicleMapper` → MySQL + MinIO

---

## 2. 车辆详情（弹窗/详情页）

- 前端组件：`frontend/src/components/Gallery/VehicleDetailModal.vue`
- 详情页：`frontend/src/views/VehicleDetail.vue`
- 快照 API：`frontend/src/api/snapshots.js` -> `/api/snapshots/plate/{plate}`
- 后端：`SnapshotController.byPlate()`
- Service：`SnapshotServiceImpl.getSnapshotByPlate()`
- 数据源：`VehicleService.listByPlateNumber()` + `ImageService.listByVehicle()` + 评论/收藏

**链路**：
`VehicleCard` → `loadVehicleDetail` → `/api/snapshots/plate/{plate}` → Redis big key → `SnapshotServiceImpl.buildSnapshot` → MySQL/MinIO

---

## 3. 登录 / 注册

- 前端：`frontend/src/views/Login.vue` / `Register.vue`
- API：`/api/auth/login`、`/api/auth/register`
- 后端：`AuthController`
- Service：`AuthServiceImpl`
- Session：`UserSessionService`（Redis）

**链路**：
`Login.vue` → `AuthController.login` → `AuthService` → MySQL + Redis Session

---

## 4. 评论

- 前端：`VehicleDetailModal.vue` / `VehicleDetail.vue`
- API：`/api/vehicles/{id}/comments`
- 后端：`CommentController`
- Service：`VehicleCommentService`
- Mapper：`VehicleCommentMapper`

---

## 5. 收藏

- 前端：`VehicleDetailModal.vue` / `VehicleDetail.vue`
- API：`/api/favorites/{id}/toggle`、`/api/favorites/{id}/summary`
- 后端：`FavoriteController`
- Service：`FavoriteServiceImpl`
- Mapper：`FavoriteMapper`

---

## 6. 上传

- 前端：`frontend/src/views/Upload.vue`
- API：`POST /api/upload`（multipart + Idempotency-Key）
- 后端：`UploadController.uploadVehicle`
- Service：`IdempotencyService` → `ImageService` → `VehicleService`
- 存储：MinIO + MySQL

---

## 7. 目录聚合（Catalog）

- 前端：`RegionCatalog.vue` / `CompanyCatalog.vue` / `ModelCatalog.vue`
- API：`/api/catalog/regions`、`/api/catalog/companies`、`/api/catalog/brands`、`/api/catalog/models`
- 后端：`CatalogController`
- Service：`RegionService`、`CompanyService`、`ModelService`、`BrandService`

---

## 8. 公司 / 品牌 / 车型 / 地区 CRUD

- 品牌：`BrandController` → `BrandService` → `BrandMapper`
- 公司：`CompanyController` → `CompanyService` → `CompanyMapper`
- 车型：`ModelController` → `ModelService` → `ModelMapper`
- 地区：`RegionController` → `RegionService` → `RegionMapper`

---

## 9. 图片

- 前端：上传页 / 详情页
- API：`/api/images/*`
- 后端：`ImageController`
- Service：`ImageServiceImpl`
- 存储：MinIO + MySQL

---

## 10. 用户中心

- 前端：`UserProfile.vue`
- API：`/api/users/me`、`/api/users/{id}`、`/api/users/{id}/images`
- 后端：`UserController` → `UserService`

---

> 若需扩展新功能，按 “前端路由 → API → Controller → Service → Mapper/DB/Redis/MinIO” 的思路定位即可。
