# 团队技能与 MCP 配置完成总结

> **配置日期**: 2026-04-01
> **项目**: 租车应用（Car Rental）
> **状态**: ✅ 配置完成

---

## 完成的配置工作

### 1. MCP 服务器配置

已配置 5 个 MCP 服务器：

| 服务器 | 状态 | 用途 |
|--------|------|------|
| `filesystem` | ✅ | 文件操作 |
| `git` | ✅ | Git 版本控制 |
| `sqlite` | ✅ | SQLite 数据库查询 |
| `pup` | ✅ | Puppeteer 无头浏览器 |
| `memory` | ✅ | 知识图谱记忆 |

### 2. 项目技能注册 (`.claude/settings.json`)

已注册 14 个技能：

| 技能 | 命令 | 用途 |
|------|------|------|
| `team-product-manager` | `/product-manager` | 产品经理 |
| `team-project-manager` | `/pm` | 项目经理（统一入口） |
| `team-architect` | `/architect` | 架构师 |
| `team-backend-dev` | `/backend` | 后端开发 |
| `team-frontend-dev` | `/frontend` | 前端开发 |
| `team-qa-tester` | `/qa` | 质量保障 |
| `team-code-reviewer` | `/reviewer` | 代码审查 |
| `team-diagnostic-analyst` | `/diagnostic` | 诊断分析师 |
| `tdd-self-heal` | `/tdd` | TDD 闭环自愈 |
| `type-precision-enforcer` | `/type-check` | 类型精度检查 |
| `pua` | `/pua` | 提示词升级 |
| `prd` | `/prd` | 产品需求文档 |
| `ralph` | `/ralph` | PRD 转 Ralph JSON |

### 3. 团队角色技能更新

已为以下角色添加 MCP 调用指南：

| 角色 | 文件 | 更新内容 |
|------|------|----------|
| PM | `team/project-manager/SKILL.md` | MCP 查询权限、调度指南 |
| Backend Dev | `team/backend-dev/SKILL.md` | TDD、类型检查、MCP 查询 |
| Frontend Dev | `team/frontend-dev/SKILL.md` | UI 设计、E2E 测试、Puppeteer |
| QA Tester | `team/qa-tester/SKILL.md` | 测试技能、数据库查询 |
| Code Reviewer | `team/code-reviewer/SKILL.md` | 类型检查、审查脚本 |

### 4. 创建的文档

| 文档 | 路径 | 用途 |
|------|------|------|
| MCP 编排配置 | `.claude/MCP-ORCHESTRATION.md` | 角色权限矩阵 |
| MCP 快速参考 | `.claude/MCP-QUICKSTART.md` | MCP 服务器使用指南 |
| MCP 环境变量 | `.claude/MCP-ENV-CONFIG.md` | 环境变量配置 |
| Agentic Workflow | `.claude/AGENTIC-WORKFLOW-GUIDE.md` | 高阶技能设计 |
| TDD 自愈技能 | `.claude/skills/agentic-workflow/tdd-self-heal/SKILL.md` | TDD 工作流 |
| 类型精度技能 | `.claude/skills/agentic-workflow/type-precision-enforcer/SKILL.md` | 精度检查 |

---

## 角色权限总览

### 文件修改权限

| 目录 | Backend | Frontend | QA | Reviewer | PM |
|------|---------|----------|----|----------|-------------|
| `backend/` | ✅ | ❌ | ⚠️ | ✅ | ✅ |
| `web-front/` | ❌ | ✅ | ⚠️ | ✅ | ✅ |
| `tests/` | ⚠️ | ⚠️ | ✅ | ✅ | ⚠️ |

**图例**: ✅ 全权 | ❌ 禁止 | ⚠️ 有限权限

### MCP 调用权限

| 角色 | FileSystem | SQLite | Puppeteer |
|------|------------|--------|-----------|
| Backend | ✅ | ✅ | ❌ |
| Frontend | ✅ | ❌ | ✅ |
| QA | ✅ | ✅ | ❌ |
| Reviewer | ✅ | ❌ | ❌ |
| PM | ✅ | ⚠️ | ❌ |

---

## 典型工作流

### 新功能开发 (TDD)

```bash
# 1. PM 分解任务
/pm 实现预订下单功能

# 2. Backend 调用 TDD 技能
/backend
/tdd 实现预订下单功能

# 3. 代码完成后检查
/simplify backend/app/services/order_service.py
/type-check backend/app/

# 4. QA 编写测试
/qa
/tdd 编写预订下单测试

# 5. Reviewer 审查
/reviewer 审查预订下单代码

# 6. PM 整合交付
```

### Bug 修复流程

```bash
# 1. QA 发现 Bug
/qa
pytest tests/unit/test_xxx.py -v
# 测试失败

# 2. 分析根因
Agent(subagent_type="systematic-debugging",
      prompt="test_xxx 失败，分析根因")

# 3. Backend 修复
/backend
# 修复业务代码

# 4. QA 回归验证
/qa
pytest tests/unit/test_xxx.py -v

# 5. Reviewer 审查
/reviewer 审查 Bug 修复

# 6. PM 交付
```

---

## 快速入门

### 使用 TDD 技能

```bash
# 调用 TDD 自愈技能
/tdd 实现订单状态机的确认/拒绝功能
契约文档：docs/arch/api.md
```

### 运行类型检查

```bash
# 使用技能
/type-check backend/app/
```

### 查询数据库

```python
# 查询用户列表
mcp__sqlite__read_query: |
  SELECT id, phone, nickname, created_at
  FROM users
  ORDER BY created_at DESC
  LIMIT 10
```

---

## 相关文档索引

| 文档 | 路径 |
|------|------|
| MCP 编排配置 | `.claude/MCP-ORCHESTRATION.md` |
| MCP 快速开始 | `.claude/MCP-QUICKSTART.md` |
| MCP 环境变量 | `.claude/MCP-ENV-CONFIG.md` |
| 团队快速参考 | `.claude/team/QUICK-REFERENCE.md` |
| Agentic Workflow | `.claude/AGENTIC-WORKFLOW-GUIDE.md` |

---

*配置完成日期：2026-04-01*
*维护者：AI Builder*
*项目：租车应用（Car Rental）*
