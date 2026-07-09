# Stage 0: 打包前端（保留华为云镜像仓库地址）
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/node:18-alpine AS build
WORKDIR /app

# 替换Alpine阿里云源（保留原有配置）
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 配置npm淘宝镜像（保留原有配置）
RUN npm config set registry https://registry.npmmirror.com/

# ========== 关键：基于项目根目录，复制frontend下的package.json ==========
COPY frontend/package*.json ./
# 验证package.json是否存在（调试用）
RUN if [ ! -f "/app/package.json" ]; then echo "ERROR: package.json未找到！"; ls -l /app/; exit 1; fi

# 安装依赖
RUN npm install --force --verbose

# ========== 关键：基于项目根目录，复制frontend下的所有源码 ==========
COPY frontend/ .
# 构建前端
RUN npm run build --verbose

# Stage 1: 用Nginx提供静态文件（保留华为云镜像仓库地址）
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nginx:1.25-alpine
WORKDIR /app

# 替换Alpine阿里云源（保留原有配置）
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# ========== 关键：基于项目根目录，复制docker/nginx下的default.conf ==========
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
# 验证default.conf是否存在（调试用）
RUN if [ ! -f "/etc/nginx/conf.d/default.conf" ]; then echo "ERROR: default.conf未找到！"; ls -l /etc/nginx/conf.d/; exit 1; fi

# 复制前端构建产物
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
EXPOSE 443

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]