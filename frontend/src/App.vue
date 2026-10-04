<script setup lang="ts">
import { computed, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { useSession } from './auth/useSession';
import ErrorPanel from './components/ErrorPanel.vue';
const session = useSession();
const route = useRoute();
const router = useRouter();
const navigation = computed(() => [
  { path: '/requirements', label: '需求工作区', visible: true },
  { path: '/reviews', label: '需求评审', visible: session.hasAnyRole(['REVIEWER']) },
  { path: '/change-reviews', label: '变更评审', visible: session.hasAnyRole(['REVIEWER']) },
  { path: '/admin/users', label: '用户与角色', visible: session.hasAnyRole(['ADMIN']) },
  { path: '/admin/audit', label: '系统审计', visible: session.hasAnyRole(['ADMIN']) },
].filter(item => item.visible));
watch(() => session.status, status => {
  if ((status === 'anonymous' || status === 'unavailable') && route.meta.requiresAuth) void router.replace({ path: '/login', query: { redirect: route.fullPath } });
});
async function logout() {
  try { await session.logout(); if (!session.authenticated) await router.replace('/login'); }
  catch { /* Sanitized issue is displayed; never log the original request. */ }
}
async function refresh() { await session.initialize(); }
</script>

<template>
  <div class="app-shell" :class="{ 'has-sidebar': session.authenticated }">
    <aside v-if="session.authenticated" class="workspace-sidebar">
      <RouterLink class="brand" to="/requirements"><span class="brand-icon">R</span><span><strong>RMS</strong><small>需求管理系统</small></span></RouterLink>
      <p class="nav-caption">工作区</p>
      <nav class="workspace-nav" aria-label="工作区导航"><RouterLink v-for="(item, index) in navigation" :key="item.path" :to="item.path"><span class="nav-icon" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>{{ item.label }}</RouterLink></nav>
      <p class="sidebar-note">软件系统分析与设计<br />单项目需求工作区</p>
    </aside>
    <div class="app-stage">
      <header class="topbar"><RouterLink v-if="!session.authenticated" class="brand" to="/requirements"><span class="brand-icon">R</span><span>需求管理系统</span></RouterLink><div v-else class="page-title"><span>需求管理工作区</span><strong>{{ route.meta.title ?? '需求管理' }}</strong></div><div v-if="session.authenticated" class="account-area"><span class="account-avatar" aria-hidden="true">{{ session.identity?.user.displayName?.slice(0, 1) }}</span><span class="account-name">{{ session.identity?.user.displayName }}</span><button class="secondary compact" :disabled="session.busy" @click="logout">{{ session.busy ? '请稍候…' : '退出' }}</button></div></header>
      <main class="main-content"><ErrorPanel v-if="route.path !== '/login'" :failure="session.issue" :busy="session.busy" @reload="refresh" /><RouterView /></main>
      <footer>需求管理 · 软件系统分析与设计课程项目</footer>
    </div>
  </div>
</template>
