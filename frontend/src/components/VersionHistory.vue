<script setup lang="ts">
import { ref, shallowRef, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { business, requireId } from '../api/business';
import type { RequirementResponse, RequirementVersionResponse, RequirementVersionResponsePage } from '../api/contracts.generated';
import { useScope } from '../features/workspace';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import FormalContentView from './FormalContentView.vue';
import ErrorPanel from './ErrorPanel.vue';
import PageControls from './PageControls.vue';
import UtcTime from './UtcTime.vue';
const props = defineProps<{ record: RequirementResponse }>(); const route = useRoute(); const router = useRouter();
const page = ref(0); const size = ref(20); const result = shallowRef<RequirementVersionResponsePage | null>(null); const selected = shallowRef<RequirementVersionResponse | null>(null);
const loading = ref(false); const detailLoading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const detailIssue = shallowRef<ApiFailure | null>(null);
const scope = useScope(() => { result.value = null; selected.value = null; issue.value = null; detailIssue.value = null; });
async function load() { const token = scope.start('versions'); loading.value = true; issue.value = null; try { const value = await business.versions(props.record.requirementId, { page: page.value, size: size.value }); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function loadVersion() {
  const token = scope.start('version'); selected.value = null; detailIssue.value = null; detailLoading.value = false;
  if (route.query.versionId === undefined) return;
  detailLoading.value = true;
  try { const value = await business.version(props.record.requirementId, requireId(route.query.versionId)); if (scope.current(token)) selected.value = value; }
  catch (error) { if (scope.current(token)) detailIssue.value = normalizeFailure(error); }
  finally { if (scope.current(token)) detailLoading.value = false; }
}
async function choose(versionId: string) { await router.replace({ path: route.path, query: { ...route.query, tab: 'versions', versionId } }); }
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
watch(() => [props.record.requirementId, props.record.currentVersionId], () => { void load(); void loadVersion(); }, { immediate: true });
watch(() => route.query.versionId, () => { void loadVersion(); });
</script>
<template>
  <section class="version-history" :class="{ 'has-version-detail': !!selected }"><h2>正式版本</h2><p class="muted small">当前正式版本 ID：{{ record.currentVersionId ?? '尚未形成正式版本' }}。历史版本为完整只读快照。</p><ErrorPanel :failure="issue" :busy="loading" @reload="load" />
    <div class="table-scroll"><table><thead><tr><th>版本／ID</th><th>产生者／时间</th><th>原因与来源</th><th>操作</th></tr></thead><tbody><tr v-for="version in result?.items" :key="version.versionId"><td>V{{ version.versionNo }} <span v-if="version.versionId === record.currentVersionId" class="badge">当前</span><br />{{ version.versionId }}</td><td class="time-cell">{{ version.createdBy }}<br /><UtcTime :value="version.createdAt" /></td><td class="text-content">{{ version.changeReason }}<br /><RouterLink v-if="version.initialReviewId" :to="{ path: `/reviews/${version.initialReviewId}`, query: { requirementId: record.requirementId } }">批准评审 {{ version.initialReviewId }}</RouterLink><RouterLink v-else-if="version.appliedChangeRequestId" :to="`/changes/${version.appliedChangeRequestId}`">来源变更 {{ version.appliedChangeRequestId }}</RouterLink></td><td><button class="secondary compact" :disabled="detailLoading" @click="choose(version.versionId)">查看完整版本</button></td></tr></tbody></table></div>
    <p v-if="result && !result.items.length && !loading" class="empty">首次批准前尚无正式版本。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
    <ErrorPanel :failure="detailIssue" :busy="detailLoading" @reload="loadVersion" /><p v-if="detailLoading" class="muted">正在读取版本…</p>
    <section v-if="selected" class="surface panel"><h3>V{{ selected.versionNo }} · {{ selected.versionId }}</h3><p class="muted small">{{ selected.createdBy }} · {{ selected.createdAt }} · {{ selected.changeReason }}</p><FormalContentView :content="selected" /></section>
  </section>
</template>
