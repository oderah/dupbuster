import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {CrashAnalyticsSettingRow} from '../src/components/CrashAnalyticsSettingRow';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('CrashAnalyticsSettingRow', () => {
  it('shows frozen settings.crashAnalytics label (US-17)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <CrashAnalyticsSettingRow value={false} onValueChange={jest.fn()} />,
      );
    });

    expect(JSON.stringify(tree!.toJSON())).toContain(
      tokens.settings.crashAnalytics,
    );
  });

  it('defaults to unchecked switch', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <CrashAnalyticsSettingRow value={false} onValueChange={jest.fn()} />,
      );
    });

    const toggle = findByTestId(tree!.root, 'crash-analytics-setting-switch');
    expect(toggle?.props.value).toBe(false);
    expect(toggle?.props.accessibilityState).toEqual({checked: false});
  });

  it('calls onValueChange when switch toggles', () => {
    const onValueChange = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <CrashAnalyticsSettingRow value={false} onValueChange={onValueChange} />,
      );
    });

    const toggle = findByTestId(tree!.root, 'crash-analytics-setting-switch');
    ReactTestRenderer.act(() => {
      toggle?.props.onValueChange(true);
    });
    expect(onValueChange).toHaveBeenCalledWith(true);
  });
});
