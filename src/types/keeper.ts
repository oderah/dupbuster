import type {MatchKind, MediaTypeHint} from './scanEngine';

/** FR-AC-02 preset rules. */
export type KeeperPreset = 'largest' | 'newest' | 'shortest_path';

export const KEEPER_PRESETS = [
  'largest',
  'newest',
  'shortest_path',
] as const satisfies readonly KeeperPreset[];

/** Member row input for keeper resolution (catalog layer — not bridge). */
export type KeeperMember = {
  fileEntryId: number;
  displayName: string;
  sizeBytes: number;
  mtimeMs: number;
  /** Display path length for shortest-path preset (FR-AC-02). */
  pathLength: number;
  mediaTypeHint: MediaTypeHint;
  thumbnailUri?: string | null;
  paths: readonly string[];
};

/**
 * AC-action-keeper-01 — no selected keeper until explicit user activation.
 * `defaultHighlightedFileEntryId` is visual-only (largest preset target).
 */
export type KeeperSelectionState = {
  explicitlyActivated: boolean;
  selectedFileEntryId: number | null;
  defaultHighlightedFileEntryId: number | null;
};

export type KeeperSelectorProps = {
  members: readonly KeeperMember[];
  selection: KeeperSelectionState;
  onSelectMember: (fileEntryId: number) => void;
  onApplyPreset: (preset: KeeperPreset) => void;
  rememberSession?: boolean;
  onRememberSessionChange?: (value: boolean) => void;
  showRememberSession?: boolean;
  testID?: string;
};

export type KeeperEducationSheetProps = {
  visible: boolean;
  matchKind: MatchKind;
  onDismiss: () => void;
  testID?: string;
};
