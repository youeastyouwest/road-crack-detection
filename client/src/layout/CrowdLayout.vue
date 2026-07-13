<template>
  <div class="crowd-layout">
    <main class="crowd-content">
      <router-view />
    </main>
    <nav class="crowd-tabs">
      <router-link to="/crowd/report" class="tab-item" :class="{ active: $route.path === '/crowd/report' }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
          <circle cx="12" cy="13" r="4"/>
        </svg>
        <span>上报病害</span>
      </router-link>
      <router-link to="/crowd/records" class="tab-item" :class="{ active: $route.path === '/crowd/records' }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
        <span>上报记录</span>
      </router-link>
      <router-link to="/crowd/profile" class="tab-item" :class="{ active: $route.path === '/crowd/profile' }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from "@/stores/auth"
const authStore = useAuthStore()
</script>

<style scoped>
.crowd-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.crowd-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 80px;
}

.crowd-tabs {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: #fff;
  border-top: 1px solid #eef0f4;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom, 0);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  text-decoration: none;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  transition: color 0.15s;
  padding: 6px 16px;
  border-radius: 8px;
}

.tab-item.active {
  color: #4361ee;
}

.tab-item:hover {
  color: #4361ee;
}
</style>
