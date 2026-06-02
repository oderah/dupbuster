import {getDeleteConfirmStepCopy} from '../src/components/deleteConfirmDisplay';
import {tokens} from '../src/tokens/tokens';

describe('getDeleteConfirmStepCopy', () => {
  it('returns review step copy with reclaimable size (AC-action-delete-01 step 1)', () => {
    const copy = getDeleteConfirmStepCopy('review', 3, 1_048_576);
    expect(copy.title).toBe(tokens.delete.review.title);
    expect(copy.body).toContain('3 files');
    expect(copy.body).toContain('1 MB');
    expect(copy.primaryLabel).toBe(tokens.delete.review.continue);
    expect(copy.secondaryLabel).toBe(tokens.delete.review.cancel);
    expect(copy.primaryIsDestructive).toBe(false);
  });

  it('returns confirm step copy without reclaimable line (step 2)', () => {
    const copy = getDeleteConfirmStepCopy('confirm', 2, 500);
    expect(copy.title).toBe(tokens.delete.confirm.title);
    expect(copy.body).toBe(
      'This cannot be undone. 2 files will be permanently deleted from your device.',
    );
    expect(copy.primaryLabel).toBe(tokens.delete.confirm.confirm);
    expect(copy.secondaryLabel).toBe(tokens.delete.confirm.back);
    expect(copy.primaryIsDestructive).toBe(true);
  });
});
