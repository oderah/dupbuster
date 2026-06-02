import React, {useRef} from 'react';
import {Modal, Pressable, StyleSheet, Text, View} from 'react-native';

import {useModalFocusTrap} from '../hooks/useModalFocusTrap';
import {tokens} from '../tokens/tokens';
import type {DeleteConfirmModalProps} from '../types/deleteConfirmModal';
import {getDeleteConfirmStepCopy} from './deleteConfirmDisplay';

const deleteA11y = tokens.a11y.delete;
const touchTarget = tokens.component.touchTargetMin;

export function DeleteConfirmModal({
  visible,
  step,
  deleteCount,
  reclaimableBytes,
  returnFocusRef,
  onCancel,
  onContinue,
  onConfirm,
  onGoBack,
  testID = 'delete-confirm-modal',
}: DeleteConfirmModalProps): React.JSX.Element {
  const headingRef = useRef<View>(null);
  const copy = getDeleteConfirmStepCopy(step, deleteCount, reclaimableBytes);

  useModalFocusTrap({
    visible,
    focusKey: step,
    headingRef,
    returnFocusRef: returnFocusRef ?? null,
  });

  const handlePrimary = (): void => {
    if (step === 'review') {
      onContinue();
      return;
    }
    onConfirm();
  };

  const handleSecondary = (): void => {
    if (step === 'review') {
      onCancel();
      return;
    }
    onGoBack();
  };

  return (
    <Modal
      testID={testID}
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onCancel}
      accessibilityViewIsModal>
      <View style={styles.backdrop}>
        <View
          style={styles.sheet}
          accessibilityRole="alert"
          accessible={false}>
          <View
            ref={headingRef}
            accessible
            accessibilityRole="header"
            testID={`${testID}-heading`}>
            <Text style={styles.title} maxFontSizeMultiplier={1.3}>
              {copy.title}
            </Text>
          </View>
          <Text style={styles.body} maxFontSizeMultiplier={1.3}>
            {copy.body}
          </Text>
          <Pressable
            testID={`${testID}-primary`}
            accessibilityRole="button"
            accessibilityLabel={copy.primaryLabel}
            onPress={handlePrimary}
            style={[
              styles.button,
              copy.primaryIsDestructive
                ? styles.primaryDanger
                : styles.primaryNeutral,
            ]}>
            <Text
              style={[
                styles.buttonLabel,
                copy.primaryIsDestructive
                  ? styles.buttonLabelOnDanger
                  : styles.buttonLabelOnNeutral,
              ]}
              maxFontSizeMultiplier={1.3}>
              {copy.primaryLabel}
            </Text>
          </Pressable>
          <Pressable
            testID={`${testID}-secondary`}
            accessibilityRole="button"
            accessibilityLabel={copy.secondaryLabel}
            accessibilityHint={
              step === 'review' ? deleteA11y.cancel : undefined
            }
            onPress={handleSecondary}
            style={[styles.button, styles.secondary]}>
            <Text style={styles.secondaryLabel} maxFontSizeMultiplier={1.3}>
              {copy.secondaryLabel}
            </Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    padding: tokens.spacing.lg,
  },
  sheet: {
    backgroundColor: tokens.color.surface.primary,
    borderRadius: tokens.radius.lg,
    padding: tokens.spacing.lg,
    gap: tokens.spacing.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.action.danger,
    elevation: tokens.elevation.modal,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.2,
    shadowRadius: 8,
  },
  title: {
    ...tokens.typography.heading,
    color: tokens.color.text.primary,
  },
  body: {
    ...tokens.typography.body,
    color: tokens.color.text.secondary,
  },
  button: {
    minHeight: touchTarget,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: tokens.radius.md,
    paddingHorizontal: tokens.spacing.lg,
  },
  primaryDanger: {
    backgroundColor: tokens.color.action.danger,
  },
  primaryNeutral: {
    backgroundColor: tokens.color.action.primary,
  },
  secondary: {
    backgroundColor: tokens.color.surface.secondary,
  },
  buttonLabel: {
    ...tokens.typography.body,
    fontWeight: '600',
  },
  buttonLabelOnDanger: {
    color: tokens.color.text.onDanger,
  },
  buttonLabelOnNeutral: {
    color: tokens.color.text.onDanger,
  },
  secondaryLabel: {
    ...tokens.typography.body,
    fontWeight: '600',
    color: tokens.color.text.primary,
  },
});
