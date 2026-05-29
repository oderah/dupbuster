/** Coverage banner permission-state variants (requirements §5.3). */
export type CoverageBannerVariant = 'partial' | 'denied' | 'limited-library';

export type CoverageBannerProps = {
  variant: CoverageBannerVariant;
  /** Stable key for a11y first-display alert tracking (A11Y-01 / A11Y-03). */
  coverageSessionKey: string;
  /** When true, partial/denied use `accessibilityRole="alert"` on first display. */
  firstDisplayAlertEligible: boolean;
  /** When true, banner is hidden for the remainder of the app session. */
  dismissedForSession: boolean;
  /** Required for `limited-library` copy (`partial.ios.limited`). */
  limitedLibraryCount?: number;
  onDismiss: () => void;
  /** Partial / limited-library expand coverage action. */
  onExpandCoverage: () => void;
  /** Denied variant — opens platform settings (deep link handled by caller). */
  onOpenSettings: () => void;
  testID?: string;
};
