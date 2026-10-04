<script setup lang="ts">
import type { ChangeRequestReviewResponse } from '../../api/contracts.generated';
import { changeSnapshot } from '../../features/change';
import FormalContentView from '../FormalContentView.vue';
defineProps<{ review: ChangeRequestReviewResponse }>();
</script>
<template>
  <dl class="formal-content"><dt>快照基准版本 ID</dt><dd>{{ review.snapshotBaseVersionId }}</dd><dt>快照申请标题</dt><dd class="text-content">{{ review.snapshotRequestTitle }}</dd><dt>快照申请理由</dt><dd class="text-content">{{ review.snapshotReason }}</dd></dl>
  <h3>本轮冻结的八项建议内容</h3><FormalContentView :content="changeSnapshot(review)" />
</template>
