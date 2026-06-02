import React from 'react';
import {StyleSheet, Switch, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {LargeFilesSettingRowProps} from '../types/scanSettings';

export function LargeFilesSettingRow({
  value,
  onValueChange,
  testID = 'large-files-setting',
}: LargeFilesSettingRowProps): React.JSX.Element {
  return (
    <View testID={testID} style={styles.row}>
      <Text
        style={styles.label}
        maxFontSizeMultiplier={1.3}
        accessibilityRole="text">
        {tokens.settings.largeFiles}
      </Text>
      <Switch
        testID={`${testID}-switch`}
        accessibilityRole="switch"
        accessibilityLabel={tokens.settings.largeFiles}
        accessibilityState={{checked: value}}
        value={value}
        onValueChange={onValueChange}
      />
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: tokens.spacing.sm,
    minHeight: touchTarget,
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.surface.secondary,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
  },
  label: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
    flex: 1,
    flexShrink: 1,
  },
});
