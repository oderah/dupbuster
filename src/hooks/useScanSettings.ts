import {useCallback, useEffect, useRef, useState} from 'react';

import {DEFAULT_SCAN_SETTINGS} from '../settings/scanSettingsStore';
import type {ScanSettings, ScanSettingsPort} from '../types/scanSettings';

export type ScanSettingsHandlers = {
  settings: ScanSettings;
  ready: boolean;
  setLargeFilesOptIn: (value: boolean) => Promise<void>;
  setCrashAnalyticsOptIn: (value: boolean) => Promise<void>;
};

export type UseScanSettingsOptions = {
  /** Syncs native telemetry egress gate when preference changes (M4-13). */
  onCrashAnalyticsOptInChange?: (enabled: boolean) => Promise<void>;
};

/** Loads and persists scan settings (M3-11, M4-13). */
export function useScanSettings(
  port: ScanSettingsPort,
  options: UseScanSettingsOptions = {},
): ScanSettingsHandlers {
  const [settings, setSettings] = useState<ScanSettings>(DEFAULT_SCAN_SETTINGS);
  const [ready, setReady] = useState(false);
  const onCrashAnalyticsOptInChangeRef = useRef(
    options.onCrashAnalyticsOptInChange,
  );
  onCrashAnalyticsOptInChangeRef.current = options.onCrashAnalyticsOptInChange;

  useEffect(() => {
    let cancelled = false;
    port
      .load()
      .then(loaded => {
        if (!cancelled) {
          setSettings(loaded);
          setReady(true);
          onCrashAnalyticsOptInChangeRef
            .current?.(loaded.crashAnalyticsOptIn)
            .catch(() => {});
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
      const next = {...settings, largeFilesOptIn: value};
      setSettings(next);
      await port.save(next);
    },
    [port, settings],
  );

  const setCrashAnalyticsOptIn = useCallback(
    async (value: boolean) => {
      const next = {...settings, crashAnalyticsOptIn: value};
      setSettings(next);
      await port.save(next);
      await onCrashAnalyticsOptInChangeRef.current?.(value);
    },
    [port, settings],
  );

  return {settings, ready, setLargeFilesOptIn, setCrashAnalyticsOptIn};
}
