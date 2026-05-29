import {
  advanceScanProgressA11y,
  computeScanPercent,
  createScanProgressA11yState,
  formatProgressAnnouncement,
  resolvePhaseTerminalAnnouncement,
  shouldAnnounceProgressDelta,
} from '../src/controllers/scanProgressA11y';
import {tokens} from '../src/tokens/tokens';
import type {ScanProgressEvent} from '../src/types/scanEngine';

function progress(
  overrides: Partial<ScanProgressEvent> = {},
): ScanProgressEvent {
  return {
    filesProcessed: 0,
    filesTotalKnown: 1000,
    groupsFound: 0,
    reclaimableBytesEst: 0,
    phase: 'hashing',
    ...overrides,
  };
}

describe('computeScanPercent', () => {
  it('returns null when total unknown', () => {
    expect(computeScanPercent(50, null)).toBeNull();
  });

  it('returns rounded percent capped at 100', () => {
    expect(computeScanPercent(250, 1000)).toBe(25);
    expect(computeScanPercent(1500, 1000)).toBe(100);
  });
});

describe('shouldAnnounceProgressDelta (A11Y-04)', () => {
  it('announces on 500-file delta', () => {
    expect(shouldAnnounceProgressDelta(0, 500, 10_000)).toBe(true);
    expect(shouldAnnounceProgressDelta(100, 599, 10_000)).toBe(false);
  });

  it('announces on 10% of filesTotalKnown bucket crossing', () => {
    expect(shouldAnnounceProgressDelta(0, 100, 1000)).toBe(true);
    expect(shouldAnnounceProgressDelta(100, 199, 1000)).toBe(false);
    expect(shouldAnnounceProgressDelta(100, 200, 1000)).toBe(true);
  });
});

describe('resolvePhaseTerminalAnnouncement (A11Y-05)', () => {
  it('announces required terminal phases on transition', () => {
    expect(resolvePhaseTerminalAnnouncement(null, 'discovering')).toBe(
      tokens.a11y.scan.discovering,
    );
    expect(resolvePhaseTerminalAnnouncement('hashing', 'complete')).toBe(
      tokens.a11y.scan.complete,
    );
    expect(resolvePhaseTerminalAnnouncement('hashing', 'paused')).toBe(
      tokens.a11y.scan.paused,
    );
    expect(resolvePhaseTerminalAnnouncement('hashing', 'error')).toBe(
      tokens.a11y.scan.error,
    );
    expect(resolvePhaseTerminalAnnouncement('hashing', 'cancelling')).toBe(
      tokens.a11y.scan.cancelling,
    );
    expect(resolvePhaseTerminalAnnouncement('cancelling', 'cancelled')).toBe(
      tokens.a11y.scan.cancelled,
    );
  });

  it('does not repeat when phase unchanged', () => {
    expect(resolvePhaseTerminalAnnouncement('hashing', 'hashing')).toBeNull();
  });

  it('returns null for non-terminal active phases', () => {
    expect(resolvePhaseTerminalAnnouncement('discovering', 'hashing')).toBeNull();
    expect(resolvePhaseTerminalAnnouncement('hashing', 'grouping')).toBeNull();
  });
});

describe('advanceScanProgressA11y (AC-a11y-scan-01)', () => {
  it('prioritizes terminal phase announcement', () => {
    const state = createScanProgressA11yState();
    const result = advanceScanProgressA11y(
      state,
      progress({phase: 'complete', filesProcessed: 900}),
    );
    expect(result.announcement).toBe(tokens.a11y.scan.complete);
    expect(result.accessibilityLabel).toBe(tokens.a11y.scan.complete);
  });

  it('announces video content once per hashing entry (AC-a11y-match-05)', () => {
    let state = createScanProgressA11yState();
    const first = advanceScanProgressA11y(
      state,
      progress({phase: 'hashing', contentKind: 'video_content', filesProcessed: 10}),
    );
    expect(first.announcement).toBe(tokens.a11y.scan.videoContent);

    const second = advanceScanProgressA11y(
      first.state,
      progress({phase: 'hashing', contentKind: 'video_content', filesProcessed: 20}),
    );
    expect(second.announcement).toBeNull();
  });

  it('resets video content flag when leaving hashing', () => {
    let state = createScanProgressA11yState();
    state = advanceScanProgressA11y(
      state,
      progress({phase: 'hashing', contentKind: 'video_content'}),
    ).state;
    state = advanceScanProgressA11y(
      state,
      progress({phase: 'grouping'}),
    ).state;
    const again = advanceScanProgressA11y(
      state,
      progress({phase: 'hashing', contentKind: 'video_content'}),
    );
    expect(again.announcement).toBe(tokens.a11y.scan.videoContent);
  });

  it('announces progress on cadence threshold', () => {
    const state = createScanProgressA11yState();
    const result = advanceScanProgressA11y(
      state,
      progress({phase: 'hashing', filesProcessed: 500, groupsFound: 2}),
    );
    expect(result.announcement).toBe(
      formatProgressAnnouncement(
        progress({phase: 'hashing', filesProcessed: 500, groupsFound: 2}),
      ),
    );
  });
});
