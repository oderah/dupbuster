import {
  formatPathChipListAccessibilityLabel,
  shouldShowPathChipList,
} from '../src/components/pathDisplay';
import {formatToken, tokens} from '../src/tokens/tokens';

describe('pathDisplay', () => {
  it('shows PathChipList only for two or more paths', () => {
    expect(shouldShowPathChipList([])).toBe(false);
    expect(shouldShowPathChipList(['a'])).toBe(false);
    expect(shouldShowPathChipList(['a', 'b'])).toBe(true);
  });

  it('formats frozen same-file label (AC-a11y-path-01)', () => {
    expect(formatPathChipListAccessibilityLabel(3)).toBe(
      formatToken(tokens.a11y.path.sameFile, {n: 3}),
    );
    expect(formatPathChipListAccessibilityLabel(2)).toBe('Same file, 2 locations');
  });
});
