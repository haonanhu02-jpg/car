# 使用 DaoCloud 国内镜像源（速度快）
FROM docker.m.daocloud.io/library/node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 生产环境使用 nginx
FROM docker.m.daocloud.io/library/nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# 复制自定义 nginx 配置（SPA 路由回退 + API 代理）
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]