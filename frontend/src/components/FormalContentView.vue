<script setup lang="ts">
import type { CreateRequirementRequest } from '../api/contracts.generated';
import { displayLabel } from '../presentation/labels';
defineProps<{ content: CreateRequirementRequest }>();
</script>
<template>
  <dl class="formal-content"><template v-for="field in [{ key: 'title', label: '标题' }, { key: 'description', label: '描述' }, { key: 'level', label: '层级' }, { key: 'kind', label: '性质' }, { key: 'priority', label: '优先级' }, { key: 'source', label: '来源' }, { key: 'rationale', label: '理由' }, { key: 'acceptanceCriteria', label: '验收标准' }]" :key="field.key"><dt>{{ field.label }}</dt><dd class="text-content"><template v-if="['level', 'kind', 'priority'].includes(field.key)">{{ displayLabel(String(content[field.key as keyof CreateRequirementRequest])) }} <small class="enum-code">({{ content[field.key as keyof CreateRequirementRequest] }})</small></template><template v-else>{{ content[field.key as keyof CreateRequirementRequest] ?? '—' }}</template></dd></template></dl>
</template>
