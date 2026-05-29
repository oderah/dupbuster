import {formatToken, tokens} from '../tokens/tokens';
import {
  DUPLICATE_GROUP_THUMBNAIL_GRID_MAX,
  type MatchKind,
  type MediaTypeHint,
} from '../types/scanEngine';
import type {
  DuplicateGroupMember,
  DuplicateGroupSummary,
  DuplicateGroupThumbnail,
  ThumbnailGridSlot,
} from '../types/duplicateGroup';
import {formatBytes} from '../utils/formatBytes';

export function getMatchKindLabel(matchKind: MatchKind): string {
  if (matchKind === 'SAME_CONTENT_VIDEO') {
    return tokens.match.videoContent.label;
  }
  return tokens.match.exact.label;
}

/** AC-a11y-match-01 — match kind before member count. */
export function formatGroupAccessibilityLabel(
  matchKind: MatchKind,
  memberCount: number,
): string {
  const template =
    matchKind === 'SAME_CONTENT_VIDEO'
      ? tokens.a11y.group.videoContent
      : tokens.a11y.group.exact;
  return formatToken(template, {count: memberCount});
}

export function getMediaTypeLabel(mediaTypeHint: MediaTypeHint): string {
  return tokens.group.mediaType[mediaTypeHint];
}

export function summarizeGroupMediaTypes(
  hints: readonly MediaTypeHint[],
): string {
  const unique = [...new Set(hints)];
  if (unique.length === 0) {
    return tokens.group.mediaType.other;
  }
  if (unique.length === 1) {
    return getMediaTypeLabel(unique[0]!);
  }
  return tokens.group.mediaType.mixed;
}

export function formatGroupReclaimableLine(reclaimableBytesEst: number): string {
  return formatToken(tokens.reclaimable.label, {
    size: formatGroupMemberSize(reclaimableBytesEst),
  });
}

export function formatGroupMemberSize(sizeBytes: number): string {
  return formatBytes(sizeBytes);
}

/**
 * Build list-item thumbnail slots: up to 3 previews + overflow cell when
 * memberCount exceeds grid max (architecture §9.1).
 */
export function buildListThumbnailSlots(
  thumbnails: readonly DuplicateGroupThumbnail[],
  memberCount: number,
): ThumbnailGridSlot[] {
  const max = DUPLICATE_GROUP_THUMBNAIL_GRID_MAX;
  const overflow = memberCount > max;
  const visibleThumbnails = overflow
    ? thumbnails.slice(0, max - 1)
    : thumbnails.slice(0, max);

  const slots: ThumbnailGridSlot[] = visibleThumbnails.map(item => ({
    kind: 'thumbnail',
    fileEntryId: item.fileEntryId,
    thumbnailUri: item.thumbnailUri,
    mediaTypeHint: item.mediaTypeHint,
    accessibilityLabel: getMediaTypeLabel(item.mediaTypeHint),
  }));

  if (overflow) {
    const overflowCount = memberCount - (max - 1);
    slots.push({
      kind: 'overflow',
      overflowCount,
      accessibilityLabel: formatToken(tokens.group.overflow, {
        count: overflowCount,
      }),
    });
  }

  return slots;
}

/** Detail screen shows every member as a thumbnail tile. */
export function buildDetailThumbnailSlots(
  members: readonly DuplicateGroupMember[],
): ThumbnailGridSlot[] {
  return members.map(member => ({
    kind: 'thumbnail',
    fileEntryId: member.fileEntryId,
    thumbnailUri: member.thumbnailUri,
    mediaTypeHint: member.mediaTypeHint,
    accessibilityLabel: `${member.displayName}, ${getMediaTypeLabel(member.mediaTypeHint)}`,
  }));
}

export function resolveGroupMediaTypeSummary(
  group: Pick<DuplicateGroupSummary, 'thumbnails' | 'memberCount'>,
): string {
  return summarizeGroupMediaTypes(group.thumbnails.map(item => item.mediaTypeHint));
}

export function formatOverflowLabel(overflowCount: number): string {
  return formatToken(tokens.group.overflow, {count: overflowCount});
}

export function formatMemberSizeLine(sizeBytes: number): string {
  return formatToken(tokens.group.memberSize, {
    size: formatGroupMemberSize(sizeBytes),
  });
}
