#import "DBScanRootResolver.h"

NSString *const DBPlatformDiscoveryMarkerUri = @"*";

@implementation DBScanRootResolver

+ (DBScanRootResolverPlan *)resolveRequest:(DBScanStartRequest *)request
                               indexWriter:(DBIndexWriter *)indexWriter
{
  switch (request.mode) {
    case DBScanRootModePlatformDiscovery:
      return [self resolvePlatformDiscovery:request indexWriter:indexWriter];
    case DBScanRootModeUserSelected:
      return [self resolveUserSelected:request indexWriter:indexWriter];
  }
}

+ (DBScanRootResolverPlan *)resolvePlatformDiscovery:(DBScanStartRequest *)request
                                         indexWriter:(DBIndexWriter *)indexWriter
{
  NSInteger platformRootId =
      [indexWriter findOrInsertScanRootWithUriOrGrant:DBPlatformDiscoveryMarkerUri
                                                   mode:DBScanRootModeBridgePlatformDiscovery];

  NSMutableArray<DBResolvedScanRoot *> *resolvedAdditional = [NSMutableArray array];
  NSMutableArray<DBScanRootGrant *> *scopedGrants = [NSMutableArray array];
  for (DBScanRootInput *root in request.roots) {
    NSInteger rootId = root.scanRootId != nil
                             ? root.scanRootId.integerValue
                             : [indexWriter findOrInsertScanRootWithUriOrGrant:root.uriGrant
                                                                           mode:DBScanRootModeBridgeUserSelected];
    DBResolvedScanRoot *resolved = [[DBResolvedScanRoot alloc] init];
    resolved.scanRootId = rootId;
    resolved.uriGrant = root.uriGrant;
    resolved.mode = DBScanRootModeUserSelected;
    [resolvedAdditional addObject:resolved];
    [scopedGrants addObject:[[DBScanRootGrant alloc] initWithUriGrant:root.uriGrant
                                                                 mode:DBScanRootModeUserSelected]];
  }

  DBResolvedScanRoot *platformRoot = [[DBResolvedScanRoot alloc] init];
  platformRoot.scanRootId = platformRootId;
  platformRoot.uriGrant = DBPlatformDiscoveryMarkerUri;
  platformRoot.mode = DBScanRootModePlatformDiscovery;

  DBScanRootResolverPlan *plan = [[DBScanRootResolverPlan alloc] init];
  plan.primaryRootId = platformRootId;
  plan.roots = [@[platformRoot] arrayByAddingObjectsFromArray:resolvedAdditional];
  plan.platformGrant = [[DBScanRootGrant alloc] initWithUriGrant:DBPlatformDiscoveryMarkerUri
                                                            mode:DBScanRootModePlatformDiscovery];
  plan.additionalScopedGrants = [scopedGrants copy];
  return plan;
}

+ (DBScanRootResolverPlan *)resolveUserSelected:(DBScanStartRequest *)request
                                    indexWriter:(DBIndexWriter *)indexWriter
{
  if (request.roots.count == 0) {
    @throw [NSException exceptionWithName:NSInvalidArgumentException
                                   reason:@"user_selected scan requires at least one root grant"
                                 userInfo:nil];
  }

  NSMutableArray<DBResolvedScanRoot *> *resolved = [NSMutableArray array];
  for (DBScanRootInput *root in request.roots) {
    NSInteger rootId = root.scanRootId != nil
                             ? root.scanRootId.integerValue
                             : [indexWriter findOrInsertScanRootWithUriOrGrant:root.uriGrant
                                                                           mode:DBScanRootModeBridgeUserSelected];
    DBResolvedScanRoot *resolvedRoot = [[DBResolvedScanRoot alloc] init];
    resolvedRoot.scanRootId = rootId;
    resolvedRoot.uriGrant = root.uriGrant;
    resolvedRoot.mode = DBScanRootModeUserSelected;
    [resolved addObject:resolvedRoot];
  }

  DBScanRootResolverPlan *plan = [[DBScanRootResolverPlan alloc] init];
  plan.primaryRootId = resolved.firstObject.scanRootId;
  plan.roots = [resolved copy];
  plan.platformGrant = nil;
  plan.additionalScopedGrants = @[];
  return plan;
}

@end
