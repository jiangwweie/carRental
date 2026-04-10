# Claude Code 培训核心主题讲解框架

**培训时长**: 70分钟（更新后）
**核心主题**: 第一性原理 | MCP vs Skill | 上下文传递 | Agent团队协作
**生成日期**: 2026-04-09
**最新更新**: 融入 Agentic Coding 第一性原理文章核心观点

---

## 主题 0️⃣：第一性原理 — 理解 LLM 的本质（10分钟）⭐ 新增

### 🎯 为什么需要理解 LLM 本质

> "不理解 LLM 如何工作，就无法设计高效的协作模式。所有最佳实践都根植于 LLM 的物理约束。"

### LLM 如何「思考」

**核心机制：自回归生成（Autoregressive Generation）**

LLM 的工作方式可以用一句话概括：**预测下一个 token**。

```
你输入 prompt → LLM 预测第一个 token → 追加到序列 → 预测第二个 token → ...
```

三个本质特征：

1. **没有独立于输出的「思考」过程**：LLM 不能先想好再说，只能边说边想
2. **上下文就是全部记忆**：没有独立存储，窗口外的内容=不存在
3. **生成具有概率性**：同样输入可能产生不同输出

### Attention：LLM 如何「阅读」上下文

- **动态聚光灯**：模型生成每个 token 时，对上下文分配不同权重
- **注意力是稀疏的**：关键信息集中在少数位置，大部分权重接近于零
- **计算复杂度 O(n²)**：上下文长度翻倍，计算量变成四倍

### 上下文约束的现实

| 概念 | 事实 |
|------|------|
| 物理上限 | 主流模型 128K ~ 200K tokens |
| 有效上下文 | 仅约 10-15% 的标称值 |
| Dumb Zone | 中间 40-60% 区域性能退化 |
| 累积误差 | 错误理解会传递，形成滚雪球效应 |

### 对 Coding Agent 的影响

```
自回归 + 长上下文 → Agent 的五大问题：

1. 局部最优：边写边想 ≠ 全局规划
2. 偏差滚雪球：早期误读 → 后面全错
3. 重写倾向：擅长写新文件，不擅长精确编辑
4. 约束困难：边界条件、并发、错误处理易出错
5. 单线程思考：难并行探索多种方案
```

### 🎤 培训话术

> "LLM 不是魔法，它有物理限制。
> 就像潜水员的氧气罐——所有人说'给他更大的罐子：100万 token！'
> 但他最终还是会耗尽氧气。
> **关键不是罐子有多大，而是你怎么用。**"

**核心启示**（贯穿整个培训）：
- 短对话 > 长对话（从 LLM 本质推导出来，不是主观偏好）
- 200K 足够用了（有效利用比标称值重要）
- 对人难的事，对 AI 也难（LLM 和人类面临相似的上下文约束）
- 复利工程：每次修复都在教育系统，而不是只解决今天的问题

---

## 主题 1️⃣：MCP vs Skill - 工具与工作手册的本质区别（12分钟）

### 🎯 核心概念对比表

| 维度 | MCP (Model Context Protocol) | Skill (技能定义) |
|------|------------------------------|-----------------|
| **本质** | 🔧 **工具箱**（安装工具） | 📖 **工作手册**（定义流程） |
| **类比** | 给AI装上"手"和"眼" | 教AI"怎么做事" |
| **作用** | 扩展AI的能力边界 | 规范AI的行为模式 |
| **位置** | `~/.claude/mcp.json`（全局） | `.claude/skills/` 或插件市场 |
| **调用方式** | `mcp__sqlite__query` | `/skill-name` 或自动触发 |
| **能力类型** | 硬件能力（数据库、文件、浏览器） | 软件能力（流程、规范、模板） |
| **是否可编程** | ❌ 调用即可，无需定义 | ✅ 完全可定制（SKILL.md） |
| **依赖关系** | MCP提供工具 → Skill调用工具 | Skill依赖MCP才能工作 |

### 📊 演示用可视化结构

```
┌────────────────────────────────────────────┐
│  Claude 大模型（200K上下文窗口）             │
│  ├─ 理解指令                                │
│  ├─ 推理决策                                │
│  └─ 生成代码                                │
└────────────────────────────────────────────┘
         ↓ 需要
┌────────────────────────────────────────────┐
│  MCP 层（工具箱）                            │
│  ├─ SQLite MCP → 能查数据库                 │
│  ├─ FileSystem MCP → 能读写文件             │
│  ├─ Git MCP → 能版本控制                    │
│  ├─ Puppeteer MCP → 能操作浏览器            │
│  └────────────────────────────────────────┘
         ↓ 被
┌────────────────────────────────────────────┐
│  Skill 层（工作手册）                        │
│  ├─ planning-with-files → 教会AI用三文件    │
│  ├─ backend-dev → 教会AI写后端代码流程      │
│  ├─ qa-tester → 教会AI写测试用例流程        │
│  └────────────────────────────────────────┘
         ↓ 调用者是
┌────────────────────────────────────────────┐
│  Agent Team（团队协作）                      │
│  ├─ PM → 协调                               │
│  ├─ Backend → 用 backend-dev skill         │
│  ├─ QA → 用 qa-tester skill                │
│  └────────────────────────────────────────┘
```

### 🎤 讲解要点（培训师话术）

#### **开场类比（1分钟）**

> "想象你雇佣了一个超级聪明的程序员，但他被困在玻璃盒子里：
> - 没有手（无法操作文件、数据库）
> - 没有眼（无法查看代码、日志）
> - 只能听你说，然后给出建议
>
> **MCP 就是给AI装上手和眼** - 让他能操作SQLite、读写文件、打开浏览器
> **Skill 就是给AI工作手册** - 告诉他先写测试再写代码、先看契约再实现"

#### **MCP 讲解（3分钟）**

**展示你的配置**：
```json
// ~/.claude/mcp.json（全局）
{
  "mcpServers": {
    "sqlite": {...},     // 查数据库
    "filesystem": {...}, // 读文件
    "git": {...},        // 版本控制
    "puppeteer": {...}   // 浏览器
  }
}
```

**演示点**：
- 打开 `~/.claude/mcp.json` 文件
- 指出：这是全局配置（所有项目共享）
- 强调：MCP安装一次，所有项目都能用

**现场演示 MCP 能力**：
```bash
# 演示 SQLite MCP
mcp__sqlite__query: "SELECT * FROM signals LIMIT 5"
```

> "看，AI可以直接查数据库了，这就是MCP的能力"

#### **Skill 讲解（3分钟）**

**展示你的配置**：
```bash
# 项目级 Skill（可定制）
.claude/skills/
  ├─ backend-dev/SKILL.md    # 后端工作流程
  ├─ qa-tester/SKILL.md      # 测试工作流程
  └─ project-manager/SKILL.md # PM工作流程

# 全局 Skill（插件市场）
~/.claude/plugins/cache/
  ├─ document-skills/  # Word/PDF/Excel
  ├─ example-skills/   # MCP-builder, webapp-testing
```

**打开一个 SKILL.md 文件演示**：
```markdown
---
name: backend-dev
description: "Python + FastAPI + asyncio 后端实现"
---

# Backend Developer Skill

## 工作流程
1. 阅读 OpenAPI Spec 契约
2. 从契约生成类型定义
3. 实现业务逻辑
4. 编写单元测试
5. 调用 code-simplifier 优化
```

> "Skill 像一份详细的工作手册，教会AI：先看契约、再写代码、最后优化"

#### **关键区别强调（2分钟）**

**对比演示**：

| 场景 | 只用 MCP | MCP + Skill |
|------|---------|------------|
| **查数据库** | ✅ 能查 | ✅ 能查 + 知道何时查 |
| **写代码** | ❌ 没规范 | ✅ 有流程（测试先行） |
| **团队协作** | ❌ 无分工 | ✅ 角色分工清晰 |
| **代码质量** | ⚠️ 随机 | ✅ 强制检查清单 |

> "只用 MCP 的 AI 像聪明但乱来的实习生
> MCP + Skill 的 AI 像训练有素的工程师"

#### **提升与限制（1分钟）**

**提升效果**：
- **速度**：10倍开发速度（并行开发 + 自动审查）
- **质量**：强制遵循规范（开工/收工检查清单）
- **协作**：多Agent并行（文件边界规则）
- **数据**：直接操作数据库（MCP能力）

**限制说明**：
- **MCP 配置复杂**：需要填写 Token、路径等
- **Skill 需维护**：项目变更需更新 SKILL.md
- **上下文有限**：200K tokens，大项目需压缩
- **成本考量**：多Agent并行会增加 API 调用成本

---

## 主题 2️⃣：上下文传递与记忆系统（10分钟）

### 🎯 三层记忆架构

```
┌─────────────────────────────────────────────┐
│  Layer 1: Memory MCP（永久记忆）⭐           │
│  ├─ 架构决策（REST vs WebSocket）            │
│  ├─ 技术选型（Optuna vs 手动调参）           │
│  ├─ 架构红线（domain层严禁I/O框架）          │
│  ├─ 踩坑记录（MCP配置踩坑）                  │
│  └─────────────────────────────────────────┘
│  保留时长：永久                              │
│  会话恢复：上下文丢失后仍可恢复 ⭐⭐⭐       │
│  存储位置：Memory MCP 知识图谱               │
└─────────────────────────────────────────────┘
         ↓ 补充
┌─────────────────────────────────────────────┐
│  Layer 2: findings.md（技术发现）            │
│  ├─ 技术洞见（asyncio最佳实践）             │
│  ├─ 性能优化方案                            │
│  ├─ Bug根因分析                             │
│  └─────────────────────────────────────────┘
│  保留时长：7天归档                          │
│  智能匹配：按主题标签分类读取                │
│  存储位置：docs/planning/findings.md        │
└─────────────────────────────────────────────┘
         ↓ 补充
┌─────────────────────────────────────────────┐
│  Layer 3: progress.md（会话日志）            │
│  ├─ 今日完成工作                            │
│  ├─ 明日待办事项                            │
│  ├─ 阻塞问题列表                            │
│  └─────────────────────────────────────────┘
│  保留时长：3天归档                          │
│  更新时机：每个会话结束时                    │
│  存储位置：docs/planning/progress.md        │
└─────────────────────────────────────────────┘
```

### 🎤 讲解要点（培训师话术）

#### **开场类比（1分钟）**

> "上下文传递就像人的记忆系统：
> - **Memory MCP** = 长期记忆（永远记住架构决策）
> - **findings.md** = 知识笔记（7天内的重要发现）
> - **progress.md** = 工作日志（最近3天的进度）
>
> AI和人一样，会遗忘。三层记忆设计解决遗忘问题。"

#### **上下文窗口讲解（2分钟）**

**核心数据**：
- Claude Code：200K tokens ≈ 500页文档
- 自动压缩机制：历史对话会被压缩保留关键信息
- 问题：大项目容易超限（100+文件 + 对话历史）

**你的项目状态**（真实案例）：
```
progress.md: 119K tokens（未归档前）
findings.md: 82K tokens（未分类前）
Memory MCP: 永久保留，不计入上下文 ⭐
```

**优化效果**（你已实现的）：
```
归档前 vs 归档后：
progress.md: 119K → 30K（减少89K）⭐
findings.md: 82K → 10K（减少72K）⭐
总计节省：161K tokens（接近一个上下文窗口）⭐⭐⭐
```

#### **三层记忆详解（4分钟）**

**Layer 1: Memory MCP（最关键）**

打开你的配置展示：
```json
// settings.json
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}
```

演示 Memory MCP 使用：
```
用户："记住 DynamicRiskManager 的职责是只改价格不改数量"

→ Memory MCP 写入知识图谱

下一次会话：
AI 自动遵循这条架构红线 ⭐⭐⭐
```

**强调优势**：
> "Memory MCP 是最核心的设计：
> - 永久保留（上下文丢失后仍可恢复）
> - 知识图谱（可追溯架构决策）
> - 防止AI幻觉（强制遵循红线）"

**Layer 2: findings.md**

展示文件结构：
```markdown
# docs/planning/findings.md

## 标签：asyncio
- asyncio 最佳实践：避免 time.sleep
- 使用 asyncio.sleep 替代

## 标签：api
- FastAPI 响应时间优化方案
- 使用 background_tasks 异步处理

## 标签：frontend
- React 组件性能优化
- 使用 useMemo 减少重渲染
```

> "findings.md 智能匹配：
> Backend Dev 开工时，只读取 'asyncio' 和 'api' 标签
> Frontend Dev 开工时，只读取 'frontend' 标签
> 避免加载全部内容，节省上下文"

**Layer 3: progress.md**

展示文件：
```markdown
# docs/planning/progress.md

## 2026-04-09 会话记录
- 完成：待办事项API开发
- 测试：24/24 通过
- 待办：前端适配（明日）
- 阻塞：无

## 归档机制
- 超过3天自动归档到 archive/progress-archive.md
```

#### **会话恢复演示（2分钟）**

**演示流程**：

```bash
# 会话结束
/shougong

→ 自动执行：
  1. 写入 Memory MCP（今日总结）
  2. 更新 progress.md
  3. 更新 findings.md
  4. Git 提交

# 新会话开始（第2天）
/kaigong

→ 自动执行：
  1. 读取 Memory MCP（架构决策）⭐
  2. 读取 progress.md（最近3天）
  3. 智能读取 findings.md（相关标签）
  4. 恢复工作上下文 ⭐⭐⭐
```

> "看，上下文丢失后，通过三层记忆系统恢复：
> Memory MCP 提供永久记忆（架构红线）
> progress.md 提供短期记忆（最近进度）
> findings.md 提供专业知识（技术发现）"

#### **上下文传递最佳实践（1分钟）**

**强制规范**（从你的 WORKFLOW.md）：
```
开工前（/kaigong）：
- ✅ 读取 Memory MCP
- ✅ 读取 progress.md
- ✅ 智能读取 findings.md

收工前（/shougong）：
- ✅ 写入 Memory MCP（架构决策）
- ✅ 更新 progress.md
- ✅ 更新 findings.md
- ✅ Git 提交（不推送）
```

**红线规则**：
- ❌ 禁止跳过 `/kaigong`（会导致上下文丢失）
- ❌ 禁止跳过 `/shougong`（会导致进度丢失）
- ❌ 禁止只写代码不写文档（会无法追溯）

---

## 主题 3️⃣：Agent 团队协作规范（10分钟）

### 🎯 团队架构设计

```
决策层（3人）                执行层（3人）              支持层（3人）
┌──────────────┐           ┌──────────────┐         ┌──────────────┐
│ Product Mgr  │           │ Backend Dev  │         │ QA Tester    │
│ (产品经理)   │           │  (后端)      │         │  (测试)      │
└──────────────┘           └──────────────┘         └──────────────┘
       │                          │                        │
       │                    ┌──────────────┐         ┌──────────────┐
       │                    │ Frontend Dev │         │ CodeReviewer │
       │                    │  (前端)      │         │  (审查)      │
       │                    └──────────────┘         └──────────────┘
       │                          │                        │
┌──────────────┐           ┌──────────────┐         ┌──────────────┐
│  Architect   │           │     PM       │         │ Diagnostic   │
│  (架构师)    │           │ (项目经理)   │         │ Analyst      │
└──────────────┘           └──────────────┘         └──────────────┘
       │                          │                        │
       └──────────────────────────┼────────────────────────┘
                                  │
                        ┌──────────────┐
                        │Project Manager│
                        │  (统一入口)   │
                        └──────────────┘
```

### 🎤 讲解要点（培训师话术）

#### **开场类比（1分钟）**

> "传统开发：1人单干（需求→设计→开发→测试）
> Agent Team：10人虚拟团队（分工协作）
>
> 就像真实团队：
> - PM 不写代码，只协调
> - Backend 不写前端，专注后端
> - Reviewer 不开发，只审查
> - Diagnostic 不修改，只诊断"

#### **角色职责讲解（3分钟）**

**打开 README.md 展示角色表**：

| 角色 | 职责 | Skill 文件 | 触发方式 |
|------|------|-----------|---------|
| **Product Manager** | 需求收集、优先级、PRD | `product-manager/SKILL.md` | `/product-manager` |
| **Architect** | 架构设计、契约、技术选型 | `architect/SKILL.md` | `/architect` |
| **Project Manager** | 统一入口、协调、提交 | `project-manager/SKILL.md` | `/pm`（默认） |
| **Backend Dev** | Python+FastAPI实现 | `backend-dev/SKILL.md` | `/backend` |
| **Frontend Dev** | React+TS+TailwindCSS | `frontend-dev/SKILL.md` | `/frontend` |
| **QA Tester** | 测试策略、单元/集成测试 | `qa-tester/SKILL.md` | `/qa` |
| **Code Reviewer** | 代码审查、架构检查 | `code-reviewer/SKILL.md` | `/reviewer` |
| **Diagnostic Analyst** | 问题根因分析 | `diagnostic-analyst/SKILL.md` | `/diagnostic` |

**关键设计**：
- **决策层**：PdM、Arch、PM（不写代码）
- **执行层**：Backend、Frontend、PM（写代码）
- **支持层**：QA、Reviewer、Diagnostic（验证质量）

#### **文件边界规则（3分钟）⭐⭐⭐**

**展示边界矩阵**（从 README.md）：

```
┌────────────────────────────────────────────────┐
│ 文件所有权矩阵（避免协作冲突）                  │
└────────────────────────────────────────────────┘

src/          → Backend ✅ 全权 | Frontend ❌ 禁止
web-front/    → Backend ❌ 禁止 | Frontend ✅ 全权
tests/        → QA ✅ 全权      | Backend ⚠️ 协助
docs/arch/    → Architect ✅    | PM ⚠️ 只读
docs/planning/→ PM ✅ 全权      | All ⚠️ 只读
```

**强调红线**：
> "文件边界是协作的核心设计：
> - Backend 严禁修改 web-front/（避免前端冲突）
> - Frontend 严禁修改 src/（避免后端冲突）
> - QA 只改 tests/，不改业务代码
> - Reviewer 只审查，不修改代码"

**冲突解决流程**：
```
发现冲突 → 停止修改 → 通知 PM → PM重新分配 → 验证无冲突
```

#### **开工/收工检查清单（2分钟）**

**展示模板**：

```markdown
## Backend Dev 开工前检查
- [ ] 契约阅读：已阅读 OpenAPI Spec
- [ ] 类型导入：已从契约生成类型定义
- [ ] 接口确认：明确请求/响应 Schema
- [ ] 测试定位：确定测试文件路径

## Backend Dev 收工前检查
- [ ] 单元测试：覆盖率 ≥ 80%
- [ ] 契约一致性：实现与 Spec 一致 ⭐
- [ ] 异步检查：无同步阻塞调用
- [ ] 代码简化：已调用 code-simplifier
```

> "检查清单强制质量：
> - 开工前：理解需求、确认边界
> - 收工前：自验证、格式化、提交规范
> - 违反处理：Reviewer 标记 P0 问题"

#### **并行调度规范（1分钟）⭐⭐⭐**

**展示三条红线**（从 WORKFLOW.md）：

```
【PM 三条红线】违反 = P0 问题

1. ❌ 禁止代替执行 → 启动子Agent后，PM禁止自己写代码
2. ❌ 禁止串行 → 无依赖任务必须并行启动
3. ❌ 禁止空返回 → 子Agent必须有工具调用记录
```

**正确示例**（并行调度）：
```python
# ✅ 正确：同一消息中并行启动
Agent(subagent_type="backend-dev", prompt="...")
Agent(subagent_type="frontend-dev", prompt="...")
Agent(subagent_type="qa-tester", prompt="...")
```

**错误示例**（串行执行）：
```python
# ❌ 错误：串行启动（浪费时间）
Agent(subagent_type="backend-dev", prompt="...")
# 等待完成...
Agent(subagent_type="frontend-dev", prompt="...")
```

> "并行调度是效率的关键：
> - Backend 和 Frontend 可以同时开发（无依赖）
> - QA 和 Backend 不能并行（QA需等Backend完成）
> - 使用 TaskCreate + TaskUpdate 管理依赖"

---

## 🎯 培训实战Demo设计（10分钟）

### 演示案例：待办事项API开发

**目标**：展示三层记忆 + Agent协作 + MCP/Skill使用

#### **阶段 1：需求与架构（4分钟）**

```bash
# 演示三层记忆恢复
/kaigong

→ 输出：
  ✓ 读取 Memory MCP（架构红线）
  ✓ 读取 progress.md（最近进度）
  ✓ 读取 findings.md（技术发现）

# 演示 MCP 能力
mcp__sqlite__query: "SELECT name FROM sqlite_master WHERE type='table'"

# 演示 Skill 使用
/product-manager "我想做一个待办事项API"

→ PdM 调用 brainstorming skill
→ 交互式澄清需求（至少3个问题）
→ 输出 PRD 文档
```

#### **阶段 2：并行开发（4分钟）**

```bash
# 演示文件边界规则
/architect "设计待办事项API架构"

→ Architect 输出 OpenAPI Spec（契约）⭐
→ Memory MCP 写入架构决策（永久保留）⭐

# 演示并行调度
/pm "按照契约并行开发"

→ PM 并行启动（同一消息）：
  Agent(subagent_type="backend-dev", ...)
  Agent(subagent_type="frontend-dev", ...)
  Agent(subagent_type="qa-tester", ...)

→ 展示文件边界：
  Backend 只修改 src/
  Frontend 只修改 web-front/
  QA 只修改 tests/
```

#### **阶段 3：审查与收工（2分钟）**

```bash
# 演示开工/收工检查清单
/reviewer

→ Reviewer 检查清单：
  ✓ 契约一致性（对照 OpenAPI Spec）
  ✓ 架构检查（domain层纯净性）
  ✓ 安全检查（无SQL注入）

# 演示三层记忆写入
/shougong

→ 自动执行：
  ✓ 写入 Memory MCP（架构决策）
  ✓ 更新 progress.md（今日进度）
  ✓ Git 提交（不推送）
```

---

## 📋 常见问题预案（3分钟）

| 问题 | 答案要点 |
|------|---------|
| **MCP配置复杂怎么办？** | 先配置 SQLite + FileSystem，后续逐步添加 |
| **Skill如何定制？** | 复制 template-skill，修改 SKILL.md |
| **上下文超限怎么办？** | 启用归档机制（progress 3天、findings 7天） |
| **Agent冲突怎么办？** | 文件边界规则 + PM重新分配 |
| **成本太高怎么办？** | Sonnet性价比高，并行缩短总时间 |
| **质量可靠吗？** | Reviewer审查 + 测试覆盖 + 开工/收工检查 |

---

## 🎯 培训收尾（1分钟）

**核心价值总结**：

```
传统开发 vs Claude Code Vibe Coding

传统开发：
  1人单干 → 需求→设计→开发→测试→交付
  时间：2天完成待办API

Claude Code：
  10人虚拟团队 → 分工协作 + 强制规范 + 三层记忆
  时间：10分钟需求到交付 ⭐⭐⭐
```

**不是替代程序员，而是扩展能力边界**：
- 从1人单干 → 10人虚拟团队
- 从随机质量 → 强制规范
- 从手动文档 → 三层记忆

**培训结束语**（v2.0 更新）：
> "Claude Code 的本质是：
> 用第一性原理 理解 LLM 的物理约束
> 用 MCP 装上手眼
> 用 Skill 教会流程
> 用 Agent Team 分工协作
> 用三层记忆 防止遗忘
> 用复利工程 让系统自我改进
>
> 这不是魔法，而是工程化的AI协作规范。
> AI 就像一件乐器——只有刻意练习的人才能弹好。"