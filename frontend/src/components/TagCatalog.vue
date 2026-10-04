<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { business } from '../api/business';
import type { TagResponse, TagResponsePage } from '../api/contracts.generated';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { useSession } from '../auth/useSession';
import { normalizeFailure, type ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
import PageControls from './PageControls.vue';
const emit = defineEmits<{ loaded: [TagResponse[]] }>();
const session = useSession(); const engineer = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER']));
const page = ref(0); const size = ref(20); const nameFilter = ref(''); const result = shallowRef<TagResponsePage | null>(null);
const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const editing = shallowRef<TagResponse | null>(null);
const name = ref(''); const description = ref(''); const dirty = ref(false); const operation = useOperation();
const scope = useScope(() => { result.value = null; name.value = ''; description.value = ''; editing.value = null; dirty.value = false; issue.value = null; });
useUnsaved(dirty);
async function load(reconcile = false) {
  const token = scope.start('tags'); loading.value = true; issue.value = null;
  try { const value = await business.tags({ page: page.value, size: size.value }, nameFilter.value); if (scope.current(token)) { result.value = value; emit('loaded', value.items); if (reconcile) operation.clear(); } }
  catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
function choose(tag: TagResponse | null) { if (operation.busy.value || operation.blocked.value) return; if (dirty.value && !window.confirm('确认放弃当前标签表单修改？')) return; editing.value = tag; name.value = tag?.name ?? ''; description.value = tag?.description ?? ''; dirty.value = false; operation.clear(); }
async function save() {
  if (loading.value) return;
  const input = { name: name.value, description: description.value === '' ? null : description.value }; const tag = editing.value;
  await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER'], current); return tag ? business.editTag(tag.tagId, input) : business.createTag(input); }, async () => { dirty.value = false; editing.value = null; name.value = ''; description.value = ''; await load(); });
}
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
onMounted(() => { void load(); });
</script>
<template>
  <details class="tag-catalog"><summary>维护标签目录</summary><p class="muted small">标签可重复用于多个需求；修改名称与说明不会产生正式需求版本。</p>
    <form @submit.prevent="page = 0; load()" class="actions"><label>按名称查找<input v-model="nameFilter" maxlength="50" /></label><button class="secondary" :disabled="loading">查找标签</button></form>
    <ErrorPanel :failure="issue" :busy="loading" @reload="load(true)" />
    <div class="table-scroll"><table><thead><tr><th>ID／名称</th><th>说明</th><th>操作</th></tr></thead><tbody><tr v-for="tag in result?.items" :key="tag.tagId"><td>{{ tag.tagId }} · {{ tag.name }}</td><td class="text-content">{{ tag.description ?? '—' }}</td><td><button v-if="engineer" class="secondary compact" :disabled="operation.busy.value || operation.blocked.value" @click="choose(tag)">编辑</button></td></tr></tbody></table></div>
    <PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
    <form v-if="engineer" @submit.prevent="save" novalidate><h3>{{ editing ? `编辑标签 ${editing.tagId}` : '创建标签' }}</h3><label>名称<input v-model="name" maxlength="50" :disabled="operation.busy.value" @input="dirty = true" /></label><label>说明<textarea v-model="description" maxlength="500" rows="2" :disabled="operation.busy.value" @input="dirty = true" /></label>
      <ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" />
      <div class="actions"><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">保存标签</button><button class="secondary" type="button" :disabled="operation.busy.value || operation.blocked.value" @click="choose(null)">清空表单</button></div>
    </form>
  </details>
</template>
