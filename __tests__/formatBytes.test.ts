import {formatBytes} from '../src/utils/formatBytes';

describe('formatBytes', () => {
  it('formats zero and small values', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(512)).toBe('512 B');
  });

  it('formats KB and MB', () => {
    expect(formatBytes(1024)).toBe('1 KB');
    expect(formatBytes(1_048_576)).toBe('1 MB');
  });
});
