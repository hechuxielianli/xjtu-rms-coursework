<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { business, type RequirementFilters } from '../api/business';
import type { CreateRequirementRequest, RequirementResponsePage, TagResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { LEVELS, KINDS, PRIORITIES, STATUSES, useScope, useOperation, useUnsaved, requireFreshRoles } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ContentFields from '../components/ContentFields.vue';
import PageControls from '../components/PageControls.vue';
import ErrorPanel from '../components/ErrorPanel.vue';
import StatusBadge from '../components/StatusBadge.vue';
import UtcTime from '../components/UtcTime.vue';
import { displayLabel } from '../presentation/labels';
const session = useSession(); const router = useRouter();
const page = ref(0); const size = ref(20); const result = shallowRef<RequirementResponsePage | null>(null);
const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const tags = shallowRef<TagResponse[]>([]);
const filter = ref({ keyword: '', status: '', kind: '', level: '', priority: '', tag: '' });
let applied: RequirementFilters = { page: 0, size: 20 };
const creating = ref(false); const dirty = ref(false); const operation = useOperation();
const blank = (): CreateRequirementRequest => ({ title: '', description: '', level: 'SYSTEM', kind: 'FUNCTIONAL', priority: 'MEDIUM', source: null, rationale: null, acceptanceCriteria: null });
const draft = ref(blank()); const engineer = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER']));
const scope = useScope(() => { result.value = null; tags.value = []; issue.value = null; draft.value = blank(); creating.value = false; dirty.value = false; });
useUnsaved(dirty);
async function load() {
  const token = scope.start('list'); loading.value = true; issue.value = null;
  try { const value = await business.requirements({ ...applied, page: page.value, size: size.value }); if (scope.current(token)) result.value = value; }
  catch (error) { if (scope.current(token)) { issue.value = normalizeFailure(error); result.value = null; } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function loadTags() { const token = scope.start('tags'); try { const value = await business.tags({ page: 0, size: 100 }); if (scope.current(token)) tags.value = value.items; } catch (error) { if (scope.current(token)) issue.value = normalizeFailure(error); } }
async function search() { applied = { ...filter.value, page: 0, size: size.value } as RequirementFilters; page.value = 0; await load(); }
async function changePage(value: number) { page.value = value; await load(); }
async function changeSize(value: number) { size.value = value; page.value = 0; await load(); }
async function create() {
  if (!engineer.value) return;
  const input = { ...draft.value };
  await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER'], current); return business.createRequirement(input); }, async value => {
    dirty.value = false; draft.value = blank(); creating.value = false; await router.push(`/requirements/${value.requirementId}`);
  });
}
async function reconcileCreate() { await load(); if (!issue.value) operation.clear(); }
function cancelCreate() { if (operation.busy.value || operation.blocked.value) return; if (!dirty.value || window.confirm('确认放弃本地新建草稿？')) { creating.value = false; draft.value = blank(); dirty.value = false; operation.clear(); } }
onMounted(() => { void load(); void loadTags(); });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">需求工作区</span><h1>需求管理</h1><p class="muted">集中维护需求、评审和正式版本。</p></div><button v-if="engineer" class="primary" @click="creating = true">新建需求</button></div>
  <div v-if="result" class="stat-grid" aria-label="需求查询统计"><div class="stat-card"><span>筛选结果</span><strong>{{ result.totalElements }}</strong><small>满足当前查询条件的需求</small></div><div class="stat-card"><span>待评审</span><strong>{{ result.items.filter(item => item.status === 'UNDER_REVIEW').length }}</strong><small>当前页需求</small></div><div class="stat-card"><span>已批准</span><strong>{{ result.items.filter(item => item.status === 'APPROVED').length }}</strong><small>当前页需求</small></div><div class="stat-card"><span>已验证</span><strong>{{ result.items.filter(item => item.status === 'VERIFIED').length }}</strong><small>当前页需求</small></div></div>
  <section v-if="creating && engineer" class="surface panel" aria-labelledby="create-title">
    <h2 id="create-title">新建草稿</h2>
    <form @submit.prevent="create" novalidate><ContentFields v-model="draft" :disabled="operation.busy.value" @update:model-value="dirty = true" />
      <ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="reconcileCreate" />
      <div class="actions"><button class="primary" :disabled="operation.busy.value || operation.blocked.value">{{ operation.busy.value ? '正在创建…' : '创建草稿' }}</button><button type="button" class="secondary" :disabled="operation.busy.value || operation.blocked.value" @click="cancelCreate">取消</button></div>
    </form>
  </section>
  <section class="surface panel"><form class="filters" @submit.prevent="search">
    <label>关键词<input v-model="filter.keyword" placeholder="编号、标题或描述" /></label>
    <label>状态<select v-model="filter.status"><option value="">全部</option><option v-for="v in STATUSES" :key="v" :value="v">{{ displayLabel(v) }}</option></select></label>
    <label>层级<select v-model="filter.level"><option value="">全部</option><option v-for="v in LEVELS" :key="v" :value="v">{{ displayLabel(v) }}</option></select></label>
    <label>性质<select v-model="filter.kind"><option value="">全部</option><option v-for="v in KINDS" :key="v" :value="v">{{ displayLabel(v) }}</option></select></label>
    <label>优先级<select v-model="filter.priority"><option value="">全部</option><option v-for="v in PRIORITIES" :key="v" :value="v">{{ displayLabel(v) }}</option></select></label>
    <label>标签 ID<input v-model="filter.tag" list="filter-tags" placeholder="已知标签 ID" /><datalist id="filter-tags"><option v-for="tag in tags" :key="tag.tagId" :value="tag.tagId">{{ tag.name }}</option></datalist></label>
    <div class="actions"><button class="primary" :disabled="loading">应用筛选</button><span class="muted small">条件同时满足（AND）</span></div>
  </form></section>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load" />
  <section class="surface panel" :aria-busy="loading"><p v-if="loading" class="muted">正在读取需求…</p>
    <div class="table-scroll"><table class="requirement-table"><thead><tr><th>编号与标题</th><th>层级／性质</th><th>状态</th><th>优先级</th><th>负责人</th><th>更新时间</th></tr></thead><tbody><tr v-for="item in result?.items" :key="item.requirementId"><td class="title-cell"><RouterLink class="item-title" :to="`/requirements/${item.requirementId}`"><span class="requirement-code">{{ item.requirementKey }}</span><strong>{{ item.title }}</strong></RouterLink><div><span v-for="tag in item.tags" :key="tag.tagId" class="badge">{{ tag.name }}</span></div></td><td class="enum-cell">{{ displayLabel(item.level) }}<br /><span class="muted small">{{ displayLabel(item.kind) }}</span></td><td class="enum-cell"><StatusBadge :value="item.status" /></td><td class="enum-cell"><span class="priority-dot" :class="`priority-${item.priority.toLowerCase()}`"></span>{{ displayLabel(item.priority) }}</td><td>{{ item.assignee?.displayName ?? '未分配' }}</td><td class="time-cell"><UtcTime :value="item.updatedAt" /></td></tr></tbody></table></div>
    <p v-if="result && !result.items.length && !loading" class="empty">没有符合条件的需求。</p>
    <PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="changePage" @size="changeSize" />
  </section>
</template>
