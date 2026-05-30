export type RescanPromptBannerProps = {
  /** Stable key for a11y first-display alert tracking (schema version). */
  rescanSessionKey: string;
  /** When true, banner uses `accessibilityRole="alert"` on first display. */
  firstDisplayAlertEligible: boolean;
  /** When true, banner is hidden for the remainder of the app session. */
  dismissedForSession: boolean;
  onDismiss: () => void;
  /** Starts a fresh scan to refresh stale catalog results (US-15). */
  onRescan: () => void;
  testID?: string;
};
