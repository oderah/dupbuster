import {formatGroupReclaimableLine} from '../components/duplicateGroupDisplay';
import {formatKeeperReclaimableLine} from '../components/keeperDisplay';
import {toKeeperMembers} from '../components/keeperMembers';
import type {DuplicateGroupDetail} from '../types/duplicateGroup';
import type {KeeperSelectionState} from '../types/keeper';
import {computeReclaimableBytesForKeeper} from './keeperSelection';

export type GroupDetailReclaimable = {
  reclaimableBytes: number;
  reclaimableLine: string;
  /** True when line reflects explicit keeper choice (FR-AC-06). */
  usesKeeperSelection: boolean;
};

/**
 * FR-AC-06 / AC-action-reclaim-01 — scan-time sum−max preview until keeper
 * activation; then sum of non-keeper member sizes on group detail.
 */
export function resolveGroupDetailReclaimable(
  group: Pick<DuplicateGroupDetail, 'members' | 'reclaimableBytesEst'>,
  keeperSelection: KeeperSelectionState,
): GroupDetailReclaimable {
  const members = toKeeperMembers(group.members);

  if (
    keeperSelection.explicitlyActivated &&
    keeperSelection.selectedFileEntryId != null
  ) {
    const reclaimableBytes = computeReclaimableBytesForKeeper(
      members,
      keeperSelection.selectedFileEntryId,
    );
    return {
      reclaimableBytes,
      reclaimableLine: formatKeeperReclaimableLine(reclaimableBytes),
      usesKeeperSelection: true,
    };
  }

  return {
    reclaimableBytes: group.reclaimableBytesEst,
    reclaimableLine: formatGroupReclaimableLine(group.reclaimableBytesEst),
    usesKeeperSelection: false,
  };
}
