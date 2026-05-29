import {
  TERMINAL_SCAN_PHASES,
  UNSCANNABLE_REASONS,
  type ScanPhase,
  type UnscannableReason,
} from '../src/types/scanEngine';

describe('ScanEngine bridge types', () => {
  it('exports terminal phases aligned with architecture', () => {
    const phases: ScanPhase[] = ['complete', 'cancelled', 'error'];
    expect(TERMINAL_SCAN_PHASES).toEqual(phases);
  });

  it('keeps unscannable reasons as a closed set', () => {
    const reasons: UnscannableReason[] = [
      'CLOUD_PLACEHOLDER',
      'ENCRYPTED',
      'PERMISSION_DENIED',
      'OFFLINE_ONLY',
      'LOCKED',
      'LARGE_SKIPPED',
      'HASH_TIMEOUT',
      'VIDEO_DECODE_FAILED',
    ];
    expect(reasons).toHaveLength(8);
    expect(UNSCANNABLE_REASONS).toEqual(reasons);
  });
});
