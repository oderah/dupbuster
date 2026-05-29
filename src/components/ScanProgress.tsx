import React from 'react';
import {
  I18nManager,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {computeScanPercent} from '../controllers/scanProgressA11y';
import {formatToken, tokens} from '../tokens/tokens';
import type {ScanProgressProps} from '../types/scanProgress';
import {formatBytes} from '../utils/formatBytes';
import {
  formatScanProgressPercentLine,
  formatScanProgressStats,
  getScanPhaseLabel,
  resolveScanProgressSubcopy,
  shouldShowCancelControl,
  shouldShowPauseControl,
  shouldShowResumeControl,
  shouldShowScanFooter,
} from './scanProgressDisplay';

export function ScanProgress({
  progress,
  accessibilityLabel,
  reducedMotion = false,
  onPause,
  onResume,
  onCancel,
  testID = 'scan-progress',
}: ScanProgressProps): React.JSX.Element | null {
  if (progress.phase === 'idle') {
    return null;
  }

  const phaseLabel = getScanPhaseLabel(progress.phase);
  const subcopy = resolveScanProgressSubcopy(progress);
  const percentLine = formatScanProgressPercentLine(progress);
  const stats = formatScanProgressStats(progress);
  const percent = computeScanPercent(
    progress.filesProcessed,
    progress.filesTotalKnown,
  );
  const reclaimableLine = formatToken(tokens.reclaimable.label, {
    size: formatBytes(progress.reclaimableBytesEst),
  });
  const showFooter = shouldShowScanFooter(progress.phase);
  const isRtl = I18nManager.isRTL;

  return (
    <View
      testID={testID}
      accessibilityLiveRegion="polite"
      accessibilityLabel={accessibilityLabel}
      style={styles.container}>
      <View style={styles.main}>
        <Text style={styles.phaseTitle} maxFontSizeMultiplier={1.3}>
          {phaseLabel}
        </Text>
        {subcopy ? (
          <Text style={styles.subcopy} maxFontSizeMultiplier={1.3}>
            {subcopy}
          </Text>
        ) : null}
        <Text style={styles.percentLine} maxFontSizeMultiplier={1.3}>
          {percentLine}
        </Text>
        <View
          style={[styles.progressTrack, isRtl && styles.progressTrackRtl]}
          accessibilityRole="progressbar"
          accessibilityValue={{
            min: 0,
            max: 100,
            now: percent ?? 0,
            text: percentLine,
          }}>
          {reducedMotion ? (
            <View
              testID={`${testID}-pulse-dot`}
              style={[styles.pulseDot, percent != null && percent > 0 && styles.pulseDotActive]}
            />
          ) : (
            <View
              testID={`${testID}-fill`}
              style={[
                styles.progressFill,
                {width: `${percent ?? 0}%`},
              ]}
            />
          )}
        </View>
        <Text style={styles.statsLine} maxFontSizeMultiplier={1.3}>
          {stats.groupsLine}
        </Text>
        <Text style={styles.statsLine} maxFontSizeMultiplier={1.3}>
          {stats.filesLine}
        </Text>
        <Text style={styles.reclaimableLine} maxFontSizeMultiplier={1.3}>
          {reclaimableLine}
        </Text>
      </View>
      {showFooter ? (
        <View testID={`${testID}-footer`} style={styles.footer}>
          {shouldShowPauseControl(progress.phase) ? (
            <Pressable
              testID={`${testID}-pause`}
              accessibilityRole="button"
              accessibilityLabel={tokens.a11y.scan.pause}
              onPress={onPause}
              hitSlop={8}
              style={styles.footerButton}>
              <Text style={styles.footerButtonLabel} maxFontSizeMultiplier={1.3}>
                {tokens.a11y.scan.pause}
              </Text>
            </Pressable>
          ) : null}
          {shouldShowResumeControl(progress.phase) ? (
            <Pressable
              testID={`${testID}-resume`}
              accessibilityRole="button"
              accessibilityLabel={tokens.a11y.scan.resume}
              onPress={onResume}
              hitSlop={8}
              style={styles.footerButton}>
              <Text style={styles.footerButtonLabel} maxFontSizeMultiplier={1.3}>
                {tokens.a11y.scan.resume}
              </Text>
            </Pressable>
          ) : null}
          {shouldShowCancelControl(progress.phase) ? (
            <Pressable
              testID={`${testID}-cancel`}
              accessibilityRole="button"
              accessibilityLabel={tokens.a11y.scan.cancel}
              onPress={onCancel}
              hitSlop={8}
              style={styles.footerButton}>
              <Text style={styles.footerButtonLabel} maxFontSizeMultiplier={1.3}>
                {tokens.a11y.scan.cancel}
              </Text>
            </Pressable>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  container: {
    minHeight: tokens.component.scanProgress.height,
    backgroundColor: tokens.color.surface.primary,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: tokens.color.border.default,
  },
  main: {
    paddingHorizontal: tokens.spacing.md,
    paddingTop: tokens.spacing.sm,
    paddingBottom: tokens.spacing.xs,
  },
  phaseTitle: {
    ...tokens.typography.heading,
    color: tokens.color.text.primary,
  },
  subcopy: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
    marginTop: tokens.spacing.xs,
  },
  percentLine: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
    marginTop: tokens.spacing.xs,
  },
  progressTrack: {
    height: 8,
    marginTop: tokens.spacing.sm,
    borderRadius: tokens.radius.sm,
    backgroundColor: tokens.color.surface.secondary,
    overflow: 'hidden',
    justifyContent: 'center',
  },
  progressTrackRtl: {
    transform: [{scaleX: -1}],
  },
  progressFill: {
    height: '100%',
    backgroundColor: tokens.color.action.primary,
    borderRadius: tokens.radius.sm,
  },
  pulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginLeft: tokens.spacing.xs,
    backgroundColor: tokens.color.border.default,
  },
  pulseDotActive: {
    backgroundColor: tokens.color.action.primary,
  },
  statsLine: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
    marginTop: tokens.spacing.xs,
  },
  reclaimableLine: {
    ...tokens.typography.caption,
    color: tokens.color.text.primary,
    marginTop: tokens.spacing.xs,
    fontWeight: '600',
  },
  footer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: tokens.spacing.sm,
    paddingHorizontal: tokens.spacing.md,
    paddingBottom: tokens.spacing.sm,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: tokens.color.border.default,
  },
  footerButton: {
    minHeight: touchTarget,
    minWidth: touchTarget,
    justifyContent: 'center',
    paddingHorizontal: tokens.spacing.sm,
  },
  footerButtonLabel: {
    ...tokens.typography.body,
    color: tokens.color.action.primary,
    fontWeight: '600',
  },
});
