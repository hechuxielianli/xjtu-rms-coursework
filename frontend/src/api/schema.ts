import { contractSchemas } from './schemas.generated';
import { isDecimalString } from './decimal';
import { ApiFailure } from '../errors/api-error';

type Schema = { $ref?: string; type?: string | readonly string[]; oneOf?: readonly Schema[]; anyOf?: readonly Schema[]; const?: unknown; enum?: readonly unknown[]; properties?: Record<string, Schema>; additionalProperties?: boolean; required?: readonly string[]; items?: Schema; minItems?: number; uniqueItems?: boolean; minimum?: number; maximum?: number; minLength?: number; maxLength?: number; pattern?: string; format?: string };
const schemas = contractSchemas as unknown as Record<string, Schema>;
function invalid(): never { throw new ApiFailure('INVALID_RESPONSE', 'protocol'); }
function auditJson(value: unknown): unknown {
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return value;
  if (typeof value === 'number') { if (!Number.isFinite(value) || Number.isInteger(value) && !Number.isSafeInteger(value)) return invalid(); return value; }
  if (Array.isArray(value)) return value.map(auditJson);
  if (!value || typeof value !== 'object') return invalid();
  const result: Record<string, unknown> = Object.create(null);
  for (const [key, item] of Object.entries(value)) {
    const name = key.toLowerCase().replace(/[^a-z0-9]/g, '');
    if (/password|hash|secret|token|csrf|cookie|session|credential|authorization/.test(name) || ['proto', 'prototype', 'constructor'].includes(name)) return invalid();
    result[key] = auditJson(item);
  }
  return result;
}
function read(schema: Schema, value: unknown): unknown {
  if (schema.$ref) return read(schemas[schema.$ref.split('/').at(-1)!]!, value);
  if (schema.oneOf) {
    for (const option of schema.oneOf) { try { return read(option, value); } catch { /* Try the next declared branch. */ } }
    return invalid();
  }
  if (Array.isArray(schema.type)) {
    for (const kind of schema.type) { try { return read({ ...schema, type: kind }, value); } catch { /* Try the next declared type. */ } }
    return invalid();
  }
  if (schema.const !== undefined && value !== schema.const || schema.enum && !schema.enum.includes(value)) return invalid();
  if (schema.type === 'null') { if (value !== null) return invalid(); return null; }
  if (schema.type === 'boolean') { if (typeof value !== 'boolean') return invalid(); return value; }
  if (schema.type === 'string') {
    if (typeof value !== 'string') return invalid();
    const length = Array.from(value).length;
    if (schema.minLength !== undefined && length < schema.minLength || schema.maxLength !== undefined && length > schema.maxLength || schema.pattern && !new RegExp(schema.pattern, 'u').test(value)) return invalid();
    if (schema.pattern === '^[1-9][0-9]*$' && !isDecimalString(value) || schema.pattern === '^(0|[1-9][0-9]*)$' && !isDecimalString(value, true)) return invalid();
    if (schema.format === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return invalid();
    return value;
  }
  if (schema.type === 'integer') {
    if (typeof value !== 'number' || !Number.isSafeInteger(value) || schema.minimum !== undefined && value < schema.minimum || schema.maximum !== undefined && value > schema.maximum) return invalid();
    return value;
  }
  if (schema.type === 'array') {
    if (!Array.isArray(value) || schema.minItems !== undefined && value.length < schema.minItems || schema.uniqueItems && new Set(value.map(item => JSON.stringify(item))).size !== value.length) return invalid();
    return value.map(item => read(schema.items!, item));
  }
  if (schema.type === 'object') {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return invalid();
    const input = value as Record<string, unknown>;
    if (schema.required?.some(key => !Object.hasOwn(input, key))) return invalid();
    if (schema.anyOf && !schema.anyOf.some(branch => branch.required?.every(key => Object.hasOwn(input, key)))) return invalid();
    if (schema.additionalProperties === true && !schema.properties) return auditJson(input);
    const result: Record<string, unknown> = {};
    for (const [key, field] of Object.entries(schema.properties ?? {})) if (Object.hasOwn(input, key)) result[key] = read(field, input[key]);
    return result; // Construct the schema projection; extra secrets/identity are never stored or sent.
  }
  return invalid();
}
export function readContract<T>(name: keyof typeof contractSchemas, value: unknown): T { return read(schemas[name]!, value) as T; }
export function commandContract<T>(name: keyof typeof contractSchemas, value: unknown): T {
  try { return readContract<T>(name, value); }
  catch { throw new ApiFailure('INVALID_INPUT', 'validation', 400); }
}
