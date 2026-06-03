import fs from 'fs';
import path from 'path';

import {
  BANNED_TELEMETRY_EGRESS_KEYS,
  ALLOWED_TELEMETRY_KEYS,
  filterToAllowedTelemetryFields,
} from '../src/telemetry/telemetryAllowedFields';
import {DEFAULT_SCAN_SETTINGS} from '../src/settings/scanSettingsStore';
import {tokens} from '../src/tokens/tokens';

const repoRoot = path.join(__dirname, '..');

describe('crash analytics contract (M4-13)', () => {
  it('default scan settings keep crash analytics opt-in off (US-17)', () => {
    expect(DEFAULT_SCAN_SETTINGS.crashAnalyticsOptIn).toBe(false);
  });

  it('settings.crashAnalytics token is frozen', () => {
    expect(tokens.settings.crashAnalytics).toBe(
      'Send anonymous crash reports (no file paths or content)',
    );
  });

  it('unscannable_reason is banned from telemetry egress', () => {
    expect(BANNED_TELEMETRY_EGRESS_KEYS).toContain('unscannable_reason');
  });

  it('filterToAllowedTelemetryFields drops banned keys', () => {
    const filtered = filterToAllowedTelemetryFields({
      scan_run_id: 1,
      phase: 'hashing',
      unscannable_reason: 'PERMISSION_DENIED',
      uri_or_path: '/storage/emulated/0/a.jpg',
      frame_hashes_blob: 'deadbeef',
    });
    expect(filtered).toEqual({
      scan_run_id: 1,
      phase: 'hashing',
    });
  });

  it('native Android egress defaults off in ScanTelemetryEgress', () => {
    const source = fs.readFileSync(
      path.join(
        repoRoot,
        'android/app/src/main/java/com/dupbuster/scanengine/security/ScanTelemetryEgress.kt',
      ),
      'utf8',
    );
    expect(source).toMatch(/crashAnalyticsOptIn: Boolean = false/);
    expect(source).toMatch(/if \(!crashAnalyticsOptIn\)/);
  });

  it('bridge exposes setCrashAnalyticsOptIn on NativeScanEngine spec', () => {
    const spec = fs.readFileSync(
      path.join(repoRoot, 'src/native/NativeScanEngine.ts'),
      'utf8',
    );
    expect(spec).toContain('setCrashAnalyticsOptIn(enabled: boolean)');
  });

  it('allowed telemetry keys match architecture §8.2', () => {
    expect([...ALLOWED_TELEMETRY_KEYS]).toEqual(
      expect.arrayContaining([
        'scan_run_id',
        'phase',
        'schema_version',
        'teardown_reason',
        'platform_api_level',
        'exception_type',
      ]),
    );
  });
});
