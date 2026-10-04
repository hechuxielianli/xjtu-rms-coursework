import { computed, ref, shallowRef } from 'vue';
import { defineStore } from 'pinia';
import type { LoginRequest, RoleCode, SessionResponse } from '../api/contracts.generated';
import { api } from '../api/client';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import { clearSessionScope } from './session-scope';
import { passwordProblem } from './password';

export const useSession = defineStore('session', () => {
  const identity = shallowRef<SessionResponse | null>(null);
  const status = ref<'unknown' | 'anonymous' | 'authenticated' | 'unavailable'>('unknown');
  const issue = shallowRef<ApiFailure | null>(null);
  const busy = ref(false);
  let generation = 0;
  let sessionReadSequence = 0;
  let initializing: Promise<void> | null = null;
  const authenticated = computed(() => status.value === 'authenticated' && identity.value?.user.accountStatus === 'ENABLED');
  const hasAnyRole = (roles: readonly RoleCode[]) => authenticated.value && roles.some(role => identity.value!.roles.includes(role));

  function reset(failure: ApiFailure | null = null) {
    generation++;
    identity.value = null;
    status.value = 'anonymous';
    issue.value = failure;
    api.clearCsrf();
    clearSessionScope();
  }
  function suspend(failure: ApiFailure) {
    generation++;
    identity.value = null;
    status.value = 'unavailable';
    issue.value = failure;
    api.clearCsrf();
    clearSessionScope();
  }
  api.onSessionInvalidated(reset);
  function accept(session: SessionResponse) {
    if (session.user.accountStatus !== 'ENABLED') throw new ApiFailure('ACCOUNT_DISABLED', 'authorization', 403);
    identity.value = session;
    status.value = 'authenticated';
    issue.value = null;
  }
  async function refreshSession() {
    const started = generation;
    const sequence = ++sessionReadSequence;
    try {
      const current = await api.session();
      if (started === generation && sequence === sessionReadSequence) { accept(current); return true; }
      return false;
    } catch (error) {
      if (started !== generation || sequence !== sessionReadSequence) return false;
      const failure = normalizeFailure(error);
      if (failure.status === 401 || failure.code === 'ACCOUNT_DISABLED') reset(failure.code === 'UNAUTHENTICATED' ? null : failure);
      else suspend(failure);
      return false;
    }
  }
  async function initialize() {
    if (initializing) return initializing;
    const started = generation;
    initializing = (async () => {
      try {
        await api.refreshCsrf();
        if (started === generation) await refreshSession();
      }
      catch (error) {
        if (started !== generation) return;
        const failure = normalizeFailure(error);
        if (failure.status === 401 || failure.code === 'ACCOUNT_DISABLED') reset(failure);
        else { status.value = 'unavailable'; issue.value = failure; }
      }
    })();
    try { await initializing; } finally { initializing = null; }
  }
  async function ensureInitialized() { if (status.value === 'unknown') await initialize(); }

  async function login(input: LoginRequest) {
    if (busy.value) return;
    if (passwordProblem(input.password) || !/\S/u.test(input.login) || Array.from(input.login).length > 254) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
    busy.value = true;
    const started = generation;
    issue.value = null;
    try {
      if (!api.hasCsrf()) await api.refreshCsrf();
      if (started !== generation) return;
      const current = await api.login({ login: input.login, password: input.password });
      if (started !== generation) return;
      accept(current);
      api.clearCsrf();
      try { await api.refreshCsrf(); }
      catch (error) { if (started === generation) issue.value = normalizeFailure(error); } // Login succeeded: do not resend credentials.
    } catch (error) {
      if (started !== generation) return;
      const failure = normalizeFailure(error, true);
      if (failure.status === 401 || failure.code === 'ACCOUNT_DISABLED') reset(failure);
      else if (failure.outcomeUnknown) suspend(failure);
      else { identity.value = null; status.value = 'anonymous'; issue.value = failure; }
      throw failure;
    } finally { busy.value = false; }
  }
  async function logout() {
    if (busy.value) return;
    busy.value = true;
    issue.value = null;
    const started = generation;
    try {
      if (!api.hasCsrf()) await api.refreshCsrf();
      if (started !== generation) return;
      await api.logout();
      if (started !== generation) return;
      reset();
      const resetGeneration = generation;
      try { await api.refreshCsrf(); }
      catch (error) { if (resetGeneration === generation) issue.value = normalizeFailure(error); }
    } catch (error) {
      const failure = normalizeFailure(error, true);
      if (failure.status === 401 || failure.code === 'ACCOUNT_DISABLED') reset(failure);
      else if (started === generation) {
        if (failure.outcomeUnknown) suspend(failure);
        else issue.value = failure;
      }
      throw failure;
    } finally { busy.value = false; }
  }
  return { identity, status, issue, busy, authenticated, hasAnyRole, reset, initialize, ensureInitialized, refreshSession, login, logout };
});
