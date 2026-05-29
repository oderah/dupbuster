import React from 'react';
import {ScrollView, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {DuplicateGroupDetailScreenProps} from '../types/duplicateGroup';
import {
  buildDetailThumbnailSlots,
  formatGroupAccessibilityLabel,
  formatGroupReclaimableLine,
  formatMemberSizeLine,
  getMatchKindLabel,
  getMediaTypeLabel,
  summarizeGroupMediaTypes,
} from '../components/duplicateGroupDisplay';
import {ThumbnailGrid} from '../components/ThumbnailGrid';

const DETAIL_THUMB_CELL_SIZE = 96;

export function DuplicateGroupDetailScreen({
  group,
  testID = 'duplicate-group-detail',
}: DuplicateGroupDetailScreenProps): React.JSX.Element {
  const accessibilityLabel = formatGroupAccessibilityLabel(
    group.matchKind,
    group.memberCount,
  );
  const reclaimableLine = formatGroupReclaimableLine(group.reclaimableBytesEst);
  const matchKindLabel = getMatchKindLabel(group.matchKind);
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

      <ThumbnailGrid
        slots={thumbnailSlots}
        cellSize={DETAIL_THUMB_CELL_SIZE}
        columns={3}
        testID={`${testID}-grid`}
      />

      <View style={styles.memberList} accessibilityRole="list">
        {group.members.map(member => (
          <View
            key={member.fileEntryId}
            testID={`${testID}-member-${member.fileEntryId}`}
            style={styles.memberRow}
            accessibilityLabel={`${member.displayName}, ${getMediaTypeLabel(member.mediaTypeHint)}, ${formatMemberSizeLine(member.sizeBytes)}`}>
            <View style={styles.memberText}>
              <Text style={styles.memberName} maxFontSizeMultiplier={1.3}>
                {member.displayName}
              </Text>
              <Text style={styles.memberMeta} maxFontSizeMultiplier={1.3}>
                {getMediaTypeLabel(member.mediaTypeHint)} ·{' '}
                {formatMemberSizeLine(member.sizeBytes)}
              </Text>
            </View>
          </View>
        ))}
      </View>
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
  matchKind: {
    ...tokens.typography.heading,
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
