import fs from 'fs';
import path from 'path';

const FIXTURE_ROOT = path.join(
  __dirname,
  '..',
  'tests',
  'fixtures',
  'dupbuster',
  'v1',
);

type ManifestRow = {
  id: string;
  path: string;
  acceptanceCriteria?: string[];
};

type Manifest = {
  schemaVersion: number;
  fixtures: ManifestRow[];
};

describe('fixture matrix manifest (M1-17)', () => {
  const manifestPath = path.join(FIXTURE_ROOT, 'manifest.json');
  const manifest: Manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));

  it('lists every on-disk fixture JSON except manifest', () => {
    const onDisk = new Set(
      fs
        .readdirSync(FIXTURE_ROOT, {recursive: true})
        .filter(
          (f): f is string =>
            typeof f === 'string' && f.endsWith('.json') && f !== 'manifest.json',
        )
        .map(f => f.replace(/\\/g, '/')),
    );
    const listed = new Set(manifest.fixtures.map(row => row.path));
    expect(listed).toEqual(onDisk);
  });

  it('each row points to a valid descriptor with matching id', () => {
    for (const row of manifest.fixtures) {
      const filePath = path.join(FIXTURE_ROOT, row.path);
      expect(fs.existsSync(filePath)).toBe(true);
      const fixture = JSON.parse(fs.readFileSync(filePath, 'utf8'));
      expect(fixture.id).toBe(row.id);
      expect(fixture.input).toBeDefined();
      expect(fixture.expect).toBeDefined();
    }
  });
});
