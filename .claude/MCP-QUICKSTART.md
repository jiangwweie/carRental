# MCP Servers 配置指南

> **最后更新**: 2026-04-10
> **适用项目**: 租车应用（Car Rental）

---

## 当前已配置的 MCP 服务器

查看 `settings.local.json` 中的 `enabledMcpjsonServers`:

```json
{
  "enabledMcpjsonServers": [
    "filesystem",  // 文件读写、目录操作
    "git",         // Git 版本控制
    "sqlite",      // SQLite 数据库查询
    "pup",         // Puppeteer 无头浏览器
    "memory"       // 知识图谱记忆
  ]
}
```

**全局配置文件位置**: `~/.claude/mcp.json`

---

## MCP 服务器分类与使用场景

### 一、核心基础设施

#### 1. Memory MCP (知识图谱与长期记忆) 🧠

**应用场景**: 项目架构决策、技术选型、踩坑记录需要在多次会话间保持一致。

**实战效果**:
```
用户：记住订单状态机的流转规则：待确认 → 已确认/已拒绝

→ 下一次对话中:
AI 会自动遵循这条状态机规则，不会建议非法的状态跳转
```

**配置文件**:
```json
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}
```

---

#### 2. File System MCP (文件操作) 🔍

**应用场景**: 读取/修改代码文件、项目文档。

**当前配置**: 已启用 `filesystem` MCP

**可用命令**:
- `mcp__filesystem__search_files` - 递归搜索文件
- `mcp__filesystem__read_text_file` - 读取文件
- `mcp__filesystem__read_multiple_files` - 批量读取
- `Grep` 工具 - 正则内容搜索

---

#### 3. Git MCP (版本控制) 📦

**应用场景**: 查看 git 状态、提交记录、分支信息。

**当前配置**: 已启用 `git` MCP

---

### 二、外部交互

#### 4. Puppeteer MCP (无头浏览器) 🤖

**应用场景**: 前端 UI 测试、页面截图、E2E 验证。

**当前配置**: 已启用 `pup` MCP

**使用示例**:
```
用户：访问 http://localhost:5173 并截图

→ AI 控制无头浏览器 → 截取页面 → 返回截图
```

---

### 三、可选增强

以下服务器可按需添加：

| MCP 服务器 | 用途 | 优先级 |
|------------|------|--------|
| `duckdb` | OLAP 数据分析 | 中（需要大数据分析时） |
| `time` | 时区/时间戳处理 | 低（Python 标准库可替代） |

---

## 权限管理最佳实践

### 安全原则

1. **最小权限**: 仅授予必要操作的权限
2. **分类授权**: destructive 操作（删除、强制推送）单独审批
3. **审计日志**: 记录所有 MCP 调用历史

### 当前权限配置示例

查看 `settings.local.json` 的 `permissions.allow`:

```json
{
  "permissions": {
    "allow": [
      "mcp__filesystem__*",     // 文件系统操作
      "mcp__sqlite__*",          // 数据库查询
      "Bash(git:*)",             // Git 操作
      "Bash(python3:*)",         // Python 执行
      "Bash(curl:*)",            // API 调试
      "WebSearch",               // 网络搜索
      "WebFetch(domain:*)"       // 网页读取
    ]
  }
}
```

---

## 故障排查

### 问题：MCP 工具返回错误

**排查步骤**:
1. 检查 `~/.claude/mcp.json` 配置是否正确
2. 检查 `settings.local.json` 中 `enabledMcpjsonServers` 是否包含该服务器
3. 检查 `permissions.allow` 是否有对应权限
4. 检查文件/数据库路径是否在允许目录内

---

## 快捷命令参考

### Git 工作流

```bash
# 查看当前会话的所有修改
git diff HEAD

# 按模块分类提交
git add backend/app/services/     # 业务逻辑
git commit -m "feat(order): xxx"

git add backend/app/models/       # 数据模型
git commit -m "feat(model): xxx"

# 推送到远程分支
git push origin feature/xxx
```

---

*项目：租车应用（Car Rental）*
*维护者：AI Builder*
