import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {ScanProgress} from '../src/components/ScanProgress';
import {formatProgressAnnouncement} from '../src/controllers/scanProgressA11y';
import {tokens} from '../src/tokens/tokens';
import type {ScanProgressEvent} from '../src/types/scanEngine';

function progress(overrides: Partial<ScanProgressEvent> = {}): ScanProgressEvent {
  return {
    filesProcessed: 120,
    filesTotalKnown: 1000,
    groupsFound: 3,
    reclaimableBytesEst: 1_048_576,
    phase: 'hashing',
    ...overrides,
  };
}

function renderProgress(
  overrides: Partial<React.ComponentProps<typeof ScanProgress>> = {},
) {
  const snapshot = progress();
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(
      <ScanProgress
        progress={snapshot}
        accessibilityLabel={formatProgressAnnouncement(snapshot)}
        onPause={jest.fn()}
        onCancel={jest.fn()}
        {...overrides}
      />,
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

describe('ScanProgress', () => {
  it('renders nothing in idle phase', () => {
    const tree = renderProgress({
      progress: progress({phase: 'idle'}),
    });
    expect(tree.toJSON()).toBeNull();
  });

  it('uses polite live region (A11Y-04)', () => {
    const tree = renderProgress();
    const root = findByTestId(tree.root, 'scan-progress');
    expect(root?.props.accessibilityLiveRegion).toBe('polite');
  });

  it('shows phase title and notification percent line', () => {
    const tree = renderProgress({progress: progress({phase: 'discovering'})});
    const json = JSON.stringify(tree.toJSON());
    expect(json).toContain(tokens.scan.phase.discovering);
    expect(json).toContain('12% · 120 files');
  });

  it('shows video subcopy when hashing with video_content', () => {
    const tree = renderProgress({
      progress: progress({
        phase: 'hashing',
        contentKind: 'video_content',
      }),
    });
    const json = JSON.stringify(tree.toJSON());
    expect(json).toContain(tokens.scan.phase.videoContent);
  });

  it('uses pulse dot instead of fill when reducedMotion (A11Y-09)', () => {
    const tree = renderProgress({reducedMotion: true});
    expect(findByTestId(tree.root, 'scan-progress-pulse-dot')).not.toBeNull();
    expect(findByTestId(tree.root, 'scan-progress-fill')).toBeNull();
  });

  it('shows animated fill when reducedMotion is false', () => {
    const tree = renderProgress({reducedMotion: false});
    expect(findByTestId(tree.root, 'scan-progress-fill')).not.toBeNull();
  });

  it('shows sticky pause and cancel footer for active phases', () => {
    const tree = renderProgress({progress: progress({phase: 'hashing'})});
    expect(findByTestId(tree.root, 'scan-progress-footer')).not.toBeNull();
    expect(findByTestId(tree.root, 'scan-progress-pause')).not.toBeNull();
    expect(findByTestId(tree.root, 'scan-progress-cancel')).not.toBeNull();
  });

  it('shows resume instead of pause when paused', () => {
    const tree = renderProgress({
      progress: progress({phase: 'paused'}),
      onResume: jest.fn(),
    });
    expect(findByTestId(tree.root, 'scan-progress-resume')).not.toBeNull();
    expect(findByTestId(tree.root, 'scan-progress-pause')).toBeNull();
  });

  it('hides footer on complete', () => {
    const tree = renderProgress({progress: progress({phase: 'complete'})});
    expect(findByTestId(tree.root, 'scan-progress-footer')).toBeNull();
  });

  it('invokes onPause from pause control', () => {
    const onPause = jest.fn();
    const tree = renderProgress({onPause});
    ReactTestRenderer.act(() => {
      findByTestId(tree.root, 'scan-progress-pause')?.props.onPress();
    });
    expect(onPause).toHaveBeenCalledTimes(1);
  });

  it('invokes onCancel from cancel control', () => {
    const onCancel = jest.fn();
    const tree = renderProgress({onCancel});
    ReactTestRenderer.act(() => {
      findByTestId(tree.root, 'scan-progress-cancel')?.props.onPress();
    });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
