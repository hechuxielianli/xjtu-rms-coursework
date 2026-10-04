<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { business, requireId } from '../api/business';
import type { Decision, RequirementResponse, RequirementReviewResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { canDecide, snapshotContent } from '../features/review';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import { displayLabel } from '../presentation/labels';
import FormalContentView from '../components/FormalContentView.vue';
const route = useRoute(); const session = useSession(); const parent = shallowRef<RequirementResponse | null>(null); const review = shallowRef<RequirementReviewResponse | null>(null);
const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const decision = ref<Decision>('APPROVE'); const comment = ref(''); const operation = useOperation();
const dirty = computed(() => comment.value !== '' || decision.value !== 'APPROVE');
const scope = useScope(() => { parent.value = null; review.value = null; issue.value = null; comment.value = ''; decision.value = 'APPROVE'; }); useUnsaved(dirty);
const eligible = computed(() => !!parent.value && !!review.value && canDecide(session, parent.value, review.value));
async function load() {
  const token = scope.start('review-context'); loading.value = true; issue.value = null;
  try {
    const requirementId = requireId(route.query.requirementId); const reviewId = requireId(route.params.id);
    await session.refreshSession();
    if (!scope.current(token)) return;
    const record = await business.requirement(requirementId);
    if (!scope.current(token)) return;
    let match: RequirementReviewResponse | undefined;
    for (let page = 0; ; page++) { const rounds = await business.reviews(requirementId, { page, size: 100 }); if (!scope.current(token)) return; match = rounds.items.find(item => item.reviewId === reviewId); if (match || (page + 1) * 100 >= rounds.totalElements) break; }
    if (!match) throw new ApiFailure('NOT_FOUND', 'not_found', 404);
    if (scope.current(token)) { parent.value = record; review.value = match; operation.clear(); }
  } catch (error) { if (scope.current(token)) { parent.value = null; review.value = null; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function decide() {
  if (loading.value || !parent.value || !review.value) return;
  const record = parent.value; const round = review.value; const input = { decision: decision.value, comment: comment.value === '' ? null : comment.value, expectedRequirementLockVersion: record.lockVersion };
  await operation.run(async current => { await requireFreshRoles(session, ['REVIEWER'], current); if (!canDecide(session, record, round)) throw new ApiFailure(record.creatorId === session.identity?.user.userId ? 'SELF_REVIEW' : 'FORBIDDEN', 'authorization', 403); return business.decideReview(round.reviewId, record.requirementId, input); }, async value => { comment.value = ''; decision.value = 'APPROVE'; parent.value = value.requirement; review.value = value.review; await load(); });
}
watch(() => [route.params.id, route.query.requirementId], () => { scope.invalidate(); operation.resetContext(); void load(); }, { immediate: true });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">需求提交快照</span><h1>需求评审详情</h1><p class="muted">决策针对本轮提交快照；当前草稿的后续内容不会替代快照。</p></div><RouterLink v-if="parent" :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'reviews' } }" class="secondary">返回需求评审历史</RouterLink></div>
  <ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="!review && !loading" class="muted">评审链接需要有效的 requirementId 上下文。可从需求评审历史或待评审列表重新进入。</p><p v-if="loading" class="muted">正在核对需求与评审轮次…</p>
  <section v-if="parent && review" class="surface panel review-panel"><p class="review-subject">{{ parent.requirementKey }} · {{ review.snapshotTitle }}</p><h2>第 {{ review.roundNo }} 轮 · {{ displayLabel(review.reviewStatus) }}</h2><p class="muted small">评审 {{ review.reviewId }} · 需求 {{ review.requirementId }} · 提交者 {{ review.submittedBy }} · {{ review.submittedAt }}</p><FormalContentView :content="snapshotContent(review)" />
    <section v-if="review.reviewStatus === 'COMPLETED'"><h3>已完成决策：{{ review.decision }}</h3><p class="text-content">{{ review.comment ?? '无附加意见' }}</p><p class="muted small">评审者 {{ review.reviewerId }} · {{ review.decidedAt }}。完成轮次保持只读。</p><RouterLink v-if="parent.currentVersionId" :to="{ path: `/requirements/${parent.requirementId}`, query: { tab: 'versions', versionId: parent.currentVersionId } }">打开当前正式版本 {{ parent.currentVersionId }}</RouterLink></section>
    <form v-else-if="eligible" class="review-decision" @submit.prevent="decide" novalidate><h3>本轮决策</h3><label>结论<select v-model="decision" :disabled="operation.busy.value"><option value="APPROVE">批准</option><option value="REJECT">拒绝</option><option value="REQUEST_CHANGES">请求修改，退回草稿</option></select></label><label>意见<textarea v-model="comment" rows="4" :disabled="operation.busy.value" /></label><small>拒绝或请求修改必须填写非空意见；批准可不填写。</small><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load" /><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">提交本轮评审决策</button></form>
    <p v-else class="muted">{{ parent.creatorId === session.identity?.user.userId ? '不能评审自己创建的需求；拥有多个角色也不解除此限制。' : '当前账号、需求状态或评审轮次不允许决策。' }}</p>
  </section>
</template>
