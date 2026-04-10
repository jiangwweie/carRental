<template>
  <el-container class="layout">
    <el-aside width="200px">
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/vehicles">
          <el-icon><Van /></el-icon>
          <span>车辆管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/pricing">
          <el-icon><PriceTag /></el-icon>
          <span>价格设置</span>
        </el-menu-item>
        <el-menu-item index="/agreement">
          <el-icon><Document /></el-icon>
          <span>协议管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <h3>{{ $route.meta.title }}</h3>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
</script>

<style scoped>
.layout {
  height: 100vh;
}

.el-aside {
  background: #fff;
  border-right: 1px solid #e6e6e6;
}

.el-header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
}

.el-main {
  background: #f5f5f5;
}
</style>
