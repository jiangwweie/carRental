# 管理后台前端设计文档

**日期**: 2026-04-11
**范围**: 车辆新增/编辑、价格设置、协议管理
**状态**: 设计评审中

---

## 一、任务概览

| # | 任务 | 文件 | 后端状态 | 预估工时 |
|---|------|------|---------|---------|
| 1 | 车辆新增/编辑弹窗 | `VehicleView.vue` | ✅ API 已就绪 | 1h |
| 2 | 价格设置页 | `PricingView.vue` | ⚠️ 需新建 API | 2h |
| 3 | 协议管理页 | `AgreementView.vue` | ✅ API 已就绪 | 0.5h |

**依赖关系**: 任务 1 和 3 可并行开发；任务 2 依赖后端新建 API。

---

## 二、任务 1 — 车辆新增/编辑弹窗

### 2.1 后端 API 契约（已就绪，无需改动）

| 端点 | 方法 | 请求体 | 响应 |
|------|------|--------|------|
| `/api/v1/admin/vehicles` | POST | `CreateVehicleRequest` | `ApiResponse<Vehicle>` |
| `/api/v1/admin/vehicles/{id}` | PUT | `UpdateVehicleRequest` | `ApiResponse<Vehicle>` |

**CreateVehicleRequest 字段**:

```java
{
  "name": String,          // 必填，车型名称
  "brand": String,         // 必填，品牌
  "seats": Integer,        // 必填，座位数
  "transmission": String,  // 必填，变速箱类型
  "description": String,   // 可选，描述
  "images": [String],      // 可选，图片 base64 列表
  "tags": [String],        // 可选，标签列表
  "weekdayPrice": BigDecimal,  // 必填，工作日价
  "weekendPrice": BigDecimal,  // 必填，周末价
  "holidayPrice": BigDecimal   // 必填，节假日价
}
```

**UpdateVehicleRequest**: 所有字段可选（支持部分更新）。

### 2.2 组件设计

```
VehicleView.vue（现有文件，增量修改）
├── el-dialog（新增/编辑弹窗）
│   └── el-form
│       ├── el-input: name
│       ├── el-input: brand
│       ├── el-input-number: seats
│       ├── el-select: transmission (自动挡/手动挡)
│       ├── el-input: description (textarea)
│       ├── el-input: images (textarea，每行一个 base64 URL)
│       ├── el-select: tags (multiple，预设选项)
│       ├── el-input-number: weekdayPrice
│       ├── el-input-number: weekendPrice
│       └── el-input-number: holidayPrice
└── el-table: 操作列增加「编辑」按钮
```

### 2.3 状态管理

**仅组件局部状态**，无需 Pinia：

```javascript
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const form = ref({
  name: '',
  brand: '',
  seats: 3,
  transmission: '自动挡',
  description: '',
  images: '',          // 编辑时用 '\n' 分隔
  tags: [],
  weekdayPrice: 0,
  weekendPrice: 0,
  holidayPrice: 0
})
const formRef = ref(null)  // el-form 引用
```

### 2.4 校验规则

```javascript
const rules = {
  name: [{ required: true, message: '请输入车型名称', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  seats: [{ required: true, type: 'number', min: 1, message: '座位数必须 > 0', trigger: 'blur' }],
  transmission: [{ required: true, message: '请选择变速箱类型', trigger: 'change' }],
  weekdayPrice: [{ required: true, type: 'number', min: 0.01, message: '价格必须 > 0', trigger: 'blur' }],
  weekendPrice: [{ required: true, type: 'number', min: 0.01, message: '价格必须 > 0', trigger: 'blur' }],
  holidayPrice: [{ required: true, type: 'number', min: 0.01, message: '价格必须 > 0', trigger: 'blur' }]
}
```

### 2.5 关键交互逻辑

1. **新增**: 点击「新增车辆」→ 清空 form → 打开 dialog → 提交调用 `POST /api/v1/admin/vehicles`
2. **编辑**: 点击「编辑」→ 填充 form（images 数组 → '\n' 分隔字符串）→ 提交调用 `PUT /api/v1/admin/vehicles/{id}`
3. **图片处理**: 前端 textarea 输入（每行一个），提交时 `images.split('\n').filter(Boolean)` 转为数组

---

## 三、任务 2 — 价格设置页

### 3.1 后端 API 设计（需新建）

MVP 阶段无需独立定价 API，直接复用车辆 PUT 接口批量更新价格。

**新建后端 Controller**:

| 端点 | 方法 | 请求体 | 响应 | 说明 |
|------|------|--------|------|------|
| `/api/v1/admin/vehicles/prices` | PUT | `BatchUpdatePriceRequest` | `ApiResponse<Void>` | 批量更新车辆价格 |

**BatchUpdatePriceRequest**:

```java
@Data
public static class BatchUpdatePriceRequest {
    private List<VehiclePriceItem> items;

    @Data
    public static class VehiclePriceItem {
        private Long id;               // 车辆 ID
        private BigDecimal weekdayPrice;   // 必填
        private BigDecimal weekendPrice;   // 必填
        private BigDecimal holidayPrice;   // 必填
    }
}
```

**实现要点**:
- 遍历 items，对每个 vehicleId 调用 vehicleService.updatePrice(id, prices)
- 使用 `@Transactional` 保证原子性
- 文件位置: 在 `AdminVehicleController` 新增端点

### 3.2 组件设计

```
PricingView.vue
├── 工具栏: 「保存全部」按钮
└── el-table (inline editing)
    ├── 车型 (只读)
    ├── 品牌 (只读)
    ├── 工作日价 (el-input-number)
    ├── 周末价 (el-input-number)
    ├── 节假日价 (el-input-number)
    └── 操作: 「重置」按钮（恢复原始值）
```

### 3.3 状态管理

```javascript
const vehicles = ref([])          // 所有车辆
const originalPrices = ref({})    // 原始价格快照 { vehicleId: { weekday, weekend, holiday } }
```

**加载**: `GET /api/v1/admin/vehicles?pageSize=100`（取全部车辆）
**保存**: 对比 `vehicles` 与 `originalPrices`，收集变更项 → 调用批量 API

### 3.4 关键交互逻辑

1. 页面加载: 获取全部车辆列表
2. 用户修改: 直接在表格单元格中编辑价格
3. 保存: 点击「保存全部」→ 收集有变化的车辆 → 调用 `PUT /api/v1/admin/vehicles/prices`
4. 重置: 点击「重置」→ 恢复原始价格快照

---

## 四、任务 3 — 协议管理页

### 4.1 后端 API 契约（已就绪）

| 端点 | 方法 | 请求体 | 响应 |
|------|------|--------|------|
| `/api/v1/agreement` | GET | — | `ApiResponse<AgreementDTO>` |
| `/api/v1/admin/agreement` | PUT | `UpdateAgreementRequest` | `ApiResponse<Void>` |

**AgreementDTO**:

```java
{
  "id": Long,
  "content": String,           // 协议全文
  "version": String,           // 版本号
  "updatedAt": LocalDateTime   // 更新时间
}
```

**UpdateAgreementRequest**:

```java
{
  "content": String            // 新协议内容
}
```

### 4.2 组件设计

```
AgreementView.vue
├── 信息栏: 显示版本号 + 更新时间
├── el-input (type="textarea", :rows="20"): 协议内容编辑器
└── 底部工具栏: 「保存」按钮 + 「预览」按钮
```

### 4.3 状态管理

```javascript
const content = ref('')
const version = ref('')
const updatedAt = ref(null)
```

**加载**: `GET /api/v1/agreement`
**保存**: `PUT /api/v1/admin/agreement` → `{ content }`

### 4.4 关键交互逻辑

1. 页面加载: 获取当前活跃协议 → 填充 textarea
2. 编辑: 在 textarea 中直接修改
3. 保存: 点击「保存」→ 确认对话框 → 调用 PUT API → 成功提示
4. 预览: 在新窗口/弹窗中展示最终渲染效果

---

## 五、文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend-admin/src/views/vehicles/VehicleView.vue` | 修改 | 新增 dialog + 编辑按钮 + 表单逻辑 |
| `frontend-admin/src/views/pricing/PricingView.vue` | 重写 | 内联编辑表格 + 批量保存 |
| `frontend-admin/src/views/agreement/AgreementView.vue` | 重写 | 协议编辑器 + 保存逻辑 |
| `backend/.../controller/AdminVehicleController.java` | 修改 | 新增批量更新价格端点 |

---

## 六、技术选型决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | 组件局部 ref | 3 个页面均无跨组件共享状态，无需 Pinia |
| 图片上传 | textarea + base64 | MVP 阶段简化方案，v1.5 迁移 OSS |
| 价格批量更新 | 新建后端 API | 逐辆更新体验差，需要原子性批量操作 |
| 协议编辑器 | textarea | MVP 不支持 Markdown，纯文本 |

---

## 七、测试策略

| 任务 | 测试内容 | 类型 |
|------|---------|------|
| 车辆新增 | 表单校验 → POST 调用 → 列表刷新 | 组件测试 |
| 车辆编辑 | 数据回填 → PUT 调用 → 列表刷新 | 组件测试 |
| 价格设置 | 修改 → 批量 API 调用 → 成功提示 | 组件测试 |
| 协议管理 | 加载 → 修改 → 保存确认 → PUT 调用 | 组件测试 |

---

*设计完成，等待确认后安排前端开发。*
