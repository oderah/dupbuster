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

  it('denied.settingsCta is verbatim', () => {
    expect(tokens.denied.settingsCta).toBe('Open Settings');
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
    expect(tokens.unscan.title).toBe("Some files couldn't be scanned");
    expect(tokens.unscan.row).toBe('{count} {reason}');
    expect(tokens.unscan.retry).toBe('Retry');
    expect(tokens.unscan.cta.enableLargeFiles).toBe('Enable large file scanning');
  });

  it('unscan.reason labels cover the closed reason set (FR-UN-02)', () => {
    expect(tokens.unscan.reason.CLOUD_PLACEHOLDER).toBe('cloud-only files');
    expect(tokens.unscan.reason.ENCRYPTED).toBe('encrypted files');
    expect(tokens.unscan.reason.PERMISSION_DENIED).toBe('access denied');
    expect(tokens.unscan.reason.OFFLINE_ONLY).toBe('offline-only files');
    expect(tokens.unscan.reason.LOCKED).toBe('locked files');
    expect(tokens.unscan.reason.LARGE_SKIPPED).toBe('files over 2 GB');
    expect(tokens.unscan.reason.HASH_TIMEOUT).toBe('timed out while hashing');
    expect(tokens.unscan.reason.VIDEO_DECODE_FAILED).toBe('video decode failed');
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
    expect(tokens.a11y.scan.resume).toBe('Resume scan');
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

  it('group media type and overflow tokens are frozen', () => {
    expect(tokens.group.overflow).toBe('+{count}');
    expect(tokens.group.mediaType.image).toBe('Photo');
    expect(tokens.group.mediaType.video).toBe('Video');
    expect(tokens.group.mediaType.mixed).toBe('Mixed file types');
    expect(tokens.group.memberSize).toBe('{size}');
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

describe('tokens scan phase labels', () => {
  it('scan.phase labels match catalog phases', () => {
    expect(tokens.scan.phase.discovering).toBe('Discovering files');
    expect(tokens.scan.phase.hashing).toBe('Hashing files');
    expect(tokens.scan.phase.grouping).toBe('Finding duplicate groups');
    expect(tokens.scan.phase.videoContent).toBe('Analyzing video content…');
  });

  it('scan.stats templates are frozen', () => {
    expect(tokens.scan.stats.groups).toBe('{groupsFound} duplicate groups');
    expect(tokens.scan.stats.files).toBe('{filesProcessed} files scanned');
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
