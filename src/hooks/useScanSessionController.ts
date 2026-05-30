import {useEffect, useMemo, useRef, useState} from 'react';

import {
  createScanSessionController,
  type ScanSessionController,
} from '../controllers/scanSessionController';
import type {ScanEnginePort} from '../native/scanEnginePort';
import type {
  ScanSessionControllerOptions,
  ScanSessionState,
} from '../types/scanSession';

export type UseScanSessionControllerResult = {
  state: ScanSessionState;
  controller: ScanSessionController;
};

export function useScanSessionController(
  engine: ScanEnginePort,
  options?: ScanSessionControllerOptions,
): UseScanSessionControllerResult {
  const initialOptions = useRef(options);
  const controller = useMemo(
    () => createScanSessionController(engine, initialOptions.current),
    [engine],
  );
  const [state, setState] = useState<ScanSessionState>(() => controller.getState());

  useEffect(() => {
    return controller.subscribe(setState);
  }, [controller]);

  useEffect(() => {
    return () => controller.dispose();
  }, [controller]);

  return {state, controller};
}
