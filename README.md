# 万盛股份 · 车辆管理系统

PC 端后台管理系统 —— 车辆台账、保险/年检管理、到期提醒、统计报表、权限管理。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Element Plus + Vite + Pinia + Vue Router |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JWT + Spring Security |
| 数据库 | MySQL 8.0（Docker 默认；开发/联调可切回 H2） |

## 目录结构

```
vehicle-management/
├── server/                 # Spring Boot 后端
├── client/                 # Vue3 前端
├── docs/                   # 团队开发规范
├── docker-compose.yml      # 一键编排 MySQL + 后端 + 前端
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
| MySQL | localhost:3306（可用 Navicat 等客户端连接） |

**默认账号：**
- 管理员：`admin` / `admin123`
- 只读用户：`viewer` / `viewer123`

停止与清理：
```bash
docker compose down            # 停止容器
docker compose down -v         # 停止并删除数据卷（会清空 MySQL 数据，谨慎）
```

> 数据持久化：MySQL 数据保存在 Docker 卷 `mysql-data` 中，重启容器数据不丢失。

---

## 方式二：本地启动（需自行安装 JDK 20 + Node 20 + Maven）

后端（默认 H2 内存库，零配置）：
```bash
cd server && mvn spring-boot:run
```

前端（另开终端）：
```bash
cd client && npm install && npm run dev
```

前端开发地址：http://localhost:5173

---

## 使用 MySQL（Docker 默认已启用）

Docker 编排已包含 `mysql:8.0` 服务，后端通过 `SPRING_PROFILES_ACTIVE=mysql` 激活
`application-mysql.yml` 连接 MySQL，并在启动时自动建表 + 灌入初始数据。

### 用 Navicat 管理 MySQL

1. 新建连接 → MySQL
2. 主机 `localhost`，端口 `3306`，用户 `root`，密码 `wansheng123`
3. 数据库 `vehicle_management` → 连接即可浏览表、写查询、导入 CSV/Excel

> 改密码：同步修改 `docker-compose.yml` 中 `mysql` 的 `MYSQL_ROOT_PASSWORD`
> 与 `backend` 的 `MYSQL_ROOT_PASSWORD` 环境变量，两处保持一致后重新 `docker compose up -d`。

### 切换回 H2（仅本地联调）

把 `docker-compose.yml` 里 `backend` 的 `SPRING_PROFILES_ACTIVE` 改回 `docker` 即可
（对应 `application-docker.yml`，使用文件型 H2）。

---

## 批量导入车辆数据

- `POST /api/vehicles/batch`：JSON 数组批量新增（`{"items":[...]}`）
- `POST /api/vehicles/import`：上传 Excel（`.xlsx`）批量导入，
  表头：`车牌号,车辆类型,品牌,上牌日期,所属,投保公司,险种,保单号,保险到期,年检到期,ETC银行,油卡号,备注`
- 也可直接用 Navicat 的「导入向导」从 CSV/Excel 灌入 `vehicles` 表（适合上万条）

---

## 前端批量导入 / 导出 Excel

后台页面已内置导入/导出能力，无需 Navicat：

- **车辆列表页 → 导入Excel**：弹出对话框，上传 `.xlsx` 即调后端
  `POST /api/vehicles/import` 批量写入（仅管理员）。可点「下载导入模板」获取标准表头。
- **车辆列表页 → 导出Excel**：将当前筛选结果（全量分页拉取）导出为
  `车辆台账_YYYY-MM-DD.xlsx`。
- **报表页 → 导出Excel**：保险到期清单、年检到期清单分别导出为 Excel。

> 模板表头（导入/导出通用）：车牌号、车辆类型(0=小车/1=大巴)、品牌、上牌日期、
> 所属、投保公司、险种、保单号、保险到期、年检到期、ETC银行、油卡号、备注
