import React from 'react';
import {Linking, Pressable, StyleSheet, Text} from 'react-native';

import {PRIVACY_POLICY_URL} from '../config/privacyPolicyUrl';
import {tokens} from '../tokens/tokens';

export type PrivacyPolicyLinkProps = {
  onPress?: () => void;
  testID?: string;
};

export function PrivacyPolicyLink({
  onPress,
  testID = 'privacy-policy-link',
}: PrivacyPolicyLinkProps): React.JSX.Element {
  const handlePress = () => {
    if (onPress) {
      onPress();
      return;
    }
    Linking.openURL(PRIVACY_POLICY_URL).catch(() => {});
  };

  return (
    <Pressable
      testID={testID}
      accessibilityRole="link"
      accessibilityLabel={tokens.settings.privacyPolicy}
      accessibilityHint={tokens.a11y.settings.privacyPolicyHint}
      onPress={handlePress}
      hitSlop={8}
      style={styles.row}>
      <Text style={styles.label} maxFontSizeMultiplier={1.3}>
        {tokens.settings.privacyPolicy}
      </Text>
    </Pressable>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  row: {
    minHeight: touchTarget,
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.surface.secondary,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    justifyContent: 'center',
  },
  label: {
    ...tokens.typography.body,
    color: tokens.color.action.primary,
  },
});
