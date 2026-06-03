import AsyncStorage from '@react-native-async-storage/async-storage';

import type {ScanSettings, ScanSettingsPort} from '../types/scanSettings';

export const SCAN_SETTINGS_STORAGE_KEY = 'dupbuster.scanSettings.v1';

export const DEFAULT_SCAN_SETTINGS: ScanSettings = {
  largeFilesOptIn: false,
  crashAnalyticsOptIn: false,
};

function parseStoredSettings(raw: string | null): ScanSettings {
  if (raw == null) {
    return DEFAULT_SCAN_SETTINGS;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<ScanSettings>;
    return {
      largeFilesOptIn: parsed.largeFilesOptIn === true,
      crashAnalyticsOptIn: parsed.crashAnalyticsOptIn === true,
    };
  } catch {
    return DEFAULT_SCAN_SETTINGS;
  }
}

export function createAsyncStorageScanSettingsPort(): ScanSettingsPort {
  return {
    async load() {
      const raw = await AsyncStorage.getItem(SCAN_SETTINGS_STORAGE_KEY);
      return parseStoredSettings(raw);
    },
    async save(settings) {
      await AsyncStorage.setItem(
        SCAN_SETTINGS_STORAGE_KEY,
        JSON.stringify(settings),
      );
    },
  };
}

/** In-memory port for Jest and shell QA. */
export function createMemoryScanSettingsPort(
  initial: ScanSettings = DEFAULT_SCAN_SETTINGS,
): ScanSettingsPort {
  let current = {...initial};
  return {
    async load() {
      return {...current};
    },
    async save(settings) {
      current = {...settings};
    },
  };
}
