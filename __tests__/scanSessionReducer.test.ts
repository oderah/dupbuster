import {
  createInitialScanSessionState,
  reduceScanSessionDismissCoverage,
  reduceScanSessionDismissRescanPrompt,
  reduceScanSessionOnCatalogLoaded,
  reduceScanSessionOnCatalogMetaLoaded,
  reduceScanSessionOnProgress,
  reduceScanSessionOnScanError,
  reduceScanSessionOpenGroup,
  reduceScanSessionPrepareDeleteAttempt,
  reduceScanSessionSetCoverageVariant,
  shouldRefreshCatalogAfterProgress,
} from '../src/controllers/scanSessionReducer';
import {createMockCatalogSnapshot} from '../src/native/scanEnginePort';
import {tokens} from '../src/tokens/tokens';
import type {ScanProgressEvent} from '../src/types/scanEngine';

describe('scanSessionReducer', () => {
  it('tracks progress phase and decoupled a11y label (A11Y-04/05)', () => {
    let state = createInitialScanSessionState();
    state = reduceScanSessionOnProgress(state, {
      filesProcessed: 0,
      filesTotalKnown: 100,
      groupsFound: 0,
      reclaimableBytesEst: 0,
      phase: 'discovering',
    });
    expect(state.phase).toBe('discovering');
    expect(state.progressAccessibilityLabel).toBe(tokens.a11y.scan.discovering);

    state = reduceScanSessionOnProgress(state, {
      filesProcessed: 500,
      filesTotalKnown: 1000,
      groupsFound: 1,
      reclaimableBytesEst: 100,
      phase: 'hashing',
    });
    expect(state.progressAnnouncement).toContain('50 percent');
  });

  it('escalates coverage to denied on PERMISSION_DENIED error', () => {
    const state = createInitialScanSessionState({coverageVariant: 'partial'});
    const next = reduceScanSessionOnScanError(state, {
      fileEntryId: 42,
      unscannableReason: 'PERMISSION_DENIED',
    });
    expect(next.coverageVariant).toBe('denied');
    expect(next.coveragePresentation?.coverageSessionKey).toBe('denied');
    expect(next.unscannableCounts.PERMISSION_DENIED).toBe(1);
  });

  it('resets coverage session key on variant escalation (A11Y-03)', () => {
    let state = createInitialScanSessionState({coverageVariant: 'partial'});
    state = reduceScanSessionDismissCoverage(state);
    state = reduceScanSessionSetCoverageVariant(state, 'denied');
    expect(state.coveragePresentation?.firstDisplayAlertEligible).toBe(true);
  });

  it('loads catalog snapshot on terminal phases', () => {
    const catalog = createMockCatalogSnapshot();
    let state = createInitialScanSessionState();
    state = reduceScanSessionOnCatalogLoaded(state, catalog, {
      schemaVersion: 2,
      fullRescanRequired: false,
    });
    expect(state.duplicateGroups).toHaveLength(1);
    expect(state.unscannableCounts.HASH_TIMEOUT).toBe(1);
    expect(state.groupDetailsById[1]).toBeDefined();
    expect(state.rescanPresentation).toBeNull();
  });

  it('surfaces rescan prompt when catalog meta requires full rescan (FR-IX-05)', () => {
    let state = createInitialScanSessionState();
    state = reduceScanSessionOnCatalogMetaLoaded(state, {
      schemaVersion: 2,
      fullRescanRequired: true,
    });
    expect(state.rescanPresentation).toEqual({
      rescanSessionKey: 'schema:2',
      firstDisplayAlertEligible: true,
      dismissedForSession: false,
    });
    state = reduceScanSessionDismissRescanPrompt(state);
    expect(state.rescanPresentation?.dismissedForSession).toBe(true);
  });

  it('applies remember-largest preset when opening group detail (FR-AC-05)', () => {
    const catalog = createMockCatalogSnapshot();
    let state = createInitialScanSessionState();
    state = {
      ...reduceScanSessionOnCatalogLoaded(state, catalog, null),
      rememberLargestForSession: true,
    };
    state = reduceScanSessionOpenGroup(state, 1);
    const selection = state.keeperSelectionsByGroupId[1];
    expect(selection?.explicitlyActivated).toBe(true);
    expect(selection?.selectedFileEntryId).toBe(101);
  });

  it('shows keeper education once per session (AC-action-keeper-02)', () => {
    let state = createInitialScanSessionState();
    state = reduceScanSessionPrepareDeleteAttempt(state);
    expect(state.keeperEducationVisible).toBe(true);
    state = {
      ...state,
      keeperEducationSession: {educationShown: true},
      keeperEducationVisible: false,
    };
    state = reduceScanSessionPrepareDeleteAttempt(state);
    expect(state.keeperEducationVisible).toBe(false);
  });

  it('identifies terminal phases for catalog refresh', () => {
    const complete: ScanProgressEvent = {
      filesProcessed: 1,
      filesTotalKnown: 1,
      groupsFound: 1,
      reclaimableBytesEst: 1,
      phase: 'complete',
    };
    expect(shouldRefreshCatalogAfterProgress(complete)).toBe(true);
    expect(
      shouldRefreshCatalogAfterProgress({...complete, phase: 'hashing'}),
    ).toBe(false);
  });
});
