// Generated from docs/contracts/openapi.yaml. Do not edit manually.
// Source SHA256: d0a50639679c8ca38ccde5a16d1f9836fcb2b12ceeafb92ee90df9e8da6284ba
// Runtime boundary checks supplement these transport types.
export const contractSchemas = {
  "Id": {
    "type": "string",
    "pattern": "^[1-9][0-9]*$",
    "maxLength": 19,
    "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。"
  },
  "LockVersion": {
    "type": "string",
    "pattern": "^(0|[1-9][0-9]*)$",
    "maxLength": 19,
    "description": "原有signed BIGINT lockVersion非负十进制字符串；服务端校验<=9223372036854775807、比较并由@Version更新。"
  },
  "RoleCode": {
    "type": "string",
    "enum": [
      "ADMIN",
      "REQUIREMENT_ENGINEER",
      "REVIEWER",
      "PROJECT_MEMBER",
      "VIEWER"
    ]
  },
  "UserSummary": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "userId": {
        "$ref": "#/components/schemas/Id"
      },
      "displayName": {
        "type": "string",
        "maxLength": 100
      },
      "accountStatus": {
        "type": "string",
        "enum": [
          "ENABLED",
          "DISABLED"
        ]
      }
    },
    "required": [
      "userId",
      "displayName",
      "accountStatus"
    ]
  },
  "SessionResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "user": {
        "$ref": "#/components/schemas/UserSummary"
      },
      "roles": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RoleCode"
        },
        "minItems": 1,
        "uniqueItems": true
      }
    },
    "required": [
      "user",
      "roles"
    ]
  },
  "CsrfResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "headerName": {
        "type": "string",
        "const": "X-CSRF-TOKEN"
      },
      "token": {
        "type": "string",
        "minLength": 1
      }
    },
    "required": [
      "headerName",
      "token"
    ]
  },
  "LoginRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "login": {
        "type": "string",
        "maxLength": 254,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "password": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*",
        "writeOnly": true,
        "x-rms-max-utf8-bytes": 72,
        "description": "原始Java String；不trim、不做NFC/NFKC或其他Unicode normalization。满足既有non-blank contract且按原值UTF-8编码的实际长度为1..72 bytes；超过72 bytes以400 INVALID_INPUT拒绝，不静默截断。与创建用户initialPassword使用同一服务端byte-limit validator；不得回显、记录或持久化原密码。maxLength不代表字节限制；扩展属性须由服务端显式实现。"
      }
    },
    "required": [
      "login",
      "password"
    ]
  },
  "ApiError": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "code": {
        "type": "string",
        "enum": [
          "INVALID_INPUT",
          "INCOMPLETE_CONTENT",
          "UNAUTHENTICATED",
          "INVALID_CREDENTIALS",
          "FORBIDDEN",
          "ACCOUNT_DISABLED",
          "SELF_REVIEW",
          "NOT_AUTHOR",
          "NOT_FOUND",
          "LOCK_VERSION_CONFLICT",
          "STATE_CONFLICT",
          "BASE_VERSION_STALE",
          "DUPLICATE",
          "ACTIVE_CHANGE_EXISTS",
          "ACTIVE_REVIEW_EXISTS",
          "DEPENDENCY_CYCLE",
          "GRAPH_BUSY",
          "INTERNAL_ERROR"
        ]
      },
      "message": {
        "type": "string"
      },
      "correlationId": {
        "type": "string"
      },
      "currentLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "code",
      "message",
      "correlationId"
    ],
    "description": "安全错误，无SQL/stack/密码；currentLockVersion仅有明确受权资源的修订冲突才提供。"
  },
  "RequirementLevel": {
    "type": "string",
    "enum": [
      "BUSINESS",
      "USER",
      "SYSTEM"
    ]
  },
  "RequirementKind": {
    "type": "string",
    "enum": [
      "FUNCTIONAL",
      "QUALITY",
      "CONSTRAINT"
    ]
  },
  "Priority": {
    "type": "string",
    "enum": [
      "LOW",
      "MEDIUM",
      "HIGH",
      "CRITICAL"
    ]
  },
  "RequirementStatus": {
    "type": "string",
    "enum": [
      "DRAFT",
      "UNDER_REVIEW",
      "APPROVED",
      "REJECTED",
      "IMPLEMENTED",
      "VERIFIED"
    ]
  },
  "RequirementResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementKey": {
        "type": "string",
        "maxLength": 32,
        "readOnly": true
      },
      "title": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "description": {
        "type": "string",
        "readOnly": true
      },
      "level": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ],
        "readOnly": true
      },
      "kind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ],
        "readOnly": true
      },
      "priority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ],
        "readOnly": true
      },
      "source": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "rationale": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "acceptanceCriteria": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "status": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "DRAFT",
          "UNDER_REVIEW",
          "APPROVED",
          "REJECTED",
          "IMPLEMENTED",
          "VERIFIED"
        ],
        "readOnly": true
      },
      "creatorId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "assigneeId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "currentVersionId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "firstSubmittedAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "isWithdrawn": {
        "type": "integer",
        "minimum": 0,
        "enum": [
          0,
          1
        ],
        "readOnly": true
      },
      "withdrawnBy": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "withdrawnAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "updatedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "lockVersion": {
        "type": "string",
        "pattern": "^(0|[1-9][0-9]*)$",
        "maxLength": 19,
        "description": "原有signed BIGINT lockVersion非负十进制字符串；服务端校验<=9223372036854775807、比较并由@Version更新。",
        "readOnly": true
      },
      "creator": {
        "$ref": "#/components/schemas/UserSummary"
      },
      "assignee": {
        "oneOf": [
          {
            "$ref": "#/components/schemas/UserSummary"
          },
          {
            "type": "null"
          }
        ]
      },
      "tags": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/TagResponse"
        }
      }
    },
    "required": [
      "requirementId",
      "requirementKey",
      "title",
      "description",
      "level",
      "kind",
      "priority",
      "source",
      "rationale",
      "acceptanceCriteria",
      "status",
      "creatorId",
      "assigneeId",
      "currentVersionId",
      "firstSubmittedAt",
      "isWithdrawn",
      "withdrawnBy",
      "withdrawnAt",
      "createdAt",
      "updatedAt",
      "lockVersion",
      "creator",
      "assignee",
      "tags"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "RequirementResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateRequirementRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "title": {
        "type": "string",
        "maxLength": 200,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "description": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "level": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ]
      },
      "kind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ]
      },
      "priority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ]
      },
      "source": {
        "type": [
          "string",
          "null"
        ]
      },
      "rationale": {
        "type": [
          "string",
          "null"
        ]
      },
      "acceptanceCriteria": {
        "type": [
          "string",
          "null"
        ]
      }
    },
    "required": [
      "title",
      "description",
      "level",
      "kind",
      "priority"
    ]
  },
  "EditRequirementContentRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "title": {
        "type": "string",
        "maxLength": 200,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "description": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "level": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ]
      },
      "kind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ]
      },
      "priority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ]
      },
      "source": {
        "type": [
          "string",
          "null"
        ]
      },
      "rationale": {
        "type": [
          "string",
          "null"
        ]
      },
      "acceptanceCriteria": {
        "type": [
          "string",
          "null"
        ]
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedLockVersion"
    ],
    "description": "至少一个八项内容字段；仅DRAFT；提交时全部非空且验收标准可判定。PATCH省略字段不变，显式null仅允许三个可空字段。",
    "anyOf": [
      {
        "required": [
          "title"
        ]
      },
      {
        "required": [
          "description"
        ]
      },
      {
        "required": [
          "level"
        ]
      },
      {
        "required": [
          "kind"
        ]
      },
      {
        "required": [
          "priority"
        ]
      },
      {
        "required": [
          "source"
        ]
      },
      {
        "required": [
          "rationale"
        ]
      },
      {
        "required": [
          "acceptanceCriteria"
        ]
      }
    ]
  },
  "EditMetadataRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "assigneeId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。"
      },
      "tagIds": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Id"
        },
        "uniqueItems": true
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedLockVersion"
    ],
    "description": "至少assigneeId或tagIds之一；省略不改，assigneeId=null清除；tagIds替换当前集合，不产生正式版本。",
    "anyOf": [
      {
        "required": [
          "assigneeId"
        ]
      },
      {
        "required": [
          "tagIds"
        ]
      }
    ]
  },
  "TagResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "tagId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "name": {
        "type": "string",
        "maxLength": 50,
        "readOnly": true
      },
      "description": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 500,
        "readOnly": true
      },
      "createdBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "tagId",
      "name",
      "description",
      "createdBy",
      "createdAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "TagResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/TagResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateTagRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "name": {
        "type": "string",
        "maxLength": 50,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "description": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 500
      }
    },
    "required": [
      "name"
    ]
  },
  "EditTagRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "name": {
        "type": "string",
        "maxLength": 50,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "description": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 500
      }
    },
    "description": "至少一个字段；Tag无lockVersion，禁止捏造新字段。",
    "anyOf": [
      {
        "required": [
          "name"
        ]
      },
      {
        "required": [
          "description"
        ]
      }
    ]
  },
  "CommentResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "commentId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "authorId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "content": {
        "type": [
          "string",
          "null"
        ],
        "description": "isDeleted=1时必须为null，不返回已删除正文；原数据库content仍NOT NULL并保留。"
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "isDeleted": {
        "type": "integer",
        "minimum": 0,
        "enum": [
          0,
          1
        ],
        "readOnly": true
      },
      "deletedBy": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "deletedAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "commentId",
      "requirementId",
      "authorId",
      "content",
      "createdAt",
      "isDeleted",
      "deletedBy",
      "deletedAt"
    ]
  },
  "CommentResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/CommentResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateCommentRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "content": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*"
      }
    },
    "required": [
      "content"
    ]
  },
  "AdminUserResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "userId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "username": {
        "type": "string",
        "maxLength": 64,
        "readOnly": true
      },
      "email": {
        "type": "string",
        "maxLength": 254,
        "readOnly": true
      },
      "displayName": {
        "type": "string",
        "maxLength": 100,
        "readOnly": true
      },
      "accountStatus": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "ENABLED",
          "DISABLED"
        ],
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "updatedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "lockVersion": {
        "type": "string",
        "pattern": "^(0|[1-9][0-9]*)$",
        "maxLength": 19,
        "description": "原有signed BIGINT lockVersion非负十进制字符串；服务端校验<=9223372036854775807、比较并由@Version更新。",
        "readOnly": true
      },
      "roles": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RoleCode"
        },
        "uniqueItems": true,
        "description": "启用账号至少一个角色；禁用账号按原BR允许为空，不能用响应schema增加更强业务规则。"
      },
      "assignments": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/UserRoleResponse"
        }
      }
    },
    "required": [
      "userId",
      "username",
      "email",
      "displayName",
      "accountStatus",
      "createdAt",
      "updatedAt",
      "lockVersion",
      "roles",
      "assignments"
    ],
    "description": "仅ADMIN；绝不返回密码或哈希。"
  },
  "AdminUserResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/AdminUserResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateUserRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "username": {
        "type": "string",
        "maxLength": 64,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "email": {
        "type": "string",
        "maxLength": 254,
        "minLength": 1,
        "pattern": ".*\\S.*",
        "format": "email"
      },
      "displayName": {
        "type": "string",
        "maxLength": 100,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "initialPassword": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*",
        "writeOnly": true,
        "x-rms-max-utf8-bytes": 72,
        "description": "原始Java String；不trim、不做NFC/NFKC或其他Unicode normalization。满足既有non-blank contract且按原值UTF-8编码的实际长度为1..72 bytes；超过72 bytes以400 INVALID_INPUT拒绝，不静默截断。与登录password使用同一服务端byte-limit validator；仅服务器内存处理，校验后BCrypt生成非空哈希，不保存/记录/返回原密码。maxLength不代表字节限制；扩展属性须由服务端显式实现。"
      },
      "roles": {
        "type": "array",
        "minItems": 1,
        "uniqueItems": true,
        "items": {
          "$ref": "#/components/schemas/RoleCode"
        }
      }
    },
    "required": [
      "username",
      "email",
      "displayName",
      "initialPassword",
      "roles"
    ]
  },
  "EditUserRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "username": {
        "type": "string",
        "maxLength": 64,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "email": {
        "type": "string",
        "maxLength": 254,
        "minLength": 1,
        "pattern": ".*\\S.*",
        "format": "email"
      },
      "displayName": {
        "type": "string",
        "maxLength": 100,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedLockVersion"
    ],
    "description": "至少包含一个基本资料字段；不含密码、角色或状态。",
    "anyOf": [
      {
        "required": [
          "username"
        ]
      },
      {
        "required": [
          "email"
        ]
      },
      {
        "required": [
          "displayName"
        ]
      }
    ]
  },
  "SetRolesRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "roles": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RoleCode"
        },
        "uniqueItems": true
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "roles",
      "expectedLockVersion"
    ],
    "description": "ENABLED账号至少保留一个角色；DISABLED是否无角色由原BR-USER-005判定，不增加永久至少一角色规则。"
  },
  "LockCommand": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedLockVersion"
    ]
  },
  "RequirementVersionResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "versionId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "versionNo": {
        "type": "integer",
        "minimum": 1,
        "readOnly": true
      },
      "title": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "description": {
        "type": "string",
        "readOnly": true
      },
      "level": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ],
        "readOnly": true
      },
      "kind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ],
        "readOnly": true
      },
      "priority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ],
        "readOnly": true
      },
      "source": {
        "type": "string",
        "readOnly": true
      },
      "rationale": {
        "type": "string",
        "readOnly": true
      },
      "acceptanceCriteria": {
        "type": "string",
        "readOnly": true
      },
      "initialReviewId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "appliedChangeRequestId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "createdBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "changeReason": {
        "type": "string",
        "readOnly": true
      }
    },
    "required": [
      "versionId",
      "requirementId",
      "versionNo",
      "title",
      "description",
      "level",
      "kind",
      "priority",
      "source",
      "rationale",
      "acceptanceCriteria",
      "initialReviewId",
      "appliedChangeRequestId",
      "createdBy",
      "createdAt",
      "changeReason"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "RequirementVersionResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementVersionResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "RequirementReviewResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "reviewId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "roundNo": {
        "type": "integer",
        "minimum": 1,
        "readOnly": true
      },
      "snapshotTitle": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "snapshotDescription": {
        "type": "string",
        "readOnly": true
      },
      "snapshotLevel": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ],
        "readOnly": true
      },
      "snapshotKind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ],
        "readOnly": true
      },
      "snapshotPriority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ],
        "readOnly": true
      },
      "snapshotSource": {
        "type": "string",
        "readOnly": true
      },
      "snapshotRationale": {
        "type": "string",
        "readOnly": true
      },
      "snapshotAcceptanceCriteria": {
        "type": "string",
        "readOnly": true
      },
      "submittedBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "submittedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "reviewStatus": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "PENDING",
          "COMPLETED"
        ],
        "readOnly": true
      },
      "reviewerId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "decision": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 32,
        "enum": [
          "APPROVE",
          "REJECT",
          "REQUEST_CHANGES",
          null
        ],
        "readOnly": true
      },
      "comment": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "decidedAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "reviewId",
      "requirementId",
      "roundNo",
      "snapshotTitle",
      "snapshotDescription",
      "snapshotLevel",
      "snapshotKind",
      "snapshotPriority",
      "snapshotSource",
      "snapshotRationale",
      "snapshotAcceptanceCriteria",
      "submittedBy",
      "submittedAt",
      "reviewStatus",
      "reviewerId",
      "decision",
      "comment",
      "decidedAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "RequirementReviewResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementReviewResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "RequirementDecisionRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "decision": {
        "$ref": "#/components/schemas/Decision"
      },
      "comment": {
        "type": [
          "string",
          "null"
        ]
      },
      "expectedRequirementLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "decision",
      "expectedRequirementLockVersion"
    ],
    "description": "REJECT或REQUEST_CHANGES必填非空comment；APPROVE可为空；reviewId指向轮次，reviewer由会话确定。",
    "allOf": [
      {
        "if": {
          "properties": {
            "decision": {
              "enum": [
                "REJECT",
                "REQUEST_CHANGES"
              ]
            }
          },
          "required": [
            "decision"
          ]
        },
        "then": {
          "required": [
            "comment"
          ],
          "properties": {
            "comment": {
              "type": "string",
              "minLength": 1,
              "pattern": ".*\\S.*"
            }
          }
        }
      }
    ]
  },
  "RequirementDecisionResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "review": {
        "$ref": "#/components/schemas/RequirementReviewResponse"
      },
      "requirement": {
        "$ref": "#/components/schemas/RequirementResponse"
      },
      "version": {
        "oneOf": [
          {
            "$ref": "#/components/schemas/RequirementVersionResponse"
          },
          {
            "type": "null"
          }
        ]
      }
    },
    "required": [
      "review",
      "requirement",
      "version"
    ]
  },
  "VersionConfirmationRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "expectedVersionId": {
        "$ref": "#/components/schemas/Id"
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      },
      "description": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*"
      }
    },
    "required": [
      "expectedVersionId",
      "expectedLockVersion",
      "description"
    ],
    "description": "实现/验证说明写入审计，与当前正式版本绑定，不新增实体字段。"
  },
  "ChangeStatus": {
    "type": "string",
    "enum": [
      "DRAFT",
      "UNDER_REVIEW",
      "APPROVED",
      "REJECTED",
      "APPLIED",
      "CANCELLED"
    ]
  },
  "ChangeRequestResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "changeRequestId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "baseVersionId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requestTitle": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "reason": {
        "type": "string",
        "readOnly": true
      },
      "proposedTitle": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "proposedDescription": {
        "type": "string",
        "readOnly": true
      },
      "proposedLevel": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ],
        "readOnly": true
      },
      "proposedKind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ],
        "readOnly": true
      },
      "proposedPriority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ],
        "readOnly": true
      },
      "proposedSource": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "proposedRationale": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "proposedAcceptanceCriteria": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "status": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "DRAFT",
          "UNDER_REVIEW",
          "APPROVED",
          "REJECTED",
          "APPLIED",
          "CANCELLED"
        ],
        "readOnly": true
      },
      "createdBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "updatedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "appliedBy": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "appliedAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "lockVersion": {
        "type": "string",
        "pattern": "^(0|[1-9][0-9]*)$",
        "maxLength": 19,
        "description": "原有signed BIGINT lockVersion非负十进制字符串；服务端校验<=9223372036854775807、比较并由@Version更新。",
        "readOnly": true
      }
    },
    "required": [
      "changeRequestId",
      "requirementId",
      "baseVersionId",
      "requestTitle",
      "reason",
      "proposedTitle",
      "proposedDescription",
      "proposedLevel",
      "proposedKind",
      "proposedPriority",
      "proposedSource",
      "proposedRationale",
      "proposedAcceptanceCriteria",
      "status",
      "createdBy",
      "createdAt",
      "updatedAt",
      "appliedBy",
      "appliedAt",
      "lockVersion"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "ChangeRequestResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/ChangeRequestResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "ChangeRequestReviewResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "changeReviewId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "changeRequestId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "roundNo": {
        "type": "integer",
        "minimum": 1,
        "readOnly": true
      },
      "snapshotBaseVersionId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "snapshotRequestTitle": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "snapshotReason": {
        "type": "string",
        "readOnly": true
      },
      "snapshotProposedTitle": {
        "type": "string",
        "maxLength": 200,
        "readOnly": true
      },
      "snapshotProposedDescription": {
        "type": "string",
        "readOnly": true
      },
      "snapshotProposedLevel": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ],
        "readOnly": true
      },
      "snapshotProposedKind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ],
        "readOnly": true
      },
      "snapshotProposedPriority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ],
        "readOnly": true
      },
      "snapshotProposedSource": {
        "type": "string",
        "readOnly": true
      },
      "snapshotProposedRationale": {
        "type": "string",
        "readOnly": true
      },
      "snapshotProposedAcceptanceCriteria": {
        "type": "string",
        "readOnly": true
      },
      "submittedBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "submittedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      },
      "reviewStatus": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "PENDING",
          "COMPLETED"
        ],
        "readOnly": true
      },
      "reviewerId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "decision": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 32,
        "enum": [
          "APPROVE",
          "REJECT",
          "REQUEST_CHANGES",
          null
        ],
        "readOnly": true
      },
      "comment": {
        "type": [
          "string",
          "null"
        ],
        "readOnly": true
      },
      "decidedAt": {
        "type": [
          "string",
          "null"
        ],
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "changeReviewId",
      "requirementId",
      "changeRequestId",
      "roundNo",
      "snapshotBaseVersionId",
      "snapshotRequestTitle",
      "snapshotReason",
      "snapshotProposedTitle",
      "snapshotProposedDescription",
      "snapshotProposedLevel",
      "snapshotProposedKind",
      "snapshotProposedPriority",
      "snapshotProposedSource",
      "snapshotProposedRationale",
      "snapshotProposedAcceptanceCriteria",
      "submittedBy",
      "submittedAt",
      "reviewStatus",
      "reviewerId",
      "decision",
      "comment",
      "decidedAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "ChangeRequestReviewResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/ChangeRequestReviewResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateChangeRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "requestTitle": {
        "type": "string",
        "maxLength": 200,
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "reason": {
        "type": "string",
        "minLength": 1,
        "pattern": ".*\\S.*"
      },
      "expectedRequirementLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "requestTitle",
      "reason",
      "expectedRequirementLockVersion"
    ],
    "description": "基准和八项proposed由服务端复制当前版本，不接受客户端baseVersionId。"
  },
  "EditChangeRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "requestTitle": {
        "type": "string",
        "maxLength": 200
      },
      "reason": {
        "type": "string"
      },
      "proposedTitle": {
        "type": "string",
        "maxLength": 200
      },
      "proposedDescription": {
        "type": "string"
      },
      "proposedLevel": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "BUSINESS",
          "USER",
          "SYSTEM"
        ]
      },
      "proposedKind": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "FUNCTIONAL",
          "QUALITY",
          "CONSTRAINT"
        ]
      },
      "proposedPriority": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "LOW",
          "MEDIUM",
          "HIGH",
          "CRITICAL"
        ]
      },
      "proposedSource": {
        "type": [
          "string",
          "null"
        ]
      },
      "proposedRationale": {
        "type": [
          "string",
          "null"
        ]
      },
      "proposedAcceptanceCriteria": {
        "type": [
          "string",
          "null"
        ]
      },
      "expectedLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedLockVersion"
    ],
    "description": "至少一个申请/建议内容字段；DRAFT编辑；提交前requestTitle/reason及八项建议全部非空。",
    "anyOf": [
      {
        "required": [
          "requestTitle"
        ]
      },
      {
        "required": [
          "reason"
        ]
      },
      {
        "required": [
          "proposedTitle"
        ]
      },
      {
        "required": [
          "proposedDescription"
        ]
      },
      {
        "required": [
          "proposedLevel"
        ]
      },
      {
        "required": [
          "proposedKind"
        ]
      },
      {
        "required": [
          "proposedPriority"
        ]
      },
      {
        "required": [
          "proposedSource"
        ]
      },
      {
        "required": [
          "proposedRationale"
        ]
      },
      {
        "required": [
          "proposedAcceptanceCriteria"
        ]
      }
    ]
  },
  "ChangeDecisionRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "decision": {
        "$ref": "#/components/schemas/Decision"
      },
      "comment": {
        "type": [
          "string",
          "null"
        ]
      },
      "expectedChangeLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "decision",
      "expectedChangeLockVersion"
    ],
    "description": "REJECT或REQUEST_CHANGES必填comment；reviewer由会话确定，完成轮次只一次。",
    "allOf": [
      {
        "if": {
          "properties": {
            "decision": {
              "enum": [
                "REJECT",
                "REQUEST_CHANGES"
              ]
            }
          },
          "required": [
            "decision"
          ]
        },
        "then": {
          "required": [
            "comment"
          ],
          "properties": {
            "comment": {
              "type": "string",
              "minLength": 1,
              "pattern": ".*\\S.*"
            }
          }
        }
      }
    ]
  },
  "ChangeDecisionResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "review": {
        "$ref": "#/components/schemas/ChangeRequestReviewResponse"
      },
      "change": {
        "$ref": "#/components/schemas/ChangeRequestResponse"
      }
    },
    "required": [
      "review",
      "change"
    ]
  },
  "ApplyChangeRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "expectedRequirementLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      },
      "expectedChangeLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "expectedRequirementLockVersion",
      "expectedChangeLockVersion"
    ],
    "description": "服务端验证不可变baseVersion与当前版本，不接受currentVersionId/versionNo。"
  },
  "ApplyChangeResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "change": {
        "$ref": "#/components/schemas/ChangeRequestResponse"
      },
      "requirement": {
        "$ref": "#/components/schemas/RequirementResponse"
      },
      "version": {
        "$ref": "#/components/schemas/RequirementVersionResponse"
      }
    },
    "required": [
      "change",
      "requirement",
      "version"
    ]
  },
  "RelationType": {
    "type": "string",
    "enum": [
      "DEPENDS_ON",
      "REFINES",
      "DERIVED_FROM",
      "CONFLICTS_WITH",
      "DUPLICATES",
      "RELATES_TO"
    ]
  },
  "RequirementRelationResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "relationId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "sourceRequirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "targetRequirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "relationType": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "DEPENDS_ON",
          "REFINES",
          "DERIVED_FROM",
          "CONFLICTS_WITH",
          "DUPLICATES",
          "RELATES_TO"
        ],
        "readOnly": true
      },
      "description": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 1000,
        "readOnly": true
      },
      "createdBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "relationId",
      "sourceRequirementId",
      "targetRequirementId",
      "relationType",
      "description",
      "createdBy",
      "createdAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "RequirementRelationResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementRelationResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "CreateRelationRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "sourceRequirementId": {
        "$ref": "#/components/schemas/Id"
      },
      "targetRequirementId": {
        "$ref": "#/components/schemas/Id"
      },
      "relationType": {
        "$ref": "#/components/schemas/RelationType"
      },
      "description": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 1000
      },
      "expectedSourceLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      },
      "expectedTargetLockVersion": {
        "$ref": "#/components/schemas/LockVersion"
      }
    },
    "required": [
      "sourceRequirementId",
      "targetRequirementId",
      "relationType",
      "expectedSourceLockVersion",
      "expectedTargetLockVersion"
    ]
  },
  "TraceResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "rootRequirementId": {
        "$ref": "#/components/schemas/Id"
      },
      "nodes": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementResponse"
        }
      },
      "edges": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/RequirementRelationResponse"
        }
      },
      "depth": {
        "type": "integer",
        "minimum": 1
      },
      "truncated": {
        "type": "boolean"
      }
    },
    "required": [
      "rootRequirementId",
      "nodes",
      "edges",
      "depth",
      "truncated"
    ],
    "description": "当前关系图；按visited去重，方向及类型保留；depth有限，包含被截断标识。"
  },
  "ReviewStatus": {
    "type": "string",
    "enum": [
      "PENDING",
      "COMPLETED"
    ]
  },
  "RequirementTagResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "requirementId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "tagId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "addedBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "addedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "requirementId",
      "tagId",
      "addedBy",
      "addedAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "AuditResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "auditId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "actorId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "requirementId": {
        "type": [
          "string",
          "null"
        ],
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "targetType": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "USER",
          "ROLE_ASSIGNMENT",
          "REQUIREMENT",
          "REVIEW",
          "VERSION",
          "RELATION",
          "CHANGE_REQUEST",
          "CHANGE_REVIEW",
          "TAG_ASSIGNMENT",
          "TAG",
          "COMMENT"
        ],
        "readOnly": true
      },
      "targetId": {
        "type": "string",
        "maxLength": 128,
        "readOnly": true
      },
      "action": {
        "type": "string",
        "maxLength": 64,
        "readOnly": true
      },
      "outcome": {
        "type": "string",
        "maxLength": 32,
        "enum": [
          "SUCCESS",
          "DENIED",
          "FAILED"
        ],
        "readOnly": true
      },
      "beforeData": {
        "type": [
          "object",
          "null"
        ],
        "additionalProperties": true,
        "description": "安全字段白名单审计投影，严禁凭证/密码/哈希。",
        "readOnly": true
      },
      "afterData": {
        "type": [
          "object",
          "null"
        ],
        "additionalProperties": true,
        "description": "安全字段白名单审计投影，严禁凭证/密码/哈希。",
        "readOnly": true
      },
      "occurredAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "auditId",
      "actorId",
      "requirementId",
      "targetType",
      "targetId",
      "action",
      "outcome",
      "beforeData",
      "afterData",
      "occurredAt"
    ],
    "description": "审计只读，安全投影，排除任何密码/哈希/会话或CSRF凭证。"
  },
  "AuditResponsePage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/AuditResponse"
        }
      },
      "page": {
        "type": "integer",
        "minimum": 0
      },
      "size": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100
      },
      "totalElements": {
        "type": "integer",
        "minimum": 0
      }
    },
    "required": [
      "items",
      "page",
      "size",
      "totalElements"
    ]
  },
  "UserRoleResponse": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "userId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "roleId": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "grantedBy": {
        "type": "string",
        "pattern": "^[1-9][0-9]*$",
        "maxLength": 19,
        "description": "SQL signed BIGINT正整数十进制字符串；服务端再校验<=9223372036854775807，避免JavaScript Number精度丢失。",
        "readOnly": true
      },
      "grantedAt": {
        "type": "string",
        "format": "date-time",
        "pattern": ".*Z$",
        "description": "服务端UTC，ISO8601 Z；SQL DATETIME(6)，微秒精度。",
        "readOnly": true
      }
    },
    "required": [
      "userId",
      "roleId",
      "grantedBy",
      "grantedAt"
    ],
    "description": "安全传输投影；不是JPA Entity；nullable逐项沿用Baseline 1.3，数据库generated活动键不公开。"
  },
  "Decision": {
    "type": "string",
    "enum": [
      "APPROVE",
      "REJECT",
      "REQUEST_CHANGES"
    ]
  }
} as const;
