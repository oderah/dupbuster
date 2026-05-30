import type {MatchKind} from './scanEngine';

export type MatchKindBadgePresentation = 'compact' | 'prominent';

export type MatchKindBadgeProps = {
  matchKind: MatchKind;
  /** compact = list row chip; prominent = group detail header */
  presentation?: MatchKindBadgePresentation;
  testID?: string;
};
