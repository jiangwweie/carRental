# Sprint 2.5 核心流程验证 - 设计决策文档

**日期**: 2026-04-11
**范围**: T1 关闭 Mock 数据 / T2 个人中心动态信息 / T3 全局登录态检查

---

## T1: 关闭 Mock 数据，对接真实 API

**复杂度**: 低 — **无需设计，直接执行**

### 需要改动的文件

| 文件 | 改动 |
|------|------|
| `frontend-mini/src/pages/index/index.vue` | `useMock.value = true` → `false` |
| `frontend-mini/src/pages/vehicle-detail/vehicle-detail.vue` | Mock 数据块删除或注释，走真实 API |
| `frontend-mini/src/pages/booking/booking.vue` | `mockPrice` / `mockOrderData` 切换为真实 API |
| `frontend-mini/src/pages/order-detail/order-detail.vue` | `mockOrder` 切换为真实 API |

### 注意事项

- `request.js` 的 `handle401` 已实现自动跳转登录页
- API 失败时会降级到 Mock（catch 中 `useMock.value = true`），这是安全兜底
- 需确保后端服务运行中（`localhost:8080`），MySQL 已启动
- `request.js` 超时 10s（TODO: T16 优化为 3-5s）

---

## T2: 个人中心动态用户信息 + 退出登录

**复杂度**: 低 — **无需设计，直接执行**

### 需要改动的文件

| 文件 | 改动 |
|------|------|
| `frontend-mini/src/pages/me/me.vue` | 从 `useUserStore()` 读取 `userInfo` / `isLoggedIn`；添加退出登录按钮 |
| `frontend-mini/src/store/user.js` | `logout()` 方法已存在，验证是否正确清空 storage |

### 执行要点

- `me.vue` 中用 computed 从 userStore 读取 `userInfo` 和 `isLoggedIn`
- 未登录状态：显示"点击登录"引导
- 已登录状态：显示昵称、手机号、头像；显示"退出登录"按钮
- 退出登录调用 `userStore.logout()` 后跳回首页或显示未登录状态

---

## T3: 全局登录态检查

**复杂度**: 中 — **需要设计**

### 问题分析

当前 `App.vue` 的 `onLaunch` 只是 console.log，**没有实际跳转逻辑**：

```javascript
// 当前代码 — 什么都不做
onLaunch(() => {
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()
  if (!isLogin) {
    console.log('App Launch: 用户未登录，首页和车辆详情页为公开页面，暂不强制跳转')
  }
})
```

而现有的页面级保护是通过 `useAuthGuard` 在 `onShow` 中逐个页面检查的。

### 设计决策

#### 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. `onLaunch` 统一检查 + 跳转登录 | 一次检查，全局生效 | 首次体验差，公开页面（首页/车辆详情）也会被拦截 |
| B. `onShow` 页面级守卫（现状） | 公开页面不被拦截 | 每个需要登录的页面都要加 `useAuthGuard` |
| C. `onLaunch` 检查 + 公开页面白名单 | 灵活，兼顾公开和私有页面 | 复杂度略高 |

#### 决策: 采用方案 C

理由：项目是汽车租赁业务，首页浏览车辆和车辆详情应该是**公开页面**，不需要登录。但个人中心、下单、订单列表等页面必须登录。

#### 公开页面白名单（不需要登录）

```javascript
const PUBLIC_PAGES = [
  '/pages/index/index',           // 首页 - 浏览车辆
  '/pages/vehicle-detail/vehicle-detail',  // 车辆详情
  '/pages/login/login',           // 登录页本身
  '/pages/agreement/agreement',   // 用户协议
]
```

#### 实现逻辑

**App.vue `onShow` 中检查**（而非 `onLaunch`，因为 `onShow` 在切回前台时也会触发）：

```javascript
onShow(() => {
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()
  if (!isLogin) {
    // 获取当前页面路径
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const route = currentPage?.route || ''
    
    // 如果当前页面不是公开页面，跳转到登录页
    if (!PUBLIC_PAGES.includes(`/${route}`)) {
      uni.reLaunch({
        url: '/pages/login/login'
      })
    }
  }
})
```

#### 登录后返回来源页

在 `login.vue` 登录成功后，读取 `redirectTo` 参数（如果有），否则跳转到首页：

```javascript
// 登录成功后的跳转
const redirectUrl = options.redirectTo || '/pages/index/index'
uni.redirectTo({ url: redirectUrl })  // 或 switchTab 根据 URL 类型决定
```

#### Token 存储格式统一

**问题**: 当前 Mock 登录生成的 token 格式是 `mock_token_1234567890`，后端 Spring Security 期望的是 JWT token。

**决策**: T1 阶段暂时保留 Mock token 过渡，T1 验证通过后再切换到真实微信登录（T14）。在 `request.js` 的 `isUnauthorized` 中已经处理了 `unauthorized` 和 `token expired` 的情况，能够正确识别无效 token。

### 依赖关系

```
T3 (全局登录检查) 应先于 T2 完成，因为 T2 的退出登录后需要全局守卫来保证状态一致
```

---

## 执行顺序

```
T1 (关闭 Mock) → 并行执行 T2 + T3
```

- T1 是最核心的，验证端到端流程
- T2 和 T3 互不依赖，可以并行

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| MySQL 未启动 | T1 无法验证 | 需用户先启动 Docker MySQL |
| 后端未运行 | T1 无法验证 | 需用户确认后端服务运行在 8080 |
| Mock token 被后端拒绝 | 请求 401 | request.js 已有 401 处理，会跳登录页 |
| `getCurrentPages` 在 `onLaunch` 为空 | 获取不到页面路径 | 改用 `onShow` 而非 `onLaunch` |
