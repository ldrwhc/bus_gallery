# Swagger 静态文档

此目录用于存放 Swagger/OpenAPI 的静态导出文件。

## 导出方式

确保后端服务已启动，然后执行：

```bash
# PowerShell
./scripts/export-swagger.ps1

# 或 Bash
bash scripts/export-swagger.sh
```

导出后会生成：

- `docs/swagger/openapi.json`
- `docs/swagger/index.html`

打开 `docs/swagger/index.html` 即可离线查看接口文档（前提是 openapi.json 已生成）。
