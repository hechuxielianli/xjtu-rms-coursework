<script setup lang="ts">
import type { ApiFailure } from '../errors/api-error';
defineProps<{ failure: ApiFailure | null; busy?: boolean }>();
defineEmits<{ reload: [] }>();
</script>
<template>
  <section v-if="failure" class="error-panel" role="alert" aria-live="polite">
    <p>{{ failure.message }}</p>
    <p v-if="failure.kind === 'conflict'" class="muted">本地表单保留；确认后重新读取，不会自动覆盖或重复提交。</p>
    <p v-if="failure.outcomeUnknown" class="muted">提交结果尚未确认。请先核对最新状态，再决定下一步。</p>
    <small v-if="failure.correlationId">联系管理员时可提供：{{ failure.correlationId }}</small>
    <button v-if="failure.kind === 'conflict' || failure.outcomeUnknown || failure.code === 'CSRF_UNAVAILABLE' || failure.kind === 'protocol' || failure.kind === 'network'" class="secondary" :disabled="busy" @click="$emit('reload')">确认并重新读取</button>
  </section>
</template>
