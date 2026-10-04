# 完整业务规则

97 条原业务规则，编号和文本保持不变。

| 编号 | 规则 |
| --- | --- |
| BR-AUTH-001 | 除登录接口外，所有受保护操作必须由已认证用户执行。 |
| BR-AUTH-002 | 被禁用用户不得登录，也不得继续执行受保护业务操作。 |
| BR-AUTH-003 | 用户只能执行其角色具有权限的操作。 |
| BR-AUTH-004 | 用户不得通过客户端提交角色或用户 ID 的方式绕过服务端权限校验。 |
| BR-USER-001 | 用户名和邮箱在系统内必须唯一；创建用户时服务端将 initialPassword 用 BCrypt 转为非空 passwordHash，不保存明文且哈希不返回客户端。 |
| BR-USER-002 | 一个用户可以拥有一个或多个预定义角色。 |
| BR-USER-003 | 运行期只有 Administrator 可以创建、禁用用户及分配角色；唯一受信任初始化例外由 migration/seed 创建 bootstrap Administrator 及其 ADMIN 分配，grantedBy 自指该用户，不能通过业务接口调用。 |
| BR-USER-004 | 当前课程版本采用预定义角色集合，Administrator 可以分配角色，但不允许动态创建新的 Role 类型。 |
| BR-REQ-001 | 每个 Requirement 必须具有唯一且不可修改的 requirementKey，例如 REQ-001。 |
| BR-REQ-002 | 创建需求须填写标题、描述、层级、性质及优先级；来源、理由和验收标准可在草稿阶段补齐。 |
| BR-REQ-003 | 新创建的 Requirement 初始状态必须为 DRAFT。 |
| BR-REQ-004 | 仅 DRAFT 状态允许直接编辑八项正式内容；已形成基线的受控更新必须通过应用批准变更。 |
| BR-REQ-005 | UNDER_REVIEW 状态下正式需求内容必须锁定。 |
| BR-REQ-006 | APPROVED、IMPLEMENTED、VERIFIED 状态的正式需求内容不得直接修改。 |
| BR-REQ-007 | 已形成基线的需求如需修改正式内容，必须通过 Change Request。 |
| BR-REQ-008 | 负责人、标签和当前关系不属于正式内容基线；权限允许时独立维护并记录审计。 |
| BR-REQ-009 | Requirement 的 creator、createdAt、requirementKey 创建后不得修改。 |
| BR-REQ-010 | 只允许逻辑撤销从未提交评审的草稿；有效关联必须先解除。 |
| BR-REQ-011 | 首次进入评审后，即使退回或重新打开为 DRAFT，也不得撤销需求。 |
| BR-REQ-012 | 已逻辑撤销的需求不再接受内容、状态、评论或关系修改；保留只读历史，默认列表不显示。 |
| BR-LIFE-001 | Requirement 只能按照定义的状态机发生状态转换。 |
| BR-LIFE-002 | 不允许客户端直接任意指定 Requirement.status。 |
| BR-LIFE-003 | DRAFT 只有在满足需求完整性检查后才能进入 UNDER_REVIEW。 |
| BR-LIFE-004 | UNDER_REVIEW 只能通过 APPROVE、REJECT 或 REQUEST_CHANGES 离开。 |
| BR-LIFE-005 | REQUEST_CHANGES 后 Requirement 返回 DRAFT。 |
| BR-LIFE-006 | REJECTED Requirement 可以由 Requirement Engineer 重新打开为 DRAFT。 |
| BR-LIFE-007 | 只有 APPROVED Requirement 可以进入 IMPLEMENTED。 |
| BR-LIFE-008 | 只有 IMPLEMENTED Requirement 可以进入 VERIFIED。 |
| BR-LIFE-009 | 不允许 DRAFT → APPROVED、APPROVED → VERIFIED 等跳跃式状态转换。 |
| BR-LIFE-010 | 对 IMPLEMENTED 或 VERIFIED Requirement 应用新的需求版本后，其状态必须重新变为 APPROVED。 |
| BR-REV-001 | 同一个 Requirement 在同一时间最多只能存在一个正在进行的 Review。 |
| BR-REV-002 | Review 只能针对 UNDER_REVIEW Requirement 进行。 |
| BR-REV-003 | 只有具有 Reviewer 权限的用户可以作出最终评审决定。 |
| BR-REV-004 | Requirement 的创建者不得评审自己创建的 Requirement。 |
| BR-REV-005 | REJECT 和 REQUEST_CHANGES 必须填写 review comment。 |
| BR-REV-006 | APPROVE 可以填写评审意见，但不是强制项。 |
| BR-REV-007 | 已完成的 Review 不允许修改或删除。 |
| BR-REV-008 | Requirement 每次重新提交评审，都必须创建新的 Review Record。 |
| BR-REV-009 | Requirement 在 UNDER_REVIEW 状态期间不得修改正式内容。 |
| BR-VER-001 | 首次批准前的 Requirement 不具有正式 Baseline Version。 |
| BR-VER-002 | Requirement 第一次被 APPROVE 时必须生成 Version 1。 |
| BR-VER-003 | 后续正式版本只能通过已批准的 Change Request 创建。 |
| BR-VER-004 | RequirementVersion 创建后必须不可修改。 |
| BR-VER-005 | 每个版本必须保存完整需求内容快照，而不仅是差异。 |
| BR-VER-006 | 版本号必须严格递增，不允许重复或跳号。 |
| BR-VER-007 | 版本保留八项完整内容、产生者、时间、理由及唯一产生来源：首次批准评审或已应用变更。 |
| BR-VER-008 | Requirement 当前正式内容必须与最新 RequirementVersion 内容一致。 |
| BR-VER-009 | 历史 RequirementVersion 不允许删除。 |
| BR-REL-001 | Requirement 不得与自身建立关系。 |
| BR-REL-002 | Relation 两端 Requirement 必须真实存在且未被删除。 |
| BR-REL-003 | DEPENDS_ON、REFINES、DERIVED_FROM 为有向关系。 |
| BR-REL-004 | CONFLICTS_WITH、DUPLICATES、RELATES_TO 为对称关系。 |
| BR-REL-005 | 同一 Requirement Pair 不允许存在完全重复的关系。 |
| BR-REL-006 | 对称关系不得同时保存 A→B 和 B→A 两份重复数据。 |
| BR-REL-007 | DEPENDS_ON 不允许形成循环依赖。 |
| BR-REL-008 | 删除 RequirementRelation 不得影响 Relation 两端的 Requirement。 |
| BR-REL-009 | 创建或删除 RequirementRelation 应保留审计记录。 |
| BR-CR-001 | Change Request 只能针对已经形成正式版本的 Requirement 创建。 |
| BR-CR-002 | APPROVED、IMPLEMENTED、VERIFIED Requirement 均允许提出 Change Request。 |
| BR-CR-003 | Change Request 创建时必须记录当前 RequirementVersion 作为 baseVersion。 |
| BR-CR-004 | Change Request 必须描述 change reason 和 proposed requirement content。 |
| BR-CR-005 | 同一需求最多存在一个活动变更：DRAFT、UNDER_REVIEW、APPROVED；终态释放活动占用。 |
| BR-CR-006 | 只有 DRAFT Change Request 可以编辑。 |
| BR-CR-007 | Change Request 提交后正式变更内容必须锁定。 |
| BR-CR-008 | REQUEST_CHANGES 后 Change Request 返回 DRAFT。 |
| BR-CR-009 | REJECT 和 REQUEST_CHANGES 必须填写评审意见。 |
| BR-CR-010 | APPROVED Change Request 不允许再修改。 |
| BR-CR-011 | Apply Change Request 前必须检查 baseVersion == Requirement.currentVersion。 |
| BR-CR-012 | 若 baseVersion 已过期，则禁止 Apply。 |
| BR-CR-013 | Apply 操作必须在同一个数据库事务中完成需求更新和版本创建。 |
| BR-CR-014 | 成功应用时产生的 versionNo 必须等于该需求当前正式版本的 versionNo 加 1，而不是对版本内部 ID 做算术运算。 |
| BR-CR-015 | Apply 成功后 Change Request 状态必须变为 APPLIED。 |
| BR-CR-016 | 若原 Requirement 为 IMPLEMENTED 或 VERIFIED，Apply 后必须回退到 APPROVED。 |
| BR-CR-017 | REJECTED Change Request 不允许再次提交。 |
| BR-CR-018 | DRAFT Change Request 可由创建者取消。 |
| BR-COM-001 | Comment 不属于正式 Requirement Content。 |
| BR-COM-002 | Comment 不会改变 Requirement 状态或版本。 |
| BR-COM-003 | Comment 必须记录作者和创建时间。 |
| BR-COM-004 | 已发布 Comment 不允许修改作者或创建时间。 |
| BR-COM-005 | 删除 Comment 应采用逻辑删除。 |
| BR-COM-006 | Viewer 仅具有查看权限，不允许发表评论。 |
| BR-AUDIT-001 | 用户及角色维护、需求内容和管理信息、状态、版本、关系、评审、变更与评论操作均产生可追踪审计。 |
| BR-AUDIT-002 | Audit Record 创建后不得被普通用户修改或删除。 |
| BR-AUDIT-003 | 操作者身份必须由当前登录会话确定，不接受客户端自行指定。 |
| BR-AUDIT-004 | 业务时间必须由服务端生成。 |
| BR-AUDIT-005 | Review Record 和 RequirementVersion 本身也是需求历史的重要组成部分。 |
| BR-REQ-013 | 提交需求或变更评审前，八项正式内容全部非空且验收标准可判定。 |
| BR-REV-010 | 每次提交创建不可变内容快照；完成的评审决策和意见不得覆盖。 |
| BR-CR-019 | 变更提出者不得评审自己提出的变更；多角色叠加不解除此限制。 |
| BR-CR-020 | 每次提交 Change Request 评审时，系统必须从该 ChangeRequest 本身复制不可变快照。snapshotBaseVersionId 必须等于 ChangeRequest.baseVersionId；snapshotRequestTitle、snapshotReason 以及全部 snapshotProposed* 字段必须等于提交瞬间 ChangeRequest 中对应字段。快照创建后不得修改。 |
| BR-CR-021 | 基准版本、评审上下文和产生版本必须与对应变更属于同一需求。 |
| BR-CR-022 | 批准和应用必须一次性执行；重复请求不产生第二条决策或第二个版本。 |
| BR-VER-010 | V1 唯一来源为批准的需求评审；V2 起唯一来源为已应用变更；两种来源互斥。 |
| BR-VER-011 | 当前版本指针只能引用本需求版本；形成基线后指向最新版本。 |
| BR-REL-010 | 关系写操作串行化；DEPENDS_ON 的环检测与写入须处于同一受保护事务。 |
| BR-USER-005 | 启用用户至少具有一个预定义角色；撤销角色不得留下无角色的启用账号。 |
| BR-AUDIT-006 | 成功业务变更和成功审计在同一事务提交；失败及拒绝审计独立记录，不伪造成功历史。 |
