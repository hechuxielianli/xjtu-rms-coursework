# 两套合法转换

需求撤销是独立逻辑标记，不增加生命周期状态。下列转换来自原业务定义。

## 需求

| 源 | 动作 | 目标 | 守卫 | 后果 |
| --- | --- | --- | --- | --- |
| DRAFT | 提交评审 | UNDER_REVIEW | 需求工程师；内容完整、未撤销 | 创建 PENDING 轮次及快照 |
| UNDER_REVIEW | 批准 | APPROVED | 评审者非创建者；轮次仍 PENDING | 完成轮次，创建 V1 并更新指针 |
| UNDER_REVIEW | 要求修改 | DRAFT | 评审者；意见必填 | 完成轮次，允许继续编辑 |
| UNDER_REVIEW | 拒绝 | REJECTED | 评审者；理由必填 | 完成轮次，保留内容 |
| REJECTED | 重新打开 | DRAFT | 需求工程师 | 保留评审及首次提交时间 |
| APPROVED | 标记已实现 | IMPLEMENTED | 项目成员；确认当前版本 | 记录实现说明与版本上下文 |
| IMPLEMENTED | 标记已验证 | VERIFIED | 项目成员；确认当前版本 | 记录验证说明与版本上下文 |
| APPROVED / IMPLEMENTED / VERIFIED | 应用批准变更 | APPROVED | 需求工程师；批准且基准一致 | 生成 V(n+1)，新版本待重新实现/验证 |

## 变更

| 源 | 动作 | 目标 | 后果/条件 |
| --- | --- | --- | --- |
| DRAFT | 提交 | UNDER_REVIEW | 内容完整，创建新评审快照 |
| DRAFT | 取消 | CANCELLED | 仅提出者；终态释放活动占用 |
| UNDER_REVIEW | 要求修改 | DRAFT | 意见必填，旧轮次结束 |
| UNDER_REVIEW | 批准 | APPROVED | 非提出者评审；不改变需求版本 |
| UNDER_REVIEW | 拒绝 | REJECTED | 理由必填；终态，不可再次提交 |
| APPROVED | 应用 | APPLIED | 基准一致；事务产生新版本并结束变更 |
