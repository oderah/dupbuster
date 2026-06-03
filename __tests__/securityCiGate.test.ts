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

type FixtureExpect = {
  ciGate?: boolean;
  mustNotContainRawPath?: boolean;
};

type FixtureDescriptor = {
  id: string;
  input: {rawPath?: string};
  expect: FixtureExpect;
  acceptanceCriteria?: string[];
};

type ManifestRow = {
  id: string;
  path: string;
};

type Manifest = {
  fixtures: ManifestRow[];
};

function loadCiGateFixtures(): Array<{path: string; fixture: FixtureDescriptor}> {
  const manifest: Manifest = JSON.parse(
    fs.readFileSync(path.join(FIXTURE_ROOT, 'manifest.json'), 'utf8'),
  );
  const rows: Array<{path: string; fixture: FixtureDescriptor}> = [];
  for (const row of manifest.fixtures) {
    const filePath = path.join(FIXTURE_ROOT, row.path);
    const fixture: FixtureDescriptor = JSON.parse(
      fs.readFileSync(filePath, 'utf8'),
    );
    if (fixture.expect?.ciGate === true) {
      rows.push({path: row.path, fixture});
    }
  }
  return rows;
}

describe('security CI gates (M4-05)', () => {
  const ciGateFixtures = loadCiGateFixtures();

  it('lists AC-security-redact-01 with ciGate in the manifest matrix', () => {
    expect(ciGateFixtures.length).toBeGreaterThan(0);
    const redact = ciGateFixtures.find(
      row => row.fixture.id === 'security-redact-01',
    );
    expect(redact).toBeDefined();
    expect(redact?.fixture.acceptanceCriteria).toContain(
      'AC-security-redact-01',
    );
  });

  it.each(ciGateFixtures.map(row => [row.path, row.fixture] as const))(
    '%s declares ciGate contract fields',
    (_relativePath, fixture) => {
      expect(fixture.expect.ciGate).toBe(true);
      expect(fixture.expect.mustNotContainRawPath).toBe(true);
      expect(fixture.input).toBeDefined();
    },
  );

  it('security-redact-01 uses the canonical AC path sample', () => {
    const fixturePath = path.join(
      FIXTURE_ROOT,
      'security/security-redact-01.json',
    );
    const fixture = JSON.parse(fs.readFileSync(fixturePath, 'utf8'));
    expect(fixture.input.rawPath).toBe(
      '/storage/emulated/0/DCIM/test.jpg',
    );
  });
});
