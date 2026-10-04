import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useSession } from './useSession';
import { api } from '../api/client';
import { ApiFailure } from '../errors/api-error';
import { registerSessionClearer } from './session-scope';
import type { SessionResponse } from '../api/contracts.generated';

const identity: SessionResponse = { user: { userId: '9007199254740993', displayName: '示例', accountStatus: 'ENABLED' }, roles: ['ADMIN', 'VIEWER'] };
beforeEach(() => { setActivePinia(createPinia()); api.clearCsrf(); vi.spyOn(api, 'refreshCsrf').mockResolvedValue(); });
afterEach(() => vi.restoreAllMocks());
describe('Wave 0 session developer checks', () => {
  it('uses the server role union, with no ADMIN-implies-engineer privilege', async () => {
    vi.spyOn(api, 'session').mockResolvedValue(identity); const store = useSession(); await store.initialize();
    expect(store.hasAnyRole(['ADMIN'])).toBe(true); expect(store.hasAnyRole(['VIEWER', 'REVIEWER'])).toBe(true);
    expect(store.hasAnyRole(['REQUIREMENT_ENGINEER'])).toBe(false); expect(store.identity?.user.userId).toBe('9007199254740993');
  });
  it('treats anonymous401 as anonymous and refuses a disabled session', async () => {
    const read = vi.spyOn(api, 'session').mockRejectedValue(new ApiFailure('UNAUTHENTICATED', 'authentication', 401));
    const store = useSession(); await store.initialize(); expect(store.status).toBe('anonymous'); expect(store.issue).toBeNull();
    read.mockResolvedValue({ ...identity, user: { ...identity.user, accountStatus: 'DISABLED' } }); await store.refreshSession();
    expect(store.authenticated).toBe(false); expect(store.issue?.code).toBe('ACCOUNT_DISABLED');
  });
  it('sends the original password and refetches CSRF after successful login', async () => {
    const login = vi.spyOn(api, 'login').mockResolvedValue(identity); const store = useSession();
    await store.login({ login: 'example', password: ' e\u0301 ' });
    expect(login).toHaveBeenCalledWith({ login: 'example', password: ' e\u0301 ' }); expect(api.refreshCsrf).toHaveBeenCalledTimes(2);
    expect(JSON.stringify(store.$state)).not.toContain(' e\u0301 '); expect(store.authenticated).toBe(true);
  });
  it('blocks invalid byte/blank input before any login request', async () => {
    const login = vi.spyOn(api, 'login'); const store = useSession();
    for (const password of [' '.repeat(72), '汉'.repeat(25), '😀'.repeat(19)]) await expect(store.login({ login: 'example', password })).rejects.toMatchObject({ code: 'INVALID_INPUT' });
    expect(login).not.toHaveBeenCalled();
  });
  it('resets session-scoped state and refetches CSRF on confirmed logout', async () => {
    vi.spyOn(api, 'session').mockResolvedValue(identity); const logout = vi.spyOn(api, 'logout').mockResolvedValue();
    const cleared = vi.fn(); const unregister = registerSessionClearer(cleared); const store = useSession(); await store.initialize(); await store.logout();
    expect(logout).toHaveBeenCalledTimes(1); expect(store.authenticated).toBe(false); expect(cleared).toHaveBeenCalled(); expect(api.refreshCsrf).toHaveBeenCalledTimes(3); unregister();
  });
  it('does not repeat unknown-result logout and requires a session read', async () => {
    const read = vi.spyOn(api, 'session').mockResolvedValue(identity);
    const logout = vi.spyOn(api, 'logout').mockRejectedValue(new ApiFailure('NETWORK_ERROR', 'network', undefined, undefined, undefined, true));
    const store = useSession(); await store.initialize();
    let finish!: (value: SessionResponse) => void;
    read.mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const staleRead = store.refreshSession();
    const clearCsrf = vi.spyOn(api, 'clearCsrf');
    await expect(store.logout()).rejects.toMatchObject({ outcomeUnknown: true });
    finish(identity); await staleRead;
    expect(logout).toHaveBeenCalledTimes(1); expect(store.status).toBe('unavailable'); expect(store.issue?.outcomeUnknown).toBe(true); expect(store.authenticated).toBe(false);
    expect(clearCsrf).toHaveBeenCalled(); expect(store.identity).toBeNull();
  });
  it.each([
    new ApiFailure('INVALID_CREDENTIALS', 'authentication', 401),
    new ApiFailure('ACCOUNT_DISABLED', 'authorization', 403),
  ])('clears CSRF and session-scoped forms when login returns $code', async failure => {
    vi.spyOn(api, 'session').mockResolvedValue(identity);
    vi.spyOn(api, 'login').mockRejectedValue(failure);
    vi.spyOn(api, 'hasCsrf').mockReturnValue(true);
    const store = useSession(); await store.initialize();
    const cleared = vi.fn(); const unregister = registerSessionClearer(cleared);
    const clearCsrf = vi.spyOn(api, 'clearCsrf');
    await expect(store.login({ login: 'example', password: 'synthetic-secret' })).rejects.toMatchObject({ code: failure.code });
    expect(store.identity).toBeNull(); expect(store.status).toBe('anonymous');
    expect(cleared).toHaveBeenCalledOnce(); expect(clearCsrf).toHaveBeenCalledOnce();
    unregister();
  });
  it('does not let an obsolete initialization failure replace an invalidated session', async () => {
    let reject!: (failure: ApiFailure) => void;
    vi.mocked(api.refreshCsrf).mockImplementationOnce(() => new Promise((_, rejecter) => { reject = rejecter; }));
    const store = useSession(); const pending = store.initialize();
    store.reset(new ApiFailure('ACCOUNT_DISABLED', 'authorization', 403));
    reject(new ApiFailure('NETWORK_ERROR', 'network')); await pending;
    expect(store.status).toBe('anonymous'); expect(store.issue?.code).toBe('ACCOUNT_DISABLED');
    expect(store.identity).toBeNull();
  });
  it('discards a stale session read after reset', async () => {
    let finish!: (value: SessionResponse) => void;
    vi.spyOn(api, 'session').mockImplementation(() => new Promise(resolve => { finish = resolve; }));
    const store = useSession(); const reading = store.refreshSession(); store.reset(); finish(identity); await reading;
    expect(store.identity).toBeNull(); expect(store.status).toBe('anonymous');
  });
  it('does not restore removed roles from an older concurrent session read', async () => {
    let finish!: (value: SessionResponse) => void;
    vi.spyOn(api, 'session')
      .mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }))
      .mockResolvedValueOnce({ ...identity, roles: ['VIEWER'] });
    const store = useSession(); const older = store.refreshSession();
    await store.refreshSession(); finish(identity); await older;
    expect(store.hasAnyRole(['VIEWER'])).toBe(true); expect(store.hasAnyRole(['ADMIN'])).toBe(false);
  });
});
