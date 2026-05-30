import type {ReactTestInstance} from 'react-test-renderer';

import {getPathToComponent, isHidden} from './helpers';
import {A11Y_GATE_RULES, CRITICAL_A11Y_RULE_IDS} from './rules';
import type {A11yRuleId, A11yViolation, RunA11yGateOptions} from './types';

export function runA11yGate(
  root: ReactTestInstance,
  options: RunA11yGateOptions = {},
): A11yViolation[] {
  const ruleIds = options.rules ?? CRITICAL_A11Y_RULE_IDS;
  const rules = A11Y_GATE_RULES.filter(rule => ruleIds.includes(rule.id));
  const violations: A11yViolation[] = [];

  for (const rule of rules) {
    const matchedComponents = root.findAll(rule.matcher, {deep: true});
    if (rule.matcher(root)) {
      matchedComponents.push(root);
    }

    for (const component of matchedComponents) {
      if (isHidden(component)) {
        continue;
      }
      if (!rule.assertion(component)) {
        violations.push({
          ruleId: rule.id,
          pathToComponent: getPathToComponent(component),
          problem: rule.problem,
          solution: rule.solution,
        });
      }
    }
  }

  return violations;
}

export function formatA11yViolations(violations: A11yViolation[]): string {
  if (violations.length === 0) {
    return 'No accessibility violations found.';
  }

  return violations
    .map(
      (violation, index) =>
        `${index + 1}. [${violation.ruleId}] ${violation.pathToComponent}\n` +
        `   Problem: ${violation.problem}\n` +
        `   Solution: ${violation.solution}`,
    )
    .join('\n\n');
}

export type {A11yRuleId, A11yViolation};
