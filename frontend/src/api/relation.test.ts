import axios, { type AxiosRequestConfig } from 'axios';
import { describe, expect, it } from 'vitest';
import { createApiClient } from './client';
import { createBusinessApi } from './business';
import { relationCommand } from '../features/relation';
import { syntheticOther, syntheticPage, syntheticRelation, syntheticRequirement, syntheticTrace } from '../features/fixtures';
function setup(reply: unknown) {
  const calls: AxiosRequestConfig[] = []; const transport = axios.create({ adapter: async config => { calls.push(config); return { config, data: config.url === '/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic-csrf' } : reply, status: config.method === 'delete' ? 204 : 200, statusText: '', headers: {} }; } });
  return { business: createBusinessApi(createApiClient(transport)), calls };
}
describe('Wave 4 relation/trace/withdraw transport developer checks', () => {
  it('keeps input endpoint revisions while accepting server normalized symmetric roles', async () => {
    const normalized = { ...syntheticRelation, sourceRequirementId: syntheticOther.requirementId, targetRequirementId: syntheticRequirement.requirementId, relationType: 'RELATES_TO' as const }; const { business, calls } = setup({ ...normalized, lockVersion: '99', privateField: 'discard' }); const input = relationCommand(syntheticRequirement, syntheticOther, 'RELATES_TO', false, '说明');
    expect(await business.createRelation({ ...input, createdBy: '99', relationId: '99' } as never)).toEqual(normalized); expect(JSON.parse(String(calls[1]?.data))).toEqual(input); expect(input.expectedSourceLockVersion).toBe(syntheticRequirement.lockVersion); expect(input.expectedTargetLockVersion).toBe(syntheticOther.lockVersion);
  });
  it('retains incoming directed roles and rejects incorrect normalized/directed responses as unknown outcomes', async () => {
    const incoming = { ...syntheticRelation, sourceRequirementId: syntheticOther.requirementId, targetRequirementId: syntheticRequirement.requirementId }; const good = setup(incoming); const input = relationCommand(syntheticRequirement, syntheticOther, 'DEPENDS_ON', true, ''); await expect(good.business.createRelation(input)).resolves.toEqual(incoming);
    expect(JSON.parse(String(good.calls[1]?.data))).toMatchObject({ sourceRequirementId: '2', expectedSourceLockVersion: '8', targetRequirementId: syntheticRequirement.requirementId, expectedTargetLockVersion: syntheticRequirement.lockVersion }); const wrong = setup(syntheticRelation); await expect(wrong.business.createRelation(input)).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true }); expect(wrong.calls).toHaveLength(2);
  });
  it('rejects self links and out-of-range BIGINT/revisions before any request', async () => {
    const { business, calls } = setup({}); await expect(business.createRelation(relationCommand(syntheticRequirement, syntheticRequirement, 'DEPENDS_ON', false, ''))).rejects.toMatchObject({ code: 'INVALID_INPUT' });
    await expect(business.createRelation({ ...relationCommand(syntheticRequirement, syntheticOther, 'DEPENDS_ON', false, ''), targetRequirementId: '9223372036854775808' })).rejects.toMatchObject({ code: 'INVALID_INPUT' }); expect(calls).toHaveLength(0);
  });
  it('deletes with only source/target expected revisions in query and no invented relation revision/body', async () => {
    const { business, calls } = setup(undefined); await business.deleteRelation(syntheticRelation.relationId, { expectedSourceLockVersion: '8', expectedTargetLockVersion: syntheticRequirement.lockVersion, lockVersion: '99' } as never);
    expect(calls[1]?.url).toBe(`/relations/${syntheticRelation.relationId}?expectedSourceLockVersion=8&expectedTargetLockVersion=${syntheticRequirement.lockVersion}`); expect(calls[1]?.data).toBeUndefined(); expect(calls[1]?.method).toBe('delete');
    expect(() => business.deleteRelation('1', { expectedSourceLockVersion: '9223372036854775808', expectedTargetLockVersion: '0' })).toThrow(); expect(calls).toHaveLength(2);
  });
  it('lists incoming/outgoing relations with page/size and rejects unrelated parent responses', async () => {
    const { business, calls } = setup(syntheticPage([syntheticRelation])); await business.relations(syntheticRequirement.requirementId, { page: 0, size: 20 }); expect(calls[0]?.url).toBe(`/requirements/${syntheticRequirement.requirementId}/relations?page=0&size=20`);
    const wrong = setup(syntheticPage([{ ...syntheticRelation, sourceRequirementId: '3', targetRequirementId: '4' }])); await expect(wrong.business.relations(syntheticRequirement.requirementId, { page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
  });
  it('traces with only declared depth/type and validates typed booleans/root/depth', async () => {
    const { business, calls } = setup({ ...syntheticTrace, privateField: 'discard' }); expect(await business.trace(syntheticRequirement.requirementId, { depth: 2, relationType: 'DEPENDS_ON', direction: 'OUTGOING' } as never)).toEqual(syntheticTrace); expect(calls[0]?.url).toBe(`/requirements/${syntheticRequirement.requirementId}/trace?depth=2&relationType=DEPENDS_ON`);
    for (const value of [{ ...syntheticTrace, truncated: 'false' }, { ...syntheticTrace, rootRequirementId: '2' }, { ...syntheticTrace, depth: 20 }]) { const invalid = setup(value); await expect(invalid.business.trace(syntheticRequirement.requirementId, { depth: 2 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' }); }
  });
  it.each([0, 11, 1.5, NaN])('rejects invalid bounded trace depth %s without requesting', async depth => {
    const { business, calls } = setup({}); await expect(business.trace(syntheticRequirement.requirementId, { depth })).rejects.toMatchObject({ code: 'INVALID_INPUT' }); expect(calls).toHaveLength(0);
  });
  it('withdraws with only parent revision and consumes logical readonly response', async () => {
    const withdrawn = { ...syntheticRequirement, isWithdrawn: 1, withdrawnBy: '1', withdrawnAt: '2026-10-03T07:00:00Z', lockVersion: '9007199254740994' }; const { business, calls } = setup(withdrawn);
    expect(await business.withdrawRequirement(syntheticRequirement.requirementId, { expectedLockVersion: syntheticRequirement.lockVersion, isWithdrawn: 1, withdrawnBy: '99', currentVersionId: '99' } as never)).toEqual(withdrawn); expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedLockVersion: syntheticRequirement.lockVersion });
  });
});
