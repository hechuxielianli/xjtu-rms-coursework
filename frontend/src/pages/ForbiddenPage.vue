<script setup lang="ts">
import { RouterLink } from 'vue-router';
</script>
<template>
  <section class="surface pending-page"><span class="eyebrow">访问受限</span><h1>当前角色没有此页面权限</h1><p>如需承担对应工作，请联系管理员调整角色。</p><RouterLink to="/requirements" class="text-link">返回需求工作区</RouterLink></section>
</template>
