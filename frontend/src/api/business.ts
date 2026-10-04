import { api, type createApiClient } from './client';
import type * as C from './contracts.generated';
import { commandContract, readContract } from './schema';
import { isDecimalString } from './decimal';
import { ApiFailure, normalizeFailure } from '../errors/api-error';
import { passwordProblem } from '../auth/password';
import type { contractSchemas } from './schemas.generated';
import { utcInstant } from '../features/time';

export type RequirementFilters = { page: number; size: number; keyword?: string; status?: C.RequirementStatus; kind?: C.RequirementKind; level?: C.RequirementLevel; priority?: C.Priority; tag?: string };
export type Pagination = { page: number; size: number };
export type TraceOptions = { depth: number; relationType?: C.RelationType };
export type RelationRevisions = { expectedSourceLockVersion: string; expectedTargetLockVersion: string };
export type AuditFilters = Pagination & { from?: string; to?: string; action?: string };
export function auditQuery(input: AuditFilters): URLSearchParams {
  const query = pageQuery(input); const from = input.from ? utcInstant(input.from) : null; const to = input.to ? utcInstant(input.to) : null;
  if (input.from && !from || input.to && !to || from && to && from > to || input.action !== undefined && (typeof input.action !== 'string' || Array.from(input.action).length > 64)) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
  for (const key of ['from', 'to', 'action'] as const) if (input[key]) query.set(key, input[key]);
  return query;
}
export function requireId(value: unknown): string { if (!isDecimalString(value)) throw new ApiFailure('INVALID_INPUT', 'validation', 400); return value; }
export function pageQuery(input: Pagination): URLSearchParams {
  if (!Number.isSafeInteger(input.page) || input.page < 0 || !Number.isSafeInteger(input.size) || input.size < 1 || input.size > 100) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
  return new URLSearchParams({ page: String(input.page), size: String(input.size) });
}
export function requirementQuery(input: RequirementFilters): string {
  const query = pageQuery(input);
  for (const key of ['keyword', 'status', 'kind', 'level', 'priority', 'tag'] as const) {
    const value = input[key];
    if (value !== undefined && value !== '') {
      if (key === 'tag') requireId(value);
      if (key !== 'keyword' && key !== 'tag') commandContract(key === 'priority' ? 'Priority' : key === 'status' ? 'RequirementStatus' : key === 'kind' ? 'RequirementKind' : 'RequirementLevel', value);
      query.set(key, value);
    }
  }
  return query.toString();
}
export function createBusinessApi(client: ReturnType<typeof createApiClient> = api) {
  async function response<T>(method: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE', path: string, schema?: keyof typeof contractSchemas, data?: unknown, expected?: { key: string; id: string }): Promise<T> {
    const unsafe = method !== 'GET';
    try {
      if (unsafe && !client.hasCsrf()) await client.refreshCsrf();
      const value = await client.request<unknown>(method, path, data);
      if (!schema) return undefined as T;
      const result = readContract<T>(schema, value);
      if (expected && (result as Record<string, unknown>)[expected.key] !== expected.id) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return result;
    } catch (error) {
      const failure = normalizeFailure(error, unsafe);
      if (unsafe && failure.kind === 'protocol' && !failure.outcomeUnknown) throw new ApiFailure(failure.code, failure.kind, failure.status, undefined, undefined, true);
      throw failure;
    }
  }
  return {
    users: (page: Pagination) => response<C.AdminUserResponsePage>('GET', `/users?${pageQuery(page)}`, 'AdminUserResponsePage'),
    createUser(input: C.CreateUserRequest) {
      if (passwordProblem(input.initialPassword)) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
      return response<C.AdminUserResponse>('POST', '/users', 'AdminUserResponse', commandContract('CreateUserRequest', input));
    },
    editUser: (id: string, input: C.EditUserRequest) => response<C.AdminUserResponse>('PATCH', `/users/${requireId(id)}`, 'AdminUserResponse', commandContract('EditUserRequest', input), { key: 'userId', id }),
    setRoles: (id: string, input: C.SetRolesRequest) => response<C.AdminUserResponse>('PUT', `/users/${requireId(id)}/roles`, 'AdminUserResponse', commandContract('SetRolesRequest', input), { key: 'userId', id }),
    userStatus: (id: string, enabled: boolean, input: C.LockCommand) => response<C.AdminUserResponse>('POST', `/users/${requireId(id)}/${enabled ? 'enable' : 'disable'}`, 'AdminUserResponse', commandContract('LockCommand', input), { key: 'userId', id }),
    requirements: (filters: RequirementFilters) => response<C.RequirementResponsePage>('GET', `/requirements?${requirementQuery(filters)}`, 'RequirementResponsePage'),
    async requirement(id: string) {
      const value = await response<C.RequirementResponse>('GET', `/requirements/${requireId(id)}`, 'RequirementResponse');
      if (value.requirementId !== id) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    createRequirement: (input: C.CreateRequirementRequest) => response<C.RequirementResponse>('POST', '/requirements', 'RequirementResponse', commandContract('CreateRequirementRequest', input)),
    editContent: (id: string, input: C.EditRequirementContentRequest) => response<C.RequirementResponse>('PATCH', `/requirements/${requireId(id)}/content`, 'RequirementResponse', commandContract('EditRequirementContentRequest', input), { key: 'requirementId', id }),
    editMetadata: (id: string, input: C.EditMetadataRequest) => response<C.RequirementResponse>('PATCH', `/requirements/${requireId(id)}/metadata`, 'RequirementResponse', commandContract('EditMetadataRequest', input), { key: 'requirementId', id }),
    tags: (page: Pagination, name?: string) => response<C.TagResponsePage>('GET', `/tags?${pageQuery(page)}${name ? `&name=${encodeURIComponent(name)}` : ''}`, 'TagResponsePage'),
    createTag: (input: C.CreateTagRequest) => response<C.TagResponse>('POST', '/tags', 'TagResponse', commandContract('CreateTagRequest', input)),
    editTag: (id: string, input: C.EditTagRequest) => response<C.TagResponse>('PATCH', `/tags/${requireId(id)}`, 'TagResponse', commandContract('EditTagRequest', input), { key: 'tagId', id }),
    async comments(id: string, page: Pagination) {
      const value = await response<C.CommentResponsePage>('GET', `/requirements/${requireId(id)}/comments?${pageQuery(page)}`, 'CommentResponsePage');
      if (value.items.some(item => item.requirementId !== id || item.isDeleted === 1 && item.content !== null)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    createComment: (id: string, input: C.CreateCommentRequest) => response<C.CommentResponse>('POST', `/requirements/${requireId(id)}/comments`, 'CommentResponse', commandContract('CreateCommentRequest', input), { key: 'requirementId', id }),
    deleteComment: (id: string) => response<void>('DELETE', `/comments/${requireId(id)}`),
    submitRequirement: (id: string, input: C.LockCommand) => response<C.RequirementReviewResponse>('POST', `/requirements/${requireId(id)}/submit`, 'RequirementReviewResponse', commandContract('LockCommand', input), { key: 'requirementId', id }),
    reopenRequirement: (id: string, input: C.LockCommand) => response<C.RequirementResponse>('POST', `/requirements/${requireId(id)}/reopen`, 'RequirementResponse', commandContract('LockCommand', input), { key: 'requirementId', id }),
    confirmVersion: (id: string, action: 'implement' | 'verify', input: C.VersionConfirmationRequest) => response<C.RequirementResponse>('POST', `/requirements/${requireId(id)}/${action}`, 'RequirementResponse', commandContract('VersionConfirmationRequest', input), { key: 'requirementId', id }),
    pendingReviews: (page: Pagination) => response<C.RequirementReviewResponsePage>('GET', `/reviews/pending?${pageQuery(page)}`, 'RequirementReviewResponsePage'),
    async reviews(id: string, page: Pagination) {
      const value = await response<C.RequirementReviewResponsePage>('GET', `/requirements/${requireId(id)}/reviews?${pageQuery(page)}`, 'RequirementReviewResponsePage');
      if (value.items.some(review => review.requirementId !== id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    async decideReview(id: string, requirementId: string, input: C.RequirementDecisionRequest) {
      requireId(requirementId);
      if (input.decision !== 'APPROVE' && (typeof input.comment !== 'string' || !/\S/u.test(input.comment))) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
      const value = await response<C.RequirementDecisionResponse>('POST', `/requirement-reviews/${requireId(id)}/decision`, 'RequirementDecisionResponse', commandContract('RequirementDecisionRequest', input));
      if (value.review.reviewId !== id || value.review.requirementId !== requirementId || value.requirement.requirementId !== requirementId || value.review.decision !== input.decision || value.version && (value.version.requirementId !== requirementId || value.version.versionId !== value.requirement.currentVersionId)) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    async versions(id: string, page: Pagination) {
      const value = await response<C.RequirementVersionResponsePage>('GET', `/requirements/${requireId(id)}/versions?${pageQuery(page)}`, 'RequirementVersionResponsePage');
      if (value.items.some(version => version.requirementId !== id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    async version(id: string, versionId: string) {
      const value = await response<C.RequirementVersionResponse>('GET', `/requirements/${requireId(id)}/versions/${requireId(versionId)}`, 'RequirementVersionResponse', undefined, { key: 'versionId', id: versionId });
      if (value.requirementId !== id) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    async changes(id: string, page: Pagination, status?: C.ChangeStatus) {
      if (status) commandContract('ChangeStatus', status);
      const value = await response<C.ChangeRequestResponsePage>('GET', `/requirements/${requireId(id)}/changes?${pageQuery(page)}${status ? `&status=${status}` : ''}`, 'ChangeRequestResponsePage');
      if (value.items.some(change => change.requirementId !== id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    createChange: (id: string, input: C.CreateChangeRequest) => response<C.ChangeRequestResponse>('POST', `/requirements/${requireId(id)}/changes`, 'ChangeRequestResponse', commandContract('CreateChangeRequest', input), { key: 'requirementId', id }),
    change: (id: string) => response<C.ChangeRequestResponse>('GET', `/changes/${requireId(id)}`, 'ChangeRequestResponse', undefined, { key: 'changeRequestId', id }),
    async editChange(id: string, requirementId: string, input: C.EditChangeRequest) {
      requireId(requirementId);
      const value = await response<C.ChangeRequestResponse>('PATCH', `/changes/${requireId(id)}`, 'ChangeRequestResponse', commandContract('EditChangeRequest', input), { key: 'changeRequestId', id });
      if (value.requirementId !== requirementId) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    async submitChange(id: string, requirementId: string, input: C.LockCommand) {
      requireId(requirementId);
      const value = await response<C.ChangeRequestReviewResponse>('POST', `/changes/${requireId(id)}/submit`, 'ChangeRequestReviewResponse', commandContract('LockCommand', input), { key: 'changeRequestId', id });
      if (value.requirementId !== requirementId) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    async cancelChange(id: string, requirementId: string, input: C.LockCommand) {
      requireId(requirementId);
      const value = await response<C.ChangeRequestResponse>('POST', `/changes/${requireId(id)}/cancel`, 'ChangeRequestResponse', commandContract('LockCommand', input), { key: 'changeRequestId', id });
      if (value.requirementId !== requirementId) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    async applyChange(id: string, requirementId: string, input: C.ApplyChangeRequest) {
      requireId(requirementId);
      const value = await response<C.ApplyChangeResponse>('POST', `/changes/${requireId(id)}/apply`, 'ApplyChangeResponse', commandContract('ApplyChangeRequest', input));
      if (value.change.changeRequestId !== id || value.change.requirementId !== requirementId || value.requirement.requirementId !== requirementId || value.version.requirementId !== requirementId || value.version.versionId !== value.requirement.currentVersionId || value.version.appliedChangeRequestId !== id) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    pendingChangeReviews: (page: Pagination) => response<C.ChangeRequestReviewResponsePage>('GET', `/change-reviews/pending?${pageQuery(page)}`, 'ChangeRequestReviewResponsePage'),
    async changeReviews(id: string, requirementId: string, page: Pagination) {
      requireId(requirementId);
      const value = await response<C.ChangeRequestReviewResponsePage>('GET', `/changes/${requireId(id)}/reviews?${pageQuery(page)}`, 'ChangeRequestReviewResponsePage');
      if (value.items.some(review => review.changeRequestId !== id || review.requirementId !== requirementId)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    async decideChangeReview(id: string, changeId: string, requirementId: string, input: C.ChangeDecisionRequest) {
      requireId(changeId); requireId(requirementId);
      if (input.decision !== 'APPROVE' && (typeof input.comment !== 'string' || !/\S/u.test(input.comment))) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
      const value = await response<C.ChangeDecisionResponse>('POST', `/change-reviews/${requireId(id)}/decision`, 'ChangeDecisionResponse', commandContract('ChangeDecisionRequest', input));
      if (value.review.changeReviewId !== id || value.review.changeRequestId !== changeId || value.review.requirementId !== requirementId || value.change.changeRequestId !== changeId || value.change.requirementId !== requirementId || value.review.decision !== input.decision) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    async relations(id: string, page: Pagination) {
      const value = await response<C.RequirementRelationResponsePage>('GET', `/requirements/${requireId(id)}/relations?${pageQuery(page)}`, 'RequirementRelationResponsePage');
      if (value.items.some(item => item.sourceRequirementId !== id && item.targetRequirementId !== id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    async createRelation(input: C.CreateRelationRequest) {
      const command = commandContract<C.CreateRelationRequest>('CreateRelationRequest', input);
      if (command.sourceRequirementId === command.targetRequirementId) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
      const value = await response<C.RequirementRelationResponse>('POST', '/relations', 'RequirementRelationResponse', command);
      const symmetric = ['CONFLICTS_WITH', 'DUPLICATES', 'RELATES_TO'].includes(command.relationType);
      const lowerSource = command.sourceRequirementId.length < command.targetRequirementId.length || command.sourceRequirementId.length === command.targetRequirementId.length && command.sourceRequirementId < command.targetRequirementId;
      const source = symmetric && !lowerSource ? command.targetRequirementId : command.sourceRequirementId;
      const target = symmetric && !lowerSource ? command.sourceRequirementId : command.targetRequirementId;
      if (value.sourceRequirementId !== source || value.targetRequirementId !== target || value.relationType !== command.relationType) throw new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, true);
      return value;
    },
    deleteRelation(id: string, input: RelationRevisions) {
      const query = new URLSearchParams({ expectedSourceLockVersion: commandContract<string>('LockVersion', input.expectedSourceLockVersion), expectedTargetLockVersion: commandContract<string>('LockVersion', input.expectedTargetLockVersion) });
      return response<void>('DELETE', `/relations/${requireId(id)}?${query}`);
    },
    async trace(id: string, input: TraceOptions) {
      if (!Number.isSafeInteger(input.depth) || input.depth < 1 || input.depth > 10) throw new ApiFailure('INVALID_INPUT', 'validation', 400);
      const query = new URLSearchParams({ depth: String(input.depth) }); if (input.relationType) query.set('relationType', commandContract('RelationType', input.relationType));
      const value = await response<C.TraceResponse>('GET', `/requirements/${requireId(id)}/trace?${query}`, 'TraceResponse');
      if (value.rootRequirementId !== id || value.depth !== input.depth || !value.nodes.some(node => node.requirementId === id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    withdrawRequirement: (id: string, input: C.LockCommand) => response<C.RequirementResponse>('POST', `/requirements/${requireId(id)}/withdraw`, 'RequirementResponse', commandContract('LockCommand', input), { key: 'requirementId', id }),
    async history(id: string, input: AuditFilters) {
      const value = await response<C.AuditResponsePage>('GET', `/requirements/${requireId(id)}/history?${auditQuery(input)}`, 'AuditResponsePage');
      if (value.items.some(item => item.requirementId !== id)) throw new ApiFailure('INVALID_RESPONSE', 'protocol');
      return value;
    },
    auditEvents: (input: AuditFilters) => response<C.AuditResponsePage>('GET', `/audit-events?${auditQuery(input)}`, 'AuditResponsePage'),
  };
}
export const business = createBusinessApi();
