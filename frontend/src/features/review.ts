import type { CreateRequirementRequest, RequirementResponse, RequirementReviewResponse } from '../api/contracts.generated';
import type { useSession } from '../auth/useSession';
export function completeContent(record: RequirementResponse): boolean {
  return [record.title, record.description, record.level, record.kind, record.priority, record.source, record.rationale, record.acceptanceCriteria].every(value => typeof value === 'string' && /\S/u.test(value));
}
export function canDecide(session: ReturnType<typeof useSession>, record: RequirementResponse, review: RequirementReviewResponse): boolean {
  return session.hasAnyRole(['REVIEWER']) && record.isWithdrawn === 0 && record.status === 'UNDER_REVIEW' && review.reviewStatus === 'PENDING' && review.requirementId === record.requirementId && record.creatorId !== session.identity?.user.userId;
}
export function snapshotContent(review: RequirementReviewResponse): CreateRequirementRequest {
  return { title: review.snapshotTitle, description: review.snapshotDescription, level: review.snapshotLevel, kind: review.snapshotKind, priority: review.snapshotPriority, source: review.snapshotSource, rationale: review.snapshotRationale, acceptanceCriteria: review.snapshotAcceptanceCriteria };
}
