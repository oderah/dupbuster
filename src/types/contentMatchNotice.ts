import type {MatchKind} from './scanEngine';

export type ContentMatchNoticeProps = {
  matchKind: MatchKind;
  testID?: string;
};
