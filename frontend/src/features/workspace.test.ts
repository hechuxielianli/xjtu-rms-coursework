import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import App from '../App.vue';
import { createAppRouter } from '../router';
import { api } from '../api/client';
import { business } from '../api/business';
import { ApiFailure } from '../errors/api-error';
import type { AdminUserResponse, RequirementResponse, RoleCode } from '../api/contracts.generated';
import { syntheticComment, syntheticPage, syntheticRequirement, syntheticTag, syntheticUser } from './fixtures';

beforeEach(() => {
  api.clearCsrf(); vi.spyOn(api, 'refreshCsrf').mockResolvedValue();
  vi.spyOn(api, 'session').mockResolvedValue({ user: syntheticRequirement.creator, roles: ['ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER'] });
  vi.spyOn(business, 'requirements').mockResolvedValue(syntheticPage([syntheticRequirement]));
  vi.spyOn(business, 'requirement').mockResolvedValue(syntheticRequirement);
  vi.spyOn(business, 'users').mockResolvedValue(syntheticPage([syntheticUser]));
  vi.spyOn(business, 'tags').mockResolvedValue(syntheticPage([syntheticTag]));
  vi.spyOn(business, 'history').mockResolvedValue(syntheticPage([]));
  vi.spyOn(business, 'comments').mockResolvedValue(syntheticPage([syntheticComment]));
  vi.spyOn(window, 'confirm').mockReturnValue(true);
});
afterEach(() => vi.restoreAllMocks());
async function workspace(path: string, roles: RoleCode[] = ['ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER']) {
  vi.mocked(api.session).mockResolvedValue({ user: syntheticRequirement.creator, roles });
  const pinia = createPinia(); const router = createAppRouter(pinia, createMemoryHistory());
  await router.push(path); await router.isReady();
  const wrapper = mount(App, { global: { plugins: [pinia, router] } }); await flushPromises();
  return { wrapper, router };
}
describe('Wave 1 workspace component developer checks', () => {
  it('loads a validated parent before comments and loads readonly history without replaying comment reads', async () => {
    let finish!: (value: RequirementResponse) => void;
    vi.mocked(business.requirement).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const { wrapper, router } = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=comments`);
    expect(business.comments).not.toHaveBeenCalled(); finish(syntheticRequirement); await flushPromises();
    expect(business.comments).toHaveBeenCalledWith(syntheticRequirement.requirementId, { page: 0, size: 20 });
    expect(wrapper.find('[role=tab][data-tab=comments]').attributes('aria-selected')).toBe('true');
    vi.mocked(business.requirement).mockResolvedValueOnce(syntheticRequirement);
    await router.push(`/requirements/${syntheticRequirement.requirementId}?tab=history`); await flushPromises();
    expect(wrapper.text()).toContain('需求业务历史'); expect(business.history).toHaveBeenCalledWith(syntheticRequirement.requirementId, { page: 0, size: 20 }); expect(business.requirement).toHaveBeenCalledTimes(2); expect(business.comments).toHaveBeenCalledTimes(1); wrapper.unmount();
  });
  it('preserves content on409 and only adopts the refreshed revision after explicit reload', async () => {
    const send = vi.spyOn(business, 'editContent').mockRejectedValueOnce(new ApiFailure('LOCK_VERSION_CONFLICT', 'conflict', 409)).mockResolvedValueOnce({ ...syntheticRequirement, title: '本地标题', lockVersion: '9007199254740995' });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}/edit`);
    await wrapper.find('input').setValue('本地标题'); await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(send).toHaveBeenCalledTimes(1); expect(wrapper.text()).toContain('本地表单保留');
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('本地标题');
    vi.mocked(business.requirement).mockResolvedValueOnce({ ...syntheticRequirement, title: '服务器的新标题', lockVersion: '9007199254740994' });
    await wrapper.find('.error-panel button').trigger('click'); await flushPromises();
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('本地标题'); expect(send).toHaveBeenCalledTimes(1);
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(send).toHaveBeenLastCalledWith(syntheticRequirement.requirementId, expect.objectContaining({ title: '本地标题', expectedLockVersion: '9007199254740994' }));
    expect(wrapper.text()).toContain('草稿已保存'); wrapper.unmount();
  });
  it('keeps known assignee input separate from content and never reads the ADMIN directory for an engineer', async () => {
    const save = vi.spyOn(business, 'editMetadata').mockResolvedValue({ ...syntheticRequirement, assigneeId: '2', assignee: { userId: '2', displayName: '已知负责人', accountStatus: 'ENABLED' }, lockVersion: '9007199254740994' });
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}`, ['REQUIREMENT_ENGINEER']);
    await wrapper.find('input[placeholder="已知且启用的用户 ID，留空清除"]').setValue('2');
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(save).toHaveBeenCalledWith(syntheticRequirement.requirementId, { assigneeId: '2', tagIds: [syntheticTag.tagId], expectedLockVersion: syntheticRequirement.lockVersion });
    expect(business.users).not.toHaveBeenCalled(); expect(wrapper.text()).toContain('已知负责人'); wrapper.unmount();
  });
  it('clears initialPassword while user creation is pending and preserves non-secret fields on failure', async () => {
    let reject!: (failure: ApiFailure) => void;
    const send = vi.spyOn(business, 'createUser').mockImplementation(() => new Promise((_, rejecter) => { reject = rejecter; }));
    const { wrapper } = await workspace('/admin/users', ['ADMIN']);
    await wrapper.findAll('button').find(button => button.text() === '创建用户')!.trigger('click');
    const inputs = wrapper.find('form').findAll('input');
    await inputs[0]!.setValue('fresh-user'); await inputs[1]!.setValue('fresh@example.test'); await inputs[2]!.setValue('新用户');
    await wrapper.find('#initial-password').setValue(' e\u0301 '); await wrapper.find('input[type=checkbox]').setValue(true);
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect((wrapper.find('#initial-password').element as HTMLInputElement).value).toBe('');
    expect(send).toHaveBeenCalledWith({ username: 'fresh-user', email: 'fresh@example.test', displayName: '新用户', initialPassword: ' e\u0301 ', roles: ['ADMIN'] });
    reject(new ApiFailure('DUPLICATE', 'conflict', 409)); await flushPromises();
    expect((wrapper.find('form input').element as HTMLInputElement).value).toBe('fresh-user'); expect(wrapper.text()).not.toContain(' e\u0301 '); wrapper.unmount();
  });
  it('renders comments as escaped text, hides tombstone content and limits deletion to the current author', async () => {
    vi.mocked(business.comments).mockResolvedValue(syntheticPage([syntheticComment, { ...syntheticComment, commentId: '10', authorId: '2' }, { ...syntheticComment, commentId: '11', isDeleted: 1, content: null, deletedBy: '1', deletedAt: '2026-10-03T01:00:00Z' }]));
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=comments`, ['PROJECT_MEMBER']);
    expect(wrapper.find('b').exists()).toBe(false); expect(wrapper.text()).toContain('<b>纯文本</b>'); expect(wrapper.text()).toContain('此评论已删除');
    expect(wrapper.findAll('button').filter(button => button.text() === '删除本人评论')).toHaveLength(1); wrapper.unmount();
    const viewer = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=comments`, ['VIEWER']);
    expect(viewer.wrapper.findAll('form')).toHaveLength(0); expect(viewer.wrapper.text()).not.toContain('删除本人评论'); viewer.wrapper.unmount();
  });
  it('blocks duplicate comment sends and requires explicit reconciliation after an unknown500 result', async () => {
    let reject!: (failure: ApiFailure) => void;
    const send = vi.spyOn(business, 'createComment').mockImplementation(() => new Promise((_, rejecter) => { reject = rejecter; }));
    const { wrapper } = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=comments`, ['PROJECT_MEMBER']);
    await wrapper.find('textarea').setValue('保留的评论草稿'); await wrapper.find('form').trigger('submit'); await wrapper.find('form').trigger('submit'); await flushPromises(); expect(send).toHaveBeenCalledTimes(1);
    reject(new ApiFailure('INTERNAL_ERROR', 'server', 500, undefined, undefined, true)); await flushPromises();
    await wrapper.find('form').trigger('submit'); await flushPromises(); expect(send).toHaveBeenCalledTimes(1);
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('保留的评论草稿');
    await wrapper.find('.error-panel button').trigger('click'); await flushPromises(); expect(business.comments).toHaveBeenCalledTimes(2); expect(send).toHaveBeenCalledTimes(1); wrapper.unmount();
  });
  it('allows a disabled user empty roles while an enabled user cannot be left without roles', async () => {
    const { wrapper } = await workspace('/admin/users', ['ADMIN']);
    await wrapper.findAll('button').find(button => button.text() === '管理')!.trigger('click');
    for (const input of wrapper.findAll('input[type=checkbox]')) await input.setValue(false);
    expect(wrapper.findAll('button').find(button => button.text() === '保存角色集合')!.attributes('disabled')).toBeDefined(); wrapper.unmount();
    const disabled: AdminUserResponse = { ...syntheticUser, userId: '2', accountStatus: 'DISABLED', roles: [], assignments: [] };
    vi.mocked(business.users).mockResolvedValue(syntheticPage([disabled])); const save = vi.spyOn(business, 'setRoles').mockResolvedValue({ ...disabled, lockVersion: '9007199254740994' });
    const second = await workspace('/admin/users', ['ADMIN']); await second.wrapper.findAll('button').find(button => button.text() === '管理')!.trigger('click');
    expect(second.wrapper.findAll('button').find(button => button.text() === '保存角色集合')!.attributes('disabled')).toBeUndefined();
    await second.wrapper.findAll('form')[1]!.trigger('submit'); await flushPromises(); expect(save).toHaveBeenCalledWith('2', { roles: [], expectedLockVersion: disabled.lockVersion }); second.wrapper.unmount();
  });
  it('discards an older parent response after a new requirement route wins', async () => {
    let finish!: (value: RequirementResponse) => void;
    vi.mocked(business.requirement).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; })).mockResolvedValueOnce({ ...syntheticRequirement, requirementId: '2', title: '第二个需求' });
    const { wrapper, router } = await workspace(`/requirements/${syntheticRequirement.requirementId}?tab=history`, ['VIEWER']);
    await router.push('/requirements/2?tab=history'); await flushPromises(); expect(wrapper.text()).toContain('第二个需求');
    finish(syntheticRequirement); await flushPromises(); expect(wrapper.text()).toContain('第二个需求'); expect(wrapper.text()).not.toContain(syntheticRequirement.title); wrapper.unmount();
  });
  it('discards a late save response after the editor changes to another requirement', async () => {
    let finish!: (value: RequirementResponse) => void;
    vi.spyOn(business, 'editContent').mockImplementation(() => new Promise(resolve => { finish = resolve; }));
    const { wrapper, router } = await workspace(`/requirements/${syntheticRequirement.requirementId}/edit`, ['REQUIREMENT_ENGINEER']);
    await wrapper.find('input').setValue('第一项本地草稿'); await wrapper.find('form').trigger('submit'); await flushPromises();
    vi.mocked(business.requirement).mockResolvedValueOnce({ ...syntheticRequirement, requirementId: '2', title: '第二项内容' });
    await router.push('/requirements/2/edit'); await flushPromises();
    finish({ ...syntheticRequirement, title: '第一项保存结果' }); await flushPromises();
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('第二项内容'); expect(wrapper.text()).not.toContain('草稿已保存'); wrapper.unmount();
  });
  it('does not erase an unsaved role selection when only profile fields are saved', async () => {
    const save = vi.spyOn(business, 'editUser').mockResolvedValue({ ...syntheticUser, displayName: '资料新名称', lockVersion: '9007199254740994' });
    const { wrapper } = await workspace('/admin/users', ['ADMIN']); await wrapper.findAll('button').find(button => button.text() === '管理')!.trigger('click');
    const viewerRole = wrapper.findAll('input[type=checkbox]').at(-1)!; await viewerRole.setValue(true);
    await wrapper.findAll('input')[2]!.setValue('资料新名称'); await wrapper.findAll('form')[0]!.trigger('submit'); await flushPromises();
    expect(save).toHaveBeenCalledWith('1', expect.objectContaining({ displayName: '资料新名称', expectedLockVersion: syntheticUser.lockVersion }));
    expect((viewerRole.element as HTMLInputElement).checked).toBe(true); wrapper.unmount();
  });
});
