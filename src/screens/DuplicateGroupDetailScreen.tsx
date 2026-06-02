import React from 'react';
import {Pressable, ScrollView, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {DuplicateGroupDetailScreenProps} from '../types/duplicateGroup';
import {
  buildDetailThumbnailSlots,
  formatGroupAccessibilityLabel,
  summarizeGroupMediaTypes,
} from '../components/duplicateGroupDisplay';
import {ContentMatchNotice} from '../components/ContentMatchNotice';
import {KeeperSelector} from '../components/KeeperSelector';
import {MatchKindBadge} from '../components/MatchKindBadge';
import {toKeeperMembers} from '../components/keeperMembers';
import {ThumbnailGrid} from '../components/ThumbnailGrid';
import {resolveGroupDetailReclaimable} from '../controllers/groupDetailReclaimable';

const DETAIL_THUMB_CELL_SIZE = 96;

export function DuplicateGroupDetailScreen({
  group,
  keeperSelection,
  onSelectKeeper,
  onApplyKeeperPreset,
  rememberSession,
  onRememberSessionChange,
  showRememberSession,
  deleteEnabled = false,
  deleteTriggerRef,
  onDeletePress,
  testID = 'duplicate-group-detail',
}: DuplicateGroupDetailScreenProps): React.JSX.Element {
  const accessibilityLabel = formatGroupAccessibilityLabel(
    group.matchKind,
    group.memberCount,
  );
  const keeperMembers = toKeeperMembers(group.members);
  const {reclaimableLine} = resolveGroupDetailReclaimable(
    group,
    keeperSelection,
  );
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
      </View>

      <ThumbnailGrid
        slots={thumbnailSlots}
        cellSize={DETAIL_THUMB_CELL_SIZE}
        columns={3}
        testID={`${testID}-grid`}
      />

      <ContentMatchNotice
        matchKind={group.matchKind}
        testID={`${testID}-content-match-notice`}
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

      <Text
        testID={`${testID}-reclaimable`}
        style={styles.reclaimable}
        accessibilityLiveRegion="polite"
        maxFontSizeMultiplier={1.3}>
        {reclaimableLine}
      </Text>

      <Pressable
        ref={deleteTriggerRef}
        testID={`${testID}-delete-trigger`}
        accessibilityRole="button"
        accessibilityLabel={tokens.a11y.delete.trigger}
        accessibilityState={{disabled: !deleteEnabled}}
        disabled={!deleteEnabled}
        onPress={onDeletePress}
        style={[
          styles.deleteButton,
          deleteEnabled ? styles.deleteButtonEnabled : styles.deleteButtonDisabled,
        ]}>
        <Text
          style={[
            styles.deleteButtonLabel,
            !deleteEnabled && styles.deleteButtonLabelDisabled,
          ]}
          maxFontSizeMultiplier={1.3}>
          {tokens.delete.trigger}
        </Text>
      </Pressable>
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
  deleteButton: {
    minHeight: tokens.component.touchTargetMin,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: tokens.radius.md,
    paddingHorizontal: tokens.spacing.lg,
    marginTop: tokens.spacing.sm,
  },
  deleteButtonEnabled: {
    backgroundColor: tokens.color.action.danger,
  },
  deleteButtonDisabled: {
    backgroundColor: tokens.color.surface.secondary,
  },
  deleteButtonLabel: {
    ...tokens.typography.body,
    fontWeight: '600',
    color: tokens.color.text.onDanger,
  },
  deleteButtonLabelDisabled: {
    color: tokens.color.text.secondary,
  },
});
