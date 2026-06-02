import {
  createNativeScanEnginePort,
  mapCatalogSnapshot,
  normalizeScanErrorEvent,
  normalizeScanProgressEvent,
} from '../src/native/scanEnginePort';
import type {CatalogSnapshot} from '../src/types/scanEngine';

describe('normalizeScanProgressEvent', () => {
  it('maps native numeric fields and null filesTotalKnown', () => {
    const event = normalizeScanProgressEvent({
      filesProcessed: 12,
      filesTotalKnown: null,
      groupsFound: 1,
      reclaimableBytesEst: 500,
      phase: 'hashing',
      contentKind: 'video_content',
    });

    expect(event).toEqual({
      filesProcessed: 12,
      filesTotalKnown: null,
      groupsFound: 1,
      reclaimableBytesEst: 500,
      phase: 'hashing',
      contentKind: 'video_content',
    });
  });

  it('rejects invalid phases and contentKind values', () => {
    expect(
      normalizeScanProgressEvent({
        filesProcessed: 0,
        filesTotalKnown: 0,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'not-a-phase',
      }),
    ).toBeNull();
    expect(
      normalizeScanProgressEvent({
        filesProcessed: 0,
        filesTotalKnown: 0,
        groupsFound: 0,
        reclaimableBytesEst: 0,
        phase: 'hashing',
        contentKind: 'invalid',
      })?.contentKind,
    ).toBeUndefined();
  });
});

describe('normalizeScanErrorEvent', () => {
  it('maps bridge-safe error payloads', () => {
    expect(
      normalizeScanErrorEvent({
        fileEntryId: 42,
        unscannableReason: 'PERMISSION_DENIED',
        scanRunId: 7,
      }),
    ).toEqual({
      fileEntryId: 42,
      unscannableReason: 'PERMISSION_DENIED',
      scanRunId: 7,
    });
  });
});

describe('createNativeScanEnginePort', () => {
  it('subscribes via codegen EventEmitter callables', () => {
    let progressHandler: ((event: unknown) => void) | undefined;
    let errorHandler: ((event: unknown) => void) | undefined;
    const removeProgress = jest.fn();
    const removeError = jest.fn();

    const port = createNativeScanEnginePort({
      onScanProgress: listener => {
        progressHandler = listener;
        return {remove: removeProgress};
      },
      onScanError: listener => {
        errorHandler = listener;
        return {remove: removeError};
      },
      startScan: jest.fn(),
      pauseScan: jest.fn(),
      resumeScan: jest.fn(),
      cancelScan: jest.fn(),
      getCatalogMeta: jest.fn(),
      getCatalogSnapshot: jest.fn(),
    });

    const onProgress = jest.fn();
    const unsubscribeProgress = port.addProgressListener(onProgress);
    port.addErrorListener(() => {});

    expect(progressHandler).toBeDefined();
    expect(errorHandler).toBeDefined();

    progressHandler?.({
      filesProcessed: 1,
      filesTotalKnown: 10,
      groupsFound: 0,
      reclaimableBytesEst: 0,
      phase: 'discovering',
    });
    expect(onProgress).toHaveBeenCalledWith(
      expect.objectContaining({phase: 'discovering'}),
    );

    unsubscribeProgress();
    expect(removeProgress).toHaveBeenCalledTimes(1);
  });
});

describe('mapCatalogSnapshot', () => {
  it('maps native catalog payload into ScanCatalogSnapshot', () => {
    const raw: CatalogSnapshot = {
      duplicateGroups: [
        {
          groupId: 1,
          matchKind: 'SAME_CONTENT_VIDEO',
          memberCount: 2,
          reclaimableBytesEst: 500,
          thumbnails: [
            {
              fileEntryId: 10,
              mediaTypeHint: 'video',
              thumbnailUri: 'content://test/a.mp4',
            },
          ],
        },
      ],
      unscannableCounts: {
        HASH_TIMEOUT: 1,
        INVALID_REASON: 99,
      },
      groupDetailsById: {
        '1': {
          groupId: 1,
          matchKind: 'SAME_CONTENT_VIDEO',
          memberCount: 2,
          reclaimableBytesEst: 500,
          members: [
            {
              fileEntryId: 10,
              displayName: 'a.mp4',
              sizeBytes: 1000,
              mtimeMs: 2000,
              pathLength: 18,
              mediaTypeHint: 'video',
              thumbnailUri: 'content://test/a.mp4',
              paths: ['content://test/a.mp4'],
            },
          ],
        },
      },
    };

    const snapshot = mapCatalogSnapshot(raw);

    expect(snapshot.duplicateGroups).toHaveLength(1);
    expect(snapshot.duplicateGroups[0]?.matchKind).toBe('SAME_CONTENT_VIDEO');
    expect(snapshot.unscannableCounts.HASH_TIMEOUT).toBe(1);
    expect(snapshot.unscannableCounts.INVALID_REASON).toBeUndefined();
    expect(snapshot.groupDetailsById[1]?.members[0]?.displayName).toBe('a.mp4');
  });
});
