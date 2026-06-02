export type ResumePromptBannerProps = {
  resumeSessionKey: string;
  firstDisplayAlertEligible: boolean;
  dismissedForSession: boolean;
  onDismiss: () => void;
  onResume: () => void;
  onRestart: () => void;
  testID?: string;
};
