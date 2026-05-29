import React from 'react';
import {
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {formatToken, tokens} from '../tokens/tokens';
import type {CoverageBannerProps, CoverageBannerVariant} from '../types/coverageBanner';

function getMessage(
  variant: CoverageBannerVariant,
  limitedLibraryCount?: number,
): string {
  if (variant === 'limited-library') {
    return formatToken(tokens.partial.ios.limited, {
      count: limitedLibraryCount ?? 0,
    });
  }
  if (variant === 'denied') {
    return tokens.denied.blocking;
  }
  return tokens.partial.android;
}

function getPrimaryCtaLabel(variant: CoverageBannerVariant): string {
  if (variant === 'denied') {
    return tokens.denied.settingsCta;
  }
  if (variant === 'limited-library') {
    return tokens.partial.cta.expandIos;
  }
  return Platform.OS === 'ios'
    ? tokens.partial.cta.expandIos
    : tokens.partial.cta.expandAndroid;
}

function resolveAccessibilityRole(
  variant: CoverageBannerVariant,
  firstDisplayAlertEligible: boolean,
): 'alert' | undefined {
  if (variant === 'limited-library') {
    return undefined;
  }
  return firstDisplayAlertEligible ? 'alert' : undefined;
}

function resolveLiveRegion(
  variant: CoverageBannerVariant,
  firstDisplayAlertEligible: boolean,
): 'polite' | undefined {
  if (variant === 'limited-library') {
    return 'polite';
  }
  return firstDisplayAlertEligible ? undefined : 'polite';
}

export function CoverageBanner({
  variant,
  coverageSessionKey,
  firstDisplayAlertEligible,
  dismissedForSession,
  limitedLibraryCount,
  onDismiss,
  onExpandCoverage,
  onOpenSettings,
  testID = 'coverage-banner',
}: CoverageBannerProps): React.JSX.Element | null {
  if (dismissedForSession) {
    return null;
  }

  const message = getMessage(variant, limitedLibraryCount);
  const ctaLabel = getPrimaryCtaLabel(variant);
  const isDenied = variant === 'denied';
  const surfaceStyle =
    variant === 'denied' ? styles.surfaceDenied : styles.surfaceCaution;

  return (
    <View
      testID={testID}
      accessibilityRole={resolveAccessibilityRole(
        variant,
        firstDisplayAlertEligible,
      )}
      accessibilityLiveRegion={resolveLiveRegion(
        variant,
        firstDisplayAlertEligible,
      )}
      accessibilityLabel={message}
      nativeID={`coverage-banner-${coverageSessionKey}`}
      style={[styles.banner, surfaceStyle]}>
      <View style={styles.content}>
        <Text
          style={[
            styles.message,
            variant === 'denied' ? styles.messageDenied : styles.messageCaution,
          ]}
          maxFontSizeMultiplier={1.3}>
          {message}
        </Text>
        <Pressable
          testID={`${testID}-cta`}
          accessibilityRole="link"
          accessibilityLabel={ctaLabel}
          accessibilityHint={
            isDenied ? tokens.a11y.coverage.settingsHint : undefined
          }
          onPress={isDenied ? onOpenSettings : onExpandCoverage}
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
    minHeight: tokens.component.coverageBanner.minHeight,
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: tokens.spacing.sm,
    paddingHorizontal: tokens.spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  surfaceCaution: {
    backgroundColor: tokens.color.surface.caution,
    borderBottomColor: tokens.color.border.caution,
  },
  surfaceDenied: {
    backgroundColor: tokens.color.surface.secondary,
    borderBottomColor: tokens.color.border.default,
  },
  content: {
    flex: 1,
    paddingRight: tokens.spacing.sm,
  },
  message: {
    ...tokens.typography.body,
    flexShrink: 1,
  },
  messageCaution: {
    color: tokens.color.text.onCaution,
  },
  messageDenied: {
    color: tokens.color.text.primary,
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
