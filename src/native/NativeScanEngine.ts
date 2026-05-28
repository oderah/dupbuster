/**
 * Codegen Turbo Module spec for ScanEngine (M1-02).
 * Keep [scanEngineBridge.types.ts] in sync for Jest-safe type imports.
 * Bridge law: progress/error events never include paths, hashes, or file bytes.
 */
import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';
import type {
  Double,
  EventEmitter,
} from 'react-native/Libraries/Types/CodegenTypes';

/** Must match architecture §3.1 and ScanSessionController. */
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

export type ScanProgressContentKind = 'none' | 'video_content';

export type ScanProgressEvent = {
  filesProcessed: Double;
  filesTotalKnown: Double | null;
  groupsFound: Double;
  reclaimableBytesEst: Double;
  phase: ScanPhase;
  contentKind?: ScanProgressContentKind;
};

export type ScanErrorEvent = {
  fileEntryId: Double;
  unscannableReason: UnscannableReason;
  scanRunId?: Double;
};

export type ScanRootInput = {
  uriGrant: string;
  scanRootId?: Double;
};

export type ScanStartOptions = {
  mode: ScanRootMode;
  roots: ScanRootInput[];
  resumeScanRunId?: Double;
};

export type ScanStartResult = {
  scanRunId: Double;
};

export type DeleteDuplicatesCommand = {
  groupId: Double;
  keeperFileEntryId: Double;
  deleteFileEntryIds: Double[];
};

export type DeleteDuplicatesResult = {
  deletedCount: Double;
  failedCount: Double;
};

export type CatalogMeta = {
  schemaVersion: Double;
  fullRescanRequired: boolean;
};

export interface Spec extends TurboModule {
  readonly onScanProgress: EventEmitter<ScanProgressEvent>;
  readonly onScanError: EventEmitter<ScanErrorEvent>;

  startScan(options: ScanStartOptions): Promise<ScanStartResult>;
  pauseScan(scanRunId: Double): Promise<void>;
  resumeScan(scanRunId: Double): Promise<void>;
  cancelScan(scanRunId: Double): Promise<void>;
  deleteDuplicates(command: DeleteDuplicatesCommand): Promise<DeleteDuplicatesResult>;
  getCatalogMeta(): Promise<CatalogMeta>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeScanEngine');
