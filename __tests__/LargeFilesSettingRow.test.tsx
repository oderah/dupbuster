import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {LargeFilesSettingRow} from '../src/components/LargeFilesSettingRow';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('LargeFilesSettingRow', () => {
  it('shows frozen settings.largeFiles label (FR-FP-03)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <LargeFilesSettingRow value={false} onValueChange={jest.fn()} />,
      );
    });

    expect(JSON.stringify(tree!.toJSON())).toContain(tokens.settings.largeFiles);
  });

  it('calls onValueChange when switch toggles', () => {
    const onValueChange = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <LargeFilesSettingRow value={false} onValueChange={onValueChange} />,
      );
    });

    const toggle = findByTestId(tree!.root, 'large-files-setting-switch');
    ReactTestRenderer.act(() => {
      toggle?.props.onValueChange(true);
    });
    expect(onValueChange).toHaveBeenCalledWith(true);
  });

  it('reflects checked accessibility state when enabled', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <LargeFilesSettingRow value={true} onValueChange={jest.fn()} />,
      );
    });

    const toggle = findByTestId(tree!.root, 'large-files-setting-switch');
    expect(toggle?.props.accessibilityState).toEqual({checked: true});
  });
});
