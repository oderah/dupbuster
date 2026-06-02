import React from 'react';
import {StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {PathChipListProps} from '../types/pathChipList';
import {
  formatPathChipListAccessibilityLabel,
  shouldShowPathChipList,
} from './pathDisplay';

export function PathChipList({
  paths,
  testID = 'path-chip-list',
}: PathChipListProps): React.JSX.Element | null {
  if (!shouldShowPathChipList(paths)) {
    return null;
  }

  const accessibilityLabel = formatPathChipListAccessibilityLabel(paths.length);

  return (
    <View
      testID={testID}
      accessible
      accessibilityLabel={accessibilityLabel}
      style={styles.container}
      importantForAccessibility="yes">
      {paths.map((path, index) => (
        <View
          key={`${index}-${path}`}
          style={styles.chip}
          accessible={false}
          importantForAccessibility="no-hide-descendants"
          accessibilityElementsHidden>
          <Text
            testID={`${testID}-chip-${index}`}
            style={styles.chipText}
            maxFontSizeMultiplier={1.3}
            writingDirection="ltr">
            {path}
          </Text>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: tokens.spacing.xs,
    marginTop: tokens.spacing.xs,
  },
  chip: {
    alignSelf: 'stretch',
    paddingHorizontal: tokens.spacing.sm,
    paddingVertical: tokens.spacing.xs,
    borderRadius: tokens.radius.sm,
    backgroundColor: tokens.color.surface.secondary,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
  },
  chipText: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
  },
});
