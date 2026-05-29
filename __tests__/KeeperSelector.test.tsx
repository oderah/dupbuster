import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {KeeperSelector} from '../src/components/KeeperSelector';
import {createKeeperSelectionState} from '../src/controllers/keeperSelection';
import {tokens} from '../src/tokens/tokens';
import type {KeeperMember} from '../src/types/keeper';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

const members: KeeperMember[] = [
  {
    fileEntryId: 10,
    displayName: 'tiny.bin',
    sizeBytes: 50,
    mtimeMs: 100,
    pathLength: 30,
    mediaTypeHint: 'other',
  },
  {
    fileEntryId: 20,
    displayName: 'big.bin',
    sizeBytes: 9000,
    mtimeMs: 200,
    pathLength: 6,
    mediaTypeHint: 'other',
  },
];

describe('KeeperSelector', () => {
  it('uses radiogroup with frozen group label (AC-a11y-keeper-01)', () => {
    const selection = createKeeperSelectionState(members);
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperSelector
          members={members}
          selection={selection}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
        />,
      );
    });

    const group = findByTestId(tree!.root, 'keeper-selector');
    expect(group?.props.accessibilityRole).toBe('radiogroup');
    expect(group?.props.accessibilityLabel).toBe(tokens.a11y.keeper.group);
  });

  it('does not mark selected=true until explicit activation (AC-action-keeper-01)', () => {
    const selection = createKeeperSelectionState(members);
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperSelector
          members={members}
          selection={selection}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
        />,
      );
    });

    const largest = findByTestId(tree!.root, 'keeper-selector-member-20');
    expect(largest?.props.accessibilityState?.selected).toBe(false);
  });

  it('marks selected member after explicit activation', () => {
    const selection = {
      ...createKeeperSelectionState(members),
      explicitlyActivated: true,
      selectedFileEntryId: 20,
    };
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperSelector
          members={members}
          selection={selection}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
        />,
      );
    });

    const selected = findByTestId(tree!.root, 'keeper-selector-member-20');
    expect(selected?.props.accessibilityState?.selected).toBe(true);
  });

  it('invokes preset and member handlers', () => {
    const onSelectMember = jest.fn();
    const onApplyPreset = jest.fn();
    const selection = createKeeperSelectionState(members);
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperSelector
          members={members}
          selection={selection}
          onSelectMember={onSelectMember}
          onApplyPreset={onApplyPreset}
        />,
      );
    });

    ReactTestRenderer.act(() => {
      findByTestId(tree!.root, 'keeper-selector-preset-newest')?.props.onPress();
    });
    expect(onApplyPreset).toHaveBeenCalledWith('newest');

    ReactTestRenderer.act(() => {
      findByTestId(tree!.root, 'keeper-selector-member-10')?.props.onPress();
    });
    expect(onSelectMember).toHaveBeenCalledWith(10);
  });

  it('renders remember-session toggle from frozen token (FR-AC-05)', () => {
    const onRememberSessionChange = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <KeeperSelector
          members={members}
          selection={createKeeperSelectionState(members)}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
          showRememberSession
          rememberSession={false}
          onRememberSessionChange={onRememberSessionChange}
        />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.keeper.rememberSession);
  });
});
