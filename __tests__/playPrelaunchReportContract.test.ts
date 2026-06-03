import fs from 'fs';
import path from 'path';

const repoRoot = path.join(__dirname, '..');
const runbookPath = path.join(
  repoRoot,
  'tests/manual/m4-play-prelaunch-report.md',
);
const manifestPath = path.join(
  repoRoot,
  'android/app/src/main/AndroidManifest.xml',
);

const runbook = fs.readFileSync(runbookPath, 'utf8');
const manifest = fs.readFileSync(manifestPath, 'utf8');

const REQUIRED_ANDROID_APIS = ['26', '33', '34'] as const;
const REQUIRED_ROW_IDS = [
  'M4-PRELAUNCH-01',
  'M4-PRELAUNCH-05',
  'M4-PRELAUNCH-06',
  'M4-PRELAUNCH-12',
] as const;

const REQUIRED_MANIFEST_PERMISSIONS = [
  'READ_MEDIA_IMAGES',
  'READ_MEDIA_VIDEO',
  'READ_MEDIA_AUDIO',
  'READ_EXTERNAL_STORAGE',
  'FOREGROUND_SERVICE',
  'FOREGROUND_SERVICE_DATA_SYNC',
] as const;

const BANNED_MANIFEST_PERMISSIONS = [
  'MANAGE_EXTERNAL_STORAGE',
  'ACCESS_FINE_LOCATION',
  'ACCESS_COARSE_LOCATION',
  'READ_CONTACTS',
  'WRITE_EXTERNAL_STORAGE',
] as const;

describe('Play pre-launch report contract (M4-11)', () => {
  it('manual runbook exists and is tagged M4-11', () => {
    expect(fs.existsSync(runbookPath)).toBe(true);
    expect(runbook).toMatch(/M4-11/);
    expect(runbook).toMatch(/Play pre-launch report/);
  });

  it('runbook requires API 26, 33, and 34 coverage', () => {
    for (const api of REQUIRED_ANDROID_APIS) {
      expect(runbook).toMatch(new RegExp(`API\\s*${api}`));
    }
  });

  it('runbook requires M4-10 matrix on the same AAB', () => {
    expect(runbook).toMatch(/M4-10/);
    expect(runbook).toContain('m4-store-matrix-qa.md');
  });

  it('runbook includes required checklist row IDs', () => {
    for (const id of REQUIRED_ROW_IDS) {
      expect(runbook).toContain(id);
    }
  });

  it('runbook links Play Data safety alignment', () => {
    expect(runbook).toMatch(/play-data-safety-answers\.md/);
  });

  it('Android manifest matches pre-launch permission policy', () => {
    for (const permission of REQUIRED_MANIFEST_PERMISSIONS) {
      expect(manifest).toContain(permission);
    }
    for (const permission of BANNED_MANIFEST_PERMISSIONS) {
      expect(manifest).not.toContain(permission);
    }
    expect(manifest).toMatch(/foregroundServiceType="dataSync"/);
    expect(manifest).toContain('ScanForegroundService');
  });
});
