-- RMS Design Baseline 1.3 / MySQL 8.4 / design only, not deployed.
-- Successful mutations require Service validation + transaction + audit.
-- Business times are supplied by the server as UTC DATETIME(6).
-- No cascading deletion of business history.
SET NAMES utf8mb4;

CREATE TABLE `User` (
  `userId` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `email` VARCHAR(254) NOT NULL,
  `passwordHash` VARCHAR(255) NOT NULL,
  `displayName` VARCHAR(100) NOT NULL,
  `accountStatus` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `createdAt` DATETIME(6) NOT NULL,
  `updatedAt` DATETIME(6) NOT NULL,
  `lockVersion` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `uk_User_1` (`username`),
  UNIQUE KEY `uk_User_2` (`email`),
  CONSTRAINT `ck_User_1` CHECK (lockVersion >= 0),
  CONSTRAINT `ck_User_2` CHECK (`accountStatus` IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Role` (
  `roleId` BIGINT NOT NULL AUTO_INCREMENT,
  `roleCode` VARCHAR(32) NOT NULL,
  `roleName` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`roleId`),
  UNIQUE KEY `uk_Role_1` (`roleCode`),
  CONSTRAINT `ck_Role_1` CHECK (`roleCode` IN ('ADMIN', 'REQUIREMENT_ENGINEER', 'REVIEWER', 'PROJECT_MEMBER', 'VIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `UserRole` (
  `userId` BIGINT NOT NULL,
  `roleId` BIGINT NOT NULL,
  `grantedBy` BIGINT NOT NULL,
  `grantedAt` DATETIME(6) NOT NULL,
  PRIMARY KEY (`userId`, `roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Requirement` (
  `requirementId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementKey` VARCHAR(32) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NOT NULL,
  `level` VARCHAR(32) NOT NULL,
  `kind` VARCHAR(32) NOT NULL,
  `priority` VARCHAR(32) NOT NULL,
  `source` TEXT NULL DEFAULT NULL,
  `rationale` TEXT NULL DEFAULT NULL,
  `acceptanceCriteria` TEXT NULL DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `creatorId` BIGINT NOT NULL,
  `assigneeId` BIGINT NULL DEFAULT NULL,
  `currentVersionId` BIGINT NULL DEFAULT NULL,
  `firstSubmittedAt` DATETIME(6) NULL DEFAULT NULL,
  `isWithdrawn` TINYINT NOT NULL DEFAULT 0,
  `withdrawnBy` BIGINT NULL DEFAULT NULL,
  `withdrawnAt` DATETIME(6) NULL DEFAULT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  `updatedAt` DATETIME(6) NOT NULL,
  `lockVersion` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`requirementId`),
  UNIQUE KEY `uk_Requirement_1` (`requirementKey`),
  CONSTRAINT `ck_Requirement_1` CHECK (isWithdrawn IN (0,1)),
  CONSTRAINT `ck_Requirement_2` CHECK (lockVersion >= 0),
  CONSTRAINT `ck_Requirement_3` CHECK (((isWithdrawn=0 AND withdrawnBy IS NULL AND withdrawnAt IS NULL) OR (isWithdrawn=1 AND withdrawnBy IS NOT NULL AND withdrawnAt IS NOT NULL AND status='DRAFT' AND firstSubmittedAt IS NULL AND currentVersionId IS NULL))),
  CONSTRAINT `ck_Requirement_4` CHECK (((status IN ('DRAFT','UNDER_REVIEW','REJECTED') AND currentVersionId IS NULL) OR (status IN ('APPROVED','IMPLEMENTED','VERIFIED') AND currentVersionId IS NOT NULL))),
  CONSTRAINT `ck_Requirement_5` CHECK (`level` IN ('BUSINESS', 'USER', 'SYSTEM')),
  CONSTRAINT `ck_Requirement_6` CHECK (`kind` IN ('FUNCTIONAL', 'QUALITY', 'CONSTRAINT')),
  CONSTRAINT `ck_Requirement_7` CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `ck_Requirement_8` CHECK (`status` IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'IMPLEMENTED', 'VERIFIED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `RequirementVersion` (
  `versionId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementId` BIGINT NOT NULL,
  `versionNo` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NOT NULL,
  `level` VARCHAR(32) NOT NULL,
  `kind` VARCHAR(32) NOT NULL,
  `priority` VARCHAR(32) NOT NULL,
  `source` TEXT NOT NULL,
  `rationale` TEXT NOT NULL,
  `acceptanceCriteria` TEXT NOT NULL,
  `initialReviewId` BIGINT NULL DEFAULT NULL,
  `appliedChangeRequestId` BIGINT NULL DEFAULT NULL,
  `createdBy` BIGINT NOT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  `changeReason` TEXT NOT NULL,
  PRIMARY KEY (`versionId`),
  UNIQUE KEY `uk_RequirementVersion_1` (`requirementId`, `versionNo`),
  UNIQUE KEY `uk_RequirementVersion_2` (`requirementId`, `versionId`),
  UNIQUE KEY `uk_RequirementVersion_3` (`initialReviewId`),
  UNIQUE KEY `uk_RequirementVersion_4` (`appliedChangeRequestId`),
  CONSTRAINT `ck_RequirementVersion_1` CHECK (versionNo >= 1),
  CONSTRAINT `ck_RequirementVersion_2` CHECK (((versionNo=1 AND initialReviewId IS NOT NULL AND appliedChangeRequestId IS NULL) OR (versionNo>1 AND initialReviewId IS NULL AND appliedChangeRequestId IS NOT NULL))),
  CONSTRAINT `ck_RequirementVersion_3` CHECK (`level` IN ('BUSINESS', 'USER', 'SYSTEM')),
  CONSTRAINT `ck_RequirementVersion_4` CHECK (`kind` IN ('FUNCTIONAL', 'QUALITY', 'CONSTRAINT')),
  CONSTRAINT `ck_RequirementVersion_5` CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `RequirementReview` (
  `reviewId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementId` BIGINT NOT NULL,
  `roundNo` INT NOT NULL,
  `snapshotTitle` VARCHAR(200) NOT NULL,
  `snapshotDescription` TEXT NOT NULL,
  `snapshotLevel` VARCHAR(32) NOT NULL,
  `snapshotKind` VARCHAR(32) NOT NULL,
  `snapshotPriority` VARCHAR(32) NOT NULL,
  `snapshotSource` TEXT NOT NULL,
  `snapshotRationale` TEXT NOT NULL,
  `snapshotAcceptanceCriteria` TEXT NOT NULL,
  `submittedBy` BIGINT NOT NULL,
  `submittedAt` DATETIME(6) NOT NULL,
  `reviewStatus` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `reviewerId` BIGINT NULL DEFAULT NULL,
  `decision` VARCHAR(32) NULL DEFAULT NULL,
  `comment` TEXT NULL DEFAULT NULL,
  `decidedAt` DATETIME(6) NULL DEFAULT NULL,
  `activeRequirementId` BIGINT GENERATED ALWAYS AS (CASE WHEN reviewStatus='PENDING' THEN requirementId ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`reviewId`),
  UNIQUE KEY `uk_RequirementReview_1` (`requirementId`, `roundNo`),
  UNIQUE KEY `uk_RequirementReview_2` (`requirementId`, `reviewId`),
  UNIQUE KEY `uk_RequirementReview_3` (`activeRequirementId`),
  CONSTRAINT `ck_RequirementReview_1` CHECK (roundNo >= 1),
  CONSTRAINT `ck_RequirementReview_2` CHECK (((reviewStatus='PENDING' AND reviewerId IS NULL AND decision IS NULL AND decidedAt IS NULL) OR (reviewStatus='COMPLETED' AND reviewerId IS NOT NULL AND decision IS NOT NULL AND decidedAt IS NOT NULL))),
  CONSTRAINT `ck_RequirementReview_3` CHECK ((decision IS NULL OR decision='APPROVE' OR (comment IS NOT NULL AND CHAR_LENGTH(TRIM(comment))>0))),
  CONSTRAINT `ck_RequirementReview_4` CHECK (`snapshotLevel` IN ('BUSINESS', 'USER', 'SYSTEM')),
  CONSTRAINT `ck_RequirementReview_5` CHECK (`snapshotKind` IN ('FUNCTIONAL', 'QUALITY', 'CONSTRAINT')),
  CONSTRAINT `ck_RequirementReview_6` CHECK (`snapshotPriority` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `ck_RequirementReview_7` CHECK (`reviewStatus` IN ('PENDING', 'COMPLETED')),
  CONSTRAINT `ck_RequirementReview_8` CHECK (`decision` IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ChangeRequest` (
  `changeRequestId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementId` BIGINT NOT NULL,
  `baseVersionId` BIGINT NOT NULL,
  `requestTitle` VARCHAR(200) NOT NULL,
  `reason` TEXT NOT NULL,
  `proposedTitle` VARCHAR(200) NOT NULL,
  `proposedDescription` TEXT NOT NULL,
  `proposedLevel` VARCHAR(32) NOT NULL,
  `proposedKind` VARCHAR(32) NOT NULL,
  `proposedPriority` VARCHAR(32) NOT NULL,
  `proposedSource` TEXT NULL DEFAULT NULL,
  `proposedRationale` TEXT NULL DEFAULT NULL,
  `proposedAcceptanceCriteria` TEXT NULL DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `createdBy` BIGINT NOT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  `updatedAt` DATETIME(6) NOT NULL,
  `appliedBy` BIGINT NULL DEFAULT NULL,
  `appliedAt` DATETIME(6) NULL DEFAULT NULL,
  `lockVersion` BIGINT NOT NULL DEFAULT 0,
  `activeRequirementId` BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('DRAFT','UNDER_REVIEW','APPROVED') THEN requirementId ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`changeRequestId`),
  UNIQUE KEY `uk_ChangeRequest_1` (`requirementId`, `changeRequestId`),
  UNIQUE KEY `uk_ChangeRequest_2` (`activeRequirementId`),
  CONSTRAINT `ck_ChangeRequest_1` CHECK (lockVersion >= 0),
  CONSTRAINT `ck_ChangeRequest_2` CHECK (((status='APPLIED' AND appliedBy IS NOT NULL AND appliedAt IS NOT NULL) OR (status<>'APPLIED' AND appliedBy IS NULL AND appliedAt IS NULL))),
  CONSTRAINT `ck_ChangeRequest_3` CHECK (`proposedLevel` IN ('BUSINESS', 'USER', 'SYSTEM')),
  CONSTRAINT `ck_ChangeRequest_4` CHECK (`proposedKind` IN ('FUNCTIONAL', 'QUALITY', 'CONSTRAINT')),
  CONSTRAINT `ck_ChangeRequest_5` CHECK (`proposedPriority` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `ck_ChangeRequest_6` CHECK (`status` IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'APPLIED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ChangeRequestReview` (
  `changeReviewId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementId` BIGINT NOT NULL,
  `changeRequestId` BIGINT NOT NULL,
  `roundNo` INT NOT NULL,
  `snapshotBaseVersionId` BIGINT NOT NULL,
  `snapshotRequestTitle` VARCHAR(200) NOT NULL,
  `snapshotReason` TEXT NOT NULL,
  `snapshotProposedTitle` VARCHAR(200) NOT NULL,
  `snapshotProposedDescription` TEXT NOT NULL,
  `snapshotProposedLevel` VARCHAR(32) NOT NULL,
  `snapshotProposedKind` VARCHAR(32) NOT NULL,
  `snapshotProposedPriority` VARCHAR(32) NOT NULL,
  `snapshotProposedSource` TEXT NOT NULL,
  `snapshotProposedRationale` TEXT NOT NULL,
  `snapshotProposedAcceptanceCriteria` TEXT NOT NULL,
  `submittedBy` BIGINT NOT NULL,
  `submittedAt` DATETIME(6) NOT NULL,
  `reviewStatus` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `reviewerId` BIGINT NULL DEFAULT NULL,
  `decision` VARCHAR(32) NULL DEFAULT NULL,
  `comment` TEXT NULL DEFAULT NULL,
  `decidedAt` DATETIME(6) NULL DEFAULT NULL,
  `activeChangeRequestId` BIGINT GENERATED ALWAYS AS (CASE WHEN reviewStatus='PENDING' THEN changeRequestId ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`changeReviewId`),
  UNIQUE KEY `uk_ChangeRequestReview_1` (`changeRequestId`, `roundNo`),
  UNIQUE KEY `uk_ChangeRequestReview_2` (`activeChangeRequestId`),
  CONSTRAINT `ck_ChangeRequestReview_1` CHECK (roundNo >= 1),
  CONSTRAINT `ck_ChangeRequestReview_2` CHECK (((reviewStatus='PENDING' AND reviewerId IS NULL AND decision IS NULL AND decidedAt IS NULL) OR (reviewStatus='COMPLETED' AND reviewerId IS NOT NULL AND decision IS NOT NULL AND decidedAt IS NOT NULL))),
  CONSTRAINT `ck_ChangeRequestReview_3` CHECK ((decision IS NULL OR decision='APPROVE' OR (comment IS NOT NULL AND CHAR_LENGTH(TRIM(comment))>0))),
  CONSTRAINT `ck_ChangeRequestReview_4` CHECK (`snapshotProposedLevel` IN ('BUSINESS', 'USER', 'SYSTEM')),
  CONSTRAINT `ck_ChangeRequestReview_5` CHECK (`snapshotProposedKind` IN ('FUNCTIONAL', 'QUALITY', 'CONSTRAINT')),
  CONSTRAINT `ck_ChangeRequestReview_6` CHECK (`snapshotProposedPriority` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `ck_ChangeRequestReview_7` CHECK (`reviewStatus` IN ('PENDING', 'COMPLETED')),
  CONSTRAINT `ck_ChangeRequestReview_8` CHECK (`decision` IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `RequirementRelation` (
  `relationId` BIGINT NOT NULL AUTO_INCREMENT,
  `sourceRequirementId` BIGINT NOT NULL,
  `targetRequirementId` BIGINT NOT NULL,
  `relationType` VARCHAR(32) NOT NULL,
  `description` VARCHAR(1000) NULL DEFAULT NULL,
  `createdBy` BIGINT NOT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  PRIMARY KEY (`relationId`),
  UNIQUE KEY `uk_RequirementRelation_1` (`sourceRequirementId`, `targetRequirementId`, `relationType`),
  CONSTRAINT `ck_RequirementRelation_1` CHECK (sourceRequirementId <> targetRequirementId),
  CONSTRAINT `ck_RequirementRelation_2` CHECK ((relationType NOT IN ('CONFLICTS_WITH','DUPLICATES','RELATES_TO') OR sourceRequirementId < targetRequirementId)),
  CONSTRAINT `ck_RequirementRelation_3` CHECK (`relationType` IN ('DEPENDS_ON', 'REFINES', 'DERIVED_FROM', 'CONFLICTS_WITH', 'DUPLICATES', 'RELATES_TO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Tag` (
  `tagId` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(500) NULL DEFAULT NULL,
  `createdBy` BIGINT NOT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  PRIMARY KEY (`tagId`),
  UNIQUE KEY `uk_Tag_1` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `RequirementTag` (
  `requirementId` BIGINT NOT NULL,
  `tagId` BIGINT NOT NULL,
  `addedBy` BIGINT NOT NULL,
  `addedAt` DATETIME(6) NOT NULL,
  PRIMARY KEY (`requirementId`, `tagId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Comment` (
  `commentId` BIGINT NOT NULL AUTO_INCREMENT,
  `requirementId` BIGINT NOT NULL,
  `authorId` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `createdAt` DATETIME(6) NOT NULL,
  `isDeleted` TINYINT NOT NULL DEFAULT 0,
  `deletedBy` BIGINT NULL DEFAULT NULL,
  `deletedAt` DATETIME(6) NULL DEFAULT NULL,
  PRIMARY KEY (`commentId`),
  CONSTRAINT `ck_Comment_1` CHECK (isDeleted IN (0,1)),
  CONSTRAINT `ck_Comment_2` CHECK ((deletedBy IS NULL OR deletedBy=authorId)),
  CONSTRAINT `ck_Comment_3` CHECK (((isDeleted=0 AND deletedBy IS NULL AND deletedAt IS NULL) OR (isDeleted=1 AND deletedBy IS NOT NULL AND deletedAt IS NOT NULL)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `AuditEvent` (
  `auditId` BIGINT NOT NULL AUTO_INCREMENT,
  `actorId` BIGINT NOT NULL,
  `requirementId` BIGINT NULL DEFAULT NULL,
  `targetType` VARCHAR(32) NOT NULL,
  `targetId` VARCHAR(128) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `outcome` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
  `beforeData` JSON NULL DEFAULT NULL,
  `afterData` JSON NULL DEFAULT NULL,
  `occurredAt` DATETIME(6) NOT NULL,
  PRIMARY KEY (`auditId`),
  CONSTRAINT `ck_AuditEvent_1` CHECK (`targetType` IN ('USER', 'ROLE_ASSIGNMENT', 'REQUIREMENT', 'REVIEW', 'VERSION', 'RELATION', 'CHANGE_REQUEST', 'CHANGE_REVIEW', 'TAG_ASSIGNMENT', 'TAG', 'COMMENT')),
  CONSTRAINT `ck_AuditEvent_2` CHECK (`outcome` IN ('SUCCESS', 'DENIED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `UserRole` ADD CONSTRAINT `fk_R01` FOREIGN KEY (`userId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `UserRole` ADD CONSTRAINT `fk_R02` FOREIGN KEY (`roleId`) REFERENCES `Role` (`roleId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `UserRole` ADD CONSTRAINT `fk_R03` FOREIGN KEY (`grantedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Requirement` ADD CONSTRAINT `fk_R04` FOREIGN KEY (`creatorId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Requirement` ADD CONSTRAINT `fk_R05` FOREIGN KEY (`assigneeId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Requirement` ADD CONSTRAINT `fk_R06` FOREIGN KEY (`withdrawnBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementVersion` ADD CONSTRAINT `fk_R07` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Requirement` ADD CONSTRAINT `fk_R08` FOREIGN KEY (`requirementId`, `currentVersionId`) REFERENCES `RequirementVersion` (`requirementId`, `versionId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementReview` ADD CONSTRAINT `fk_R09` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementReview` ADD CONSTRAINT `fk_R10` FOREIGN KEY (`submittedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementReview` ADD CONSTRAINT `fk_R11` FOREIGN KEY (`reviewerId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementVersion` ADD CONSTRAINT `fk_R12` FOREIGN KEY (`requirementId`, `initialReviewId`) REFERENCES `RequirementReview` (`requirementId`, `reviewId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementVersion` ADD CONSTRAINT `fk_R13` FOREIGN KEY (`requirementId`, `appliedChangeRequestId`) REFERENCES `ChangeRequest` (`requirementId`, `changeRequestId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementVersion` ADD CONSTRAINT `fk_R14` FOREIGN KEY (`createdBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequest` ADD CONSTRAINT `fk_R15` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequest` ADD CONSTRAINT `fk_R16` FOREIGN KEY (`requirementId`, `baseVersionId`) REFERENCES `RequirementVersion` (`requirementId`, `versionId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequest` ADD CONSTRAINT `fk_R17` FOREIGN KEY (`createdBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequest` ADD CONSTRAINT `fk_R18` FOREIGN KEY (`appliedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequestReview` ADD CONSTRAINT `fk_R19` FOREIGN KEY (`requirementId`, `changeRequestId`) REFERENCES `ChangeRequest` (`requirementId`, `changeRequestId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequestReview` ADD CONSTRAINT `fk_R20` FOREIGN KEY (`requirementId`, `snapshotBaseVersionId`) REFERENCES `RequirementVersion` (`requirementId`, `versionId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequestReview` ADD CONSTRAINT `fk_R21` FOREIGN KEY (`submittedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `ChangeRequestReview` ADD CONSTRAINT `fk_R22` FOREIGN KEY (`reviewerId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementRelation` ADD CONSTRAINT `fk_R23` FOREIGN KEY (`sourceRequirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementRelation` ADD CONSTRAINT `fk_R24` FOREIGN KEY (`targetRequirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementRelation` ADD CONSTRAINT `fk_R25` FOREIGN KEY (`createdBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Tag` ADD CONSTRAINT `fk_R26` FOREIGN KEY (`createdBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementTag` ADD CONSTRAINT `fk_R27` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementTag` ADD CONSTRAINT `fk_R28` FOREIGN KEY (`tagId`) REFERENCES `Tag` (`tagId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `RequirementTag` ADD CONSTRAINT `fk_R29` FOREIGN KEY (`addedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Comment` ADD CONSTRAINT `fk_R30` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Comment` ADD CONSTRAINT `fk_R31` FOREIGN KEY (`authorId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `Comment` ADD CONSTRAINT `fk_R32` FOREIGN KEY (`deletedBy`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `AuditEvent` ADD CONSTRAINT `fk_R33` FOREIGN KEY (`actorId`) REFERENCES `User` (`userId`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `AuditEvent` ADD CONSTRAINT `fk_R34` FOREIGN KEY (`requirementId`) REFERENCES `Requirement` (`requirementId`) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Trusted deployment seed: create the five predefined roles, then one bootstrap ADMIN user.
-- Its ADMIN UserRole.grantedBy refers to that newly created user; this is the sole seed exception.
-- At runtime only an existing ADMIN may grant roles; grantedBy is server-derived, never client input.
-- initialPassword is request-only; the server computes BCrypt and stores only NOT NULL passwordHash.
-- Exclude initialPassword and passwordHash from responses and audit payloads; log no plaintext password.
-- Service-only rules: at least one role for enabled accounts, snapshot immutability,
-- CR review snapshots copy the corresponding CR at submission, field-for-field, in the same transaction.
-- Composite FKs enforce the same Requirement; they do not enforce CR snapshot value equality.
-- self-review exclusion, complete content on submission, current = latest version,
-- version sequence, final review decisions, graph cycle checks and audit atomicity.
-- Lock Requirement first for approval, CR creation/application and version allocation.
-- Serialize graph mutations via one MySQL named lock on the same connection; release in finally.
