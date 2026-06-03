import fs from 'fs';
import path from 'path';
import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {RescanPromptBanner} from '../src/components/RescanPromptBanner';
import {
  createInitialScanSessionState,
  reduceScanSessionOnCatalogMetaLoaded,
} from '../src/controllers/scanSessionReducer';
import {
  catalogMetaFromMigrationFixture,
  loadSchemaMigrationFixture,
  SCHEMA_MIGRATION_FIXTURE_ID,
} from '../src/testing/schemaMigrationFixture';
import {tokens} from '../src/tokens/tokens';

/** Stable golden for FR-IX-05 rescan UX after schema v1→v2 migration (M4-15). */
export const RESCAN_AFTER_MIGRATION_GOLDEN = {
  presentation: {
    rescanSessionKey: 'schema:2',
    firstDisplayAlertEligible: true,
    dismissedForSession: false,
  },
  banner: {
    message: tokens.rescan.required,
    ctaLabel: tokens.notification.scan.title,
    accessibilityRole: 'alert' as const,
    dismissLabel: tokens.a11y.coverage.dismiss,
    dismissHint: tokens.a11y.coverage.dismissHint,
    nativeId: 'rescan-prompt-banner-schema:2',
  },
};

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('schema migration + rescan prompt golden (M4-15)', () => {
  const fixture = loadSchemaMigrationFixture();

  it('index-schema-migration-01 fixture declares v1→v2 + full rescan', () => {
    expect(fixture.id).toBe(SCHEMA_MIGRATION_FIXTURE_ID);
    expect(fixture.input.fromSchemaVersion).toBe(1);
    expect(fixture.input.toSchemaVersion).toBe(2);
    expect(fixture.expect.schemaVersion).toBe(2);
    expect(fixture.expect.fullRescanRequired).toBe(true);
    expect(fixture.expect.duplicateGroupsCleared).toBe(true);
  });

  it('post-migration catalog meta matches fixture expect', () => {
    expect(catalogMetaFromMigrationFixture(fixture)).toEqual({
      schemaVersion: 2,
      fullRescanRequired: true,
    });
  });

  it('ScanSessionReducer surfaces golden rescan presentation (FR-IX-05)', () => {
    let state = createInitialScanSessionState();
    state = reduceScanSessionOnCatalogMetaLoaded(
      state,
      catalogMetaFromMigrationFixture(fixture),
    );
    expect(state.rescanPresentation).toEqual(
      RESCAN_AFTER_MIGRATION_GOLDEN.presentation,
    );
  });

  it('RescanPromptBanner golden tree after migration meta load', () => {
    const presentation = RESCAN_AFTER_MIGRATION_GOLDEN.presentation;
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <RescanPromptBanner
          rescanSessionKey={presentation.rescanSessionKey}
          firstDisplayAlertEligible={presentation.firstDisplayAlertEligible}
          dismissedForSession={presentation.dismissedForSession}
          onDismiss={jest.fn()}
          onRescan={jest.fn()}
        />,
      );
    });

    const banner = findByTestId(tree!.root, 'rescan-prompt-banner');
    const cta = findByTestId(tree!.root, 'rescan-prompt-banner-cta');
    const dismiss = findByTestId(tree!.root, 'rescan-prompt-banner-dismiss');

    expect(banner?.props.accessibilityLabel).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.message,
    );
    expect(banner?.props.accessibilityRole).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.accessibilityRole,
    );
    expect(banner?.props.nativeID).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.nativeId,
    );
    expect(cta?.props.accessibilityLabel).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.ctaLabel,
    );
    expect(dismiss?.props.accessibilityLabel).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.dismissLabel,
    );
    expect(dismiss?.props.accessibilityHint).toBe(
      RESCAN_AFTER_MIGRATION_GOLDEN.banner.dismissHint,
    );
  });

  it('native CatalogMigrator documents FR-IX-05 full_rescan_required', () => {
    const migrator = fs.readFileSync(
      path.join(
        __dirname,
        '..',
        'android/app/src/main/java/com/dupbuster/scanengine/index/CatalogMigrator.kt',
      ),
      'utf8',
    );
    expect(migrator).toMatch(/full_rescan_required.*FR-IX-05|FR-IX-05.*full_rescan_required/);
  });
});
