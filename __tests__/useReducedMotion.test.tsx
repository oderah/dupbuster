import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import {AccessibilityInfo, Text} from 'react-native';

import {useReducedMotion} from '../src/hooks/useReducedMotion';

function ReducedMotionProbe(): React.JSX.Element {
  const reducedMotion = useReducedMotion();
  return <Text testID="reduced-motion">{reducedMotion ? 'on' : 'off'}</Text>;
}

describe('useReducedMotion', () => {
  const remove = jest.fn();
  let reduceMotionHandler: ((enabled: boolean) => void) | undefined;

  beforeEach(() => {
    reduceMotionHandler = undefined;
    remove.mockClear();
    jest.spyOn(AccessibilityInfo, 'isReduceMotionEnabled').mockResolvedValue(false);
    jest.spyOn(AccessibilityInfo, 'addEventListener').mockImplementation((event, handler) => {
      if (event === 'reduceMotionChanged') {
        reduceMotionHandler = handler as (enabled: boolean) => void;
      }
      return {remove};
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  async function renderProbe(): Promise<ReactTestRenderer.ReactTestRenderer> {
    let tree!: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      tree = ReactTestRenderer.create(<ReducedMotionProbe />);
      await Promise.resolve();
    });
    return tree;
  }

  function readValue(tree: ReactTestRenderer.ReactTestRenderer): string {
    return tree.root.findByProps({testID: 'reduced-motion'}).props.children;
  }

  it('reads initial reduce-motion setting from AccessibilityInfo', async () => {
    jest.spyOn(AccessibilityInfo, 'isReduceMotionEnabled').mockResolvedValue(true);

    const tree = await renderProbe();

    expect(readValue(tree)).toBe('on');
  });

  it('updates when reduceMotionChanged fires', async () => {
    const tree = await renderProbe();
    expect(readValue(tree)).toBe('off');

    await ReactTestRenderer.act(() => {
      reduceMotionHandler?.(true);
    });

    expect(readValue(tree)).toBe('on');
  });

  it('removes reduceMotionChanged listener on unmount', async () => {
    const tree = await renderProbe();

    await ReactTestRenderer.act(() => {
      tree.unmount();
    });

    expect(remove).toHaveBeenCalledTimes(1);
  });
});
