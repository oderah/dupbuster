import {pickDirectory} from '@react-native-documents/picker';
import {AppState, Linking, Platform} from 'react-native';
import {
  checkMultiple,
  openPhotoPicker,
  PERMISSIONS,
  requestMultiple,
  RESULTS,
  type Permission,
  type PermissionStatus,
} from 'react-native-permissions';

import type {ScanRootInput} from '../types/scanEngine';
import type {
  PlatformDiscoveryAccess,
  ScanPermissionPort,
  ScanPermissionSnapshot,
} from '../types/scanPermission';

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

function getAndroidMediaPermissions(): Permission[] {
  if (Platform.OS !== 'android') {
    return [];
  }
  if (Platform.Version >= 33) {
    return [
      PERMISSIONS.ANDROID.READ_MEDIA_IMAGES,
      PERMISSIONS.ANDROID.READ_MEDIA_VIDEO,
      PERMISSIONS.ANDROID.READ_MEDIA_AUDIO,
    ];
  }
  return [PERMISSIONS.ANDROID.READ_EXTERNAL_STORAGE];
}

function summarizeAndroidAccess(
  statuses: Record<string, PermissionStatus>,
): PlatformDiscoveryAccess {
  const values = Object.values(statuses);
  if (values.length === 0) {
    return 'denied';
  }
  if (values.every(status => status === RESULTS.GRANTED)) {
    return 'granted';
  }
  if (values.some(status => status === RESULTS.BLOCKED)) {
    return 'blocked';
  }
  if (values.some(status => status === RESULTS.GRANTED)) {
    return 'granted';
  }
  if (values.some(status => status === RESULTS.DENIED)) {
    return 'denied';
  }
  return 'denied';
}

async function queryPlatformDiscoveryAccess(): Promise<PlatformDiscoveryAccess> {
  if (Platform.OS === 'ios') {
    const status = await checkMultiple([PERMISSIONS.IOS.PHOTO_LIBRARY]).then(
      result => result[PERMISSIONS.IOS.PHOTO_LIBRARY],
    );
    if (status === RESULTS.GRANTED) {
      return 'granted';
    }
    if (status === RESULTS.LIMITED) {
      return 'limited';
    }
    if (status === RESULTS.BLOCKED) {
      return 'blocked';
    }
    return 'denied';
  }

  const permissions = getAndroidMediaPermissions();
  if (permissions.length === 0) {
    return 'denied';
  }
  const statuses = await checkMultiple(permissions);
  return summarizeAndroidAccess(statuses);
}

async function requestPlatformDiscoveryAccessInternal(): Promise<PlatformDiscoveryAccess> {
  if (Platform.OS === 'ios') {
    const status = await requestMultiple([PERMISSIONS.IOS.PHOTO_LIBRARY]).then(
      result => result[PERMISSIONS.IOS.PHOTO_LIBRARY],
    );
    if (status === RESULTS.GRANTED) {
      return 'granted';
    }
    if (status === RESULTS.LIMITED) {
      return 'limited';
    }
    if (status === RESULTS.BLOCKED) {
      return 'blocked';
    }
    return 'denied';
  }

  const permissions = getAndroidMediaPermissions();
  if (permissions.length === 0) {
    return 'denied';
  }
  const statuses = await requestMultiple(permissions);
  return summarizeAndroidAccess(statuses);
}

/** Production permission port — drives CoverageBanner + scan start (M2-08). */
export function createNativeScanPermissionPort(): ScanPermissionPort {
  const platform = Platform.OS === 'ios' ? 'ios' : 'android';
  let additionalFolderGrants: ScanRootInput[] = [];

  async function buildSnapshot(): Promise<ScanPermissionSnapshot> {
    const platformDiscovery = await queryPlatformDiscoveryAccess();
    return {
      platform,
      platformDiscovery,
      limitedLibraryCount:
        platformDiscovery === 'limited' ? 0 : undefined,
      additionalFolderGrants: [...additionalFolderGrants],
    };
  }

  async function pickFolderGrant(): Promise<ScanPermissionSnapshot> {
    const result = await pickDirectory({requestLongTermAccess: true});
    additionalFolderGrants = dedupeFolderGrants([
      ...additionalFolderGrants,
      {uriGrant: result.uri},
    ]);
    return buildSnapshot();
  }

  return {
    getSnapshot: buildSnapshot,
    async requestPlatformDiscoveryAccess() {
      await requestPlatformDiscoveryAccessInternal();
      return buildSnapshot();
    },
    pickAdditionalFolder: pickFolderGrant,
    async expandPhotoLibraryAccess() {
      if (Platform.OS === 'ios') {
        await openPhotoPicker();
      } else {
        await pickFolderGrant();
      }
      return buildSnapshot();
    },
    async openAppSettings() {
      await Linking.openSettings();
    },
    addAppStateListener(listener) {
      const subscription = AppState.addEventListener('change', nextState => {
        if (nextState === 'active') {
          listener();
        }
      });
      return () => subscription.remove();
    },
  };
}
