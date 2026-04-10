# MCP 编排配置 - 团队技能与权限分配

> **创建日期**: 2026-04-01
> **适用项目**: 租车应用（Car Rental）
> **配置目标**: 为不同角色分配合理的 MCP 调用权限，确保安全高效的协作

---

## MCP 服务器总览

### 已配置服务器 (settings.local.json enabledMcpjsonServers)

| 服务器 | 用途 | 状态 |
|--------|------|------|
| `filesystem` | 文件操作 | ✅ 已配置 |
| `git` | Git 版本控制 | ✅ 已配置 |
| `sqlite` | SQLite 数据库查询 | ✅ 已配置 |
| `pup` | Puppeteer 无头浏览器 | ✅ 已配置 |
| `memory` | MCP 知识图谱记忆 | ✅ 已配置 |

### 项目技能 (settings.json)

| 技能 | 命令 | 用途 |
|------|------|------|
| `team-project-manager` | `/pm` | 项目经理（统一协调入口）⭐ |
| `team-product-manager` | `/product-manager` | 产品经理 |
| `team-architect` | `/architect` | 架构师 |
| `team-backend-dev` | `/backend` | 后端开发 |
| `team-frontend-dev` | `/frontend` | 前端开发 |
| `team-qa-tester` | `/qa` | 质量保障 |
| `team-code-reviewer` | `/reviewer` | 代码审查 |
| `team-diagnostic-analyst` | `/diagnostic` | 诊断分析师 |
| `tdd-self-heal` | `/tdd` | TDD 自愈 |
| `type-precision-enforcer` | `/type-check` | 类型精度检查 |

---

## 角色权限矩阵

### 权限级别定义

| 级别 | 说明 | 示例 |
|------|------|------|
| ✅ **完全权限** | 可独立调用 | 读取文件、运行测试 |
| ⚠️ **受限权限** | 需特定条件或 PM 授权 | 修改生产配置、部署 |
| ❌ **禁止** | 无权调用 | 越权修改其他模块 |

---

## 各角色 MCP 配置

### 1. Team PM (`/pm`)

**核心职责**: 任务分解、角色调度、进度追踪

#### MCP 调用权限

| MCP 服务器 | 权限 | 使用场景 |
|------------|------|----------|
| `filesystem` | ✅ | 读取任务文档、更新进度文件 |
| `sqlite` | ⚠️ | 仅查询数据，不修改业务数据 |

#### 推荐调用的全局技能

| 阶段 | 技能 | 命令 |
|------|------|------|
| 需求分析 | `brainstorming` | `Agent(subagent_type="brainstorming")` |
| 任务规划 | `planning-with-files-zh` | `/planning-with-files` |
| 并行调度 | `dispatching-parallel-agents` | 单消息多 Agent 调用 |
| 代码审查 | `code-review` | `/reviewer` |
| 完成验证 | `verification-before-completion` | `Agent(subagent_type="verification-before-completion")` |

#### 权限边界

```python
# ✅ 允许的操作
- 读取 docs/planning/*.md
- 更新 docs/planning/progress.md
- 创建/更新 Task

# ❌ 禁止的操作
- 直接修改 backend/app/ 业务代码（由 Dev 负责）
- 直接修改 tests/ 测试代码（由 QA 负责）
```

---

### 2. Backend Developer (`/backend`)

**核心职责**: API 接口、业务逻辑、数据模型

#### MCP 调用权限

| MCP 服务器 | 权限 | 使用场景 |
|------------|------|----------|
| `filesystem` | ✅ | 读取/修改 backend/ 目录 |
| `sqlite` | ✅ | 查询 schema、验证数据模型 |

#### 推荐调用的全局技能

| 场景 | 技能 | 命令 |
|------|------|------|
| 代码简化 | `code-simplifier` | `/simplify` |
| TDD 开发 | `tdd-self-heal` | `/tdd` |
| 类型检查 | `type-precision-enforcer` | `/type-check` |
| Bug 调试 | `systematic-debugging` | `Agent(subagent_type="systematic-debugging")` |

#### 权限边界

```python
# ✅ 允许的操作
- 读取/修改 backend/app/ 目录
- 读取/修改 tests/unit/, tests/integration/ (与 QA 协作)
- 查询数据库表

# ❌ 禁止的操作
- 修改 web-front/ 目录（前端代码）

# ⚠️ 需要协调的操作
- 修改 API 接口 Schema → 通知 PM 分配给 frontend-dev 对接
- 修改数据库 schema → 通知 PM 记录
```

#### 典型工作流

```python
# 1. 接收任务后调用 TDD 技能
/tdd 实现订单创建接口

# 2. 代码完成后调用简化
/simplify backend/app/services/order_service.py

# 3. 调用类型检查
/type-check backend/app/

# 4. 运行测试验证
pytest tests/unit/test_order_service.py -v
```

---

### 3. Frontend Developer (`/frontend`)

**核心职责**: React 组件、TypeScript 类型、UI/UX

#### MCP 调用权限

| MCP 服务器 | 权限 | 使用场景 |
|------------|------|----------|
| `filesystem` | ✅ | 读取/修改 web-front/ 目录 |
| `puppeteer` | ✅ | UI 自动化测试、页面截图 |

#### 推荐调用的全局技能

| 场景 | 技能 | 命令 |
|------|------|------|
| UI 设计 | `ui-ux-pro-max` | 设计组件样式 |
| 组件构建 | `web-artifacts-builder` | 复杂组件开发 |
| 代码简化 | `code-simplifier` | `/simplify` |
| E2E 测试 | `webapp-testing` | Playwright 测试 |

#### 权限边界

```python
# ✅ 允许的操作
- 读取/修改 web-front/ 所有文件
- 读取后端 API Schema
- 调用 puppeteer 进行页面测试
- 调用 ui-ux-pro-max 设计样式

# ❌ 禁止的操作
- 修改 backend/ 目录（后端代码）

# ⚠️ 需要协调的操作
- 需要 API 字段变更 → 通知 PM 分配给 backend-dev
```

#### 典型工作流

```typescript
// 1. 阅读契约文档

// 2. 调用 UI 设计技能
Agent(subagent_type="ui-ux-pro-max", prompt="设计预订表单的交互样式")

// 3. 实现组件后调用简化
/simplify web-front/src/components/BookingForm.tsx

// 4. 调用 E2E 测试
Agent(subagent_type="webapp-testing", prompt="为预订功能编写 Playwright 测试")
```

---

### 4. QA Tester (`/qa`)

**核心职责**: 测试策略、单元测试、集成测试、E2E 测试

#### MCP 调用权限

| MCP 服务器 | 权限 | 使用场景 |
|------------|------|----------|
| `filesystem` | ✅ | 读取/修改 tests/ 目录 |
| `sqlite` | ✅ | 查询测试数据、验证结果 |

#### 推荐调用的全局技能

| 场景 | 技能 | 命令 |
|------|------|------|
| E2E 测试 | `webapp-testing` | Playwright 测试 |
| 测试简化 | `code-simplifier` | `/simplify` |
| 测试失败分析 | `systematic-debugging` | `Agent(subagent_type="systematic-debugging")` |

#### 权限边界

```python
# ✅ 允许的操作
- 读取/修改 tests/ 所有文件
- 读取 backend/app/ 目录（理解被测代码）
- 查询数据库验证测试结果
- 运行 pytest tests/ -v

# ❌ 禁止的操作
- 直接修改 backend/app/ 业务代码（发现 Bug 时通知对应 Dev 修复）
- 修改 web-front/ 业务代码

# ⚠️ 需要协调的操作
- 测试发现 Bug → 通知 PM 分配修复任务
```

#### 典型工作流

```python
# 1. 阅读契约文档，设计测试用例

# 2. 编写测试代码
# tests/unit/test_order_api.py

# 3. 运行测试
pytest tests/unit/test_order_api.py -v

# 4. 测试失败时调用调试
Agent(subagent_type="systematic-debugging", prompt="test_order_api 失败，分析根因")
```

---

### 5. Code Reviewer (`/reviewer`)

**核心职责**: 独立代码审查、架构一致性检查、安全隐患识别

#### MCP 调用权限

| MCP 服务器 | 权限 | 使用场景 |
|------------|------|----------|
| `filesystem` | ✅ | 读取所有代码文件 |

#### 推荐调用的全局技能

| 场景 | 技能 | 命令 |
|------|------|------|
| 类型精度审查 | `type-precision-enforcer` | `/type-check` |

#### 权限边界

```python
# ✅ 允许的操作
- 读取所有源代码文件
- 读取测试文件
- 读取架构文档
- 批准/拒绝代码合并

# ❌ 禁止的操作
- 直接修改业务代码（发现问题通知对应角色修复）

# ⚠️ 需要协调的操作
- 发现严重架构问题 → 通知 PM 重新规划
```

#### 审查清单

```markdown
## 审查检查清单

### 类型安全
- [ ] 无 float 污染（金额计算使用 Decimal）
- [ ] Pydantic 模型定义完整
- [ ] 金额计算使用 Decimal

### 安全
- [ ] 无 SQL 注入风险
- [ ] JWT 短有效期
- [ ] 敏感信息不记录日志

### 架构一致性
- [ ] 分层架构正确
- [ ] 日志脱敏正确

### 测试质量
- [ ] 测试覆盖率达标
- [ ] 边界条件已测试
- [ ] 回归测试通过
```

---

## MCP 调用最佳实践

### 1. 数据库查询 (SQLite MCP)

```python
# ✅ 推荐：只读查询
mcp__sqlite__read_query: |
  SELECT * FROM users
  WHERE phone = '13800000000'
  LIMIT 10

# ❌ 避免：直接修改生产数据
mcp__sqlite__write_query: |
  DELETE FROM orders WHERE ...  # 除非是测试清理
```

### 2. 文件操作 (FileSystem MCP)

```python
# ✅ 推荐：读取多个文件
mcp__filesystem__read_multiple_files:
  paths:
    - backend/app/services/order_service.py
    - backend/app/models/order.py

# ✅ 推荐：搜索文件
mcp__filesystem__search_files:
  path: backend/app
  pattern: "**/*.py
```

### 3. Python 执行 (Bash 权限)

```bash
# ✅ 允许的运行测试命令
Bash(pytest tests/unit/ -v)

# ⚠️ 需要授权的命令
Bash(python3 backend/app/main.py)  # 启动生产服务
Bash(git push origin main)  # 推送到主分支
```

---

## 技能调用流程图

```
                    ┌─────────────────┐
                    │  用户需求输入    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  PM    │
                    │  任务分解        │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
     ┌────────────┐ ┌────────────┐ ┌────────────┐
     │ Backend    │ │ Frontend   │ │ QA         │
     │ /backend   │ │ /frontend  │ │ /qa        │
     └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
           │              │              │
           │ 调用技能      │ 调用技能      │ 调用技能
           ▼              ▼              ▼
     ┌────────────┐ ┌────────────┐ ┌────────────┐
     │ /tdd       │ │ /ui-ux     │ │ /webapp    │
     │ /simplify  │ │ /simplify  │ │ /debug     │
     │ /type-check│ │            │ │            │
     └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
           │              │              │
           └──────────────┼──────────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │  Reviewer       │
                 │  /reviewer      │
                 │  /type-check    │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │  PM    │
                 │  整合输出        │
                 └─────────────────┘
```

---

## 故障排查

### MCP 工具调用失败

```bash
# 1. 检查 MCP 服务器是否加载
cat ~/.claude/mcp.json

# 2. 检查 permissions.allow 是否包含对应权限
cat .claude/settings.local.json | jq '.permissions.allow'

# 3. 重启 Claude Code
/exit
claude
```

### 技能未加载

```bash
# 检查 settings.json 配置
cat .claude/settings.json | jq '.skills.local'

# 确认技能文件存在
ls -la .claude/skills/*/SKILL.md
ls -la .claude/team/*/SKILL.md
```

---

*维护者：AI Builder*
*项目：租车应用（Car Rental）*
*最后更新：2026-04-10*
