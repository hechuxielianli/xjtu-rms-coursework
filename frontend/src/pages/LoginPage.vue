<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSession } from '../auth/useSession';
import { passwordProblem } from '../auth/password';
import { registerSessionClearer } from '../auth/session-scope';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import { safeReturnPath } from '../router/redirect';
import ErrorPanel from '../components/ErrorPanel.vue';

const session = useSession();
const route = useRoute();
const router = useRouter();
const login = ref('');
const password = ref('');
const localError = ref<ApiFailure | null>(null);
const checking = ref(false);
const failure = computed(() => localError.value ?? session.issue);
const passwordHint = computed(() => password.value ? passwordProblem(password.value) : null);
const clearPassword = () => { password.value = ''; };
const unregister = registerSessionClearer(clearPassword);
onUnmounted(() => { clearPassword(); unregister(); });

async function enterWorkspace() {
  if (session.authenticated) {
    const target = safeReturnPath(route.query.redirect);
    await router.replace(router.resolve(target).matched.length && !router.resolve(target).matched.some(r => r.path.includes('pathMatch')) ? target : '/requirements');
  }
}
async function submit() {
  if (session.busy || checking.value || failure.value?.outcomeUnknown) return;
  const rawPassword = password.value;
  clearPassword();
  localError.value = null;
  try { await session.login({ login: login.value, password: rawPassword }); await enterWorkspace(); }
  catch (error) { localError.value = normalizeFailure(error, true); }
}
async function reconcile() {
  if (checking.value || session.busy) return;
  checking.value = true;
  localError.value = null;
  clearPassword();
  try { await session.initialize(); await enterWorkspace(); }
  finally { checking.value = false; }
}
</script>
<template>
  <div class="login-layout">
    <section class="login-intro">
      <span class="eyebrow">软件系统分析与设计 · 课程项目</span>
      <h1>需求管理系统</h1>
      <p>集中维护需求、评审和正式版本。</p>
      <div class="process-line"><span>提出需求</span><span>评审确认</span><span>受控变更</span></div>
    </section>
    <section class="surface login-card" aria-labelledby="login-title">
      <p class="eyebrow">账号访问</p><h2 id="login-title">登录需求管理系统</h2>
      <p class="muted">使用管理员提供的启用账号。</p>
      <form @submit.prevent="submit" novalidate>
        <label for="login">用户名或邮箱</label>
        <input id="login" v-model="login" name="login" autocomplete="username" required :disabled="session.busy || checking" />
        <label for="password">密码</label>
        <input id="password" v-model="password" name="password" type="password" autocomplete="current-password" required :disabled="session.busy || checking" aria-describedby="password-help" />
        <small id="password-help" :class="{ invalid: passwordHint }">{{ passwordHint ?? '密码区分空格与字符形式，最多 72 个 UTF-8 字节。' }}</small>
        <ErrorPanel :failure="failure" :busy="checking || session.busy" @reload="reconcile" />
        <button class="primary full" type="submit" :disabled="session.busy || checking || !!failure?.outcomeUnknown">{{ session.busy ? '正在登录…' : '登录' }}</button>
      </form>
      <button class="link-button" :disabled="checking || session.busy" @click="reconcile">{{ checking ? '正在核对…' : '重新核对会话' }}</button>
      <p class="muted small">账号禁用或访问受限时，请联系管理员。</p>
    </section>
  </div>
</template>
