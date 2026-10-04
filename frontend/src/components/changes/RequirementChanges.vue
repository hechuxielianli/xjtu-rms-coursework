<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { business } from '../../api/business';
import type { ChangeRequestResponsePage, ChangeStatus, RequirementResponse } from '../../api/contracts.generated';
import { useSession } from '../../auth/useSession';
import { CHANGE_STATUSES, changeParentAllowed } from '../../features/change';
import { useOperation, useScope, useUnsaved } from '../../features/workspace';
import { ApiFailure, normalizeFailure } from '../../errors/api-error';
import ErrorPanel from '../ErrorPanel.vue';
import PageControls from '../PageControls.vue';
const props = defineProps<{ record: RequirementResponse }>(); const emit = defineEmits<{ updated: [RequirementResponse] }>();
const router = useRouter(); const session = useSession(); const page = ref(0); const size = ref(20); const status = ref<ChangeStatus | ''>(''); const result = shallowRef<ChangeRequestResponsePage | null>(null); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null);
const requestTitle = ref(''); const reason = ref(''); const createdId = ref<string | null>(null); const operation = useOperation();
const scope = useScope(() => { result.value = null; issue.value = null; requestTitle.value = ''; reason.value = ''; createdId.value = null; });
const dirty = computed(() => requestTitle.value !== '' || reason.value !== ''); useUnsaved(dirty);
const allowed = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER']) && changeParentAllowed(props.record));
async function load() { const token = scope.start('changes'); loading.value = true; issue.value = null; try { const value = await business.changes(props.record.requirementId, { page: page.value, size: size.value }, status.value || undefined); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function reload() { const token = scope.start('parent'); loading.value = true; try { await session.refreshSession(); if (!scope.current(token)) return; const value = await business.requirement(props.record.requirementId); if (scope.current(token)) { emit('updated', value); await load(); if (scope.current(token) && !issue.value) operation.clear(); } } catch (error) { if (scope.current(token)) issue.value = normalizeFailure(error); } finally { if (scope.current(token)) loading.value = false; } }
async function create() {
  if (!allowed.value || loading.value) return;
  const record = props.record; const token = scope.start('create-context'); const input = { requestTitle: requestTitle.value, reason: reason.value, expectedRequirementLockVersion: record.lockVersion };
  await operation.run(async current => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!current() || !scope.current(token) || !session.hasAnyRole(['REQUIREMENT_ENGINEER']) || !changeParentAllowed(record)) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.createChange(record.requirementId, input); }, async value => { requestTitle.value = ''; reason.value = ''; createdId.value = value.changeRequestId; await router.push(`/changes/${value.changeRequestId}`); }, () => props.record.requirementId === record.requirementId && props.record.lockVersion === record.lockVersion);
}
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
async function filter() { page.value = 0; await load(); }
watch(() => [props.record.requirementId, props.record.lockVersion], () => { void load(); }, { immediate: true });
</script>
<template>
  <section class="requirement-changes"><h2>需求变更申请</h2><p class="muted small">同一需求最多存在一条 DRAFT、UNDER_REVIEW 或 APPROVED 活动申请；服务器检查活动占用及当前基准。</p><label>变更状态筛选<select v-model="status" :disabled="loading" @change="filter"><option value="">全部状态</option><option v-for="value in CHANGE_STATUSES" :key="value">{{ value }}</option></select></label><ErrorPanel :failure="issue" :busy="loading" @reload="reload" />
    <div class="table-scroll"><table><thead><tr><th>申请标题／ID</th><th>状态</th><th>基准版本</th><th>提出者／时间</th><th>操作</th></tr></thead><tbody><tr v-for="change in result?.items" :key="change.changeRequestId"><td>{{ change.requestTitle }}<br />{{ change.changeRequestId }}</td><td>{{ change.status }}</td><td>{{ change.baseVersionId }}</td><td>{{ change.createdBy }}<br />{{ change.createdAt }}</td><td><RouterLink :to="`/changes/${change.changeRequestId}`">查看变更</RouterLink></td></tr></tbody></table></div><p v-if="result && !result.items.length && !loading" class="empty">没有匹配的变更申请。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
    <form v-if="allowed" class="change-create" @submit.prevent="create" novalidate><h3>创建变更草稿</h3><p class="muted small">当前正式版本 {{ record.currentVersionId }} · 需求修订 {{ record.lockVersion }}。服务器复制该版本的基准与八项内容作为建议草稿。</p><label>申请标题<input v-model="requestTitle" maxlength="200" :disabled="operation.busy.value" /></label><label>申请理由<textarea v-model="reason" rows="3" :disabled="operation.busy.value" /></label><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="reload" /><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">创建变更草稿</button></form>
    <p v-else class="muted">只有需求工程师可针对未撤销的 APPROVED、IMPLEMENTED、VERIFIED 正式需求提出变更。</p><RouterLink v-if="createdId" :to="`/changes/${createdId}`">打开已创建的变更 {{ createdId }}</RouterLink>
  </section>
</template>
