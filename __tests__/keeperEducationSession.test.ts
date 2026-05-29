import {
  createKeeperEducationSessionState,
  markKeeperEducationShown,
  shouldShowKeeperEducation,
} from '../src/controllers/keeperEducationSession';

describe('keeperEducationSession', () => {
  it('shows education only once per session (AC-action-keeper-02)', () => {
    const initial = createKeeperEducationSessionState();
    expect(shouldShowKeeperEducation(initial)).toBe(true);

    const after = markKeeperEducationShown(initial);
    expect(shouldShowKeeperEducation(after)).toBe(false);
  });
});
