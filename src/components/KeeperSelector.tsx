import React from 'react';
import {
  Image,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {tokens} from '../tokens/tokens';
import {KEEPER_PRESETS} from '../types/keeper';
import type {KeeperSelectorProps} from '../types/keeper';
import {
  formatKeeperMemberAccessibilityLabel,
  getKeeperPresetLabel,
} from './keeperDisplay';
import {getMediaTypeLabel} from './duplicateGroupDisplay';
import {PathChipList} from './PathChipList';

export function KeeperSelector({
  members,
  selection,
  onSelectMember,
  onApplyPreset,
  rememberSession = false,
  onRememberSessionChange,
  showRememberSession = false,
  testID = 'keeper-selector',
}: KeeperSelectorProps): React.JSX.Element {
  return (
    <View
      testID={testID}
      accessibilityRole="radiogroup"
      accessibilityLabel={tokens.a11y.keeper.group}
      style={styles.container}>
      <Text style={styles.heading} maxFontSizeMultiplier={1.3}>
        {tokens.a11y.keeper.group}
      </Text>

      <View style={styles.presetRow} accessibilityRole="toolbar">
        {KEEPER_PRESETS.map(preset => (
          <Pressable
            key={preset}
            testID={`${testID}-preset-${preset}`}
            accessibilityRole="button"
            accessibilityLabel={getKeeperPresetLabel(preset)}
            onPress={() => onApplyPreset(preset)}
            style={styles.presetChip}
            hitSlop={4}>
            <Text style={styles.presetLabel} maxFontSizeMultiplier={1.3}>
              {getKeeperPresetLabel(preset)}
            </Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.memberList}>
        {members.map(member => {
          const isSelected =
            selection.explicitlyActivated &&
            selection.selectedFileEntryId === member.fileEntryId;
          const isDefaultHighlighted =
            !selection.explicitlyActivated &&
            selection.defaultHighlightedFileEntryId === member.fileEntryId;

          return (
            <Pressable
              key={member.fileEntryId}
              testID={`${testID}-member-${member.fileEntryId}`}
              accessibilityRole="radio"
              accessibilityLabel={formatKeeperMemberAccessibilityLabel(
                member,
                selection,
              )}
              accessibilityState={{selected: isSelected}}
              onPress={() => onSelectMember(member.fileEntryId)}
              style={[
                styles.memberRow,
                isDefaultHighlighted && styles.memberRowHighlighted,
                isSelected && styles.memberRowSelected,
              ]}>
              <View style={styles.memberThumb}>
                {member.thumbnailUri ? (
                  <Image
                    source={{uri: member.thumbnailUri}}
                    style={styles.thumbImage}
                    resizeMode="cover"
                    accessibilityIgnoresInvertColors
                  />
                ) : (
                  <Text style={styles.thumbPlaceholder} maxFontSizeMultiplier={1.3}>
                    {getMediaTypeLabel(member.mediaTypeHint)}
                  </Text>
                )}
              </View>
              <View style={styles.memberText}>
                <Text style={styles.memberName} maxFontSizeMultiplier={1.3}>
                  {member.displayName}
                </Text>
                <Text style={styles.memberMeta} maxFontSizeMultiplier={1.3}>
                  {getMediaTypeLabel(member.mediaTypeHint)}
                </Text>
                <PathChipList
                  paths={member.paths}
                  testID={`${testID}-member-${member.fileEntryId}-paths`}
                />
              </View>
              <View
                style={[
                  styles.radioOuter,
                  isSelected && styles.radioOuterSelected,
                ]}
                importantForAccessibility="no-hide-descendants"
                accessibilityElementsHidden>
                {isSelected ? <View style={styles.radioInner} /> : null}
              </View>
            </Pressable>
          );
        })}
      </View>

      {showRememberSession && onRememberSessionChange ? (
        <Pressable
          testID={`${testID}-remember-session`}
          accessibilityRole="checkbox"
          accessibilityState={{checked: rememberSession}}
          accessibilityLabel={tokens.keeper.rememberSession}
          onPress={() => onRememberSessionChange(!rememberSession)}
          style={styles.rememberRow}
          hitSlop={4}>
          <View
            style={[
              styles.checkboxOuter,
              rememberSession && styles.checkboxOuterChecked,
            ]}>
            {rememberSession ? <Text style={styles.checkboxMark}>✓</Text> : null}
          </View>
          <Text style={styles.rememberLabel} maxFontSizeMultiplier={1.3}>
            {tokens.keeper.rememberSession}
          </Text>
        </Pressable>
      ) : null}
    </View>
  );
}

const touchTarget = tokens.component.touchTargetMin;

const styles = StyleSheet.create({
  container: {
    gap: tokens.spacing.sm,
  },
  heading: {
    ...tokens.typography.heading,
    fontSize: 18,
    lineHeight: 24,
    color: tokens.color.text.primary,
  },
  presetRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: tokens.spacing.sm,
  },
  presetChip: {
    minHeight: touchTarget,
    paddingHorizontal: tokens.spacing.md,
    paddingVertical: tokens.spacing.sm,
    borderRadius: tokens.radius.md,
    backgroundColor: tokens.color.surface.secondary,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    justifyContent: 'center',
  },
  presetLabel: {
    ...tokens.typography.caption,
    color: tokens.color.text.primary,
    fontWeight: '600',
  },
  memberList: {
    gap: tokens.spacing.xs,
  },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: tokens.spacing.sm,
    minHeight: touchTarget,
    paddingVertical: tokens.spacing.sm,
    paddingHorizontal: tokens.spacing.sm,
    borderRadius: tokens.radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: tokens.color.border.default,
    backgroundColor: tokens.color.surface.primary,
  },
  memberRowHighlighted: {
    borderColor: tokens.color.action.primary,
    borderWidth: 2,
    backgroundColor: tokens.color.surface.secondary,
  },
  memberRowSelected: {
    borderColor: tokens.color.action.primary,
    borderWidth: 2,
  },
  memberThumb: {
    width: 48,
    height: 48,
    borderRadius: tokens.radius.sm,
    backgroundColor: tokens.color.surface.secondary,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  thumbImage: {
    width: '100%',
    height: '100%',
  },
  thumbPlaceholder: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
    textAlign: 'center',
  },
  memberText: {
    flex: 1,
    gap: 2,
  },
  memberName: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
  },
  memberMeta: {
    ...tokens.typography.caption,
    color: tokens.color.text.secondary,
  },
  radioOuter: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: tokens.color.border.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioOuterSelected: {
    borderColor: tokens.color.action.primary,
  },
  radioInner: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: tokens.color.action.primary,
  },
  rememberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: tokens.spacing.sm,
    minHeight: touchTarget,
  },
  checkboxOuter: {
    width: 22,
    height: 22,
    borderRadius: tokens.radius.sm,
    borderWidth: 2,
    borderColor: tokens.color.border.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxOuterChecked: {
    borderColor: tokens.color.action.primary,
    backgroundColor: tokens.color.action.primary,
  },
  checkboxMark: {
    color: tokens.color.text.onDanger,
    fontSize: 14,
    fontWeight: '700',
  },
  rememberLabel: {
    ...tokens.typography.body,
    color: tokens.color.text.primary,
    flex: 1,
  },
});
