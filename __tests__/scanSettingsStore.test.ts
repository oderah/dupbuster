import AsyncStorage from '@react-native-async-storage/async-storage';

import {
  createAsyncStorageScanSettingsPort,
  createMemoryScanSettingsPort,
  DEFAULT_SCAN_SETTINGS,
  SCAN_SETTINGS_STORAGE_KEY,
} from '../src/settings/scanSettingsStore';

jest.mock('@react-native-async-storage/async-storage', () => ({
  __esModule: true,
  default: {
    getItem: jest.fn(),
    setItem: jest.fn(),
  },
}));

const mockGetItem = AsyncStorage.getItem as jest.Mock;
const mockSetItem = AsyncStorage.setItem as jest.Mock;

describe('scanSettingsStore', () => {
  beforeEach(() => {
    mockGetItem.mockReset();
    mockSetItem.mockReset();
  });

  it('createMemoryScanSettingsPort round-trips largeFilesOptIn', async () => {
    const port = createMemoryScanSettingsPort();
    expect(await port.load()).toEqual(DEFAULT_SCAN_SETTINGS);

    await port.save({largeFilesOptIn: true});
    expect(await port.load()).toEqual({largeFilesOptIn: true});
  });

  it('createAsyncStorageScanSettingsPort defaults when storage is empty', async () => {
    mockGetItem.mockResolvedValue(null);
    const port = createAsyncStorageScanSettingsPort();
    expect(await port.load()).toEqual(DEFAULT_SCAN_SETTINGS);
  });

  it('createAsyncStorageScanSettingsPort persists opt-in', async () => {
    mockGetItem.mockResolvedValue(JSON.stringify({largeFilesOptIn: true}));
    mockSetItem.mockResolvedValue(undefined);
    const port = createAsyncStorageScanSettingsPort();

    expect(await port.load()).toEqual({largeFilesOptIn: true});

    await port.save({largeFilesOptIn: false});
    expect(mockSetItem).toHaveBeenCalledWith(
      SCAN_SETTINGS_STORAGE_KEY,
      JSON.stringify({largeFilesOptIn: false}),
    );
  });
});
