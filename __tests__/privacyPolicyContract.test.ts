import fs from 'fs';
import path from 'path';

import {PRIVACY_POLICY_URL} from '../src/config/privacyPolicyUrl';
import {tokens} from '../src/tokens/tokens';

const repoRoot = path.join(__dirname, '..');
const policyPath = path.join(repoRoot, 'docs/store/privacy-policy.md');
const runbookPath = path.join(
  repoRoot,
  'tests/manual/m4-privacy-policy-url.md',
);

const policy = fs.readFileSync(policyPath, 'utf8');
const runbook = fs.readFileSync(runbookPath, 'utf8');

const REQUIRED_POLICY_PHRASES = [
  'on your device',
  'on your device only',
  'do not upload',
  'off by default',
] as const;

const REQUIRED_ROW_IDS = ['M4-PRIVACY-01'] as const;

describe('privacy policy contract (M4-14)', () => {
  it('privacy policy document exists and references canonical URL', () => {
    expect(fs.existsSync(policyPath)).toBe(true);
    expect(policy).toContain(PRIVACY_POLICY_URL);
    expect(policy).toMatch(/on.device/i);
  });

  it('policy states on-device processing and no file upload (NFR-06)', () => {
    const lower = policy.toLowerCase();
    for (const phrase of REQUIRED_POLICY_PHRASES) {
      expect(lower).toContain(phrase);
    }
    expect(policy).toMatch(/crash reports.*off by default|off by default.*crash/i);
  });

  it('canonical URL is HTTPS and matches config module', () => {
    expect(PRIVACY_POLICY_URL).toMatch(/^https:\/\//);
    const configSource = fs.readFileSync(
      path.join(repoRoot, 'src/config/privacyPolicyUrl.ts'),
      'utf8',
    );
    expect(configSource).toContain(PRIVACY_POLICY_URL);
  });

  it('settings.privacyPolicy token is frozen', () => {
    expect(tokens.settings.privacyPolicy).toBe('Privacy policy');
  });

  it('manual runbook exists and is tagged M4-14', () => {
    expect(fs.existsSync(runbookPath)).toBe(true);
    expect(runbook).toMatch(/M4-14/);
    expect(runbook).toMatch(/Privacy policy URL/);
    expect(runbook).toContain(PRIVACY_POLICY_URL);
  });

  it('runbook includes required checklist row IDs', () => {
    for (const id of REQUIRED_ROW_IDS) {
      expect(runbook).toContain(id);
    }
  });

  it('store disclosure docs link M4-14 policy', () => {
    const disclosure = fs.readFileSync(
      path.join(repoRoot, 'docs/store/privacy-store-disclosure.md'),
      'utf8',
    );
    expect(disclosure).toMatch(/privacy-policy\.md|M4-14/);
    const storeReadme = fs.readFileSync(
      path.join(repoRoot, 'docs/store/README.md'),
      'utf8',
    );
    expect(storeReadme).toMatch(/privacy-policy\.md/);
  });

  it('in-app PrivacyPolicyLink uses canonical URL', () => {
    const component = fs.readFileSync(
      path.join(repoRoot, 'src/components/PrivacyPolicyLink.tsx'),
      'utf8',
    );
    expect(component).toContain('PRIVACY_POLICY_URL');
    expect(component).toContain('tokens.settings.privacyPolicy');
  });
});
