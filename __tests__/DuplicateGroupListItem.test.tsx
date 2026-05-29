import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {DuplicateGroupListItem} from '../src/components/DuplicateGroupListItem';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('DuplicateGroupListItem', () => {
  const baseGroup = {
    groupId: 42,
    matchKind: 'EXACT_BYTES' as const,
    memberCount: 2,
    reclaimableBytesEst: 2048,
    thumbnails: [
      {fileEntryId: 1, mediaTypeHint: 'image' as const},
      {fileEntryId: 2, mediaTypeHint: 'image' as const},
    ],
  };

  it('shows match kind, media type, and reclaimable copy (US-09)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupListItem group={baseGroup} />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.match.exact.label);
    expect(json).toContain(tokens.group.mediaType.image);
    expect(json).toContain('You can free up');
  });

  it('announces match kind before count (AC-a11y-match-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupListItem group={baseGroup} />,
      );
    });

    const row = findByTestId(tree!.root, 'duplicate-group-list-item');
    expect(row?.props.accessibilityLabel).toBe('Identical files, 2 copies');
  });

  it('shows overflow cell when memberCount exceeds grid max', () => {
    const group = {
      ...baseGroup,
      memberCount: 6,
      thumbnails: Array.from({length: 6}, (_, index) => ({
        fileEntryId: index + 1,
        mediaTypeHint: 'video' as const,
      })),
    };

    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<DuplicateGroupListItem group={group} />);
    });

    expect(
      findByTestId(tree!.root, 'duplicate-group-list-item-thumbnails-overflow-3'),
    ).not.toBeNull();
  });

  it('invokes onPress with group id', () => {
    const onPress = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupListItem group={baseGroup} onPress={onPress} />,
      );
    });

    const row = findByTestId(tree!.root, 'duplicate-group-list-item');
    ReactTestRenderer.act(() => {
      row?.props.onPress();
    });
    expect(onPress).toHaveBeenCalledWith(42);
  });
});
