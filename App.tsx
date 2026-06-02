/**
 * DupBuster scan shell — wires ScanSessionController + permission flows (M2-08).
 */
import React, {useMemo} from 'react';
import {StatusBar, StyleSheet, useColorScheme} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';

import {useReducedMotion} from './src/hooks/useReducedMotion';
import {useScanPermissionFlow} from './src/hooks/useScanPermissionFlow';
import {useScanSessionController} from './src/hooks/useScanSessionController';
import {useScanSettings} from './src/hooks/useScanSettings';
import {createNativeScanPermissionPort} from './src/permissions/nativeScanPermissionPort';
import {createNativeScanEnginePort} from './src/native/scanEnginePort';
import {createAsyncStorageScanSettingsPort} from './src/settings/scanSettingsStore';
import NativeScanEngine from './src/native/NativeScanEngine';
import {ScanSessionScreen} from './src/screens/ScanSessionScreen';

function AppContent(): React.JSX.Element {
  const engine = useMemo(() => createNativeScanEnginePort(NativeScanEngine), []);
  const permissionPort = useMemo(() => createNativeScanPermissionPort(), []);
  const settingsPort = useMemo(() => createAsyncStorageScanSettingsPort(), []);
  const {state, controller} = useScanSessionController(engine);
  const {settings, setLargeFilesOptIn} = useScanSettings(settingsPort);
  const {
    handleStartScan,
    handleResumeInterruptedScan,
    handleRestartInterruptedScan,
    handleExpandCoverage,
    handleOpenSettings,
    handleEnableLargeFiles,
  } = useScanPermissionFlow(
    controller,
    permissionPort,
    () => settings,
    setLargeFilesOptIn,
  );
  const reducedMotion = useReducedMotion();

  return (
    <ScanSessionScreen
      state={state}
      controller={controller}
      reducedMotion={reducedMotion}
      onStartScan={() => {
        handleStartScan().catch(() => {});
      }}
      onResumeInterruptedScan={scanRunId => {
        handleResumeInterruptedScan(scanRunId).catch(() => {});
      }}
      onRestartInterruptedScan={scanRunId => {
        handleRestartInterruptedScan(scanRunId).catch(() => {});
      }}
      onExpandCoverage={() => {
        handleExpandCoverage().catch(() => {});
      }}
      onOpenSettings={() => {
        handleOpenSettings().catch(() => {});
      }}
      largeFilesOptIn={settings.largeFilesOptIn}
      onLargeFilesOptInChange={value => {
        setLargeFilesOptIn(value).catch(() => {});
      }}
      onEnableLargeFiles={() => {
        handleEnableLargeFiles().catch(() => {});
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
