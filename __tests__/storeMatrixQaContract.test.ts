import fs from 'fs';
import path from 'path';

const repoRoot = path.join(__dirname, '..');
const matrixPath = path.join(
  repoRoot,
  'tests/manual/m4-store-matrix-qa.md',
);

const matrix = fs.readFileSync(matrixPath, 'utf8');

const REQUIRED_ANDROID_APIS = ['26', '33', '34'] as const;
const REQUIRED_IOS_VERSIONS = ['15', '17', '18'] as const;
const REQUIRED_SMOKE_IDS = [
  'M4-SMOKE-01',
  'M4-SMOKE-07',
  'M4-SMOKE-09',
  'M4-SMOKE-14',
  'M4-SMOKE-20',
  'M4-SMOKE-27',
] as const;
const REQUIRED_AC_REFS = [
  'AC-integrity-cancel-01',
  'AC-security-uri-01',
  'AC-action-delete-01',
] as const;

describe('store matrix QA contract (M4-10)', () => {
  it('manual matrix document exists and is tagged M4-10', () => {
    expect(fs.existsSync(matrixPath)).toBe(true);
    expect(matrix).toMatch(/M4-10/);
    expect(matrix).toMatch(/Store-matrix QA/);
  });

  it('matrix declares Android API 26, 33, and 34 slots', () => {
    for (const api of REQUIRED_ANDROID_APIS) {
      expect(matrix).toMatch(new RegExp(`API\\s*${api}|A${api}`));
    }
  });

  it('matrix declares iOS 15, 17, and 18 slots', () => {
    for (const version of REQUIRED_IOS_VERSIONS) {
      expect(matrix).toMatch(new RegExp(`iOS\\s*${version}|iOS${version}`));
    }
  });

  it('matrix includes required smoke row IDs and AC references', () => {
    for (const id of REQUIRED_SMOKE_IDS) {
      expect(matrix).toContain(id);
    }
    for (const ac of REQUIRED_AC_REFS) {
      expect(matrix).toContain(ac);
    }
  });

  it('minSdkVersion remains 26 (matrix floor)', () => {
    const gradle = fs.readFileSync(
      path.join(repoRoot, 'android/build.gradle'),
      'utf8',
    );
    expect(gradle).toMatch(/minSdkVersion\s*=\s*26/);
  });
});
