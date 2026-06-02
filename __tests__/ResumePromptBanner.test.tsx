import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {ResumePromptBanner} from '../src/components/ResumePromptBanner';
import {tokens} from '../src/tokens/tokens';

const baseProps = {
  resumeSessionKey: 'run:3',
  firstDisplayAlertEligible: true,
  dismissedForSession: false,
  onDismiss: jest.fn(),
  onResume: jest.fn(),
  onRestart: jest.fn(),
};

function renderBanner(
  overrides: Partial<React.ComponentProps<typeof ResumePromptBanner>> = {},
) {
  let tree: ReactTestRenderer.ReactTestRenderer;
  ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(
      <ResumePromptBanner {...baseProps} {...overrides} />,
    );
  });
  return tree!;
}

describe('ResumePromptBanner', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when dismissed for session', () => {
    const tree = renderBanner({dismissedForSession: true});
    expect(tree.toJSON()).toBeNull();
  });

  it('shows frozen interrupted copy and resume/restart CTAs', () => {
    const tree = renderBanner();
    const json = JSON.stringify(tree.toJSON());
    expect(json).toContain(tokens.resume.interrupted);
    expect(json).toContain(tokens.resume.cta.resume);
    expect(json).toContain(tokens.resume.cta.restart);
  });

  it('invokes onResume when resume CTA pressed', () => {
    const tree = renderBanner();
    const resume = tree.root.findByProps({testID: 'resume-prompt-banner-resume'});
    ReactTestRenderer.act(() => {
      resume.props.onPress();
    });
    expect(baseProps.onResume).toHaveBeenCalledTimes(1);
  });
});
