import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {ContentMatchNotice} from '../../src/components/ContentMatchNotice';
import {DeleteConfirmModal} from '../../src/components/DeleteConfirmModal';
import {CoverageBanner} from '../../src/components/CoverageBanner';
import {KeeperSelector} from '../../src/components/KeeperSelector';
import {MatchKindBadge} from '../../src/components/MatchKindBadge';
import {RescanPromptBanner} from '../../src/components/RescanPromptBanner';
import {ScanProgress} from '../../src/components/ScanProgress';
import {createKeeperSelectionState} from '../../src/controllers/keeperSelection';
import {formatProgressAnnouncement} from '../../src/controllers/scanProgressA11y';
import type {KeeperMember} from '../../src/types/keeper';
import type {ScanProgressEvent} from '../../src/types/scanEngine';

const coverageBaseProps = {
  coverageSessionKey: 'session',
  firstDisplayAlertEligible: true,
  dismissedForSession: false,
  onDismiss: jest.fn(),
  onExpandCoverage: jest.fn(),
  onOpenSettings: jest.fn(),
};

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

function renderRoot(element: React.ReactElement): ReactTestRenderer.ReactTestInstance {
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(element);
  });
  return tree!.root;
}

function scanProgressSnapshot(
  overrides: Partial<ScanProgressEvent> = {},
): ScanProgressEvent {
  return {
    filesProcessed: 120,
    filesTotalKnown: 1000,
    groupsFound: 3,
    reclaimableBytesEst: 1_048_576,
    phase: 'hashing',
    ...overrides,
  };
}

/**
 * M2 exit gate — automated a11y (implementation-plan §4.2).
 * DeleteConfirmModal is M3; covered when that component lands.
 */
describe('M2 gate components — automated a11y (M2-10)', () => {
  describe('CoverageBanner', () => {
    it.each([
      ['partial', {variant: 'partial' as const}],
      [
        'limited-library',
        {
          variant: 'limited-library' as const,
          coverageSessionKey: 'limited-library:7',
          limitedLibraryCount: 7,
          firstDisplayAlertEligible: false,
        },
      ],
      ['denied', {variant: 'denied' as const, coverageSessionKey: 'denied'}],
    ])('%s variant has zero critical violations', (_label, props) => {
      const root = renderRoot(
        <CoverageBanner {...coverageBaseProps} {...props} />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('ScanProgress', () => {
    it('hashing phase has zero critical violations', () => {
      const progress = scanProgressSnapshot();
      const root = renderRoot(
        <ScanProgress
          progress={progress}
          accessibilityLabel={formatProgressAnnouncement(progress)}
          onPause={jest.fn()}
          onCancel={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });

    it('paused phase with resume control has zero critical violations', () => {
      const progress = scanProgressSnapshot({phase: 'paused'});
      const root = renderRoot(
        <ScanProgress
          progress={progress}
          accessibilityLabel={formatProgressAnnouncement(progress)}
          onResume={jest.fn()}
          onCancel={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });

    it('hashing with video_content subcopy has zero critical violations', () => {
      const progress = scanProgressSnapshot({
        phase: 'hashing',
        contentKind: 'video_content',
      });
      const root = renderRoot(
        <ScanProgress
          progress={progress}
          accessibilityLabel={formatProgressAnnouncement(progress)}
          onPause={jest.fn()}
          onCancel={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('KeeperSelector', () => {
    it('default selection state has zero critical violations', () => {
      const root = renderRoot(
        <KeeperSelector
          members={members}
          selection={createKeeperSelectionState(members)}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });

    it('remember-session checkbox state has zero critical violations', () => {
      const root = renderRoot(
        <KeeperSelector
          members={members}
          selection={createKeeperSelectionState(members)}
          onSelectMember={jest.fn()}
          onApplyPreset={jest.fn()}
          showRememberSession
          rememberSession={false}
          onRememberSessionChange={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('RescanPromptBanner', () => {
    it('first display has zero critical violations', () => {
      const root = renderRoot(
        <RescanPromptBanner
          rescanSessionKey="schema:2"
          firstDisplayAlertEligible
          dismissedForSession={false}
          onDismiss={jest.fn()}
          onRescan={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('ContentMatchNotice', () => {
    it('SAME_CONTENT_VIDEO variant has zero critical violations', () => {
      const root = renderRoot(
        <ContentMatchNotice matchKind="SAME_CONTENT_VIDEO" />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('MatchKindBadge', () => {
    it.each([
      ['EXACT_BYTES' as const],
      ['SAME_CONTENT_VIDEO' as const],
    ])('%s variant has zero critical violations', matchKind => {
      const root = renderRoot(<MatchKindBadge matchKind={matchKind} />);
      expect(root).toHaveZeroCriticalA11yViolations();
    });

    it('prominent SAME_CONTENT_VIDEO has zero critical violations', () => {
      const root = renderRoot(
        <MatchKindBadge matchKind="SAME_CONTENT_VIDEO" presentation="prominent" />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });

  describe('DeleteConfirmModal', () => {
    it('review step has zero critical violations (M3-01)', () => {
      const root = renderRoot(
        <DeleteConfirmModal
          visible
          step="review"
          deleteCount={2}
          reclaimableBytes={4096}
          onCancel={jest.fn()}
          onContinue={jest.fn()}
          onConfirm={jest.fn()}
          onGoBack={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });

    it('confirm step has zero critical violations (M3-01)', () => {
      const root = renderRoot(
        <DeleteConfirmModal
          visible
          step="confirm"
          deleteCount={2}
          reclaimableBytes={4096}
          onCancel={jest.fn()}
          onContinue={jest.fn()}
          onConfirm={jest.fn()}
          onGoBack={jest.fn()}
        />,
      );
      expect(root).toHaveZeroCriticalA11yViolations();
    });
  });
});
