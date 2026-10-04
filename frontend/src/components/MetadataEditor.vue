<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { business, requireId } from '../api/business';
import type { RequirementResponse, TagResponse } from '../api/contracts.generated';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { useSession } from '../auth/useSession';
import ErrorPanel from './ErrorPanel.vue';
import TagCatalog from './TagCatalog.vue';
const props = defineProps<{ record: RequirementResponse }>();
const emit = defineEmits<{ updated: [RequirementResponse] }>();
const session = useSession();
const assignee = ref(props.record.assigneeId ?? ''); const selected = ref(props.record.tags.map(tag => tag.tagId)); const additionalId = ref('');
const dirty = ref(false); const tags = ref<TagResponse[]>([]); const loading = ref(false); const saved = ref(false); const operation = useOperation();
const options = computed(() => [...new Map([...props.record.tags, ...tags.value].map(tag => [tag.tagId, tag])).values()]);
const scope = useScope(() => { assignee.value = ''; selected.value = []; additionalId.value = ''; tags.value = []; dirty.value = false; });
useUnsaved(dirty);
watch(() => props.record, value => { if (!dirty.value) { assignee.value = value.assigneeId ?? ''; selected.value = value.tags.map(tag => tag.tagId); } });
async function reload() {
  const token = scope.start('record'); loading.value = true;
  const id = props.record.requirementId;
  try { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER'], () => scope.current(token)); const value = await business.requirement(id); if (scope.current(token)) { emit('updated', value); operation.clear(); } }
  catch (error) { if (scope.current(token)) operation.fail(error); }
  finally { if (scope.current(token)) loading.value = false; }
}
function addTag() { try { const id = requireId(additionalId.value); if (!selected.value.includes(id)) selected.value.push(id); additionalId.value = ''; dirty.value = true; if (!operation.blocked.value) operation.clear(); } catch (error) { if (!operation.blocked.value) operation.fail(error); } }
async function save() {
  if (props.record.isWithdrawn || loading.value) return;
  saved.value = false;
  const record = props.record; const rawAssignee = assignee.value; const tagIds = [...selected.value];
  await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER'], current); return business.editMetadata(record.requirementId, { assigneeId: rawAssignee === '' ? null : requireId(rawAssignee), tagIds, expectedLockVersion: record.lockVersion }); }, value => { dirty.value = false; emit('updated', value); assignee.value = value.assigneeId ?? ''; selected.value = value.tags.map(tag => tag.tagId); saved.value = true; }, () => props.record.requirementId === record.requirementId && props.record.lockVersion === record.lockVersion);
}
</script>
<template>
  <section><h3>负责人和标签</h3><p class="muted small">管理信息独立于正式内容；保存不会生成正式版本。</p>
    <form @submit.prevent="save" novalidate><fieldset :disabled="!!record.isWithdrawn || loading || operation.busy.value">
      <label>负责人 ID<input v-model="assignee" inputmode="numeric" placeholder="已知且启用的用户 ID，留空清除" @input="dirty = true; saved = false" /></label>
      <p v-if="record.assignee" class="muted small">当前负责人：{{ record.assignee.displayName }}（{{ record.assignee.userId }}）</p><small>填写已知用户 ID；服务端验证账号启用状态。</small>
      <div class="role-options"><label v-for="tag in options" :key="tag.tagId"><input v-model="selected" type="checkbox" :value="tag.tagId" @change="dirty = true; saved = false" />{{ tag.name }}（{{ tag.tagId }}）</label></div>
      <div class="actions"><label>其他已知标签 ID<input v-model="additionalId" inputmode="numeric" /></label><button type="button" class="secondary" @click="addTag">加入标签集合</button></div>
      <div class="actions"><span v-for="id in selected" :key="id" class="badge">{{ options.find(tag => tag.tagId === id)?.name ?? `标签 ${id}` }} <button type="button" class="link-button" @click="selected = selected.filter(value => value !== id); dirty = true">移除</button></span></div>
    </fieldset><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="reload" /><p v-if="saved" role="status">管理信息已保存。</p><div class="actions"><button class="primary" :disabled="!!record.isWithdrawn || loading || operation.busy.value || operation.blocked.value">保存管理信息</button></div></form>
    <TagCatalog @loaded="tags = $event" />
  </section>
</template>
