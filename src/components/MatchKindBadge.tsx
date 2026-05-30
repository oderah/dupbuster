import React from 'react';
import {StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {MatchKindBadgeProps} from '../types/matchKindBadge';
import type {MatchKind} from '../types/scanEngine';
import {getMatchKindLabel} from './duplicateGroupDisplay';

function resolveBadgeColors(matchKind: MatchKind): {
  backgroundColor: string;
  borderColor: string;
  textColor: string;
} {
  if (matchKind === 'SAME_CONTENT_VIDEO') {
    return {
      backgroundColor: tokens.color.surface.caution,
      borderColor: tokens.color.border.caution,
      textColor: tokens.color.text.onCaution,
    };
  }
  return {
    backgroundColor: tokens.color.surface.secondary,
    borderColor: tokens.color.border.default,
    textColor: tokens.color.text.primary,
  };
}

export function MatchKindBadge({
  matchKind,
  presentation = 'compact',
  testID = 'match-kind-badge',
}: MatchKindBadgeProps): React.JSX.Element {
  const label = getMatchKindLabel(matchKind);
  const colors = resolveBadgeColors(matchKind);
  const prominent = presentation === 'prominent';

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLabel={label}
      style={[
        styles.badge,
        prominent ? styles.badgeProminent : styles.badgeCompact,
        {
          backgroundColor: colors.backgroundColor,
          borderColor: colors.borderColor,
        },
      ]}>
      <Text
        style={[
          prominent ? styles.labelProminent : styles.labelCompact,
          {color: colors.textColor},
        ]}
        maxFontSizeMultiplier={1.3}
        numberOfLines={2}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    borderWidth: StyleSheet.hairlineWidth,
    maxWidth: '100%',
  },
  badgeCompact: {
    paddingHorizontal: tokens.spacing.sm,
    paddingVertical: tokens.spacing.xs,
    borderRadius: tokens.radius.sm,
  },
  badgeProminent: {
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    borderRadius: tokens.radius.md,
  },
  labelCompact: {
    ...tokens.typography.caption,
    fontWeight: '600',
  },
  labelProminent: {
    ...tokens.typography.heading,
  },
});
