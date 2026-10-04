<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ value: string | null }>();
const parts = computed(() => props.value?.endsWith('Z') && props.value.includes('T') ? props.value.slice(0, -1).split('T') : null);
</script>
<template><time v-if="parts" class="utc-time" :datetime="value ?? undefined" :title="value ?? undefined"><span>{{ parts[0] }}</span><span>{{ parts[1] }} UTC</span></time><span v-else>{{ value ?? '—' }}</span></template>
