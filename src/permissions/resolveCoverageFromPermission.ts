import type {CoverageBannerVariant} from '../types/coverageBanner';
import type {ScanPermissionSnapshot} from '../types/scanPermission';

export type ResolvedCoverageState = {
  variant: CoverageBannerVariant | null;
  limitedLibraryCount?: number;
};

function hasFolderFallback(snapshot: ScanPermissionSnapshot): boolean {
  return snapshot.additionalFolderGrants.length > 0;
}

/** Maps native permission snapshot → CoverageBanner variant (requirements §5.3). */
export function resolveCoverageFromPermission(
  snapshot: ScanPermissionSnapshot,
): ResolvedCoverageState {
  if (
    snapshot.platformDiscovery === 'blocked' ||
    (snapshot.platformDiscovery === 'denied' && !hasFolderFallback(snapshot))
  ) {
    return {variant: 'denied'};
  }

  if (snapshot.platformDiscovery === 'limited') {
    return {
      variant: 'limited-library',
      limitedLibraryCount: snapshot.limitedLibraryCount ?? 0,
    };
  }

  if (snapshot.platform === 'android') {
    return {variant: 'partial'};
  }

  if (snapshot.platformDiscovery === 'granted') {
    return {variant: null};
  }

  if (hasFolderFallback(snapshot)) {
    return {variant: 'partial'};
  }

  return {variant: 'denied'};
}
