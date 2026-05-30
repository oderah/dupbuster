import type {CoverageBannerVariant} from './coverageBanner';
import type {DuplicateGroupDetail, DuplicateGroupSummary} from './duplicateGroup';
import type {KeeperSelectionState} from './keeper';
import type {CatalogMeta, ScanPhase, ScanProgressEvent, ScanRootMode, ScanStartOptions} from './scanEngine';
import type {UnscannableCountsByReason} from './unscannableSummary';
import type {CoverageBannerSessionState} from '../controllers/coverageSessionState';
import type {ScanProgressA11yState} from '../controllers/scanProgressA11y';
import type {KeeperEducationSessionState} from '../controllers/keeperEducationSession';

export const IDLE_PROGRESS: ScanProgressEvent = {
  filesProcessed: 0,
  filesTotalKnown: null,
  groupsFound: 0,
  reclaimableBytesEst: 0,
  phase: 'idle',
};

export type ScanSessionCoveragePresentation = {
  coverageSessionKey: string;
  firstDisplayAlertEligible: boolean;
  dismissedForSession: boolean;
};

export type ScanSessionState = {
  phase: ScanPhase;
  scanRunId: number | null;
  progress: ScanProgressEvent;
  progressAccessibilityLabel: string;
  progressAnnouncement: string | null;
  scanProgressA11y: ScanProgressA11yState;
  coverageVariant: CoverageBannerVariant | null;
  limitedLibraryCount?: number;
  coverageBannerSession: CoverageBannerSessionState;
  coveragePresentation: ScanSessionCoveragePresentation | null;
  catalogMeta: CatalogMeta | null;
  duplicateGroups: DuplicateGroupSummary[];
  unscannableCounts: UnscannableCountsByReason;
  selectedGroupId: number | null;
  groupDetailsById: Readonly<Record<number, DuplicateGroupDetail>>;
  keeperEducationSession: KeeperEducationSessionState;
  keeperSelectionsByGroupId: Readonly<Record<number, KeeperSelectionState>>;
  rememberLargestForSession: boolean;
  keeperEducationVisible: boolean;
};

export type ScanSessionControllerOptions = {
  coverageVariant?: CoverageBannerVariant | null;
  limitedLibraryCount?: number;
};

export type ScanSessionStartRequest = {
  mode: ScanRootMode;
  roots: ScanStartOptions['roots'];
  resumeScanRunId?: number;
};
