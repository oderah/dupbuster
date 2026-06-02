import {useCallback, useEffect, useState} from 'react';

import {DEFAULT_SCAN_SETTINGS} from '../settings/scanSettingsStore';
import type {ScanSettings, ScanSettingsPort} from '../types/scanSettings';

export type ScanSettingsHandlers = {
  settings: ScanSettings;
  ready: boolean;
  setLargeFilesOptIn: (value: boolean) => Promise<void>;
};

/** Loads and persists scan settings (M3-11). */
export function useScanSettings(port: ScanSettingsPort): ScanSettingsHandlers {
  const [settings, setSettings] = useState<ScanSettings>(DEFAULT_SCAN_SETTINGS);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    port
      .load()
      .then(loaded => {
        if (!cancelled) {
          setSettings(loaded);
          setReady(true);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [port]);

  const setLargeFilesOptIn = useCallback(
    async (value: boolean) => {
      const next = {largeFilesOptIn: value};
      setSettings(next);
      await port.save(next);
    },
    [port],
  );

  return {settings, ready, setLargeFilesOptIn};
}
