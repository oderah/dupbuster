import {formatToken, tokens} from '../src/tokens/tokens';

describe('tokens EN freeze (requirements §9)', () => {
  it('partial.android is verbatim', () => {
    expect(tokens.partial.android).toBe(
      "Scanning media and downloads you've granted access to. Some folders aren't visible without choosing a folder.",
    );
  });

  it('partial.ios.limited is verbatim', () => {
    expect(tokens.partial.ios.limited).toBe(
      "Scanning {count} photos and videos you selected. Your full library isn't included.",
    );
  });

  it('partial.cta.expand platform strings are verbatim', () => {
    expect(tokens.partial.cta.expandAndroid).toBe('Choose folder');
    expect(tokens.partial.cta.expandIos).toBe('Manage photo access');
  });

  it('denied.blocking is verbatim', () => {
    expect(tokens.denied.blocking).toBe(
      'DupBuster needs storage access to find duplicates.',
    );
  });

  it('notification scan strings are verbatim', () => {
    expect(tokens.notification.scan.title).toBe('Scanning for duplicates');
    expect(tokens.notification.scan.body).toBe(
      '{percent}% · {filesProcessed} files',
    );
    expect(tokens.notification.channel.scan).toBe('Duplicate scan');
  });

  it('keeper, settings, reclaimable, rescan, unscan strings are verbatim', () => {
    expect(tokens.keeper.rememberSession).toBe(
      'Use largest for the rest of this session',
    );
    expect(tokens.settings.largeFiles).toBe(
      'Hash files larger than 2 GB (uses more battery)',
    );
    expect(tokens.reclaimable.label).toBe('You can free up {size}');
    expect(tokens.rescan.required).toBe(
      'This update changed how files are compared. Rescan to refresh results.',
    );
    expect(tokens.unscan.retry).toBe('Retry');
  });

  it('coverage.footer matches requirements §5.3', () => {
    expect(tokens.coverage.footer).toBe(
      'Results cover authorized items only',
    );
  });
});

describe('tokens a11y freeze (architecture §9.3)', () => {
  it('a11y.coverage strings are verbatim', () => {
    expect(tokens.a11y.coverage.dismiss).toBe('Dismiss coverage notice');
    expect(tokens.a11y.coverage.dismissHint).toBe(
      'Hides this notice until next app launch',
    );
    expect(tokens.a11y.coverage.settingsHint).toBe('Opens system settings');
  });

  it('a11y.scan strings are verbatim', () => {
    expect(tokens.a11y.scan.progress).toBe(
      '{percent} percent, {filesProcessed} files scanned, {groupsFound} duplicate groups found',
    );
    expect(tokens.a11y.scan.discovering).toBe('Scanning, discovering files');
    expect(tokens.a11y.scan.complete).toBe('Scan complete');
    expect(tokens.a11y.scan.paused).toBe('Scan paused');
    expect(tokens.a11y.scan.error).toBe('Scan stopped with errors');
    expect(tokens.a11y.scan.cancelling).toBe('Cancelling scan');
    expect(tokens.a11y.scan.cancelled).toBe('Scan cancelled');
    expect(tokens.a11y.scan.pause).toBe('Pause scan');
    expect(tokens.a11y.scan.cancel).toBe('Cancel scan');
    expect(tokens.a11y.scan.videoContent).toBe('Analyzing video content');
  });

  it('a11y.keeper, group, path strings are verbatim', () => {
    expect(tokens.a11y.keeper.group).toBe('Choose file to keep');
    expect(tokens.a11y.group.exact).toBe('Identical files, {count} copies');
    expect(tokens.a11y.group.videoContent).toBe(
      'Same video at different quality, {count} files',
    );
    expect(tokens.a11y.path.sameFile).toBe('Same file, {n} locations');
  });
});

describe('tokens platform constants', () => {
  it('notification channel id is stable per requirements §9', () => {
    expect(tokens.platform.notification.channelId).toBe(
      'com.dupbuster.scan.foreground.v1',
    );
  });

  it('touch target minimum meets NFR-08', () => {
    expect(tokens.component.touchTargetMin).toBeGreaterThanOrEqual(44);
  });
});

describe('formatToken', () => {
  it('replaces placeholders in frozen templates', () => {
    expect(
      formatToken(tokens.reclaimable.label, {size: '1.2 GB'}),
    ).toBe('You can free up 1.2 GB');
    expect(
      formatToken(tokens.a11y.path.sameFile, {n: 3}),
    ).toBe('Same file, 3 locations');
  });
});
