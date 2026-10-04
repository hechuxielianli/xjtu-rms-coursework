<script setup lang="ts">
import { computed, watch } from 'vue';
import { business } from '../../api/business';
import type { ApplyChangeResponse, ChangeRequestResponse, RequirementResponse } from '../../api/contracts.generated';
import { useSession } from '../../auth/useSession';
import { canApplyChange } from '../../features/change';
import { useOperation, useScope } from '../../features/workspace';
import { ApiFailure } from '../../errors/api-error';
import ErrorPanel from '../ErrorPanel.vue';
const props = defineProps<{ parent: RequirementResponse; change: ChangeRequestResponse; loading?: boolean }>();
const emit = defineEmits<{ applied: [ApplyChangeResponse]; reload: [] }>();
const session = useSession(); const operation = useOperation(); const scope = useScope(() => {});
const eligible = computed(() => canApplyChange(session, props.parent, props.change));
async function apply() {
  if (props.loading || !eligible.value) return;
  const parent = props.parent; const change = props.change; const token = scope.start('apply-context');
  const input = { expectedRequirementLockVersion: parent.lockVersion, expectedChangeLockVersion: change.lockVersion };
  await operation.run(async current => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!current() || !scope.current(token) || !canApplyChange(session, parent, change)) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.applyChange(change.changeRequestId, parent.requirementId, input); }, value => emit('applied', value), () => props.parent.requirementId === parent.requirementId && props.parent.lockVersion === parent.lockVersion && props.change.changeRequestId === change.changeRequestId && props.change.lockVersion === change.lockVersion);
}
function reload() { emit('reload'); }
watch(() => [props.parent, props.change], () => { operation.clear(); });
</script>
<template>
  <section v-if="change.status === 'APPROVED' || change.status === 'APPLIED'" class="lifecycle-actions change-apply"><h3>{{ change.status === 'APPLIED' ? '变更已应用' : '批准后的显式应用' }}</h3>
    <p v-if="change.status === 'APPROVED'" class="muted">APPROVED 表示评审已批准，尚未改变需求或创建新版本。需求工程师确认应用后，服务器产生下一正式版本。</p>
    <p v-else class="muted">APPLIED 为终态。应用者 {{ change.appliedBy }} · {{ change.appliedAt }}。当前需求仍需对新的正式版本进行实现与验证确认。</p>
    <p v-if="change.status === 'APPROVED' && change.baseVersionId !== parent.currentVersionId" class="danger">基准版本已过期：申请基准 {{ change.baseVersionId }}，当前正式版本 {{ parent.currentVersionId }}。禁止应用。</p>
    <template v-if="eligible"><p class="muted small">基准／当前版本 {{ change.baseVersionId }} · 需求修订 {{ parent.lockVersion }} · 变更修订 {{ change.lockVersion }}。应用后需求回到 APPROVED，需重新实现与验证。</p><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value" @click="apply">确认应用批准变更</button></template>
    <ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="reload" />
  </section>
</template>
