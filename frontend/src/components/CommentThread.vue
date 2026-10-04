<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { business } from '../api/business';
import type { CommentResponse, CommentResponsePage, RequirementResponse } from '../api/contracts.generated';
import { useSession } from '../auth/useSession';
import { useOperation, useScope, useUnsaved, requireFreshRoles } from '../features/workspace';
import { normalizeFailure, ApiFailure } from '../errors/api-error';
import ErrorPanel from './ErrorPanel.vue';
import PageControls from './PageControls.vue';
import UtcTime from './UtcTime.vue';
const props = defineProps<{ record: RequirementResponse }>();
const emit = defineEmits<{ updated: [RequirementResponse] }>();
const session = useSession(); const page = ref(0); const size = ref(20); const result = shallowRef<CommentResponsePage | null>(null); const content = ref('');
const dirty = computed(() => content.value !== ''); const loading = ref(false); const issue = shallowRef<ApiFailure | null>(null); const operation = useOperation();
const scope = useScope(() => { result.value = null; content.value = ''; issue.value = null; }); useUnsaved(dirty);
const commentRole = computed(() => session.hasAnyRole(['REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER']));
const canCreate = computed(() => commentRole.value && props.record.isWithdrawn === 0);
const canDelete = (comment: CommentResponse) => canCreate.value && comment.isDeleted === 0 && comment.authorId === session.identity?.user.userId;
async function load(reconcile = false) {
  const token = scope.start('comments'); loading.value = true; issue.value = null;
  const id = props.record.requirementId;
  try { if (reconcile) { await session.refreshSession(); if (!scope.current(token)) return; const parent = await business.requirement(id); if (!scope.current(token)) return; emit('updated', parent); } const value = await business.comments(id, { page: page.value, size: size.value }); if (scope.current(token)) { result.value = value; if (reconcile) operation.clear(); } }
  catch (error) { if (scope.current(token)) { result.value = null; issue.value = normalizeFailure(error); } }
  finally { if (scope.current(token)) loading.value = false; }
}
async function send() { if (!canCreate.value || loading.value) return; const record = props.record; const input = { content: content.value }; await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER'], current); if (record.isWithdrawn) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.createComment(record.requirementId, input); }, async () => { content.value = ''; page.value = 0; await load(); }, () => props.record.requirementId === record.requirementId && props.record.lockVersion === record.lockVersion); }
async function remove(comment: CommentResponse) { if (!canDelete(comment) || !window.confirm('确认逻辑删除本人的这条评论？')) return; const record = props.record; await operation.run(async current => { await requireFreshRoles(session, ['REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER'], current); if (record.isWithdrawn || comment.isDeleted || comment.authorId !== session.identity?.user.userId) throw new ApiFailure('FORBIDDEN', 'authorization', 403); return business.deleteComment(comment.commentId); }, async () => { await load(); }, () => props.record.requirementId === record.requirementId && props.record.lockVersion === record.lockVersion); }
async function paginate(value: number) { page.value = value; await load(); }
async function resize(value: number) { size.value = value; page.value = 0; await load(); }
onMounted(() => { void load(); });
</script>
<template>
  <section><h2>评论</h2><ErrorPanel :failure="issue" :busy="loading" @reload="load(true)" /><ErrorPanel :failure="operation.issue.value" :busy="loading || operation.busy.value" @reload="load(true)" />
    <p v-if="loading" class="muted">正在读取评论…</p><article v-for="comment in result?.items" :key="comment.commentId" class="comment"><div class="workspace-heading"><span class="muted small">作者 {{ comment.authorId }} · <UtcTime :value="comment.createdAt" /></span><button v-if="canDelete(comment)" class="secondary compact danger" :disabled="loading || operation.busy.value || operation.blocked.value" @click="remove(comment)">删除本人评论</button></div><p v-if="comment.isDeleted" class="muted">此评论已删除。</p><p v-else class="text-content">{{ comment.content }}</p></article>
    <p v-if="result && !result.items.length && !loading" class="empty">尚无评论。</p><PageControls :page="page" :size="size" :total="result?.totalElements ?? 0" :busy="loading" @page="paginate" @size="resize" />
    <form v-if="canCreate" @submit.prevent="send" novalidate><label>发表评论<textarea v-model="content" rows="4" :disabled="operation.busy.value" placeholder="输入纯文本评论" /></label><button class="primary" :disabled="loading || operation.busy.value || operation.blocked.value">{{ operation.busy.value ? '正在发布…' : '发布评论' }}</button></form><p v-else class="muted small">当前账号或需求状态仅允许查看评论。</p>
  </section>
</template>
