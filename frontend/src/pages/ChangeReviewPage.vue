<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { business, requireId } from '../api/business';
import type { ChangeRequestResponse, ChangeRequestReviewResponse, Decision, RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { canDecideChange } from '../features/change';
import { useOperation, useScope, useUnsaved } from '../features/workspace';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import { displayLabel } from '../presentation/labels';
import ChangeSnapshotView from '../components/changes/ChangeSnapshotView.vue';
const route = useRoute(); const session = useSession(); const parent = shallowRef<RequirementResponse | null>(null); const change = shallowRef<ChangeRequestResponse | null>(null); const review = shallowRef<ChangeRequestReviewResponse | null>(null);
const loading = ref(false); const ready = ref(false); const issue = shallowRef<ApiFailure | null>(null); const decision = ref<Decision>('APPROVE'); const comment = ref(''); const operation = useOperation(); const dirty = computed(() => comment.value !== '' || decision.value !== 'APPROVE');
const scope = useScope(() => { parent.value = null; change.value = null; review.value = null; ready.value = false; issue.value = null; comment.value = ''; decision.value = 'APPROVE'; }); useUnsaved(dirty);
const eligible = computed(() => !!parent.value && !!change.value && !!review.value && canDecideChange(session, parent.value, change.value, review.value));
async function load() {
  const token = scope.start('review-context'); loading.value = true; ready.value = false; issue.value = null;
  try {
    const requirementId = requireId(route.query.requirementId); const changeId = requireId(route.query.changeRequestId); const reviewId = requireId(route.params.id);
    await session.refreshSession(); if (!scope.current(token)) return;
    const value = await business.change(changeId); if (!scope.current(token)) return; if (value.requirementId !== requirementId) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
    const record = await business.requirement(requirementId); if (!scope.current(token)) return;
    let match: ChangeRequestReviewResponse | undefined;
    for (let page = 0; ; page++) { const rounds = await business.changeReviews(changeId, requirementId, { page, size: 100 }); if (!scope.current(token)) return; match = rounds.items.find(item => item.changeReviewId === reviewId); if (match || (page + 1) * 100 >= rounds.totalElements) break; }
    if (!match) throw new ApiFailure('NOT_FOUND', 'not_found', 404); if (match.snapshotBaseVersionId !== value.baseVersionId) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
    parent.value = record; change.value = value; review.value = match; operation.clear(); ready.value = true;
  } catch (error) { if (scope.current(token)) { ready.value = false; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function decide() {
  if (loading.value || !ready.value || !parent.value || !change.value || !review.value) return;
  const record = parent.value; const value = change.value; const round = review.value; const token = scope.start('decision-context'); const input = { decision: decision.value, comment: comment.value === '' ? null : comment.value, expectedChangeLockVersion: value.lockVersion };
  await operation.run(async () => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!scope.current(token) || !canDecideChange(session, record, value, round)) throw new ApiFailure(value.createdBy === session.identity?.user.userId ? 'SELF_REVIEW' : 'FORBIDDEN', 'authorization', 403); return business.decideChangeReview(round.changeReviewId, value.changeRequestId, record.requirementId, input); }, async result => { comment.value = ''; decision.value = 'APPROVE'; change.value = result.change; review.value = result.review; await load(); });
}
watch(() => [route.params.id, route.query.requirementId, route.query.changeRequestId], () => { scope.invalidate(); operation.resetContext(); void load(); }, { immediate: true });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">变更提交快照</span><h1>变更评审详情</h1><p class="muted">本轮十一项快照来自提交时的变更申请；后续草稿修改不会替换本轮内容。</p></div><RouterLink v-if="change" :to="`/changes/${change.changeRequestId}`" class="secondary">返回变更详情与轮次</RouterLink></div><ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="!review && !loading" class="muted">评审链接需要有效的 changeRequestId 与 requirementId 上下文。请从待评审列表或变更轮次重新进入。</p><p v-if="loading" class="muted">正在核对变更、需求与评审轮次…</p>
  <section v-if="ready && parent && change && review" class="surface panel"><p class="review-subject">{{ parent.requirementKey }} · {{ review.snapshotRequestTitle }}</p><h2>第 {{ review.roundNo }} 轮 · {{ displayLabel(review.reviewStatus) }}</h2><p class="muted small">评审 {{ review.changeReviewId }} · 申请 {{ change.changeRequestId }} · 需求 {{ parent.requirementId }} · 提交者 {{ review.submittedBy }} · {{ review.submittedAt }}</p><ChangeSnapshotView :review="review" /><RouterLink :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'versions', versionId: review.snapshotBaseVersionId } }">查看本轮基准正式版本 {{ review.snapshotBaseVersionId }}</RouterLink>
    <section v-if="review.reviewStatus === 'COMPLETED'"><h3>已完成决策：{{ review.decision }}</h3><p class="text-content">{{ review.comment ?? '无附加意见' }}</p><p class="muted small">评审者 {{ review.reviewerId }} · {{ review.decidedAt }}。完成轮次保持只读。</p><p v-if="review.decision === 'APPROVE'" class="muted">本轮批准只完成评审；申请需要由需求工程师另行显式应用后才产生新正式版本。</p><p v-if="review.decision === 'REQUEST_CHANGES'" class="muted">本轮请求修改已保留；变更草稿重新提交时会形成新的独立评审轮次。</p></section>
    <form v-else-if="eligible" class="change-decision review-decision" @submit.prevent="decide" novalidate><h3>本轮变更决策</h3><p class="muted small">决策绑定读取的变更修订 {{ change.lockVersion }}。</p><label>结论<select v-model="decision" :disabled="operation.busy.value"><option value="APPROVE">批准</option><option value="REJECT">拒绝，进入终态</option><option value="REQUEST_CHANGES">请求修改，退回草稿</option></select></label><label>意见<textarea v-model="comment" rows="4" :disabled="operation.busy.value" /></label><small>拒绝或请求修改必须填写非空意见；批准可不填写。</small><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load" /><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">提交本轮变更评审决策</button></form>
    <p v-else class="muted">{{ change.createdBy === session.identity?.user.userId ? '不能评审自己提出的变更；拥有多个角色也不解除此限制。' : '当前账号、需求状态或变更评审轮次不允许决策。' }}</p>
  </section>
</template>
