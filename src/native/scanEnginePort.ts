import type {ScanCatalogSnapshot} from '../types/scanCatalog';
import type {DuplicateGroupDetail, DuplicateGroupMember, DuplicateGroupSummary, DuplicateGroupThumbnail} from '../types/duplicateGroup';
import type {
  CatalogMeta,
  CatalogSnapshot,
  CatalogSnapshotGroupDetail,
  CatalogSnapshotGroupSummary,
  CatalogSnapshotMember,
  CatalogSnapshotThumbnail,
  MatchKind,
  MediaTypeHint,
  ScanErrorEvent,
  ScanPhase,
  ScanProgressContentKind,
  ScanProgressEvent,
  ScanStartOptions,
  ScanStartResult,
  UnscannableReason,
} from '../types/scanEngine';
import {MATCH_KINDS, MEDIA_TYPE_HINTS, UNSCANNABLE_REASONS} from '../types/scanEngine';

export type ScanEngineUnsubscribe = () => void;

/** Injectable ScanEngine surface for ScanSessionController (bridge law preserved). */
export type ScanEnginePort = {
  addProgressListener: (
    listener: (event: ScanProgressEvent) => void,
  ) => ScanEngineUnsubscribe;
  addErrorListener: (
    listener: (event: ScanErrorEvent) => void,
  ) => ScanEngineUnsubscribe;
  startScan: (options: ScanStartOptions) => Promise<ScanStartResult>;
  pauseScan: (scanRunId: number) => Promise<void>;
  resumeScan: (scanRunId: number) => Promise<void>;
  cancelScan: (scanRunId: number) => Promise<void>;
  getCatalogMeta: () => Promise<CatalogMeta>;
  getCatalogSnapshot: () => Promise<ScanCatalogSnapshot>;
};

export type MockScanEngineOptions = {
  catalogSnapshot?: ScanCatalogSnapshot;
  catalogMeta?: CatalogMeta;
  /** When true, simulates a short scan on startScan (default true). */
  simulateScan?: boolean;
};

const DEFAULT_MOCK_CATALOG_META: CatalogMeta = {
  schemaVersion: 2,
  fullRescanRequired: false,
};

function delay(ms: number): Promise<void> {
  return new Promise(resolve => {
    setTimeout(resolve, ms);
  });
}

/** Deterministic mock catalog for RN shell until native orchestrator is live. */
export function createMockCatalogSnapshot(): ScanCatalogSnapshot {
  const videoGroupId = 1;
  const exactGroupId = 2;
  const videoMembers = [
    {
      fileEntryId: 101,
      displayName: 'vacation-1080p.mp4',
      sizeBytes: 4_000_000,
      mtimeMs: 2000,
      pathLength: 22,
      mediaTypeHint: 'video' as const,
    },
    {
      fileEntryId: 102,
      displayName: 'vacation-720p.mp4',
      sizeBytes: 1_000_000,
      mtimeMs: 1000,
      pathLength: 21,
      mediaTypeHint: 'video' as const,
    },
  ];
  const exactMembers = [
    {
      fileEntryId: 201,
      displayName: 'photo-copy.jpg',
      sizeBytes: 512_000,
      mtimeMs: 3000,
      pathLength: 18,
      mediaTypeHint: 'image' as const,
    },
    {
      fileEntryId: 202,
      displayName: 'photo-dup.jpg',
      sizeBytes: 512_000,
      mtimeMs: 2500,
      pathLength: 17,
      mediaTypeHint: 'image' as const,
    },
  ];
  const videoDetail = {
    groupId: videoGroupId,
    matchKind: 'SAME_CONTENT_VIDEO' as const,
    memberCount: 2,
    reclaimableBytesEst: 1_000_000,
    members: videoMembers,
  };
  const exactDetail = {
    groupId: exactGroupId,
    matchKind: 'EXACT_BYTES' as const,
    memberCount: 2,
    reclaimableBytesEst: 512_000,
    members: exactMembers,
  };
  return {
    duplicateGroups: [
      {
        groupId: videoGroupId,
        matchKind: 'SAME_CONTENT_VIDEO',
        memberCount: 2,
        reclaimableBytesEst: 1_000_000,
        thumbnails: videoMembers.map(member => ({
          fileEntryId: member.fileEntryId,
          mediaTypeHint: member.mediaTypeHint,
        })),
      },
      {
        groupId: exactGroupId,
        matchKind: 'EXACT_BYTES',
        memberCount: 2,
        reclaimableBytesEst: 512_000,
        thumbnails: exactMembers.map(member => ({
          fileEntryId: member.fileEntryId,
          mediaTypeHint: member.mediaTypeHint,
        })),
      },
    ],
    unscannableCounts: {
      HASH_TIMEOUT: 1,
      LARGE_SKIPPED: 2,
    },
    groupDetailsById: {
      [videoGroupId]: videoDetail,
      [exactGroupId]: exactDetail,
    },
  };
}

/** Mock progress feed matching bridge contract (≤4 Hz coalesce is native-side). */
export function createMockScanEnginePort(
  options: MockScanEngineOptions = {},
): ScanEnginePort {
  const catalogSnapshot = options.catalogSnapshot ?? createMockCatalogSnapshot();
  const catalogMeta = options.catalogMeta ?? DEFAULT_MOCK_CATALOG_META;
  const simulateScan = options.simulateScan ?? true;

  let scanRunSeq = 1;
  let activeRunId: number | null = null;
  let cancelled = false;
  let paused = false;
  const progressListeners = new Set<(event: ScanProgressEvent) => void>();
  const errorListeners = new Set<(event: ScanErrorEvent) => void>();

  function emitProgress(event: ScanProgressEvent): void {
    for (const listener of progressListeners) {
      listener(event);
    }
  }

  async function simulateRun(_scanRunId: number): Promise<void> {
    const steps: ScanProgressEvent[] = [
      {
        filesProcessed: 0,
        filesTotalKnown: 100,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'discovering',
      },
      {
        filesProcessed: 25,
        filesTotalKnown: 100,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'hashing',
      },
      {
        filesProcessed: 50,
        filesTotalKnown: 100,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'hashing',
        contentKind: 'video_content',
      },
      {
        filesProcessed: 75,
        filesTotalKnown: 100,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'grouping',
      },
      {
        filesProcessed: 100,
        filesTotalKnown: 100,
        groupsFound: 1,
        reclaimableBytesEst: 1_000_000,
        phase: 'complete',
      },
    ];

    for (const step of steps) {
      if (cancelled) {
        emitProgress({
          filesProcessed: step.filesProcessed,
          filesTotalKnown: step.filesTotalKnown,
          groupsFound: step.groupsFound,
          reclaimableBytesEst: step.reclaimableBytesEst,
          phase: 'cancelled',
        });
        activeRunId = null;
        return;
      }
      while (paused) {
        await delay(20);
        if (cancelled) {
          return;
        }
      }
      emitProgress({...step});
      await delay(10);
    }
    activeRunId = null;
  }

  return {
    addProgressListener(listener) {
      progressListeners.add(listener);
      return () => progressListeners.delete(listener);
    },
    addErrorListener(listener) {
      errorListeners.add(listener);
      return () => errorListeners.delete(listener);
    },
    async startScan(_options) {
      cancelled = false;
      paused = false;
      const scanRunId = scanRunSeq++;
      activeRunId = scanRunId;
      if (simulateScan) {
        simulateRun(scanRunId).catch(() => {});
      }
      return {scanRunId};
    },
    async pauseScan(scanRunId) {
      if (activeRunId === scanRunId) {
        paused = true;
        emitProgress({
          filesProcessed: 0,
          filesTotalKnown: 100,
          groupsFound: 0,
          reclaimableBytesEst: 0,
          phase: 'paused',
        });
      }
    },
    async resumeScan(scanRunId) {
      if (activeRunId === scanRunId && paused) {
        paused = false;
        emitProgress({
          filesProcessed: 50,
          filesTotalKnown: 100,
          groupsFound: 0,
          reclaimableBytesEst: 0,
          phase: 'hashing',
        });
      }
    },
    async cancelScan(scanRunId) {
      if (activeRunId === scanRunId) {
        cancelled = true;
        paused = false;
        emitProgress({
          filesProcessed: 0,
          filesTotalKnown: 100,
          groupsFound: 0,
          reclaimableBytesEst: 0,
          phase: 'cancelling',
        });
        await delay(5);
        emitProgress({
          filesProcessed: 0,
          filesTotalKnown: 100,
          groupsFound: 0,
          reclaimableBytesEst: 0,
          phase: 'cancelled',
        });
        activeRunId = null;
      }
    },
    async getCatalogMeta() {
      return catalogMeta;
    },
    async getCatalogSnapshot() {
      return catalogSnapshot;
    },
  };
}

function isMatchKind(value: string): value is MatchKind {
  return (MATCH_KINDS as readonly string[]).includes(value);
}

function isMediaTypeHint(value: string): value is MediaTypeHint {
  return (MEDIA_TYPE_HINTS as readonly string[]).includes(value);
}

function isUnscannableReason(value: string): value is UnscannableReason {
  return (UNSCANNABLE_REASONS as readonly string[]).includes(value);
}

function mapCatalogThumbnail(
  thumbnail: CatalogSnapshotThumbnail,
): DuplicateGroupThumbnail {
  return {
    fileEntryId: thumbnail.fileEntryId,
    mediaTypeHint: isMediaTypeHint(thumbnail.mediaTypeHint)
      ? thumbnail.mediaTypeHint
      : 'other',
    thumbnailUri: thumbnail.thumbnailUri ?? null,
  };
}

function mapCatalogMember(member: CatalogSnapshotMember): DuplicateGroupMember {
  return {
    fileEntryId: member.fileEntryId,
    displayName: member.displayName,
    sizeBytes: member.sizeBytes,
    mtimeMs: member.mtimeMs,
    pathLength: member.pathLength,
    mediaTypeHint: isMediaTypeHint(member.mediaTypeHint)
      ? member.mediaTypeHint
      : 'other',
    thumbnailUri: member.thumbnailUri ?? null,
  };
}

function mapCatalogGroupSummary(
  group: CatalogSnapshotGroupSummary,
): DuplicateGroupSummary {
  return {
    groupId: group.groupId,
    matchKind: isMatchKind(group.matchKind) ? group.matchKind : 'EXACT_BYTES',
    memberCount: group.memberCount,
    reclaimableBytesEst: group.reclaimableBytesEst,
    thumbnails: group.thumbnails.map(mapCatalogThumbnail),
  };
}

function mapCatalogGroupDetail(
  detail: CatalogSnapshotGroupDetail,
): DuplicateGroupDetail {
  return {
    groupId: detail.groupId,
    matchKind: isMatchKind(detail.matchKind) ? detail.matchKind : 'EXACT_BYTES',
    memberCount: detail.memberCount,
    reclaimableBytesEst: detail.reclaimableBytesEst,
    members: detail.members.map(mapCatalogMember),
  };
}

/** Maps native `getCatalogSnapshot` payload to [ScanCatalogSnapshot]. */
export function mapCatalogSnapshot(raw: CatalogSnapshot): ScanCatalogSnapshot {
  const unscannableCounts: ScanCatalogSnapshot['unscannableCounts'] = {};
  for (const [reason, count] of Object.entries(raw.unscannableCounts ?? {})) {
    if (typeof count !== 'number' || !isUnscannableReason(reason)) {
      continue;
    }
    unscannableCounts[reason] = count;
  }

  const groupDetailsById: Record<number, DuplicateGroupDetail> = {};
  const rawDetails = raw.groupDetailsById as Record<string, CatalogSnapshotGroupDetail>;
  for (const [groupIdKey, detail] of Object.entries(rawDetails ?? {})) {
    if (detail == null) {
      continue;
    }
    groupDetailsById[Number(groupIdKey)] = mapCatalogGroupDetail(detail);
  }

  return {
    duplicateGroups: (raw.duplicateGroups ?? []).map(mapCatalogGroupSummary),
    unscannableCounts,
    groupDetailsById,
  };
}

type NativeScanEngineEventSubscription = {
  remove?: () => void;
};

/** Codegen EventEmitter<T> is a callable listener registrar, not `{ addListener }`. */
type NativeScanEngineEventEmitter<T> = (
  listener: (event: T) => void,
) => NativeScanEngineEventSubscription;

const VALID_SCAN_PHASES = new Set<ScanPhase>([
  'idle',
  'discovering',
  'hashing',
  'grouping',
  'complete',
  'paused',
  'error',
  'cancelling',
  'cancelled',
]);

function coerceBridgeNumber(value: unknown, fallback: number): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return fallback;
}

/** Maps throttled native progress payloads to the JS contract (guards bridge shape drift). */
export function normalizeScanProgressEvent(raw: unknown): ScanProgressEvent | null {
  if (raw == null || typeof raw !== 'object') {
    return null;
  }
  const event = raw as Record<string, unknown>;
  if (typeof event.phase !== 'string' || !VALID_SCAN_PHASES.has(event.phase as ScanPhase)) {
    return null;
  }

  const filesTotalKnownRaw = event.filesTotalKnown;
  const contentKindRaw = event.contentKind;
  let contentKind: ScanProgressContentKind | undefined;
  if (contentKindRaw === 'none' || contentKindRaw === 'video_content') {
    contentKind = contentKindRaw;
  }

  return {
    filesProcessed: coerceBridgeNumber(event.filesProcessed, 0),
    filesTotalKnown:
      filesTotalKnownRaw == null
        ? null
        : coerceBridgeNumber(filesTotalKnownRaw, 0),
    groupsFound: coerceBridgeNumber(event.groupsFound, 0),
    reclaimableBytesEst: coerceBridgeNumber(event.reclaimableBytesEst, 0),
    phase: event.phase as ScanPhase,
    contentKind,
  };
}

/** Maps native error payloads to the JS contract. */
export function normalizeScanErrorEvent(raw: unknown): ScanErrorEvent | null {
  if (raw == null || typeof raw !== 'object') {
    return null;
  }
  const event = raw as Record<string, unknown>;
  if (
    typeof event.unscannableReason !== 'string' ||
    !isUnscannableReason(event.unscannableReason)
  ) {
    return null;
  }
  const fileEntryId = coerceBridgeNumber(event.fileEntryId, NaN);
  if (!Number.isFinite(fileEntryId)) {
    return null;
  }

  const normalized: ScanErrorEvent = {
    fileEntryId,
    unscannableReason: event.unscannableReason,
  };
  if (event.scanRunId != null) {
    const scanRunId = coerceBridgeNumber(event.scanRunId, NaN);
    if (Number.isFinite(scanRunId)) {
      normalized.scanRunId = scanRunId;
    }
  }
  return normalized;
}

function subscribeNativeModuleEvent<T>(
  register: unknown,
  listener: (event: T) => void,
  normalize: (raw: unknown) => T | null,
): () => void {
  const dispatch = (raw: unknown) => {
    const event = normalize(raw);
    if (event != null) {
      listener(event);
    }
  };

  if (typeof register === 'function') {
    const subscription = (register as NativeScanEngineEventEmitter<T>)(dispatch);
    return () => subscription?.remove?.();
  }

  if (
    register != null &&
    typeof register === 'object' &&
    typeof (register as {addListener?: unknown}).addListener === 'function'
  ) {
    const subscription = (
      register as {
        addListener: (handler: (event: T) => void) => NativeScanEngineEventSubscription;
      }
    ).addListener((event: T) => dispatch(event));
    return () => subscription?.remove?.();
  }

  throw new Error('NativeScanEngine event emitter is unavailable — rebuild the native app.');
}

type NativeScanEngineModule = {
  onScanProgress: NativeScanEngineEventEmitter<ScanProgressEvent>;
  onScanError: NativeScanEngineEventEmitter<ScanErrorEvent>;
  startScan: ScanEnginePort['startScan'];
  pauseScan: ScanEnginePort['pauseScan'];
  resumeScan: ScanEnginePort['resumeScan'];
  cancelScan: ScanEnginePort['cancelScan'];
  getCatalogMeta: ScanEnginePort['getCatalogMeta'];
  getCatalogSnapshot: () => Promise<CatalogSnapshot>;
};

/** Wraps TurboModule; catalog snapshot from native CatalogReader (Phase B). */
export function createNativeScanEnginePort(
  module: NativeScanEngineModule,
): ScanEnginePort {
  const startScan = module.startScan.bind(module);
  const pauseScan = module.pauseScan.bind(module);
  const resumeScan = module.resumeScan.bind(module);
  const cancelScan = module.cancelScan.bind(module);
  const getCatalogMeta = module.getCatalogMeta.bind(module);
  const getCatalogSnapshot = module.getCatalogSnapshot.bind(module);

  return {
    addProgressListener(listener) {
      return subscribeNativeModuleEvent(
        module.onScanProgress,
        listener,
        normalizeScanProgressEvent,
      );
    },
    addErrorListener(listener) {
      return subscribeNativeModuleEvent(
        module.onScanError,
        listener,
        normalizeScanErrorEvent,
      );
    },
    startScan: options => startScan(options),
    pauseScan: scanRunId => pauseScan(scanRunId),
    resumeScan: scanRunId => resumeScan(scanRunId),
    cancelScan: scanRunId => cancelScan(scanRunId),
    getCatalogMeta: () => getCatalogMeta(),
    getCatalogSnapshot: async () => {
      const snapshot = await getCatalogSnapshot();
      return mapCatalogSnapshot(snapshot);
    },
  };
}
