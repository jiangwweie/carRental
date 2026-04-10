---
name: 项目核心记忆
description: 租车应用项目的工作流程、技术栈、质量要求、核心文档索引
type: project
---

# 项目核心记忆整合

**更新日期**: 2026-04-10
**项目**: 租车应用（Car Rental）

---

## 一、项目概述

个人自营租车业务，面向旅游场景用户提供车辆出租服务。MVP 阶段跑通"展示-预订-支付-取车"核心流程。

### 技术栈
- **前端**: React 18+ / TypeScript 5+ / TailwindCSS 3+ / Zustand / Vite
- **后端**: Python + FastAPI / SQLAlchemy 2.0 / Alembic
- **数据库**: PostgreSQL 15+
- **部署**: Docker + Docker Compose + Nginx
- **外部服务**: 微信 JSAPI 支付 / 阿里云或腾讯云短信 / 腾讯云 COS 或阿里云 OSS

### 架构模式
```
api/          → HTTP 层：路由、请求校验、响应格式化
services/     → 业务逻辑层：定价计算、订单状态机、支付流程
models/       → 数据模型层：SQLAlchemy 模型
schemas/      → 数据校验层：Pydantic 请求/响应模型
core/         → 基础设施：数据库连接、JWT、配置
utils/        → 工具函数：日期计算、价格计算
```

### 核心实体
- User（用户）- 手机号验证码登录
- Vehicle（车辆）- 工作日/周末/节假日定价
- Order（订单）- 待确认/已确认/进行中/已完成/已取消/已拒绝
- HolidayConfig（节假日配置）
- UserAgreement（用户协议）

---

## 二、工作流程偏好

**复杂任务必须走全自动工作流**：

```
【阶段 0】需求接收 → 【阶段 1】契约设计 → 【阶段 2】任务分解 → 【阶段 3】并行开发
                                                                    ↓
【阶段 6】提交汇报 ←─【阶段 5】测试执行 ←─【阶段 4】审查验证 ←──────────┘
```

### 核心原则
1. **契约先行**: 先写接口契约表，作为 SSOT（单一事实来源）
2. **并行执行**: 前后端独立任务并行开发
3. **自动审查**: Reviewer 对照契约表检查
4. **无人值守**: 简单问题自解，严重问题标记 blocked 最后汇报

### 通知节点
用户明确要求：**阶段 4 完成，阶段 5 启动前，必须通知用户确认**

---

## 三、质量要求与审查红线

### 1. 类型安全
- **禁用 `Dict[str, Any]`** - 核心参数必须定义具名 Pydantic 类
- **辨识联合** - 多态对象必须使用 `discriminator='type'`
- **金额计算** - 使用 `decimal.Decimal`（禁用 float）

### 2. 异步规范
- 所有 I/O 使用 `async/await`
- 禁止 `time.sleep()` 阻塞事件循环
- 并发控制使用 `asyncio.Lock`

### 3. 安全设计
- HTTPS 全站，JWT 认证，微信支付签名验证
- API 限流通过 FastAPI 中间件
- 敏感信息不存储，JWT 短有效期

### 4. 测试覆盖要求
| 模块 | 覆盖率要求 |
|------|-----------|
| 定价引擎 | 100% |
| 订单状态机 | 100% |
| 支付服务 | 95% |
| 认证服务 | 90% |

---

## 四、核心文档索引

### 产品文档
| 文档 | 路径 |
|------|------|
| 产品需求文档 (PRD) | `docs/products/car-rental-brief.md` |
| 需求池 (Backlog) | `docs/products/backlog.md` |

### 架构文档
| 文档 | 路径 |
|------|------|
| 架构设计 (ADR) | `docs/arch/design.md` |
| API 契约 | `docs/arch/api.md` |
| 数据库设计 | `docs/arch/database.md` |

### 规划文档
| 文档 | 路径 |
|------|------|
| 进度日志 | `docs/planning/progress.md` |
| 交接文档 | `docs/planning/2026-04-09-handoff.md` |

---

*本文件为项目从零开始的核心记忆*
