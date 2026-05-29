#import "DBGrouper.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBMatchKind.h"
#import "DBNormalizationProfile.h"

@implementation DBGrouperRebuildResult
@end

@interface DBGrouperFingerprintAggregate : NSObject
@property (nonatomic, assign) NSInteger fingerprintId;
@property (nonatomic, copy) NSString *normalizationProfile;
@property (nonatomic, strong) NSMutableArray<NSNumber *> *fileEntryIds;
@property (nonatomic, strong) NSMutableArray<NSNumber *> *sizes;
@end

@implementation DBGrouperFingerprintAggregate
@end

@interface DBGrouper ()
@property (nonatomic, strong) DBCatalogDatabase *database;
@end

@implementation DBGrouper

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database
{
  self = [super init];
  if (self) {
    _database = database;
  }
  return self;
}

- (DBGrouperRebuildResult *)rebuildDuplicateGroups
{
  sqlite3 *db = _database.db;
  sqlite3_exec(db, "BEGIN IMMEDIATE TRANSACTION", NULL, NULL, NULL);
  sqlite3_exec(db, "DELETE FROM duplicate_member", NULL, NULL, NULL);
  sqlite3_exec(db, "DELETE FROM duplicate_group", NULL, NULL, NULL);

  NSArray *exactCandidates = [self loadExactBytesCandidates];
  NSMutableSet<NSNumber *> *exactBytesMemberIds = [NSMutableSet set];
  NSInteger groupsCreated = 0;
  int64_t totalReclaimable = 0;

  for (NSDictionary *candidate in exactCandidates) {
    NSInteger groupId = [self insertGroupWithCandidate:candidate matchKind:DBMatchKindExactBytes];
    if (groupId > 0) {
      groupsCreated++;
      totalReclaimable += [candidate[@"reclaimable"] longLongValue];
      for (NSNumber *entryId in candidate[@"fileEntryIds"]) {
        [exactBytesMemberIds addObject:entryId];
      }
    }
  }

  NSArray *videoCandidates = [self loadVideoContentCandidates];
  for (NSDictionary *candidate in videoCandidates) {
    NSString *profile = candidate[@"profile"];
    if (![[DBGrouper matchKindForNormalizationProfile:profile]
            isEqualToString:DBMatchKindSameContentVideo]) {
      continue;
    }
    NSMutableArray<NSNumber *> *filtered = [NSMutableArray array];
    for (NSNumber *entryId in candidate[@"fileEntryIds"]) {
      if (![exactBytesMemberIds containsObject:entryId]) {
        [filtered addObject:entryId];
      }
    }
    if (filtered.count < 2) {
      continue;
    }
    int64_t reclaimable = [self estimateReclaimableBytesForFileEntryIds:filtered];
    NSMutableDictionary *adjusted = [candidate mutableCopy];
    adjusted[@"fileEntryIds"] = filtered;
    adjusted[@"memberCount"] = @(filtered.count);
    adjusted[@"reclaimable"] = @(reclaimable);
    NSInteger groupId = [self insertGroupWithCandidate:adjusted matchKind:DBMatchKindSameContentVideo];
    if (groupId > 0) {
      groupsCreated++;
      totalReclaimable += reclaimable;
    }
  }

  sqlite3_exec(db, "COMMIT", NULL, NULL, NULL);

  DBGrouperRebuildResult *result = [[DBGrouperRebuildResult alloc] init];
  result.groupsCreated = groupsCreated;
  result.totalReclaimableBytesEst = totalReclaimable;
  return result;
}

- (NSInteger)duplicateGroupCount
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM duplicate_group", -1, &stmt, NULL);
  NSInteger count = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    count = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return count;
}

- (NSInteger)memberCountForGroupId:(NSInteger)groupId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "SELECT COUNT(*) FROM duplicate_member WHERE group_id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, groupId);
  NSInteger count = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    count = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return count;
}

- (int64_t)reclaimableBytesForGroupId:(NSInteger)groupId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "SELECT reclaimable_bytes_est FROM duplicate_group WHERE id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, groupId);
  int64_t value = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    value = sqlite3_column_int64(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return value;
}

- (NSString *)matchKindForGroupId:(NSInteger)groupId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT match_kind FROM duplicate_group WHERE id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, groupId);
  NSString *kind = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    kind = @(sqlite3_column_text(stmt, 0));
  }
  sqlite3_finalize(stmt);
  return kind;
}

- (NSInteger)firstDuplicateGroupId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT id FROM duplicate_group ORDER BY id ASC LIMIT 1", -1, &stmt, NULL);
  NSInteger groupId = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    groupId = sqlite3_column_int64(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return groupId;
}

+ (NSString *)matchKindForNormalizationProfile:(NSString *)profile
{
  if ([profile isEqualToString:DBNormalizationProfileVideoContentV1]) {
    return DBMatchKindSameContentVideo;
  }
  return DBMatchKindExactBytes;
}

+ (int64_t)estimateReclaimableBytesForSizes:(NSArray<NSNumber *> *)sizes
{
  if (sizes.count < 2) {
    return 0;
  }
  int64_t sum = 0;
  int64_t maxSize = 0;
  for (NSNumber *size in sizes) {
    int64_t value = size.longLongValue;
    sum += value;
    if (value > maxSize) {
      maxSize = value;
    }
  }
  return sum - maxSize;
}

#pragma mark - Private

- (NSArray<NSDictionary *> *)loadExactBytesCandidates
{
  const char *sql =
      "SELECT COALESCE(fe.raw_content_fingerprint_id, fe.fingerprint_id), f.normalization_profile, "
      "fe.id, fe.size "
      "FROM file_entry fe "
      "INNER JOIN fingerprint f ON f.id = COALESCE(fe.raw_content_fingerprint_id, fe.fingerprint_id) "
      "WHERE fe.fingerprint_id IS NOT NULL "
      "AND fe.is_symlink = 0 "
      "AND fe.unscannable_reason IS NULL "
      "AND f.normalization_profile != 'VIDEO_CONTENT_V1' "
      "ORDER BY 1 ASC, fe.id ASC";
  return [self aggregateCandidatesForSQL:sql];
}

- (NSArray<NSDictionary *> *)loadVideoContentCandidates
{
  const char *sql =
      "SELECT fe.fingerprint_id, f.normalization_profile, fe.id, fe.size "
      "FROM file_entry fe "
      "INNER JOIN fingerprint f ON fe.fingerprint_id = f.id "
      "WHERE fe.fingerprint_id IS NOT NULL "
      "AND fe.is_symlink = 0 "
      "AND fe.unscannable_reason IS NULL "
      "AND f.normalization_profile = 'VIDEO_CONTENT_V1' "
      "ORDER BY fe.fingerprint_id ASC, fe.id ASC";
  return [self aggregateCandidatesForSQL:sql];
}

- (NSArray<NSDictionary *> *)aggregateCandidatesForSQL:(const char *)sql
{
  NSMutableDictionary<NSNumber *, DBGrouperFingerprintAggregate *> *byFingerprint =
      [NSMutableDictionary dictionary];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    NSInteger fingerprintId = sqlite3_column_int64(stmt, 0);
    NSString *profile = @(sqlite3_column_text(stmt, 1));
    NSInteger fileEntryId = sqlite3_column_int64(stmt, 2);
    int64_t size = sqlite3_column_int64(stmt, 3);
    NSNumber *fpKey = @(fingerprintId);
    DBGrouperFingerprintAggregate *aggregate = byFingerprint[fpKey];
    if (!aggregate) {
      aggregate = [[DBGrouperFingerprintAggregate alloc] init];
      aggregate.fingerprintId = fingerprintId;
      aggregate.normalizationProfile = profile;
      aggregate.fileEntryIds = [NSMutableArray array];
      aggregate.sizes = [NSMutableArray array];
      byFingerprint[fpKey] = aggregate;
    }
    [aggregate.fileEntryIds addObject:@(fileEntryId)];
    [aggregate.sizes addObject:@(size)];
  }
  sqlite3_finalize(stmt);

  NSMutableArray<NSDictionary *> *candidates = [NSMutableArray array];
  for (DBGrouperFingerprintAggregate *aggregate in byFingerprint.allValues) {
    if (aggregate.fileEntryIds.count < 2) {
      continue;
    }
    int64_t reclaimable = [[self class] estimateReclaimableBytesForSizes:aggregate.sizes];
    [candidates addObject:@{
      @"fingerprintId" : @(aggregate.fingerprintId),
      @"profile" : aggregate.normalizationProfile,
      @"fileEntryIds" : aggregate.fileEntryIds,
      @"memberCount" : @(aggregate.fileEntryIds.count),
      @"reclaimable" : @(reclaimable),
    }];
  }
  return candidates;
}

- (int64_t)estimateReclaimableBytesForFileEntryIds:(NSArray<NSNumber *> *)fileEntryIds
{
  if (fileEntryIds.count == 0) {
    return 0;
  }
  NSMutableArray<NSNumber *> *sizes = [NSMutableArray array];
  for (NSNumber *entryId in fileEntryIds) {
    sqlite3_stmt *stmt = NULL;
    sqlite3 *db = _database.db;
    sqlite3_prepare_v2(db, "SELECT size FROM file_entry WHERE id = ?", -1, &stmt, NULL);
    sqlite3_bind_int64(stmt, 1, entryId.longLongValue);
    if (sqlite3_step(stmt) == SQLITE_ROW) {
      [sizes addObject:@(sqlite3_column_int64(stmt, 0))];
    }
    sqlite3_finalize(stmt);
  }
  return [[self class] estimateReclaimableBytesForSizes:sizes];
}

- (NSInteger)insertGroupWithCandidate:(NSDictionary *)candidate matchKind:(NSString *)matchKind
{
  NSArray<NSNumber *> *fileEntryIds = candidate[@"fileEntryIds"];
  if (fileEntryIds.count < 2) {
    return 0;
  }
  double confidence =
      [matchKind isEqualToString:DBMatchKindSameContentVideo] ? DBMatchKindConfidenceVideoContent
                                                              : DBMatchKindConfidenceExact;
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "INSERT INTO duplicate_group (fingerprint_id, member_count, reclaimable_bytes_est, "
      "match_kind, confidence_score) VALUES (?, ?, ?, ?, ?)",
      -1,
      &stmt,
      NULL);
  sqlite3_bind_int64(stmt, 1, [candidate[@"fingerprintId"] longLongValue]);
  sqlite3_bind_int(stmt, 2, (int)[candidate[@"memberCount"] integerValue]);
  sqlite3_bind_int64(stmt, 3, [candidate[@"reclaimable"] longLongValue]);
  sqlite3_bind_text(stmt, 4, matchKind.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_double(stmt, 5, confidence);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  NSInteger groupId = sqlite3_last_insert_rowid(db);

  for (NSNumber *entryId in fileEntryIds) {
    sqlite3_prepare_v2(
        db,
        "INSERT INTO duplicate_member (group_id, file_entry_id, is_keeper) VALUES (?, ?, 0)",
        -1,
        &stmt,
        NULL);
    sqlite3_bind_int64(stmt, 1, groupId);
    sqlite3_bind_int64(stmt, 2, entryId.longLongValue);
    sqlite3_step(stmt);
    sqlite3_finalize(stmt);
  }
  return groupId;
}

@end
