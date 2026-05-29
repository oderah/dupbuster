import type {
  ScanPhase,
  ScanProgressContentKind,
  ScanProgressEvent,
} from './scanEngine';

export type ScanProgressProps = {
  progress: ScanProgressEvent;
  /** Decoupled a11y label (from scanProgressA11y helpers or ScanSessionController). */
  accessibilityLabel: string;
  reducedMotion?: boolean;
  onPause?: () => void;
  onResume?: () => void;
  onCancel?: () => void;
  testID?: string;
};

export type ScanStatusChipProps = {
  phase: ScanPhase;
  testID?: string;
};

export type {ScanPhase, ScanProgressContentKind, ScanProgressEvent};
