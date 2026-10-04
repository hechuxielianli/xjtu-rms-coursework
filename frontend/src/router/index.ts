import { createRouter, createWebHistory, type RouterHistory, type RouteRecordRaw } from 'vue-router';
import type { Pinia } from 'pinia';
import type { RoleCode } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import LoginPage from '../pages/LoginPage.vue';
import PendingPage from '../pages/PendingPage.vue';
import ForbiddenPage from '../pages/ForbiddenPage.vue';
import RequirementListPage from '../pages/RequirementListPage.vue';
import RequirementDetailPage from '../pages/RequirementDetailPage.vue';
import RequirementEditorPage from '../pages/RequirementEditorPage.vue';
import UserAdminPage from '../pages/UserAdminPage.vue';
import ReviewWorkspacePage from '../pages/ReviewWorkspacePage.vue';
import RequirementReviewPage from '../pages/RequirementReviewPage.vue';
import ChangeDetailPage from '../pages/ChangeDetailPage.vue';
import ChangeReviewWorkspacePage from '../pages/ChangeReviewWorkspacePage.vue';
import ChangeReviewPage from '../pages/ChangeReviewPage.vue';
import SystemAuditPage from '../pages/SystemAuditPage.vue';

declare module 'vue-router' {
  interface RouteMeta { requiresAuth?: boolean; rolesAny?: readonly RoleCode[]; title?: string; }
}
export const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/requirements' },
  { path: '/login', component: LoginPage, meta: { title: '登录' } },
  { path: '/requirements', component: RequirementListPage, meta: { requiresAuth: true, title: '需求工作区' } },
  { path: '/requirements/:id', component: RequirementDetailPage, meta: { requiresAuth: true, title: '需求详情' } },
  { path: '/requirements/:id/edit', component: RequirementEditorPage, meta: { requiresAuth: true, title: '草稿编辑', rolesAny: ['REQUIREMENT_ENGINEER'] } },
  { path: '/reviews', component: ReviewWorkspacePage, meta: { requiresAuth: true, title: '需求评审', rolesAny: ['REVIEWER'] } },
  { path: '/reviews/:id', component: RequirementReviewPage, meta: { requiresAuth: true, title: '需求评审详情' } },
  { path: '/changes/:id', component: ChangeDetailPage, meta: { requiresAuth: true, title: '变更详情' } },
  { path: '/change-reviews', component: ChangeReviewWorkspacePage, meta: { requiresAuth: true, title: '变更评审', rolesAny: ['REVIEWER'] } },
  { path: '/change-reviews/:id', component: ChangeReviewPage, meta: { requiresAuth: true, title: '变更评审详情' } },
  { path: '/admin/users', component: UserAdminPage, meta: { requiresAuth: true, title: '用户与角色', rolesAny: ['ADMIN'] } },
  { path: '/admin/audit', component: SystemAuditPage, meta: { requiresAuth: true, title: '系统审计', rolesAny: ['ADMIN'] } },
  { path: '/forbidden', component: ForbiddenPage, meta: { requiresAuth: true, title: '访问受限' } },
  { path: '/:pathMatch(.*)*', component: PendingPage, meta: { title: '页面不存在' } },
];

export function createAppRouter(pinia: Pinia, history: RouterHistory = createWebHistory()) {
  const router = createRouter({ history, routes });
  router.beforeEach(async to => {
    const session = useSession(pinia);
    const wasUnknown = session.status === 'unknown';
    await session.ensureInitialized();
    if (to.meta.requiresAuth) {
      if (!wasUnknown) await session.refreshSession();
      if (!session.authenticated) return { path: '/login', query: { redirect: to.fullPath } };
      if (to.meta.rolesAny && !session.hasAnyRole(to.meta.rolesAny)) return '/forbidden';
    } else if (to.path === '/login' && session.authenticated) return '/requirements';
    return true;
  });
  return router;
}
