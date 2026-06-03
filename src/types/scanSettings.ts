/** User scan preferences persisted across cold start (FR-FP-03 / M3-11, US-17 / M4-13). */
export type ScanSettings = {
  largeFilesOptIn: boolean;
  crashAnalyticsOptIn: boolean;
};

export type ScanSettingsPort = {
  load: () => Promise<ScanSettings>;
  save: (settings: ScanSettings) => Promise<void>;
};

export type LargeFilesSettingRowProps = {
  value: boolean;
  onValueChange: (value: boolean) => void;
  testID?: string;
};

export type CrashAnalyticsSettingRowProps = {
  value: boolean;
  onValueChange: (value: boolean) => void;
  testID?: string;
};
