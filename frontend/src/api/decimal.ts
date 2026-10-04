const MAX_SIGNED_BIGINT = '9223372036854775807';

/** Validate without converting a SQL BIGINT to an imprecise JavaScript number. */
export function isDecimalString(value: unknown, allowZero = false): value is string {
  if (typeof value !== 'string' || !(allowZero ? /^(0|[1-9]\d*)$/ : /^[1-9]\d*$/).test(value)) return false;
  return value.length < MAX_SIGNED_BIGINT.length ||
    (value.length === MAX_SIGNED_BIGINT.length && value <= MAX_SIGNED_BIGINT);
}
