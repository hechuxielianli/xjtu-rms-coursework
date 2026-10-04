<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { business } from '../api/business';
import type { RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { canWithdrawDraft } from '../features/relation';
import { useOperation, useScope } from '../features/workspace';
import { ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
const props = defineProps<{ record: RequirementResponse }>(); const emit = defineEmits<{ updated: [RequirementResponse] }>(); const session = useSession(); const operation = useOperation(); const checking = ref(false); const count = ref<number | null>(null); const checkedRevision = ref<string | null>(null); const scope = useScope(() => { count.value = null; checkedRevision.value = null; });
const candidate = computed(() => canWithdrawDraft(session, props.record)); const eligible = computed(() => candidate.value && count.value === 0 && checkedRevision.value === props.record.lockVersion);
async function inspect() {
  if (checking.value || operation.busy.value) return; const token = scope.start('withdraw-context'); const id = props.record.requirementId; checking.value = true;
  try { await session.refreshSession(); if (!scope.current(token)) return; const parent = await business.requirement(id); if (!scope.current(token)) return; emit('updated', parent); const relations = await business.relations(id, { page: 0, size: 1 }); if (!scope.current(token)) return; count.value = relations.totalElements; checkedRevision.value = parent.lockVersion; operation.clear(); }
  catch (error) { if (scope.current(token)) operation.fail(error); }
  finally { if (scope.current(token)) checking.value = false; }
}
async function withdraw() {
  if (!eligible.value || checking.value || !window.confirm('确认逻辑撤销这条从未提交评审的草稿？原记录与只读历史将保留，之后不能再修改。')) return;
  const parent = props.record; const token = scope.start('withdraw-command');
  await operation.run(async current => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!current() || !scope.current(token) || !canWithdrawDraft(session, parent) || count.value !== 0 || checkedRevision.value !== parent.lockVersion) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.withdrawRequirement(parent.requirementId, { expectedLockVersion: parent.lockVersion }); }, value => { emit('updated', value); count.value = null; checkedRevision.value = null; }, () => props.record.requirementId === parent.requirementId && props.record.lockVersion === parent.lockVersion);
}
</script>
<template>
  <section class="withdraw-action"><h3>草稿逻辑撤销</h3><p v-if="record.isWithdrawn" class="muted">已逻辑撤销 · 操作者 {{ record.withdrawnBy }} · {{ record.withdrawnAt }}。原记录、版本、评审、评论与变更历史保留只读。</p><template v-else-if="candidate"><p class="muted small">只有从未进入评审的 DRAFT 且没有有效关系才能撤销。先核对最新修订与关系数量，服务器进行最终图锁校验。</p><button class="secondary" :disabled="checking || operation.busy.value" @click="inspect">核对草稿撤销条件</button><p v-if="count !== null" class="muted small">已核对修订 {{ checkedRevision }} · {{ count }} 条有效直接关系。</p><p v-if="count && count > 0" class="muted">请先解除有效关联。<RouterLink :to="{ path: `/requirements/${record.requirementId}`, query: { tab: 'relations' } }">打开需求关系</RouterLink></p><button v-if="eligible" class="secondary danger" :disabled="checking || operation.busy.value || operation.blocked.value" @click="withdraw">确认逻辑撤销草稿</button></template><p v-else class="muted small">当前权限、状态或首次提交记录不允许撤销；退回或重新打开的草稿也不能撤销。</p><ErrorPanel :failure="operation.issue.value" :busy="checking || operation.busy.value" @reload="inspect" />
  </section>
</template>
