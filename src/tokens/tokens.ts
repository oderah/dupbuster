/**
 * DupBuster v1 design + copy tokens (EN freeze).
 * User-facing strings must match requirements.md §9 and architecture.md §9.3 verbatim.
 * Do not paraphrase in UI — import literals from this module.
 */

/** Replace `{key}` placeholders in frozen token templates. */
export function formatToken(
  template: string,
  params: Record<string, string | number>,
): string {
  return Object.entries(params).reduce(
    (result, [key, value]) =>
      result.replace(new RegExp(`\\{${key}\\}`, 'g'), String(value)),
    template,
  );
}

export const tokens = {
  color: {
    surface: {
      primary: '#FFFFFF',
      secondary: '#F5F5F7',
      caution: '#FFF8E6',
    },
    text: {
      primary: '#1C1C1E',
      secondary: '#636366',
      onCaution: '#5C4A00',
      onDanger: '#FFFFFF',
    },
    border: {
      default: '#D1D1D6',
      caution: '#FFE082',
    },
    action: {
      primary: '#007AFF',
      danger: '#D70015',
    },
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
  },
  radius: {
    sm: 4,
    md: 8,
    lg: 12,
  },
  typography: {
    body: {
      fontSize: 16,
      lineHeight: 24,
      fontWeight: '400' as const,
    },
    caption: {
      fontSize: 14,
      lineHeight: 20,
      fontWeight: '400' as const,
    },
    heading: {
      fontSize: 20,
      lineHeight: 28,
      fontWeight: '600' as const,
    },
  },
  elevation: {
    banner: 2,
    modal: 8,
  },
  component: {
    touchTargetMin: 44,
    coverageBanner: {
      minHeight: 56,
    },
    scanProgress: {
      height: 64,
    },
  },
  platform: {
    notification: {
      channelId: 'com.dupbuster.scan.foreground.v1',
    },
  },

  /** requirements.md §5.3 — scan summary footer when coverage is partial */
  coverage: {
    footer: 'Results cover authorized items only',
  },

  /** requirements.md §9 */
  partial: {
    android:
      "Scanning media and downloads you've granted access to. Some folders aren't visible without choosing a folder.",
    ios: {
      limited:
        "Scanning {count} photos and videos you selected. Your full library isn't included.",
    },
    cta: {
      expandAndroid: 'Choose folder',
      expandIos: 'Manage photo access',
    },
  },

  denied: {
    blocking: 'DupBuster needs storage access to find duplicates.',
  },

  notification: {
    scan: {
      title: 'Scanning for duplicates',
      body: '{percent}% · {filesProcessed} files',
    },
    channel: {
      scan: 'Duplicate scan',
    },
  },

  keeper: {
    rememberSession: 'Use largest for the rest of this session',
    education: {
      videoContent:
        'DupBuster can find the same video saved at different resolutions or in different formats. Files may look different but match by content. Always review the group before deleting.',
    },
  },

  settings: {
    largeFiles: 'Hash files larger than 2 GB (uses more battery)',
  },

  reclaimable: {
    label: 'You can free up {size}',
  },

  rescan: {
    required:
      'This update changed how files are compared. Rescan to refresh results.',
  },

  unscan: {
    retry: 'Retry',
  },

  /** architecture.md §9.3 — match-kind UI (EN freeze) */
  match: {
    exact: {
      label: 'Identical files',
    },
    videoContent: {
      label: 'Same video, different quality',
      notice:
        'These files contain the same video at different resolutions or formats. Review each file before choosing what to keep or delete.',
    },
  },

  scan: {
    phase: {
      videoContent: 'Analyzing video content…',
    },
  },

  /** architecture.md §9.3 — accessibility announcements */
  a11y: {
    coverage: {
      dismiss: 'Dismiss coverage notice',
      dismissHint: 'Hides this notice until next app launch',
      settingsHint: 'Opens system settings',
    },
    scan: {
      progress:
        '{percent} percent, {filesProcessed} files scanned, {groupsFound} duplicate groups found',
      discovering: 'Scanning, discovering files',
      complete: 'Scan complete',
      paused: 'Scan paused',
      error: 'Scan stopped with errors',
      cancelling: 'Cancelling scan',
      cancelled: 'Scan cancelled',
      pause: 'Pause scan',
      cancel: 'Cancel scan',
      videoContent: 'Analyzing video content',
    },
    keeper: {
      group: 'Choose file to keep',
    },
    group: {
      exact: 'Identical files, {count} copies',
      videoContent: 'Same video at different quality, {count} files',
    },
    path: {
      sameFile: 'Same file, {n} locations',
    },
  },
} as const;

export type Tokens = typeof tokens;
