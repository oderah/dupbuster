import {formatToken, tokens} from '../tokens/tokens';
import {
  UNSCANNABLE_REASONS,
  type UnscannableReason,
} from '../types/scanEngine';
import type {
  UnscannableCountsByReason,
  UnscannableSummaryRow,
} from '../types/unscannableSummary';

const REASON_LABELS: Record<UnscannableReason, string> = {
  CLOUD_PLACEHOLDER: tokens.unscan.reason.CLOUD_PLACEHOLDER,
  ENCRYPTED: tokens.unscan.reason.ENCRYPTED,
  PERMISSION_DENIED: tokens.unscan.reason.PERMISSION_DENIED,
  OFFLINE_ONLY: tokens.unscan.reason.OFFLINE_ONLY,
  LOCKED: tokens.unscan.reason.LOCKED,
  LARGE_SKIPPED: tokens.unscan.reason.LARGE_SKIPPED,
  HASH_TIMEOUT: tokens.unscan.reason.HASH_TIMEOUT,
  VIDEO_DECODE_FAILED: tokens.unscan.reason.VIDEO_DECODE_FAILED,
};

export function getUnscannableReasonLabel(reason: UnscannableReason): string {
  return REASON_LABELS[reason];
}

export function resolveUnscannableRowCta(
  reason: UnscannableReason,
): UnscannableSummaryRow['ctaAction'] {
  if (reason === 'HASH_TIMEOUT') {
    return 'retry';
  }
  if (reason === 'LARGE_SKIPPED') {
    return 'enableLargeFiles';
  }
  return null;
}

export function formatUnscannableRowLabel(
  reason: UnscannableReason,
  count: number,
): string {
  return formatToken(tokens.unscan.row, {
    count,
    reason: getUnscannableReasonLabel(reason),
  });
}

export function buildUnscannableSummaryRows(
  countsByReason: UnscannableCountsByReason,
): UnscannableSummaryRow[] {
  return UNSCANNABLE_REASONS.flatMap(reason => {
    const count = countsByReason[reason] ?? 0;
    if (count <= 0) {
      return [];
    }
    const ctaAction = resolveUnscannableRowCta(reason);
    const ctaLabel =
      ctaAction === 'retry'
        ? tokens.unscan.retry
        : ctaAction === 'enableLargeFiles'
          ? tokens.unscan.cta.enableLargeFiles
          : null;
    const label = formatUnscannableRowLabel(reason, count);
    return [
      {
        reason,
        count,
        label,
        accessibilityLabel: label,
        ctaLabel,
        ctaAction,
      },
    ];
  });
}

export function hasUnscannableSummary(
  countsByReason: UnscannableCountsByReason,
): boolean {
  return buildUnscannableSummaryRows(countsByReason).length > 0;
}
