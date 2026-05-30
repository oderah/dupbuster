import type {ScanCatalogSnapshot} from '../types/scanCatalog';
import {EMPTY_CATALOG_SNAPSHOT} from '../types/scanCatalog';
import type {
  CatalogMeta,
  ScanErrorEvent,
  ScanProgressEvent,
  ScanStartOptions,
  ScanStartResult,
} from '../types/scanEngine';

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

type NativeScanEngineModule = {
  onScanProgress: {
    addListener: (listener: (event: ScanProgressEvent) => void) => {
      remove: () => void;
    };
  };
  onScanError: {
    addListener: (listener: (event: ScanErrorEvent) => void) => {
      remove: () => void;
    };
  };
  startScan: ScanEnginePort['startScan'];
  pauseScan: ScanEnginePort['pauseScan'];
  resumeScan: ScanEnginePort['resumeScan'];
  cancelScan: ScanEnginePort['cancelScan'];
  getCatalogMeta: ScanEnginePort['getCatalogMeta'];
};

/** Wraps TurboModule; catalog snapshot empty until orchestrator exposes query APIs. */
export function createNativeScanEnginePort(
  module: NativeScanEngineModule,
): ScanEnginePort {
  return {
    addProgressListener(listener) {
      const subscription = module.onScanProgress.addListener(listener);
      return () => subscription.remove();
    },
    addErrorListener(listener) {
      const subscription = module.onScanError.addListener(listener);
      return () => subscription.remove();
    },
    startScan: options => module.startScan(options),
    pauseScan: scanRunId => module.pauseScan(scanRunId),
    resumeScan: scanRunId => module.resumeScan(scanRunId),
    cancelScan: scanRunId => module.cancelScan(scanRunId),
    getCatalogMeta: () => module.getCatalogMeta(),
    getCatalogSnapshot: async () => EMPTY_CATALOG_SNAPSHOT,
  };
}
