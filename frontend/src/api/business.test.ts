import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './client';
import { createBusinessApi, pageQuery, requirementQuery } from './business';
import { readContract } from './schema';
import { syntheticComment, syntheticPage, syntheticRequirement, syntheticTag, syntheticUser } from '../features/fixtures';
const response = (config: InternalAxiosRequestConfig, data: unknown) => ({ config, data, status: 200, statusText: '', headers: {} });
function setup(reply: (config: InternalAxiosRequestConfig) => unknown) {
  const calls: AxiosRequestConfig[] = [];
  const transport = axios.create({ adapter: async config => { calls.push(config); return response(config, config.url === '/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic-csrf' } : reply(config)); } });
  return { calls, transport, business: createBusinessApi(createApiClient(transport)) };
}
describe('Wave 1 contract and request developer checks', () => {
  it('uses AND filters together, encoded keyword, decimal tag id and zero-based pages', () => {
    const query = new URLSearchParams(requirementQuery({ page: 0, size: 100, keyword: 'A&B 空格', status: 'DRAFT', kind: 'FUNCTIONAL', level: 'SYSTEM', priority: 'HIGH', tag: syntheticTag.tagId }));
    expect(Object.fromEntries(query)).toEqual({ page: '0', size: '100', keyword: 'A&B 空格', status: 'DRAFT', kind: 'FUNCTIONAL', level: 'SYSTEM', priority: 'HIGH', tag: syntheticTag.tagId });
    for (const input of [{ page: -1, size: 20 }, { page: 0, size: 101 }, { page: 0, size: 0 }, { page: .5, size: 20 }]) expect(() => pageQuery(input)).toThrow();
    expect(() => requirementQuery({ page: 0, size: 20, tag: '01' })).toThrow();
  });
  it('constructs safe DTO projections and rejects imprecise IDs, revisions and invalid pages', () => {
    expect(readContract('AdminUserResponse', { ...syntheticUser, passwordHash: 'untrusted-extra' })).toEqual(syntheticUser);
    expect(readContract('RequirementResponse', syntheticRequirement)).toEqual(syntheticRequirement);
    expect(() => readContract('RequirementResponse', { ...syntheticRequirement, lockVersion: 9007199254740993 })).toThrow();
    expect(() => readContract('RequirementResponsePage', { ...syntheticPage([]), totalElements: '0' })).toThrow();
  });
  it('creates a user with the unchanged raw password and only five permitted fields', async () => {
    const { business, calls } = setup(() => syntheticUser); const password = ' e\u0301 ';
    await business.createUser({ username: 'synthetic-user', email: 'synthetic@example.test', displayName: '开发用户', initialPassword: password, roles: ['VIEWER'], actorId: '9', accountStatus: 'ENABLED' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ username: 'synthetic-user', email: 'synthetic@example.test', displayName: '开发用户', initialPassword: password, roles: ['VIEWER'] });
    expect(calls[1]?.withCredentials).toBe(true); expect(calls[1]?.headers?.['X-CSRF-TOKEN']).toBe('synthetic-csrf');
    expect(() => business.createUser({ username: 'x', email: 'x@example.test', displayName: 'x', initialPassword: '汉'.repeat(25), roles: ['ADMIN'] })).toThrow();
    expect(calls).toHaveLength(2);
  });
  it('keeps partial content and metadata commands independent with string revisions', async () => {
    const { business, calls } = setup(() => syntheticRequirement);
    await business.editContent(syntheticRequirement.requirementId, { source: null, expectedLockVersion: syntheticRequirement.lockVersion, status: 'APPROVED', creatorId: '9' } as never);
    await business.editMetadata(syntheticRequirement.requirementId, { assigneeId: null, tagIds: [syntheticTag.tagId], expectedLockVersion: syntheticRequirement.lockVersion, title: 'wrong-context' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ source: null, expectedLockVersion: syntheticRequirement.lockVersion });
    expect(JSON.parse(String(calls[2]?.data))).toEqual({ assigneeId: null, tagIds: [syntheticTag.tagId], expectedLockVersion: syntheticRequirement.lockVersion });
    expect(calls.some(call => call.url?.startsWith('/users'))).toBe(false);
    expect(() => business.editContent(syntheticRequirement.requirementId, { expectedLockVersion: '0' })).toThrow();
  });
  it('does not invent a Tag lockVersion or allow author/time fields in comments', async () => {
    const { business, calls } = setup(config => config.url?.startsWith('/tags') ? syntheticTag : syntheticComment);
    await business.editTag(syntheticTag.tagId, { name: '共享标签', description: null, expectedLockVersion: '123' } as never);
    await business.createComment(syntheticRequirement.requirementId, { content: '<b>纯文本</b>', authorId: '9', createdAt: 'wrong' } as never);
    expect(JSON.parse(String(calls[1]?.data))).toEqual({ name: '共享标签', description: null });
    expect(JSON.parse(String(calls[2]?.data))).toEqual({ content: '<b>纯文本</b>' });
  });
  it.each([
    { ...syntheticComment, requirementId: '2' },
    { ...syntheticComment, isDeleted: 1, content: 'must-never-be-displayed' },
  ])('rejects a mismatched parent or leaked tombstone body before display', async comment => {
    const { business } = setup(() => syntheticPage([comment]));
    await expect(business.comments(syntheticRequirement.requirementId, { page: 0, size: 20 })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' });
  });
  it('marks an invalid success response to a mutation unknown and never repeats it', async () => {
    const { business, transport } = setup(() => ({ requirementId: 3 })); const request = vi.spyOn(transport, 'request');
    await expect(business.createRequirement({ title: 'x', description: 'x', level: 'SYSTEM', kind: 'FUNCTIONAL', priority: 'LOW' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true });
    expect(request).toHaveBeenCalledTimes(2);
  });
  it('refuses a mutation response identifying another aggregate', async () => {
    const { business } = setup(() => ({ ...syntheticRequirement, requirementId: '2' }));
    await expect(business.editMetadata(syntheticRequirement.requirementId, { assigneeId: null, expectedLockVersion: '0' })).rejects.toMatchObject({ code: 'INVALID_RESPONSE', outcomeUnknown: true });
  });
});
