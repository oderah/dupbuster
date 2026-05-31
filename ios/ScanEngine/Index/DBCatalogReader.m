#import "DBCatalogReader.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBDiscoveredEntry.h"
#import "DBMediaTypeHintResolver.h"

static const NSInteger kDBCatalogReaderThumbnailPreviewLimit = 4;

@implementation DBCatalogMember
@end

@implementation DBCatalogGroupSummary
@end

@implementation DBCatalogGroupDetail
@end

@implementation DBCatalogSnapshot
@end

@implementation DBCatalogReader {
  DBCatalogDatabase *_database;
}

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database
{
  self = [super init];
  if (self) {
    _database = database;
  }
  return self;
}

- (DBCatalogSnapshot *)readSnapshot
{
  NSArray<NSDictionary *> *groupRows = [self loadGroupRows];
  NSDictionary<NSNumber *, NSArray<DBCatalogMember *> *> *membersByGroupId = [self loadMembersByGroupId];
  NSMutableArray<DBCatalogGroupSummary *> *duplicateGroups = [NSMutableArray array];
  NSMutableDictionary<NSNumber *, DBCatalogGroupDetail *> *groupDetailsById = [NSMutableDictionary dictionary];

  for (NSDictionary *row in groupRows) {
    NSInteger groupId = [row[@"groupId"] integerValue];
    NSArray<DBCatalogMember *> *members = membersByGroupId[@(groupId)] ?: @[];
    NSInteger previewCount = MIN((NSInteger)members.count, kDBCatalogReaderThumbnailPreviewLimit);
    NSArray<DBCatalogMember *> *thumbnails = previewCount > 0 ? [members subarrayWithRange:NSMakeRange(0, previewCount)] : @[];

    DBCatalogGroupSummary *summary = [[DBCatalogGroupSummary alloc] init];
    summary.groupId = groupId;
    summary.matchKind = row[@"matchKind"];
    summary.memberCount = [row[@"memberCount"] integerValue];
    summary.reclaimableBytesEst = [row[@"reclaimableBytesEst"] longLongValue];
    summary.thumbnails = thumbnails;
    [duplicateGroups addObject:summary];

    DBCatalogGroupDetail *detail = [[DBCatalogGroupDetail alloc] init];
    detail.groupId = groupId;
    detail.matchKind = row[@"matchKind"];
    detail.memberCount = summary.memberCount;
    detail.reclaimableBytesEst = summary.reclaimableBytesEst;
    detail.members = members;
    groupDetailsById[@(groupId)] = detail;
  }

  DBCatalogSnapshot *snapshot = [[DBCatalogSnapshot alloc] init];
  snapshot.duplicateGroups = [duplicateGroups copy];
  snapshot.unscannableCounts = [self loadUnscannableCounts];
  snapshot.groupDetailsById = [groupDetailsById copy];
  return snapshot;
}

- (NSArray<NSDictionary *> *)loadGroupRows
{
  NSMutableArray<NSDictionary *> *rows = [NSMutableArray array];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "SELECT id, match_kind, member_count, reclaimable_bytes_est FROM duplicate_group ORDER BY id ASC",
      -1,
      &stmt,
      NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    [rows addObject:@{
      @"groupId" : @(sqlite3_column_int64(stmt, 0)),
      @"matchKind" : [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 1)],
      @"memberCount" : @(sqlite3_column_int(stmt, 2)),
      @"reclaimableBytesEst" : @(sqlite3_column_int64(stmt, 3)),
    }];
  }
  sqlite3_finalize(stmt);
  return [rows copy];
}

- (NSDictionary<NSNumber *, NSArray<DBCatalogMember *> *> *)loadMembersByGroupId
{
  NSMutableDictionary<NSNumber *, NSMutableArray<DBCatalogMember *> *> *membersByGroupId =
      [NSMutableDictionary dictionary];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "SELECT dm.group_id, fe.id, fe.display_name, fe.uri_or_path, fe.size, fe.mtime_ns, fe.duration_ms "
      "FROM duplicate_member dm "
      "INNER JOIN file_entry fe ON fe.id = dm.file_entry_id "
      "ORDER BY dm.group_id ASC, fe.id ASC",
      -1,
      &stmt,
      NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    NSInteger groupId = sqlite3_column_int64(stmt, 0);
    DBCatalogMember *member = [self memberFromStatement:stmt startingAtColumn:1];
    NSMutableArray<DBCatalogMember *> *members = membersByGroupId[@(groupId)];
    if (members == nil) {
      members = [NSMutableArray array];
      membersByGroupId[@(groupId)] = members;
    }
    [members addObject:member];
  }
  sqlite3_finalize(stmt);

  NSMutableDictionary<NSNumber *, NSArray<DBCatalogMember *> *> *result = [NSMutableDictionary dictionary];
  for (NSNumber *groupId in membersByGroupId) {
    result[groupId] = [membersByGroupId[groupId] copy];
  }
  return [result copy];
}

- (NSDictionary<NSString *, NSNumber *> *)loadUnscannableCounts
{
  NSMutableDictionary<NSString *, NSNumber *> *counts = [NSMutableDictionary dictionary];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "SELECT unscannable_reason, COUNT(*) FROM file_entry "
      "WHERE unscannable_reason IS NOT NULL GROUP BY unscannable_reason ORDER BY unscannable_reason ASC",
      -1,
      &stmt,
      NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    NSString *reason = [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 0)];
    counts[reason] = @(sqlite3_column_int(stmt, 1));
  }
  sqlite3_finalize(stmt);
  return [counts copy];
}

- (DBCatalogMember *)memberFromStatement:(sqlite3_stmt *)stmt startingAtColumn:(int)column
{
  NSString *displayName = [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, column + 1)];
  NSString *uriOrPath = [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, column + 2)];
  int64_t durationMs = sqlite3_column_type(stmt, column + 5) == SQLITE_NULL ? 0 : sqlite3_column_int64(stmt, column + 5);
  DBCatalogMediaTypeHint mediaTypeHint = [DBCatalogReader resolveMediaTypeHintForDisplayName:displayName
                                                                                  durationMs:durationMs];

  DBCatalogMember *member = [[DBCatalogMember alloc] init];
  member.fileEntryId = sqlite3_column_int64(stmt, column);
  member.displayName = displayName;
  member.sizeBytes = sqlite3_column_int64(stmt, column + 3);
  member.mtimeMs = sqlite3_column_int64(stmt, column + 4) / 1000000LL;
  member.pathLength = uriOrPath.length;
  member.mediaTypeHint = mediaTypeHint;
  member.thumbnailUri = [DBCatalogReader thumbnailUriForMediaTypeHint:mediaTypeHint uriOrPath:uriOrPath];
  return member;
}

+ (DBCatalogMediaTypeHint)resolveMediaTypeHintForDisplayName:(NSString *)displayName
                                                  durationMs:(int64_t)durationMs
{
  if (durationMs > 0) {
    return DBMediaTypeHintVideo;
  }
  return [DBMediaTypeHintResolver hintForFileName:displayName];
}

+ (nullable NSString *)thumbnailUriForMediaTypeHint:(DBCatalogMediaTypeHint)mediaTypeHint
                                          uriOrPath:(NSString *)uriOrPath
{
  if (![mediaTypeHint isEqualToString:DBMediaTypeHintImage] &&
      ![mediaTypeHint isEqualToString:DBMediaTypeHintVideo]) {
    return nil;
  }
  return uriOrPath;
}

@end
