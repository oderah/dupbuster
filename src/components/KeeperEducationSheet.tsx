import React from 'react';
import {
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {KeeperEducationSheetProps} from '../types/keeper';

const keeperEducation = tokens.keeper.education;

export function KeeperEducationSheet({
  visible,
  matchKind,
  onDismiss,
  testID = 'keeper-education-sheet',
}: KeeperEducationSheetProps): React.JSX.Element {
  const showVideoEducation = matchKind === 'SAME_CONTENT_VIDEO';

  return (
    <Modal
      testID={testID}
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onDismiss}
      accessibilityViewIsModal>
      <View style={styles.backdrop}>
        <Pressable
          style={styles.backdropPress}
          accessibilityRole="button"
          accessibilityLabel={keeperEducation.dismiss}
          onPress={onDismiss}
        />
        <View style={styles.sheet} accessibilityRole="alert">
          <Text style={styles.title} maxFontSizeMultiplier={1.3}>
            {keeperEducation.title}
          </Text>
          <Text style={styles.body} maxFontSizeMultiplier={1.3}>
            {keeperEducation.general}
          </Text>
          {showVideoEducation ? (
            <Text style={styles.body} maxFontSizeMultiplier={1.3}>
              {keeperEducation.videoContent}
            </Text>
          ) : null}
          <Pressable
            testID={`${testID}-dismiss`}
            accessibilityRole="button"
            accessibilityLabel={keeperEducation.dismiss}
            onPress={onDismiss}
            style={styles.dismissButton}>
            <Text style={styles.dismissLabel} maxFontSizeMultiplier={1.3}>
              {keeperEducation.dismiss}
            </Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    justifyContent: 'center',
    padding: tokens.spacing.lg,
  },
  backdropPress: {
    ...StyleSheet.absoluteFillObject,
  },
  sheet: {
    backgroundColor: tokens.color.surface.primary,
    borderRadius: tokens.radius.lg,
    padding: tokens.spacing.lg,
    gap: tokens.spacing.md,
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
  dismissButton: {
    minHeight: touchTarget,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.action.primary,
    paddingHorizontal: tokens.spacing.lg,
  },
  dismissLabel: {
    ...tokens.typography.body,
    fontWeight: '600',
    color: tokens.color.text.onDanger,
  },
});
