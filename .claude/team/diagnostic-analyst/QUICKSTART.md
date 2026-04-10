# 诊断分析师快速使用指南

> **创建日期**: 2026-03-28
> **角色**: Diagnostic Analyst (`/diagnostic`)

---

## 何时使用诊断分析师

### ✅ 适合的场景

| 场景 | 示例 |
|------|------|
| **疑难杂症** | "为什么订单费用计算只有 0.01 元？" |
| **共性问题排查** | "API 返回 503 错误，是否还有其他地方有同样问题？" |
| **系统性分析** | "订单重复/丢失的原因是什么？" |
| **架构一致性审查** | "这段代码是否符合 Clean Architecture？" |
| **技术债识别** | "系统中有哪些潜在的技术债？" |

### ❌ 不适合的场景

| 场景 | 应该使用的角色 |
|------|----------------|
| **新功能开发** | Team PM / Backend Dev |
| **代码审查** | Code Reviewer |
| **编写测试** | QA Tester |
| **前端实现** | Frontend Dev |

---

## 使用方法

### 方式 1: Slash Command（推荐）

```bash
/diagnostic
```

然后描述你的问题：

```
我发现历史订单的费用异常低，只有 0.01 元级别，帮我分析一下原因
```

### 方式 2: 直接描述问题

```
帮我诊断一下为什么 /api/strategies/preview 接口返回 503 错误
```

诊断分析师会自动激活并输出诊断报告。

### 方式 3: 从 PM 分配

```
/pm
我发现订单费用计算有问题，先让 diagnostic 分析一下根因
```

---

## 诊断报告示例

```markdown
# 诊断报告：订单费用计算异常问题

**报告编号**: DA-20260328-001
**优先级**: 🔴 P0
**状态**: 已完成

---

## 问题描述

| 字段 | 内容 |
|------|------|
| 用户报告 | 历史订单费用仅 0.01 元级别 |
| 影响范围 | 所有短租订单 |
| 出现频率 | 必现 |

---

## 根因定位

**根本原因**: PriceFilterDynamic.check() 方法是占位符实现，始终返回 passed=True

**问题代码位置**: `backend/app/domain/filter_factory.py:387-401`

**5 Why 分析**:
```
Why 1: 为什么费用这么低？
  → 因为 calculate_rental_cost() 直接用 base_price

Why 2: 为什么没有附加费用？
  → 因为函数没有接收保险参数

Why 3: 为什么不用保险计算？
  → 因为 PriceFilterDynamic 的 check() 是占位符实现

Why 4: 为什么是占位符？
  → 因为开发优先级安排，先实现框架后填充逻辑

Why 5: 为什么框架有了但逻辑没实现？
  → 因为任务分解时价格过滤器被列为"可选增强"
```

---

## 修复方案

### 方案 A [推荐]

**修改内容**:
```
文件：backend/app/domain/filter_factory.py
位置：第 387-401 行
当前代码：
    def check(self, pattern, context):
        return TraceEvent(passed=True, reason="price_check_met")

修改为：
    def check(self, pattern, context):
        base_price = self._get_base_price(order.vehicle_type, order.days)
        insurance_cost = order.insurance * self._insurance_rate
        min_price = base_price + insurance_cost
        if order.total < min_price:
            return TraceEvent(passed=False, reason="insufficient_pricing")
        return TraceEvent(passed=True, reason="price_check_met")
```

**优点**: 从根本上解决波动率不足的问题
**缺点**: 需要配置 min_atr_ratio 参数
**工作量**: 2-3 小时

### 方案 B

...
```

---

## 诊断分析师的工作流程

```
1. 接收问题
       ↓
2. 问题澄清（可能向你提问）
       ↓
3. 生成初步假设
       ↓
4. 系统性排查（Read/Grep/Glob/运行测试）
       ↓
5. 根因确认（5 Why 分析）
       ↓
6. 输出诊断报告（包含 A/B/C 多方案）
       ↓
7.  PM 分配任务给 Dev 实施
```

---

## 与其他角色的区别

| 角色 | 职责 | 核心差异 |
|------|------|----------|
| **Diagnostic Analyst** | 问题诊断 | 只分析，不修改代码 |
| **Code Reviewer** | 代码审查 | 审查已完成的代码 |
| **Backend Dev** | 功能实现 | 修改业务代码 |
| **QA Tester** | 测试验证 | 编写测试用例 |

---

## 注意事项

### ⚠️ 诊断分析师不会做什么

- ❌ 直接修改业务代码
- ❌ 创建新的业务功能
- ❌ 删除文件或代码

### ✅ 诊断分析师会做什么

- ✅ 读取任意文件进行分析
- ✅ 运行测试验证假设
- ✅ 创建诊断报告文档
- ✅ 输出详细的修复方案（文字描述）

---

## 典型协作流程

```
用户 → Diagnostic Analyst: 报告问题
                     ↓
            输出诊断报告
                     ↓
用户 → PM: 阅读报告，请求实施
                     ↓
         PM → Backend Dev: 分配修复任务
                     ↓
            Backend Dev 实施修复
                     ↓
              QA Tester 验证
                     ↓
           Code Reviewer 审查
```

---

## 示例问题

以下是一些适合问诊断分析师的问题：

### 业务逻辑问题
- "为什么我的订单没有触发确认通知？"
- "为什么价格过滤器没有生效？"
- "为什么库存缓存没有命中？"

### API 错误问题
- "/api/orders/preview 返回 503 错误，原因是什么？"
- "/api/account 返回 unavailable 状态，是否正常？"

### 架构问题
- "这段代码是否符合 Clean Architecture？"
- "有没有循环导入的问题？"

### 技术债问题
- "系统中有哪些潜在的技术债？"
- "哪些地方需要优化？"

---

*诊断分析师 - 你的系统医生*
