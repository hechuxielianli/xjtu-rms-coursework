<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { business, requireId } from '../api/business';
import type { ApplyChangeResponse, ChangeRequestResponse, CreateRequirementRequest, RequirementResponse, RequirementVersionResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { canCancelChange, canEditChange, completeChange, proposalCommand, proposalContent } from '../features/change';
import { useOperation, useScope, useUnsaved } from '../features/workspace';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import ContentFields from '../components/ContentFields.vue';
import FormalContentView from '../components/FormalContentView.vue';
import ChangeReviewRounds from '../components/changes/ChangeReviewRounds.vue';
import ChangeApplyAction from '../components/changes/ChangeApplyAction.vue';
import StatusBadge from '../components/StatusBadge.vue';
const route = useRoute(); const session = useSession(); const change = shallowRef<ChangeRequestResponse | null>(null); const parent = shallowRef<RequirementResponse | null>(null); const baseline = shallowRef<RequirementVersionResponse | null>(null); const appliedVersion = shallowRef<RequirementVersionResponse | null>(null);
const blank = (): CreateRequirementRequest => ({ title: '', description: '', level: 'SYSTEM', kind: 'FUNCTIONAL', priority: 'MEDIUM', source: null, rationale: null, acceptanceCriteria: null });
const draft = ref(blank()); const requestTitle = ref(''); const reason = ref(''); const dirty = ref(false); const loading = ref(false); const ready = ref(false); const saved = ref(false); const issue = shallowRef<ApiFailure | null>(null); const operation = useOperation();
const scope = useScope(() => { change.value = null; parent.value = null; baseline.value = null; appliedVersion.value = null; draft.value = blank(); requestTitle.value = ''; reason.value = ''; dirty.value = false; ready.value = false; saved.value = false; issue.value = null; }); useUnsaved(dirty);
const editable = computed(() => !!parent.value && !!change.value && canEditChange(session, parent.value, change.value));
const cancellable = computed(() => !!parent.value && !!change.value && canCancelChange(session, parent.value, change.value));
function adopt(value: ChangeRequestResponse) { draft.value = proposalContent(value); requestTitle.value = value.requestTitle; reason.value = value.reason; dirty.value = false; }
async function load(preserveDraft = false) {
  const token = scope.start('context'); loading.value = true; ready.value = false; issue.value = null;
  try {
    const id = requireId(route.params.id); await session.refreshSession(); if (!scope.current(token)) return;
    const value = await business.change(id); if (!scope.current(token)) return;
    const record = await business.requirement(value.requirementId); if (!scope.current(token)) return;
    const base = await business.version(record.requirementId, value.baseVersionId); if (!scope.current(token)) return;
    let produced: RequirementVersionResponse | null = null;
    if (value.status === 'APPLIED') {
      for (let page = 0; ; page++) { const versions = await business.versions(record.requirementId, { page, size: 100 }); if (!scope.current(token)) return; produced = versions.items.find(item => item.appliedChangeRequestId === value.changeRequestId) ?? null; if (produced || (page + 1) * 100 >= versions.totalElements) break; }
      if (!produced) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
    }
    change.value = value; parent.value = record; baseline.value = base; appliedVersion.value = produced; if (!preserveDraft) adopt(value); operation.clear(); ready.value = true;
  } catch (error) { if (scope.current(token)) { ready.value = false; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function save() {
  if (!editable.value || loading.value || !change.value || !parent.value) return;
  const value = change.value; const record = parent.value; const token = scope.start('save-context'); const input = { requestTitle: requestTitle.value, reason: reason.value, ...proposalCommand(draft.value), expectedLockVersion: value.lockVersion }; saved.value = false;
  await operation.run(async () => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!scope.current(token) || !canEditChange(session, record, value)) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.editChange(value.changeRequestId, record.requirementId, input); }, result => { change.value = result; adopt(result); saved.value = true; });
}
async function perform(action: 'submit' | 'cancel') {
  if (loading.value || !change.value || !parent.value) return;
  const value = change.value; const record = parent.value; const token = scope.start('command-context');
  if (action === 'cancel' && !window.confirm('确认取消自己的变更草稿？取消后申请为终态，未保存的建议内容将放弃。')) return;
  await operation.run(async () => {
    if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!scope.current(token) || !canEditChange(session, record, value)) throw new ApiFailure('FORBIDDEN', 'authorization', 403);
    if (action === 'cancel') { if (!canCancelChange(session, record, value)) throw new ApiFailure('NOT_AUTHOR', 'authorization', 403); return business.cancelChange(value.changeRequestId, record.requirementId, { expectedLockVersion: value.lockVersion }); }
    if (dirty.value || !completeChange(value)) throw new ApiFailure('INCOMPLETE_CONTENT', 'validation', 400);
    await business.submitChange(value.changeRequestId, record.requirementId, { expectedLockVersion: value.lockVersion }); return null;
  }, async result => { if (result) { change.value = result; adopt(result); } await load(); });
}
async function applied(value: ApplyChangeResponse) { parent.value = value.requirement; change.value = value.change; appliedVersion.value = value.version; adopt(value.change); await load(); }
function modified() { dirty.value = true; saved.value = false; }
watch(() => route.params.id, () => { scope.invalidate(); operation.resetContext(); void load(); }, { immediate: true });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">受控需求变更</span><h1>变更申请详情</h1><p v-if="change" class="muted">申请 {{ change.changeRequestId }} · {{ change.status }} · 修订 {{ change.lockVersion }}</p></div><div class="actions"><RouterLink v-if="parent" :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'changes' } }" class="secondary">返回需求变更列表</RouterLink><button class="secondary" :disabled="loading || operation.busy.value" @click="load(true)">重新读取变更上下文</button></div></div>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load(true)" /><p v-if="loading" class="muted">正在核对变更、需求与不可变基准版本…</p>
  <template v-if="ready && change && parent && baseline"><div class="change-flow"><div><small>原正式版本</small><strong>V{{ baseline.versionNo }} · {{ baseline.title }}</strong></div><span aria-hidden="true">→</span><div><small>建议修改</small><strong>{{ change.requestTitle }}</strong></div><span aria-hidden="true">→</span><div><small>申请状态</small><StatusBadge :value="change.status" /></div></div><section class="surface panel"><h2>{{ change.requestTitle }}</h2><p class="text-content">{{ change.reason }}</p><p class="muted small">需求 {{ parent.requirementKey }}（{{ parent.requirementId }}）· {{ parent.status }} · 需求修订 {{ parent.lockVersion }} · 提出者 {{ change.createdBy }} · {{ change.createdAt }}</p><p class="muted small">不可变基准 V{{ baseline.versionNo }}（{{ change.baseVersionId }}）· 当前正式版本 {{ parent.currentVersionId }}。修改建议不会直接覆盖需求。</p><RouterLink :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'versions', versionId: change.baseVersionId } }">打开完整基准版本 V{{ baseline.versionNo }}</RouterLink>
      <form v-if="editable" class="change-draft" @submit.prevent="save" novalidate><h3>编辑变更草稿</h3><label>申请标题<input v-model="requestTitle" maxlength="200" :disabled="operation.busy.value" @input="modified" /></label><label>申请理由<textarea v-model="reason" rows="3" :disabled="operation.busy.value" @input="modified" /></label><ContentFields v-model="draft" :disabled="loading || operation.busy.value" @update:model-value="modified" /><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" /><p v-if="saved" role="status">变更草稿已保存。</p><div class="actions"><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">保存变更草稿</button><span class="muted small">先保存建议，再单独提交评审；提交需要申请标题、理由与八项建议全部非空。</span></div></form>
      <template v-else><h3>已保存的八项建议内容 · 只读</h3><FormalContentView :content="proposalContent(change)" /><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" /></template>
      <section v-if="editable" class="lifecycle-actions"><p v-if="dirty" class="muted">存在未保存的修改，请先保存后再提交评审。</p><p v-else-if="!completeChange(change)" class="muted">提交前需补齐申请标题、理由与八项建议内容。</p><div class="actions"><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value || dirty || !completeChange(change)" @click="perform('submit')">提交变更评审</button><button v-if="cancellable" class="secondary danger" :disabled="loading || operation.busy.value || operation.blocked.value" @click="perform('cancel')">取消本人草稿变更</button></div></section>
      <p v-if="['REJECTED', 'CANCELLED'].includes(change.status)" class="muted">{{ change.status }} 为终态，不能编辑或重新提交。</p><p v-if="parent.isWithdrawn" class="muted">需求已撤销，变更保持只读。</p><ChangeApplyAction :key="change.changeRequestId" :parent="parent" :change="change" :loading="loading" @applied="applied" @reload="load(true)" />
      <p v-if="appliedVersion" role="status">已产生正式 V{{ appliedVersion.versionNo }}（{{ appliedVersion.versionId }}）· 来源申请 {{ appliedVersion.appliedChangeRequestId }}。<RouterLink :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'versions', versionId: appliedVersion.versionId } }">查看产生的正式版本</RouterLink></p><RouterLink v-if="change.status === 'APPLIED'" :to="`/requirements/${parent.requirementId}`">打开需求进行新版本实现与验证</RouterLink>
    </section><section class="surface panel"><ChangeReviewRounds :key="change.changeRequestId" :change="change" /></section><details class="surface panel"><summary>只读基准 V{{ baseline.versionNo }} 的八项内容</summary><FormalContentView :content="baseline" /></details><details v-if="appliedVersion" class="surface panel"><summary>本申请产生的正式 V{{ appliedVersion.versionNo }} · {{ appliedVersion.versionId }}</summary><FormalContentView :content="appliedVersion" /></details></template>
</template>
