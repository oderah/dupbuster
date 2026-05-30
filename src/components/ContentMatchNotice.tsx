import React from 'react';
import {StyleSheet, Text, View} from 'react-native';

import {tokens} from '../tokens/tokens';
import type {ContentMatchNoticeProps} from '../types/contentMatchNotice';

/** Trust copy for SAME_CONTENT_VIDEO detail (AC-a11y-match-02); omitted for EXACT_BYTES. */
export function ContentMatchNotice({
  matchKind,
  testID = 'content-match-notice',
}: ContentMatchNoticeProps): React.JSX.Element | null {
  if (matchKind !== 'SAME_CONTENT_VIDEO') {
    return null;
  }

  const message = tokens.match.videoContent.notice;

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLiveRegion="polite"
      accessibilityLabel={message}
      style={styles.notice}>
      <Text style={styles.message} maxFontSizeMultiplier={1.3}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  notice: {
    padding: tokens.spacing.md,
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.surface.caution,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.caution,
  },
  message: {
    ...tokens.typography.body,
    color: tokens.color.text.onCaution,
  },
});
