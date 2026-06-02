import {resolveDeleteConfirmCounts} from '../src/controllers/deleteConfirmCounts';
import {selectKeeperMember} from '../src/controllers/keeperSelection';
import {createKeeperSelectionState} from '../src/controllers/keeperSelection';

describe('resolveDeleteConfirmCounts', () => {
  const group = {
    groupId: 1,
    matchKind: 'EXACT_BYTES' as const,
    memberCount: 3,
    reclaimableBytesEst: 3000,
    members: [
      {
        fileEntryId: 1,
        displayName: 'a',
        sizeBytes: 1000,
        mtimeMs: 1,
        pathLength: 5,
        mediaTypeHint: 'other' as const,
        paths: [],
      },
      {
        fileEntryId: 2,
        displayName: 'b',
        sizeBytes: 2000,
        mtimeMs: 2,
        pathLength: 5,
        mediaTypeHint: 'other' as const,
        paths: [],
      },
      {
        fileEntryId: 3,
        displayName: 'c',
        sizeBytes: 500,
        mtimeMs: 3,
        pathLength: 5,
        mediaTypeHint: 'other' as const,
        paths: [],
      },
    ],
  };

  it('counts non-keeper members and reclaimable bytes (AC-action-reclaim-01)', () => {
    const base = createKeeperSelectionState(group.members);
    const selection = selectKeeperMember(base, 1);
    const counts = resolveDeleteConfirmCounts(group, selection);
    expect(counts.deleteCount).toBe(2);
    expect(counts.reclaimableBytes).toBe(2500);
  });
});
