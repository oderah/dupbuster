import {buildDeleteDuplicatesCommand} from '../src/controllers/buildDeleteDuplicatesCommand';
import {
  createKeeperSelectionState,
  selectKeeperMember,
} from '../src/controllers/keeperSelection';

describe('buildDeleteDuplicatesCommand', () => {
  const group = {
    groupId: 5,
    matchKind: 'EXACT_BYTES' as const,
    memberCount: 3,
    reclaimableBytesEst: 300,
    members: [
      {
        fileEntryId: 1,
        displayName: 'a',
        sizeBytes: 100,
        mtimeMs: 1,
        pathLength: 5,
        mediaTypeHint: 'other' as const,
        paths: [],
      },
      {
        fileEntryId: 2,
        displayName: 'b',
        sizeBytes: 200,
        mtimeMs: 2,
        pathLength: 5,
        mediaTypeHint: 'other' as const,
        paths: [],
      },
    ],
  };

  it('builds bridge payload with non-keeper member ids', () => {
    const selection = selectKeeperMember(
      createKeeperSelectionState(group.members),
      1,
    );
    expect(buildDeleteDuplicatesCommand(group, selection)).toEqual({
      groupId: 5,
      keeperFileEntryId: 1,
      deleteFileEntryIds: [2],
    });
  });

  it('returns null without explicit keeper', () => {
    expect(
      buildDeleteDuplicatesCommand(
        group,
        createKeeperSelectionState(group.members),
      ),
    ).toBeNull();
  });
});
