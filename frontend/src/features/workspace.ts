import { computed, onBeforeUnmount, ref, shallowRef, type Ref } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router';
import { registerSessionClearer } from '../auth/session-scope';
import { useSession } from '../auth/useSession';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import type { RoleCode } from '../api/contracts.generated';

export const LEVELS = ['BUSINESS', 'USER', 'SYSTEM'] as const;
export const KINDS = ['FUNCTIONAL', 'QUALITY', 'CONSTRAINT'] as const;
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
export const STATUSES = ['DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'IMPLEMENTED', 'VERIFIED'] as const;
export async function requireFreshRoles(session: ReturnType<typeof useSession>, roles: readonly RoleCode[], current: () => boolean): Promise<void> { const refreshed = await session.refreshSession(); if (!refreshed || !current() || !session.hasAnyRole(roles)) throw new ApiFailure('FORBIDDEN', 'authorization', 403); }
export function useScope(clear: () => void) {
  let epoch = 0; let mounted = true;
  const requests = new Map<string, number>();
  const unregister = registerSessionClearer(() => { epoch++; requests.clear(); clear(); });
  onBeforeUnmount(() => { mounted = false; epoch++; unregister(); clear(); });
  return {
    invalidate() { epoch++; requests.clear(); clear(); },
    start(key: string) { const sequence = (requests.get(key) ?? 0) + 1; requests.set(key, sequence); return { key, sequence, epoch }; },
    current(token: { key: string; sequence: number; epoch: number }) { return mounted && token.epoch === epoch && requests.get(token.key) === token.sequence; },
  };
}
export function useOperation() {
  const busy = ref(false); const issue = shallowRef<ApiFailure | null>(null);
  const blocked = computed(() => issue.value?.kind === 'conflict' || issue.value?.outcomeUnknown === true);
  const scope = useScope(() => { issue.value = null; busy.value = false; });
  return {
    busy, issue, blocked,
    clear() { issue.value = null; },
    resetContext() { scope.invalidate(); },
    fail(error: unknown) { issue.value = normalizeFailure(error); },
    async run<T>(action: (current: () => boolean) => Promise<T>, apply: (value: T) => void | Promise<void>, contextCurrent: () => boolean = () => true): Promise<boolean> {
      if (busy.value || blocked.value) return false;
      busy.value = true; issue.value = null; const token = scope.start('command');
      try { const value = await action(() => scope.current(token) && contextCurrent()); if (!scope.current(token) || !contextCurrent()) return false; await apply(value); return true; }
      catch (error) { if (scope.current(token) && contextCurrent()) issue.value = normalizeFailure(error, true); return false; }
      finally { if (scope.current(token)) busy.value = false; }
    },
  };
}
export function useUnsaved(dirty: Ref<boolean>) {
  const session = useSession();
  onBeforeRouteLeave(() => !dirty.value || !session.authenticated || window.confirm('有尚未保存的修改，确认离开并放弃本地草稿？'));
  onBeforeRouteUpdate((to, from) => to.path === from.path && to.query.tab === from.query.tab && to.query.requirementId === from.query.requirementId && to.query.changeRequestId === from.query.changeRequestId || !dirty.value || !session.authenticated || window.confirm('有尚未保存的修改，确认离开并放弃本地草稿？'));
  const unload = (event: BeforeUnloadEvent) => { if (dirty.value) { event.preventDefault(); event.returnValue = ''; } };
  window.addEventListener('beforeunload', unload);
  onBeforeUnmount(() => window.removeEventListener('beforeunload', unload));
}
