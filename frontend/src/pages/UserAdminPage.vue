<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import { business } from '../api/business';
import type { AdminUserResponse, AdminUserResponsePage, RoleCode } from '../api/contracts.generated';
import { ROLE_CODES } from '../api/guards';
import { useSession } from '../auth/useSession';
import { passwordProblem } from '../auth/password';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { normalizeFailure, ApiFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import PageControls from '../components/PageControls.vue';
const router = useRouter(); const session = useSession(); const page = ref(0); const size = ref(20);
const admin = computed(() => session.hasAnyRole(['ADMIN']));
const result = shallowRef<AdminUserResponsePage | null>(null); const selected = shallowRef<AdminUserResponse | null>(null);
const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const operation = useOperation(); const creation = useOperation();
const creating = ref(false); const initialPassword = ref(''); const createForm = ref({ username: '', email: '', displayName: '', roles: [] as RoleCode[] });
const editForm = ref({ username: '', email: '', displayName: '' }); const roles = ref<RoleCode[]>([]); const profileDirty = ref(false); const rolesDirty = ref(false); const editDirty = computed(() => profileDirty.value || rolesDirty.value); const createDirty = ref(false);
const dirty = computed(() => editDirty.value || createDirty.value || initialPassword.value !== '');
const passwordHint = computed(() => initialPassword.value ? passwordProblem(initialPassword.value) : null);
const scope = useScope(() => { result.value = null; selected.value = null; initialPassword.value = ''; createForm.value = { username: '', email: '', displayName: '', roles: [] }; editForm.value = { username: '', email: '', displayName: '' }; roles.value = []; profileDirty.value = false; rolesDirty.value = false; createDirty.value = false; issue.value = null; });
useUnsaved(dirty);
async function load(reconcile = false) {
  const token = scope.start('users'); loading.value = true; issue.value = null;
  try { await requireFreshRoles(session, ['ADMIN'], () => scope.current(token)); const value = await business.users({ page: page.value, size: size.value }); if (scope.current(token)) { result.value = value; if (selected.value) selected.value = value.items.find(user => user.userId === selected.value?.userId) ?? null; if (reconcile) { operation.clear(); creation.clear(); } } }
  catch (error) { if (scope.current(token)) { issue.value = normalizeFailure(error); result.value = null; selected.value = null; } }
  finally { if (scope.current(token)) loading.value = false; }
}
function choose(user: AdminUserResponse) {
  if (!admin.value || operation.busy.value || operation.blocked.value) return;
  if (editDirty.value && !window.confirm('确认放弃当前用户的本地资料或角色修改？')) return;
  selected.value = user; editForm.value = { username: user.username, email: user.email, displayName: user.displayName }; roles.value = [...user.roles]; profileDirty.value = false; rolesDirty.value = false; operation.clear();
}
async function refreshSelf(user: AdminUserResponse) {
  if (user.userId === session.identity?.user.userId) { await session.refreshSession(); if (session.authenticated && !session.hasAnyRole(['ADMIN'])) await router.replace('/forbidden'); }
}
async function create() {
  if (!session.hasAnyRole(['ADMIN']) || loading.value || creation.busy.value || creation.blocked.value) return;
  const input = { ...createForm.value, roles: [...createForm.value.roles], initialPassword: initialPassword.value }; initialPassword.value = '';
  await creation.run(async current => { await requireFreshRoles(session, ['ADMIN'], current); return business.createUser(input); }, async () => { createForm.value = { username: '', email: '', displayName: '', roles: [] }; createDirty.value = false; creating.value = false; await load(); });
}
async function applyResult(user: AdminUserResponse, saved: 'profile' | 'roles' | 'status') {
  selected.value = user;
  if (saved === 'profile' || !profileDirty.value) { editForm.value = { username: user.username, email: user.email, displayName: user.displayName }; profileDirty.value = false; }
  if (saved === 'roles' || !rolesDirty.value) { roles.value = [...user.roles]; rolesDirty.value = false; }
  if (result.value) result.value = { ...result.value, items: result.value.items.map(item => item.userId === user.userId ? user : item) };
  await refreshSelf(user);
}
async function saveProfile() { if (!selected.value || !admin.value || loading.value) return; const user = selected.value; const input = { ...editForm.value, expectedLockVersion: user.lockVersion }; await operation.run(async current => { await requireFreshRoles(session, ['ADMIN'], current); return business.editUser(user.userId, input); }, value => applyResult(value, 'profile')); }
async function saveRoles() {
  if (!selected.value || !session.hasAnyRole(['ADMIN']) || loading.value) return;
  if (selected.value.accountStatus === 'ENABLED' && roles.value.length === 0) { operation.fail(new ApiFailure('INVALID_INPUT', 'validation', 400)); return; }
  const user = selected.value; const input = { roles: [...roles.value], expectedLockVersion: user.lockVersion }; await operation.run(async current => { await requireFreshRoles(session, ['ADMIN'], current); return business.setRoles(user.userId, input); }, value => applyResult(value, 'roles'));
}
async function changeStatus() { if (!selected.value || !admin.value || loading.value) return; const user = selected.value; const enabled = user.accountStatus !== 'ENABLED'; if (!window.confirm(`确认${enabled ? '启用' : '禁用'}用户 ${user.displayName}？`)) return; await operation.run(async current => { await requireFreshRoles(session, ['ADMIN'], current); return business.userStatus(user.userId, enabled, { expectedLockVersion: user.lockVersion }); }, value => applyResult(value, 'status')); }
async function paginate(value: number) { if (editDirty.value && !window.confirm('确认放弃当前用户的本地修改并翻页？')) return; selected.value = null; profileDirty.value = false; rolesDirty.value = false; page.value = value; await load(); }
async function resize(value: number) { if (editDirty.value && !window.confirm('确认放弃当前用户的本地修改？')) return; selected.value = null; profileDirty.value = false; rolesDirty.value = false; size.value = value; page.value = 0; await load(); }
function cancelCreate() { if (creation.busy.value || creation.blocked.value) return; if (createDirty.value && !window.confirm('确认放弃新用户表单？')) return; initialPassword.value = ''; createForm.value = { username: '', email: '', displayName: '', roles: [] }; createDirty.value = false; creating.value = false; creation.clear(); }
onMounted(() => { void load(); });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">ADMINISTRATION</span><h1>用户与角色</h1><p class="muted">维护账号、启用状态与五种预定义角色。</p></div><button v-if="admin" class="primary" @click="creating = true">创建用户</button></div>
  <section v-if="creating && admin" class="surface panel"><h2>创建用户</h2><form @submit.prevent="create" novalidate><fieldset :disabled="creation.busy.value"><div class="form-grid"><label>用户名<input v-model="createForm.username" maxlength="64" @input="createDirty = true" /></label><label>邮箱<input v-model="createForm.email" type="email" maxlength="254" @input="createDirty = true" /></label><label>显示名称<input v-model="createForm.displayName" maxlength="100" @input="createDirty = true" /></label></div><label for="initial-password">初始密码<input id="initial-password" v-model="initialPassword" type="password" autocomplete="new-password" aria-describedby="initial-password-help" /></label><small id="initial-password-help" :class="{ invalid: passwordHint }">{{ passwordHint ?? '保持原输入；必须非空白且为 1 至 72 个 UTF-8 字节。提交后清空。' }}</small><h3>初始角色</h3><div class="role-options"><label v-for="role in ROLE_CODES" :key="role"><input v-model="createForm.roles" type="checkbox" :value="role" @change="createDirty = true" />{{ role }}</label></div><small>启用账号至少分配一种角色；ADMIN 不自动包含其他角色权限。</small></fieldset>
    <ErrorPanel :failure="creation.issue.value" :busy="loading || creation.busy.value" @reload="load(true)" /><div class="actions"><button class="primary" :disabled="loading || creation.busy.value || creation.blocked.value">{{ creation.busy.value ? '正在创建…' : '创建用户' }}</button><button type="button" class="secondary" :disabled="creation.busy.value || creation.blocked.value" @click="cancelCreate">取消</button></div></form></section>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load(true)" /><section class="surface panel" :aria-busy="loading"><p v-if="loading" class="muted">正在读取用户…</p><div class="table-scroll"><table><thead><tr><th>用户 ID／用户名</th><th>显示名称／邮箱</th><th>状态</th><th>角色</th><th>操作</th></tr></thead><tbody><tr v-for="user in result?.items" :key="user.userId"><td>{{ user.userId }}<br />{{ user.username }}</td><td>{{ user.displayName }}<br />{{ user.email }}</td><td>{{ user.accountStatus }}</td><td><span v-for="role in user.roles" :key="role" class="badge">{{ role }}</span></td><td><button class="secondary compact" :disabled="!admin || operation.busy.value || operation.blocked.value" @click="choose(user)">管理</button></td></tr></tbody></table></div><p v-if="result && !result.items.length" class="empty">没有用户记录。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading || operation.busy.value || operation.blocked.value" @page="paginate" @size="resize" /></section>
  <section v-if="selected && admin" class="surface panel"><h2>管理 {{ selected.displayName }}</h2><p class="muted small">用户 {{ selected.userId }} · 当前修订 {{ selected.lockVersion }} · {{ selected.accountStatus }}</p><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" />
    <form @submit.prevent="saveProfile" novalidate><fieldset :disabled="loading || operation.busy.value"><div class="form-grid"><label>用户名<input v-model="editForm.username" maxlength="64" @input="profileDirty = true" /></label><label>邮箱<input v-model="editForm.email" type="email" maxlength="254" @input="profileDirty = true" /></label><label>显示名称<input v-model="editForm.displayName" maxlength="100" @input="profileDirty = true" /></label></div></fieldset><div class="actions"><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">保存基本资料</button></div></form>
    <form @submit.prevent="saveRoles"><h3>分配预定义角色</h3><fieldset :disabled="loading || operation.busy.value" class="role-options"><label v-for="role in ROLE_CODES" :key="role"><input v-model="roles" type="checkbox" :value="role" @change="rolesDirty = true" />{{ role }}</label></fieldset><small>启用用户至少保留一个角色；禁用用户可暂时没有角色。</small><div class="actions"><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value || (selected.accountStatus === 'ENABLED' && roles.length === 0)">保存角色集合</button><button type="button" class="secondary" :disabled="loading || operation.busy.value || operation.blocked.value" @click="changeStatus">{{ selected.accountStatus === 'ENABLED' ? '禁用账号' : '启用账号' }}</button></div></form>
    <details><summary>角色授予记录</summary><ul><li v-for="assignment in selected.assignments" :key="assignment.roleId">角色 ID {{ assignment.roleId }} · 授予者 {{ assignment.grantedBy }} · {{ assignment.grantedAt }}</li></ul></details>
  </section>
</template>
