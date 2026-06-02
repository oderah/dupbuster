import type {DuplicateGroupMember} from '../types/duplicateGroup';
import type {KeeperMember} from '../types/keeper';

export function toKeeperMembers(
  members: readonly DuplicateGroupMember[],
): KeeperMember[] {
  return members.map(member => ({
    fileEntryId: member.fileEntryId,
    displayName: member.displayName,
    sizeBytes: member.sizeBytes,
    mtimeMs: member.mtimeMs,
    pathLength: member.pathLength,
    mediaTypeHint: member.mediaTypeHint,
    thumbnailUri: member.thumbnailUri,
    paths: member.paths,
  }));
}
