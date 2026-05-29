import {
  DUPLICATE_GROUP_THUMBNAIL_GRID_MAX,
  MATCH_KINDS,
  MEDIA_TYPE_HINTS,
  TERMINAL_SCAN_PHASES,
  UNSCANNABLE_REASONS,
  type MatchKind,
  type MediaTypeHint,
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

  it('keeps match kinds as a closed set', () => {
    const kinds: MatchKind[] = ['EXACT_BYTES', 'SAME_CONTENT_VIDEO'];
    expect(MATCH_KINDS).toEqual(kinds);
  });

  it('keeps media type hints aligned with discovery wire values', () => {
    const hints: MediaTypeHint[] = [
      'image',
      'video',
      'audio',
      'document',
      'text',
      'other',
    ];
    expect(MEDIA_TYPE_HINTS).toEqual(hints);
  });

  it('fixes duplicate group thumbnail grid max at 4', () => {
    expect(DUPLICATE_GROUP_THUMBNAIL_GRID_MAX).toBe(4);
  });
});
