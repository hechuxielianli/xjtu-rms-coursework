# 测试范围与实际结果

原设计 103 项，本次两版实际执行相同 32 个代表性 ID。V1 为 31 PASS / 1 FAIL / 0 BLOCKED，V2 为 32 PASS / 0 FAIL / 0 BLOCKED。其余 71 项 NOT_RUN；已选场景内尚未覆盖的变体不算通过。

| ID | 说明 | V1 | V2 |
| --- | --- | --- | --- |
| T-AUTH-001 | 启用账号按用户名和邮箱登录 | FAIL | PASS |
| T-AUTH-002 | 错误凭证与禁用登录 | PASS | PASS |
| T-AUTH-004 | 禁用和角色变更即时影响既有会话 | PASS | PASS |
| T-AUTH-005 | 客户端身份和角色声明不可信 | PASS | PASS |
| T-USER-006 | ADMIN 本身不隐含内容或审批权 | PASS | PASS |
| T-REV-004 | 需求创建者自我评审严格排除 | PASS | PASS |
| T-CR-009 | 变更提出者自我评审严格排除 | PASS | PASS |
| T-LIFE-001 | 提交原子建立需求评审轮次 | PASS | PASS |
| T-LIFE-002 | 实现绑定当前正式版本 | PASS | PASS |
| T-LIFE-003 | 验证仅允许已实现且当前版本 | PASS | PASS |
| T-LIFE-004 | 拒绝需求重开保留历史 | PASS | PASS |
| T-REV-002 | 首次批准唯一产生 V1 | PASS | PASS |
| T-REV-003 | 拒绝和退回意见必填 | PASS | PASS |
| T-REV-006 | 退回重提的快照轮次隔离 | PASS | PASS |
| T-CR-001 | 从三个基线状态创建变更 | PASS | PASS |
| T-CR-005 | 每次提交复制 CR 本身全部十一项快照 | PASS | PASS |
| T-CR-007 | 批准 CR 与 Apply 严格分离 | PASS | PASS |
| T-APPLY-001 | V2 应用原子且重置待实现验证 | PASS | PASS |
| T-APPLY-002 | 过期基准拒绝 | PASS | PASS |
| T-APPLY-003 | 重复 Apply 一次性与序号不跳 | PASS | PASS |
| T-CON-001 | 并发唯一活动 CR | PASS | PASS |
| T-CON-004 | 两类评审同时决策只完成一次 | PASS | PASS |
| T-CON-005 | 并发 Apply 只产生一个新版本 | PASS | PASS |
| T-REL-004 | 两点三点 DEPENDS_ON 依赖环 | PASS | PASS |
| T-TX-002 | Apply 每步故障与重试一致 | PASS | PASS |
| T-REQ-004 | 稳定标识与可信字段输入边界 | PASS | PASS |
| T-REST-002 | Mass assignment 信任字段系统性负例 | PASS | PASS |
| T-SEC-001 | 全链路凭证泄漏检查 | PASS | PASS |
| T-REV-005 | 重复决策与完成评审保护 | PASS | PASS |
| T-CR-011 | 重复 CR 决策不覆盖 | PASS | PASS |
| T-E2E-001 | 完整正常链至 V2 重新验证 | PASS | PASS |
| T-E2E-002 | 拒绝重开与 CR 拒绝取消分支 | PASS | PASS |

## 未运行原 ID

T-AUTH-003, T-AUTH-006, T-USER-001, T-USER-002, T-USER-003, T-USER-004, T-USER-005, T-BOOT-001, T-REQ-001, T-REQ-002, T-REQ-003, T-REQ-005, T-REQ-006, T-REQ-007, T-REQ-008, T-LIFE-005, T-REV-001, T-VER-001, T-VER-002, T-VER-003, T-REL-001, T-REL-002, T-REL-003, T-REL-005, T-REL-006, T-REL-007, T-REL-008, T-REL-009, T-CR-002, T-CR-003, T-CR-004, T-CR-006, T-CR-008, T-CR-010, T-APPLY-004, T-APPLY-005, T-TX-001, T-TX-003, T-TX-004, T-TX-005, T-CON-002, T-CON-003, T-CON-006, T-CON-007, T-CON-008, T-DB-001, T-DB-002, T-DB-003, T-DB-004, T-DB-005, T-COM-001, T-COM-002, T-COM-003, T-COM-004, T-SEARCH-001, T-SEARCH-002, T-SEARCH-003, T-AUDIT-001, T-AUDIT-002, T-AUDIT-003, T-AUDIT-004, T-REST-001, T-REST-003, T-REST-004, T-E2E-003, T-E2E-004, T-NFR-001, T-NFR-002, T-ARCH-001, T-AUTH-007, T-REST-005

## 部分场景的范围

```text
[
  "PASS describes only the disclosed representative scope; PARTIAL original specifications were not executed in full.",
  "No production HTTPS, load testing, penetration testing or all-103 automation.",
  "Apply faults were injected at four writes; a distinct flush/commit transport fault was NOT_RUN.",
  "Mass-assignment probes cover four requirement-related request types plus five additional DTOs, rather than the whole original matrix.",
  "Secret checks compare known test passwords and BCrypt patterns across captured responses, fixture audits and the actual server log; not an exhaustive security audit.",
  "Two supplemental protocol failures are independent observations, not additional original test IDs.",
  "Two supplemental protocol failures are independent observations, not additional original test IDs."
]
```

## 另外的检查

开发检查与上述独立代表场景分开计数。V2 后端 72 个 JUnit（其中 27 个定向新增），前端 152 项检查均通过；当前展示层修改后的安装、构建、152 项前端检查和两条原 E2E 也通过。协议、认证边界和查询计数为补充验证，不扩大原 32 项计数。查询次数不等于测得生产时延。

原始运行材料保存在私人历史中，本公开仓库只提供足够理解结果的摘要和可阅读源码，不发布 Cookie、数据库、执行原始日志或数百份 XML。历史比较不能仅靠当前 V2 源码重新生成 V1。
