import fs from 'fs';
import path from 'path';

import type {CatalogMeta} from '../types/scanEngine';

export const SCHEMA_MIGRATION_FIXTURE_ID = 'index-schema-migration-01';
export const SCHEMA_MIGRATION_FIXTURE_PATH = 'index-schema-migration-01.json';

export type SchemaMigrationFixture = {
  id: string;
  description: string;
  platform: string;
  input: {
    fromSchemaVersion: number;
    toSchemaVersion: number;
  };
  expect: {
    schemaVersion: number;
    fullRescanRequired: boolean;
    columnsAdded: string[];
    duplicateGroupsCleared: boolean;
  };
};

const FIXTURE_ROOT = path.join(
  __dirname,
  '..',
  '..',
  'tests',
  'fixtures',
  'dupbuster',
  'v1',
);

export function loadSchemaMigrationFixture(): SchemaMigrationFixture {
  const filePath = path.join(FIXTURE_ROOT, SCHEMA_MIGRATION_FIXTURE_PATH);
  return JSON.parse(fs.readFileSync(filePath, 'utf8')) as SchemaMigrationFixture;
}

/** Post-migration catalog meta as exposed by `getCatalogMeta` (FR-IX-05). */
export function catalogMetaFromMigrationFixture(
  fixture: SchemaMigrationFixture = loadSchemaMigrationFixture(),
): CatalogMeta {
  return {
    schemaVersion: fixture.expect.schemaVersion,
    fullRescanRequired: fixture.expect.fullRescanRequired,
  };
}
