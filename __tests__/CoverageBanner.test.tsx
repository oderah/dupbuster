import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {CoverageBanner} from '../src/components/CoverageBanner';
import {tokens} from '../src/tokens/tokens';

const baseProps = {
  coverageSessionKey: 'partial',
  firstDisplayAlertEligible: true,
  dismissedForSession: false,
  onDismiss: jest.fn(),
  onExpandCoverage: jest.fn(),
  onOpenSettings: jest.fn(),
};

function renderBanner(
  overrides: Partial<React.ComponentProps<typeof CoverageBanner>> = {},
) {
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(
      <CoverageBanner variant="partial" {...baseProps} {...overrides} />,
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

describe('CoverageBanner', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when dismissed for session', () => {
    const tree = renderBanner({dismissedForSession: true});
    expect(tree.toJSON()).toBeNull();
  });

  it('shows partial.android copy for partial variant', () => {
    const tree = renderBanner({variant: 'partial'});
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityLabel).toBe(tokens.partial.android);
  });

  it('shows limited-library copy with count', () => {
    const tree = renderBanner({
      variant: 'limited-library',
      coverageSessionKey: 'limited-library:7',
      limitedLibraryCount: 7,
      firstDisplayAlertEligible: false,
    });
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityLabel).toBe(
      'Scanning 7 photos and videos you selected. Your full library isn\'t included.',
    );
  });

  it('shows denied.blocking copy for denied variant', () => {
    const tree = renderBanner({
      variant: 'denied',
      coverageSessionKey: 'denied',
    });
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityLabel).toBe(tokens.denied.blocking);
  });

  it('uses alert role on first partial display (AC-a11y-coverage-01)', () => {
    const tree = renderBanner({
      variant: 'partial',
      firstDisplayAlertEligible: true,
    });
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityRole).toBe('alert');
  });

  it('uses polite live region on repeat partial display', () => {
    const tree = renderBanner({
      variant: 'partial',
      firstDisplayAlertEligible: false,
    });
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityRole).toBeUndefined();
    expect(banner?.props.accessibilityLiveRegion).toBe('polite');
  });

  it('is polite always for limited-library (AC-a11y-coverage-02)', () => {
    const tree = renderBanner({
      variant: 'limited-library',
      limitedLibraryCount: 3,
      firstDisplayAlertEligible: false,
    });
    const banner = findByTestId(tree.root, 'coverage-banner');
    expect(banner?.props.accessibilityRole).toBeUndefined();
    expect(banner?.props.accessibilityLiveRegion).toBe('polite');
  });

  it('invokes onDismiss from dismiss control (AC-a11y-coverage-03)', () => {
    const onDismiss = jest.fn();
    const tree = renderBanner({onDismiss});
    const dismiss = findByTestId(tree.root, 'coverage-banner-dismiss');
    ReactTestRenderer.act(() => {
      dismiss?.props.onPress();
    });
    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(dismiss?.props.accessibilityLabel).toBe(
      tokens.a11y.coverage.dismiss,
    );
  });

  it('invokes onExpandCoverage from partial CTA', () => {
    const onExpandCoverage = jest.fn();
    const tree = renderBanner({onExpandCoverage});
    const cta = findByTestId(tree.root, 'coverage-banner-cta');
    ReactTestRenderer.act(() => {
      cta?.props.onPress();
    });
    expect(onExpandCoverage).toHaveBeenCalledTimes(1);
  });

  it('invokes onOpenSettings from denied CTA with link role', () => {
    const onOpenSettings = jest.fn();
    const tree = renderBanner({variant: 'denied', onOpenSettings});
    const cta = findByTestId(tree.root, 'coverage-banner-cta');
    expect(cta?.props.accessibilityRole).toBe('link');
    expect(cta?.props.accessibilityHint).toBe(
      tokens.a11y.coverage.settingsHint,
    );
    ReactTestRenderer.act(() => {
      cta?.props.onPress();
    });
    expect(onOpenSettings).toHaveBeenCalledTimes(1);
  });
});
