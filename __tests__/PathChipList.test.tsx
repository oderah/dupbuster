import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {PathChipList} from '../src/components/PathChipList';
import {formatPathChipListAccessibilityLabel} from '../src/components/pathDisplay';
import {formatToken, tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('PathChipList', () => {
  const paths = [
    'content://test/photos/a.jpg',
    'content://test/mirror/a-link.jpg',
  ];

  it('renders nothing for a single path', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <PathChipList paths={['content://test/only.jpg']} />,
      );
    });

    expect(tree!.toJSON()).toBeNull();
  });

  it('renders path chips with single a11y label (AC-a11y-path-01 / A11Y-08)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<PathChipList paths={paths} />);
    });

    const list = findByTestId(tree!.root, 'path-chip-list');
    expect(list?.props.accessibilityLabel).toBe(
      formatToken(tokens.a11y.path.sameFile, {n: 2}),
    );
    expect(formatPathChipListAccessibilityLabel(2)).toBe('Same file, 2 locations');

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(paths[0]);
    expect(json).toContain(paths[1]);
  });

  it('uses LTR writing direction on path chip text (A11Y-10)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<PathChipList paths={paths} />);
    });

    const chip = findByTestId(tree!.root, 'path-chip-list-chip-0');
    expect(chip?.props.writingDirection).toBe('ltr');
  });
});
