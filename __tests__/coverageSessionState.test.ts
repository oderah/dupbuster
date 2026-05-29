import {
  buildCoverageSessionKey,
  computeFirstDisplayAlertEligible,
  createCoverageBannerSessionState,
  dismissCoverageBannerForSession,
  markCoverageBannerDisplayed,
  resolveCoverageBannerPresentation,
} from '../src/controllers/coverageSessionState';

describe('buildCoverageSessionKey', () => {
  it('uses variant for partial and denied', () => {
    expect(buildCoverageSessionKey('partial')).toBe('partial');
    expect(buildCoverageSessionKey('denied')).toBe('denied');
  });

  it('includes count for limited-library', () => {
    expect(
      buildCoverageSessionKey('limited-library', {limitedLibraryCount: 42}),
    ).toBe('limited-library:42');
  });
});

describe('computeFirstDisplayAlertEligible', () => {
  it('is false for limited-library always (A11Y-02)', () => {
    const state = createCoverageBannerSessionState();
    expect(
      computeFirstDisplayAlertEligible('limited-library', 'limited-library:5', state),
    ).toBe(false);
  });

  it('is true on first partial/denied display per key (A11Y-01)', () => {
    const state = createCoverageBannerSessionState();
    expect(
      computeFirstDisplayAlertEligible('partial', 'partial', state),
    ).toBe(true);
  });

  it('is false on repeat same key', () => {
    let state = createCoverageBannerSessionState();
    state = markCoverageBannerDisplayed('partial', state);
    expect(
      computeFirstDisplayAlertEligible('partial', 'partial', state),
    ).toBe(false);
  });

  it('resets alert on variant escalation (A11Y-03)', () => {
    let state = createCoverageBannerSessionState();
    state = markCoverageBannerDisplayed('partial', state);
    expect(
      computeFirstDisplayAlertEligible('denied', 'denied', state),
    ).toBe(true);
  });

  it('is false when dismissed for session', () => {
    const state = dismissCoverageBannerForSession(
      createCoverageBannerSessionState(),
    );
    expect(
      computeFirstDisplayAlertEligible('partial', 'partial', state),
    ).toBe(false);
  });
});

describe('resolveCoverageBannerPresentation', () => {
  it('bundles key, alert eligibility, and dismiss flag for UI wiring', () => {
    const state = createCoverageBannerSessionState();
    expect(
      resolveCoverageBannerPresentation('limited-library', {limitedLibraryCount: 12}, state),
    ).toEqual({
      coverageSessionKey: 'limited-library:12',
      firstDisplayAlertEligible: false,
      dismissedForSession: false,
    });
  });
});
