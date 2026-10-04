import type { AxiosInstance } from 'axios';
import type { CsrfResponse, LoginRequest, SessionResponse } from './contracts.generated';
import { http } from './http';
import { readCsrf, readSession } from './guards';
import { ApiFailure, normalizeFailure } from '../errors/api-error';

export function createApiClient(transport: AxiosInstance = http) {
  let csrf: CsrfResponse | null = null;
  let csrfGeneration = 0;
  let invalidated: (failure: ApiFailure) => void = () => {};
  async function request<T>(method: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE', path: string, data?: unknown, notifySession = true): Promise<T> {
    const unsafe = method !== 'GET';
    if (!path.startsWith('/') || path.startsWith('//') || path.includes('://')) throw new ApiFailure('INVALID_INPUT', 'validation');
    if (unsafe && !csrf) throw new ApiFailure('CSRF_UNAVAILABLE', 'authorization');
    try {
      const response = await transport.request<T>({
        method, url: path, data, withCredentials: true,
        headers: unsafe ? { 'X-CSRF-TOKEN': csrf!.token } : undefined,
      });
      return response.data;
    } catch (error) {
      const failure = normalizeFailure(error, unsafe);
      if (notifySession && (failure.status === 401 || failure.code === 'ACCOUNT_DISABLED')) invalidated(failure);
      throw failure; // No automatic retry, including CSRF/409/timeout/500.
    }
  }
  return {
    request,
    clearCsrf() { csrfGeneration++; csrf = null; },
    hasCsrf() { return csrf !== null; },
    onSessionInvalidated(handler: (failure: ApiFailure) => void) { invalidated = handler; },
    async refreshCsrf(): Promise<void> {
      const started = ++csrfGeneration;
      csrf = null;
      try {
        const token = readCsrf(await request<unknown>('GET', '/auth/csrf'));
        if (started === csrfGeneration) csrf = token;
      }
      catch (error) { throw normalizeFailure(error); }
    },
    async session(): Promise<SessionResponse> {
      try { return readSession(await request<unknown>('GET', '/auth/session', undefined, false)); }
      catch (error) { throw normalizeFailure(error); }
    },
    async login(input: LoginRequest): Promise<SessionResponse> {
      // Explicit whitelist. Never merge caller identity/roles into a credential request.
      const data: LoginRequest = { login: input.login, password: input.password };
      try { return readSession(await request<unknown>('POST', '/auth/login', data, false)); }
      catch (error) { throw normalizeFailure(error, true); }
    },
    async logout(): Promise<void> { await request<void>('POST', '/auth/logout'); },
  };
}
export const api = createApiClient();
