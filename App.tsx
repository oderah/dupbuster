/**
 * DupBuster scan shell — wires ScanSessionController + permission flows (M2-08).
 */
import React, {useMemo} from 'react';
import {StatusBar, StyleSheet, useColorScheme} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';

import {useScanPermissionFlow} from './src/hooks/useScanPermissionFlow';
import {useScanSessionController} from './src/hooks/useScanSessionController';
import {createNativeScanPermissionPort} from './src/permissions/nativeScanPermissionPort';
import {createMockScanEnginePort} from './src/native/scanEnginePort';
import {ScanSessionScreen} from './src/screens/ScanSessionScreen';

function AppContent(): React.JSX.Element {
  const engine = useMemo(() => createMockScanEnginePort(), []);
  const permissionPort = useMemo(() => createNativeScanPermissionPort(), []);
  const {state, controller} = useScanSessionController(engine);
  const {handleStartScan, handleExpandCoverage, handleOpenSettings} =
    useScanPermissionFlow(controller, permissionPort);

  return (
    <ScanSessionScreen
      state={state}
      controller={controller}
      onStartScan={() => {
        handleStartScan().catch(() => {});
      }}
      onExpandCoverage={() => {
        handleExpandCoverage().catch(() => {});
      }}
      onOpenSettings={() => {
        handleOpenSettings().catch(() => {});
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
