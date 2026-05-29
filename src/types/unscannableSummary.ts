import type {UnscannableReason} from './scanEngine';

/** Aggregate unscannable counts keyed by reason (FR-UN-04). */
export type UnscannableCountsByReason = Partial<
  Record<UnscannableReason, number>
>;

export type UnscannableSummaryCardProps = {
  countsByReason: UnscannableCountsByReason;
  /** HASH_TIMEOUT — re-queue timed-out files (AC-unscan-04). */
  onRetryHashTimeout?: () => void;
  /** LARGE_SKIPPED — open settings / opt in to large file hashing (AC-unscan-03). */
  onEnableLargeFiles?: () => void;
  testID?: string;
};

export type UnscannableSummaryRow = {
  reason: UnscannableReason;
  count: number;
  label: string;
  accessibilityLabel: string;
  ctaLabel: string | null;
  ctaAction: 'retry' | 'enableLargeFiles' | null;
};
