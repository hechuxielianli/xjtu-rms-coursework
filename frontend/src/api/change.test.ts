import axios, { type AxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './client';
import { createBusinessApi } from './business';
import { syntheticChange, syntheticChangeReview, syntheticPage, syntheticRequirement, syntheticVersion } from '../features/fixtures';
function setup(reply: unknown) {
  const calls: AxiosRequestConfig[] = []; const transport = axios.create({ adapter: async config => { calls.push(config); return { config, data: config.url === '/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic-csrf' } : reply, status: 200, statusText: '', headers: {} }; } });
  return { business: createBusinessApi(createApiClient(transport)), calls, transport };
}
describe('Wave 3 controlled-change transport developer checks', () => {
  it('creates with only title/reason/parent revision and consumes server copied base/content', async () => {
    const { business, calls } = setup({ ...syntheticChange, secret: 'omitted' });
    const result = await business.createChange(syntheticRequirement.requirementId, { requestTitle: syntheticChange.requestTitle, reason: syntheticChange.reason, expectedRequirementLockVersion: syntheticRequirement.lockVersion, baseVersionId: '99', proposedTitle: 'override', createdBy: '99', status: 'APPROVED' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ requestTitle: syntheticChange.requestTitle, reason: syntheticChange.reason, expectedRequirementLockVersion: syntheticRequirement.lockVersion }); expect(result).toEqual(syntheticChange);
  });
  it('whitelists draft fields and string revision, excluding immutable/actor/state fields', async () => {
    const { business, calls } = setup(syntheticChange);
    await business.editChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { proposedTitle: '新建议', proposedSource: null, expectedLockVersion: syntheticChange.lockVersion, requirementId: '99', baseVersionId: '99', status: 'DRAFT', appliedBy: '2' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ proposedTitle: '新建议', proposedSource: null, expectedLockVersion: syntheticChange.lockVersion });
    await expect(business.editChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_INPUT' });
    await expect(business.editChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { proposedTitle: 'x', expectedLockVersion: '9223372036854775808' })).rejects.toMatchObject({ code: 'INVALID_INPUT' }); expect(calls).toHaveLength(2);
  });
  it('submits only the CR revision, reads all eleven snapshot fields and numeric round', async () => {
    const { business, calls } = setup(syntheticChangeReview);
    expect(await business.submitChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedLockVersion: syntheticChange.lockVersion, snapshotBaseVersionId: '99', snapshotReason: 'override' } as never)).toEqual(syntheticChangeReview);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedLockVersion: syntheticChange.lockVersion }); expect(typeof syntheticChangeReview.roundNo).toBe('number');
  });
  it.each(['REJECT', 'REQUEST_CHANGES'] as const)('requires a nonblank %s comment before sending', async decision => {
    const { business, calls } = setup({}); await expect(business.decideChangeReview(syntheticChangeReview.changeReviewId, syntheticChange.changeRequestId, syntheticChange.requirementId, { decision, comment: '  ', expectedChangeLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_INPUT' }); expect(calls).toHaveLength(0);
  });
  it('decides with only expectedChangeLockVersion and never triggers apply', async () => {
    const review = { ...syntheticChangeReview, reviewStatus: 'COMPLETED', decision: 'APPROVE', reviewerId: '2', decidedAt: '2026-10-03T04:00:00Z' };
    const { business, calls } = setup({ review, change: { ...syntheticChange, status: 'APPROVED' } });
    await business.decideChangeReview(syntheticChangeReview.changeReviewId, syntheticChange.changeRequestId, syntheticChange.requirementId, { decision: 'APPROVE', expectedChangeLockVersion: syntheticChange.lockVersion, expectedRequirementLockVersion: '99', reviewerId: '2', versionNo: 2 } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ decision: 'APPROVE', expectedChangeLockVersion: syntheticChange.lockVersion }); expect(calls.map(call => call.url)).not.toContain(`/changes/${syntheticChange.changeRequestId}/apply`);
  });
  it('applies with both parent/CR revisions and reads the actual V2 aggregate', async () => {
    const version = { ...syntheticVersion, versionId: '9007199254741000', versionNo: 2, initialReviewId: null, appliedChangeRequestId: syntheticChange.changeRequestId };
    const result = { change: { ...syntheticChange, status: 'APPLIED', appliedBy: '1', appliedAt: '2026-10-03T04:00:00Z' }, requirement: { ...syntheticRequirement, status: 'APPROVED', currentVersionId: version.versionId }, version };
    const { business, calls } = setup(result);
    expect(await business.applyChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedRequirementLockVersion: syntheticRequirement.lockVersion, expectedChangeLockVersion: syntheticChange.lockVersion, currentVersionId: '99', versionNo: 2, status: 'APPLIED' } as never)).toEqual(result);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedRequirementLockVersion: syntheticRequirement.lockVersion, expectedChangeLockVersion: syntheticChange.lockVersion });
  });
  it('cancels with only CR revision and validates both aggregate IDs', async () => {
    const { business, calls } = setup({ ...syntheticChange, status: 'CANCELLED' });
    await business.cancelChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedLockVersion: syntheticChange.lockVersion, createdBy: '99' } as never); expect(JSON.parse(String(calls[1]?.data))).toEqual({ expectedLockVersion: syntheticChange.lockVersion });
    const wrong = setup({ ...syntheticChange, requirementId: '2' }); await expect(wrong.business.cancelChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true });
  });
  it('uses paged filter/history endpoints and rejects cross-parent or invalid numeric results', async () => {
    const { business, calls } = setup(syntheticPage([syntheticChange])); expect(await business.changes(syntheticChange.requirementId, { page: 0, size: 20 }, 'DRAFT')).toEqual(syntheticPage([syntheticChange])); expect(calls[0]?.url).toBe(`/requirements/${syntheticChange.requirementId}/changes?page=0&size=20&status=DRAFT`);
    const wrong = setup(syntheticPage([{ ...syntheticChangeReview, requirementId: '2' }])); await expect(wrong.business.changeReviews(syntheticChange.changeRequestId, syntheticChange.requirementId, { page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
    const invalid = setup(syntheticPage([{ ...syntheticChangeReview, roundNo: '1' }])); await expect(invalid.business.pendingChangeReviews({ page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
  });
  it('rejects mismatched decision and apply results with unknown outcome and no replay', async () => {
    const wrong = setup({ review: { ...syntheticChangeReview, changeRequestId: '2', decision: 'APPROVE' }, change: syntheticChange }); const spy = vi.spyOn(wrong.transport, 'request');
    await expect(wrong.business.decideChangeReview(syntheticChangeReview.changeReviewId, syntheticChange.changeRequestId, syntheticChange.requirementId, { decision: 'APPROVE', expectedChangeLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true }); expect(spy).toHaveBeenCalledTimes(2);
    const invalid = setup({ change: syntheticChange, requirement: { ...syntheticRequirement, currentVersionId: syntheticVersion.versionId }, version: syntheticVersion }); await expect(invalid.business.applyChange(syntheticChange.changeRequestId, syntheticChange.requirementId, { expectedRequirementLockVersion: '0', expectedChangeLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true });
  });
});
