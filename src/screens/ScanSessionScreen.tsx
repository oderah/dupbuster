import React from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {CoverageBanner} from '../components/CoverageBanner';
import {DuplicateGroupListItem} from '../components/DuplicateGroupListItem';
import {KeeperEducationSheet} from '../components/KeeperEducationSheet';
import {RescanPromptBanner} from '../components/RescanPromptBanner';
import {ScanProgress} from '../components/ScanProgress';
import {ScanStatusChip} from '../components/ScanStatusChip';
import {UnscannableSummaryCard} from '../components/UnscannableSummaryCard';
import type {ScanSessionController} from '../controllers/scanSessionController';
import {DuplicateGroupDetailScreen} from '../screens/DuplicateGroupDetailScreen';
import {tokens} from '../tokens/tokens';
import type {ScanSessionState} from '../types/scanSession';

export type ScanSessionScreenProps = {
  state: ScanSessionState;
  controller: ScanSessionController;
  reducedMotion?: boolean;
  onStartScan: () => void;
  onExpandCoverage: () => void;
  onOpenSettings: () => void;
  testID?: string;
};

export function ScanSessionScreen({
  state,
  controller,
  reducedMotion = false,
  onStartScan,
  onExpandCoverage,
  onOpenSettings,
  testID = 'scan-session',
}: ScanSessionScreenProps): React.JSX.Element {
  const selectedGroup =
    state.selectedGroupId != null
      ? state.groupDetailsById[state.selectedGroupId]
      : null;
  const keeperSelection =
    state.selectedGroupId != null
      ? state.keeperSelectionsByGroupId[state.selectedGroupId]
      : null;

  const showCoverageBanner =
    state.coverageVariant != null &&
    state.coveragePresentation != null &&
    !state.coveragePresentation.dismissedForSession;

  const showRescanPrompt =
    state.rescanPresentation != null &&
    !state.rescanPresentation.dismissedForSession;

  const showUnscannableSummary =
    state.phase === 'complete' ||
    state.phase === 'error' ||
    state.phase === 'cancelled';

  return (
    <View testID={testID} style={styles.root}>
      <View style={styles.header}>
        <ScanStatusChip phase={state.phase} testID={`${testID}-status-chip`} />
      </View>

      {showCoverageBanner && state.coveragePresentation ? (
        <CoverageBanner
          variant={state.coverageVariant!}
          coverageSessionKey={state.coveragePresentation.coverageSessionKey}
          firstDisplayAlertEligible={
            state.coveragePresentation.firstDisplayAlertEligible
          }
          dismissedForSession={state.coveragePresentation.dismissedForSession}
          limitedLibraryCount={state.limitedLibraryCount}
          onDismiss={() => controller.dismissCoverageBanner()}
          onExpandCoverage={onExpandCoverage}
          onOpenSettings={onOpenSettings}
          testID={`${testID}-coverage-banner`}
        />
      ) : null}

      {showRescanPrompt && state.rescanPresentation ? (
        <RescanPromptBanner
          rescanSessionKey={state.rescanPresentation.rescanSessionKey}
          firstDisplayAlertEligible={
            state.rescanPresentation.firstDisplayAlertEligible
          }
          dismissedForSession={state.rescanPresentation.dismissedForSession}
          onDismiss={() => controller.dismissRescanPrompt()}
          onRescan={() => {
            controller.markRescanPromptDisplayed();
            onStartScan();
          }}
          testID={`${testID}-rescan-prompt-banner`}
        />
      ) : null}

      <ScanProgress
        progress={state.progress}
        accessibilityLabel={state.progressAccessibilityLabel}
        reducedMotion={reducedMotion}
        onPause={() => {
          controller.pauseScan().catch(() => {});
        }}
        onResume={() => {
          controller.resumeScan().catch(() => {});
        }}
        onCancel={() => {
          controller.cancelScan().catch(() => {});
        }}
        testID={`${testID}-progress`}
      />

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        accessibilityLabel="Duplicate scan results">
        {state.phase === 'idle' ? (
          <Pressable
            testID={`${testID}-start-scan`}
            accessibilityRole="button"
            accessibilityLabel={tokens.notification.scan.title}
            onPress={onStartScan}
            style={styles.startScanButton}>
            <Text style={styles.startScanLabel} maxFontSizeMultiplier={1.3}>
              {tokens.notification.scan.title}
            </Text>
          </Pressable>
        ) : null}

        {showUnscannableSummary ? (
          <UnscannableSummaryCard
            countsByReason={state.unscannableCounts}
            testID={`${testID}-unscannable-summary`}
          />
        ) : null}

        {state.duplicateGroups.map(group => (
          <DuplicateGroupListItem
            key={group.groupId}
            group={group}
            onPress={groupId => controller.openGroupDetail(groupId)}
            testID={`${testID}-group-${group.groupId}`}
          />
        ))}

        {state.coverageVariant === 'partial' ||
        state.coverageVariant === 'limited-library' ? (
          <Text style={styles.coverageFooter} maxFontSizeMultiplier={1.3}>
            {tokens.coverage.footer}
          </Text>
        ) : null}
      </ScrollView>

      <Modal
        visible={selectedGroup != null && keeperSelection != null}
        animationType="slide"
        onRequestClose={() => controller.closeGroupDetail()}>
        {selectedGroup && keeperSelection ? (
          <DuplicateGroupDetailScreen
            group={selectedGroup}
            keeperSelection={keeperSelection}
            onSelectKeeper={fileEntryId =>
              controller.selectKeeper(selectedGroup.groupId, fileEntryId)
            }
            onApplyKeeperPreset={preset =>
              controller.applyKeeperPreset(selectedGroup.groupId, preset)
            }
            rememberSession={state.rememberLargestForSession}
            onRememberSessionChange={value =>
              controller.setRememberLargestForSession(value)
            }
            showRememberSession
            testID={`${testID}-group-detail`}
          />
        ) : null}
      </Modal>

      <KeeperEducationSheet
        visible={state.keeperEducationVisible}
        matchKind={selectedGroup?.matchKind ?? 'EXACT_BYTES'}
        onDismiss={() => controller.dismissKeeperEducation()}
        testID={`${testID}-keeper-education`}
      />
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: tokens.color.surface.primary,
  },
  header: {
    paddingHorizontal: tokens.spacing.md,
    paddingTop: tokens.spacing.sm,
    paddingBottom: tokens.spacing.xs,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    padding: tokens.spacing.md,
    gap: tokens.spacing.md,
  },
  startScanButton: {
    minHeight: touchTarget,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.action.primary,
    paddingHorizontal: tokens.spacing.lg,
  },
  startScanLabel: {
    ...tokens.typography.body,
    fontWeight: '600',
    color: tokens.color.text.onDanger,
  },
  coverageFooter: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
    textAlign: 'center',
    marginTop: tokens.spacing.sm,
  },
});
