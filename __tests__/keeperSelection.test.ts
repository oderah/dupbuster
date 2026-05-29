import {
  applyKeeperPreset,
  computeReclaimableBytesForKeeper,
  createKeeperSelectionState,
  isKeeperSelectionComplete,
  resolveDefaultHighlightedKeeperId,
  resolvePresetKeeperId,
  selectKeeperMember,
} from '../src/controllers/keeperSelection';
import type {KeeperMember} from '../src/types/keeper';

const members: KeeperMember[] = [
  {
    fileEntryId: 1,
    displayName: 'small.jpg',
    sizeBytes: 100,
    mtimeMs: 1000,
    pathLength: 20,
    mediaTypeHint: 'image',
  },
  {
    fileEntryId: 2,
    displayName: 'large.jpg',
    sizeBytes: 5000,
    mtimeMs: 500,
    pathLength: 8,
    mediaTypeHint: 'image',
  },
  {
    fileEntryId: 3,
    displayName: 'newest.jpg',
    sizeBytes: 200,
    mtimeMs: 9000,
    pathLength: 15,
    mediaTypeHint: 'image',
  },
];

describe('keeperSelection', () => {
  it('resolves largest, newest, and shortest-path presets (FR-AC-02)', () => {
    expect(resolvePresetKeeperId('largest', members)).toBe(2);
    expect(resolvePresetKeeperId('newest', members)).toBe(3);
    expect(resolvePresetKeeperId('shortest_path', members)).toBe(2);
  });

  it('defaultHighlighted is largest without explicit activation (AC-action-keeper-01)', () => {
    expect(resolveDefaultHighlightedKeeperId(members)).toBe(2);
    const state = createKeeperSelectionState(members);
    expect(state.explicitlyActivated).toBe(false);
    expect(state.selectedFileEntryId).toBeNull();
    expect(state.defaultHighlightedFileEntryId).toBe(2);
  });

  it('delete is disabled until explicit keeper activation (AC-action-keeper-01)', () => {
    const initial = createKeeperSelectionState(members);
    expect(isKeeperSelectionComplete(initial)).toBe(false);

    const activated = selectKeeperMember(initial, 2);
    expect(isKeeperSelectionComplete(activated)).toBe(true);
  });

  it('applies preset with explicit activation', () => {
    const initial = createKeeperSelectionState(members);
    const applied = applyKeeperPreset(initial, 'newest', members);
    expect(applied.selectedFileEntryId).toBe(3);
    expect(applied.explicitlyActivated).toBe(true);
  });

  it('computes reclaimable bytes as sum of non-keeper sizes (FR-AC-06)', () => {
    expect(computeReclaimableBytesForKeeper(members, 2)).toBe(300);
    expect(computeReclaimableBytesForKeeper(members, null)).toBe(0);
  });
});
