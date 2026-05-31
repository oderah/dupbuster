/**
 * DupBuster scan shell — wires ScanSessionController + permission flows (M2-08).
 */
import React, {useMemo} from 'react';
import {StatusBar, StyleSheet, useColorScheme} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';

import {useReducedMotion} from './src/hooks/useReducedMotion';
import {useScanPermissionFlow} from './src/hooks/useScanPermissionFlow';
import {useScanSessionController} from './src/hooks/useScanSessionController';
import {createNativeScanPermissionPort} from './src/permissions/nativeScanPermissionPort';
import {createNativeScanEnginePort} from './src/native/scanEnginePort';
import NativeScanEngine from './src/native/NativeScanEngine';
import {ScanSessionScreen} from './src/screens/ScanSessionScreen';

function AppContent(): React.JSX.Element {
  const engine = useMemo(() => createNativeScanEnginePort(NativeScanEngine), []);
  const permissionPort = useMemo(() => createNativeScanPermissionPort(), []);
  const {state, controller} = useScanSessionController(engine);
  const {handleStartScan, handleExpandCoverage, handleOpenSettings} =
    useScanPermissionFlow(controller, permissionPort);
  const reducedMotion = useReducedMotion();

  return (
    <ScanSessionScreen
      state={state}
      controller={controller}
      reducedMotion={reducedMotion}
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
