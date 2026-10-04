import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import App from '../App.vue';
import { createAppRouter } from '../router';
import { api } from '../api/client';
import { business } from '../api/business';
import { ApiFailure } from '../errors/api-error';
import type { RequirementDecisionResponse, RequirementResponse, RoleCode } from '../api/contracts.generated';
import { syntheticPage, syntheticRequirement, syntheticReview, syntheticTag, syntheticVersion } from './fixtures';
const submitted: RequirementResponse = { ...syntheticRequirement, status: 'UNDER_REVIEW', title: '当前内容标题', source: '输入', rationale: '理由', acceptanceCriteria: '步骤与结果', firstSubmittedAt: syntheticReview.submittedAt };
beforeEach(() => {
  api.clearCsrf(); vi.spyOn(api, 'refreshCsrf').mockResolvedValue(); vi.spyOn(api, 'session').mockResolvedValue({ user: { userId: '2', displayName: '独立评审者', accountStatus: 'ENABLED' }, roles: ['REVIEWER'] });
  vi.spyOn(business, 'requirement').mockResolvedValue(submitted); vi.spyOn(business, 'reviews').mockResolvedValue(syntheticPage([syntheticReview], 100));
  vi.spyOn(business, 'pendingReviews').mockResolvedValue(syntheticPage([syntheticReview])); vi.spyOn(business, 'versions').mockResolvedValue(syntheticPage([syntheticVersion])); vi.spyOn(business, 'version').mockResolvedValue(syntheticVersion);
  vi.spyOn(business, 'tags').mockResolvedValue(syntheticPage([syntheticTag])); vi.spyOn(window, 'confirm').mockReturnValue(true);
});
afterEach(() => vi.restoreAllMocks());
async function workspace(path: string, roles: RoleCode[] = ['REVIEWER'], userId = '2') {
  vi.mocked(api.session).mockResolvedValue({ user: { userId, displayName: '开发账号', accountStatus: 'ENABLED' }, roles });
  const pinia = createPinia(); const router = createAppRouter(pinia, createMemoryHistory()); await router.push(path); await router.isReady();
  const wrapper = mount(App, { global: { plugins: [pinia, router] } }); await flushPromises(); return { wrapper, router };
}
const reviewPath = `/reviews/${syntheticReview.reviewId}?requirementId=${syntheticRequirement.requirementId}`;
describe('Wave 2 lifecycle, immutable review and version component developer checks', () => {
  it('reads the matching parent/history round and displays snapshot content instead of mutable current content', async () => {
    const { wrapper } = await workspace(reviewPath); expect(wrapper.text()).toContain(syntheticReview.snapshotTitle); expect(wrapper.text()).not.toContain('当前内容标题');
    expect(business.requirement).toHaveBeenCalledWith(syntheticRequirement.requirementId); expect(business.reviews).toHaveBeenCalledWith(syntheticRequirement.requirementId, { page: 0, size: 100 });
    expect(wrapper.find('form').exists()).toBe(true); wrapper.unmount();
  });
  it('excludes the actual creator despite ADMIN/engineer/reviewer union and a different submitter', async () => {
    const { wrapper } = await workspace(reviewPath, ['ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER'], '1');
    expect(wrapper.text()).toContain('不能评审自己创建的需求'); expect(wrapper.find('form').exists()).toBe(false); wrapper.unmount();
  });
  it('keeps completed rounds read-only for viewers and requires a valid deep-link parent', async () => {
    vi.mocked(business.reviews).mockResolvedValue(syntheticPage([{ ...syntheticReview, reviewStatus: 'COMPLETED', decision: 'REJECT', reviewerId: '2', comment: '保留的拒绝理由', decidedAt: '2026-10-03T01:00:00Z' }], 100));
    const { wrapper } = await workspace(reviewPath, ['VIEWER']); expect(wrapper.text()).toContain('保留的拒绝理由'); expect(wrapper.find('form').exists()).toBe(false); wrapper.unmount();
    vi.mocked(business.requirement).mockClear(); const missing = await workspace(`/reviews/${syntheticReview.reviewId}`, ['VIEWER']); expect(business.requirement).not.toHaveBeenCalled(); expect(missing.wrapper.text()).toContain('requirementId 上下文'); missing.wrapper.unmount();
  });
  it('preserves decision and reason on409, then uses the explicit refreshed parent revision', async () => {
    const result: RequirementDecisionResponse = { review: { ...syntheticReview, reviewStatus: 'COMPLETED', decision: 'REQUEST_CHANGES', comment: '本地修改意见', reviewerId: '2', decidedAt: '2026-10-03T01:00:00Z' }, requirement: { ...submitted, status: 'DRAFT', lockVersion: '9007199254740995' }, version: null };
    const send = vi.spyOn(business, 'decideReview').mockRejectedValueOnce(new ApiFailure('LOCK_VERSION_CONFLICT', 'conflict', 409)).mockImplementationOnce(async () => { vi.mocked(business.requirement).mockResolvedValue(result.requirement); vi.mocked(business.reviews).mockResolvedValue(syntheticPage([result.review], 100)); return result; });
    const { wrapper } = await workspace(reviewPath); await wrapper.find('select').setValue('REQUEST_CHANGES'); await wrapper.find('textarea').setValue('本地修改意见'); await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(send).toHaveBeenCalledTimes(1); expect(wrapper.text()).toContain('本地表单保留');
    vi.mocked(business.requirement).mockResolvedValueOnce({ ...submitted, lockVersion: '9007199254740994' }); await wrapper.find('.error-panel button').trigger('click'); await flushPromises();
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('本地修改意见'); expect((wrapper.find('select').element as HTMLSelectElement).value).toBe('REQUEST_CHANGES'); expect(send).toHaveBeenCalledTimes(1);
    await wrapper.find('form').trigger('submit'); await flushPromises(); expect(send).toHaveBeenLastCalledWith(syntheticReview.reviewId, syntheticRequirement.requirementId, { decision: 'REQUEST_CHANGES', comment: '本地修改意见', expectedRequirementLockVersion: '9007199254740994' }); expect(wrapper.text()).toContain('已完成决策：REQUEST_CHANGES'); expect(wrapper.find('form').exists()).toBe(false); wrapper.unmount();
  });
  it('blocks a decision replay after unknown500 until an explicit context read', async () => {
    const send = vi.spyOn(business, 'decideReview').mockRejectedValue(new ApiFailure('INTERNAL_ERROR', 'server', 500, undefined, undefined, true));
    const { wrapper } = await workspace(reviewPath); await wrapper.find('textarea').setValue('仍保留的意见'); await wrapper.find('form').trigger('submit'); await flushPromises(); await wrapper.find('form').trigger('submit'); await flushPromises(); expect(send).toHaveBeenCalledTimes(1);
    await wrapper.find('.error-panel button').trigger('click'); await flushPromises(); expect(send).toHaveBeenCalledTimes(1); expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('仍保留的意见'); wrapper.unmount();
  });
  it('gates complete draft submission and consumes the returned round before refreshing parent state', async () => {
    const send = vi.spyOn(business, 'submitRequirement').mockImplementation(async () => { vi.mocked(business.requirement).mockResolvedValue(submitted); return syntheticReview; });
    vi.mocked(business.requirement).mockResolvedValue({ ...syntheticRequirement, source: '来源', rationale: '理由', acceptanceCriteria: '步骤与结果' });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['REQUIREMENT_ENGINEER']); await wrapper.find('.lifecycle-actions button').trigger('click'); await flushPromises();
    expect(send).toHaveBeenCalledWith(syntheticRequirement.requirementId, { expectedLockVersion: syntheticRequirement.lockVersion }); expect(wrapper.text()).toContain('UNDER_REVIEW'); expect(wrapper.findAll('button').some(button => button.text() === '提交评审')).toBe(false); wrapper.unmount();
    vi.mocked(business.requirement).mockResolvedValue(syntheticRequirement); const incomplete = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['REQUIREMENT_ENGINEER']); expect(incomplete.wrapper.find('.lifecycle-actions button').attributes('disabled')).toBeDefined(); incomplete.wrapper.unmount();
  });
  it('binds a member implementation note to the displayed current formal version and revision', async () => {
    vi.mocked(business.requirement).mockResolvedValue({ ...submitted, status: 'APPROVED', currentVersionId: syntheticVersion.versionId });
    const send = vi.spyOn(business, 'confirmVersion').mockResolvedValueOnce({ ...submitted, status: 'IMPLEMENTED', currentVersionId: syntheticVersion.versionId, lockVersion: '9007199254740994' }).mockResolvedValueOnce({ ...submitted, status: 'VERIFIED', currentVersionId: syntheticVersion.versionId, lockVersion: '9007199254740995' });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['PROJECT_MEMBER']); await wrapper.find('.lifecycle-actions textarea').setValue('实际实现说明'); await wrapper.find('.lifecycle-actions form').trigger('submit'); await flushPromises();
    expect(send).toHaveBeenCalledWith(syntheticRequirement.requirementId, 'implement', { expectedVersionId: syntheticVersion.versionId, expectedLockVersion: submitted.lockVersion, description: '实际实现说明' }); expect(wrapper.text()).toContain('确认验证当前正式版本'); expect((wrapper.find('.lifecycle-actions textarea').element as HTMLTextAreaElement).value).toBe('');
    await wrapper.find('.lifecycle-actions textarea').setValue('实际验证说明'); await wrapper.find('.lifecycle-actions form').trigger('submit'); await flushPromises(); expect(send).toHaveBeenLastCalledWith(syntheticRequirement.requirementId, 'verify', { expectedVersionId: syntheticVersion.versionId, expectedLockVersion: '9007199254740994', description: '实际验证说明' }); expect(wrapper.text()).toContain('VERIFIED'); expect(wrapper.find('.lifecycle-actions form').exists()).toBe(false); wrapper.unmount();
    const admin = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['ADMIN']); expect(admin.wrapper.find('.lifecycle-actions form').exists()).toBe(false); admin.wrapper.unmount();
  });
  it('shows V1 provenance/current marker and full authorized historical content as read-only', async () => {
    vi.mocked(business.requirement).mockResolvedValue({ ...submitted, status: 'APPROVED', currentVersionId: syntheticVersion.versionId });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=versions&versionId=${syntheticVersion.versionId}`, ['VIEWER']);
    expect(wrapper.text()).toContain('V1'); expect(wrapper.text()).toContain('当前'); expect(wrapper.text()).toContain('首次批准'); expect(wrapper.text()).toContain(syntheticVersion.acceptanceCriteria); expect(wrapper.findAll('textarea')).toHaveLength(0); expect(wrapper.findAll('form')).toHaveLength(0);
    expect(business.version).toHaveBeenCalledWith(syntheticRequirement.requirementId, syntheticVersion.versionId); wrapper.unmount();
  });
  it('uses queue parent IDs in history deep links and restricts the pending workspace to reviewers', async () => {
    const { wrapper, router } = await workspace('/reviews'); expect(business.pendingReviews).toHaveBeenCalledWith({ page: 0, size: 20 }); expect(wrapper.findAll('a').some(link => link.attributes('href') === reviewPath)).toBe(true); wrapper.unmount();
    const viewer = await workspace('/reviews', ['VIEWER']); expect(viewer.router.currentRoute.value.path).toBe('/forbidden'); viewer.wrapper.unmount(); expect(router).toBeDefined();
  });
  it('reopens only a rejected requirement, preserving the prior first-submission context', async () => {
    const rejected = { ...submitted, status: 'REJECTED' as const }; vi.mocked(business.requirement).mockResolvedValue(rejected);
    const send = vi.spyOn(business, 'reopenRequirement').mockResolvedValue({ ...rejected, status: 'DRAFT', lockVersion: '9007199254740994' });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['REQUIREMENT_ENGINEER']); await wrapper.find('.lifecycle-actions button').trigger('click'); await flushPromises();
    expect(send).toHaveBeenCalledWith(syntheticRequirement.requirementId, { expectedLockVersion: rejected.lockVersion }); expect(wrapper.text()).toContain('DRAFT'); expect(wrapper.findAll('button').some(button => button.text() === '提交评审')).toBe(true); wrapper.unmount();
  });
  it('revalidates the actual reviewer role before a decision instead of trusting an earlier route grant', async () => {
    const send = vi.spyOn(business, 'decideReview'); const { wrapper } = await workspace(reviewPath);
    vi.mocked(api.session).mockResolvedValue({ user: { userId: '2', displayName: '角色已更新', accountStatus: 'ENABLED' }, roles: ['VIEWER'] });
    await wrapper.find('form').trigger('submit'); await flushPromises(); expect(send).not.toHaveBeenCalled(); expect(wrapper.find('form').exists()).toBe(false); expect(wrapper.text()).toContain('当前账号、需求状态或评审轮次不允许决策'); wrapper.unmount();
  });
});
