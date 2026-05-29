import type {CoverageBannerVariant} from '../types/coverageBanner';

/** Builds a session key that changes on variant escalation (A11Y-03). */
export function buildCoverageSessionKey(
  variant: CoverageBannerVariant,
  options?: {limitedLibraryCount?: number},
): string {
  if (variant === 'limited-library') {
    return `limited-library:${options?.limitedLibraryCount ?? 0}`;
  }
  return variant;
}

export type CoverageBannerSessionState = {
  dismissedForSession: boolean;
  lastSeenSessionKey: string | null;
};

export function createCoverageBannerSessionState(): CoverageBannerSessionState {
  return {
    dismissedForSession: false,
    lastSeenSessionKey: null,
  };
}

/**
 * Partial/denied: alert on first display per `coverageSessionKey` (A11Y-01).
 * Limited-library: never alert-eligible (A11Y-02).
 */
export function computeFirstDisplayAlertEligible(
  variant: CoverageBannerVariant,
  coverageSessionKey: string,
  sessionState: CoverageBannerSessionState,
): boolean {
  if (variant === 'limited-library') {
    return false;
  }
  if (sessionState.dismissedForSession) {
    return false;
  }
  return sessionState.lastSeenSessionKey !== coverageSessionKey;
}

export function markCoverageBannerDisplayed(
  coverageSessionKey: string,
  sessionState: CoverageBannerSessionState,
): CoverageBannerSessionState {
  return {
    ...sessionState,
    lastSeenSessionKey: coverageSessionKey,
  };
}

export function dismissCoverageBannerForSession(
  sessionState: CoverageBannerSessionState,
): CoverageBannerSessionState {
  return {
    ...sessionState,
    dismissedForSession: true,
  };
}

/** Resets first-display alert when coverage escalates mid-scan (A11Y-03). */
export function resolveCoverageBannerPresentation(
  variant: CoverageBannerVariant,
  options?: {limitedLibraryCount?: number},
  sessionState: CoverageBannerSessionState = createCoverageBannerSessionState(),
): {
  coverageSessionKey: string;
  firstDisplayAlertEligible: boolean;
  dismissedForSession: boolean;
} {
  const coverageSessionKey = buildCoverageSessionKey(variant, options);
  return {
    coverageSessionKey,
    firstDisplayAlertEligible: computeFirstDisplayAlertEligible(
      variant,
      coverageSessionKey,
      sessionState,
    ),
    dismissedForSession: sessionState.dismissedForSession,
  };
}
