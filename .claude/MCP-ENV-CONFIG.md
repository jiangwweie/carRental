# MCP 服务器环境变量配置指南

> **配置日期**: 2026-04-01
> **文件位置**: `~/.claude/mcp.json`

---

## 已配置的 MCP 服务器清单

| 服务器 | 状态 | 用途 | 配置项 |
|--------|------|------|--------|
| **filesystem** | ✅ 已配置 | 文件操作/代码检索 | 路径已指向项目目录 |
| **git** | ✅ 已配置 | Git 版本控制 | 无需额外配置 |
| **sqlite** | ✅ 已配置 | 本地数据库查询 | `--db-path` 配置 |
| **pup** | ✅ 已配置 | 无头浏览器自动化 | 无需额外配置 |
| **memory** | ✅ 已配置 | 知识图谱记忆 | 无需额外配置 |

---

## 可选添加的 MCP 服务器

### 1. DuckDB (OLAP 数据分析)

**添加位置**: `~/.claude/mcp.json`
```json
"duckdb": {
  "command": "uvx",
  "args": ["mcp-server-duckdb", "--db-path", "/path/to/analysis.db"]
}
```

---

### 2. Time (时区/时间戳处理)

**添加位置**: `~/.claude/mcp.json`
```json
"time": {
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-time"]
}
```

---

## 重启 Claude Code 使配置生效

```bash
# 退出当前 Claude 会话
/exit

# 重新启动
claude
```

或者在桌面应用中：
1. 关闭 Claude Code 窗口
2. 重新打开应用
3. 验证新 MCP 服务器是否加载：`/help mcp`

---

*维护者：AI Builder*
*项目：租车应用（Car Rental）*
