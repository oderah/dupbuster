import type {CatalogMeta, ResumableScanRun} from '../types/scanEngine';

export function buildResumeSessionKey(scanRunId: number): string {
  return `run:${scanRunId}`;
}

export type ResumePromptSessionState = {
  dismissedForSession: boolean;
  lastSeenSessionKey: string | null;
};

export type ResumePromptPresentation = {
  resumeSessionKey: string;
  scanRunId: number;
  firstDisplayAlertEligible: boolean;
  dismissedForSession: boolean;
};

export function createResumePromptSessionState(): ResumePromptSessionState {
  return {
    dismissedForSession: false,
    lastSeenSessionKey: null,
  };
}

export function computeResumeFirstDisplayAlertEligible(
  resumeSessionKey: string,
  sessionState: ResumePromptSessionState,
): boolean {
  if (sessionState.dismissedForSession) {
    return false;
  }
  return sessionState.lastSeenSessionKey !== resumeSessionKey;
}

export function markResumePromptDisplayed(
  resumeSessionKey: string,
  sessionState: ResumePromptSessionState,
): ResumePromptSessionState {
  return {
    ...sessionState,
    lastSeenSessionKey: resumeSessionKey,
  };
}

export function dismissResumePromptForSession(
  sessionState: ResumePromptSessionState,
): ResumePromptSessionState {
  return {
    ...sessionState,
    dismissedForSession: true,
  };
}

/** FR-SI-03 / AC-integrity-resume-01 — offer resume or restart on relaunch. */
export function resolveResumePromptPresentation(
  resumable: ResumableScanRun | null,
  catalogMeta: CatalogMeta | null,
  phase: string,
  sessionState: ResumePromptSessionState = createResumePromptSessionState(),
): ResumePromptPresentation | null {
  if (resumable == null || phase !== 'idle') {
    return null;
  }
  if (catalogMeta?.fullRescanRequired) {
    return null;
  }

  const resumeSessionKey = buildResumeSessionKey(resumable.scanRunId);
  return {
    resumeSessionKey,
    scanRunId: resumable.scanRunId,
    firstDisplayAlertEligible: computeResumeFirstDisplayAlertEligible(
      resumeSessionKey,
      sessionState,
    ),
    dismissedForSession: sessionState.dismissedForSession,
  };
}
