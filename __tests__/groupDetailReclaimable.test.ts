import {resolveGroupDetailReclaimable} from '../src/controllers/groupDetailReclaimable';
import {
  applyKeeperPreset,
  createKeeperSelectionState,
  selectKeeperMember,
} from '../src/controllers/keeperSelection';
import {tokens} from '../src/tokens/tokens';
import type {DuplicateGroupDetail} from '../src/types/duplicateGroup';

describe('resolveGroupDetailReclaimable', () => {
  const group: Pick<DuplicateGroupDetail, 'members' | 'reclaimableBytesEst'> = {
    reclaimableBytesEst: 5_000_000,
    members: [
      {
        fileEntryId: 101,
        displayName: 'large.mp4',
        sizeBytes: 4_000_000,
        mtimeMs: 2000,
        pathLength: 10,
        mediaTypeHint: 'video',
        paths: [],
      },
      {
        fileEntryId: 102,
        displayName: 'small.mp4',
        sizeBytes: 1_000_000,
        mtimeMs: 1000,
        pathLength: 10,
        mediaTypeHint: 'video',
        paths: [],
      },
    ],
  };

  it('uses scan-time estimate before explicit keeper activation', () => {
    const selection = createKeeperSelectionState(group.members);
    const result = resolveGroupDetailReclaimable(group, selection);

    expect(result.usesKeeperSelection).toBe(false);
    expect(result.reclaimableBytes).toBe(5_000_000);
    expect(result.reclaimableLine).toContain(tokens.reclaimable.label.split('{')[0]!);
  });

  it('uses sum of non-keeper sizes after explicit keeper pick (AC-action-reclaim-01)', () => {
    const selection = selectKeeperMember(
      createKeeperSelectionState(group.members),
      101,
    );
    const result = resolveGroupDetailReclaimable(group, selection);

    expect(result.usesKeeperSelection).toBe(true);
    expect(result.reclaimableBytes).toBe(1_000_000);
  });

  it('updates reclaimable when preset changes keeper choice', () => {
    const selection = applyKeeperPreset(
      createKeeperSelectionState(group.members),
      'smallest_file',
      group.members,
    );
    const result = resolveGroupDetailReclaimable(group, selection);

    expect(result.usesKeeperSelection).toBe(true);
    expect(result.reclaimableBytes).toBe(4_000_000);
  });
});
