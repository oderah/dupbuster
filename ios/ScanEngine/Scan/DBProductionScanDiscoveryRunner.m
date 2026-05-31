#import "DBProductionScanDiscoveryRunner.h"

@implementation DBProductionScanDiscoveryRunner {
  DBDiscoveryEmitter *_discoveryEmitter;
}

- (instancetype)initWithDiscoveryEmitter:(DBDiscoveryEmitter *)discoveryEmitter
{
  self = [super init];
  if (self) {
    _discoveryEmitter = discoveryEmitter;
  }
  return self;
}

- (instancetype)init
{
  return [self initWithDiscoveryEmitter:[[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]]];
}

- (DBDiscoveryResult *)discoverWithRequest:(DBScanStartRequest *)request
                                      plan:(DBScanRootResolverPlan *)plan
                                generation:(NSInteger)generation
                                   handler:(DBScanDiscoveryEntryHandler)handler
                               isCancelled:(DBScanDiscoveryCancelBlock)isCancelled
{
  switch (request.mode) {
    case DBScanRootModePlatformDiscovery:
      return [self discoverPlatformWithPlan:plan generation:generation handler:handler isCancelled:isCancelled];
    case DBScanRootModeUserSelected:
      return [self discoverUserSelectedWithPlan:plan generation:generation handler:handler isCancelled:isCancelled];
  }
}

- (DBDiscoveryResult *)discoverPlatformWithPlan:(DBScanRootResolverPlan *)plan
                                     generation:(NSInteger)generation
                                        handler:(DBScanDiscoveryEntryHandler)handler
                                    isCancelled:(DBScanDiscoveryCancelBlock)isCancelled
{
  if (plan.platformGrant == nil) {
    @throw [NSException exceptionWithName:NSInternalInconsistencyException
                                   reason:@"platform_discovery plan missing platform grant"
                                 userInfo:nil];
  }

  DBResolvedScanRoot *platformRoot = nil;
  for (DBResolvedScanRoot *root in plan.roots) {
    if (root.mode == DBScanRootModePlatformDiscovery) {
      platformRoot = root;
      break;
    }
  }
  if (platformRoot == nil) {
    @throw [NSException exceptionWithName:NSInternalInconsistencyException
                                   reason:@"platform_discovery plan missing platform root"
                                 userInfo:nil];
  }

  NSMutableArray<NSURL *> *folderURLs = [NSMutableArray array];
  for (DBScanRootGrant *grant in plan.additionalScopedGrants) {
    NSURL *url = [NSURL URLWithString:grant.uriGrant];
    if (url != nil) {
      [folderURLs addObject:url];
    }
  }

  return [_discoveryEmitter emitModeBWithScanRootGrant:plan.platformGrant
                                            scanRootId:platformRoot.scanRootId
                                            generation:generation
                           authorizedLocalIdentifiers:nil
                         additionalScopedFolderURLs:folderURLs.count > 0 ? folderURLs : nil
                         additionalScopedGrants:plan.additionalScopedGrants.count > 0
                                                    ? plan.additionalScopedGrants
                                                    : nil
                                               handler:handler
                                           isCancelled:isCancelled];
}

- (DBDiscoveryResult *)discoverUserSelectedWithPlan:(DBScanRootResolverPlan *)plan
                                         generation:(NSInteger)generation
                                            handler:(DBScanDiscoveryEntryHandler)handler
                                        isCancelled:(DBScanDiscoveryCancelBlock)isCancelled
{
  DBDiscoveryResult *aggregate = [[DBDiscoveryResult alloc] init];
  for (DBResolvedScanRoot *root in plan.roots) {
    if (isCancelled != nil && isCancelled()) {
      aggregate.cancelled = YES;
      break;
    }
    NSURL *folderURL = [NSURL URLWithString:root.uriGrant];
    if (folderURL == nil) {
      continue;
    }
    DBScanRootGrant *grant = [[DBScanRootGrant alloc] initWithUriGrant:root.uriGrant
                                                                  mode:DBScanRootModeUserSelected];
    DBDiscoveryResult *result =
        [_discoveryEmitter emitModeAWithFolderURL:folderURL
                                  scanRootGrant:grant
                                     scanRootId:root.scanRootId
                                     generation:generation
                                        handler:handler
                                      isCancelled:isCancelled];
    aggregate.entriesEmitted += result.entriesEmitted;
    aggregate.entriesDenied += result.entriesDenied;
    aggregate.directoriesVisited += result.directoriesVisited;
    aggregate.cancelled = aggregate.cancelled || result.cancelled;
    if (aggregate.cancelled) {
      break;
    }
  }
  return aggregate;
}

@end
