import fs from 'fs';
import path from 'path';

import {tokens} from '../src/tokens/tokens';

const repoRoot = path.join(__dirname, '..');
const runbookPath = path.join(
  repoRoot,
  'tests/manual/m4-testflight-external-beta.md',
);
const infoPlistPath = path.join(repoRoot, 'ios/Dupbuster/Info.plist');
const privacyManifestPath = path.join(
  repoRoot,
  'ios/Dupbuster/PrivacyInfo.xcprivacy',
);

const runbook = fs.readFileSync(runbookPath, 'utf8');
const infoPlist = fs.readFileSync(infoPlistPath, 'utf8');
const privacyManifest = fs.readFileSync(privacyManifestPath, 'utf8');

const REQUIRED_IOS_VERSIONS = ['15', '17', '18'] as const;
const REQUIRED_ROW_IDS = [
  'M4-TFEXT-01',
  'M4-TFEXT-03',
  'M4-TFEXT-06',
  'M4-TFEXT-12',
] as const;

const BANNED_INFO_PLIST_KEYS = [
  'NSLocationWhenInUseUsageDescription',
  'NSLocationAlwaysUsageDescription',
  'NSContactsUsageDescription',
  'NSCameraUsageDescription',
  'NSMicrophoneUsageDescription',
] as const;

function readPlistString(key: string): string | undefined {
  const keyPattern = new RegExp(
    `<key>${key}</key>\\s*<string>([\\s\\S]*?)</string>`,
  );
  return infoPlist.match(keyPattern)?.[1]?.trim();
}

describe('TestFlight external beta contract (M4-12)', () => {
  it('manual runbook exists and is tagged M4-12', () => {
    expect(fs.existsSync(runbookPath)).toBe(true);
    expect(runbook).toMatch(/M4-12/);
    expect(runbook).toMatch(/TestFlight external beta/);
  });

  it('runbook requires iOS 15, 17, and 18 coverage', () => {
    for (const version of REQUIRED_IOS_VERSIONS) {
      expect(runbook).toMatch(new RegExp(`iOS\\s*${version}|iOS${version}`));
    }
  });

  it('runbook requires M4-10 matrix on the same build', () => {
    expect(runbook).toMatch(/M4-10/);
    expect(runbook).toContain('m4-store-matrix-qa.md');
  });

  it('runbook includes required checklist row IDs', () => {
    for (const id of REQUIRED_ROW_IDS) {
      expect(runbook).toContain(id);
    }
  });

  it('runbook links App Privacy alignment', () => {
    expect(runbook).toMatch(/ios-app-privacy-answers\.md/);
  });

  it('runbook states independence from Play pre-launch', () => {
    expect(runbook).toMatch(/M4-11/);
    expect(runbook).toMatch(/does not satisfy|Independent of Android/i);
  });

  it('Info.plist matches external-beta privacy policy', () => {
    expect(readPlistString('NSPhotoLibraryUsageDescription')).toBe(
      tokens.denied.blocking,
    );
    for (const key of BANNED_INFO_PLIST_KEYS) {
      expect(infoPlist).not.toContain(key);
    }
    expect(infoPlist).toContain(
      'com.dupbuster.scan.background-continuation.v1',
    );
    expect(infoPlist).toMatch(/<key>UIBackgroundModes<\/key>/);
    expect(infoPlist).toContain('<string>processing</string>');
  });

  it('PrivacyInfo.xcprivacy declares no collected data and no tracking', () => {
    const collectedEmpty =
      /<key>NSPrivacyCollectedDataTypes<\/key>\s*<array\s*\/>/.test(
        privacyManifest,
      ) ||
      /<key>NSPrivacyCollectedDataTypes<\/key>\s*<array>\s*<\/array>/.test(
        privacyManifest,
      );
    const trackingFalse = /<key>NSPrivacyTracking<\/key>\s*<false\s*\/>/.test(
      privacyManifest,
    );
    expect(collectedEmpty).toBe(true);
    expect(trackingFalse).toBe(true);
  });
});
