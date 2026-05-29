/**
 * ScanEngine bridge types and constants (safe for Jest — no native module load).
 */
export type {
  CatalogMeta,
  DeleteDuplicatesCommand,
  DeleteDuplicatesResult,
  MatchKind,
  MediaTypeHint,
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

export {
  DUPLICATE_GROUP_THUMBNAIL_GRID_MAX,
  MATCH_KINDS,
  MEDIA_TYPE_HINTS,
  TERMINAL_SCAN_PHASES,
  UNSCANNABLE_REASONS,
} from '../native/scanEngineBridge.types';
