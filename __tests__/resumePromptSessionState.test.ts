import {
  buildResumeSessionKey,
  resolveResumePromptPresentation,
} from '../src/controllers/resumePromptSessionState';

describe('resumePromptSessionState', () => {
  it('buildResumeSessionKey encodes scan run id', () => {
    expect(buildResumeSessionKey(42)).toBe('run:42');
  });

  it('resolveResumePromptPresentation returns null when no resumable run', () => {
    expect(
      resolveResumePromptPresentation(null, {schemaVersion: 2, fullRescanRequired: false}, 'idle'),
    ).toBeNull();
  });

  it('resolveResumePromptPresentation returns null when phase is not idle', () => {
    expect(
      resolveResumePromptPresentation(
        {scanRunId: 1, lastProcessedId: 10, status: 'running'},
        {schemaVersion: 2, fullRescanRequired: false},
        'hashing',
      ),
    ).toBeNull();
  });

  it('resolveResumePromptPresentation returns null when full rescan required', () => {
    expect(
      resolveResumePromptPresentation(
        {scanRunId: 1, lastProcessedId: 10, status: 'paused'},
        {schemaVersion: 2, fullRescanRequired: true},
        'idle',
      ),
    ).toBeNull();
  });

  it('resolveResumePromptPresentation exposes resume CTA when interrupted run exists', () => {
    const presentation = resolveResumePromptPresentation(
      {scanRunId: 7, lastProcessedId: 500, status: 'running'},
      {schemaVersion: 2, fullRescanRequired: false},
      'idle',
    );
    expect(presentation).toEqual(
      expect.objectContaining({
        resumeSessionKey: 'run:7',
        scanRunId: 7,
        firstDisplayAlertEligible: true,
        dismissedForSession: false,
      }),
    );
  });
});
