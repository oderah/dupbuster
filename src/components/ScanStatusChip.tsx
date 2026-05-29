import React from 'react';
import {StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {ScanStatusChipProps} from '../types/scanProgress';
import {getScanPhaseLabel} from './scanProgressDisplay';

export function ScanStatusChip({
  phase,
  testID = 'scan-status-chip',
}: ScanStatusChipProps): React.JSX.Element {
  const label = getScanPhaseLabel(phase);

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLabel={label}
      style={styles.chip}>
      <Text style={styles.label} maxFontSizeMultiplier={1.3} numberOfLines={1}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    alignSelf: 'flex-start',
    paddingHorizontal: tokens.spacing.sm,
    paddingVertical: tokens.spacing.xs,
    borderRadius: tokens.radius.sm,
    backgroundColor: tokens.color.surface.secondary,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    maxWidth: '100%',
  },
  label: {
    ...tokens.typography.caption,
    color: tokens.color.text.primary,
    fontWeight: '600',
  },
});
