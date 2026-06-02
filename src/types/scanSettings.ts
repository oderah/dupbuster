/** User scan preferences persisted across cold start (FR-FP-03 / M3-11). */
export type ScanSettings = {
  largeFilesOptIn: boolean;
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
