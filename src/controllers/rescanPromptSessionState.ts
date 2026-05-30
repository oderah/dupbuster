import type {CatalogMeta} from '../types/scanEngine';

export function buildRescanSessionKey(schemaVersion: number): string {
  return `schema:${schemaVersion}`;
}

export type RescanPromptSessionState = {
  dismissedForSession: boolean;
  lastSeenSessionKey: string | null;
};

export type RescanPromptPresentation = {
  rescanSessionKey: string;
  firstDisplayAlertEligible: boolean;
  dismissedForSession: boolean;
};

export function createRescanPromptSessionState(): RescanPromptSessionState {
  return {
    dismissedForSession: false,
    lastSeenSessionKey: null,
  };
}

export function computeRescanFirstDisplayAlertEligible(
  rescanSessionKey: string,
  sessionState: RescanPromptSessionState,
): boolean {
  if (sessionState.dismissedForSession) {
    return false;
  }
  return sessionState.lastSeenSessionKey !== rescanSessionKey;
}

export function markRescanPromptDisplayed(
  rescanSessionKey: string,
  sessionState: RescanPromptSessionState,
): RescanPromptSessionState {
  return {
    ...sessionState,
    lastSeenSessionKey: rescanSessionKey,
  };
}

export function dismissRescanPromptForSession(
  sessionState: RescanPromptSessionState,
): RescanPromptSessionState {
  return {
    ...sessionState,
    dismissedForSession: true,
  };
}

/** Resolves presentation when `catalogMeta.fullRescanRequired` is set (FR-IX-05). */
export function resolveRescanPromptPresentation(
  catalogMeta: CatalogMeta | null,
  sessionState: RescanPromptSessionState = createRescanPromptSessionState(),
): RescanPromptPresentation | null {
  if (catalogMeta == null || !catalogMeta.fullRescanRequired) {
    return null;
  }

  const rescanSessionKey = buildRescanSessionKey(catalogMeta.schemaVersion);
  return {
    rescanSessionKey,
    firstDisplayAlertEligible: computeRescanFirstDisplayAlertEligible(
      rescanSessionKey,
      sessionState,
    ),
    dismissedForSession: sessionState.dismissedForSession,
  };
}
