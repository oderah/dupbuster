/** Mirrors native TelemetryAllowedFields / DBTelemetryAllowedFields (architecture §8.2). */
export const ALLOWED_TELEMETRY_KEYS = [
  'scan_run_id',
  'phase',
  'files_processed',
  'groups_found',
  'schema_version',
  'teardown_reason',
  'platform_api_level',
  'exception_type',
] as const;

export const BANNED_TELEMETRY_EGRESS_KEYS = [
  'unscannable_reason',
  'uri_or_path',
  'display_name',
  'path',
  'paths',
  'hash_value',
  'hash_algo',
  'frame_hashes_blob',
  'normalization_profile',
  'thumbnail_uri',
  'thumbnailUri',
] as const;

export function filterToAllowedTelemetryFields(
  payload: Record<string, unknown>,
): Record<string, unknown> {
  const allowed = new Set<string>(ALLOWED_TELEMETRY_KEYS);
  const banned = new Set<string>(BANNED_TELEMETRY_EGRESS_KEYS);
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([key]) => allowed.has(key) && !banned.has(key),
    ),
  );
}
