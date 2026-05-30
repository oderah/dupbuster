import React from 'react';
import {Pressable, Text} from 'react-native';
import ReactTestRenderer from 'react-test-renderer';

import {runA11yGate} from '../../src/testing/a11yGate';

function renderRoot(element: React.ReactElement) {
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(element);
  });
  return tree!.root;
}

describe('a11yGate engine', () => {
  it('passes a labeled pressable with button role', () => {
    const root = renderRoot(
      <Pressable accessibilityRole="button" accessibilityLabel="Save">
        <Text>Save</Text>
      </Pressable>,
    );
    expect(runA11yGate(root)).toEqual([]);
  });

  it('flags pressable with invalid role', () => {
    const root = renderRoot(
      <Pressable accessibilityRole="text" accessibilityLabel="Save">
        <Text>Save</Text>
      </Pressable>,
    );
    const violations = runA11yGate(root);
    expect(violations.some(v => v.ruleId === 'pressable-role-required')).toBe(
      true,
    );
  });

  it('flags checkbox without checked state', () => {
    const root = renderRoot(
      <Pressable
        accessibilityRole="checkbox"
        accessibilityLabel="Remember"
        accessibilityState={{}}>
        <Text>Remember</Text>
      </Pressable>,
    );
    const violations = runA11yGate(root);
    expect(violations.some(v => v.ruleId === 'checked-state-required')).toBe(
      true,
    );
  });

  it('accepts checkbox with explicit checked state', () => {
    const root = renderRoot(
      <Pressable
        accessibilityRole="checkbox"
        accessibilityLabel="Remember"
        accessibilityState={{checked: false}}>
        <Text>Remember</Text>
      </Pressable>,
    );
    expect(runA11yGate(root)).toEqual([]);
  });
});

describe('toHaveZeroCriticalA11yViolations matcher', () => {
  it('reports formatted failures', () => {
    const root = renderRoot(
      <Pressable accessibilityRole="text" accessibilityLabel="Save">
        <Text>Save</Text>
      </Pressable>,
    );

    expect(() => expect(root).toHaveZeroCriticalA11yViolations()).toThrow(
      /pressable-role-required/,
    );
  });
});
