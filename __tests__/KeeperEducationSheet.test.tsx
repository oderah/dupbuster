import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {KeeperEducationSheet} from '../src/components/KeeperEducationSheet';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('KeeperEducationSheet', () => {
  it('shows general education copy when visible', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperEducationSheet
          visible
          matchKind="EXACT_BYTES"
          onDismiss={jest.fn()}
        />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.keeper.education.title);
    expect(json).toContain(tokens.keeper.education.general);
    expect(json).not.toContain(tokens.keeper.education.videoContent);
  });

  it('includes video education for SAME_CONTENT_VIDEO groups', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperEducationSheet
          visible
          matchKind="SAME_CONTENT_VIDEO"
          onDismiss={jest.fn()}
        />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.keeper.education.videoContent);
  });

  it('calls onDismiss from dismiss button', () => {
    const onDismiss = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperEducationSheet
          visible
          matchKind="EXACT_BYTES"
          onDismiss={onDismiss}
        />,
      );
    });

    ReactTestRenderer.act(() => {
      findByTestId(tree!.root, 'keeper-education-sheet-dismiss')?.props.onPress();
    });
    expect(onDismiss).toHaveBeenCalled();
  });
});
