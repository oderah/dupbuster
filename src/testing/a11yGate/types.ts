import type {ReactTestInstance} from 'react-test-renderer';

/** Rule ids aligned with react-native-accessibility-engine (M2-10 equivalent). */
export type A11yRuleId =
  | 'link-role-required'
  | 'link-role-misused'
  | 'pressable-accessible-required'
  | 'pressable-role-required'
  | 'pressable-label-required'
  | 'adjustable-role-required'
  | 'adjustable-value-required'
  | 'checked-state-required'
  | 'disabled-state-required'
  | 'no-empty-text';

export type A11yViolation = {
  ruleId: A11yRuleId;
  pathToComponent: string;
  problem: string;
  solution: string;
};

export type A11yRule = {
  id: A11yRuleId;
  matcher: (node: ReactTestInstance) => boolean;
  assertion: (node: ReactTestInstance) => boolean;
  problem: string;
  solution: string;
};

export type RunA11yGateOptions = {
  /** Subset of rules; defaults to all critical gate rules. */
  rules?: A11yRuleId[];
};
