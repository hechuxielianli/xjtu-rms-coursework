import axios, { type AxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './client';
import { createBusinessApi } from './business';
import { syntheticPage, syntheticRequirement, syntheticReview, syntheticVersion } from '../features/fixtures';
function setup(reply: unknown) {
  const calls: AxiosRequestConfig[] = []; const transport = axios.create({ adapter: async config => { calls.push(config); return { config, data: config.url === '/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic-csrf' } : reply, status: 200, statusText: '', headers: {} }; } });
  return { business: createBusinessApi(createApiClient(transport)), calls, transport };
}
describe('Wave 2 lifecycle/review/version boundary developer checks', () => {
  it('submits only the read requirement revision and consumes the full immutable review', async () => {
    const { business, calls } = setup(syntheticReview);
    expect(await business.submitRequirement(syntheticRequirement.requirementId, { expectedLockVersion: syntheticRequirement.lockVersion, status: 'UNDER_REVIEW', snapshotTitle: 'client override', submittedBy: '99' } as never)).toEqual(syntheticReview);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedLockVersion: syntheticRequirement.lockVersion });
  });
  it('binds member confirmation to version/revision/note without actor/time/state fields', async () => {
    const { business, calls } = setup({ ...syntheticRequirement, status: 'IMPLEMENTED', currentVersionId: syntheticVersion.versionId });
    await business.confirmVersion(syntheticRequirement.requirementId, 'implement', { expectedVersionId: syntheticVersion.versionId, expectedLockVersion: syntheticRequirement.lockVersion, description: '实现说明', currentVersionId: '2', actorId: '2', status: 'IMPLEMENTED', implementedAt: 'wrong' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedVersionId: syntheticVersion.versionId, expectedLockVersion: syntheticRequirement.lockVersion, description: '实现说明' });
    expect(() => business.confirmVersion(syntheticRequirement.requirementId, 'verify', { expectedVersionId: syntheticVersion.versionId, expectedLockVersion: '0', description: ' ' })).toThrow(); expect(calls).toHaveLength(2);
  });
  it.each(['REJECT', 'REQUEST_CHANGES'] as const)('rejects a blank %s reason before issuing a decision command', async decision => {
    const { business, calls } = setup({});
    await expect(business.decideReview(syntheticReview.reviewId, syntheticRequirement.requirementId, { decision, comment: '  ', expectedRequirementLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_INPUT' }); expect(calls).toHaveLength(0);
  });
  it('permits approval without a reason and projects only decision fields, keeping V1 provenance', async () => {
    const result = { review: { ...syntheticReview, reviewStatus: 'COMPLETED', decision: 'APPROVE', reviewerId: '2', decidedAt: '2026-10-03T01:00:00Z' }, requirement: { ...syntheticRequirement, status: 'APPROVED', currentVersionId: syntheticVersion.versionId }, version: syntheticVersion };
    const { business, calls } = setup(result);
    const value = await business.decideReview(syntheticReview.reviewId, syntheticRequirement.requirementId, { decision: 'APPROVE', expectedRequirementLockVersion: syntheticRequirement.lockVersion, reviewerId: '99', versionNo: 9 } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ decision: 'APPROVE', expectedRequirementLockVersion: syntheticRequirement.lockVersion });
    expect(value.version?.versionNo).toBe(1); expect(value.version?.initialReviewId).toBe(syntheticReview.reviewId);
  });
  it('rejects round/version responses from another parent and a mismatched decision result without retry', async () => {
    const { business, transport } = setup(syntheticPage([{ ...syntheticReview, requirementId: '2' }]));
    await expect(business.reviews(syntheticRequirement.requirementId, { page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
    const version = setup({ ...syntheticVersion, requirementId: '2' }); await expect(version.business.version(syntheticRequirement.requirementId, syntheticVersion.versionId)).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
    const wrong = setup({ review: { ...syntheticReview, reviewStatus: 'COMPLETED', decision: 'APPROVE' }, requirement: { ...syntheticRequirement, requirementId: '2' }, version: null }); const spy = vi.spyOn(wrong.transport, 'request');
    await expect(wrong.business.decideReview(syntheticReview.reviewId, syntheticRequirement.requirementId, { decision: 'APPROVE', expectedRequirementLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true }); expect(spy).toHaveBeenCalledTimes(2); expect(transport).toBeDefined();
  });
});
