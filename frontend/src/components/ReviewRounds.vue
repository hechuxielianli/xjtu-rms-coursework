<script setup lang="ts">
import { ref, shallowRef, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { business } from '../api/business';
import type { RequirementResponse, RequirementReviewResponsePage } from '../api/contracts.generated';
import { useScope } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
import PageControls from './PageControls.vue';
import UtcTime from './UtcTime.vue';
const props = defineProps<{ record: RequirementResponse }>();
const page = ref(0); const size = ref(20); const result = shallowRef<RequirementReviewResponsePage | null>(null); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null);
const scope = useScope(() => { result.value = null; issue.value = null; });
async function load() { const token = scope.start('rounds'); loading.value = true; issue.value = null; try { const value = await business.reviews(props.record.requirementId, { page: page.value, size: size.value }); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
watch(() => [props.record.requirementId, props.record.lockVersion], () => { void load(); }, { immediate: true });
</script>
<template>
  <section><h2>需求评审轮次</h2><p class="muted small">每次提交形成独立快照；完成的意见和决策只读。</p><ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="loading" class="muted">正在读取评审轮次…</p>
    <div class="table-scroll"><table class="review-table"><thead><tr><th>轮次／快照标题</th><th>提交者／时间</th><th>状态／决策</th><th>评审者／时间</th><th>操作</th></tr></thead><tbody><tr v-for="review in result?.items" :key="review.reviewId"><td>第 {{ review.roundNo }} 轮<br />{{ review.snapshotTitle }}</td><td class="time-cell">{{ review.submittedBy }}<br /><UtcTime :value="review.submittedAt" /></td><td class="enum-cell">{{ review.reviewStatus }}<br />{{ review.decision ?? '待决策' }}</td><td class="time-cell">{{ review.reviewerId ?? '—' }}<br /><UtcTime :value="review.decidedAt" /></td><td><RouterLink :to="{ path: `/reviews/${review.reviewId}`, query: { requirementId: record.requirementId } }">查看快照与决策</RouterLink></td></tr></tbody></table></div>
    <p v-if="result && !result.items.length && !loading" class="empty">尚未提交评审。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
  </section>
</template>
