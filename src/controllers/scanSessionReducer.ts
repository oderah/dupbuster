import {toKeeperMembers} from '../components/keeperMembers';
import {
  createCoverageBannerSessionState,
  dismissCoverageBannerForSession,
  markCoverageBannerDisplayed,
  resolveCoverageBannerPresentation,
} from './coverageSessionState';
import {
  createRescanPromptSessionState,
  dismissRescanPromptForSession,
  markRescanPromptDisplayed,
  resolveRescanPromptPresentation,
} from './rescanPromptSessionState';
import {
  applyKeeperPreset,
  createKeeperSelectionState,
  selectKeeperMember,
} from './keeperSelection';
import {
  markKeeperEducationShown,
  shouldShowKeeperEducation,
} from './keeperEducationSession';
import {
  advanceScanProgressA11y,
  createScanProgressA11yState,
} from './scanProgressA11y';
import type {CoverageBannerVariant} from '../types/coverageBanner';
import type {KeeperPreset} from '../types/keeper';
import type {ScanCatalogSnapshot} from '../types/scanCatalog';
import type {CatalogMeta} from '../types/scanEngine';
import {TERMINAL_SCAN_PHASES} from '../types/scanEngine';
import type {ScanErrorEvent, ScanProgressEvent} from '../types/scanEngine';
import type {
  ScanSessionCoveragePresentation,
  ScanSessionState,
} from '../types/scanSession';

function isTerminalPhase(phase: ScanProgressEvent['phase']): boolean {
  return (TERMINAL_SCAN_PHASES as readonly string[]).includes(phase);
}

function resolveCoveragePresentation(
  variant: CoverageBannerVariant | null,
  limitedLibraryCount: number | undefined,
  session: ScanSessionState['coverageBannerSession'],
): ScanSessionCoveragePresentation | null {
  if (variant == null) {
    return null;
  }
  return resolveCoverageBannerPresentation(variant, {limitedLibraryCount}, session);
}

function applyProgressEvent(
  state: ScanSessionState,
  progress: ScanProgressEvent,
): ScanSessionState {
  const a11y = advanceScanProgressA11y(state.scanProgressA11y, progress);
  return {
    ...state,
    phase: progress.phase,
    progress,
    scanProgressA11y: a11y.state,
    progressAccessibilityLabel: a11y.accessibilityLabel,
    progressAnnouncement: a11y.announcement,
  };
}

function applyCatalogMeta(
  state: ScanSessionState,
  catalogMeta: CatalogMeta | null,
): ScanSessionState {
  return {
    ...state,
    catalogMeta,
    rescanPresentation: resolveRescanPromptPresentation(
      catalogMeta,
      state.rescanPromptSession,
    ),
  };
}

function mergeCatalogIntoState(
  state: ScanSessionState,
  catalog: ScanCatalogSnapshot,
  catalogMeta: CatalogMeta | null,
): ScanSessionState {
  return {
    ...applyCatalogMeta(state, catalogMeta),
    duplicateGroups: catalog.duplicateGroups,
    unscannableCounts: catalog.unscannableCounts,
    groupDetailsById: catalog.groupDetailsById,
    progress: {
      ...state.progress,
      groupsFound: catalog.duplicateGroups.length,
      reclaimableBytesEst: catalog.duplicateGroups.reduce(
        (sum, group) => sum + group.reclaimableBytesEst,
        0,
      ),
    },
  };
}

export function createInitialScanSessionState(
  options: {
    coverageVariant?: CoverageBannerVariant | null;
    limitedLibraryCount?: number;
  } = {},
): ScanSessionState {
  const coverageBannerSession = createCoverageBannerSessionState();
  const rescanPromptSession = createRescanPromptSessionState();
  const coverageVariant = options.coverageVariant ?? null;
  const a11y = createScanProgressA11yState();
  const progress = {
    filesProcessed: 0,
    filesTotalKnown: null,
    groupsFound: 0,
    reclaimableBytesEst: 0,
    phase: 'idle' as const,
  };
  const initialA11y = advanceScanProgressA11y(a11y, progress);

  return {
    phase: 'idle',
    scanRunId: null,
    progress,
    progressAccessibilityLabel: initialA11y.accessibilityLabel,
    progressAnnouncement: initialA11y.announcement,
    scanProgressA11y: initialA11y.state,
    coverageVariant,
    limitedLibraryCount: options.limitedLibraryCount,
    coverageBannerSession,
    coveragePresentation: resolveCoveragePresentation(
      coverageVariant,
      options.limitedLibraryCount,
      coverageBannerSession,
    ),
    rescanPromptSession,
    rescanPresentation: null,
    catalogMeta: null,
    duplicateGroups: [],
    unscannableCounts: {},
    selectedGroupId: null,
    groupDetailsById: {},
    keeperEducationSession: {educationShown: false},
    keeperSelectionsByGroupId: {},
    rememberLargestForSession: false,
    keeperEducationVisible: false,
    pendingDeleteGroupId: null,
    deleteConfirmVisible: false,
    deleteConfirmStep: 'review',
  };
}

export function reduceScanSessionOnScanStarted(
  state: ScanSessionState,
  scanRunId: number,
): ScanSessionState {
  return {
    ...state,
    scanRunId,
    selectedGroupId: null,
    keeperEducationVisible: false,
    pendingDeleteGroupId: null,
    deleteConfirmVisible: false,
    deleteConfirmStep: 'review',
  };
}

export function reduceScanSessionOnProgress(
  state: ScanSessionState,
  progress: ScanProgressEvent,
): ScanSessionState {
  return applyProgressEvent(state, progress);
}

export function reduceScanSessionOnCatalogLoaded(
  state: ScanSessionState,
  catalog: ScanCatalogSnapshot,
  catalogMeta: CatalogMeta | null,
): ScanSessionState {
  return mergeCatalogIntoState(state, catalog, catalogMeta);
}

export function reduceScanSessionOnCatalogMetaLoaded(
  state: ScanSessionState,
  catalogMeta: CatalogMeta | null,
): ScanSessionState {
  return applyCatalogMeta(state, catalogMeta);
}

export function reduceScanSessionOnScanError(
  state: ScanSessionState,
  error: ScanErrorEvent,
): ScanSessionState {
  const nextCounts = {
    ...state.unscannableCounts,
    [error.unscannableReason]:
      (state.unscannableCounts[error.unscannableReason] ?? 0) + 1,
  };

  let next = {
    ...state,
    unscannableCounts: nextCounts,
    scanRunId: error.scanRunId ?? state.scanRunId,
  };

  if (error.unscannableReason === 'PERMISSION_DENIED') {
    const coverageBannerSession = createCoverageBannerSessionState();
    next = {
      ...next,
      coverageVariant: 'denied',
      coverageBannerSession,
      coveragePresentation: resolveCoveragePresentation(
        'denied',
        next.limitedLibraryCount,
        coverageBannerSession,
      ),
    };
  }

  return next;
}

export function reduceScanSessionSetCoverageVariant(
  state: ScanSessionState,
  variant: CoverageBannerVariant | null,
  limitedLibraryCount?: number,
): ScanSessionState {
  const coverageBannerSession = createCoverageBannerSessionState();
  return {
    ...state,
    coverageVariant: variant,
    limitedLibraryCount,
    coverageBannerSession,
    coveragePresentation: resolveCoveragePresentation(
      variant,
      limitedLibraryCount,
      coverageBannerSession,
    ),
  };
}

export function reduceScanSessionDismissCoverage(
  state: ScanSessionState,
): ScanSessionState {
  const coverageBannerSession = dismissCoverageBannerForSession(
    state.coverageBannerSession,
  );
  return {
    ...state,
    coverageBannerSession,
    coveragePresentation: state.coveragePresentation
      ? {
          ...state.coveragePresentation,
          dismissedForSession: true,
          firstDisplayAlertEligible: false,
        }
      : null,
  };
}

export function reduceScanSessionDismissRescanPrompt(
  state: ScanSessionState,
): ScanSessionState {
  const rescanPromptSession = dismissRescanPromptForSession(
    state.rescanPromptSession,
  );
  return {
    ...state,
    rescanPromptSession,
    rescanPresentation: state.rescanPresentation
      ? {
          ...state.rescanPresentation,
          dismissedForSession: true,
          firstDisplayAlertEligible: false,
        }
      : null,
  };
}

export function reduceScanSessionMarkRescanPromptDisplayed(
  state: ScanSessionState,
): ScanSessionState {
  const rescanPromptSession = markRescanPromptDisplayed(
    state.rescanPresentation?.rescanSessionKey ?? '',
    state.rescanPromptSession,
  );
  const rescanPresentation = state.rescanPresentation
    ? {
        ...state.rescanPresentation,
        firstDisplayAlertEligible: false,
      }
    : null;
  return {
    ...state,
    rescanPromptSession,
    rescanPresentation,
  };
}

export function reduceScanSessionMarkCoverageDisplayed(
  state: ScanSessionState,
): ScanSessionState {
  const coverageBannerSession = markCoverageBannerDisplayed(
    state.coveragePresentation?.coverageSessionKey ?? '',
    state.coverageBannerSession,
  );
  const coveragePresentation = state.coveragePresentation
    ? {
        ...state.coveragePresentation,
        firstDisplayAlertEligible: false,
      }
    : null;
  return {
    ...state,
    coverageBannerSession,
    coveragePresentation,
  };
}

export function reduceScanSessionOpenGroup(
  state: ScanSessionState,
  groupId: number,
): ScanSessionState {
  const detail = state.groupDetailsById[groupId];
  if (!detail) {
    return state;
  }

  const existing =
    state.keeperSelectionsByGroupId[groupId] ??
    createKeeperSelectionState(detail.members);
  const keeperSelection = state.rememberLargestForSession
    ? applyKeeperPreset(existing, 'largest', toKeeperMembers(detail.members))
    : existing;

  return {
    ...state,
    selectedGroupId: groupId,
    keeperSelectionsByGroupId: {
      ...state.keeperSelectionsByGroupId,
      [groupId]: keeperSelection,
    },
  };
}

export function reduceScanSessionCloseGroup(
  state: ScanSessionState,
): ScanSessionState {
  return {
    ...state,
    selectedGroupId: null,
    keeperEducationVisible: false,
    pendingDeleteGroupId: null,
    deleteConfirmVisible: false,
    deleteConfirmStep: 'review',
  };
}

export function reduceScanSessionSelectKeeper(
  state: ScanSessionState,
  groupId: number,
  fileEntryId: number,
): ScanSessionState {
  const current =
    state.keeperSelectionsByGroupId[groupId] ??
    createKeeperSelectionState(
      state.groupDetailsById[groupId]?.members ?? [],
    );
  return {
    ...state,
    keeperSelectionsByGroupId: {
      ...state.keeperSelectionsByGroupId,
      [groupId]: selectKeeperMember(current, fileEntryId),
    },
  };
}

export function reduceScanSessionApplyKeeperPreset(
  state: ScanSessionState,
  groupId: number,
  preset: KeeperPreset,
): ScanSessionState {
  const detail = state.groupDetailsById[groupId];
  if (!detail) {
    return state;
  }
  const current =
    state.keeperSelectionsByGroupId[groupId] ??
    createKeeperSelectionState(detail.members);
  return {
    ...state,
    keeperSelectionsByGroupId: {
      ...state.keeperSelectionsByGroupId,
      [groupId]: applyKeeperPreset(
        current,
        preset,
        toKeeperMembers(detail.members),
      ),
    },
  };
}

export function reduceScanSessionSetRememberLargest(
  state: ScanSessionState,
  value: boolean,
): ScanSessionState {
  return {
    ...state,
    rememberLargestForSession: value,
  };
}

function openDeleteConfirmForGroup(
  state: ScanSessionState,
  groupId: number,
): ScanSessionState {
  return {
    ...state,
    pendingDeleteGroupId: null,
    deleteConfirmVisible: true,
    deleteConfirmStep: 'review',
    selectedGroupId: groupId,
  };
}

/** AC-action-keeper-02 + AC-action-delete-01 — education gate then two-step modal. */
export function reduceScanSessionBeginDeleteFlow(
  state: ScanSessionState,
  groupId: number,
): ScanSessionState {
  if (shouldShowKeeperEducation(state.keeperEducationSession)) {
    return {
      ...state,
      pendingDeleteGroupId: groupId,
      keeperEducationVisible: true,
    };
  }
  return openDeleteConfirmForGroup(state, groupId);
}

/** @deprecated Use reduceScanSessionBeginDeleteFlow — kept for reducer tests. */
export function reduceScanSessionPrepareDeleteAttempt(
  state: ScanSessionState,
): ScanSessionState {
  if (state.selectedGroupId == null) {
    return state;
  }
  return reduceScanSessionBeginDeleteFlow(state, state.selectedGroupId);
}

export function reduceScanSessionDismissKeeperEducation(
  state: ScanSessionState,
): ScanSessionState {
  const pendingGroupId = state.pendingDeleteGroupId;
  const next: ScanSessionState = {
    ...state,
    keeperEducationVisible: false,
    keeperEducationSession: markKeeperEducationShown(
      state.keeperEducationSession,
    ),
  };
  if (pendingGroupId != null) {
    return openDeleteConfirmForGroup(
      {...next, pendingDeleteGroupId: null},
      pendingGroupId,
    );
  }
  return next;
}

export function reduceScanSessionAdvanceDeleteConfirm(
  state: ScanSessionState,
): ScanSessionState {
  if (!state.deleteConfirmVisible) {
    return state;
  }
  return {
    ...state,
    deleteConfirmStep: 'confirm',
  };
}

export function reduceScanSessionGoBackDeleteConfirm(
  state: ScanSessionState,
): ScanSessionState {
  if (!state.deleteConfirmVisible) {
    return state;
  }
  return {
    ...state,
    deleteConfirmStep: 'review',
  };
}

export function reduceScanSessionCancelDeleteConfirm(
  state: ScanSessionState,
): ScanSessionState {
  return {
    ...state,
    deleteConfirmVisible: false,
    deleteConfirmStep: 'review',
    pendingDeleteGroupId: null,
  };
}

export function reduceScanSessionConfirmDelete(
  state: ScanSessionState,
): ScanSessionState {
  return {
    ...state,
    deleteConfirmVisible: false,
    deleteConfirmStep: 'review',
    pendingDeleteGroupId: null,
  };
}

export function shouldRefreshCatalogAfterProgress(
  progress: ScanProgressEvent,
): boolean {
  return isTerminalPhase(progress.phase);
}

