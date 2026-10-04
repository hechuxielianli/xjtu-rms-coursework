<script setup lang="ts">
import { computed, ref } from 'vue';
import { business } from '../api/business';
import type { RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { completeContent } from '../features/review';
import { ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
const props = defineProps<{ record: RequirementResponse }>();
const emit = defineEmits<{ updated: [RequirementResponse]; changed: [] }>();
const session = useSession(); const operation = useOperation(); const note = ref(''); const loading = ref(false);
const dirty = computed(() => note.value !== ''); const scope = useScope(() => { note.value = ''; }); useUnsaved(dirty);
const engineer = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER']) && props.record.isWithdrawn === 0);
const member = computed(() => session.hasAnyRole(['PROJECT_MEMBER']) && props.record.isWithdrawn === 0 && !!props.record.currentVersionId);
const confirmAction = computed(() => member.value && props.record.status === 'APPROVED' ? 'implement' : member.value && props.record.status === 'IMPLEMENTED' ? 'verify' : null);
async function reload() { const token = scope.start('context'); const id = props.record.requirementId; loading.value = true; try { await session.refreshSession(); if (!scope.current(token)) return; const value = await business.requirement(id); if (scope.current(token)) { emit('updated', value); operation.clear(); } } catch (error) { if (scope.current(token)) operation.fail(error); } finally { if (scope.current(token)) loading.value = false; } }
async function perform(action: 'submit' | 'reopen' | 'implement' | 'verify') {
  if (loading.value) return;
  const record = props.record; const description = note.value;
  await operation.run(async current => {
    await requireFreshRoles(session, action === 'submit' || action === 'reopen' ? ['REQUIREMENT_ENGINEER'] : ['PROJECT_MEMBER'], current);
    if (record.isWithdrawn || !session.hasAnyRole(action === 'submit' || action === 'reopen' ? ['REQUIREMENT_ENGINEER'] : ['PROJECT_MEMBER'])) throw new ApiFailure('FORBIDDEN', 'authorization', 403);
    if (action === 'submit') {
      if (record.status !== 'DRAFT' || !completeContent(record)) throw new ApiFailure('INCOMPLETE_CONTENT', 'validation', 400);
      await business.submitRequirement(record.requirementId, { expectedLockVersion: record.lockVersion }); return null;
    }
    if (action === 'reopen') { if (record.status !== 'REJECTED') throw new ApiFailure('STATE_CONFLICT', 'conflict', 409); return business.reopenRequirement(record.requirementId, { expectedLockVersion: record.lockVersion }); }
    if (record.status !== (action === 'implement' ? 'APPROVED' : 'IMPLEMENTED') || !record.currentVersionId) throw new ApiFailure('STATE_CONFLICT', 'conflict', 409);
    return business.confirmVersion(record.requirementId, action, { expectedVersionId: record.currentVersionId, expectedLockVersion: record.lockVersion, description });
  }, value => { note.value = ''; if (value) emit('updated', value); else emit('changed'); }, () => props.record.requirementId === record.requirementId && props.record.lockVersion === record.lockVersion);
}
</script>
<template>
  <section class="lifecycle-actions"><h3>生命周期操作</h3><p class="muted small">当前状态 {{ record.status }} · 修订 {{ record.lockVersion }} · 当前正式版本 {{ record.currentVersionId ?? '尚未形成' }}</p>
    <p v-if="engineer && record.status === 'DRAFT' && !completeContent(record)" class="muted small">提交评审前需补齐八项正式内容，并确保验收标准可判定。服务器进行最终完整性检查。</p>
    <ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="reload" />
    <div class="actions"><button v-if="engineer && record.status === 'DRAFT'" class="primary" :disabled="loading || operation.busy.value || operation.blocked.value || !completeContent(record)" @click="perform('submit')">提交评审</button><button v-if="engineer && record.status === 'REJECTED'" class="primary" :disabled="loading || operation.busy.value || operation.blocked.value" @click="perform('reopen')">重新打开草稿</button></div>
    <form v-if="confirmAction" @submit.prevent="perform(confirmAction)" novalidate><label>{{ confirmAction === 'implement' ? '实现说明' : '验证说明' }}<textarea v-model="note" rows="3" :disabled="operation.busy.value" /></label><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">{{ confirmAction === 'implement' ? '确认实现当前正式版本' : '确认验证当前正式版本' }}</button></form>
    <p v-if="record.isWithdrawn" class="muted">已撤销需求保持只读。</p>
  </section>
</template>
