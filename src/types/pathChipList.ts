export type PathChipListProps = {
  /** Primary path first, then hard-link aliases (FR-IX-04). */
  paths: readonly string[];
  testID?: string;
};
