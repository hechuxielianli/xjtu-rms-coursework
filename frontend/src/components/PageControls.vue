<script setup lang="ts">
defineProps<{ page: number; size: number; total: number; busy?: boolean }>();
defineEmits<{ page: [number]; size: [number] }>();
</script>
<template>
  <div class="page-controls" aria-label="分页">
    <span>共 {{ total }} 条 · 第 {{ page + 1 }} 页</span>
    <label>每页 <select :value="size" :disabled="busy" @change="$emit('size', Number(($event.target as HTMLSelectElement).value))"><option v-for="n in [10, 20, 50, 100]" :key="n" :value="n">{{ n }}</option></select></label>
    <button class="secondary compact" :disabled="busy || page === 0" @click="$emit('page', page - 1)">上一页</button>
    <button class="secondary compact" :disabled="busy || (page + 1) * size >= total" @click="$emit('page', page + 1)">下一页</button>
  </div>
</template>
