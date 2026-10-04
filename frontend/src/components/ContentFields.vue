<script setup lang="ts">
import type { CreateRequirementRequest } from '../api/contracts.generated';
import { LEVELS, KINDS, PRIORITIES } from '../features/workspace';
const props = defineProps<{ modelValue: CreateRequirementRequest; disabled?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [CreateRequirementRequest] }>();
function update(key: keyof CreateRequirementRequest, event: Event) {
  const value = (event.target as HTMLInputElement).value;
  emit('update:modelValue', { ...props.modelValue, [key]: ['source', 'rationale', 'acceptanceCriteria'].includes(key) && value === '' ? null : value });
}
</script>
<template>
  <fieldset :disabled="disabled" class="content-fields">
    <label>标题 <span class="required">*</span><input :value="modelValue.title" maxlength="200" required @input="update('title', $event)" /></label>
    <label>描述 <span class="required">*</span><textarea :value="modelValue.description" rows="4" required @input="update('description', $event)" /></label>
    <div class="form-grid">
      <label>层级 <select :value="modelValue.level" @change="update('level', $event)"><option v-for="v in LEVELS" :key="v">{{ v }}</option></select></label>
      <label>性质 <select :value="modelValue.kind" @change="update('kind', $event)"><option v-for="v in KINDS" :key="v">{{ v }}</option></select></label>
      <label>优先级 <select :value="modelValue.priority" @change="update('priority', $event)"><option v-for="v in PRIORITIES" :key="v">{{ v }}</option></select></label>
    </div>
    <label>来源<textarea :value="modelValue.source ?? ''" rows="2" @input="update('source', $event)" /></label>
    <label>理由<textarea :value="modelValue.rationale ?? ''" rows="2" @input="update('rationale', $event)" /></label>
    <label>验收标准<textarea :value="modelValue.acceptanceCriteria ?? ''" rows="3" @input="update('acceptanceCriteria', $event)" /></label>
    <small>标题、描述、层级、性质和优先级为创建必填项。来源、理由和验收标准可在草稿中逐步补齐。</small>
  </fieldset>
</template>
