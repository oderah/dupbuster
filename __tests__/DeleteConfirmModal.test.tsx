import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {DeleteConfirmModal} from '../src/components/DeleteConfirmModal';
import {tokens} from '../src/tokens/tokens';

function findByTestId(
  root: ReactTestRenderer.ReactTestInstance,
  testID: string,
): ReactTestRenderer.ReactTestInstance | null {
  return root.findAll(node => node.props.testID === testID)[0] ?? null;
}

describe('DeleteConfirmModal', () => {
  const handlers = {
    onCancel: jest.fn(),
    onContinue: jest.fn(),
    onConfirm: jest.fn(),
    onGoBack: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders review step with non-destructive primary (AC-action-delete-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="review"
          deleteCount={2}
          reclaimableBytes={1024}
          {...handlers}
        />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.delete.review.title);
    expect(json).toContain(tokens.delete.review.continue);
    expect(json).toContain(tokens.delete.review.cancel);
  });

  it('calls onContinue from review primary action', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="review"
          deleteCount={2}
          reclaimableBytes={1024}
          {...handlers}
        />,
      );
    });

    const primary = findByTestId(tree!.root, 'delete-confirm-modal-primary');
    ReactTestRenderer.act(() => {
      primary?.props.onPress();
    });
    expect(handlers.onContinue).toHaveBeenCalledTimes(1);
    expect(handlers.onConfirm).not.toHaveBeenCalled();
  });

  it('renders confirm step with destructive primary label', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="confirm"
          deleteCount={2}
          reclaimableBytes={1024}
          {...handlers}
        />,
      );
    });

    const json = JSON.stringify(tree!.toJSON());
    expect(json).toContain(tokens.delete.confirm.title);
    expect(json).toContain(tokens.delete.confirm.confirm);
    expect(json).toContain(tokens.delete.confirm.back);
  });

  it('calls onConfirm from confirm primary and onGoBack from secondary', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="confirm"
          deleteCount={2}
          reclaimableBytes={1024}
          {...handlers}
        />,
      );
    });

    ReactTestRenderer.act(() => {
      findByTestId(tree!.root, 'delete-confirm-modal-primary')?.props.onPress();
    });
    expect(handlers.onConfirm).toHaveBeenCalledTimes(1);

    ReactTestRenderer.act(() => {
      findByTestId(tree!.root, 'delete-confirm-modal-secondary')?.props.onPress();
    });
    expect(handlers.onGoBack).toHaveBeenCalledTimes(1);
  });

  it('uses accessibilityViewIsModal on Modal (AC-a11y-delete-01)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="review"
          deleteCount={1}
          reclaimableBytes={100}
          {...handlers}
        />,
      );
    });

    const modal = tree!.root.find(node => node.type === 'Modal');
    expect(modal?.props.accessibilityViewIsModal).toBe(true);
  });

  it('exposes header for initial focus (A11Y-07)', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <DeleteConfirmModal
          visible
          step="review"
          deleteCount={1}
          reclaimableBytes={100}
          {...handlers}
        />,
      );
    });

    const heading = findByTestId(tree!.root, 'delete-confirm-modal-heading');
    expect(heading?.props.accessibilityRole).toBe('header');
  });
});
