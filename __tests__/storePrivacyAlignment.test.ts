import fs from 'fs';
import path from 'path';

import {tokens} from '../src/tokens/tokens';

const repoRoot = path.join(__dirname, '..');

function readPlistValue(plistPath: string, key: string): string | undefined {
  const xml = fs.readFileSync(plistPath, 'utf8');
  const keyPattern = new RegExp(
    `<key>${key}</key>\\s*<string>([\\s\\S]*?)</string>`,
  );
  const match = xml.match(keyPattern);
  return match?.[1]?.trim();
}

function readPrivacyManifest(): {
  collectedDataTypesEmpty: boolean;
  trackingFalse: boolean;
} {
  const xml = fs.readFileSync(
    path.join(repoRoot, 'ios/Dupbuster/PrivacyInfo.xcprivacy'),
    'utf8',
  );
  const collectedEmpty =
    /<key>NSPrivacyCollectedDataTypes<\/key>\s*<array\s*\/>/.test(xml) ||
    /<key>NSPrivacyCollectedDataTypes<\/key>\s*<array>\s*<\/array>/.test(xml);
  const trackingFalse = /<key>NSPrivacyTracking<\/key>\s*<false\s*\/>/.test(
    xml,
  );
  return {
    collectedDataTypesEmpty: collectedEmpty,
    trackingFalse,
  };
}

describe('store privacy alignment (M4-09)', () => {
  it('NSPhotoLibraryUsageDescription matches tokens.denied.blocking', () => {
    const usage = readPlistValue(
      path.join(repoRoot, 'ios/Dupbuster/Info.plist'),
      'NSPhotoLibraryUsageDescription',
    );
    expect(usage).toBe(tokens.denied.blocking);
  });

  it('does not declare unused location permission copy', () => {
    const plist = fs.readFileSync(
      path.join(repoRoot, 'ios/Dupbuster/Info.plist'),
      'utf8',
    );
    expect(plist).not.toContain('NSLocationWhenInUseUsageDescription');
  });

  it('PrivacyInfo.xcprivacy declares no collected data and no tracking', () => {
    const manifest = readPrivacyManifest();
    expect(manifest.collectedDataTypesEmpty).toBe(true);
    expect(manifest.trackingFalse).toBe(true);
  });

  it('Android manifest does not request location or contacts', () => {
    const manifest = fs.readFileSync(
      path.join(repoRoot, 'android/app/src/main/AndroidManifest.xml'),
      'utf8',
    );
    expect(manifest).not.toMatch(/ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION/);
    expect(manifest).not.toMatch(/READ_CONTACTS/);
  });
});
