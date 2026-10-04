<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { business, requireId } from '../api/business';
import type { RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { useScope } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import MetadataEditor from '../components/MetadataEditor.vue';
import CommentThread from '../components/CommentThread.vue';
import LifecycleActions from '../components/LifecycleActions.vue';
import ReviewRounds from '../components/ReviewRounds.vue';
import VersionHistory from '../components/VersionHistory.vue';
import RequirementChanges from '../components/changes/RequirementChanges.vue';
import RelationPanel from '../components/relations/RelationPanel.vue';
import WithdrawAction from '../components/WithdrawAction.vue';
import AuditLogTable from '../components/AuditLogTable.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { displayLabel } from '../presentation/labels';
const route = useRoute(); const session = useSession(); const record = shallowRef<RequirementResponse | null>(null); const loading = ref(false); const ready = ref(false); const issue = shallowRef<ApiFailure | null>(null);
const tabs = [{ id: 'overview', label: '概览' }, { id: 'versions', label: '版本' }, { id: 'reviews', label: '评审' }, { id: 'relations', label: '关系' }, { id: 'changes', label: '变更' }, { id: 'comments', label: '评论' }, { id: 'history', label: '历史' }];
const tab = computed(() => typeof route.query.tab === 'string' && tabs.some(item => item.id === route.query.tab) ? route.query.tab : 'overview');
const engineer = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER'])); const editable = computed(() => engineer.value && record.value?.status === 'DRAFT' && record.value.isWithdrawn === 0);
const scope = useScope(() => { record.value = null; ready.value = false; issue.value = null; });
async function load() {
  const token = scope.start('parent'); loading.value = true; issue.value = null;
  try { const id = requireId(route.params.id); await session.refreshSession(); if (!scope.current(token)) return; const value = await business.requirement(id); if (scope.current(token)) { record.value = value; ready.value = true; } }
  catch (error) { if (scope.current(token)) { record.value = null; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
watch(() => [route.params.id, tab.value], (value, previous) => { if (!previous || value[0] !== previous[0]) scope.invalidate(); else ready.value = false; void load(); }, { immediate: true });
</script>
<template>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="loading" class="muted">正在读取需求…</p>
  <template v-if="record && ready"><div class="breadcrumb"><RouterLink to="/requirements">需求管理</RouterLink><span> / </span><span>需求详情</span></div><div class="workspace-heading detail-heading"><div><span class="eyebrow">{{ record.requirementKey }}</span><h1>{{ record.title }}</h1><p class="detail-summary muted">{{ record.description }}</p><StatusBadge :value="record.status" /><span v-if="record.isWithdrawn" class="badge">已撤销 · 只读</span></div><div class="actions"><button class="secondary" :disabled="loading" @click="load">重新读取需求</button><RouterLink v-if="editable" :to="`/requirements/${record.requirementId}/edit`" class="primary">编辑草稿</RouterLink></div></div>
    <nav class="tabs" role="tablist" aria-label="需求详情页签"><RouterLink v-for="item in tabs" :key="item.id" role="tab" :data-tab="item.id" :aria-selected="tab === item.id" :class="{ selected: tab === item.id }" :to="{ path: route.path, query: { tab: item.id } }">{{ item.label }}</RouterLink></nav>
    <section class="surface panel"><template v-if="tab === 'overview'"><div class="details-grid"><div class="requirement-content"><h2>正式内容</h2><h3>描述</h3><p class="text-content">{{ record.description }}</p><h3>来源</h3><p class="text-content">{{ record.source ?? '尚未补齐' }}</p><h3>理由</h3><p class="text-content">{{ record.rationale ?? '尚未补齐' }}</p><h3>验收标准</h3><p class="text-content">{{ record.acceptanceCriteria ?? '尚未补齐' }}</p></div><dl class="meta-list"><dt>状态</dt><dd><StatusBadge :value="record.status" /></dd><dt>层级／性质／优先级</dt><dd>{{ displayLabel(record.level) }} / {{ displayLabel(record.kind) }} / {{ displayLabel(record.priority) }}</dd><dt>创建者</dt><dd>{{ record.creator.displayName }}（{{ record.creatorId }}）</dd><dt>负责人</dt><dd>{{ record.assignee?.displayName ?? '未分配' }} <span v-if="record.assigneeId">（{{ record.assigneeId }}）</span></dd><dt>标签</dt><dd><span v-for="tagItem in record.tags" :key="tagItem.tagId" class="badge">{{ tagItem.name }}</span><span v-if="!record.tags.length">无</span></dd><dt>创建／更新时间</dt><dd>{{ record.createdAt }}<br />{{ record.updatedAt }}</dd><dt>修订／正式版本 ID</dt><dd>{{ record.lockVersion }} / {{ record.currentVersionId ?? '尚未形成' }}</dd></dl></div><MetadataEditor v-if="engineer && !record.isWithdrawn" :key="record.requirementId" :record="record" @updated="record = $event" /><LifecycleActions :key="`lifecycle-${record.requirementId}`" :record="record" @updated="record = $event" @changed="load" /><WithdrawAction :key="`withdraw-${record.requirementId}`" :record="record" @updated="record = $event" /></template>
      <VersionHistory v-else-if="tab === 'versions'" :key="record.requirementId" :record="record" />
      <ReviewRounds v-else-if="tab === 'reviews'" :key="record.requirementId" :record="record" />
      <RequirementChanges v-else-if="tab === 'changes'" :key="record.requirementId" :record="record" @updated="record = $event" />
      <RelationPanel v-else-if="tab === 'relations'" :key="record.requirementId" :record="record" @updated="record = $event" />
      <CommentThread v-else-if="tab === 'comments'" :key="record.requirementId" :record="record" @updated="record = $event" />
      <AuditLogTable v-else-if="tab === 'history'" :key="record.requirementId" :record="record" />
    </section>
  </template>
</template>
