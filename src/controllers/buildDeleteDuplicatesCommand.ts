import type {DuplicateGroupDetail} from '../types/duplicateGroup';
import type {KeeperSelectionState} from '../types/keeper';
import type {DeleteDuplicatesCommand} from '../types/scanEngine';

export function buildDeleteDuplicatesCommand(
  group: DuplicateGroupDetail,
  keeperSelection: KeeperSelectionState,
): DeleteDuplicatesCommand | null {
  const keeperId = keeperSelection.selectedFileEntryId;
  if (keeperId == null) {
    return null;
  }
  const deleteFileEntryIds = group.members
    .map(member => member.fileEntryId)
    .filter(fileEntryId => fileEntryId !== keeperId);
  if (deleteFileEntryIds.length === 0) {
    return null;
  }
  return {
    groupId: group.groupId,
    keeperFileEntryId: keeperId,
    deleteFileEntryIds,
  };
}
