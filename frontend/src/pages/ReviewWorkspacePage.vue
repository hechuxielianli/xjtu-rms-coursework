<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue';
import { RouterLink } from 'vue-router';
import { business } from '../api/business';
import type { RequirementReviewResponsePage } from '../api/contracts.generated';
import { useScope } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ErrorPanel from '../components/ErrorPanel.vue';
import PageControls from '../components/PageControls.vue';
import UtcTime from '../components/UtcTime.vue';
const page = ref(0); const size = ref(20); const result = shallowRef<RequirementReviewResponsePage | null>(null); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null);
const scope = useScope(() => { result.value = null; issue.value = null; });
async function load() { const token = scope.start('pending'); loading.value = true; issue.value = null; try { const value = await business.pendingReviews({ page: page.value, size: size.value }); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
onMounted(() => { void load(); });
</script>
<template>
  <div class="workspace-heading"><div><span class="eyebrow">REVIEW WORKSPACE</span><h1>待处理需求评审</h1><p class="muted">逐轮阅读不可变提交快照，再做出独立决策。</p></div><button class="secondary" :disabled="loading" @click="load">重新读取待评审列表</button></div><ErrorPanel :failure="issue" :busy="loading" @reload="load" /><section class="surface panel"><p v-if="loading" class="muted">正在读取待评审轮次…</p>
    <div class="table-scroll"><table class="review-table"><thead><tr><th>快照标题</th><th>需求 ID／轮次</th><th>提交者／时间</th><th>操作</th></tr></thead><tbody><tr v-for="review in result?.items" :key="review.reviewId"><td>{{ review.snapshotTitle }}</td><td class="id-cell">{{ review.requirementId }}<br />第 {{ review.roundNo }} 轮</td><td class="time-cell">{{ review.submittedBy }}<br /><UtcTime :value="review.submittedAt" /></td><td><RouterLink :to="{ path: `/reviews/${review.reviewId}`, query: { requirementId: review.requirementId } }">阅读快照并评审</RouterLink></td></tr></tbody></table></div><p v-if="result && !result.items.length && !loading" class="empty">暂无待处理需求评审。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
  </section>
</template>
