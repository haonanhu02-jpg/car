# 万盛股份 · 车辆管理系统

PC 端后台管理系统 —— 车辆台账、保险/年检管理、到期提醒、统计报表、权限管理。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Element Plus + Vite + Pinia + Vue Router |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JWT + Spring Security |
| 数据库 | H2（Docker 内置，零配置即可运行；生产可切换 MySQL） |

## 目录结构

```
vehicle-management/
├── server/                 # Spring Boot 后端
├── client/                 # Vue3 前端
├── docs/                   # 团队开发规范
├── docker-compose.yml      # 一键编排后端 + 前端
└── server.py               # 本地启动脚本（非 Docker）
```

---

## 方式一：Docker 一键启动（推荐，无需本地安装任何环境）

> 前提：本机已安装 [Docker](https://www.docker.com/products/docker-desktop/) 与 Docker Compose。

```bash
# 在项目根目录执行
docker compose up -d --build
```

启动完成后访问：

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| 后端 API | http://localhost:8080/api |
| API 文档 | http://localhost:8080/doc.html |
| H2 控制台 | http://localhost:8080/h2-console |

**默认账号：**
- 管理员：`admin` / `admin123`
- 只读用户：`viewer` / `viewer123`

停止与清理：
```bash
docker compose down            # 停止容器
docker compose down -v         # 停止并删除数据卷
```

> 数据持久化：H2 数据库文件保存在 Docker 卷 `backend-data` 中，重启容器数据不丢失。

---

## 方式二：本地启动（需自行安装 JDK 20 + Node 20 + Maven）

后端：
```bash
cd server && mvn spring-boot:run
```

前端（另开终端）：
```bash
cd client && npm install && npm run dev
```

前端开发地址：http://localhost:5173

---

## 切换到 MySQL（生产环境）

1. 修改 `server/src/main/resources/application.yml` 的 `datasource` 为 MySQL 连接
2. 将 `db/schema.sql`、`db/data.sql` 调整为 MySQL 语法
3. 在 `docker-compose.yml` 中新增 `mysql` 服务并让 `backend` 依赖它
