/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

jest.mock('../src/native/NativeScanEngine', () => ({
  __esModule: true,
  default: {},
}));

jest.mock('../src/native/scanEnginePort', () => {
  const actual = jest.requireActual('../src/native/scanEnginePort');
  return {
    ...actual,
    createNativeScanEnginePort: jest.fn(() =>
      actual.createMockScanEnginePort({simulateScan: false}),
    ),
  };
});

import App from '../App';

test('renders correctly', async () => {
  await ReactTestRenderer.act(async () => {
    ReactTestRenderer.create(<App />);
    await Promise.resolve();
  });
});
