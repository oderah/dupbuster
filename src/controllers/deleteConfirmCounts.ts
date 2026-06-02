import {toKeeperMembers} from '../components/keeperMembers';
import {computeReclaimableBytesForKeeper} from './keeperSelection';
import type {DuplicateGroupDetail} from '../types/duplicateGroup';
import type {KeeperSelectionState} from '../types/keeper';

export type DeleteConfirmCounts = {
  deleteCount: number;
  reclaimableBytes: number;
};

/** Non-keeper members targeted after two-step confirm (AC-action-reclaim-01 preview). */
export function resolveDeleteConfirmCounts(
  group: DuplicateGroupDetail,
  keeperSelection: KeeperSelectionState,
): DeleteConfirmCounts {
  const members = toKeeperMembers(group.members);
  const keeperId = keeperSelection.selectedFileEntryId;
  const deleteCount = Math.max(0, members.length - (keeperId != null ? 1 : 0));
  return {
    deleteCount,
    reclaimableBytes: computeReclaimableBytesForKeeper(members, keeperId),
  };
}
