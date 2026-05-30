import type {A11yRule} from './types';
import {
  canBeDisabled,
  getTextNode,
  isAdjustable,
  isCheckbox,
  isPressableType,
  isTextType,
} from './helpers';

const PRESSABLE_ROLES = [
  'button',
  'link',
  'imagebutton',
  'radio',
  'tab',
  'checkbox',
] as const;

export const A11Y_GATE_RULES: A11yRule[] = [
  {
    id: 'pressable-role-required',
    matcher: node => isPressableType(node.type),
    assertion: node =>
      PRESSABLE_ROLES.includes(node.props.accessibilityRole),
    problem:
      "This component is pressable but the user hasn't been informed that it behaves like a button, link, or radio",
    solution: `Set the 'accessibilityRole' prop to one of: ${PRESSABLE_ROLES.join(', ')}`,
  },
  {
    id: 'pressable-accessible-required',
    matcher: node => isPressableType(node.type),
    assertion: node => node.props.accessible !== false,
    problem: 'This button is not accessible (selectable) to the user',
    solution:
      "Set the 'accessible' prop to 'true' or remove it (pressables are accessible by default)",
  },
  {
    id: 'pressable-label-required',
    matcher: node => isPressableType(node.type),
    assertion: node => {
      const textNode = getTextNode(node);
      const textContent = textNode?.props.children;
      const accessibilityLabel = node.props.accessibilityLabel;
      return Boolean(accessibilityLabel || textContent);
    },
    problem:
      "This pressable has no text content, so an accessibility label can't be automatically inferred",
    solution:
      "Place a text component in the button or define an 'accessibilityLabel' prop",
  },
  {
    id: 'link-role-required',
    matcher: node => isTextType(node.type),
    assertion: node => {
      const {onPress, accessibilityRole} = node.props;
      if (onPress) {
        return accessibilityRole === 'link';
      }
      return true;
    },
    problem:
      "The text is clickable, but the user wasn't informed that it behaves like a link",
    solution:
      "Set the 'accessibilityRole' prop to 'link' or remove the 'onPress' prop",
  },
  {
    id: 'link-role-misused',
    matcher: node => isTextType(node.type),
    assertion: node => {
      const {onPress, accessibilityRole} = node.props;
      if (!onPress) {
        return accessibilityRole !== 'link';
      }
      return true;
    },
    problem: "The 'link' role has been used but the text isn't clickable",
    solution:
      "Set the 'accessibilityRole' prop to 'text' or add an 'onPress' prop",
  },
  {
    id: 'checked-state-required',
    matcher: node => isCheckbox(node),
    assertion: node => {
      const checked = node.props.accessibilityState?.checked;
      return checked === true || checked === false || checked === 'mixed';
    },
    problem:
      "This component has an accessibility role of 'checkbox' but doesn't have a checked state",
    solution:
      "Set the 'accessibilityState' prop to an object like { checked: true | false | 'mixed' }",
  },
  {
    id: 'disabled-state-required',
    matcher: node => canBeDisabled(node),
    assertion: node =>
      node.props.accessibilityState?.disabled !== undefined,
    problem:
      "This component has a disabled state but it isn't exposed to the user",
    solution:
      "Set the 'accessibilityState' prop to an object containing a boolean 'disabled' key",
  },
  {
    id: 'adjustable-role-required',
    matcher: node => isAdjustable(node),
    assertion: node => node.props.accessibilityRole === 'adjustable',
    problem:
      "This component has an adjustable value but the user wasn't informed of this",
    solution: "Set the 'accessibilityRole' prop to 'adjustable'",
  },
  {
    id: 'adjustable-value-required',
    matcher: node => isAdjustable(node),
    assertion: node => {
      const value = node.props.accessibilityValue;
      return (
        value?.now !== undefined &&
        value?.min !== undefined &&
        value?.max !== undefined
      );
    },
    problem:
      "This component has an adjustable value but the user wasn't informed of its min, max, and current value",
    solution:
      "Set the 'accessibilityValue' prop to an object: { min: ?, max: ?, now: ? }",
  },
  {
    id: 'no-empty-text',
    matcher: node => isTextType(node.type),
    assertion: node => Boolean(node.props.children),
    problem:
      "This text node doesn't contain text and so no accessibility label can be inferred",
    solution:
      'Add text content or prevent this component from rendering if it has no content',
  },
];

export const CRITICAL_A11Y_RULE_IDS = A11Y_GATE_RULES.map(rule => rule.id);
