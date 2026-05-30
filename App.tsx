/**
 * DupBuster scan shell — wires ScanSessionController to the M2 UI catalog.
 */
import React, {useMemo} from 'react';
import {StatusBar, StyleSheet, useColorScheme} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';

import {useScanSessionController} from './src/hooks/useScanSessionController';
import {createMockScanEnginePort} from './src/native/scanEnginePort';
import {ScanSessionScreen} from './src/screens/ScanSessionScreen';

function AppContent(): React.JSX.Element {
  const engine = useMemo(() => createMockScanEnginePort(), []);
  const sessionOptions = useMemo(
    () => ({coverageVariant: 'partial' as const}),
    [],
  );
  const {state, controller} = useScanSessionController(engine, sessionOptions);

  return (
    <ScanSessionScreen
      state={state}
      controller={controller}
      showStartScanControl
      onStartScan={() => {
        controller
          .startScan({
            mode: 'platform_discovery',
            roots: [],
          })
          .catch(() => {});
      }}
    />
  );
}

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <SafeAreaProvider>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <SafeAreaView style={styles.container}>
        <AppContent />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});

export default App;
