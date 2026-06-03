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
  mustNotOpen?: boolean;
  validation?: string;
  unscannableReason?: string;
};

type FixtureDescriptor = {
  id: string;
  platform?: string;
  input: {
    rawPath?: string;
    treeGrant?: string;
    candidate?: string;
    provenance?: string;
  };
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

function loadManifest(): Manifest {
  return JSON.parse(
    fs.readFileSync(path.join(FIXTURE_ROOT, 'manifest.json'), 'utf8'),
  );
}

function loadFixture(relativePath: string): FixtureDescriptor {
  return JSON.parse(
    fs.readFileSync(path.join(FIXTURE_ROOT, relativePath), 'utf8'),
  );
}

function loadCiGateFixtures(): Array<{path: string; fixture: FixtureDescriptor}> {
  const manifest = loadManifest();
  const rows: Array<{path: string; fixture: FixtureDescriptor}> = [];
  for (const row of manifest.fixtures) {
    const fixture = loadFixture(row.path);
    if (fixture.expect?.ciGate === true) {
      rows.push({path: row.path, fixture});
    }
  }
  return rows;
}

describe('security CI gates (M4-05 / M4-06)', () => {
  const ciGateFixtures = loadCiGateFixtures();

  it('lists AC-security-redact-01 with ciGate in the manifest matrix', () => {
    const redact = ciGateFixtures.find(
      row => row.fixture.id === 'security-redact-01',
    );
    expect(redact).toBeDefined();
    expect(redact?.fixture.acceptanceCriteria).toContain(
      'AC-security-redact-01',
    );
  });

  it('lists AC-security-uri-01 with ciGate in the manifest matrix', () => {
    const uri = ciGateFixtures.find(row => row.fixture.id === 'security-uri-01');
    expect(uri).toBeDefined();
    expect(uri?.fixture.acceptanceCriteria).toContain('AC-security-uri-01');
  });

  it.each(
    ciGateFixtures
      .filter(row => row.fixture.expect.mustNotContainRawPath === true)
      .map(row => [row.path, row.fixture] as const),
  )('%s declares redact ciGate contract fields', (_relativePath, fixture) => {
    expect(fixture.expect.ciGate).toBe(true);
    expect(fixture.expect.mustNotContainRawPath).toBe(true);
    expect(fixture.input.rawPath).toBeDefined();
  });

  it.each(
    ciGateFixtures
      .filter(row => row.fixture.expect.mustNotOpen === true)
      .map(row => [row.path, row.fixture] as const),
  )('%s declares uri ciGate contract fields', (_relativePath, fixture) => {
    expect(fixture.expect.ciGate).toBe(true);
    expect(fixture.expect.mustNotOpen).toBe(true);
    expect(fixture.expect.validation).toBe('denied');
    expect(fixture.expect.unscannableReason).toBe('PERMISSION_DENIED');
    expect(fixture.input.treeGrant).toBeDefined();
    expect(fixture.input.candidate).toBeDefined();
    expect(fixture.input.provenance).toBe('discovery');
  });

  it('security-redact-01 uses the canonical AC path sample', () => {
    const fixture = loadFixture('security/security-redact-01.json');
    expect(fixture.input.rawPath).toBe(
      '/storage/emulated/0/DCIM/test.jpg',
    );
  });

  it('security-uri-01 uses the canonical crafted SAF docId sample', () => {
    const fixture = loadFixture('security/security-uri-01.json');
    expect(fixture.platform).toBe('android');
    expect(fixture.input.candidate).toBe(
      'content://com.android.externalstorage.documents/document/primary%3ADocuments%2F..%2Fsecret',
    );
    expect(fixture.input.candidate).toContain('%2F..%2F');
  });
});
