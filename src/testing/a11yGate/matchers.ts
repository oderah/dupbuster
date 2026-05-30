import type {ReactTestInstance} from 'react-test-renderer';

import {formatA11yViolations, runA11yGate} from './engine';
import type {RunA11yGateOptions} from './types';

declare global {
  namespace jest {
    interface Matchers<R> {
      toHaveZeroCriticalA11yViolations(
        options?: RunA11yGateOptions,
      ): R;
    }
  }
}

export function registerA11yGateMatchers(): void {
  expect.extend({
    toHaveZeroCriticalA11yViolations(
      received: ReactTestInstance,
      options?: RunA11yGateOptions,
    ) {
      const violations = runA11yGate(received, options);
      const pass = violations.length === 0;

      return {
        pass,
        message: () =>
          pass
            ? 'Expected accessibility violations, but found none.'
            : `Expected zero critical accessibility violations, but found ${violations.length}:\n\n${formatA11yViolations(violations)}`,
      };
    },
  });
}
