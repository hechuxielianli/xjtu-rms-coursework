import axios from 'axios';
import type { ApiError } from '../api/contracts.generated';
import { isDecimalString } from '../api/decimal';

export type FailureKind = 'validation' | 'authentication' | 'authorization' | 'not_found' | 'conflict' | 'network' | 'server' | 'protocol';
const MESSAGES: Record<ApiError['code'], string> = {
  INVALID_INPUT: '输入不符合要求，请检查后重新提交。', INCOMPLETE_CONTENT: '请补齐必填内容。',
  UNAUTHENTICATED: '会话已失效，请重新登录。', INVALID_CREDENTIALS: '用户名、邮箱或密码不正确。',
  FORBIDDEN: '当前账号没有此操作权限。', ACCOUNT_DISABLED: '账号已禁用，请联系管理员。',
  SELF_REVIEW: '不能评审自己提出的内容。', NOT_AUTHOR: '只有作者本人可以执行此操作。',
  NOT_FOUND: '内容不存在或已经不可用。', LOCK_VERSION_CONFLICT: '内容已被其他操作修改，请先重新读取。',
  STATE_CONFLICT: '当前状态不允许此操作，请先重新读取。', BASE_VERSION_STALE: '基准版本已过期，请先重新读取。',
  DUPLICATE: '记录已存在。', ACTIVE_CHANGE_EXISTS: '已有活动变更请求。', ACTIVE_REVIEW_EXISTS: '已有进行中的评审。',
  DEPENDENCY_CYCLE: '该关系会形成依赖环。', GRAPH_BUSY: '关系操作正在进行，请稍后核对最新数据。',
  INTERNAL_ERROR: '服务器暂不可用，请先核对操作结果。',
};

/** Deliberately omit Axios config/cause/server message: they can contain credentials. */
export class ApiFailure extends Error {
  constructor(
    public readonly code: ApiError['code'] | 'NETWORK_ERROR' | 'INVALID_RESPONSE' | 'CSRF_UNAVAILABLE',
    public readonly kind: FailureKind,
    public readonly status?: number,
    public readonly correlationId?: string,
    public readonly currentLockVersion?: string,
    public readonly outcomeUnknown = false,
  ) {
    super(code in MESSAGES ? MESSAGES[code as ApiError['code']] :
      code === 'CSRF_UNAVAILABLE' ? '安全令牌暂不可用，请先刷新会话。' :
      code === 'INVALID_RESPONSE' ? '服务器响应不符合接口约定，请先核对会话。' : '连接中断或超时，请先核对操作结果。');
    this.name = 'ApiFailure';
  }
}

export function normalizeFailure(error: unknown, unsafe = false): ApiFailure {
  if (error instanceof ApiFailure) return error;
  if (!axios.isAxiosError(error)) return new ApiFailure('INVALID_RESPONSE', 'protocol', undefined, undefined, undefined, unsafe);
  const status = error.response?.status;
  if (status === undefined) return new ApiFailure('NETWORK_ERROR', 'network', undefined, undefined, undefined, unsafe);
  const data = error.response?.data as Partial<ApiError> | undefined;
  const knownCode = data && typeof data.code === 'string' && Object.hasOwn(MESSAGES, data.code);
  const code = knownCode ? data.code as ApiError['code'] :
    status === 400 ? 'INVALID_INPUT' : status === 401 ? 'UNAUTHENTICATED' : status === 403 ? 'FORBIDDEN' :
    status === 404 ? 'NOT_FOUND' : status === 409 ? 'STATE_CONFLICT' : 'INTERNAL_ERROR';
  const correlationId = data && typeof data.correlationId === 'string' && /^[a-zA-Z0-9._:-]{1,128}$/.test(data.correlationId) ? data.correlationId : undefined;
  const currentLockVersion = isDecimalString(data?.currentLockVersion, true) ? data.currentLockVersion : undefined;
  const kind: FailureKind = status === 400 ? 'validation' : status === 401 ? 'authentication' : status === 403 ? 'authorization' :
    status === 404 ? 'not_found' : status === 409 ? 'conflict' : 'server';
  return new ApiFailure(code, kind, status, correlationId, currentLockVersion, unsafe && status >= 500);
}
