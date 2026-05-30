import {
  createRescanPromptSessionState,
  dismissRescanPromptForSession,
  resolveRescanPromptPresentation,
} from '../src/controllers/rescanPromptSessionState';

describe('rescanPromptSessionState', () => {
  it('returns null when full rescan is not required', () => {
    expect(
      resolveRescanPromptPresentation({
        schemaVersion: 2,
        fullRescanRequired: false,
      }),
    ).toBeNull();
  });

  it('returns presentation keyed by schema version when rescan required', () => {
    expect(
      resolveRescanPromptPresentation({
        schemaVersion: 2,
        fullRescanRequired: true,
      }),
    ).toEqual({
      rescanSessionKey: 'schema:2',
      firstDisplayAlertEligible: true,
      dismissedForSession: false,
    });
  });

  it('resets first-display alert when schema version changes', () => {
    const session = createRescanPromptSessionState();
    session.lastSeenSessionKey = 'schema:1';

    expect(
      resolveRescanPromptPresentation(
        {schemaVersion: 2, fullRescanRequired: true},
        session,
      )?.firstDisplayAlertEligible,
    ).toBe(true);
  });

  it('hides presentation after session dismiss', () => {
    const session = dismissRescanPromptForSession(createRescanPromptSessionState());
    expect(
      resolveRescanPromptPresentation(
        {schemaVersion: 2, fullRescanRequired: true},
        session,
      ),
    ).toEqual({
      rescanSessionKey: 'schema:2',
      firstDisplayAlertEligible: false,
      dismissedForSession: true,
    });
  });
});
