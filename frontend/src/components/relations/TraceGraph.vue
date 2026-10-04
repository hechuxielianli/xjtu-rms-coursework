<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { business, type TraceOptions } from '../../api/business';
import type { RelationType, RequirementResponse, TraceResponse } from '../../api/contracts.generated';
import { RELATION_TYPES, symmetricRelation, traceProjection } from '../../features/relation';
import { useScope } from '../../features/workspace';
import { normalizeFailure, type ApiFailure } from '../../errors/api-error';
import ErrorPanel from '../ErrorPanel.vue';
const props = defineProps<{ record: RequirementResponse; refreshKey: number }>(); const depth = ref(2); const relationType = ref<RelationType | ''>(''); const result = shallowRef<TraceResponse | null>(null); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); let applied: TraceOptions = { depth: 2 };
const scope = useScope(() => { result.value = null; issue.value = null; }); const names = computed(() => new Map(result.value?.nodes.map(node => [node.requirementId, node.requirementKey]) ?? []));
async function load() { const token = scope.start('trace'); loading.value = true; issue.value = null; const input = { ...applied }; try { const value = await business.trace(props.record.requirementId, input); if (scope.current(token)) result.value = traceProjection(value); } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function trace() { applied = { depth: depth.value, relationType: relationType.value || undefined }; await load(); }
watch([() => props.record.requirementId, () => props.refreshKey], () => { void load(); }, { immediate: true });
</script>
<template>
  <section class="trace-graph"><h2>当前关系追踪</h2><p class="muted small">按有限深度读取当前关系图。节点与关系按 ID 去重，一次显示服务器返回的结果；历史正式内容可从节点需求的版本页查看。</p><form class="trace-filters form-grid" @submit.prevent="trace" novalidate><label>追踪深度<input v-model.number="depth" type="number" min="1" max="10" :disabled="loading" /></label><label>关系类型<select v-model="relationType" :disabled="loading"><option value="">全部类型</option><option v-for="type in RELATION_TYPES" :key="type">{{ type }}</option></select></label><div class="actions"><button class="secondary" :disabled="loading">读取追踪结果</button></div></form><ErrorPanel :failure="issue" :busy="loading" @reload="load" /><p v-if="loading" class="muted">正在读取有限深度关系图…</p>
    <template v-if="result"><p class="muted small">根需求 {{ result.rootRequirementId }} · 深度 {{ result.depth }} · {{ result.nodes.length }} 个节点 · {{ result.edges.length }} 条关系。{{ result.truncated ? '结果已截断，边界外可能还有关系。' : '服务器返回的当前深度结果未截断。' }}</p><ul class="trace-nodes"><li v-for="node in result.nodes" :key="node.requirementId"><RouterLink :to="`/requirements/${node.requirementId}`">{{ node.requirementKey }} · {{ node.title }}</RouterLink><p class="muted small">{{ node.requirementId }} · {{ node.status }}<span v-if="node.requirementId === result.rootRequirementId"> · 根节点</span><span v-if="node.isWithdrawn"> · 已撤销，只读</span></p></li></ul>
      <div class="table-scroll"><table><thead><tr><th>源需求</th><th>关系类型／方向</th><th>目标需求</th><th>说明</th></tr></thead><tbody><tr v-for="edge in result.edges" :key="edge.relationId"><td><RouterLink :to="`/requirements/${edge.sourceRequirementId}`">{{ names.get(edge.sourceRequirementId) ?? edge.sourceRequirementId }}</RouterLink><br />{{ edge.sourceRequirementId }}</td><td>{{ edge.relationType }}<br />{{ symmetricRelation(edge.relationType) ? '↔ 对称' : '→ 有向' }}</td><td><RouterLink :to="`/requirements/${edge.targetRequirementId}`">{{ names.get(edge.targetRequirementId) ?? edge.targetRequirementId }}</RouterLink><br />{{ edge.targetRequirementId }}</td><td class="text-content">{{ edge.description ?? '—' }}</td></tr></tbody></table></div><p v-if="!result.edges.length" class="empty">当前追踪条件下没有关系。</p></template>
  </section>
</template>
<style scoped>
.trace-nodes { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; padding: 0; list-style: none; }
.trace-nodes li { padding: 14px; border: 1px solid #dce5df; border-radius: 8px; overflow-wrap: anywhere; }
.trace-nodes p { margin: 7px 0 0; }
</style>
