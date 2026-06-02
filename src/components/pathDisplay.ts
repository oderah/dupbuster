import {formatToken, tokens} from '../tokens/tokens';

/** A11Y-08 / AC-a11y-path-01 — single summary label for multi-path members. */
export function formatPathChipListAccessibilityLabel(pathCount: number): string {
  return formatToken(tokens.a11y.path.sameFile, {n: pathCount});
}

/** PathChipList renders only when a member has two or more locations. */
export function shouldShowPathChipList(paths: readonly string[]): boolean {
  return paths.length >= 2;
}
