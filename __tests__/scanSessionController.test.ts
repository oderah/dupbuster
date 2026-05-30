import {
  createScanSessionController,
  ScanSessionController,
} from '../src/controllers/scanSessionController';
import {
  createMockScanEnginePort,
  type ScanEnginePort,
} from '../src/native/scanEnginePort';
import {tokens} from '../src/tokens/tokens';

function waitForPhase(
  controller: ScanSessionController,
  phase: string,
  timeoutMs = 2000,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const started = Date.now();
    const tick = (): void => {
      if (controller.getState().phase === phase) {
        resolve();
        return;
      }
      if (Date.now() - started > timeoutMs) {
        reject(new Error(`Timed out waiting for phase ${phase}`));
        return;
      }
      setTimeout(tick, 5);
    };
    tick();
  });
}

describe('ScanSessionController', () => {
  let controller: ScanSessionController;

  beforeEach(() => {
    controller = createScanSessionController(createMockScanEnginePort());
  });

  afterEach(() => {
    controller.dispose();
  });

  it('starts idle with coverage presentation when configured', () => {
    controller.dispose();
    controller = createScanSessionController(createMockScanEnginePort(), {
      coverageVariant: 'partial',
    });
    const state = controller.getState();
    expect(state.phase).toBe('idle');
    expect(state.coveragePresentation?.coverageSessionKey).toBe('partial');
  });

  it('advances phase from mock bridge progress and loads catalog on complete', async () => {
    await controller.startScan({mode: 'platform_discovery', roots: []});
    await waitForPhase(controller, 'complete');

    const state = controller.getState();
    expect(state.scanRunId).not.toBeNull();
    expect(state.duplicateGroups).toHaveLength(1);
    expect(state.unscannableCounts.LARGE_SKIPPED).toBe(2);
    expect(state.progressAccessibilityLabel).toBe(tokens.a11y.scan.complete);
  });

  it('maps PERMISSION_DENIED errors to denied coverage banner', () => {
    controller.dispose();
    const base = createMockScanEnginePort({simulateScan: false});
    const engine: ScanEnginePort = {
      ...base,
      addErrorListener(listener) {
        listener({
          fileEntryId: 9,
          unscannableReason: 'PERMISSION_DENIED',
        });
        return () => {};
      },
    };
    controller = createScanSessionController(engine, {coverageVariant: 'partial'});
    expect(controller.getState().coverageVariant).toBe('denied');
  });

  it('notifies subscribers on progress updates', async () => {
    const seen: string[] = [];
    controller.subscribe(state => {
      seen.push(state.phase);
    });

    await controller.startScan({mode: 'platform_discovery', roots: []});
    await waitForPhase(controller, 'complete');

    expect(seen).toContain('discovering');
    expect(seen).toContain('complete');
  });

  it('opens group detail with keeper selection state', async () => {
    await controller.startScan({mode: 'platform_discovery', roots: []});
    await waitForPhase(controller, 'complete');

    controller.openGroupDetail(1);
    const state = controller.getState();
    expect(state.selectedGroupId).toBe(1);
    expect(state.keeperSelectionsByGroupId[1]?.defaultHighlightedFileEntryId).toBe(
      101,
    );
  });
});
