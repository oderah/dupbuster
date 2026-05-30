import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {RescanPromptBanner} from '../src/components/RescanPromptBanner';
import {tokens} from '../src/tokens/tokens';

const baseProps = {
  rescanSessionKey: 'schema:2',
  firstDisplayAlertEligible: true,
  dismissedForSession: false,
  onDismiss: jest.fn(),
  onRescan: jest.fn(),
};

function renderBanner(
  overrides: Partial<React.ComponentProps<typeof RescanPromptBanner>> = {},
) {
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(
      <RescanPromptBanner {...baseProps} {...overrides} />,
    );
  });
  return tree!;
}

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('RescanPromptBanner', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when dismissed for session', () => {
    const tree = renderBanner({dismissedForSession: true});
    expect(tree.toJSON()).toBeNull();
  });

  it('shows rescan.required copy (FR-IX-05)', () => {
    const tree = renderBanner();
    const banner = findByTestId(tree.root, 'rescan-prompt-banner');
    expect(banner?.props.accessibilityLabel).toBe(tokens.rescan.required);
  });

  it('uses alert role on first display', () => {
    const tree = renderBanner({firstDisplayAlertEligible: true});
    const banner = findByTestId(tree.root, 'rescan-prompt-banner');
    expect(banner?.props.accessibilityRole).toBe('alert');
  });

  it('uses polite live region on repeat display', () => {
    const tree = renderBanner({firstDisplayAlertEligible: false});
    const banner = findByTestId(tree.root, 'rescan-prompt-banner');
    expect(banner?.props.accessibilityRole).toBeUndefined();
    expect(banner?.props.accessibilityLiveRegion).toBe('polite');
  });

  it('invokes onRescan from CTA with notification.scan.title label', () => {
    const onRescan = jest.fn();
    const tree = renderBanner({onRescan});
    const cta = findByTestId(tree.root, 'rescan-prompt-banner-cta');
    expect(cta?.props.accessibilityLabel).toBe(tokens.notification.scan.title);
    ReactTestRenderer.act(() => {
      cta?.props.onPress();
    });
    expect(onRescan).toHaveBeenCalledTimes(1);
  });

  it('invokes onDismiss from dismiss control', () => {
    const onDismiss = jest.fn();
    const tree = renderBanner({onDismiss});
    const dismiss = findByTestId(tree.root, 'rescan-prompt-banner-dismiss');
    ReactTestRenderer.act(() => {
      dismiss?.props.onPress();
    });
    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(dismiss?.props.accessibilityLabel).toBe(
      tokens.a11y.coverage.dismiss,
    );
  });
});
