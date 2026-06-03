import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {PrivacyPolicyLink} from '../src/components/PrivacyPolicyLink';
import {PRIVACY_POLICY_URL} from '../src/config/privacyPolicyUrl';
import {tokens} from '../src/tokens/tokens';

describe('PrivacyPolicyLink', () => {
  it('shows frozen settings.privacyPolicy label', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<PrivacyPolicyLink />);
    });

    expect(JSON.stringify(tree!.toJSON())).toContain(
      tokens.settings.privacyPolicy,
    );
  });

  it('uses link accessibility role and hint', () => {
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<PrivacyPolicyLink />);
    });

    const link = tree!.root.findByProps({testID: 'privacy-policy-link'});
    expect(link.props.accessibilityRole).toBe('link');
    expect(link.props.accessibilityHint).toBe(
      tokens.a11y.settings.privacyPolicyHint,
    );
  });

  it('delegates press to custom handler when provided', () => {
    const onPress = jest.fn();
    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(
        <PrivacyPolicyLink onPress={onPress} testID="custom-privacy" />,
      );
    });

    const link = tree!.root.findByProps({testID: 'custom-privacy'});
    ReactTestRenderer.act(() => {
      link.props.onPress();
    });
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('default press targets canonical privacy policy URL', () => {
    const openURL = jest.fn().mockResolvedValue(undefined);
    jest.spyOn(require('react-native').Linking, 'openURL').mockImplementation(
      openURL,
    );

    let tree: ReactTestRenderer.ReactTestRenderer;
    ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<PrivacyPolicyLink />);
    });

    const link = tree!.root.findByProps({testID: 'privacy-policy-link'});
    ReactTestRenderer.act(() => {
      link.props.onPress();
    });
    expect(openURL).toHaveBeenCalledWith(PRIVACY_POLICY_URL);
  });
});
