import {
  buildDetailThumbnailSlots,
  buildListThumbnailSlots,
  formatGroupAccessibilityLabel,
  formatGroupReclaimableLine,
  formatOverflowLabel,
  getMatchKindLabel,
  getMediaTypeLabel,
  resolveGroupMediaTypeSummary,
  summarizeGroupMediaTypes,
} from '../src/components/duplicateGroupDisplay';
import {tokens} from '../src/tokens/tokens';
import {DUPLICATE_GROUP_THUMBNAIL_GRID_MAX} from '../src/types/scanEngine';

describe('duplicateGroupDisplay', () => {
  it('maps match kinds to frozen labels', () => {
    expect(getMatchKindLabel('EXACT_BYTES')).toBe(tokens.match.exact.label);
    expect(getMatchKindLabel('SAME_CONTENT_VIDEO')).toBe(
      tokens.match.videoContent.label,
    );
  });

  it('formats group a11y with match kind before count (AC-a11y-match-01)', () => {
    expect(formatGroupAccessibilityLabel('EXACT_BYTES', 3)).toBe(
      'Identical files, 3 copies',
    );
    expect(formatGroupAccessibilityLabel('SAME_CONTENT_VIDEO', 2)).toBe(
      'Same video at different quality, 2 files',
    );
  });

  it('summarizes media types for file-type display (US-09)', () => {
    expect(summarizeGroupMediaTypes(['image', 'image'])).toBe('Photo');
    expect(summarizeGroupMediaTypes(['video', 'image'])).toBe(
      tokens.group.mediaType.mixed,
    );
  });

  it('builds list thumbnail slots with overflow when memberCount > grid max', () => {
    const thumbs = Array.from({length: 6}, (_, index) => ({
      fileEntryId: index + 1,
      mediaTypeHint: 'image' as const,
    }));
    const slots = buildListThumbnailSlots(thumbs, 6);
    expect(slots).toHaveLength(DUPLICATE_GROUP_THUMBNAIL_GRID_MAX);
    expect(slots.slice(0, 3).every(slot => slot.kind === 'thumbnail')).toBe(
      true,
    );
    expect(slots[3]).toMatchObject({
      kind: 'overflow',
      overflowCount: 3,
    });
    expect(formatOverflowLabel(3)).toBe('+3');
  });

  it('shows all thumbnails without overflow when memberCount <= grid max', () => {
    const thumbs = [
      {fileEntryId: 1, mediaTypeHint: 'video' as const},
      {fileEntryId: 2, mediaTypeHint: 'video' as const},
    ];
    const slots = buildListThumbnailSlots(thumbs, 2);
    expect(slots).toHaveLength(2);
    expect(slots.every(slot => slot.kind === 'thumbnail')).toBe(true);
  });

  it('builds detail slots for every member', () => {
    const slots = buildDetailThumbnailSlots([
      {
        fileEntryId: 10,
        displayName: 'clip.mp4',
        sizeBytes: 1024,
        mtimeMs: 1,
        pathLength: 8,
        mediaTypeHint: 'video',
      },
    ]);
    expect(slots).toHaveLength(1);
    expect(slots[0]).toMatchObject({
      kind: 'thumbnail',
      fileEntryId: 10,
    });
  });

  it('formats reclaimable line from frozen token', () => {
    expect(formatGroupReclaimableLine(1_048_576)).toBe('You can free up 1 MB');
  });

  it('resolves media type labels from frozen tokens', () => {
    expect(getMediaTypeLabel('document')).toBe('Document');
    expect(resolveGroupMediaTypeSummary({
      memberCount: 2,
      thumbnails: [
        {fileEntryId: 1, mediaTypeHint: 'text'},
        {fileEntryId: 2, mediaTypeHint: 'text'},
      ],
    })).toBe('Text');
  });
});
