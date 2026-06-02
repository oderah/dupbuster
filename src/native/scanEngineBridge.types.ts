/**
 * Jest-safe ScanEngine bridge types (no TurboModuleRegistry side effects).
 * Must stay aligned with [NativeScanEngine.ts] — codegen requires types in that file.
 */

export type ScanPhase =
  | 'idle'
  | 'discovering'
  | 'hashing'
  | 'grouping'
  | 'complete'
  | 'paused'
  | 'error'
  | 'cancelling'
  | 'cancelled';

export type ScanRootMode = 'user_selected' | 'platform_discovery';

export type UnscannableReason =
  | 'CLOUD_PLACEHOLDER'
  | 'ENCRYPTED'
  | 'PERMISSION_DENIED'
  | 'OFFLINE_ONLY'
  | 'LOCKED'
  | 'LARGE_SKIPPED'
  | 'HASH_TIMEOUT'
  | 'VIDEO_DECODE_FAILED';

/** Stable display order for UnscannableSummaryCard rows (FR-UN-02). */
export const UNSCANNABLE_REASONS = [
  'CLOUD_PLACEHOLDER',
  'ENCRYPTED',
  'PERMISSION_DENIED',
  'OFFLINE_ONLY',
  'LOCKED',
  'LARGE_SKIPPED',
  'HASH_TIMEOUT',
  'VIDEO_DECODE_FAILED',
] as const satisfies readonly UnscannableReason[];

export type ScanProgressContentKind = 'none' | 'video_content';

export type ScanProgressEvent = {
  filesProcessed: number;
  filesTotalKnown: number | null;
  groupsFound: number;
  reclaimableBytesEst: number;
  phase: ScanPhase;
  contentKind?: ScanProgressContentKind;
};

export type ScanErrorEvent = {
  fileEntryId: number;
  unscannableReason: UnscannableReason;
  scanRunId?: number;
};

export type ScanRootInput = {
  uriGrant: string;
  scanRootId?: number;
};

export type ScanStartOptions = {
  mode: ScanRootMode;
  roots: ScanRootInput[];
  resumeScanRunId?: number;
};

export type ScanStartResult = {
  scanRunId: number;
};

export type DeleteDuplicatesCommand = {
  groupId: number;
  keeperFileEntryId: number;
  deleteFileEntryIds: number[];
};

export type DeleteDuplicatesResult = {
  deletedCount: number;
  failedCount: number;
};

export type CatalogMeta = {
  schemaVersion: number;
  fullRescanRequired: boolean;
};

/** Catalog read bridge (Phase B) — aligned with [NativeScanEngine.ts]. */
export type CatalogSnapshotThumbnail = {
  fileEntryId: number;
  mediaTypeHint: MediaTypeHint;
  thumbnailUri?: string;
};

export type CatalogSnapshotGroupSummary = {
  groupId: number;
  matchKind: MatchKind;
  memberCount: number;
  reclaimableBytesEst: number;
  thumbnails: readonly CatalogSnapshotThumbnail[];
};

export type CatalogSnapshotMember = {
  fileEntryId: number;
  displayName: string;
  sizeBytes: number;
  mtimeMs: number;
  pathLength: number;
  mediaTypeHint: MediaTypeHint;
  thumbnailUri?: string;
  paths?: readonly string[];
};

export type CatalogSnapshotGroupDetail = {
  groupId: number;
  matchKind: MatchKind;
  memberCount: number;
  reclaimableBytesEst: number;
  members: readonly CatalogSnapshotMember[];
};

export type CatalogSnapshot = {
  duplicateGroups: readonly CatalogSnapshotGroupSummary[];
  unscannableCounts: Partial<Record<UnscannableReason, number>>;
  groupDetailsById: Readonly<Record<number, CatalogSnapshotGroupDetail>>;
};

export const TERMINAL_SCAN_PHASES = [
  'complete',
  'cancelled',
  'error',
] as const satisfies readonly ScanPhase[];

/** `duplicate_group.match_kind` values (architecture §5.2). */
export type MatchKind = 'EXACT_BYTES' | 'SAME_CONTENT_VIDEO';

export const MATCH_KINDS = [
  'EXACT_BYTES',
  'SAME_CONTENT_VIDEO',
] as const satisfies readonly MatchKind[];

/** Discovery media hint wire values (architecture §4.1 / M1-04). */
export type MediaTypeHint =
  | 'image'
  | 'video'
  | 'audio'
  | 'document'
  | 'text'
  | 'other';

export const MEDIA_TYPE_HINTS = [
  'image',
  'video',
  'audio',
  'document',
  'text',
  'other',
] as const satisfies readonly MediaTypeHint[];

/** Max visible thumbnail cells in DuplicateGroupListItem (architecture §9.1). */
export const DUPLICATE_GROUP_THUMBNAIL_GRID_MAX = 4;
