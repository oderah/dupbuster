import {formatToken, tokens} from '../tokens/tokens';
import {formatBytes} from '../utils/formatBytes';
import type {DeleteConfirmStep} from '../types/deleteConfirmModal';

export type DeleteConfirmStepCopy = {
  title: string;
  body: string;
  primaryLabel: string;
  secondaryLabel: string;
  primaryIsDestructive: boolean;
};

export function getDeleteConfirmStepCopy(
  step: DeleteConfirmStep,
  deleteCount: number,
  reclaimableBytes: number,
): DeleteConfirmStepCopy {
  const size = formatBytes(reclaimableBytes);
  if (step === 'review') {
    return {
      title: tokens.delete.review.title,
      body: formatToken(tokens.delete.review.body, {count: deleteCount, size}),
      primaryLabel: tokens.delete.review.continue,
      secondaryLabel: tokens.delete.review.cancel,
      primaryIsDestructive: false,
    };
  }
  return {
    title: tokens.delete.confirm.title,
    body: formatToken(tokens.delete.confirm.body, {count: deleteCount}),
    primaryLabel: tokens.delete.confirm.confirm,
    secondaryLabel: tokens.delete.confirm.back,
    primaryIsDestructive: true,
  };
}
