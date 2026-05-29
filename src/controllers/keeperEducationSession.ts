/** AC-action-keeper-02 — education sheet once per app session (not persisted). */
export type KeeperEducationSessionState = {
  educationShown: boolean;
};

export function createKeeperEducationSessionState(): KeeperEducationSessionState {
  return {educationShown: false};
}

export function shouldShowKeeperEducation(
  session: KeeperEducationSessionState,
): boolean {
  return !session.educationShown;
}

export function markKeeperEducationShown(
  _session: KeeperEducationSessionState,
): KeeperEducationSessionState {
  return {educationShown: true};
}
