import type {ScanRootInput} from './scanEngine';

/** Platform discovery grant state for Mode B scans (requirements §5.2). */
export type PlatformDiscoveryAccess =
  | 'granted'
  | 'limited'
  | 'denied'
  | 'blocked';

export type ScanPermissionSnapshot = {
  platform: 'android' | 'ios';
  platformDiscovery: PlatformDiscoveryAccess;
  /** iOS limited library only — best-effort until native catalog exposes count. */
  limitedLibraryCount?: number;
  /** SAF tree URIs (Android) or security-scoped folder grants (iOS). */
  additionalFolderGrants: ScanRootInput[];
};

export type ScanPermissionUnsubscribe = () => void;

/** Injectable permission surface for ScanSessionController wiring (M2-08). */
export type ScanPermissionPort = {
  getSnapshot: () => Promise<ScanPermissionSnapshot>;
  requestPlatformDiscoveryAccess: () => Promise<ScanPermissionSnapshot>;
  pickAdditionalFolder: () => Promise<ScanPermissionSnapshot>;
  /** iOS limited library — `openPhotoPicker` from react-native-permissions. */
  expandPhotoLibraryAccess: () => Promise<ScanPermissionSnapshot>;
  openAppSettings: () => Promise<void>;
  addAppStateListener: (
    listener: () => void,
  ) => ScanPermissionUnsubscribe;
};
