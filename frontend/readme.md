# Bus Gallery Frontend

前端基于 Vue 3 + Vite + Vuex + Element Plus，负责车辆图库、筛选、详情弹窗、评论与收藏、上传与个人中心等页面展示。

---

## 技术栈

- Vue 3（Composition API）
- Vite
- Vue Router 4
- Vuex 4
- Element Plus
- Axios
- SCSS

---

## 目录结构

```
frontend/
├─ src/
│  ├─ api/          # axios 封装与接口
│  ├─ components/   # UI 组件
│  ├─ views/        # 页面
│  ├─ store/        # Vuex 模块
│  ├─ router/       # 路由
│  └─ utils/        # 工具
```

---

## 运行与构建

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

---

## 关键设计点（面试高频）

1. 快照驱动详情渲染
- 车辆详情优先使用 `/api/snapshots/plate/{plate}` 快照
- 快照包含变体、评论、收藏摘要
- 详情渲染仅一次请求，显著降低接口拼装复杂度

2. 车辆变体合并策略
- 前端按 `plateNumber` 去空格归一化聚合
- 变体在卡片与详情页统一展示

3. 收藏按钮去抖 + 最终态同步
- 连续点击仅提交一次请求
- UI 先乐观更新，最终态与服务端对齐

4. 缩略图优先策略
- 列表页优先使用 `thumbnailUrl`
- 详情页再异步加载原图

5. 前端状态管理
- 车辆详情缓存存放于 Vuex `detailMap`
- 收藏摘要写回 `detailMap`，弹窗重复打开时直接读取

---

## 面试追问点（可直接回答）

- 为什么详情用快照：减少 N+1 请求与链路复杂度
- 为什么合并车牌：同牌多变体统一入口，降低用户认知成本
- 缓存如何失效：快照 TTL + 列表版本号失效
- 收藏为何去抖：防止高频点击打爆 DB

---

## 环境变量

- `VITE_API_BASE_URL`：后端 API 地址
  - 留空则默认使用当前域名 + `/api`
  - 示例：`VITE_API_BASE_URL=http://localhost:8080/api`

---

## 典型页面

- 首页：热门快照、入口导航
- 图库：`/gallery` 按筛选组合展示车辆卡片
- 车辆详情：图片轮播、变体切换、评论、收藏
- 上传：车辆与图片信息一次性提交
- 用户中心：我的图片、我的喜爱

---

## API 约定

- 所有接口默认前缀 `/api`
- 登录后使用 `Authorization: Bearer <token>`

完整接口文档见项目根目录：`API_DOCS.md`
