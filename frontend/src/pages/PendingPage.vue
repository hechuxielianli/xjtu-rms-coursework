<script setup lang="ts">
import { useRoute, RouterLink } from 'vue-router';
const route = useRoute();
</script>
<template>
  <section class="surface pending-page">
    <span class="eyebrow">{{ route.meta.title === '页面不存在' ? '404' : '工作区' }}</span><h1>{{ route.meta.title }}</h1>
    <p v-if="route.meta.title === '页面不存在'">找不到此页面，请检查链接或返回工作区。</p>
    <template v-else><p>页面地址无效，请从工作区导航重新进入。</p></template>
    <RouterLink v-if="route.path !== '/requirements'" to="/requirements" class="text-link">返回需求工作区</RouterLink>
  </section>
</template>
