import type {ScanRootInput} from '../types/scanEngine';
import type {
  PlatformDiscoveryAccess,
  ScanPermissionPort,
  ScanPermissionSnapshot,
} from '../types/scanPermission';

export type MockScanPermissionOptions = {
  platform?: 'android' | 'ios';
  platformDiscovery?: PlatformDiscoveryAccess;
  limitedLibraryCount?: number;
  additionalFolderGrants?: ScanRootInput[];
};

function dedupeFolderGrants(grants: ScanRootInput[]): ScanRootInput[] {
  const seen = new Set<string>();
  const next: ScanRootInput[] = [];
  for (const grant of grants) {
    if (seen.has(grant.uriGrant)) {
      continue;
    }
    seen.add(grant.uriGrant);
    next.push(grant);
  }
  return next;
}

/** Deterministic permission snapshot for Jest and RN shell demos. */
export function createMockScanPermissionPort(
  options: MockScanPermissionOptions = {},
): ScanPermissionPort {
  const platform = options.platform ?? 'android';
  let snapshot: ScanPermissionSnapshot = {
    platform,
    platformDiscovery: options.platformDiscovery ?? 'granted',
    limitedLibraryCount: options.limitedLibraryCount,
    additionalFolderGrants: dedupeFolderGrants(
      options.additionalFolderGrants ?? [],
    ),
  };

  const listeners = new Set<() => void>();

  function emitChange(): ScanPermissionSnapshot {
    for (const listener of listeners) {
      listener();
    }
    return snapshot;
  }

  return {
    async getSnapshot() {
      return {...snapshot, additionalFolderGrants: [...snapshot.additionalFolderGrants]};
    },
    async requestPlatformDiscoveryAccess() {
      if (snapshot.platformDiscovery === 'denied') {
        snapshot = {...snapshot, platformDiscovery: 'granted'};
      }
      return emitChange();
    },
    async pickAdditionalFolder() {
      snapshot = {
        ...snapshot,
        additionalFolderGrants: dedupeFolderGrants([
          ...snapshot.additionalFolderGrants,
          {uriGrant: 'content://mock/tree/document'},
        ]),
      };
      return emitChange();
    },
    async expandPhotoLibraryAccess() {
      if (snapshot.platformDiscovery === 'limited') {
        snapshot = {
          ...snapshot,
          limitedLibraryCount: (snapshot.limitedLibraryCount ?? 0) + 1,
        };
      }
      return emitChange();
    },
    async openAppSettings() {
      return;
    },
    addAppStateListener(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}
