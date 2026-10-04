<script setup lang="ts">
import AuditLogTable from '../components/AuditLogTable.vue';
</script>
<template><div class="workspace-heading"><div><span class="eyebrow">ADMIN SYSTEM AUDIT</span><h1>系统审计</h1><p class="muted">管理员查看全系统安全审计记录；审计与历史保持只读。</p></div></div><section class="surface panel"><AuditLogTable /></section></template>
