import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it } from 'vitest';
import { createApiClient } from './client';
import { auditQuery, createBusinessApi } from './business';
import { readContract } from './schema';
import { contractSchemas } from './schemas.generated';
import { syntheticAudit, syntheticPage, syntheticRequirement } from '../features/fixtures';
function setup(data: unknown) { const calls: AxiosRequestConfig[] = []; const transport = axios.create({ adapter: async (config: InternalAxiosRequestConfig) => { calls.push(config); return { config, data, status: 200, statusText: '', headers: {} }; } }); return { calls, business: createBusinessApi(createApiClient(transport)) }; }
describe('Wave 5 audit boundary developer checks', () => {
  it('allows only frozen pagination/UTC/action queries without display-field filters', async () => {
    const input = { page: 2, size: 100, from: '2026-10-03T00:00:00.000001Z', to: '2026-10-03T23:59:59.123456Z', action: 'A&B 动作', actorId: '2', targetType: 'USER', outcome: 'FAILED', direction: 'outgoing' };
    expect(Object.fromEntries(auditQuery(input))).toEqual({ page: '2', size: '100', from: input.from, to: input.to, action: input.action });
    const { calls, business } = setup(syntheticPage([syntheticAudit])); await business.history(syntheticRequirement.requirementId, input); await business.auditEvents(input);
    expect(calls.map(call => call.method)).toEqual(['get', 'get']); expect(calls.every(call => call.withCredentials === true && call.data === undefined)).toBe(true);
    expect(calls[0]?.url).toBe(`/requirements/${syntheticRequirement.requirementId}/history?${auditQuery(input)}`); expect(calls[1]?.url).toBe(`/audit-events?${auditQuery(input)}`);
  });
  it.each([
    { from: '2026-02-30T01:00:00Z' }, { from: '2026-10-03T00:00:00+08:00' }, { from: '2026-10-03' }, { to: '2026-10-03T24:00:00Z' },
    { from: '2026-10-03T00:00:00.000002Z', to: '2026-10-03T00:00:00.000001Z' }, { action: '长'.repeat(65) },
  ])('rejects an invalid UTC/range/action input before an HTTP request: %j', input => { expect(() => auditQuery({ page: 0, size: 20, ...input })).toThrow(); });
  it('retains safe nested JSON, precise string IDs and microseconds while projecting extra response properties away', () => {
    const value = readContract('AuditResponse', { ...syntheticAudit, passwordHash: 'extra-must-not-survive' }); expect(value).toEqual(syntheticAudit); expect(Object.keys(contractSchemas)).toHaveLength(59);
    expect(() => readContract('AuditResponse', { ...syntheticAudit, actorId: 9007199254740993 })).toThrow(); expect(() => readContract('AuditResponse', { ...syntheticAudit, outcome: 'PENDING' })).toThrow();
  });
  it.each(['password', 'passwordHash', 'accessToken', 'csrf', 'Cookie', 'sessionId', 'credential', 'authorization', 'bcryptHash', '__proto__'])('rejects nested sensitive %s payloads before they can be displayed', key => {
    const afterData = { nested: JSON.parse(`{"${key}":"must-not-leak"}`) }; expect(() => readContract('AuditResponse', { ...syntheticAudit, afterData })).toThrow();
  });
  it('rejects malformed unsafe JSON numbers and another requirement history before projection is used', async () => {
    expect(() => readContract('AuditResponse', { ...syntheticAudit, afterData: { userId: 9007199254740993 } })).toThrow();
    const { business } = setup(syntheticPage([{ ...syntheticAudit, requirementId: '2' }])); await expect(business.history(syntheticRequirement.requirementId, { page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
  });
});
