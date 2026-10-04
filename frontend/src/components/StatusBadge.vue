<script setup lang="ts">
import { displayLabel } from '../presentation/labels';
defineProps<{ value: string }>();
</script>
<template><span class="badge status-badge" :class="`state-${value.toLowerCase()}`">{{ displayLabel(value) }}</span></template>
