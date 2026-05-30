import type {ScanSessionStartRequest} from '../types/scanSession';
import type {ScanPermissionSnapshot} from '../types/scanPermission';

/** Builds a scan request from the current permission snapshot (US-01 / US-02). */
export function buildScanStartRequest(
  snapshot: ScanPermissionSnapshot,
): ScanSessionStartRequest | null {
  const canPlatformDiscover =
    snapshot.platformDiscovery === 'granted' ||
    snapshot.platformDiscovery === 'limited';

  if (canPlatformDiscover) {
    return {
      mode: 'platform_discovery',
      roots: [...snapshot.additionalFolderGrants],
    };
  }

  if (snapshot.additionalFolderGrants.length > 0) {
    return {
      mode: 'user_selected',
      roots: [...snapshot.additionalFolderGrants],
    };
  }

  return null;
}
