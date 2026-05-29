import React from 'react';
import {Image, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {ThumbnailGridProps} from '../types/duplicateGroup';
import {
  formatOverflowLabel,
  getMediaTypeLabel,
} from './duplicateGroupDisplay';

export function ThumbnailGrid({
  slots,
  cellSize,
  columns = 2,
  testID = 'thumbnail-grid',
}: ThumbnailGridProps): React.JSX.Element {
  return (
    <View
      testID={testID}
      style={[styles.grid, {width: cellSize * columns + tokens.spacing.xs}]}
      accessibilityRole="image">
      {slots.map((slot, index) => {
        if (slot.kind === 'overflow') {
          return (
            <View
              key={`overflow-${index}`}
              testID={`${testID}-overflow-${index}`}
              style={[styles.cell, {width: cellSize, height: cellSize}]}
              accessibilityLabel={slot.accessibilityLabel}>
              <Text style={styles.overflowLabel} maxFontSizeMultiplier={1.3}>
                {formatOverflowLabel(slot.overflowCount)}
              </Text>
            </View>
          );
        }

        return (
          <View
            key={slot.fileEntryId}
            testID={`${testID}-thumb-${slot.fileEntryId}`}
            style={[styles.cell, {width: cellSize, height: cellSize}]}
            accessibilityLabel={slot.accessibilityLabel}>
            {slot.thumbnailUri ? (
              <Image
                source={{uri: slot.thumbnailUri}}
                style={styles.image}
                resizeMode="cover"
                accessibilityIgnoresInvertColors
              />
            ) : (
              <Text style={styles.placeholderLabel} maxFontSizeMultiplier={1.3}>
                {getMediaTypeLabel(slot.mediaTypeHint)}
              </Text>
            )}
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: tokens.spacing.xs,
  },
  cell: {
    backgroundColor: tokens.color.surface.secondary,
    borderRadius: tokens.radius.sm,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  placeholderLabel: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
    textAlign: 'center',
    paddingHorizontal: tokens.spacing.xs,
  },
  overflowLabel: {
    ...tokens.typography.heading,
    color: tokens.color.text.secondary,
  },
});
