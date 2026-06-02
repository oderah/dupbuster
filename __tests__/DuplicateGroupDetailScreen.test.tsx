import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {
  applyKeeperPreset,
  createKeeperSelectionState,
  selectKeeperMember,
} from '../src/controllers/keeperSelection';
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
        mtimeMs: 2000,
        pathLength: 22,
        mediaTypeHint: 'video' as const,
        paths: ['content://test/videos/vacation-1080p.mp4'],
      },
      {
        fileEntryId: 102,
        displayName: 'vacation-720p.mp4',
        sizeBytes: 1_000_000,
        mtimeMs: 1000,
        pathLength: 21,
        mediaTypeHint: 'video' as const,
        paths: ['content://test/videos/vacation-720p.mp4'],
      },
    ],
  };

  const keeperSelection = createKeeperSelectionState(group.members);
  const keeperProps = {
    keeperSelection,
    onSelectKeeper: jest.fn(),
    onApplyKeeperPreset: jest.fn(),
  };

  it('renders MatchKindBadge in header (M2-12)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    expect(
      findByTestId(tree!.root, 'duplicate-group-detail-match-kind-badge'),
    ).not.toBeNull();
  });

  it('shows match kind, reclaimable, and member rows', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
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
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    expect(findByTestId(tree!.root, 'duplicate-group-detail-grid-thumb-101'))
      .not.toBeNull();
    expect(findByTestId(tree!.root, 'duplicate-group-detail-grid-thumb-102'))
      .not.toBeNull();
  });

  it('renders ContentMatchNotice for SAME_CONTENT_VIDEO (M2-13)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    expect(
      findByTestId(tree!.root, 'duplicate-group-detail-content-match-notice'),
    ).not.toBeNull();
    expect(JSON.stringify(tree!.toJSON())).toContain(
      tokens.match.videoContent.notice,
    );
  });

  it('omits ContentMatchNotice for EXACT_BYTES (AC-a11y-match-02)', () => {
    const exactGroup = {
      ...group,
      matchKind: 'EXACT_BYTES' as const,
    };
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={exactGroup} {...keeperProps} />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).not.toContain(tokens.match.videoContent.notice);
  });

  it('places ContentMatchNotice before KeeperSelector (AC-a11y-match-04)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    const notice = findByTestId(
      tree!.root,
      'duplicate-group-detail-content-match-notice',
    );
    const keeper = findByTestId(tree!.root, 'duplicate-group-detail-keeper');
    expect(notice).not.toBeNull();
    expect(keeper).not.toBeNull();

    const noticeIndex = tree!.root.findAll(
      node => node === notice,
    )[0];
    const keeperIndex = tree!.root.findAll(
      node => node === keeper,
    )[0];
    const flat = tree!.root.findAll(() => true);
    expect(flat.indexOf(noticeIndex)).toBeLessThan(flat.indexOf(keeperIndex));
  });

  it('renders KeeperSelector radiogroup (M2-06)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    expect(findByTestId(tree!.root, 'duplicate-group-detail-keeper')).not.toBeNull();
  });

  it('renders delete trigger after KeeperSelector (AC-a11y-match-04)', () => {
    const activated = selectKeeperMember(keeperSelection, 101);
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen
          group={group}
          {...keeperProps}
          keeperSelection={activated}
          deleteEnabled
          onDeletePress={jest.fn()}
        />,
      );
    });

    const notice = findByTestId(
      tree!.root,
      'duplicate-group-detail-content-match-notice',
    );
    const keeper = findByTestId(tree!.root, 'duplicate-group-detail-keeper');
    const deleteTrigger = findByTestId(
      tree!.root,
      'duplicate-group-detail-delete-trigger',
    );
    const flat = tree!.root.findAll(() => true);
    expect(flat.indexOf(notice!)).toBeLessThan(flat.indexOf(keeper!));
    expect(flat.indexOf(keeper!)).toBeLessThan(flat.indexOf(deleteTrigger!));
    expect(JSON.stringify(tree!.toJSON())).toContain(tokens.delete.trigger);
  });

  it('disables delete trigger until keeper is explicitly selected (AC-action-keeper-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen
          group={group}
          {...keeperProps}
          deleteEnabled={false}
          onDeletePress={jest.fn()}
        />,
      );
    });

    const deleteTrigger = findByTestId(
      tree!.root,
      'duplicate-group-detail-delete-trigger',
    );
    expect(deleteTrigger?.props.accessibilityState?.disabled).toBe(true);
    expect(deleteTrigger?.props.disabled).toBe(true);
  });

  it('shows scan-time reclaimable preview before keeper activation', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    const reclaimable = findByTestId(tree!.root, 'duplicate-group-detail-reclaimable');
    expect(reclaimable).not.toBeNull();
    expect(JSON.stringify(tree!.toJSON())).toContain('You can free up');
  });

  it('updates reclaimable to non-keeper sum after keeper pick (AC-action-reclaim-01)', () => {
    const keepSmallest = applyKeeperPreset(
      createKeeperSelectionState(group.members),
      'smallest_file',
      group.members,
    );
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen
          group={group}
          {...keeperProps}
          keeperSelection={keepSmallest}
        />,
      );
    });

    const reclaimable = findByTestId(tree!.root, 'duplicate-group-detail-reclaimable');
    expect(reclaimable).not.toBeNull();
    expect(JSON.stringify(tree!.toJSON())).toContain('3.8 MB');
  });

  it('places reclaimable line after KeeperSelector and before delete trigger', () => {
    const activated = selectKeeperMember(keeperSelection, 101);
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen
          group={group}
          {...keeperProps}
          keeperSelection={activated}
          deleteEnabled
          onDeletePress={jest.fn()}
        />,
      );
    });

    const keeper = findByTestId(tree!.root, 'duplicate-group-detail-keeper');
    const reclaimable = findByTestId(
      tree!.root,
      'duplicate-group-detail-reclaimable',
    );
    const deleteTrigger = findByTestId(
      tree!.root,
      'duplicate-group-detail-delete-trigger',
    );
    const flat = tree!.root.findAll(() => true);
    expect(flat.indexOf(keeper!)).toBeLessThan(flat.indexOf(reclaimable!));
    expect(flat.indexOf(reclaimable!)).toBeLessThan(flat.indexOf(deleteTrigger!));
  });

  it('uses group a11y label on scroll container (AC-a11y-match-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DuplicateGroupDetailScreen group={group} {...keeperProps} />,
      );
    });

    const screen = findByTestId(tree!.root, 'duplicate-group-detail');
    expect(screen?.props.accessibilityLabel).toBe(
      'Same video at different quality, 2 files',
    );
  });
});
