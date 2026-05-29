import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {DuplicateGroupDetailScreen} from '../src/screens/DuplicateGroupDetailScreen';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('DuplicateGroupDetailScreen', () => {
  const group = {
    groupId: 7,
    matchKind: 'SAME_CONTENT_VIDEO' as const,
    memberCount: 2,
    reclaimableBytesEst: 5_000_000,
    members: [
      {
        fileEntryId: 101,
        displayName: 'vacation-1080p.mp4',
        sizeBytes: 4_000_000,
        mediaTypeHint: 'video' as const,
      },
      {
        fileEntryId: 102,
        displayName: 'vacation-720p.mp4',
        sizeBytes: 1_000_000,
        mediaTypeHint: 'video' as const,
      },
    ],
  };

  it('shows match kind, reclaimable, and member rows', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.match.videoContent.label);
    expect(json).toContain('You can free up');
    expect(json).toContain('vacation-1080p.mp4');
    expect(json).toContain('vacation-720p.mp4');
  });

  it('renders thumbnail grid for all members', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} />,
      );
    });

    expect(findByTestId(tree!.root, 'duplicate-group-detail-grid-thumb-101'))
      .not.toBeNull();
    expect(findByTestId(tree!.root, 'duplicate-group-detail-grid-thumb-102'))
      .not.toBeNull();
  });

  it('uses group a11y label on scroll container (AC-a11y-match-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} />,
      );
    });

    const screen = findByTestId(tree!.root, 'duplicate-group-detail');
    expect(screen?.props.accessibilityLabel).toBe(
      'Same video at different quality, 2 files',
    );
  });
});
