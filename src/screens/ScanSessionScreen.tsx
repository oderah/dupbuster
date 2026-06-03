import React, {Suspense, useRef} from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {CoverageBanner} from '../components/CoverageBanner';
import {DeleteConfirmModal} from '../components/DeleteConfirmModal';
import {DuplicateGroupListItem} from '../components/DuplicateGroupListItem';
import {CrashAnalyticsSettingRow} from '../components/CrashAnalyticsSettingRow';
import {LargeFilesSettingRow} from '../components/LargeFilesSettingRow';
import {RescanPromptBanner} from '../components/RescanPromptBanner';
import {ResumePromptBanner} from '../components/ResumePromptBanner';
import {ScanProgress} from '../components/ScanProgress';
import {ScanStatusChip} from '../components/ScanStatusChip';
import {UnscannableSummaryCard} from '../components/UnscannableSummaryCard';
import {resolveDeleteConfirmCounts} from '../controllers/deleteConfirmCounts';
import {isKeeperSelectionComplete} from '../controllers/keeperSelection';
import type {ScanSessionController} from '../controllers/scanSessionController';
import {DuplicateGroupDetailScreen} from '../screens/DuplicateGroupDetailScreen';
import {tokens} from '../tokens/tokens';
import type {ScanSessionState} from '../types/scanSession';

const LazyKeeperEducationSheet = React.lazy(async () => {
  const module = await import('../components/KeeperEducationSheet');
  return {default: module.KeeperEducationSheet};
});

export type ScanSessionScreenProps = {
  state: ScanSessionState;
  controller: ScanSessionController;
  reducedMotion?: boolean;
  onStartScan: () => void;
  onResumeInterruptedScan: (scanRunId: number) => void;
  onRestartInterruptedScan: (scanRunId: number) => void;
  onExpandCoverage: () => void;
  onOpenSettings: () => void;
  largeFilesOptIn: boolean;
  onLargeFilesOptInChange: (value: boolean) => void;
  crashAnalyticsOptIn: boolean;
  onCrashAnalyticsOptInChange: (value: boolean) => void;
  onEnableLargeFiles: () => void;
  testID?: string;
};

export function ScanSessionScreen({
  state,
  controller,
  reducedMotion = false,
  onStartScan,
  onResumeInterruptedScan,
  onRestartInterruptedScan,
  onExpandCoverage,
  onOpenSettings,
  largeFilesOptIn,
  onLargeFilesOptInChange,
  crashAnalyticsOptIn,
  onCrashAnalyticsOptInChange,
  onEnableLargeFiles,
  testID = 'scan-session',
}: ScanSessionScreenProps): React.JSX.Element {
  const deleteTriggerRef = useRef<React.ComponentRef<typeof Pressable>>(null);
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

  const showResumePrompt =
    state.resumePresentation != null &&
    !state.resumePresentation.dismissedForSession;

  const showUnscannableSummary =
    state.phase === 'complete' ||
    state.phase === 'error' ||
    state.phase === 'cancelled';

  const deleteCounts =
    selectedGroup && keeperSelection
      ? resolveDeleteConfirmCounts(selectedGroup, keeperSelection)
      : {deleteCount: 0, reclaimableBytes: 0};

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

      {showResumePrompt && state.resumePresentation ? (
        <ResumePromptBanner
          resumeSessionKey={state.resumePresentation.resumeSessionKey}
          firstDisplayAlertEligible={
            state.resumePresentation.firstDisplayAlertEligible
          }
          dismissedForSession={state.resumePresentation.dismissedForSession}
          onDismiss={() => controller.dismissResumePrompt()}
          onResume={() => {
            controller.markResumePromptDisplayed();
            onResumeInterruptedScan(state.resumePresentation!.scanRunId);
          }}
          onRestart={() => {
            controller.markResumePromptDisplayed();
            onRestartInterruptedScan(state.resumePresentation!.scanRunId);
          }}
          testID={`${testID}-resume-prompt-banner`}
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
        <LargeFilesSettingRow
          value={largeFilesOptIn}
          onValueChange={onLargeFilesOptInChange}
          testID={`${testID}-large-files-setting`}
        />
        <CrashAnalyticsSettingRow
          value={crashAnalyticsOptIn}
          onValueChange={onCrashAnalyticsOptInChange}
          testID={`${testID}-crash-analytics-setting`}
        />

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
            onEnableLargeFiles={onEnableLargeFiles}
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
            deleteEnabled={
              keeperSelection != null &&
              isKeeperSelectionComplete(keeperSelection)
            }
            deleteTriggerRef={deleteTriggerRef}
            onDeletePress={() => {
              if (
                selectedGroup &&
                keeperSelection &&
                isKeeperSelectionComplete(keeperSelection)
              ) {
                controller.beginDeleteFlow(selectedGroup.groupId);
              }
            }}
            testID={`${testID}-group-detail`}
          />
        ) : null}
      </Modal>

      <DeleteConfirmModal
        visible={state.deleteConfirmVisible}
        step={state.deleteConfirmStep}
        deleteCount={deleteCounts.deleteCount}
        reclaimableBytes={deleteCounts.reclaimableBytes}
        returnFocusRef={deleteTriggerRef}
        onCancel={() => controller.cancelDeleteConfirm()}
        onContinue={() => controller.advanceDeleteConfirm()}
        onGoBack={() => controller.goBackDeleteConfirm()}
        onConfirm={() => {
          controller.confirmDelete().catch(() => {});
        }}
        testID={`${testID}-delete-confirm`}
      />

      {state.keeperEducationVisible ? (
        <Suspense fallback={null}>
          <LazyKeeperEducationSheet
            visible
            matchKind={selectedGroup?.matchKind ?? 'EXACT_BYTES'}
            onDismiss={() => controller.dismissKeeperEducation()}
            testID={`${testID}-keeper-education`}
          />
        </Suspense>
      ) : null}
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
