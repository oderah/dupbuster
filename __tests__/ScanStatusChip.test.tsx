import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {ScanStatusChip} from '../src/components/ScanStatusChip';
import {tokens} from '../src/tokens/tokens';

describe('ScanStatusChip', () => {
  it('mirrors phase label for hashing', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<ScanStatusChip phase="hashing" />);
    });
    const chip = tree!.root.findByProps({testID: 'scan-status-chip'});
    expect(chip.props.accessibilityLabel).toBe(tokens.scan.phase.hashing);
  });

  it('binds cancelled phase label', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<ScanStatusChip phase="cancelled" />);
    });
    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.scan.phase.cancelled);
  });
});
