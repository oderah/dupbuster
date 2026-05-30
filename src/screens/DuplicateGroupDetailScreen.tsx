import React from 'react';
import {ScrollView, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {DuplicateGroupDetailScreenProps} from '../types/duplicateGroup';
import {
  buildDetailThumbnailSlots,
  formatGroupAccessibilityLabel,
  formatGroupReclaimableLine,
  summarizeGroupMediaTypes,
} from '../components/duplicateGroupDisplay';
import {KeeperSelector} from '../components/KeeperSelector';
import {MatchKindBadge} from '../components/MatchKindBadge';
import {toKeeperMembers} from '../components/keeperMembers';
import {ThumbnailGrid} from '../components/ThumbnailGrid';
import {computeReclaimableBytesForKeeper} from '../controllers/keeperSelection';
import {formatKeeperReclaimableLine} from '../components/keeperDisplay';

const DETAIL_THUMB_CELL_SIZE = 96;

export function DuplicateGroupDetailScreen({
  group,
  keeperSelection,
  onSelectKeeper,
  onApplyKeeperPreset,
  rememberSession,
  onRememberSessionChange,
  showRememberSession,
  testID = 'duplicate-group-detail',
}: DuplicateGroupDetailScreenProps): React.JSX.Element {
  const accessibilityLabel = formatGroupAccessibilityLabel(
    group.matchKind,
    group.memberCount,
  );
  const keeperMembers = toKeeperMembers(group.members);
  const reclaimableBytes = keeperSelection.explicitlyActivated
    ? computeReclaimableBytesForKeeper(
        keeperMembers,
        keeperSelection.selectedFileEntryId,
      )
    : group.reclaimableBytesEst;
  const reclaimableLine = keeperSelection.explicitlyActivated
    ? formatKeeperReclaimableLine(reclaimableBytes)
    : formatGroupReclaimableLine(group.reclaimableBytesEst);
  const mediaTypeSummary = summarizeGroupMediaTypes(
    group.members.map(member => member.mediaTypeHint),
  );
  const thumbnailSlots = buildDetailThumbnailSlots(group.members);

  return (
    <ScrollView
      testID={testID}
      contentContainerStyle={styles.content}
      accessibilityLabel={accessibilityLabel}>
      <View style={styles.header}>
        <MatchKindBadge
          matchKind={group.matchKind}
          presentation="prominent"
          testID={`${testID}-match-kind-badge`}
        />
        <Text style={styles.mediaType} maxFontSizeMultiplier={1.3}>
          {mediaTypeSummary}
        </Text>
        <Text style={styles.reclaimable} maxFontSizeMultiplier={1.3}>
          {reclaimableLine}
        </Text>
      </View>

      <ThumbnailGrid
        slots={thumbnailSlots}
        cellSize={DETAIL_THUMB_CELL_SIZE}
        columns={3}
        testID={`${testID}-grid`}
      />

      <KeeperSelector
        members={keeperMembers}
        selection={keeperSelection}
        onSelectMember={onSelectKeeper}
        onApplyPreset={onApplyKeeperPreset}
        rememberSession={rememberSession}
        onRememberSessionChange={onRememberSessionChange}
        showRememberSession={showRememberSession}
        testID={`${testID}-keeper`}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: tokens.spacing.md,
    gap: tokens.spacing.md,
  },
  header: {
    gap: tokens.spacing.xs,
  },
  mediaType: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
  },
  reclaimable: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
  },
  memberList: {
    gap: tokens.spacing.sm,
  },
  memberRow: {
    paddingVertical: tokens.spacing.xs,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: tokens.color.border.default,
  },
  memberText: {
    gap: tokens.spacing.xs,
  },
  memberName: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
  },
  memberMeta: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
  },
});
