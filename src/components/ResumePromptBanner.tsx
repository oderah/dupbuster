import React from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {ResumePromptBannerProps} from '../types/resumePromptBanner';

export function ResumePromptBanner({
  resumeSessionKey,
  firstDisplayAlertEligible,
  dismissedForSession,
  onDismiss,
  onResume,
  onRestart,
  testID = 'resume-prompt-banner',
}: ResumePromptBannerProps): React.JSX.Element | null {
  if (dismissedForSession) {
    return null;
  }

  const message = tokens.resume.interrupted;
  const resumeLabel = tokens.resume.cta.resume;
  const restartLabel = tokens.resume.cta.restart;

  return (
    <View
      testID={testID}
      accessibilityRole={firstDisplayAlertEligible ? 'alert' : undefined}
      accessibilityLiveRegion={firstDisplayAlertEligible ? undefined : 'polite'}
      accessibilityLabel={message}
      nativeID={`resume-prompt-banner-${resumeSessionKey}`}
      style={styles.banner}>
      <View style={styles.content}>
        <Text style={styles.message} maxFontSizeMultiplier={1.3}>
          {message}
        </Text>
        <View style={styles.ctaRow}>
          <Pressable
            testID={`${testID}-resume`}
            accessibilityRole="button"
            accessibilityLabel={tokens.a11y.scan.resume}
            onPress={onResume}
            hitSlop={8}
            style={styles.cta}>
            <Text
              style={styles.ctaLabel}
              maxFontSizeMultiplier={1.3}
              numberOfLines={2}>
              {resumeLabel}
            </Text>
          </Pressable>
          <Pressable
            testID={`${testID}-restart`}
            accessibilityRole="button"
            accessibilityLabel={restartLabel}
            onPress={onRestart}
            hitSlop={8}
            style={styles.cta}>
            <Text
              style={[styles.ctaLabel, styles.ctaLabelSecondary]}
              maxFontSizeMultiplier={1.3}
              numberOfLines={2}>
              {restartLabel}
            </Text>
          </Pressable>
        </View>
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
    minHeight: tokens.component.resumePromptBanner.minHeight,
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
  ctaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: tokens.spacing.xs,
    gap: tokens.spacing.md,
  },
  cta: {
    minHeight: touchTarget,
    justifyContent: 'center',
    alignSelf: 'flex-start',
    paddingVertical: tokens.spacing.xs,
  },
  ctaLabel: {
    ...tokens.typography.body,
    color: tokens.color.action.primary,
    fontWeight: '600',
  },
  ctaLabelSecondary: {
    color: tokens.color.text.secondary,
    fontWeight: '500',
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
