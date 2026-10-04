/** Client hint only. The server applies the trusted identical UTF-8 limit. */
export function passwordProblem(raw: string): string | null {
  if (!/\S/u.test(raw)) return '密码不能为空或仅包含空白。';
  const bytes = new TextEncoder().encode(raw).length;
  return bytes < 1 || bytes > 72 ? '密码必须为 1 至 72 个 UTF-8 字节。' : null;
}
