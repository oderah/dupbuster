import {Pressable, Text} from 'react-native';
import type {ReactTestInstance} from 'react-test-renderer';

function getComponentTypeName(type: unknown): string | null {
  if (typeof type === 'string') {
    return type;
  }
  if (typeof type === 'function' || typeof type === 'object') {
    return (
      (type as {displayName?: string; name?: string}).displayName ??
      (type as {name?: string}).name ??
      null
    );
  }
  return null;
}

const PRESSABLE_TYPE_NAMES = new Set([
  'Pressable',
  'TouchableHighlight',
  'TouchableOpacity',
  'TouchableNativeFeedback',
  'TouchableWithoutFeedback',
]);

export function isPressableType(type: unknown): boolean {
  if (type === Pressable) {
    return true;
  }
  const name = getComponentTypeName(type);
  return name != null && PRESSABLE_TYPE_NAMES.has(name);
}

export function isTextType(type: unknown): boolean {
  if (type === Text) {
    return true;
  }
  return getComponentTypeName(type) === 'Text';
}

export function isHidden(node: ReactTestInstance): boolean {
  return (
    node.props.accessibilityElementsHidden === true ||
    node.props.importantForAccessibility === 'no-hide-descendants'
  );
}

export function isCheckbox(node: ReactTestInstance): boolean {
  return (
    isPressableType(node.type) && node.props.accessibilityRole === 'checkbox'
  );
}

export function isAdjustable(node: ReactTestInstance): boolean {
  const slidersInTree = node.findAll(
    child => String(child.type).includes('Slider'),
    {deep: true},
  );
  return (
    String(node.type).includes('Slider') && slidersInTree.length === 1
  );
}

export function canBeDisabled(node: ReactTestInstance): boolean {
  const inTree = node.findAll(
    child =>
      child.props.disabled !== undefined ||
      child.props.enabled !== undefined,
    {deep: true},
  );
  return (
    (node.props.disabled !== undefined || node.props.enabled !== undefined) &&
    inTree.length === 1
  );
}

export function getTextNode(
  node: ReactTestInstance,
): ReactTestInstance | null {
  try {
    return node.find(
      child => isTextType(child.type),
      {deep: true},
    );
  } catch {
    return null;
  }
}

export function getPathToComponent(node: ReactTestInstance): string {
  const segments: string[] = [];
  let current: ReactTestInstance | null = node;

  while (current != null) {
    const typeName =
      typeof current.type === 'string'
        ? current.type
        : (current.type as {displayName?: string; name?: string}).displayName ??
          (current.type as {name?: string}).name ??
          'Component';
    segments.unshift(typeName);
    current = current.parent;
  }

  return segments.join(' > ');
}
