import {formatToken, tokens} from '../tokens/tokens';
import type {KeeperPreset} from '../types/keeper';
import type {KeeperMember, KeeperSelectionState} from '../types/keeper';
import {formatBytes} from '../utils/formatBytes';
import {formatMemberSizeLine} from './duplicateGroupDisplay';

export function getKeeperPresetLabel(preset: KeeperPreset): string {
  if (preset === 'largest') {
    return tokens.keeper.preset.largest;
  }
  if (preset === 'newest') {
    return tokens.keeper.preset.newest;
  }
  return tokens.keeper.preset.smallestFile;
}

export function formatKeeperMemberAccessibilityLabel(
  member: KeeperMember,
  selection: KeeperSelectionState,
): string {
  const sizeLine = formatMemberSizeLine(member.sizeBytes);
  const isDefaultHighlighted =
    !selection.explicitlyActivated &&
    selection.defaultHighlightedFileEntryId === member.fileEntryId;
  if (isDefaultHighlighted) {
    return `${member.displayName}, ${sizeLine}, suggested`;
  }
  return `${member.displayName}, ${sizeLine}`;
}

export function formatKeeperReclaimableLine(reclaimableBytes: number): string {
  return formatToken(tokens.reclaimable.label, {
    size: formatBytes(reclaimableBytes),
  });
}
