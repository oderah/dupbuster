const {registerA11yGateMatchers} = require('./src/testing/a11yGate/matchers');

registerA11yGateMatchers();

jest.mock('react-native-permissions', () =>
  require('react-native-permissions/mock'),
);

jest.mock('@react-native-documents/picker', () => ({
  pickDirectory: jest.fn(async () => ({uri: 'content://mock/tree/document'})),
  types: {},
}));
