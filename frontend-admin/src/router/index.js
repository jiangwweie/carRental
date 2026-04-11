import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('../views/vehicles/VehicleView.vue'),
        meta: { title: '车辆管理' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/orders/OrderView.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('../views/orders/OrderDetailView.vue'),
        meta: { title: '订单详情' }
      },
      {
        path: 'holidays',
        name: 'Holidays',
        component: () => import('../views/holiday/HolidayView.vue'),
        meta: { title: '节假日管理' }
      },
      {
        path: 'pricing',
        name: 'Pricing',
        component: () => import('../views/pricing/PricingView.vue'),
        meta: { title: '价格设置' }
      },
      {
        path: 'agreement',
        name: 'Agreement',
        component: () => import('../views/agreement/AgreementView.vue'),
        meta: { title: '协议管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
