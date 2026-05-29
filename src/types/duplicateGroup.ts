import type {MatchKind, MediaTypeHint} from './scanEngine';

/** Thumbnail slot for a duplicate group member (local catalog layer — not bridge). */
export type DuplicateGroupThumbnail = {
  fileEntryId: number;
  /** Platform thumbnail URI when available; placeholder when absent. */
  thumbnailUri?: string | null;
  mediaTypeHint: MediaTypeHint;
};

/** List-row summary for a duplicate group (US-09). */
export type DuplicateGroupSummary = {
  groupId: number;
  matchKind: MatchKind;
  memberCount: number;
  reclaimableBytesEst: number;
  /** First members for preview grid; order preserved. */
  thumbnails: DuplicateGroupThumbnail[];
};

export type DuplicateGroupListItemProps = {
  group: DuplicateGroupSummary;
  onPress?: (groupId: number) => void;
  testID?: string;
};

/** Full member row on group detail (paths via PathChipList in M3). */
export type DuplicateGroupMember = {
  fileEntryId: number;
  displayName: string;
  sizeBytes: number;
  mediaTypeHint: MediaTypeHint;
  thumbnailUri?: string | null;
};

export type DuplicateGroupDetail = {
  groupId: number;
  matchKind: MatchKind;
  memberCount: number;
  reclaimableBytesEst: number;
  members: DuplicateGroupMember[];
};

export type DuplicateGroupDetailScreenProps = {
  group: DuplicateGroupDetail;
  testID?: string;
};

export type ThumbnailGridSlot =
  | {
      kind: 'thumbnail';
      fileEntryId: number;
      thumbnailUri?: string | null;
      mediaTypeHint: MediaTypeHint;
      accessibilityLabel: string;
    }
  | {
      kind: 'overflow';
      overflowCount: number;
      accessibilityLabel: string;
    };

export type ThumbnailGridProps = {
  slots: ThumbnailGridSlot[];
  cellSize: number;
  columns?: number;
  testID?: string;
};
