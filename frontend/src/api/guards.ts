import type { CsrfResponse, RoleCode, SessionResponse } from './contracts.generated';
import { isDecimalString } from './decimal';

export const ROLE_CODES: readonly RoleCode[] = ['ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER', 'VIEWER'];
const object = (value: unknown): value is Record<string, unknown> => value !== null && typeof value === 'object' && !Array.isArray(value);
export const isRole = (value: unknown): value is RoleCode => typeof value === 'string' && ROLE_CODES.includes(value as RoleCode);

export function readSession(value: unknown): SessionResponse {
  if (!object(value) || !object(value.user) || !isDecimalString(value.user.userId) ||
      typeof value.user.displayName !== 'string' || Array.from(value.user.displayName).length > 100 ||
      !['ENABLED', 'DISABLED'].includes(String(value.user.accountStatus)) ||
      !Array.isArray(value.roles) || value.roles.length === 0 || !value.roles.every(isRole) ||
      new Set(value.roles).size !== value.roles.length) throw new Error('INVALID_SESSION_RESPONSE');
  // Construct the permitted projection; never put raw responses or extra credentials into Pinia.
  return {
    user: { userId: value.user.userId, displayName: value.user.displayName, accountStatus: value.user.accountStatus as 'ENABLED' | 'DISABLED' },
    roles: [...value.roles],
  };
}

export function readCsrf(value: unknown): CsrfResponse {
  if (!object(value) || value.headerName !== 'X-CSRF-TOKEN' || typeof value.token !== 'string' || value.token.length === 0) {
    throw new Error('INVALID_CSRF_RESPONSE');
  }
  return { headerName: 'X-CSRF-TOKEN', token: value.token };
}
