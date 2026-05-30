import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {ContentMatchNotice} from '../src/components/ContentMatchNotice';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('ContentMatchNotice (M2-13)', () => {
  it('renders frozen trust copy for SAME_CONTENT_VIDEO (AC-a11y-match-02)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <ContentMatchNotice matchKind="SAME_CONTENT_VIDEO" />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.match.videoContent.notice);
  });

  it('uses polite live region on mount (AC-a11y-match-02)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <ContentMatchNotice matchKind="SAME_CONTENT_VIDEO" />,
      );
    });

    const notice = findByTestId(tree!.root, 'content-match-notice');
    expect(notice?.props.accessibilityLiveRegion).toBe('polite');
    expect(notice?.props.accessibilityRole).toBe('text');
    expect(notice?.props.accessibilityLabel).toBe(
      tokens.match.videoContent.notice,
    );
  });

  it('renders nothing for EXACT_BYTES (AC-a11y-match-02)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <ContentMatchNotice matchKind="EXACT_BYTES" />,
      );
    });

    expect(tree!.toJSON()).toBeNull();
  });
});
