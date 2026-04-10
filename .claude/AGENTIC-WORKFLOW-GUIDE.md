# Agentic Workflow Skills - 配置与使用指南

> **创建日期**: 2026-04-01
> **技能版本**: v1.0
> **适用项目**: 租车应用（Car Rental）

---

## 已配置的技能清单

### ✅ 已注册技能 (settings.json)

| 技能名称 | 命令 | 用途 |
|---------|------|------|
| `team-project-manager` | `/pm` | 项目经理 - 任务分解与调度 |
| `team-product-manager` | `/product-manager` | 产品经理 - 需求收集、PRD 输出 |
| `team-architect` | `/architect` | 架构师 - 架构设计、技术选型 |
| `team-backend-dev` | `/backend` | 后端开发专家 |
| `team-frontend-dev` | `/frontend` | 前端开发专家 |
| `team-qa-tester` | `/qa` | 质量保障专家 |
| `team-code-reviewer` | `/reviewer` | 代码审查员 |
| `team-diagnostic-analyst` | `/diagnostic` | 诊断分析师 |
| `tdd-self-heal` | `/tdd` | TDD 闭环自愈 |
| `type-precision-enforcer` | `/type-check` | 类型与精度检查 |

---

## 技能使用说明

### 1. TDD 闭环自愈 (`/tdd`)

**触发方式**:
```
/tdd 实现订单状态机的确认/拒绝功能
契约文档：docs/arch/api.md
```

**工作流程**:
```
1. 解析契约文档 → 提取测试用例
2. 生成 pytest 测试代码
3. 运行测试 (预期失败)
4. 实现业务代码
5. 运行测试 (自动修复直到通过)
6. 提交代码
```

**文件位置**: `.claude/skills/agentic-workflow/tdd-self-heal/SKILL.md`

---

### 2. 类型与精度宪兵 (`/type-check`)

**触发方式**:
```
/type-check 审查 backend/app/services/order_service.py
```

**检查项目**:
- ❌ float 污染（金额计算应使用 Decimal）
- ❌ Pydantic 判别器缺失
- ❌ 类型不匹配

**文件位置**: `.claude/skills/agentic-workflow/type-precision-enforcer/SKILL.md`

---

## MCP 服务器配置

### 已配置 (settings.local.json enabledMcpjsonServers)

| 服务器 | 状态 | 用途 |
|--------|------|------|
| `filesystem` | ✅ 已配置 | 文件操作 |
| `git` | ✅ 已配置 | Git 版本控制 |
| `sqlite` | ✅ 已配置 | SQLite 数据库查询 |
| `pup` | ✅ 已配置 | Puppeteer 无头浏览器 |
| `memory` | ✅ 已配置 | MCP 知识图谱记忆 |

**配置文件**: `~/.claude/mcp.json`（全局）+ `settings.local.json`（项目级）

---

## 快速开始

### 使用 TDD 技能开发新功能

```bash
# 1. 调用技能
/tdd 实现预订下单功能

# 2. AI 自动执行
# - 生成测试代码
# - 运行 pytest
# - 实现业务逻辑
# - 自我修复直到通过

# 3. 检查结果
git diff HEAD  # 查看变更
```

### 运行类型检查

```bash
# 方式 1: 使用技能
/type-check 审查 backend/app/

# 方式 2: 直接运行脚本
python3 scripts/check_float.py
```

---

## 与现有工作流集成

### planning-with-files 集成

使用技能时自动更新规划文件：

```markdown
# docs/planning/progress.md

## 进度日志

使用 TDD 闭环自愈技能开发功能，每次迭代记录进度。
```

### Git 提交规范

```bash
# 功能开发提交
git add backend/app/services/
git commit -m "feat(order): 实现订单状态机

- 待确认 → 已确认/已拒绝
- 已确认 → 进行中 → 已完成
- 待确认 → 已取消

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 故障排查

### 技能未加载

```bash
# 检查 settings.json 配置
cat .claude/settings.json | jq '.skills.local'

# 确认技能文件存在
ls -la .claude/skills/*/SKILL.md
ls -la .claude/team/*/SKILL.md
```

### MCP 服务器未加载

```bash
# 检查全局配置
cat ~/.claude/mcp.json

# 重启 Claude Code
/exit
claude
```

---

## 参考文档

| 文档 | 位置 |
|------|------|
| MCP 配置指南 | `.claude/MCP-QUICKSTART.md` |
| MCP 编排配置 | `.claude/MCP-ORCHESTRATION.md` |
| 技能设计文档 | `.claude/skills/agentic-workflow/README.md` |
| 架构设计 | `docs/arch/design.md` |

---

*维护者：AI Builder*
*项目：租车应用（Car Rental）*
