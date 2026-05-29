import React from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {UnscannableSummaryCardProps} from '../types/unscannableSummary';
import {
  buildUnscannableSummaryRows,
  hasUnscannableSummary,
} from './unscannableSummaryDisplay';

export function UnscannableSummaryCard({
  countsByReason,
  onRetryHashTimeout,
  onEnableLargeFiles,
  testID = 'unscannable-summary-card',
}: UnscannableSummaryCardProps): React.JSX.Element | null {
  if (!hasUnscannableSummary(countsByReason)) {
    return null;
  }

  const rows = buildUnscannableSummaryRows(countsByReason);

  return (
    <View
      testID={testID}
      accessibilityRole="summary"
      accessibilityLabel={tokens.unscan.title}
      style={styles.card}>
      <Text style={styles.title} maxFontSizeMultiplier={1.3}>
        {tokens.unscan.title}
      </Text>
      {rows.map(row => (
        <View
          key={row.reason}
          testID={`${testID}-row-${row.reason}`}
          style={styles.row}>
          <Text
            style={styles.rowLabel}
            maxFontSizeMultiplier={1.3}
            accessibilityLabel={row.accessibilityLabel}>
            {row.label}
          </Text>
          {row.ctaLabel && row.ctaAction ? (
            <Pressable
              testID={`${testID}-cta-${row.reason}`}
              accessibilityRole="button"
              accessibilityLabel={row.ctaLabel}
              onPress={
                row.ctaAction === 'retry'
                  ? onRetryHashTimeout
                  : onEnableLargeFiles
              }
              hitSlop={8}
              style={styles.cta}>
              <Text style={styles.ctaLabel} maxFontSizeMultiplier={1.3}>
                {row.ctaLabel}
              </Text>
            </Pressable>
          ) : null}
        </View>
      ))}
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  card: {
    backgroundColor: tokens.color.surface.secondary,
    borderRadius: tokens.radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    marginHorizontal: tokens.spacing.md,
    marginVertical: tokens.spacing.sm,
  },
  title: {
    ...tokens.typography.heading,
    color: tokens.color.text.primary,
    marginBottom: tokens.spacing.xs,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: tokens.spacing.xs,
    gap: tokens.spacing.sm,
  },
  rowLabel: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
    flexShrink: 1,
    flex: 1,
  },
  cta: {
    minHeight: touchTarget,
    justifyContent: 'center',
    paddingHorizontal: tokens.spacing.xs,
  },
  ctaLabel: {
    ...tokens.typography.body,
    color: tokens.color.action.primary,
    fontWeight: '600',
  },
});
