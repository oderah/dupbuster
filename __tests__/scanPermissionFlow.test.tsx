import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

import {createScanSessionController} from '../src/controllers/scanSessionController';
import {useScanPermissionFlow} from '../src/hooks/useScanPermissionFlow';
import {createMockScanEnginePort} from '../src/native/scanEnginePort';
import {buildScanStartRequest} from '../src/permissions/buildScanStartRequest';
import {resolveCoverageFromPermission} from '../src/permissions/resolveCoverageFromPermission';
import {createMockScanPermissionPort} from '../src/permissions/scanPermissionPort';

describe('resolveCoverageFromPermission', () => {
  it('maps blocked platform discovery to denied', () => {
    expect(
      resolveCoverageFromPermission({
        platform: 'android',
        platformDiscovery: 'blocked',
        additionalFolderGrants: [],
      }),
    ).toEqual({variant: 'denied'});
  });

  it('maps iOS limited library with count', () => {
    expect(
      resolveCoverageFromPermission({
        platform: 'ios',
        platformDiscovery: 'limited',
        limitedLibraryCount: 12,
        additionalFolderGrants: [],
      }),
    ).toEqual({variant: 'limited-library', limitedLibraryCount: 12});
  });

  it('maps Android granted media access to partial (US-06)', () => {
    expect(
      resolveCoverageFromPermission({
        platform: 'android',
        platformDiscovery: 'granted',
        additionalFolderGrants: [],
      }),
    ).toEqual({variant: 'partial'});
  });

  it('hides banner for iOS full photo library access', () => {
    expect(
      resolveCoverageFromPermission({
        platform: 'ios',
        platformDiscovery: 'granted',
        additionalFolderGrants: [],
      }),
    ).toEqual({variant: null});
  });

  it('allows SAF-only scan with partial banner when media denied', () => {
    expect(
      resolveCoverageFromPermission({
        platform: 'android',
        platformDiscovery: 'denied',
        additionalFolderGrants: [{uriGrant: 'content://tree/grant'}],
      }),
    ).toEqual({variant: 'partial'});
  });
});

describe('buildScanStartRequest', () => {
  it('starts platform discovery when media access is granted', () => {
    expect(
      buildScanStartRequest({
        platform: 'android',
        platformDiscovery: 'granted',
        additionalFolderGrants: [{uriGrant: 'content://tree/extra'}],
      }),
    ).toEqual({
      mode: 'platform_discovery',
      roots: [{uriGrant: 'content://tree/extra'}],
    });
  });

  it('starts user-selected mode from folder grants only', () => {
    expect(
      buildScanStartRequest({
        platform: 'android',
        platformDiscovery: 'denied',
        additionalFolderGrants: [{uriGrant: 'content://tree/grant'}],
      }),
    ).toEqual({
      mode: 'user_selected',
      roots: [{uriGrant: 'content://tree/grant'}],
    });
  });

  it('returns null when scan cannot start', () => {
    expect(
      buildScanStartRequest({
        platform: 'android',
        platformDiscovery: 'blocked',
        additionalFolderGrants: [],
      }),
    ).toBeNull();
  });
});

describe('createMockScanPermissionPort', () => {
  it('requests platform discovery from denied state', async () => {
    const port = createMockScanPermissionPort({platformDiscovery: 'denied'});
    const next = await port.requestPlatformDiscoveryAccess();
    expect(next.platformDiscovery).toBe('granted');
  });

  it('accumulates folder grants from pickAdditionalFolder', async () => {
    const port = createMockScanPermissionPort();
    const next = await port.pickAdditionalFolder();
    expect(next.additionalFolderGrants).toHaveLength(1);
  });
});

describe('useScanPermissionFlow', () => {
  it('applies denied coverage from snapshot on refresh', async () => {
    const port = createMockScanPermissionPort({platformDiscovery: 'blocked'});
    const controller = createScanSessionController(createMockScanEnginePort());
    const handlers = mountPermissionFlow(controller, port);

    await handlers.refreshCoverage();
    expect(controller.getState().coverageVariant).toBe('denied');
    controller.dispose();
  });

  it('starts scan after permission request succeeds', async () => {
    const port = createMockScanPermissionPort({platformDiscovery: 'denied'});
    const controller = createScanSessionController(createMockScanEnginePort());
    const handlers = mountPermissionFlow(controller, port);

    await handlers.handleStartScan();
    expect(controller.getState().scanRunId).not.toBeNull();
    controller.dispose();
  });
});

function mountPermissionFlow(
  controller: ReturnType<typeof createScanSessionController>,
  port: ReturnType<typeof createMockScanPermissionPort>,
) {
  let handlers: ReturnType<typeof useScanPermissionFlow> | null = null;

  function Probe(): React.JSX.Element {
    handlers = useScanPermissionFlow(controller, port);
    return <></>;
  }

  ReactTestRenderer.act(() => {
    ReactTestRenderer.create(<Probe />);
  });

  if (handlers == null) {
    throw new Error('permission flow hook did not mount');
  }
  return handlers;
}
