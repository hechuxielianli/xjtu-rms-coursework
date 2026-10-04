# 需求到设计的对应

用于复核 64 条需求的用例、业务规则、实体与验收场景；不代表每个验收场景均已运行。

| 需求 | 用例 | 规则 | 实体 | 验收场景 |
| --- | --- | --- | --- | --- |
| FR-AUTH-001 | UC-01 | BR-AUTH-001, BR-AUTH-002, BR-AUTH-004 | User, Role, UserRole | AC-01 |
| FR-AUTH-002 | UC-01 | BR-AUTH-001, BR-AUTH-002 | User, Role, UserRole | AC-01 |
| FR-AUTH-003 | UC-01 | BR-AUTH-001, BR-AUTH-002, BR-AUTH-003, BR-AUTH-004 | User, Role, UserRole | AC-01 |
| FR-USER-001 | UC-02 | BR-USER-001, BR-USER-003, BR-USER-005, BR-AUDIT-001, BR-AUDIT-004 | User, Role, UserRole | AC-01 |
| FR-USER-002 | UC-02 | BR-USER-001, BR-USER-003 | User, Role, UserRole | AC-01 |
| FR-USER-003 | UC-02 | BR-AUTH-002, BR-USER-003 | User, Role, UserRole | AC-01 |
| FR-USER-004 | UC-02 | BR-AUTH-003, BR-USER-003 | User, Role, UserRole | AC-01 |
| FR-USER-005 | UC-02 | BR-USER-002, BR-USER-003, BR-USER-004, BR-USER-005 | User, Role, UserRole | AC-01 |
| FR-USER-006 | UC-02 | BR-AUTH-003, BR-AUTH-004, BR-CR-019, BR-REV-004 | User, Role, UserRole | AC-01 |
| FR-REQ-001 | UC-03 | BR-REQ-001, BR-REQ-002, BR-REQ-003, BR-REQ-009 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-002 | UC-03, UC-06 | BR-AUTH-003, BR-REQ-012 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-003 | UC-03 | BR-REQ-004, BR-REQ-005, BR-REQ-006, BR-REQ-007 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-004 | UC-03 | BR-REQ-010, BR-REQ-011, BR-REQ-012 | Requirement, Tag, RequirementTag | AC-12 |
| FR-REQ-005 | UC-03 | BR-REQ-002, BR-REQ-004, BR-REQ-013 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-006 | UC-03 | BR-REQ-004, BR-REQ-006, BR-REQ-007 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-007 | UC-03 | BR-REQ-008, BR-AUDIT-001 | Requirement, Tag, RequirementTag | AC-02 |
| FR-REQ-008 | UC-03 | BR-REQ-008, BR-AUDIT-001 | Requirement, Tag, RequirementTag | AC-02 |
| FR-LIFE-001 | UC-04 | BR-LIFE-001, BR-LIFE-002, BR-AUDIT-001 | Requirement, AuditEvent | AC-03 |
| FR-LIFE-002 | UC-04 | BR-LIFE-003, BR-REQ-013, BR-REV-008, BR-REV-010 | Requirement, AuditEvent | AC-03 |
| FR-LIFE-003 | UC-04 | BR-LIFE-001, BR-LIFE-002, BR-LIFE-004, BR-LIFE-009 | Requirement, AuditEvent | AC-03 |
| FR-LIFE-004 | UC-04 | BR-LIFE-007 | Requirement, AuditEvent | AC-03 |
| FR-LIFE-005 | UC-04 | BR-LIFE-008 | Requirement, AuditEvent | AC-03 |
| FR-LIFE-006 | UC-04 | BR-LIFE-006, BR-REQ-011 | Requirement, AuditEvent | AC-03 |
| FR-REV-001 | UC-05 | BR-REV-001, BR-REV-002, BR-REV-009 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-REV-002 | UC-05 | BR-REV-003, BR-REV-004, BR-REV-006, BR-VER-002, BR-VER-010 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-REV-003 | UC-05 | BR-REV-003, BR-REV-004, BR-REV-005 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-REV-004 | UC-05 | BR-REV-004, BR-REV-005, BR-LIFE-005 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-REV-005 | UC-05 | BR-REV-005, BR-REV-006 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-REV-006 | UC-05 | BR-REV-007, BR-REV-008, BR-REV-010, BR-AUDIT-005 | RequirementReview, Requirement, RequirementVersion | AC-04 |
| FR-VER-001 | UC-05, UC-12 | BR-VER-001, BR-VER-002, BR-VER-003, BR-VER-004, BR-VER-005, BR-VER-006, BR-VER-007, BR-VER-010 | RequirementVersion, Requirement | AC-05 |
| FR-VER-002 | UC-08 | BR-VER-004, BR-VER-009 | RequirementVersion, Requirement | AC-05 |
| FR-VER-003 | UC-08 | BR-VER-004, BR-VER-005, BR-VER-009 | RequirementVersion, Requirement | AC-05 |
| FR-VER-004 | UC-08 | BR-VER-007, BR-VER-010 | RequirementVersion, Requirement | AC-05 |
| FR-VER-005 | UC-08 | BR-VER-001, BR-VER-008, BR-VER-011 | RequirementVersion, Requirement | AC-05 |
| FR-REL-001 | UC-07 | BR-REL-001, BR-REL-002, BR-REL-005, BR-REL-006, BR-REL-007 | RequirementRelation, Requirement | AC-07 |
| FR-REL-002 | UC-07 | BR-REL-008, BR-REL-009 | RequirementRelation, Requirement | AC-07 |
| FR-REL-003 | UC-07 | BR-REL-003, BR-REL-004, BR-REL-005, BR-REL-006 | RequirementRelation, Requirement | AC-07 |
| FR-REL-004 | UC-07 | BR-REL-002, BR-REL-003, BR-REL-004 | RequirementRelation, Requirement | AC-07 |
| FR-REL-005 | UC-07 | BR-REL-001, BR-REL-005, BR-REL-006, BR-REL-007, BR-REL-010 | RequirementRelation, Requirement | AC-07 |
| FR-REL-006 | UC-07 | BR-REL-003, BR-REL-004, BR-REL-007 | RequirementRelation, Requirement | AC-07 |
| FR-CR-001 | UC-10 | BR-CR-001, BR-CR-002, BR-CR-003, BR-CR-005, BR-CR-021 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-002 | UC-10 | BR-CR-004, BR-CR-006, BR-CR-007, BR-REQ-013 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-003 | UC-10 | BR-CR-007, BR-CR-020 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-004 | UC-11 | BR-CR-009, BR-CR-010, BR-CR-019, BR-CR-022 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-005 | UC-11 | BR-CR-009, BR-CR-017, BR-CR-019 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-006 | UC-12 | BR-CR-011, BR-CR-012, BR-CR-013, BR-CR-014, BR-CR-015, BR-CR-016, BR-CR-021, BR-CR-022, BR-LIFE-010, BR-AUDIT-006 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-11 |
| FR-CR-007 | UC-10, UC-11, UC-12 | BR-CR-020, BR-VER-007, BR-AUDIT-005 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-008 | UC-10 | BR-CR-018, BR-CR-005 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-CR-009 | UC-11 | BR-CR-008, BR-CR-009, BR-CR-019, BR-CR-020 | ChangeRequest, ChangeRequestReview, RequirementVersion, Requirement | AC-06 |
| FR-COM-001 | UC-09 | BR-COM-001, BR-COM-002, BR-COM-003, BR-COM-006 | Comment | AC-08 |
| FR-COM-002 | UC-09 | BR-COM-005, BR-REQ-012 | Comment | AC-08 |
| FR-COM-003 | UC-09 | BR-COM-003, BR-COM-004, BR-AUDIT-003, BR-AUDIT-004 | Comment | AC-08 |
| FR-COM-004 | UC-09 | BR-COM-005, BR-COM-002 | Comment | AC-08 |
| FR-SEARCH-001 | UC-06 | BR-AUTH-003, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-002 | UC-06 | BR-AUTH-003, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-003 | UC-06 | BR-LIFE-001, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-004 | UC-06 | BR-REQ-002, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-005 | UC-06 | BR-REQ-002, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-006 | UC-06 | BR-REQ-008, BR-REQ-012 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-SEARCH-007 | UC-06 | BR-VER-005, BR-VER-009, BR-AUDIT-005 | Requirement, RequirementRelation, Tag | AC-09 |
| FR-AUDIT-001 | S1 | BR-AUDIT-001, BR-AUDIT-002, BR-AUDIT-003, BR-AUDIT-004, BR-AUDIT-006 | AuditEvent | AC-10 |
| FR-AUDIT-002 | S1 | BR-AUDIT-001, BR-AUDIT-004 | AuditEvent | AC-10 |
| FR-AUDIT-003 | S1 | BR-AUDIT-001, BR-AUDIT-005, BR-AUDIT-006 | AuditEvent | AC-10 |
| FR-AUDIT-004 | UC-08 | BR-AUDIT-002, BR-AUDIT-005, BR-AUTH-003 | AuditEvent | AC-10 |
