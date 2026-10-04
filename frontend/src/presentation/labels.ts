/** Display labels only. API values, guards and state transitions use their original enums. */
const labels: Readonly<Record<string, string>> = {
  DRAFT: '草稿', UNDER_REVIEW: '待评审', APPROVED: '已批准', REJECTED: '已拒绝',
  IMPLEMENTED: '已实现', VERIFIED: '已验证', APPLIED: '已应用', CANCELLED: '已取消',
  BUSINESS: '业务需求', USER: '用户需求', SYSTEM: '系统需求', FUNCTIONAL: '功能需求',
  QUALITY: '质量需求', CONSTRAINT: '约束需求', LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '紧急',
  PENDING: '待评审', IN_PROGRESS: '进行中', COMPLETED: '已完成', APPROVE: '批准', REQUEST_CHANGES: '要求修改', REJECT: '拒绝',
};
export function displayLabel(value: string): string { return labels[value] ?? value; }
