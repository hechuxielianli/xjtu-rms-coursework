import type { CreateRelationRequest, RelationType, RequirementRelationResponse, RequirementResponse, TraceResponse } from '../api/contracts.generated';
import type { useSession } from '../auth/useSession';
export const RELATION_TYPES = ['DEPENDS_ON', 'REFINES', 'DERIVED_FROM', 'CONFLICTS_WITH', 'DUPLICATES', 'RELATES_TO'] as const;
export function symmetricRelation(type: RelationType): boolean { return ['CONFLICTS_WITH', 'DUPLICATES', 'RELATES_TO'].includes(type); }
export function relationDirection(relation: RequirementRelationResponse, parentId: string): string { return symmetricRelation(relation.relationType) ? '对称' : relation.sourceRequirementId === parentId ? '出向' : '入向'; }
export function relationCommand(parent: RequirementResponse, other: RequirementResponse, type: RelationType, incoming: boolean, description: string): CreateRelationRequest {
  const source = incoming ? other : parent; const target = incoming ? parent : other;
  return { sourceRequirementId: source.requirementId, targetRequirementId: target.requirementId, relationType: type, description: description === '' ? null : description, expectedSourceLockVersion: source.lockVersion, expectedTargetLockVersion: target.lockVersion };
}
export function canManageRelations(session: ReturnType<typeof useSession>, parent: RequirementResponse): boolean { return session.hasAnyRole(['REQUIREMENT_ENGINEER']) && parent.isWithdrawn === 0; }
export function canWithdrawDraft(session: ReturnType<typeof useSession>, parent: RequirementResponse): boolean { return canManageRelations(session, parent) && parent.status === 'DRAFT' && parent.firstSubmittedAt === null; }
export function traceProjection(trace: TraceResponse): TraceResponse { return { ...trace, nodes: [...new Map(trace.nodes.map(node => [node.requirementId, node])).values()], edges: [...new Map(trace.edges.map(edge => [edge.relationId, edge])).values()] }; }
