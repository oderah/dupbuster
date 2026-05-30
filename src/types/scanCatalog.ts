import type {DuplicateGroupDetail, DuplicateGroupSummary} from './duplicateGroup';
import type {UnscannableCountsByReason} from './unscannableSummary';

/** Catalog read model for RN shell (local DB layer — not progress bridge). */
export type ScanCatalogSnapshot = {
  duplicateGroups: DuplicateGroupSummary[];
  unscannableCounts: UnscannableCountsByReason;
  groupDetailsById: Readonly<Record<number, DuplicateGroupDetail>>;
};

export const EMPTY_CATALOG_SNAPSHOT: ScanCatalogSnapshot = {
  duplicateGroups: [],
  unscannableCounts: {},
  groupDetailsById: {},
};
