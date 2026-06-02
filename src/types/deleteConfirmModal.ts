import type {RefObject} from 'react';
import type {Pressable, View} from 'react-native';

export type DeleteConfirmStep = 'review' | 'confirm';

export type DeleteConfirmModalProps = {
  visible: boolean;
  step: DeleteConfirmStep;
  deleteCount: number;
  reclaimableBytes: number;
  /** Delete trigger on group detail — focus returns here on dismiss (A11Y-07). */
  returnFocusRef?: RefObject<View | null> | RefObject<Pressable | null> | null;
  onCancel: () => void;
  onContinue: () => void;
  onConfirm: () => void;
  onGoBack: () => void;
  testID?: string;
};
