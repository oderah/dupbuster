import type {
  KeeperMember,
  KeeperPreset,
  KeeperSelectionState,
} from '../types/keeper';

export function resolvePresetKeeperId(
  preset: KeeperPreset,
  members: readonly KeeperMember[],
): number | null {
  if (members.length === 0) {
    return null;
  }

  if (preset === 'largest') {
    return members.reduce((best, member) =>
      member.sizeBytes > best.sizeBytes ? member : best,
    ).fileEntryId;
  }

  if (preset === 'newest') {
    return members.reduce((best, member) =>
      member.mtimeMs > best.mtimeMs ? member : best,
    ).fileEntryId;
  }

  return members.reduce((best, member) =>
    member.pathLength < best.pathLength ? member : best,
  ).fileEntryId;
}

/** Largest file is defaultHighlighted without `selected=true` (AC-action-keeper-01). */
export function resolveDefaultHighlightedKeeperId(
  members: readonly KeeperMember[],
): number | null {
  return resolvePresetKeeperId('largest', members);
}

export function createKeeperSelectionState(
  members: readonly KeeperMember[],
): KeeperSelectionState {
  return {
    explicitlyActivated: false,
    selectedFileEntryId: null,
    defaultHighlightedFileEntryId: resolveDefaultHighlightedKeeperId(members),
  };
}

export function selectKeeperMember(
  state: KeeperSelectionState,
  fileEntryId: number,
): KeeperSelectionState {
  return {
    ...state,
    explicitlyActivated: true,
    selectedFileEntryId: fileEntryId,
  };
}

export function applyKeeperPreset(
  state: KeeperSelectionState,
  preset: KeeperPreset,
  members: readonly KeeperMember[],
): KeeperSelectionState {
  const keeperId = resolvePresetKeeperId(preset, members);
  if (keeperId == null) {
    return state;
  }
  return selectKeeperMember(state, keeperId);
}

/** FR-AC-03 / AC-action-keeper-01 — delete enabled only after explicit activation. */
export function isKeeperSelectionComplete(
  selection: KeeperSelectionState,
): boolean {
  return (
    selection.explicitlyActivated && selection.selectedFileEntryId != null
  );
}

/** FR-AC-06 — sum of non-keeper member sizes. */
export function computeReclaimableBytesForKeeper(
  members: readonly KeeperMember[],
  keeperFileEntryId: number | null,
): number {
  if (keeperFileEntryId == null) {
    return 0;
  }
  return members
    .filter(member => member.fileEntryId !== keeperFileEntryId)
    .reduce((sum, member) => sum + member.sizeBytes, 0);
}
