import {resolveScanProgressSubcopy} from '../src/components/scanProgressDisplay';
import {tokens} from '../src/tokens/tokens';
import type {ScanProgressEvent} from '../src/types/scanEngine';

function progress(overrides: Partial<ScanProgressEvent> = {}): ScanProgressEvent {
  return {
    filesProcessed: 10,
    filesTotalKnown: 100,
    groupsFound: 0,
    reclaimableBytesEst: 0,
    phase: 'hashing',
    ...overrides,
  };
}

describe('resolveScanProgressSubcopy (M2-14)', () => {
  it('returns frozen scan.phase.videoContent when hashing + video_content', () => {
    expect(
      resolveScanProgressSubcopy(
        progress({phase: 'hashing', contentKind: 'video_content'}),
      ),
    ).toBe(tokens.scan.phase.videoContent);
  });

  it('returns null for hashing without contentKind', () => {
    expect(resolveScanProgressSubcopy(progress({phase: 'hashing'}))).toBeNull();
  });

  it('returns null for hashing with contentKind none', () => {
    expect(
      resolveScanProgressSubcopy(
        progress({phase: 'hashing', contentKind: 'none'}),
      ),
    ).toBeNull();
  });

  it('returns null when video_content is not in hashing phase', () => {
    expect(
      resolveScanProgressSubcopy(
        progress({phase: 'discovering', contentKind: 'video_content'}),
      ),
    ).toBeNull();
    expect(
      resolveScanProgressSubcopy(
        progress({phase: 'grouping', contentKind: 'video_content'}),
      ),
    ).toBeNull();
  });
});
