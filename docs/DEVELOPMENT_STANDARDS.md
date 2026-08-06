# 万盛股份 · 车辆管理系统 — 团队开发规范

> **版本**: v1.0 | **维护者**: Senior Developer | **适用团队**: 1-3人

---

## 一、代码规范

### 1.1 Java 后端规范

#### 命名约定
| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `VehicleController` |
| 方法名 | camelCase | `findExpiringSoon()` |
| 常量 | UPPER_SNAKE | `MAX_PAGE_SIZE` |
| 包名 | 全小写 | `com.wansheng.vehicle` |
| 数据库表 | snake_case 复数 | `insurance_history` |
| 数据库列 | snake_case | `plate_number` |

#### 分层职责
```
Controller → 只做参数校验和响应封装，不写业务逻辑
Service    → 业务逻辑编排，事务管理
Mapper     → 数据访问，只写 SQL
Entity     → 纯数据载体，不写业务方法
DTO        → 接口传输对象，与 Entity 解耦
```

#### 禁止事项
- ❌ Controller 中写业务逻辑
- ❌ 直接返回 Entity 给前端（应使用 DTO）
- ❌ SQL 拼接（使用 MyBatis-Plus LambdaWrapper）
- ❌ 捕获异常后不处理（吞异常）
- ❌ 循环中查询数据库（N+1 问题）

### 1.2 Vue3 前端规范

#### 组件命名
- 页面组件：PascalCase，如 `Vehicles.vue`
- 公共组件：PascalCase，如 `StatCard.vue`
- 单文件组件顺序：`<template>` → `<script setup>` → `<style scoped>`

#### 状态管理
- 全局状态用 Pinia
- 组件局部状态用 `ref/reactive`
- API 调用统一通过 `@/api/` 模块
- 不在组件中直接写 axios 调用

---

## 二、Git 工作流

### 分支策略
```
main          ← 生产分支（只接受 PR）
  └─ develop  ← 开发分支
       ├─ feature/xxx  ← 功能分支
       ├─ bugfix/xxx   ← 修复分支
       └─ hotfix/xxx   ← 紧急修复
```

### Commit 规范
```
feat: 新增车辆导入Excel功能
fix: 修复保险到期日期计算错误
refactor: 重构提醒扫描定时任务
docs: 更新API接口文档
style: 统一表格样式
test: 补充车辆Service单元测试
```

### Code Review 清单
- [ ] 代码符合命名规范
- [ ] 没有硬编码的魔法值
- [ ] 异常处理完善
- [ ] 数据库查询是否有 N+1
- [ ] 接口是否有权限校验
- [ ] 是否有对应的测试
- [ ] 日志是否合适（不打印敏感信息）

---

## 三、API 设计规范

### RESTful 风格
```
GET    /api/vehicles          → 列表查询
GET    /api/vehicles/{id}     → 详情
POST   /api/vehicles          → 新增
PUT    /api/vehicles/{id}     → 全量更新
PATCH  /api/vehicles/{id}     → 部分更新
DELETE /api/vehicles/{id}     → 删除
POST   /api/vehicles/{id}/renew-insurance  → 子资源操作
```

### 统一响应格式
```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "meta": {
    "current_page": 1,
    "last_page": 5,
    "per_page": 15,
    "total": 72
  }
}
```

### 错误码约定
| code | 含义 |
|------|------|
| 0 | 成功 |
| 1 | 业务错误 |
| 401 | 未登录/Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 422 | 参数校验失败 |
| 500 | 服务器内部错误 |

---

## 四、数据库设计规范

### 命名
- 表名：小写 + 下划线 + 复数（`vehicles`, `insurance_history`）
- 主键：统一用 `id`，自增
- 时间列：`created_at`, `updated_at`
- 索引：`idx_表名_列名`（`idx_vehicles_plate_number`）
- 唯一键：`uk_表名_列名`（`uk_vehicles_plate_number`）

### 字段类型
- 金额：`DECIMAL(10,2)`
- 状态枚举：`TINYINT` + 注释说明
- 长文本：`TEXT`
- 文件路径：`VARCHAR(255)`
- 所有表必须加注释 `COMMENT`

---

## 五、安全规范

1. **密码加密**: BCrypt，不允许明文存储
2. **JWT 过期**: 24小时，前端自动刷新
3. **SQL 注入防护**: 全部使用参数化查询
4. **XSS 防护**: 前端输出转义
5. **权限校验**: 前端隐藏 + 后端二次校验（防止篡改前端代码）
6. **敏感信息**: Token/密码不打印到日志
7. **文件上传**: 限制类型（pdf/jpg/png）+ 大小（10MB）

---

## 六、测试规范

### 覆盖率目标
- Service 层: ≥ 80%
- Controller 层: ≥ 60%
- 定时任务: 100%

### 测试命名
```java
// 方法名_场景_预期结果
@Test
void shouldReturnOverdueWhenInsuranceExpirePassed() { ... }
```

---

## 七、性能基准

| 指标 | 目标 |
|------|------|
| API 响应时间 (P95) | < 500ms |
| 列表页加载 | < 1s |
| 定时任务执行 | < 30s |
| 数据库连接池 | 20 连接 |
| 前端首屏加载 | < 2s |
