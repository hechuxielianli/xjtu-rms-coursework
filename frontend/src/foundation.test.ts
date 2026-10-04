import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import App from './App.vue';
import ErrorPanel from './components/ErrorPanel.vue';
import { createAppRouter } from './router';
import { useSession } from './auth/useSession';
import { api } from './api/client';
import { ApiFailure } from './errors/api-error';
import type { SessionResponse } from './api/contracts.generated';
import { business } from './api/business';

const example: SessionResponse = { user: { userId: '9007199254740993', displayName: '示例用户', accountStatus: 'ENABLED' }, roles: ['VIEWER'] };
beforeEach(() => { api.clearCsrf(); vi.spyOn(api, 'refreshCsrf').mockResolvedValue(); vi.spyOn(business, 'requirements').mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 }); vi.spyOn(business, 'auditEvents').mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 }); vi.spyOn(business, 'tags').mockResolvedValue({ items: [], page: 0, size: 100, totalElements: 0 }); });
afterEach(() => vi.restoreAllMocks());
describe('Wave 0 shell, routes and form developer checks', () => {
  it('renders the real login form and clears password even while a request is pending', async () => {
    vi.spyOn(api, 'session').mockRejectedValue(new ApiFailure('UNAUTHENTICATED', 'authentication', 401));
    let reject!: (error: unknown) => void;
    const send = vi.spyOn(api, 'login').mockImplementation(() => new Promise((_, rejecter) => { reject = rejecter; }));
    const pinia = createPinia(); const router = createAppRouter(pinia, createMemoryHistory());
    await router.push('/login');
    await router.isReady();
    const wrapper = mount(App, { global: { plugins: [pinia, router] } });
    await wrapper.find('#login').setValue('example'); await wrapper.find('#password').setValue(' padded ');
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect((wrapper.find('#password').element as HTMLInputElement).value).toBe('');
    expect(send).toHaveBeenCalledWith({ login: 'example', password: ' padded ' });
    reject(new ApiFailure('INVALID_CREDENTIALS', 'authentication', 401)); await flushPromises();
    expect(wrapper.text()).toContain('用户名、邮箱或密码不正确');
    expect((wrapper.find('#login').element as HTMLInputElement).value).toBe('example');
    expect(wrapper.text()).not.toContain(' padded '); wrapper.unmount();
  });
  it('protects deep links, uses fresh server roles and displays readonly system audit', async () => {
    let current = example; vi.spyOn(api, 'session').mockImplementation(async () => current);
    const pinia = createPinia(); const router = createAppRouter(pinia, createMemoryHistory());
    await router.push('/admin/users'); await router.isReady(); expect(router.currentRoute.value.path).toBe('/forbidden');
    current = { ...example, roles: ['ADMIN', 'REVIEWER'] }; await router.push('/admin/audit');
    const wrapper = mount(App, { global: { plugins: [pinia, router] } }); await flushPromises();
    expect(wrapper.text()).toContain('全系统审计记录'); expect(business.auditEvents).toHaveBeenCalledWith({ page: 0, size: 20 }); expect(wrapper.text()).toContain('系统审计'); expect(wrapper.text()).toContain('需求评审');
    expect(useSession(pinia).hasAnyRole(['REQUIREMENT_ENGINEER'])).toBe(false); expect(vi.mocked(api.session).mock.calls.length).toBeGreaterThanOrEqual(3); wrapper.unmount();
  });
  it('redirects an expired or disabled protected session to login', async () => {
    const read = vi.spyOn(api, 'session').mockResolvedValue(example);
    const pinia = createPinia(); const router = createAppRouter(pinia, createMemoryHistory());
    await router.push('/requirements'); await router.isReady();
    const wrapper = mount(App, { global: { plugins: [pinia, router] } });
    read.mockRejectedValue(new ApiFailure('ACCOUNT_DISABLED', 'authorization', 403));
    await useSession(pinia).refreshSession(); await flushPromises();
    expect(router.currentRoute.value.path).toBe('/login'); expect(wrapper.text()).toContain('账号已禁用'); wrapper.unmount();
  });
  it('keeps409 reconciliation an explicit action and never edits a local draft', async () => {
    const localDraft = { title: 'unsaved draft' };
    const wrapper = mount(ErrorPanel, { props: { failure: new ApiFailure('LOCK_VERSION_CONFLICT', 'conflict', 409) } });
    expect(wrapper.text()).toContain('本地表单保留'); expect(wrapper.emitted('reload')).toBeUndefined();
    await wrapper.find('button').trigger('click'); expect(wrapper.emitted('reload')).toHaveLength(1);
    expect(localDraft.title).toBe('unsaved draft'); wrapper.unmount();
  });
});
