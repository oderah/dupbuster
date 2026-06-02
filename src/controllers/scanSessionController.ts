import {buildDeleteDuplicatesCommand} from './buildDeleteDuplicatesCommand';
import type {ScanEnginePort} from '../native/scanEnginePort';
import type {CoverageBannerVariant} from '../types/coverageBanner';
import type {KeeperPreset} from '../types/keeper';
import {EMPTY_CATALOG_SNAPSHOT} from '../types/scanCatalog';
import type {ScanErrorEvent, ScanProgressEvent} from '../types/scanEngine';
import {
  createInitialScanSessionState,
  reduceScanSessionApplyKeeperPreset,
  reduceScanSessionCloseGroup,
  reduceScanSessionDismissCoverage,
  reduceScanSessionDismissKeeperEducation,
  reduceScanSessionDismissRescanPrompt,
  reduceScanSessionMarkCoverageDisplayed,
  reduceScanSessionMarkRescanPromptDisplayed,
  reduceScanSessionOnCatalogLoaded,
  reduceScanSessionOnCatalogMetaLoaded,
  reduceScanSessionOnProgress,
  reduceScanSessionOnScanError,
  reduceScanSessionOnScanStarted,
  reduceScanSessionOpenGroup,
  reduceScanSessionAdvanceDeleteConfirm,
  reduceScanSessionBeginDeleteFlow,
  reduceScanSessionCancelDeleteConfirm,
  reduceScanSessionConfirmDelete,
  reduceScanSessionGoBackDeleteConfirm,
  reduceScanSessionSelectKeeper,
  reduceScanSessionSetCoverageVariant,
  reduceScanSessionSetRememberLargest,
  shouldRefreshCatalogAfterProgress,
} from './scanSessionReducer';
import type {
  ScanSessionControllerOptions,
  ScanSessionStartRequest,
  ScanSessionState,
} from '../types/scanSession';
export type ScanSessionListener = (state: ScanSessionState) => void;

/** Owns scan phase state, coverage session keys, and native bridge subscriptions (M2-07). */
export class ScanSessionController {
  private state: ScanSessionState;
  private readonly listeners = new Set<ScanSessionListener>();
  private readonly engine: ScanEnginePort;
  private readonly unsubscribeProgress: () => void;
  private readonly unsubscribeError: () => void;
  private catalogRefreshInFlight: Promise<void> | null = null;

  constructor(engine: ScanEnginePort, options: ScanSessionControllerOptions = {}) {
    this.engine = engine;
    this.state = createInitialScanSessionState(options);
    this.unsubscribeProgress = engine.addProgressListener(event => {
      this.handleProgress(event);
    });
    this.unsubscribeError = engine.addErrorListener(event => {
      this.handleError(event);
    });
    this.refreshCatalogMeta().catch(() => {});
  }

  getState(): ScanSessionState {
    return this.state;
  }

  subscribe(listener: ScanSessionListener): () => void {
    this.listeners.add(listener);
    listener(this.state);
    return () => this.listeners.delete(listener);
  }

  dispose(): void {
    this.unsubscribeProgress();
    this.unsubscribeError();
    this.listeners.clear();
  }

  async startScan(request: ScanSessionStartRequest): Promise<void> {
    const result = await this.engine.startScan({
      mode: request.mode,
      roots: request.roots,
      resumeScanRunId: request.resumeScanRunId,
    });
    this.dispatch(reduceScanSessionOnScanStarted(this.state, result.scanRunId));
  }

  async pauseScan(): Promise<void> {
    if (this.state.scanRunId == null) {
      return;
    }
    await this.engine.pauseScan(this.state.scanRunId);
  }

  async resumeScan(): Promise<void> {
    if (this.state.scanRunId == null) {
      return;
    }
    await this.engine.resumeScan(this.state.scanRunId);
  }

  async cancelScan(): Promise<void> {
    if (this.state.scanRunId == null) {
      return;
    }
    await this.engine.cancelScan(this.state.scanRunId);
  }

  setCoverageVariant(
    variant: CoverageBannerVariant | null,
    limitedLibraryCount?: number,
  ): void {
    this.dispatch(
      reduceScanSessionSetCoverageVariant(
        this.state,
        variant,
        limitedLibraryCount,
      ),
    );
  }

  dismissCoverageBanner(): void {
    this.dispatch(reduceScanSessionDismissCoverage(this.state));
  }

  dismissRescanPrompt(): void {
    this.dispatch(reduceScanSessionDismissRescanPrompt(this.state));
  }

  markCoverageBannerDisplayed(): void {
    this.dispatch(reduceScanSessionMarkCoverageDisplayed(this.state));
  }

  markRescanPromptDisplayed(): void {
    this.dispatch(reduceScanSessionMarkRescanPromptDisplayed(this.state));
  }

  openGroupDetail(groupId: number): void {
    this.dispatch(reduceScanSessionOpenGroup(this.state, groupId));
  }

  closeGroupDetail(): void {
    this.dispatch(reduceScanSessionCloseGroup(this.state));
  }

  selectKeeper(groupId: number, fileEntryId: number): void {
    this.dispatch(reduceScanSessionSelectKeeper(this.state, groupId, fileEntryId));
  }

  applyKeeperPreset(groupId: number, preset: KeeperPreset): void {
    this.dispatch(
      reduceScanSessionApplyKeeperPreset(this.state, groupId, preset),
    );
  }

  setRememberLargestForSession(value: boolean): void {
    this.dispatch(reduceScanSessionSetRememberLargest(this.state, value));
  }

  /** AC-action-keeper-02 + AC-action-delete-01 — education gate then two-step modal. */
  beginDeleteFlow(groupId: number): void {
    this.dispatch(reduceScanSessionBeginDeleteFlow(this.state, groupId));
  }

  advanceDeleteConfirm(): void {
    this.dispatch(reduceScanSessionAdvanceDeleteConfirm(this.state));
  }

  goBackDeleteConfirm(): void {
    this.dispatch(reduceScanSessionGoBackDeleteConfirm(this.state));
  }

  cancelDeleteConfirm(): void {
    this.dispatch(reduceScanSessionCancelDeleteConfirm(this.state));
  }

  /** Invokes native DeleteCoordinator after two-step confirm (M3-03). */
  async confirmDelete(): Promise<void> {
    const groupId = this.state.selectedGroupId;
    if (groupId == null || !this.state.deleteConfirmVisible) {
      return;
    }
    const group = this.state.groupDetailsById[groupId];
    const selection = this.state.keeperSelectionsByGroupId[groupId];
    const command =
      group != null && selection != null
        ? buildDeleteDuplicatesCommand(group, selection)
        : null;
    if (command != null) {
      await this.engine.deleteDuplicates(command);
    }
    this.dispatch(reduceScanSessionConfirmDelete(this.state));
    await this.refreshCatalog();
    if (this.state.selectedGroupId != null) {
      const detail = this.state.groupDetailsById[this.state.selectedGroupId];
      if (detail == null) {
        this.dispatch(reduceScanSessionCloseGroup(this.state));
      }
    }
  }

  dismissKeeperEducation(): void {
    this.dispatch(reduceScanSessionDismissKeeperEducation(this.state));
  }

  private handleProgress(event: ScanProgressEvent): void {
    this.dispatch(reduceScanSessionOnProgress(this.state, event));
    if (shouldRefreshCatalogAfterProgress(event)) {
      this.refreshCatalog().catch(() => {});
    }
  }

  private handleError(event: ScanErrorEvent): void {
    this.dispatch(reduceScanSessionOnScanError(this.state, event));
  }

  private async refreshCatalogMeta(): Promise<void> {
    try {
      const catalogMeta = await this.engine.getCatalogMeta();
      this.dispatch(reduceScanSessionOnCatalogMetaLoaded(this.state, catalogMeta));
    } catch {
      // Native stub may reject until DB is ready; shell stays usable.
    }
  }

  private async refreshCatalog(): Promise<void> {
    if (this.catalogRefreshInFlight) {
      return this.catalogRefreshInFlight;
    }

    this.catalogRefreshInFlight = (async () => {
      try {
        const [catalog, catalogMeta] = await Promise.all([
          this.engine.getCatalogSnapshot(),
          this.engine.getCatalogMeta(),
        ]);
        this.dispatch(
          reduceScanSessionOnCatalogLoaded(this.state, catalog, catalogMeta),
        );
      } catch {
        this.dispatch(
          reduceScanSessionOnCatalogLoaded(
            this.state,
            EMPTY_CATALOG_SNAPSHOT,
            this.state.catalogMeta,
          ),
        );
      } finally {
        this.catalogRefreshInFlight = null;
      }
    })();

    return this.catalogRefreshInFlight;
  }

  private dispatch(next: ScanSessionState): void {
    this.state = next;
    for (const listener of this.listeners) {
      listener(this.state);
    }
  }
}

export function createScanSessionController(
  engine: ScanEnginePort,
  options?: ScanSessionControllerOptions,
): ScanSessionController {
  return new ScanSessionController(engine, options);
}
