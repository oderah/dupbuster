import {formatToken, tokens} from '../tokens/tokens';
import type {ScanPhase, ScanProgressEvent} from '../types/scanEngine';
import {computeScanPercent} from '../controllers/scanProgressA11y';

const PHASE_LABELS: Record<ScanPhase, string> = {
  idle: tokens.scan.phase.idle,
  discovering: tokens.scan.phase.discovering,
  hashing: tokens.scan.phase.hashing,
  grouping: tokens.scan.phase.grouping,
  complete: tokens.scan.phase.complete,
  paused: tokens.scan.phase.paused,
  error: tokens.scan.phase.error,
  cancelling: tokens.scan.phase.cancelling,
  cancelled: tokens.scan.phase.cancelled,
};

const ACTIVE_PHASES = new Set<ScanPhase>([
  'discovering',
  'hashing',
  'grouping',
  'paused',
]);

export function getScanPhaseLabel(phase: ScanPhase): string {
  return PHASE_LABELS[phase];
}

export function shouldShowScanFooter(phase: ScanPhase): boolean {
  return ACTIVE_PHASES.has(phase);
}

export function shouldShowPauseControl(phase: ScanPhase): boolean {
  return phase === 'discovering' || phase === 'hashing' || phase === 'grouping';
}

export function shouldShowResumeControl(phase: ScanPhase): boolean {
  return phase === 'paused';
}

export function shouldShowCancelControl(phase: ScanPhase): boolean {
  return (
    phase === 'discovering' ||
    phase === 'hashing' ||
    phase === 'grouping' ||
    phase === 'paused'
  );
}

export function resolveScanProgressSubcopy(progress: ScanProgressEvent): string | null {
  if (
    progress.phase === 'hashing' &&
    progress.contentKind === 'video_content'
  ) {
    return tokens.scan.phase.videoContent;
  }
  return null;
}

export function formatScanProgressPercentLine(progress: ScanProgressEvent): string {
  const percent = computeScanPercent(
    progress.filesProcessed,
    progress.filesTotalKnown,
  );
  return formatToken(tokens.notification.scan.body, {
    percent: percent ?? 0,
    filesProcessed: progress.filesProcessed,
  });
}

export function formatScanProgressStats(progress: ScanProgressEvent): {
  groupsLine: string;
  filesLine: string;
} {
  return {
    groupsLine: formatToken(tokens.scan.stats.groups, {
      groupsFound: progress.groupsFound,
    }),
    filesLine: formatToken(tokens.scan.stats.files, {
      filesProcessed: progress.filesProcessed,
    }),
  };
}
