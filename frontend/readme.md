# Bus Gallery Frontend

基于 **Vue 3 + Vue CLI + Element Plus + Vuex + axios** 的公交车图库前端，负责展示地区/公司/品牌/车型分类、车辆详情、图片上传等功能。

## 技术栈

- **Vue 3**（Composition API）
- **Vue CLI 5**
- **Element Plus**
- **Vue Router 4**
- **Vuex 4**
- **axios**
- **SCSS / dayjs**

## 目录结构

```
frontend/
├── package.json
├── vue.config.js
├── public/
│   └── index.html
└── src/
    ├── main.js
    ├── App.vue
    ├── api/
    ├── components/
    ├── layout/
    ├── router/
    ├── store/
    ├── styles/
    ├── utils/
    └── views/
```

## 依赖安装

确保已安装 Node.js（建议 16+）。执行：

```bash
cd frontend
npm install
```

> 若使用 `yarn` 或 `pnpm`，可自行调整命令。

## 启动与构建

### 开发模式

```bash
npm run serve
```

默认在 `http://localhost:8081` 启动，同时 `vue.config.js` 将 API 请求代理到 `http://localhost:8080`（Spring Boot 后端），确保后端服务已启动。

### 生产构建

```bash
npm run build
```

编译生成的静态文件位于 `dist/`，可配合 Nginx 或 Docker 部署。

### 代码检查（可选）

```bash
npm run lint
```

## 接口对接

- 后端 API 基于 `/api` 前缀，前端通过 `src/api/*.js` 与 Vuex 模块调用。
- 若后端部署地址变化，可在 `vue.config.js` 中调整 `devServer.proxy`；
- 生产环境下可在部署服务器/Nginx 中配置反向代理到 `/api`。

## 常见问题

1. **跨域问题**：开发模式通过代理解决，生产部署需在 Nginx/网关配置好反向代理。
2. **样式冲突**：Element Plus 样式可在 `src/styles/global.scss` 中统一覆盖。
3. **接口异常**：`src/api/axios.js` 中配置了响应拦截器，会弹出 `ElMessage` 提示。可根据需要调整错误处理。

如需进一步扩展页面或功能，可在 `src/views`、`src/components` 中按业务划分组件，Vuex 模块和 API 封装也可按需新增。