# 数据模型和系统设计

## 两层 ER 表达

报告正文使用**核心 ER 图**，说明实体为什么存在、彼此怎样关联。完整字段级图信息量较大，作为仓库补充材料供逐项核对；两层表达保持可读性和完整性，业务模型没有改变。

| 表达层次 | PDF | SVG | PNG 预览 | 用途 |
|---|---|---|---|---|
| 核心 ER | [core-er.pdf](core-er.pdf) | [core-er.svg](core-er.svg) | [core-er-preview.png](core-er-preview.png) | 课程报告正文 |
| 完整字段级 ER | [complete-er.pdf](complete-er.pdf) | [complete-er.svg](complete-er.svg) | [complete-er-preview.png](complete-er-preview.png) | 完整字段和约束核对 |

[RMS_Presentation_Models.vsdx](RMS_Presentation_Models.vsdx) 是原生可编辑 Visio，包含 `complete-er` 与 `core-er` 两页。实体为组合表框，字段为原生文字，连接线为两端粘连的 1D 图形；可以分别编辑布局和文字。完整页及已有完整图导出文件保留原样，核心页新增。

## 核心图怎样选择字段

核心图共 13 个实体、96 行字段。每个实体保留全部主键和外键列，再选择身份、标题、分类、状态、版本号、轮次、基准、关系类型、决策和必要协作信息。大部分实体为 7–10 行；Role、UserRole、Tag、RequirementTag 原本只有 4–5 个字段，因此直接全部保留，没有填入虚构字段。

两类评审快照和变更建议内容各保留标题作为代表；其余正式内容仍在完整模型中。核心图不承担数据字典的工作，列类型、唯一键组、生成列、默认值等实现细节在完整图和 CSV 中查阅。省略展示不等于删除字段，完整八项正式内容仍然保存。

| 实体 | 行数 | 核心图保留字段 |
|---|---:|---|
| User | 7 | `userId`, `username`, `email`, `passwordHash`, `displayName`, `accountStatus`, `createdAt` |
| UserRole | 4 | `userId`, `roleId`, `grantedBy`, `grantedAt` |
| Role | 4 | `roleId`, `roleCode`, `roleName`, `description` |
| Tag | 5 | `tagId`, `name`, `description`, `createdBy`, `createdAt` |
| Requirement | 10 | `requirementId`, `requirementKey`, `title`, `level`, `kind`, `status`, `creatorId`, `assigneeId`, `currentVersionId`, `withdrawnBy` |
| RequirementTag | 4 | `requirementId`, `tagId`, `addedBy`, `addedAt` |
| RequirementRelation | 7 | `relationId`, `sourceRequirementId`, `targetRequirementId`, `relationType`, `description`, `createdBy`, `createdAt` |
| RequirementVersion | 9 | `versionId`, `requirementId`, `versionNo`, `title`, `initialReviewId`, `appliedChangeRequestId`, `createdBy`, `createdAt`, `changeReason` |
| RequirementReview | 10 | `reviewId`, `requirementId`, `roundNo`, `snapshotTitle`, `submittedBy`, `submittedAt`, `reviewStatus`, `reviewerId`, `decision`, `comment` |
| Comment | 8 | `commentId`, `requirementId`, `authorId`, `content`, `createdAt`, `isDeleted`, `deletedBy`, `deletedAt` |
| ChangeRequest | 10 | `changeRequestId`, `requirementId`, `baseVersionId`, `requestTitle`, `reason`, `proposedTitle`, `status`, `createdBy`, `appliedBy`, `lockVersion` |
| ChangeRequestReview | 10 | `changeReviewId`, `requirementId`, `changeRequestId`, `roundNo`, `snapshotBaseVersionId`, `snapshotProposedTitle`, `submittedBy`, `reviewStatus`, `reviewerId`, `decision` |
| AuditEvent | 8 | `auditId`, `actorId`, `requirementId`, `targetType`, `targetId`, `action`, `outcome`, `occurredAt` |

## 核心图怎样表达关系

核心图逐条画出版本归属与当前指针、需求评审、首次批准、变更归属与基准、应用产生版本、变更评审及其基准快照、源/目标需求关系、标签关联、评论、用户角色、需求创建者/负责人，以及审计的需求上下文。审计主体和其余重复人员引用使用 **`FK·U`** 标记，表示引用 `User.userId`，减少跨越整张图的重复连线。

核心图实际展开 19 条外键关系：R01, R02, R04, R05, R07, R08, R09, R12, R13, R15, R16, R19, R20, R23, R24, R27, R28, R30, R34

以下 15 条人员外键通过 `FK·U` 字段表达，未逐条画线：R03, R06, R10, R11, R14, R17, R18, R21, R22, R25, R26, R29, R31, R32, R33

这些编号对应 [foreign-keys.csv](foreign-keys.csv)；完整图仍逐条展开全部 34 个真实外键。`RequirementRelation` 的源端和目标端分别连线，当前版本与版本归属分别连线，不能把不同约束合并成一条含混关系。

PK 表示主键，FK 表示外键，`?` 表示可空。Crow’s Foot 两端的圆圈、短线、三叉分别表示 0、1、多个；实线表示标识关系，虚线表示非标识关系。完整图中的 UK 编号表示**同一实体内的一组唯一键**，相同编号的多列共同唯一；G 表示数据库生成的约束字段。两张图都使用原始字段名，没有用重复引用框扩充实体数。

布局采用用户与权限在左、需求居中、版本/评审/变更在右、辅助协作在下、审计在外围。白底、深灰边框、浅灰标题栏以及正交连线让读者先辨认业务结构，再按需查阅详细约束。

## 完整设计资料

- [151 字段数据字典](data-dictionary.csv)：类型、含义、主外键、可空性、默认值、域与修改条件。
- [34 外键](foreign-keys.csv)：复合列、两端基数和标识关系。
- [状态转换表](state-transitions.md)、[需求状态](requirement-state.pdf)、[变更状态](change-request-state.pdf)。
- [系统架构](architecture.pdf)、[多智能体协作流程](multi-agent-workflow.pdf)。
- [完整接口定义](openapi.yaml)。

外键是真实的结构约束；权限、自我评审、连续版本号和依赖环还需要业务服务保证。审计 `targetType/targetId` 只是逻辑目标定位，没有伪造到所有实体的外键。当前内容、正式版本和提交快照有意保存副本，副本对应不同时间和业务含义，不能宣称模型完全没有冗余。

当前负责人、标签和需求关系的历史通过审计追踪，正式版本只保存八项正式内容。批准变更与应用变更独立，应用新版本后需求回到 APPROVED，再重新确认实现和验证。
