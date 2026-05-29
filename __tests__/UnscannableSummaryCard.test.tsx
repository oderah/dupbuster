import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {UnscannableSummaryCard} from '../src/components/UnscannableSummaryCard';
import {tokens} from '../src/tokens/tokens';
import {UNSCANNABLE_REASONS} from '../src/types/scanEngine';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('UnscannableSummaryCard', () => {
  it('renders nothing when all counts are zero', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <UnscannableSummaryCard countsByReason={{}} />,
      );
    });
    expect(tree!.toJSON()).toBeNull();
  });

  it('shows title and row per non-zero reason (AC-unscan-01)', () => {
    const counts = Object.fromEntries(
      UNSCANNABLE_REASONS.map((reason, index) => [reason, index + 1]),
    ) as Record<(typeof UNSCANNABLE_REASONS)[number], number>;

    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <UnscannableSummaryCard countsByReason={counts} />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.unscan.title);
    for (const reason of UNSCANNABLE_REASONS) {
      expect(findByTestId(tree!.root, `unscannable-summary-card-row-${reason}`))
        .not.toBeNull();
    }
  });

  it('shows Retry CTA for HASH_TIMEOUT (AC-unscan-04)', () => {
    const onRetryHashTimeout = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <UnscannableSummaryCard
          countsByReason={{HASH_TIMEOUT: 4}}
          onRetryHashTimeout={onRetryHashTimeout}
        />,
      );
    });

    const cta = findByTestId(tree!.root, 'unscannable-summary-card-cta-HASH_TIMEOUT');
    expect(cta?.props.accessibilityLabel).toBe(tokens.unscan.retry);
    ReactTestRenderer.act(() => {
      cta?.props.onPress();
    });
    expect(onRetryHashTimeout).toHaveBeenCalledTimes(1);
  });

  it('shows enable-large-files CTA for LARGE_SKIPPED (AC-unscan-03)', () => {
    const onEnableLargeFiles = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <UnscannableSummaryCard
          countsByReason={{LARGE_SKIPPED: 2}}
          onEnableLargeFiles={onEnableLargeFiles}
        />,
      );
    });

    const cta = findByTestId(
      tree!.root,
      'unscannable-summary-card-cta-LARGE_SKIPPED',
    );
    expect(cta?.props.accessibilityLabel).toBe(tokens.unscan.cta.enableLargeFiles);
    ReactTestRenderer.act(() => {
      cta?.props.onPress();
    });
    expect(onEnableLargeFiles).toHaveBeenCalledTimes(1);
  });

  it('omits CTAs for reasons without actions', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <UnscannableSummaryCard countsByReason={{ENCRYPTED: 1}} />,
      );
    });
    expect(
      findByTestId(tree!.root, 'unscannable-summary-card-cta-ENCRYPTED'),
    ).toBeNull();
  });
});
