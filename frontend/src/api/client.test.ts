import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './client';
import { isDecimalString } from './decimal';
import { readSession } from './guards';
import { ApiFailure } from '../errors/api-error';
import { passwordProblem } from '../auth/password';
import { safeReturnPath } from '../router/redirect';

const session = { user: { userId: '9007199254740993', displayName: 'Example', accountStatus: 'ENABLED' }, roles: ['VIEWER'] };
const response = (config: InternalAxiosRequestConfig, data: unknown, status = 200) => ({ config, data, status, statusText: '', headers: {} });

describe('Wave 0 transport and safe boundary developer checks', () => {
  it('keeps BIGINT strings precise and rejects values outside the signed range', () => {
    expect(isDecimalString('9007199254740993')).toBe(true);
    expect(isDecimalString('9223372036854775807')).toBe(true);
    for (const invalid of [9007199254740993, '9223372036854775808', '01', '-1', '0', '1e3']) expect(isDecimalString(invalid)).toBe(false);
    expect(isDecimalString('0', true)).toBe(true);
  });
  it('retains a safe session projection and rejects malformed roles/ids', () => {
    expect(readSession({ ...session, passwordHash: 'untrusted-extra' })).toEqual(session);
    expect(() => readSession({ ...session, roles: ['ADMIN', 'ADMIN'] })).toThrow();
    expect(() => readSession({ ...session, user: { ...session.user, userId: 3 } })).toThrow();
    expect(() => readSession({ ...session, roles: ['SUPERUSER'] })).toThrow();
  });
  it('uses actual UTF8 bytes, keeps spaces and does not normalize Unicode', () => {
    for (const value of ['a', 'a'.repeat(72), '汉'.repeat(24), '😀'.repeat(18), ' a ', '\u00e9'.repeat(36), 'e\u0301'.repeat(24)]) expect(passwordProblem(value)).toBeNull();
    for (const value of ['', ' '.repeat(72), 'a'.repeat(73), '汉'.repeat(25), '😀'.repeat(19), '\u00e9'.repeat(37), 'e\u0301'.repeat(25)]) expect(passwordProblem(value)).not.toBeNull();
  });
  it('rejects external/protocol-relative return routes', () => {
    for (const value of ['https://outside.test', '//outside.test', '/\\outside.test', '/login', undefined]) expect(safeReturnPath(value)).toBe('/requirements');
    expect(safeReturnPath('/admin/users?tab=roles')).toBe('/admin/users?tab=roles');
  });
  it('sends only login/password with cookie credentials and the encoded CSRF header', async () => {
    const calls: AxiosRequestConfig[] = [];
    const transport = axios.create({ adapter: async config => {
      calls.push(config);
      return response(config, config.url === '/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'encoded-synthetic-token' } : session);
    } });
    const client = createApiClient(transport); await client.refreshCsrf();
    const raw = ' e\u0301 ';
    await client.login({ login: 'example', password: raw, roles: ['ADMIN'] } as never);
    expect(JSON.parse(String(calls[1].data))).toEqual({ login: 'example', password: raw });
    expect(calls[1].withCredentials).toBe(true);
    expect(calls[1].headers?.['X-CSRF-TOKEN']).toBe('encoded-synthetic-token');
    expect(calls[0].headers?.['X-CSRF-TOKEN']).toBeUndefined();
    client.clearCsrf(); await expect(client.logout()).rejects.toMatchObject({ code: 'CSRF_UNAVAILABLE' });
    expect(calls).toHaveLength(2);
  });
  it('does not retry a timeout or retain Axios credentials in a normalized failure', async () => {
    const transport = axios.create({ adapter: async config => {
      if (config.url === '/auth/csrf') return response(config, { headerName: 'X-CSRF-TOKEN', token: 'synthetic-token' });
      throw new AxiosError('server echoed synthetic-secret', 'ECONNABORTED', config);
    } });
    const client = createApiClient(transport); const spy = vi.spyOn(transport, 'request'); await client.refreshCsrf();
    let failure: unknown;
    try { await client.login({ login: 'example', password: 'synthetic-secret' }); } catch (error) { failure = error; }
    expect(failure).toBeInstanceOf(ApiFailure); expect(failure).toMatchObject({ outcomeUnknown: true, kind: 'network' });
    expect(JSON.stringify(failure)).not.toContain('synthetic-secret'); expect(String(failure)).not.toContain('synthetic-secret');
    expect(spy).toHaveBeenCalledTimes(2);
  });
  it.each([409, 500])('requires explicit reconciliation and never replays an unsafe %i response', async status => {
    const transport = axios.create({ adapter: async config => {
      if (config.url === '/auth/csrf') return response(config, { headerName: 'X-CSRF-TOKEN', token: 'synthetic-token' });
      throw new AxiosError('unsafe', 'ERR_BAD_RESPONSE', config, undefined,
        response(config, { code: status === 409 ? 'LOCK_VERSION_CONFLICT' : 'INTERNAL_ERROR', message: 'untrusted-secret', correlationId: 'test-id', currentLockVersion: '9007199254740993' }, status));
    } });
    const client = createApiClient(transport); const spy = vi.spyOn(transport, 'request'); await client.refreshCsrf();
    await expect(client.request('POST', '/test-command', { title: 'local draft' })).rejects.toMatchObject({ status, outcomeUnknown: status === 500, currentLockVersion: '9007199254740993' });
    expect(spy).toHaveBeenCalledTimes(2);
  });
  it('notifies expired/disabled session without treating ordinary403 as expiry', async () => {
    let status = 403; let code = 'FORBIDDEN';
    const transport = axios.create({ adapter: async config => { throw new AxiosError('denied', '', config, undefined, response(config, { code, message: '', correlationId: 'test-id' }, status)); } });
    const client = createApiClient(transport); const invalidated = vi.fn(); client.onSessionInvalidated(invalidated);
    await expect(client.request('GET', '/protected')).rejects.toMatchObject({ code: 'FORBIDDEN' }); expect(invalidated).not.toHaveBeenCalled();
    code = 'ACCOUNT_DISABLED'; await expect(client.request('GET', '/protected')).rejects.toMatchObject({ code });
    status = 401; code = 'UNAUTHENTICATED'; await expect(client.request('GET', '/protected')).rejects.toMatchObject({ code });
    expect(invalidated).toHaveBeenCalledTimes(2);
  });
  it('does not restore a stale in-flight CSRF response after invalidation', async () => {
    let finish!: (value: unknown) => void; const pending = new Promise(resolve => { finish = resolve; });
    const transport = axios.create({ adapter: async config => response(config, await pending) });
    const client = createApiClient(transport); const getting = client.refreshCsrf(); client.clearCsrf();
    finish({ headerName: 'X-CSRF-TOKEN', token: 'obsolete-token' }); await getting;
    expect(client.hasCsrf()).toBe(false);
  });
  it('invalidates a disabled session encountered while fetching CSRF for a protected command', async () => {
    const transport = axios.create({ adapter: async config => { throw new AxiosError('denied', '', config, undefined, response(config, { code: 'ACCOUNT_DISABLED', message: '', correlationId: 'test-id' }, 403)); } });
    const client = createApiClient(transport); const invalidated = vi.fn(); client.onSessionInvalidated(invalidated);
    await expect(client.refreshCsrf()).rejects.toMatchObject({ code: 'ACCOUNT_DISABLED' });
    expect(invalidated).toHaveBeenCalledOnce(); expect(client.hasCsrf()).toBe(false);
  });
});
