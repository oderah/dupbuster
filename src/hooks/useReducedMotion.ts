import {useEffect, useState} from 'react';
import {AccessibilityInfo} from 'react-native';

/** Mirrors system reduce-motion / remove-animations (A11Y-09). */
export function useReducedMotion(): boolean {
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    let active = true;

    AccessibilityInfo.isReduceMotionEnabled().then(enabled => {
      if (active) {
        setReducedMotion(enabled);
      }
    });

    const subscription = AccessibilityInfo.addEventListener(
      'reduceMotionChanged',
      enabled => {
        setReducedMotion(enabled);
      },
    );

    return () => {
      active = false;
      subscription.remove();
    };
  }, []);

  return reducedMotion;
}
