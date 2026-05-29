import React from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {DuplicateGroupListItemProps} from '../types/duplicateGroup';
import {
  buildListThumbnailSlots,
  formatGroupAccessibilityLabel,
  formatGroupReclaimableLine,
  getMatchKindLabel,
  resolveGroupMediaTypeSummary,
} from './duplicateGroupDisplay';
import {ThumbnailGrid} from './ThumbnailGrid';

const LIST_THUMB_CELL_SIZE = 72;

export function DuplicateGroupListItem({
  group,
  onPress,
  testID = 'duplicate-group-list-item',
}: DuplicateGroupListItemProps): React.JSX.Element {
  const slots = buildListThumbnailSlots(group.thumbnails, group.memberCount);
  const accessibilityLabel = formatGroupAccessibilityLabel(
    group.matchKind,
    group.memberCount,
  );
  const reclaimableLine = formatGroupReclaimableLine(group.reclaimableBytesEst);
  const mediaTypeSummary = resolveGroupMediaTypeSummary(group);
  const matchKindLabel = getMatchKindLabel(group.matchKind);

  const content = (
    <>
      <ThumbnailGrid
        slots={slots}
        cellSize={LIST_THUMB_CELL_SIZE}
        testID={`${testID}-thumbnails`}
      />
      <View style={styles.meta}>
        <Text style={styles.matchKind} maxFontSizeMultiplier={1.3}>
          {matchKindLabel}
        </Text>
        <Text style={styles.mediaType} maxFontSizeMultiplier={1.3}>
          {mediaTypeSummary}
        </Text>
        <Text style={styles.reclaimable} maxFontSizeMultiplier={1.3}>
          {reclaimableLine}
        </Text>
      </View>
    </>
  );

  if (!onPress) {
    return (
      <View
        testID={testID}
        accessibilityLabel={accessibilityLabel}
        style={styles.container}>
        {content}
      </View>
    );
  }

  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      onPress={() => onPress(group.groupId)}
      style={styles.container}>
      {content}
    </Pressable>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: tokens.spacing.md,
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    minHeight: touchTarget,
    backgroundColor: tokens.color.surface.primary,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: tokens.color.border.default,
  },
  meta: {
    flex: 1,
    gap: tokens.spacing.xs,
  },
  matchKind: {
    ...tokens.typography.body,
    fontWeight: '600',
    color: tokens.color.text.primary,
  },
  mediaType: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
  },
  reclaimable: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
  },
});
