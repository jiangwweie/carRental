# 项目进度追踪

## 2026-04-18 进度更新

### ✅ 已完成

#### 1. 命名规范统一（camelCase）
- **后端**：修改 Jackson 配置为 `LOWER_CAMEL_CASE`
- **小程序**：移除 `convertKeysToCamelCase` 转换逻辑
- **管理端**：修复所有 snake_case 参数
- **影响范围**：所有 API 请求和响应

#### 2. 参数格式统一
- **小程序**：
  - `booking.vue`：预订参数改为 camelCase
  - `index.vue`：分页参数改为 camelCase
- **管理端**：
  - `VehicleView.vue`：车辆参数改为 camelCase
  - `ImageUploader.vue`：图片上传参数改为 camelCase
- **后端**：
  - `AdminImageController.java`：图片上传接口参数改为 camelCase

#### 3. 错误处理改进
- **错误码映射**：添加后端错误码到用户友好文案的映射
- **统一错误处理**：避免控制台打印错误堆栈
- **友好提示**：提供操作建议（如"请选择其他时间"）
- **改进页面**：booking、index、vehicle-detail、order-detail、orders

#### 4. Mock 数据清理
- **移除范围**：vehicle-detail、booking、order-detail、index
- **保留**：login.vue 的 Mock 登录（支持后端不可用时的降级登录）

#### 5. 图片上传功能完善
- **后端**：支持 JPG/PNG/WebP 格式
- **管理端**：图片上传组件
- **小程序**：图片 URL 拼接逻辑

#### 6. 真机调试支持
- **局域网 IP 配置**：192.168.123.232:8081
- **跨域配置**：后端已支持所有来源
- **登录改进**：支持真实微信登录和 Mock 登录自动切换

### 🔄 进行中

#### 1. 小程序真机调试
- **问题**：app.json 未找到（需重新导入项目）
- **问题**：图片未加载（需勾选"不校验合法域名"）

### 📋 待办

#### 1. 生产环境配置
- 配置正式域名（HTTPS）
- 配置微信小程序服务器域名
- 配置真实的 AppID 和 Secret

#### 2. 支付功能（Sprint 4）
- 微信支付集成
- 支付回调处理

---

## 版本记录

### v1.7 (2026-04-18)
- 统一命名规范为 camelCase
- 改进错误处理和用户交互
- 支持真机调试
- 清理 Mock 数据

### v1.6 (2026-04-18)
- 图片上传功能
- 小牛电动车数据更新

### v1.5 (之前)
- 基础功能实现
- 订单流程
- 管理端
