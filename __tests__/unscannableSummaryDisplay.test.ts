import {
  buildUnscannableSummaryRows,
  formatUnscannableRowLabel,
  getUnscannableReasonLabel,
  hasUnscannableSummary,
  resolveUnscannableRowCta,
} from '../src/components/unscannableSummaryDisplay';
import {UNSCANNABLE_REASONS} from '../src/types/scanEngine';
import {tokens} from '../src/tokens/tokens';

describe('unscannableSummaryDisplay', () => {
  it('covers every closed reason code with a label (AC-unscan-01)', () => {
    for (const reason of UNSCANNABLE_REASONS) {
      expect(getUnscannableReasonLabel(reason)).toBeTruthy();
    }
  });

  it('formats row copy from frozen tokens', () => {
    expect(formatUnscannableRowLabel('LARGE_SKIPPED', 3)).toBe(
      '3 files over 2 GB',
    );
  });

  it('assigns CTAs only for LARGE_SKIPPED and HASH_TIMEOUT', () => {
    expect(resolveUnscannableRowCta('HASH_TIMEOUT')).toBe('retry');
    expect(resolveUnscannableRowCta('LARGE_SKIPPED')).toBe('enableLargeFiles');
    expect(resolveUnscannableRowCta('ENCRYPTED')).toBeNull();
  });

  it('builds rows in stable order with non-zero counts only', () => {
    const rows = buildUnscannableSummaryRows({
      HASH_TIMEOUT: 2,
      CLOUD_PLACEHOLDER: 1,
      ENCRYPTED: 0,
    });
    expect(rows.map(row => row.reason)).toEqual([
      'CLOUD_PLACEHOLDER',
      'HASH_TIMEOUT',
    ]);
    expect(rows[1]?.ctaLabel).toBe(tokens.unscan.retry);
    expect(rows.find(row => row.reason === 'LARGE_SKIPPED')).toBeUndefined();
  });

  it('hasUnscannableSummary is false when all counts are zero', () => {
    expect(hasUnscannableSummary({})).toBe(false);
    expect(hasUnscannableSummary({LARGE_SKIPPED: 0})).toBe(false);
    expect(hasUnscannableSummary({LOCKED: 1})).toBe(true);
  });
});
