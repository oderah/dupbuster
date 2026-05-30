import type {ScanSessionController} from '../controllers/scanSessionController';
import type {ScanPermissionSnapshot} from '../types/scanPermission';
import {resolveCoverageFromPermission} from './resolveCoverageFromPermission';

export function applyCoverageFromSnapshot(
  controller: ScanSessionController,
  snapshot: ScanPermissionSnapshot,
): ScanPermissionSnapshot {
  const coverage = resolveCoverageFromPermission(snapshot);
  controller.setCoverageVariant(coverage.variant, coverage.limitedLibraryCount);
  return snapshot;
}
