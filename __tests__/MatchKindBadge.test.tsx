import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {MatchKindBadge} from '../src/components/MatchKindBadge';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('MatchKindBadge (M2-12)', () => {
  it.each([
    ['EXACT_BYTES', tokens.match.exact.label],
    ['SAME_CONTENT_VIDEO', tokens.match.videoContent.label],
  ] as const)('shows readable label for %s (AC-a11y-match-03)', (matchKind, label) => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<MatchKindBadge matchKind={matchKind} />);
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(label);
  });

  it.each([
    ['EXACT_BYTES', tokens.match.exact.label],
    ['SAME_CONTENT_VIDEO', tokens.match.videoContent.label],
  ] as const)(
    'uses label as accessibilityLabel for %s (AC-a11y-match-03)',
    (matchKind, label) => {
      let tree: ReactTestRenderer.ReactTestRenderer;
      ReactTestRenderer.act(() => {
        tree = ReactTestRenderer.create(<MatchKindBadge matchKind={matchKind} />);
      });

      const badge = findByTestId(tree!.root, 'match-kind-badge');
      expect(badge?.props.accessibilityRole).toBe('text');
      expect(badge?.props.accessibilityLabel).toBe(label);
    },
  );

  it('supports prominent presentation on detail screens', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <MatchKindBadge matchKind="SAME_CONTENT_VIDEO" presentation="prominent" />,
      );
    });

    expect(findByTestId(tree!.root, 'match-kind-badge')).not.toBeNull();
    expect(JSON.stringify(tree!.toJSON())).toContain(
      tokens.match.videoContent.label,
    );
  });
});
