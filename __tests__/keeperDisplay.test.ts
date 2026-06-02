import {
  formatKeeperMemberAccessibilityLabel,
  getKeeperPresetLabel,
} from '../src/components/keeperDisplay';
import {createKeeperSelectionState} from '../src/controllers/keeperSelection';
import {tokens} from '../src/tokens/tokens';
import type {KeeperMember} from '../src/types/keeper';

const members: KeeperMember[] = [
  {
    fileEntryId: 1,
    displayName: 'a.jpg',
    sizeBytes: 100,
    mtimeMs: 1,
    pathLength: 10,
    mediaTypeHint: 'image',
    paths: [],
  },
  {
    fileEntryId: 2,
    displayName: 'b.jpg',
    sizeBytes: 500,
    mtimeMs: 2,
    pathLength: 5,
    mediaTypeHint: 'image',
    paths: [],
  },
];

describe('keeperDisplay', () => {
  it('uses frozen preset labels', () => {
    expect(getKeeperPresetLabel('largest')).toBe(tokens.keeper.preset.largest);
    expect(getKeeperPresetLabel('newest')).toBe(tokens.keeper.preset.newest);
    expect(getKeeperPresetLabel('smallest_file')).toBe(
      tokens.keeper.preset.smallestFile,
    );
  });

  it('annotates default highlighted member without selected state (AC-a11y-keeper-01)', () => {
    const selection = createKeeperSelectionState(members);
    expect(
      formatKeeperMemberAccessibilityLabel(members[1]!, selection),
    ).toContain('suggested');
    const activated = {
      ...selection,
      explicitlyActivated: true,
      selectedFileEntryId: 1,
    };
    expect(
      formatKeeperMemberAccessibilityLabel(members[0]!, activated),
    ).not.toContain('suggested');
  });
});
