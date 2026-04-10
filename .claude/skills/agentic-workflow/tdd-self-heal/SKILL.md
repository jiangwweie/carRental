# TDD 闭环自愈技能 (Test-Driven Self-Correction)

> **技能类型**: 自动开发 + 自我修复
> **适用场景**: 业务逻辑复杂、边界条件多的功能
> **预期收益**: 阻挡 90% 低级逻辑漏洞

---

## 技能描述

本技能赋予 AI 完整的 TDD 闭环能力：
1. 解析契约文档中的测试用例清单
2. 生成 pytest 测试代码
3. 执行测试并分析失败
4. 自我修复业务代码
5. 直到所有测试通过才提交

---

## 权限要求

```json
{
  "permissions": {
    "allow": [
      "Bash(pytest:*)",
      "Bash(python3 -c:*)",
      "Read(//Users/jiangwei/Documents/final/tests/**)",
      "Read(//Users/jiangwei/Documents/final/src/**)",
      "Write(//Users/jiangwei/Documents/final/tests/**)",
      "Write(//Users/jiangwei/Documents/final/src/**)"
    ]
  }
}
```

---

## 工作流详解

### 阶段 0: 接收任务

**用户输入示例**:
```
实现 BookingService 的订单状态流转功能
契约文档：docs/v3/phase3-booking-state-machine-contract.md
测试用例：UT-005 ~ UT-008
```

### 阶段 1: 契约解析

**AI 执行**:
1. 读取契约文档
2. 提取测试用例清单
3. 理解每个用例的 Given/When/Then

**契约表示例**:
| 用例 ID | 测试场景 | 前置条件 | 触发条件 | 预期结果 | 优先级 |
|---------|----------|----------|----------|----------|--------|
| UT-005 | 订单确认 | 订单状态为 PENDING | 管理员确认 | 状态变为 CONFIRMED | 高 |
| UT-006 | 重复确认 | 订单状态已为 CONFIRMED | 管理员再次确认 | 抛出状态流转异常 | 中 |

### 阶段 2: 生成测试代码

**AI 输出**:
```python
# tests/unit/test_booking_service_state_transition.py

import pytest
from decimal import Decimal
from src.domain.booking_service import BookingService
from src.domain.models import Booking, BookingStatus

class TestBookingStateTransition:
    """订单状态流转测试 - 基于契约 UT-005 ~ UT-008"""

    @pytest.mark.asyncio
    async def test_ut_005_confirm_booking_transitions_to_confirmed(self):
        """UT-005: 待确认订单经确认后变为已确认"""
        # Given: 待确认的订单
        booking = Booking(
            vehicle_id="VEH-001",
            user_id="USER-001",
            start_date="2026-04-15",
            end_date="2026-04-18",
            total_price=Decimal("900.00"),
            status=BookingStatus.PENDING,
        )

        # When: 管理员确认订单
        booking_service = BookingService()
        await booking_service.confirm_booking(booking)

        # Then: 订单状态变为已确认
        assert booking.status == BookingStatus.CONFIRMED

    @pytest.mark.asyncio
    async def test_ut_006_double_confirm_raises_error(self):
        """UT-006: 已确认订单不能再次确认"""
        # Given: 已确认的订单
        booking = Booking(
            vehicle_id="VEH-001",
            user_id="USER-001",
            start_date="2026-04-15",
            end_date="2026-04-18",
            total_price=Decimal("900.00"),
            status=BookingStatus.CONFIRMED,
        )

        # When: 尝试再次确认
        booking_service = BookingService()

        # Then: 抛出状态流转异常
        with pytest.raises(InvalidStateTransitionError):
            await booking_service.confirm_booking(booking)
```

### 阶段 3: 运行测试 (Red)

**AI 执行**:
```bash
cd /Users/jiangwei/Documents/demo
source venv/bin/activate
pytest tests/unit/test_booking_service_state_transition.py -v
```

**预期输出** (失败):
```
FAILED test_booking_service_state_transition.py::TestBookingStateTransition::test_ut_005_confirm_booking_transitions_to_confirmed
AssertionError: assert BookingStatus.PENDING == BookingStatus.CONFIRMED
```

### 阶段 4: 实现业务代码 (Green)

**AI 分析失败原因并实现**:
```python
# src/domain/booking_service.py

from decimal import Decimal
from src.domain.models import Booking, BookingStatus

class InvalidStateTransitionError(Exception):
    """非法的状态流转异常"""
    pass

class BookingService:
    """订单服务管理器"""

    ALLOWED_TRANSITIONS = {
        BookingStatus.PENDING: [BookingStatus.CONFIRMED, BookingStatus.CANCELLED],
        BookingStatus.CONFIRMED: [BookingStatus.ACTIVE, BookingStatus.CANCELLED],
        BookingStatus.ACTIVE: [BookingStatus.COMPLETED, BookingStatus.CANCELLED],
        BookingStatus.CANCELLED: [],
        BookingStatus.COMPLETED: [],
    }

    async def confirm_booking(self, booking: Booking) -> bool:
        """确认订单"""
        # 检查状态流转是否合法
        if booking.status not in self.ALLOWED_TRANSITIONS:
            raise InvalidStateTransitionError(f"未知状态: {booking.status}")

        if BookingStatus.CONFIRMED not in self.ALLOWED_TRANSITIONS[booking.status]:
            raise InvalidStateTransitionError(
                f"不允许从 {booking.status} 流转到 CONFIRMED"
            )

        booking.status = BookingStatus.CONFIRMED
        booking.confirmed_at = datetime.now(timezone.utc)
        return True
```

### 阶段 5: 自愈合循环

**AI 执行**:
```bash
pytest tests/unit/test_booking_service_state_transition.py -v
```

**如果通过** ✅ → 进入重构阶段
**如果失败** ❌ → 分析 Traceback，返回阶段 4

**失败分析示例**:
```
分析：测试失败是因为 confirm_booking() 未更新订单状态

根本原因：缺少状态流转逻辑

修复方案：
1. 添加 ALLOWED_TRANSITIONS 状态流转规则
2. 在 confirm_booking() 中检查并更新状态
3. 重新运行测试
```

### 阶段 6: 重构与提交

**AI 执行**:
```bash
# 类型检查
mypy src/domain/booking_service.py

# 格式化
black src/domain/booking_service.py

# 提交
git add src/domain/booking_service.py tests/unit/test_booking_service_state_transition.py
git commit -m "feat(domain): 实现订单状态流转逻辑 (UT-005 ~ UT-008)

- 添加 BookingService.confirm_booking() 方法
- 定义 ALLOWED_TRANSITIONS 状态流转规则
- 非法状态流转抛出 InvalidStateTransitionError
- 100% 测试覆盖

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 技能触发条件

当用户指令包含以下关键词时激活此技能：

| 关键词 | 触发行为 |
|--------|----------|
| "实现 X 功能，契约是..." | 解析契约 → 生成测试 → 实现代码 |
| "为 X 编写测试，遵循..." | 生成测试 → 运行 → 修复 |
| "修复这个 Bug，测试用例是..." | 生成回归测试 → 修复 → 验证 |
| "/tdd [功能描述]" | 完整 TDD 流程 |

---

## 输出格式

**技能执行中的状态更新**:
```
🔴 Red: 测试已生成，等待运行
  └─ 已创建 4 个测试用例
  └─ 运行 pytest...

🟢 Green: 测试通过
  └─ 4 passed in 0.52s
  └─ 正在格式化代码...

🔵 Refactor: 重构完成
  └─ mypy 检查通过
  └─ 已提交 git: abc123
```

**最终汇报**:
```markdown
## TDD 闭环完成

### 实现功能
BookingService 订单状态流转逻辑

### 测试覆盖
- ✅ UT-005: 待确认订单确认后变为已确认
- ✅ UT-006: 已确认订单不能再次确认
- ✅ UT-007: 已取消订单不能流转
- ✅ UT-008: 非法状态流转抛出异常

### 代码变更
- `src/domain/booking_service.py` (+45 行)
- `tests/unit/test_booking_service_state_transition.py` (+89 行)

### 提交
- abc1234 feat(domain): 实现订单状态流转逻辑
```

---

## 与规划文件的集成

执行此技能时，自动更新规划文档：

```bash
# 更新 task_plan.md
- 标记当前阶段为 completed
- 更新下一阶段的 blockedBy 状态

# 更新 progress.md  
- 记录本次 TDD 循环的迭代次数
- 记录遇到的典型错误模式
```

---

*技能版本：v1.0*
*创建日期：2026-04-01*
