import {useCallback, useEffect, useRef} from 'react';

import type {ScanSessionController} from '../controllers/scanSessionController';
import type {ScanPermissionPort} from '../types/scanPermission';
import {applyCoverageFromSnapshot} from '../permissions/applyCoverageFromSnapshot';
import {buildScanStartRequest} from '../permissions/buildScanStartRequest';

export type ScanPermissionFlowHandlers = {
  refreshCoverage: () => Promise<void>;
  handleStartScan: () => Promise<void>;
  handleResumeInterruptedScan: (resumeScanRunId: number) => Promise<void>;
  handleRestartInterruptedScan: (scanRunId: number) => Promise<void>;
  handleExpandCoverage: () => Promise<void>;
  handleOpenSettings: () => Promise<void>;
};

async function syncCoverage(
  controller: ScanSessionController,
  port: ScanPermissionPort,
): Promise<void> {
  const snapshot = await port.getSnapshot();
  applyCoverageFromSnapshot(controller, snapshot);
}

/** Coordinates permission port with ScanSessionController (M2-08). */
export function useScanPermissionFlow(
  controller: ScanSessionController,
  port: ScanPermissionPort,
): ScanPermissionFlowHandlers {
  const portRef = useRef(port);
  portRef.current = port;

  const refreshCoverage = useCallback(async () => {
    await syncCoverage(controller, portRef.current);
  }, [controller]);

  useEffect(() => {
    refreshCoverage().catch(() => {});
    return port.addAppStateListener(() => {
      refreshCoverage().catch(() => {});
    });
  }, [port, refreshCoverage]);

  const startScanFromSnapshot = useCallback(
    async (resumeScanRunId?: number) => {
      let snapshot = await portRef.current.getSnapshot();

      if (
        snapshot.platformDiscovery === 'denied' ||
        snapshot.platformDiscovery === 'blocked'
      ) {
        snapshot = await portRef.current.requestPlatformDiscoveryAccess();
        applyCoverageFromSnapshot(controller, snapshot);
      }

      const request = buildScanStartRequest(snapshot);
      if (request == null) {
        applyCoverageFromSnapshot(controller, snapshot);
        return;
      }

      await controller.startScan({
        ...request,
        resumeScanRunId,
      });
    },
    [controller],
  );

  const handleStartScan = useCallback(async () => {
    await startScanFromSnapshot();
  }, [startScanFromSnapshot]);

  const handleResumeInterruptedScan = useCallback(
    async (resumeScanRunId: number) => {
      await startScanFromSnapshot(resumeScanRunId);
    },
    [startScanFromSnapshot],
  );

  const handleRestartInterruptedScan = useCallback(
    async (scanRunId: number) => {
      await controller.abandonResumableScan(scanRunId);
      await startScanFromSnapshot();
    },
    [controller, startScanFromSnapshot],
  );

  const handleExpandCoverage = useCallback(async () => {
    const current = await portRef.current.getSnapshot();
    const snapshot =
      current.platform === 'ios'
        ? await portRef.current.expandPhotoLibraryAccess()
        : await portRef.current.pickAdditionalFolder();
    applyCoverageFromSnapshot(controller, snapshot);
    controller.markCoverageBannerDisplayed();
  }, [controller]);

  const handleOpenSettings = useCallback(async () => {
    await portRef.current.openAppSettings();
    controller.markCoverageBannerDisplayed();
  }, [controller]);

  return {
    refreshCoverage,
    handleStartScan,
    handleResumeInterruptedScan,
    handleRestartInterruptedScan,
    handleExpandCoverage,
    handleOpenSettings,
  };
}
