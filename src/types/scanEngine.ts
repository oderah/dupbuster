/**
 * ScanEngine bridge types and constants (safe for Jest — no native module load).
 */
export type {
  CatalogMeta,
  DeleteDuplicatesCommand,
  DeleteDuplicatesResult,
  ScanErrorEvent,
  ScanPhase,
  ScanProgressContentKind,
  ScanProgressEvent,
  ScanRootInput,
  ScanRootMode,
  ScanStartOptions,
  ScanStartResult,
  UnscannableReason,
} from '../native/scanEngineBridge.types';

export {TERMINAL_SCAN_PHASES} from '../native/scanEngineBridge.types';
