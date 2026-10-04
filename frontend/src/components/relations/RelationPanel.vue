<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { business, requireId } from '../../api/business';
import type { RelationType, RequirementRelationResponse, RequirementRelationResponsePage, RequirementResponse } from '../../api/contracts.generated';
import { useSession } from '../../auth/useSession';
import { RELATION_TYPES, canManageRelations, relationCommand, relationDirection, symmetricRelation } from '../../features/relation';
import { useOperation, useScope, useUnsaved } from '../../features/workspace';
import { ApiFailure, normalizeFailure } from '../../errors/api-error';
import ErrorPanel from '../ErrorPanel.vue';
import PageControls from '../PageControls.vue';
import TraceGraph from './TraceGraph.vue';
const props = defineProps<{ record: RequirementResponse }>(); const emit = defineEmits<{ updated: [RequirementResponse] }>(); const session = useSession(); const page = ref(0); const size = ref(20); const result = shallowRef<RequirementRelationResponsePage | null>(null); const loading = ref(false); const reading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const graphRefresh = ref(0);
const otherId = ref(''); const other = shallowRef<RequirementResponse | null>(null); const type = ref<RelationType>('DEPENDS_ON'); const incoming = ref(false); const description = ref(''); const created = shallowRef<RequirementRelationResponse | null>(null); const deletion = shallowRef<{ relation: RequirementRelationResponse; source: RequirementResponse; target: RequirementResponse } | null>(null); const operation = useOperation();
const scope = useScope(() => { result.value = null; otherId.value = ''; other.value = null; description.value = ''; issue.value = null; created.value = null; deletion.value = null; }); const dirty = computed(() => otherId.value !== '' || description.value !== ''); useUnsaved(dirty);
const manageable = computed(() => canManageRelations(session, props.record)); const createAllowed = computed(() => manageable.value && !!other.value && other.value.requirementId === otherId.value && other.value.isWithdrawn === 0 && other.value.requirementId !== props.record.requirementId); const deleteAllowed = computed(() => manageable.value && !!deletion.value && deletion.value.source.isWithdrawn === 0 && deletion.value.target.isWithdrawn === 0);
async function load() { const token = scope.start('relations'); loading.value = true; issue.value = null; try { const value = await business.relations(props.record.requirementId, { page: page.value, size: size.value }); if (scope.current(token)) result.value = value; } catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } } finally { if (scope.current(token)) loading.value = false; } }
async function inspectOther() {
  if (reading.value || operation.busy.value) return; const token = scope.start('other'); reading.value = true; issue.value = null; other.value = null;
  try { const id = requireId(otherId.value); if (id === props.record.requirementId) throw new ApiFailure('INVALID_INPUT', 'validation', 400); const value = await business.requirement(id); if (scope.current(token) && otherId.value === id) other.value = value; }
  catch (error) { if (scope.current(token)) issue.value = normalizeFailure(error); }
  finally { if (scope.current(token)) reading.value = false; }
}
async function prepareDelete(relation: RequirementRelationResponse) {
  if (!manageable.value || operation.busy.value || reading.value) return; const token = scope.start('delete-context'); reading.value = true; issue.value = null; deletion.value = null;
  try { const [source, target] = await Promise.all([business.requirement(relation.sourceRequirementId), business.requirement(relation.targetRequirementId)]); if (scope.current(token)) deletion.value = { relation, source, target }; }
  catch (error) { if (scope.current(token)) issue.value = normalizeFailure(error); }
  finally { if (scope.current(token)) reading.value = false; }
}
async function refreshContext(reconcile = false) {
  const token = scope.start('refresh'); reading.value = true; issue.value = null;
  try {
    await session.refreshSession(); if (!scope.current(token)) return;
    const parent = await business.requirement(props.record.requirementId); if (!scope.current(token)) return; emit('updated', parent);
    if (other.value) { const id = other.value.requirementId; const value = await business.requirement(id); if (!scope.current(token)) return; if (otherId.value === id) other.value = value; }
    const pending = deletion.value; if (pending) { const [source, target] = await Promise.all([business.requirement(pending.relation.sourceRequirementId), business.requirement(pending.relation.targetRequirementId)]); if (!scope.current(token)) return; deletion.value = { ...pending, source, target }; }
    await load(); if (!scope.current(token) || issue.value) return; if (pending && !result.value?.items.some(item => item.relationId === pending.relation.relationId)) deletion.value = null; graphRefresh.value++; if (reconcile) operation.clear();
  } catch (error) { if (scope.current(token)) issue.value = normalizeFailure(error); }
  finally { if (scope.current(token)) reading.value = false; }
}
async function create() {
  if (!createAllowed.value || !other.value || reading.value || loading.value) return; const parent = props.record; const counterpart = other.value; const token = scope.start('create-context'); const command = relationCommand(parent, counterpart, type.value, incoming.value, description.value);
  await operation.run(async current => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!current() || !scope.current(token) || !canManageRelations(session, parent) || counterpart.isWithdrawn) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.createRelation(command); }, async value => { created.value = value; otherId.value = ''; other.value = null; description.value = ''; deletion.value = null; await refreshContext(); }, () => props.record.requirementId === parent.requirementId && props.record.lockVersion === parent.lockVersion);
}
async function remove() {
  if (!deleteAllowed.value || !deletion.value || reading.value || loading.value) return; const pending = deletion.value; const parent = props.record; const token = scope.start('remove-context');
  await operation.run(async current => { if (!await session.refreshSession()) throw new ApiFailure('FORBIDDEN', 'authorization', 403); if (!current() || !scope.current(token) || !canManageRelations(session, parent) || pending.source.isWithdrawn || pending.target.isWithdrawn) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.deleteRelation(pending.relation.relationId, { expectedSourceLockVersion: pending.source.lockVersion, expectedTargetLockVersion: pending.target.lockVersion }); }, async () => { deletion.value = null; await refreshContext(); }, () => props.record.requirementId === parent.requirementId && props.record.lockVersion === parent.lockVersion);
}
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
watch([() => props.record.requirementId, () => props.record.lockVersion], () => { void load(); }, { immediate: true });
</script>
<template>
  <section class="relation-panel"><h2>当前直接关系</h2><p class="muted small">DEPENDS_ON、REFINES、DERIVED_FROM 有方向；CONFLICTS_WITH、DUPLICATES、RELATES_TO 对称。对称关系按服务器返回的规范源／目标展示。</p><button class="secondary" :disabled="loading || reading || operation.busy.value" @click="refreshContext(true)">重新读取关系与两端上下文</button><ErrorPanel :failure="issue" :busy="loading || reading" @reload="refreshContext(true)" /><ErrorPanel :failure="operation.issue.value" :busy="loading || reading || operation.busy.value" @reload="refreshContext(true)" />
    <p v-if="operation.issue.value?.code === 'DUPLICATE'" class="danger">相同需求对与关系类型已经存在；对称关系的反向输入也视为重复。请核对现有关系。</p>
    <div class="table-scroll"><table><thead><tr><th>源需求</th><th>类型／相对方向</th><th>目标需求</th><th>说明</th><th>操作</th></tr></thead><tbody><tr v-for="relation in result?.items" :key="relation.relationId"><td><RouterLink :to="`/requirements/${relation.sourceRequirementId}`">{{ relation.sourceRequirementId }}</RouterLink></td><td>{{ relation.relationType }}<br />{{ relationDirection(relation, record.requirementId) }}</td><td><RouterLink :to="`/requirements/${relation.targetRequirementId}`">{{ relation.targetRequirementId }}</RouterLink></td><td class="text-content">{{ relation.description ?? '—' }}</td><td><button v-if="manageable" class="secondary compact danger" :disabled="loading || reading || operation.busy.value || operation.blocked.value" @click="prepareDelete(relation)">核对两端后解除</button></td></tr></tbody></table></div><p v-if="result && !result.items.length && !loading" class="empty">当前没有直接关系。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading || reading" @page="paginate" @size="resize" />
    <section v-if="deletion" class="relation-delete"><h3>确认解除关系 {{ deletion.relation.relationId }}</h3><p class="muted small">返回的源需求 {{ deletion.source.requirementId }} · 修订 {{ deletion.source.lockVersion }}<br />返回的目标需求 {{ deletion.target.requirementId }} · 修订 {{ deletion.target.lockVersion }}</p><p>解除仅移除关系，保留两端需求。{{ symmetricRelation(deletion.relation.relationType) ? '这是规范排序后的对称关系，两个修订号分别对应上面的源和目标。' : '方向与返回的源、目标保持一致。' }}</p><p v-if="!deleteAllowed" class="muted">当前权限或两端撤销状态禁止解除。</p><div class="actions"><button v-if="deleteAllowed" class="secondary danger" :disabled="loading || reading || operation.busy.value || operation.blocked.value" @click="remove">确认解除关系</button><button class="secondary" :disabled="operation.busy.value" @click="deletion = null">关闭确认</button></div></section>
    <form v-if="manageable" class="relation-create" @submit.prevent="create" novalidate><h3>建立新关系</h3><label>对端需求 ID<input v-model="otherId" inputmode="numeric" :disabled="reading || operation.busy.value" @input="other = null" /></label><p v-if="otherId === record.requirementId" class="danger">不能关联自身：源与目标必须是不同需求。</p><button type="button" class="secondary" :disabled="reading || operation.busy.value" @click="inspectOther">读取并核对对端需求</button><p v-if="other" class="muted small">{{ other.requirementKey }} · {{ other.title }} · 修订 {{ other.lockVersion }}{{ other.isWithdrawn ? ' · 已撤销，不可写' : '' }}</p><div class="form-grid"><label>类型<select v-model="type" :disabled="operation.busy.value"><option v-for="value in RELATION_TYPES" :key="value">{{ value }}</option></select></label><label>输入源／目标<select v-model="incoming" :disabled="operation.busy.value"><option :value="false">当前需求 → 对端需求</option><option :value="true">对端需求 → 当前需求</option></select></label></div><p v-if="symmetricRelation(type)" class="muted small">该类型为对称关系；服务器统一源／目标顺序。请求修订号仍对应你在上面选择的输入源与目标。</p><label>说明<textarea v-model="description" rows="3" maxlength="1000" :disabled="operation.busy.value" /></label><p v-if="other" class="muted small">当前需求 {{ record.requirementId }} · 修订 {{ record.lockVersion }}；对端 {{ other.requirementId }} · 修订 {{ other.lockVersion }}。</p><button class="primary" :disabled="!createAllowed || loading || reading || operation.busy.value || operation.blocked.value">建立关系</button><small>禁止自关联和完全重复关系；DEPENDS_ON 不得形成环。服务器在同一图锁事务中最终校验。</small></form><p v-else class="muted">当前账号或需求撤销状态仅允许读取关系。</p><p v-if="created" role="status">已建立 {{ created.relationType }}：{{ created.sourceRequirementId }} {{ symmetricRelation(created.relationType) ? '↔' : '→' }} {{ created.targetRequirementId }}（关系 {{ created.relationId }}）。</p>
    <TraceGraph :record="record" :refresh-key="graphRefresh" />
  </section>
</template>
