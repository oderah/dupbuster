import React from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {RescanPromptBannerProps} from '../types/rescanPromptBanner';

export function RescanPromptBanner({
  rescanSessionKey,
  firstDisplayAlertEligible,
  dismissedForSession,
  onDismiss,
  onRescan,
  testID = 'rescan-prompt-banner',
}: RescanPromptBannerProps): React.JSX.Element | null {
  if (dismissedForSession) {
    return null;
  }

  const message = tokens.rescan.required;
  const ctaLabel = tokens.notification.scan.title;

  return (
    <View
      testID={testID}
      accessibilityRole={firstDisplayAlertEligible ? 'alert' : undefined}
      accessibilityLiveRegion={firstDisplayAlertEligible ? undefined : 'polite'}
      accessibilityLabel={message}
      nativeID={`rescan-prompt-banner-${rescanSessionKey}`}
      style={styles.banner}>
      <View style={styles.content}>
        <Text style={styles.message} maxFontSizeMultiplier={1.3}>
          {message}
        </Text>
        <Pressable
          testID={`${testID}-cta`}
          accessibilityRole="button"
          accessibilityLabel={ctaLabel}
          onPress={onRescan}
          hitSlop={8}
          style={styles.cta}>
          <Text
            style={styles.ctaLabel}
            maxFontSizeMultiplier={1.3}
            numberOfLines={2}>
            {ctaLabel}
          </Text>
        </Pressable>
      </View>
      <Pressable
        testID={`${testID}-dismiss`}
        accessibilityRole="button"
        accessibilityLabel={tokens.a11y.coverage.dismiss}
        accessibilityHint={tokens.a11y.coverage.dismissHint}
        onPress={onDismiss}
        hitSlop={8}
        style={styles.dismiss}>
        <Text style={styles.dismissGlyph} maxFontSizeMultiplier={1.3}>
          ×
        </Text>
      </Pressable>
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  banner: {
    minHeight: tokens.component.rescanPromptBanner.minHeight,
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: tokens.spacing.sm,
    paddingHorizontal: tokens.spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
    backgroundColor: tokens.color.surface.caution,
    borderBottomColor: tokens.color.border.caution,
  },
  content: {
    flex: 1,
    paddingRight: tokens.spacing.sm,
  },
  message: {
    ...tokens.typography.body,
    flexShrink: 1,
    color: tokens.color.text.onCaution,
  },
  cta: {
    minHeight: touchTarget,
    justifyContent: 'center',
    alignSelf: 'flex-start',
    marginTop: tokens.spacing.xs,
    paddingVertical: tokens.spacing.xs,
  },
  ctaLabel: {
    ...tokens.typography.body,
    color: tokens.color.action.primary,
    fontWeight: '600',
  },
  dismiss: {
    minWidth: touchTarget,
    minHeight: touchTarget,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dismissGlyph: {
    fontSize: 24,
    lineHeight: 28,
    color: tokens.color.text.secondary,
  },
});
