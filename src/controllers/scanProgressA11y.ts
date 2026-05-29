import {formatToken, tokens} from '../tokens/tokens';
import type {ScanPhase, ScanProgressEvent} from '../types/scanEngine';

export const PROGRESS_FILE_DELTA = 500;
export const PROGRESS_PERCENT_STEP = 0.1;

const TERMINAL_PHASE_ANNOUNCEMENTS: Partial<Record<ScanPhase, string>> = {
  discovering: tokens.a11y.scan.discovering,
  complete: tokens.a11y.scan.complete,
  paused: tokens.a11y.scan.paused,
  error: tokens.a11y.scan.error,
  cancelling: tokens.a11y.scan.cancelling,
  cancelled: tokens.a11y.scan.cancelled,
};

export type ScanProgressA11yState = {
  lastAnnouncedFilesProcessed: number;
  lastPhase: ScanPhase | null;
  videoContentAnnouncedForHashingPass: boolean;
};

export function createScanProgressA11yState(): ScanProgressA11yState {
  return {
    lastAnnouncedFilesProcessed: 0,
    lastPhase: null,
    videoContentAnnouncedForHashingPass: false,
  };
}

export function computeScanPercent(
  filesProcessed: number,
  filesTotalKnown: number | null,
): number | null {
  if (filesTotalKnown == null || filesTotalKnown <= 0) {
    return null;
  }
  const ratio = filesProcessed / filesTotalKnown;
  return Math.min(100, Math.max(0, Math.round(ratio * 100)));
}

export function shouldAnnounceProgressDelta(
  previousFilesProcessed: number,
  filesProcessed: number,
  filesTotalKnown: number | null,
): boolean {
  const delta = filesProcessed - previousFilesProcessed;
  if (delta >= PROGRESS_FILE_DELTA) {
    return true;
  }
  if (filesTotalKnown == null || filesTotalKnown <= 0) {
    return false;
  }
  const threshold = Math.max(1, Math.floor(filesTotalKnown * PROGRESS_PERCENT_STEP));
  const previousBucket = Math.floor(previousFilesProcessed / threshold);
  const currentBucket = Math.floor(filesProcessed / threshold);
  return currentBucket > previousBucket;
}

export function resolvePhaseTerminalAnnouncement(
  previousPhase: ScanPhase | null,
  phase: ScanPhase,
): string | null {
  if (previousPhase === phase) {
    return null;
  }
  return TERMINAL_PHASE_ANNOUNCEMENTS[phase] ?? null;
}

export function formatProgressAnnouncement(progress: ScanProgressEvent): string {
  const percent = computeScanPercent(
    progress.filesProcessed,
    progress.filesTotalKnown,
  );
  return formatToken(tokens.a11y.scan.progress, {
    percent: percent ?? 0,
    filesProcessed: progress.filesProcessed,
    groupsFound: progress.groupsFound,
  });
}

export function buildScanProgressSummaryLabel(progress: ScanProgressEvent): string {
  return formatProgressAnnouncement(progress);
}

export function advanceScanProgressA11y(
  state: ScanProgressA11yState,
  progress: ScanProgressEvent,
): {
  state: ScanProgressA11yState;
  announcement: string | null;
  accessibilityLabel: string;
} {
  const previousPhase = state.lastPhase;
  let nextState: ScanProgressA11yState = {
    ...state,
    lastPhase: progress.phase,
  };

  if (previousPhase === 'hashing' && progress.phase !== 'hashing') {
    nextState = {...nextState, videoContentAnnouncedForHashingPass: false};
  }
  if (progress.phase === 'hashing' && previousPhase !== 'hashing') {
    nextState = {...nextState, videoContentAnnouncedForHashingPass: false};
  }

  const terminal = resolvePhaseTerminalAnnouncement(previousPhase, progress.phase);
  if (terminal) {
    nextState = {
      ...nextState,
      lastAnnouncedFilesProcessed: progress.filesProcessed,
      videoContentAnnouncedForHashingPass:
        progress.phase === 'hashing' ? nextState.videoContentAnnouncedForHashingPass : false,
    };
    return {
      state: nextState,
      announcement: terminal,
      accessibilityLabel: terminal,
    };
  }

  if (
    progress.phase === 'hashing' &&
    progress.contentKind === 'video_content' &&
    !nextState.videoContentAnnouncedForHashingPass
  ) {
    nextState = {
      ...nextState,
      videoContentAnnouncedForHashingPass: true,
      lastAnnouncedFilesProcessed: progress.filesProcessed,
    };
    return {
      state: nextState,
      announcement: tokens.a11y.scan.videoContent,
      accessibilityLabel: tokens.a11y.scan.videoContent,
    };
  }

  if (
    shouldAnnounceProgressDelta(
      state.lastAnnouncedFilesProcessed,
      progress.filesProcessed,
      progress.filesTotalKnown,
    )
  ) {
    const announcement = formatProgressAnnouncement(progress);
    nextState = {
      ...nextState,
      lastAnnouncedFilesProcessed: progress.filesProcessed,
    };
    return {
      state: nextState,
      announcement,
      accessibilityLabel: announcement,
    };
  }

  return {
    state: nextState,
    announcement: null,
    accessibilityLabel: buildScanProgressSummaryLabel(progress),
  };
}
