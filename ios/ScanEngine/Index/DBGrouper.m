#import "DBGrouper.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBImageContentMatcher.h"
#import "DBImageConstants.h"
#import "DBMatchKind.h"
#import "DBNormalizationProfile.h"
#import "DBVideoContentMatcher.h"
#import "DBVideoConstants.h"
#import "DBVideoFingerprint.h"

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

@interface DBGrouperVideoContentEntry : NSObject
@property (nonatomic, assign) NSInteger fileEntryId;
@property (nonatomic, assign) NSInteger fingerprintId;
@property (nonatomic, copy) NSArray<NSNumber *> *frameHashes;
@property (nonatomic, assign) int64_t durationMs;
@property (nonatomic, assign) NSInteger videoWidth;
@property (nonatomic, assign) NSInteger videoHeight;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, strong, nullable) NSNumber *rawContentFingerprintId;
@end

@implementation DBGrouperVideoContentEntry
@end

@interface DBGrouperImageContentEntry : NSObject
@property (nonatomic, assign) NSInteger fileEntryId;
@property (nonatomic, assign) NSInteger fingerprintId;
@property (nonatomic, assign) uint64_t dHash;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, strong, nullable) NSNumber *rawContentFingerprintId;
@end

@implementation DBGrouperImageContentEntry
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

  NSMutableSet<NSNumber *> *exactBytesMemberIds = [NSMutableSet set];
  NSInteger groupsCreated = 0;
  int64_t totalReclaimable = 0;

  for (NSDictionary *candidate in [self loadExactBytesCandidates]) {
    NSInteger groupId = [self insertGroupWithCandidate:candidate matchKind:DBMatchKindExactBytes];
    if (groupId > 0) {
      groupsCreated++;
      totalReclaimable += [candidate[@"reclaimable"] longLongValue];
      for (NSNumber *entryId in candidate[@"fileEntryIds"]) {
        [exactBytesMemberIds addObject:entryId];
      }
    }
  }

  for (NSDictionary *cluster in [self clusterVideoContentEntries:[self loadVideoContentEntries]]) {
    if ([cluster[@"fileEntryIds"] count] < 2) {
      continue;
    }
    NSInteger groupId = [self insertGroupWithCandidate:cluster matchKind:cluster[@"matchKind"]];
    if (groupId > 0) {
      groupsCreated++;
      totalReclaimable += [cluster[@"reclaimable"] longLongValue];
      if ([cluster[@"matchKind"] isEqualToString:DBMatchKindExactBytes]) {
        for (NSNumber *entryId in cluster[@"fileEntryIds"]) {
          [exactBytesMemberIds addObject:entryId];
        }
      }
    }
  }

  for (NSDictionary *cluster in [self clusterImageContentEntries:[self loadImageContentEntries]]) {
    if ([cluster[@"fileEntryIds"] count] < 2) {
      continue;
    }
    NSInteger groupId = [self insertGroupWithCandidate:cluster matchKind:cluster[@"matchKind"]];
    if (groupId > 0) {
      groupsCreated++;
      totalReclaimable += [cluster[@"reclaimable"] longLongValue];
      if ([cluster[@"matchKind"] isEqualToString:DBMatchKindExactBytes]) {
        for (NSNumber *entryId in cluster[@"fileEntryIds"]) {
          [exactBytesMemberIds addObject:entryId];
        }
      }
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
  if ([profile isEqualToString:DBNormalizationProfileImageContentV1]) {
    return DBMatchKindSameContentImage;
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

static NSArray<NSNumber *> *DBGrouperDecodeFrameHashesBlob(NSData *blob)
{
  if (blob.length == 0) {
    return @[];
  }
  NSMutableArray<NSNumber *> *hashes = [NSMutableArray array];
  const uint8_t *bytes = blob.bytes;
  for (NSUInteger offset = 0; offset + sizeof(uint64_t) <= blob.length; offset += sizeof(uint64_t)) {
    uint64_t value = 0;
    memcpy(&value, bytes + offset, sizeof(uint64_t));
    [hashes addObject:@(value)];
  }
  return hashes;
}

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
      "AND fe.raw_content_fingerprint_id IS NULL "
      "AND f.normalization_profile NOT IN ('VIDEO_CONTENT_V1', 'IMAGE_CONTENT_V1') "
      "ORDER BY 1 ASC, fe.id ASC";
  return [self aggregateCandidatesForSQL:sql];
}

- (NSArray<DBGrouperVideoContentEntry *> *)loadVideoContentEntries
{
  const char *sql =
      "SELECT fe.id, fe.fingerprint_id, f.frame_hashes_blob, fe.size, fe.raw_content_fingerprint_id, "
      "fe.duration_ms, fe.video_width, fe.video_height "
      "FROM file_entry fe "
      "INNER JOIN fingerprint f ON fe.fingerprint_id = f.id "
      "WHERE fe.fingerprint_id IS NOT NULL "
      "AND fe.is_symlink = 0 "
      "AND fe.unscannable_reason IS NULL "
      "AND f.normalization_profile = 'VIDEO_CONTENT_V1' "
      "AND f.frame_hashes_blob IS NOT NULL "
      "ORDER BY fe.id ASC";
  NSMutableArray<DBGrouperVideoContentEntry *> *entries = [NSMutableArray array];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    NSData *blob = [NSData dataWithBytes:sqlite3_column_blob(stmt, 2)
                                  length:(NSUInteger)sqlite3_column_bytes(stmt, 2)];
    NSArray<NSNumber *> *hashes = DBGrouperDecodeFrameHashesBlob(blob);
    if (hashes.count == 0) {
      continue;
    }
    DBGrouperVideoContentEntry *entry = [[DBGrouperVideoContentEntry alloc] init];
    entry.fileEntryId = sqlite3_column_int64(stmt, 0);
    entry.fingerprintId = sqlite3_column_int64(stmt, 1);
    entry.frameHashes = hashes;
    entry.sizeBytes = sqlite3_column_int64(stmt, 3);
    entry.rawContentFingerprintId =
        sqlite3_column_type(stmt, 4) == SQLITE_NULL ? nil : @(sqlite3_column_int64(stmt, 4));
    entry.durationMs = sqlite3_column_type(stmt, 5) == SQLITE_NULL ? 0 : sqlite3_column_int64(stmt, 5);
    entry.videoWidth = sqlite3_column_type(stmt, 6) == SQLITE_NULL ? 0 : sqlite3_column_int(stmt, 6);
    entry.videoHeight = sqlite3_column_type(stmt, 7) == SQLITE_NULL ? 0 : sqlite3_column_int(stmt, 7);
    [entries addObject:entry];
  }
  sqlite3_finalize(stmt);
  return entries;
}

- (NSArray<NSDictionary *> *)clusterVideoContentEntries:(NSArray<DBGrouperVideoContentEntry *> *)entries
{
  if (entries.count < 2) {
    return @[];
  }

  NSInteger count = entries.count;
  NSInteger *parent = malloc((size_t)count * sizeof(NSInteger));
  for (NSInteger index = 0; index < count; index++) {
    parent[index] = index;
  }

  NSInteger (^find)(NSInteger) = ^NSInteger(NSInteger index) {
    NSInteger root = index;
    while (parent[root] != root) {
      root = parent[root];
    }
    NSInteger node = index;
    while (parent[node] != node) {
      NSInteger next = parent[node];
      parent[node] = root;
      node = next;
    }
    return root;
  };

  void (^unionNodes)(NSInteger, NSInteger) = ^(NSInteger left, NSInteger right) {
    NSInteger rootLeft = find(left);
    NSInteger rootRight = find(right);
    if (rootLeft != rootRight) {
      parent[rootRight] = rootLeft;
    }
  };

  for (NSInteger i = 0; i < count; i++) {
    DBGrouperVideoContentEntry *leftEntry = entries[i];
    DBVideoFingerprint *leftFingerprint =
        [[DBVideoFingerprint alloc] initWithFrameHashes:leftEntry.frameHashes
                                             durationMs:leftEntry.durationMs
                                             videoWidth:leftEntry.videoWidth
                                            videoHeight:leftEntry.videoHeight];
    for (NSInteger j = i + 1; j < count; j++) {
      DBGrouperVideoContentEntry *rightEntry = entries[j];
      DBVideoFingerprint *rightFingerprint =
          [[DBVideoFingerprint alloc] initWithFrameHashes:rightEntry.frameHashes
                                               durationMs:rightEntry.durationMs
                                               videoWidth:rightEntry.videoWidth
                                              videoHeight:rightEntry.videoHeight];
      if ([DBVideoContentMatcher contentMatchesLeft:leftFingerprint
                                              right:rightFingerprint
                                   hammingThreshold:DBVideoHammingThresholdDefault]) {
        unionNodes(i, j);
      }
    }
  }

  NSMutableDictionary<NSNumber *, NSMutableArray<DBGrouperVideoContentEntry *> *> *clusters =
      [NSMutableDictionary dictionary];
  for (NSInteger index = 0; index < count; index++) {
    NSInteger root = find(index);
    NSNumber *key = @(root);
    NSMutableArray *bucket = clusters[key];
    if (bucket == nil) {
      bucket = [NSMutableArray array];
      clusters[key] = bucket;
    }
    [bucket addObject:entries[index]];
  }
  free(parent);

  NSMutableArray<NSDictionary *> *results = [NSMutableArray array];
  for (NSArray<DBGrouperVideoContentEntry *> *cluster in clusters.allValues) {
    if (cluster.count < 2) {
      continue;
    }
    NSMutableArray<NSNumber *> *fileEntryIds = [NSMutableArray array];
    NSMutableArray<NSNumber *> *sizes = [NSMutableArray array];
    for (DBGrouperVideoContentEntry *entry in cluster) {
      [fileEntryIds addObject:@(entry.fileEntryId)];
      [sizes addObject:@(entry.sizeBytes)];
    }
    NSString *matchKind = [self matchKindForVideoCluster:cluster];
    [results addObject:@{
      @"fingerprintId" : @(cluster.firstObject.fingerprintId),
      @"profile" : DBNormalizationProfileVideoContentV1,
      @"fileEntryIds" : fileEntryIds,
      @"memberCount" : @(fileEntryIds.count),
      @"reclaimable" : @([[self class] estimateReclaimableBytesForSizes:sizes]),
      @"matchKind" : matchKind,
    }];
  }
  return results;
}

- (NSString *)matchKindForVideoCluster:(NSArray<DBGrouperVideoContentEntry *> *)cluster
{
  NSMutableSet<NSNumber *> *rawIds = [NSMutableSet set];
  BOOL allPresent = YES;
  for (DBGrouperVideoContentEntry *entry in cluster) {
    if (entry.rawContentFingerprintId == nil) {
      allPresent = NO;
      break;
    }
    [rawIds addObject:entry.rawContentFingerprintId];
  }
  if (allPresent && rawIds.count == 1) {
    return DBMatchKindExactBytes;
  }
  return DBMatchKindSameContentVideo;
}

- (NSArray<DBGrouperImageContentEntry *> *)loadImageContentEntries
{
  const char *sql =
      "SELECT fe.id, fe.fingerprint_id, f.frame_hashes_blob, fe.size, fe.raw_content_fingerprint_id "
      "FROM file_entry fe "
      "INNER JOIN fingerprint f ON fe.fingerprint_id = f.id "
      "WHERE fe.fingerprint_id IS NOT NULL "
      "AND fe.is_symlink = 0 "
      "AND fe.unscannable_reason IS NULL "
      "AND f.normalization_profile = 'IMAGE_CONTENT_V1' "
      "AND f.frame_hashes_blob IS NOT NULL "
      "ORDER BY fe.id ASC";
  NSMutableArray<DBGrouperImageContentEntry *> *entries = [NSMutableArray array];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    NSData *blob = [NSData dataWithBytes:sqlite3_column_blob(stmt, 2)
                                  length:(NSUInteger)sqlite3_column_bytes(stmt, 2)];
    NSArray<NSNumber *> *hashes = DBGrouperDecodeFrameHashesBlob(blob);
    if (hashes.count == 0) {
      continue;
    }
    DBGrouperImageContentEntry *entry = [[DBGrouperImageContentEntry alloc] init];
    entry.fileEntryId = sqlite3_column_int64(stmt, 0);
    entry.fingerprintId = sqlite3_column_int64(stmt, 1);
    entry.dHash = hashes.firstObject.unsignedLongLongValue;
    entry.sizeBytes = sqlite3_column_int64(stmt, 3);
    entry.rawContentFingerprintId =
        sqlite3_column_type(stmt, 4) == SQLITE_NULL ? nil : @(sqlite3_column_int64(stmt, 4));
    [entries addObject:entry];
  }
  sqlite3_finalize(stmt);
  return entries;
}

- (NSArray<NSDictionary *> *)clusterImageContentEntries:(NSArray<DBGrouperImageContentEntry *> *)entries
{
  if (entries.count < 2) {
    return @[];
  }

  NSInteger count = entries.count;
  NSInteger *parent = malloc((size_t)count * sizeof(NSInteger));
  for (NSInteger index = 0; index < count; index++) {
    parent[index] = index;
  }

  NSInteger (^find)(NSInteger) = ^NSInteger(NSInteger index) {
    NSInteger root = index;
    while (parent[root] != root) {
      root = parent[root];
    }
    NSInteger node = index;
    while (parent[node] != node) {
      NSInteger next = parent[node];
      parent[node] = root;
      node = next;
    }
    return root;
  };

  void (^unionNodes)(NSInteger, NSInteger) = ^(NSInteger left, NSInteger right) {
    NSInteger rootLeft = find(left);
    NSInteger rootRight = find(right);
    if (rootLeft != rootRight) {
      parent[rootRight] = rootLeft;
    }
  };

  for (NSInteger i = 0; i < count; i++) {
    uint64_t leftHash = entries[i].dHash;
    for (NSInteger j = i + 1; j < count; j++) {
      if ([DBImageContentMatcher matchesLeft:leftHash
                                       right:entries[j].dHash
                            hammingThreshold:DBImageHammingThresholdDefault]) {
        unionNodes(i, j);
      }
    }
  }

  NSMutableDictionary<NSNumber *, NSMutableArray<DBGrouperImageContentEntry *> *> *clusters =
      [NSMutableDictionary dictionary];
  for (NSInteger index = 0; index < count; index++) {
    NSInteger root = find(index);
    NSNumber *key = @(root);
    NSMutableArray *bucket = clusters[key];
    if (bucket == nil) {
      bucket = [NSMutableArray array];
      clusters[key] = bucket;
    }
    [bucket addObject:entries[index]];
  }
  free(parent);

  NSMutableArray<NSDictionary *> *results = [NSMutableArray array];
  for (NSArray<DBGrouperImageContentEntry *> *cluster in clusters.allValues) {
    if (cluster.count < 2) {
      continue;
    }
    NSMutableArray<NSNumber *> *fileEntryIds = [NSMutableArray array];
    NSMutableArray<NSNumber *> *sizes = [NSMutableArray array];
    for (DBGrouperImageContentEntry *entry in cluster) {
      [fileEntryIds addObject:@(entry.fileEntryId)];
      [sizes addObject:@(entry.sizeBytes)];
    }
    NSString *matchKind = [self matchKindForImageCluster:cluster];
    [results addObject:@{
      @"fingerprintId" : @(cluster.firstObject.fingerprintId),
      @"profile" : DBNormalizationProfileImageContentV1,
      @"fileEntryIds" : fileEntryIds,
      @"memberCount" : @(fileEntryIds.count),
      @"reclaimable" : @([[self class] estimateReclaimableBytesForSizes:sizes]),
      @"matchKind" : matchKind,
    }];
  }
  return results;
}

- (NSString *)matchKindForImageCluster:(NSArray<DBGrouperImageContentEntry *> *)cluster
{
  NSMutableSet<NSNumber *> *rawIds = [NSMutableSet set];
  BOOL allPresent = YES;
  for (DBGrouperImageContentEntry *entry in cluster) {
    if (entry.rawContentFingerprintId == nil) {
      allPresent = NO;
      break;
    }
    [rawIds addObject:entry.rawContentFingerprintId];
  }
  if (allPresent && rawIds.count == 1) {
    return DBMatchKindExactBytes;
  }
  return DBMatchKindSameContentImage;
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

- (NSInteger)insertGroupWithCandidate:(NSDictionary *)candidate matchKind:(NSString *)matchKind
{
  NSArray<NSNumber *> *fileEntryIds = candidate[@"fileEntryIds"];
  if (fileEntryIds.count < 2) {
    return 0;
  }
  double confidence = DBMatchKindConfidenceExact;
  if ([matchKind isEqualToString:DBMatchKindSameContentVideo]) {
    confidence = DBMatchKindConfidenceVideoContent;
  } else if ([matchKind isEqualToString:DBMatchKindSameContentImage]) {
    confidence = DBMatchKindConfidenceImageContent;
  }
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
