import {useEffect, useRef} from 'react';
import {
  AccessibilityInfo,
  findNodeHandle,
  type View,
} from 'react-native';

type FocusableRef = React.RefObject<View | null>;

export type UseModalFocusTrapOptions = {
  visible: boolean;
  /** Re-run heading focus when step changes inside the modal. */
  focusKey?: string | number;
  headingRef: FocusableRef;
  returnFocusRef?: FocusableRef | null;
};

const isJestRuntime = typeof jest !== 'undefined';

function focusViewRef(ref: View | null): void {
  if (ref == null || isJestRuntime) {
    return;
  }
  const node = findNodeHandle(ref);
  if (node != null) {
    AccessibilityInfo.setAccessibilityFocus(node);
  }
}

function scheduleFocus(action: () => void): () => void {
  if (isJestRuntime) {
    return () => {};
  }
  let cancelled = false;
  const timer = setTimeout(() => {
    if (!cancelled) {
      action();
    }
  }, 50);
  return () => {
    cancelled = true;
    clearTimeout(timer);
  };
}

/**
 * A11Y-07 — move screen reader focus to modal heading on open; restore on dismiss.
 * Pair with `accessibilityViewIsModal` on the enclosing Modal for trap behavior.
 */
export function useModalFocusTrap({
  visible,
  focusKey,
  headingRef,
  returnFocusRef,
}: UseModalFocusTrapOptions): void {
  const wasVisibleRef = useRef(false);

  useEffect(() => {
    if (!visible) {
      if (wasVisibleRef.current) {
        const cancel = scheduleFocus(() => {
          focusViewRef(returnFocusRef?.current ?? null);
        });
        wasVisibleRef.current = false;
        return cancel;
      }
      wasVisibleRef.current = false;
      return;
    }

    wasVisibleRef.current = true;
    return scheduleFocus(() => {
      focusViewRef(headingRef.current);
    });
  }, [visible, focusKey, headingRef, returnFocusRef]);
}
