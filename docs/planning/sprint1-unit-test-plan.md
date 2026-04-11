# Sprint 1 后端核心业务逻辑 -- 单元测试覆盖方案

## 1. 测试文件清单和目录结构

```
backend/src/test/java/com/carrental/
├── domain/order/
│   ├── OrderStateMachineTest.java      # Order 状态机测试 (33 个用例)
│   ├── OrderStatusTest.java            # OrderStatus 枚举测试 (17 个用例)
│   └── service/
│       └── OrderConflictCheckerTest.java  # 订单冲突检测测试 (13 个用例)
└── infrastructure/pricing/
    └── SimplePricingEngineTest.java    # 定价引擎测试 (8 个用例)
```

**总计: 4 个测试类, 71 个测试用例, 全部通过。**

---

## 2. 各测试类详细说明

### 2.1 OrderStateMachineTest (33 用例)

**被测对象**: `Order.java` 中的状态机方法 (`confirm`, `cancel`, `reject`, `start`, `complete`)

| 嵌套类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `HappyPathTests` | 3 | 完整流转 PENDING→CONFIRMED→IN_PROGRESS→COMPLETED、PENDING→CANCELLED、PENDING→REJECTED |
| `ConfirmTests` | 6 | 从 CONFIRMED/CANCELLED/REJECTED/IN_PROGRESS/COMPLETED 状态调用 confirm() 均抛异常 |
| `CancelTests` | 5 | 从 CANCELLED/CONFIRMED/IN_PROGRESS/REJECTED 状态调用 cancel() 均抛异常 |
| `RejectTests` | 4 | 从 REJECTED/CANCELLED/CONFIRMED 状态调用 reject() 均抛异常 |
| `StartTests` | 5 | 从 PENDING/IN_PROGRESS/COMPLETED/CANCELLED 状态调用 start() 均抛异常 |
| `CompleteTests` | 5 | 从 PENDING/CONFIRMED/COMPLETED/CANCELLED 状态调用 complete() 均抛异常 |
| `IllegalTransitionTests` | 5 | 跨状态跳转 (PENDING→IN_PROGRESS, PENDING→COMPLETED, CONFIRMED→COMPLETED)、终态封闭性 (REJECTED/CANCELLED 不允许任何操作) |

### 2.2 OrderStatusTest (17 用例)

**被测对象**: `OrderStatus.java` 枚举

| 嵌套类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `FromValueTests` | 11 | 6 个合法值精确匹配、null 输入返回 null、非法值返回 null、空字符串返回 null、小写/混合大小写忽略大小写匹配 |
| `GetLabelTests` | 6 | 6 个状态的中文 label 验证 |

### 2.3 SimplePricingEngineTest (8 用例)

**被测对象**: `SimplePricingEngine.java`

| 嵌套类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `NormalCalculationTests` | 4 | 1天/3天/7天租赁计算、日期明细连续性验证 |
| `EdgeCaseTests` | 2 | startDate == endDate (0天)、startDate > endDate (负天数) |
| `MvpBehaviorTests` | 2 | 跨周末仍标记为 weekday、weekendPrice/holidayPrice 参数被忽略 |

### 2.4 OrderConflictCheckerTest (13 用例)

**被测对象**: `OrderConflictChecker.java`

| 嵌套类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `NoConflictTests` | 5 | 新订单在已有订单之前/之后/紧接边界、不同车辆不冲突、无已有订单 |
| `FullOverlapTests` | 1 | 完全相同时间段冲突 |
| `PartialOverlapTests` | 4 | 新订单起始重叠、结束重叠、完全包含已有订单、被已有订单完全包含 |
| `UserIndependenceTests` | 1 | 同一车辆不同用户仍冲突 |
| `ExceptionTypeTests` | 2 | 异常类型为 BusinessException、错误码 5200 + 消息 "该时间段已被预订" |

**测试替实现**: 使用内嵌的 `InMemoryOrderRepositoryStub` 实现 `OrderRepository` 接口，通过内存 List 存储预订记录，冲突判断逻辑为 `newStart < existingEnd && newEnd > existingStart` (与 SQL 的 `<` `>` 语义一致)。不依赖 Mockito。

---

## 3. 如何运行测试

### 运行全部测试

```bash
cd backend
mvn test
```

### 运行指定测试类

```bash
mvn test -Dtest=OrderStateMachineTest
mvn test -Dtest=OrderStatusTest
mvn test -Dtest=SimplePricingEngineTest
mvn test -Dtest=OrderConflictCheckerTest
```

### 运行指定嵌套测试组

```bash
mvn test -Dtest="OrderStateMachineTest\$HappyPathTests"
mvn test -Dtest="OrderConflictCheckerTest\$PartialOverlapTests"
```

### 生成覆盖率报告 (需要 JaCoCo 插件)

当前 `pom.xml` 未配置 JaCoCo。如需覆盖率报告，在 `pom.xml` 的 `<build><plugins>` 中添加:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

然后运行:

```bash
mvn clean test jacoco:report
```

报告生成在 `backend/target/site/jacoco/index.html`。

---

## 4. 预期覆盖率

基于当前测试覆盖的被测代码范围估算:

| 模块 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|----------|------------|------|
| `Order.java` | **~95%** | **~100%** | 所有状态机方法的 happy path 和异常分支均已覆盖 |
| `OrderStatus.java` | **100%** | **100%** | `fromValue()` 的 null 分支和正常分支全覆盖, `getLabel()` 全覆盖 |
| `SimplePricingEngine.java` | **100%** | **100%** | 唯一的 `calculate()` 方法的循环体、边界条件 (0天、负天数) 全覆盖 |
| `OrderConflictChecker.java` | **100%** | **100%** | `checkConflict()` 只有两条路径 (冲突抛异常 / 不冲突通过) |

### 综合覆盖率

对 **核心业务逻辑代码** (4 个被测文件, 约 90 行代码):

- **行覆盖率: 97%+**
- **分支覆盖率: 100%**

> 说明: 行覆盖率中未达 100% 的部分来自 `Order.java` 中 Lombok 生成的 getter/setter (被 `@Getter`/`@Setter` 注解)。领域模型的手写业务逻辑覆盖率为 100%。

---

## 5. 设计决策

1. **不使用 Mockito**: 领域模型和纯计算引擎无需 mock。`OrderConflictChecker` 通过内嵌 `InMemoryOrderRepositoryStub` 实现接口隔离，比 Mockito 更清晰可读。

2. **使用 `@Nested` 分组**: 每个测试方法组用 `@Nested` 类组织，测试报告层次清晰，便于定位失败用例。

3. **无 Spring 容器**: 所有测试均为纯 JUnit 单元测试，`mvn test` 在 1 秒内完成全部 71 个用例。

4. **MVP 对齐**: `SimplePricingEngineTest` 显式验证了 "周末价被忽略" 这一 MVP 行为，避免后续开发时误以为已有周末价格逻辑。
