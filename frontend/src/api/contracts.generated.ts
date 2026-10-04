// Generated from docs/contracts/openapi.yaml. Do not edit manually.
// Source SHA256: d0a50639679c8ca38ccde5a16d1f9836fcb2b12ceeafb92ee90df9e8da6284ba
// Runtime boundary checks supplement these transport types.
export type Id = string;

export type LockVersion = string;

export type RoleCode = "ADMIN" | "REQUIREMENT_ENGINEER" | "REVIEWER" | "PROJECT_MEMBER" | "VIEWER";

export type UserSummary = {
  userId: Id;
  displayName: string;
  accountStatus: "ENABLED" | "DISABLED";
};

export type SessionResponse = {
  user: UserSummary;
  roles: Array<RoleCode>;
};

export type CsrfResponse = {
  headerName: "X-CSRF-TOKEN";
  token: string;
};

export type LoginRequest = {
  login: string;
  password: string;
};

export type ApiError = {
  code: "INVALID_INPUT" | "INCOMPLETE_CONTENT" | "UNAUTHENTICATED" | "INVALID_CREDENTIALS" | "FORBIDDEN" | "ACCOUNT_DISABLED" | "SELF_REVIEW" | "NOT_AUTHOR" | "NOT_FOUND" | "LOCK_VERSION_CONFLICT" | "STATE_CONFLICT" | "BASE_VERSION_STALE" | "DUPLICATE" | "ACTIVE_CHANGE_EXISTS" | "ACTIVE_REVIEW_EXISTS" | "DEPENDENCY_CYCLE" | "GRAPH_BUSY" | "INTERNAL_ERROR";
  message: string;
  correlationId: string;
  currentLockVersion?: LockVersion;
};

export type RequirementLevel = "BUSINESS" | "USER" | "SYSTEM";

export type RequirementKind = "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";

export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type RequirementStatus = "DRAFT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "IMPLEMENTED" | "VERIFIED";

export type RequirementResponse = {
  requirementId: string;
  requirementKey: string;
  title: string;
  description: string;
  level: "BUSINESS" | "USER" | "SYSTEM";
  kind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  source: string | null;
  rationale: string | null;
  acceptanceCriteria: string | null;
  status: "DRAFT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "IMPLEMENTED" | "VERIFIED";
  creatorId: string;
  assigneeId: string | null;
  currentVersionId: string | null;
  firstSubmittedAt: string | null;
  isWithdrawn: 0 | 1;
  withdrawnBy: string | null;
  withdrawnAt: string | null;
  createdAt: string;
  updatedAt: string;
  lockVersion: string;
  creator: UserSummary;
  assignee: UserSummary | null;
  tags: Array<TagResponse>;
};

export type RequirementResponsePage = {
  items: Array<RequirementResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateRequirementRequest = {
  title: string;
  description: string;
  level: "BUSINESS" | "USER" | "SYSTEM";
  kind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  source?: string | null;
  rationale?: string | null;
  acceptanceCriteria?: string | null;
};

export type EditRequirementContentRequest = {
  title?: string;
  description?: string;
  level?: "BUSINESS" | "USER" | "SYSTEM";
  kind?: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  priority?: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  source?: string | null;
  rationale?: string | null;
  acceptanceCriteria?: string | null;
  expectedLockVersion: LockVersion;
};

export type EditMetadataRequest = {
  assigneeId?: string | null;
  tagIds?: Array<Id>;
  expectedLockVersion: LockVersion;
};

export type TagResponse = {
  tagId: string;
  name: string;
  description: string | null;
  createdBy: string;
  createdAt: string;
};

export type TagResponsePage = {
  items: Array<TagResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateTagRequest = {
  name: string;
  description?: string | null;
};

export type EditTagRequest = {
  name?: string;
  description?: string | null;
};

export type CommentResponse = {
  commentId: string;
  requirementId: string;
  authorId: string;
  content: string | null;
  createdAt: string;
  isDeleted: 0 | 1;
  deletedBy: string | null;
  deletedAt: string | null;
};

export type CommentResponsePage = {
  items: Array<CommentResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateCommentRequest = {
  content: string;
};

export type AdminUserResponse = {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  accountStatus: "ENABLED" | "DISABLED";
  createdAt: string;
  updatedAt: string;
  lockVersion: string;
  roles: Array<RoleCode>;
  assignments: Array<UserRoleResponse>;
};

export type AdminUserResponsePage = {
  items: Array<AdminUserResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateUserRequest = {
  username: string;
  email: string;
  displayName: string;
  initialPassword: string;
  roles: Array<RoleCode>;
};

export type EditUserRequest = {
  username?: string;
  email?: string;
  displayName?: string;
  expectedLockVersion: LockVersion;
};

export type SetRolesRequest = {
  roles: Array<RoleCode>;
  expectedLockVersion: LockVersion;
};

export type LockCommand = {
  expectedLockVersion: LockVersion;
};

export type RequirementVersionResponse = {
  versionId: string;
  requirementId: string;
  versionNo: number;
  title: string;
  description: string;
  level: "BUSINESS" | "USER" | "SYSTEM";
  kind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  source: string;
  rationale: string;
  acceptanceCriteria: string;
  initialReviewId: string | null;
  appliedChangeRequestId: string | null;
  createdBy: string;
  createdAt: string;
  changeReason: string;
};

export type RequirementVersionResponsePage = {
  items: Array<RequirementVersionResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type RequirementReviewResponse = {
  reviewId: string;
  requirementId: string;
  roundNo: number;
  snapshotTitle: string;
  snapshotDescription: string;
  snapshotLevel: "BUSINESS" | "USER" | "SYSTEM";
  snapshotKind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  snapshotPriority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  snapshotSource: string;
  snapshotRationale: string;
  snapshotAcceptanceCriteria: string;
  submittedBy: string;
  submittedAt: string;
  reviewStatus: "PENDING" | "COMPLETED";
  reviewerId: string | null;
  decision: "APPROVE" | "REJECT" | "REQUEST_CHANGES" | null;
  comment: string | null;
  decidedAt: string | null;
};

export type RequirementReviewResponsePage = {
  items: Array<RequirementReviewResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type RequirementDecisionRequest = {
  decision: Decision;
  comment?: string | null;
  expectedRequirementLockVersion: LockVersion;
};

export type RequirementDecisionResponse = {
  review: RequirementReviewResponse;
  requirement: RequirementResponse;
  version: RequirementVersionResponse | null;
};

export type VersionConfirmationRequest = {
  expectedVersionId: Id;
  expectedLockVersion: LockVersion;
  description: string;
};

export type ChangeStatus = "DRAFT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "APPLIED" | "CANCELLED";

export type ChangeRequestResponse = {
  changeRequestId: string;
  requirementId: string;
  baseVersionId: string;
  requestTitle: string;
  reason: string;
  proposedTitle: string;
  proposedDescription: string;
  proposedLevel: "BUSINESS" | "USER" | "SYSTEM";
  proposedKind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  proposedPriority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  proposedSource: string | null;
  proposedRationale: string | null;
  proposedAcceptanceCriteria: string | null;
  status: "DRAFT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "APPLIED" | "CANCELLED";
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  appliedBy: string | null;
  appliedAt: string | null;
  lockVersion: string;
};

export type ChangeRequestResponsePage = {
  items: Array<ChangeRequestResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type ChangeRequestReviewResponse = {
  changeReviewId: string;
  requirementId: string;
  changeRequestId: string;
  roundNo: number;
  snapshotBaseVersionId: string;
  snapshotRequestTitle: string;
  snapshotReason: string;
  snapshotProposedTitle: string;
  snapshotProposedDescription: string;
  snapshotProposedLevel: "BUSINESS" | "USER" | "SYSTEM";
  snapshotProposedKind: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  snapshotProposedPriority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  snapshotProposedSource: string;
  snapshotProposedRationale: string;
  snapshotProposedAcceptanceCriteria: string;
  submittedBy: string;
  submittedAt: string;
  reviewStatus: "PENDING" | "COMPLETED";
  reviewerId: string | null;
  decision: "APPROVE" | "REJECT" | "REQUEST_CHANGES" | null;
  comment: string | null;
  decidedAt: string | null;
};

export type ChangeRequestReviewResponsePage = {
  items: Array<ChangeRequestReviewResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateChangeRequest = {
  requestTitle: string;
  reason: string;
  expectedRequirementLockVersion: LockVersion;
};

export type EditChangeRequest = {
  requestTitle?: string;
  reason?: string;
  proposedTitle?: string;
  proposedDescription?: string;
  proposedLevel?: "BUSINESS" | "USER" | "SYSTEM";
  proposedKind?: "FUNCTIONAL" | "QUALITY" | "CONSTRAINT";
  proposedPriority?: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  proposedSource?: string | null;
  proposedRationale?: string | null;
  proposedAcceptanceCriteria?: string | null;
  expectedLockVersion: LockVersion;
};

export type ChangeDecisionRequest = {
  decision: Decision;
  comment?: string | null;
  expectedChangeLockVersion: LockVersion;
};

export type ChangeDecisionResponse = {
  review: ChangeRequestReviewResponse;
  change: ChangeRequestResponse;
};

export type ApplyChangeRequest = {
  expectedRequirementLockVersion: LockVersion;
  expectedChangeLockVersion: LockVersion;
};

export type ApplyChangeResponse = {
  change: ChangeRequestResponse;
  requirement: RequirementResponse;
  version: RequirementVersionResponse;
};

export type RelationType = "DEPENDS_ON" | "REFINES" | "DERIVED_FROM" | "CONFLICTS_WITH" | "DUPLICATES" | "RELATES_TO";

export type RequirementRelationResponse = {
  relationId: string;
  sourceRequirementId: string;
  targetRequirementId: string;
  relationType: "DEPENDS_ON" | "REFINES" | "DERIVED_FROM" | "CONFLICTS_WITH" | "DUPLICATES" | "RELATES_TO";
  description: string | null;
  createdBy: string;
  createdAt: string;
};

export type RequirementRelationResponsePage = {
  items: Array<RequirementRelationResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type CreateRelationRequest = {
  sourceRequirementId: Id;
  targetRequirementId: Id;
  relationType: RelationType;
  description?: string | null;
  expectedSourceLockVersion: LockVersion;
  expectedTargetLockVersion: LockVersion;
};

export type TraceResponse = {
  rootRequirementId: Id;
  nodes: Array<RequirementResponse>;
  edges: Array<RequirementRelationResponse>;
  depth: number;
  truncated: boolean;
};

export type ReviewStatus = "PENDING" | "COMPLETED";

export type RequirementTagResponse = {
  requirementId: string;
  tagId: string;
  addedBy: string;
  addedAt: string;
};

export type AuditResponse = {
  auditId: string;
  actorId: string;
  requirementId: string | null;
  targetType: "USER" | "ROLE_ASSIGNMENT" | "REQUIREMENT" | "REVIEW" | "VERSION" | "RELATION" | "CHANGE_REQUEST" | "CHANGE_REVIEW" | "TAG_ASSIGNMENT" | "TAG" | "COMMENT";
  targetId: string;
  action: string;
  outcome: "SUCCESS" | "DENIED" | "FAILED";
  beforeData: Record<string, unknown> | null;
  afterData: Record<string, unknown> | null;
  occurredAt: string;
};

export type AuditResponsePage = {
  items: Array<AuditResponse>;
  page: number;
  size: number;
  totalElements: number;
};

export type UserRoleResponse = {
  userId: string;
  roleId: string;
  grantedBy: string;
  grantedAt: string;
};

export type Decision = "APPROVE" | "REJECT" | "REQUEST_CHANGES";
