<script setup lang="ts">
import { ref, shallowRef, watch } from 'vue';
import { business, type AuditFilters } from '../api/business';
import type { AuditResponsePage, RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { useScope, requireFreshRoles } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
import PageControls from './PageControls.vue';
import UtcTime from './UtcTime.vue';
const props = defineProps<{ record?: RequirementResponse }>(); const session = useSession(); const page = ref(0); const size = ref(20); const filters = ref({ from: '', to: '', action: '' }); let applied: Omit<AuditFilters, 'page' | 'size'> = {};
const result = shallowRef<AuditResponsePage | null>(null); const issue = shallowRef<ApiFailure | null>(null); const loading = ref(false); const scope = useScope(() => { result.value = null; issue.value = null; filters.value = { from: '', to: '', action: '' }; applied = {}; });
function json(value: Record<string, unknown> | null): string { return value === null ? '—' : JSON.stringify(value, null, 2); }
async function load() { const token = scope.start('audit'); loading.value = true; issue.value = null; const input = { ...applied, page: page.value, size: size.value }; const id = props.record?.requirementId; try { await requireFreshRoles(session, id ? ['ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER', 'VIEWER'] : ['ADMIN'], () => scope.current(token)); const value = id ? await business.history(id, input) : await business.auditEvents(input); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function filter() { applied = { ...filters.value }; page.value = 0; await load(); }
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
watch(() => props.record?.requirementId, () => { void load(); }, { immediate: true });
</script>
<template>
  <section class="audit-log"><h2>{{ record ? '需求业务历史' : '全系统审计记录' }}</h2><p class="muted small">只读记录操作者、目标、动作、结果与安全前后值。版本和评审快照可从各自页签查看。</p><form class="audit-filters form-grid" @submit.prevent="filter" novalidate><label>起点（UTC）<input v-model="filters.from" placeholder="2026-10-03T00:00:00Z" :disabled="loading" /></label><label>终点（UTC）<input v-model="filters.to" placeholder="2026-10-03T23:59:59Z" :disabled="loading" /></label><label>动作<input v-model="filters.action" maxlength="64" placeholder="精确动作名称" :disabled="loading" /></label><div class="actions"><button class="secondary" :disabled="loading">应用历史筛选</button><button type="button" class="secondary" :disabled="loading" @click="load">重新读取记录</button></div><small>时间使用带 Z 的 UTC 时间，起点不得晚于终点。过滤项只包含时间与动作。</small></form><ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="loading" class="muted">正在读取只读审计记录…</p>
    <div class="table-scroll"><table class="audit-table"><thead><tr><th>审计 ID／UTC 时间</th><th>操作者</th><th>目标</th><th>动作／结果</th><th>前后值</th></tr></thead><tbody><tr v-for="entry in result?.items" :key="entry.auditId"><td class="time-cell">{{ entry.auditId }}<br /><UtcTime :value="entry.occurredAt" /></td><td class="id-cell">{{ entry.actorId }}</td><td><span class="enum-cell">{{ entry.targetType }}</span><br /><span class="text-content">{{ entry.targetId }}</span><p v-if="entry.requirementId" class="muted small">需求 {{ entry.requirementId }}</p></td><td><span class="text-content">{{ entry.action }}</span><br /><span class="badge enum-cell">{{ entry.outcome }}</span></td><td><details class="audit-payload"><summary>查看安全前后 JSON</summary><h3>操作前</h3><pre>{{ json(entry.beforeData) }}</pre><h3>操作后</h3><pre>{{ json(entry.afterData) }}</pre></details></td></tr></tbody></table></div><p v-if="result && !result.items.length && !loading" class="empty">没有匹配的审计记录。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
  </section>
</template>
