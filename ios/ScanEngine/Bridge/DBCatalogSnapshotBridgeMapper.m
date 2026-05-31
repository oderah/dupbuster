#import "DBCatalogSnapshotBridgeMapper.h"

#import "DBCatalogReader.h"

static NSSet<NSString *> *DBCatalogSnapshotBridgeAllowedTopLevelKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[@"duplicateGroups", @"unscannableCounts", @"groupDetailsById"]];
  });
  return keys;
}

static NSSet<NSString *> *DBCatalogSnapshotBridgeAllowedGroupSummaryKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[@"groupId", @"matchKind", @"memberCount", @"reclaimableBytesEst", @"thumbnails"]];
  });
  return keys;
}

static NSSet<NSString *> *DBCatalogSnapshotBridgeAllowedThumbnailKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[@"fileEntryId", @"mediaTypeHint", @"thumbnailUri"]];
  });
  return keys;
}

static NSSet<NSString *> *DBCatalogSnapshotBridgeAllowedGroupDetailKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[@"groupId", @"matchKind", @"memberCount", @"reclaimableBytesEst", @"members"]];
  });
  return keys;
}

static NSSet<NSString *> *DBCatalogSnapshotBridgeAllowedMemberKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[
      @"fileEntryId",
      @"displayName",
      @"sizeBytes",
      @"mtimeMs",
      @"pathLength",
      @"mediaTypeHint",
      @"thumbnailUri",
    ]];
  });
  return keys;
}

@implementation DBCatalogSnapshotBridgeMapper

+ (NSDictionary *)bridgePayloadForSnapshot:(DBCatalogSnapshot *)snapshot
{
  NSMutableArray *duplicateGroups = [NSMutableArray arrayWithCapacity:snapshot.duplicateGroups.count];
  for (DBCatalogGroupSummary *group in snapshot.duplicateGroups) {
    [duplicateGroups addObject:[self groupSummaryPayload:group]];
  }

  NSMutableDictionary *unscannableCounts = [NSMutableDictionary dictionary];
  for (NSString *reason in snapshot.unscannableCounts) {
    unscannableCounts[reason] = snapshot.unscannableCounts[reason];
  }

  NSMutableDictionary *groupDetailsById = [NSMutableDictionary dictionary];
  for (NSNumber *groupId in snapshot.groupDetailsById) {
    groupDetailsById[groupId.stringValue] = [self groupDetailPayload:snapshot.groupDetailsById[groupId]];
  }

  NSDictionary *payload = @{
    @"duplicateGroups" : [duplicateGroups copy],
    @"unscannableCounts" : [unscannableCounts copy],
    @"groupDetailsById" : [groupDetailsById copy],
  };
  [self assertBridgeSafePayload:payload];
  return payload;
}

+ (NSDictionary *)groupSummaryPayload:(DBCatalogGroupSummary *)group
{
  return @{
    @"groupId" : @(group.groupId),
    @"matchKind" : group.matchKind,
    @"memberCount" : @(group.memberCount),
    @"reclaimableBytesEst" : @(group.reclaimableBytesEst),
    @"thumbnails" : [self thumbnailPayloads:group.thumbnails],
  };
}

+ (NSDictionary *)groupDetailPayload:(DBCatalogGroupDetail *)detail
{
  return @{
    @"groupId" : @(detail.groupId),
    @"matchKind" : detail.matchKind,
    @"memberCount" : @(detail.memberCount),
    @"reclaimableBytesEst" : @(detail.reclaimableBytesEst),
    @"members" : [self memberPayloads:detail.members],
  };
}

+ (NSArray<NSDictionary *> *)thumbnailPayloads:(NSArray<DBCatalogMember *> *)members
{
  NSMutableArray<NSDictionary *> *payloads = [NSMutableArray arrayWithCapacity:members.count];
  for (DBCatalogMember *member in members) {
    NSMutableDictionary *payload = [@{
      @"fileEntryId" : @(member.fileEntryId),
      @"mediaTypeHint" : member.mediaTypeHint,
    } mutableCopy];
    if (member.thumbnailUri != nil) {
      payload[@"thumbnailUri"] = member.thumbnailUri;
    }
    [payloads addObject:[payload copy]];
  }
  return [payloads copy];
}

+ (NSArray<NSDictionary *> *)memberPayloads:(NSArray<DBCatalogMember *> *)members
{
  NSMutableArray<NSDictionary *> *payloads = [NSMutableArray arrayWithCapacity:members.count];
  for (DBCatalogMember *member in members) {
    NSMutableDictionary *payload = [@{
      @"fileEntryId" : @(member.fileEntryId),
      @"displayName" : member.displayName,
      @"sizeBytes" : @(member.sizeBytes),
      @"mtimeMs" : @(member.mtimeMs),
      @"pathLength" : @(member.pathLength),
      @"mediaTypeHint" : member.mediaTypeHint,
    } mutableCopy];
    if (member.thumbnailUri != nil) {
      payload[@"thumbnailUri"] = member.thumbnailUri;
    }
    [payloads addObject:[payload copy]];
  }
  return [payloads copy];
}

+ (void)assertBridgeSafePayload:(NSDictionary *)payload
{
  [self assertKeys:[payload allKeys] allowed:DBCatalogSnapshotBridgeAllowedTopLevelKeys()];

  for (NSDictionary *group in payload[@"duplicateGroups"]) {
    [self assertKeys:[group allKeys] allowed:DBCatalogSnapshotBridgeAllowedGroupSummaryKeys()];
    for (NSDictionary *thumbnail in group[@"thumbnails"]) {
      [self assertKeys:[thumbnail allKeys] allowed:DBCatalogSnapshotBridgeAllowedThumbnailKeys()];
    }
  }

  for (NSString *groupId in payload[@"groupDetailsById"]) {
    NSDictionary *detail = payload[@"groupDetailsById"][groupId];
    [self assertKeys:[detail allKeys] allowed:DBCatalogSnapshotBridgeAllowedGroupDetailKeys()];
    for (NSDictionary *member in detail[@"members"]) {
      [self assertKeys:[member allKeys] allowed:DBCatalogSnapshotBridgeAllowedMemberKeys()];
    }
  }
}

+ (void)assertKeys:(NSArray<NSString *> *)keys allowed:(NSSet<NSString *> *)allowed
{
  for (NSString *key in keys) {
    NSAssert([allowed containsObject:key], @"Forbidden bridge field: %@", key);
  }
}

@end
