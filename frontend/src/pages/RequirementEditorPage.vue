<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { business, requireId } from '../api/business';
import type { CreateRequirementRequest, RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ContentFields from '../components/ContentFields.vue';
import ErrorPanel from '../components/ErrorPanel.vue';
import FormalContentView from '../components/FormalContentView.vue';
const route = useRoute(); const session = useSession(); const record = shallowRef<RequirementResponse | null>(null);
const blank = (): CreateRequirementRequest => ({ title: '', description: '', level: 'SYSTEM', kind: 'FUNCTIONAL', priority: 'MEDIUM', source: null, rationale: null, acceptanceCriteria: null });
const draft = ref(blank()); const dirty = ref(false); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const saved = ref(false);
const operation = useOperation(); const scope = useScope(() => { record.value = null; draft.value = blank(); dirty.value = false; issue.value = null; });
const editable = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER']) && record.value?.status === 'DRAFT' && record.value.isWithdrawn === 0);
useUnsaved(dirty);
function content(value: RequirementResponse): CreateRequirementRequest { return { title: value.title, description: value.description, level: value.level, kind: value.kind, priority: value.priority, source: value.source, rationale: value.rationale, acceptanceCriteria: value.acceptanceCriteria }; }
async function load(preserveDraft = false) {
  const token = scope.start('record'); loading.value = true; issue.value = null;
  try { const value = await business.requirement(requireId(route.params.id)); if (scope.current(token)) { record.value = value; if (!preserveDraft) { draft.value = content(value); dirty.value = false; } operation.clear(); } }
  catch (error) { if (scope.current(token)) { record.value = null; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function save() {
  if (!editable.value || loading.value || !record.value) return;
  const id = record.value.requirementId; const revision = record.value.lockVersion; saved.value = false;
  const input = { ...draft.value, expectedLockVersion: revision };
  await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER'], current); return business.editContent(id, input); }, value => { record.value = value; draft.value = content(value); dirty.value = false; saved.value = true; });
}
watch(() => route.params.id, () => { scope.invalidate(); operation.resetContext(); saved.value = false; void load(); }, { immediate: true });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">DRAFT CONTENT</span><h1>编辑正式内容草稿</h1><p v-if="record" class="muted">{{ record.requirementKey }} · 修订 {{ record.lockVersion }}</p></div><RouterLink :to="`/requirements/${route.params.id}`" class="secondary">返回需求</RouterLink></div>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load(true)" />
  <section v-if="record" class="surface panel"><p v-if="!editable" class="muted">当前需求不能直接编辑。只有未撤销的 DRAFT 可编辑正式内容；已形成基线的受控修改需通过后续变更流程。</p>
    <form v-if="editable" @submit.prevent="save" novalidate><ContentFields v-model="draft" :disabled="loading || operation.busy.value" @update:model-value="dirty = true; saved = false" />
      <ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" />
      <p v-if="saved" role="status">草稿已保存。</p>
      <div class="actions"><button class="primary" :disabled="!editable || loading || operation.busy.value || operation.blocked.value">{{ operation.busy.value ? '正在保存…' : '保存草稿' }}</button><span class="muted small">保存与提交评审是独立操作；保存完整内容后可返回需求概要提交评审。</span></div>
    </form><FormalContentView v-else :content="content(record)" />
  </section><p v-else-if="loading" class="muted">正在读取需求…</p>
</template>
