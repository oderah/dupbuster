#import "DBIndexWriter.h"

#import "DBSizeBucketPendingEntry.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogSchema.h"
#import "DBDiscoveredEntry.h"
#import "DBHashedFile.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"
#import "DBCheckpointStore.h"
#import "DBVideoContentMatcher.h"

@implementation DBIndexWriter {
  DBCatalogDatabase *_database;
  DBCheckpointStore *_checkpointStore;
}

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database
{
  self = [super init];
  if (self) {
    _database = database;
    _checkpointStore = [[DBCheckpointStore alloc] initWithDatabase:database];
  }
  return self;
}

- (DBCatalogMeta *)readCatalogMeta
{
  DBCatalogMeta *meta = [[DBCatalogMeta alloc] init];
  meta.schemaVersion = [self metaIntForKey:DBCatalogMetaSchemaVersion] ?: DBCatalogSchemaCurrentVersion;
  meta.fullRescanRequired = [self metaIntForKey:DBCatalogMetaFullRescanRequired] != 0;
  return meta;
}

- (NSInteger)insertScanRootWithUriOrGrant:(NSString *)uriOrGrant
                                     mode:(NSString *)mode
                           platformReason:(NSString *)platformReason
{
  sqlite3_stmt *stmt = NULL;
  const char *sql =
      "INSERT INTO scan_root (uri_or_grant, mode, platform_reason, created_at) VALUES (?, ?, ?, ?)";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, uriOrGrant.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 2, mode.UTF8String, -1, SQLITE_TRANSIENT);
  if (platformReason) {
    sqlite3_bind_text(stmt, 3, platformReason.UTF8String, -1, SQLITE_TRANSIENT);
  } else {
    sqlite3_bind_null(stmt, 3);
  }
  sqlite3_bind_int64(stmt, 4, (sqlite3_int64)(NSDate.date.timeIntervalSince1970 * 1000));
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return (NSInteger)sqlite3_last_insert_rowid(db);
}

- (nullable NSNumber *)findScanRootIdForUriOrGrant:(NSString *)uriOrGrant
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT id FROM scan_root WHERE uri_or_grant = ? ORDER BY id DESC LIMIT 1", -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, uriOrGrant.UTF8String, -1, SQLITE_TRANSIENT);
  NSNumber *rootId = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    rootId = @(sqlite3_column_int64(stmt, 0));
  }
  sqlite3_finalize(stmt);
  return rootId;
}

- (NSInteger)findOrInsertScanRootWithUriOrGrant:(NSString *)uriOrGrant mode:(NSString *)mode
{
  NSNumber *existing = [self findScanRootIdForUriOrGrant:uriOrGrant];
  if (existing != nil) {
    return existing.integerValue;
  }
  return [self insertScanRootWithUriOrGrant:uriOrGrant mode:mode platformReason:nil];
}

- (NSInteger)nextGenerationForRootId:(NSInteger)rootId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "SELECT COALESCE(MAX(generation), 0) + 1 FROM scan_run WHERE root_id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, rootId);
  NSInteger generation = 1;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    generation = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return generation;
}

- (BOOL)updateScanRunCheckpoint:(NSInteger)scanRunId
                lastProcessedId:(int64_t)lastProcessedId
                          error:(NSError **)error
{
  return [_checkpointStore saveCheckpointForRunId:scanRunId lastProcessedId:lastProcessedId error:error];
}

- (BOOL)completeScanRunWithId:(NSInteger)scanRunId error:(NSError **)error
{
  int64_t endedAtMs = (int64_t)(NSDate.date.timeIntervalSince1970 * 1000);
  return [_checkpointStore markCompleteRunWithId:scanRunId endedAtMs:endedAtMs error:error];
}

- (NSInteger)upsertUnscannableWithReason:(NSString *)reason
                                  staged:(DBStagedFile *)staged
                              generation:(NSInteger)generation
{
  return [self upsertFileEntryWithStaged:staged
                              generation:generation
                           fingerprintId:0
                   rawContentFingerprintId:0
                       unscannableReason:reason
                               isSymlink:staged.isSymlink];
}

- (NSInteger)beginScanRunWithGeneration:(NSInteger)generation rootId:(NSInteger)rootId
{
  NSError *error = nil;
  NSInteger runId = [_checkpointStore beginRunWithRootId:rootId generation:generation error:&error];
  if (error != nil) {
    return 0;
  }
  return runId;
}

- (NSInteger)persistHashPipelineResult:(DBHashPipelineResult *)result
                                staged:(DBStagedFile *)staged
                            generation:(NSInteger)generation
{
  switch (result.outcome) {
    case DBHashPipelineOutcomeSuccess:
      return [self upsertHashedFile:result.hashed generation:generation];
    case DBHashPipelineOutcomeSizeBucketSkipped:
      return [self upsertFileEntryWithStaged:staged
                                  generation:generation
                               fingerprintId:0
                       rawContentFingerprintId:0
                           unscannableReason:nil
                                   isSymlink:NO];
    case DBHashPipelineOutcomeSymlinkNode:
      return [self upsertFileEntryWithStaged:staged
                                  generation:generation
                               fingerprintId:0
                       rawContentFingerprintId:0
                           unscannableReason:nil
                                   isSymlink:YES];
    case DBHashPipelineOutcomeUnscannable:
      return [self upsertFileEntryWithStaged:staged
                                  generation:generation
                               fingerprintId:0
                       rawContentFingerprintId:0
                           unscannableReason:result.unscannableReason
                                   isSymlink:staged.isSymlink];
    case DBHashPipelineOutcomeVideoSuccess:
      return [self upsertVideoDualHashed:result.rawBytesHashed
                             videoContent:result.videoContentHashed
                               generation:generation];
    case DBHashPipelineOutcomeVideoPartialSuccess:
      return [self upsertVideoPartialHashed:result.rawBytesHashed
                      videoUnscannableReason:result.unscannableReason
                                generation:generation];
  }
}

- (NSInteger)upsertVideoDualHashed:(DBHashedFile *)rawBytes
                      videoContent:(DBHashedFile *)videoContent
                        generation:(NSInteger)generation
{
  NSInteger rawFingerprintId = [self getOrCreateFingerprintWithHashValue:rawBytes.hashValue
                                                    normalizationProfile:rawBytes.normalizationProfile
                                                         frameHashesBlob:nil];
  NSInteger videoFingerprintId =
      [self getOrCreateFingerprintWithHashValue:videoContent.hashValue
                           normalizationProfile:videoContent.normalizationProfile
                                frameHashesBlob:videoContent.frameHashesBlob];
  return [self upsertFileEntryWithStaged:rawBytes.staged
                              generation:generation
                           fingerprintId:videoFingerprintId
                   rawContentFingerprintId:rawFingerprintId
                       unscannableReason:nil
                               isSymlink:NO];
}

- (NSInteger)upsertVideoPartialHashed:(DBHashedFile *)rawBytes
                 videoUnscannableReason:(NSString *)videoUnscannableReason
                           generation:(NSInteger)generation
{
  NSInteger rawFingerprintId = [self getOrCreateFingerprintWithHashValue:rawBytes.hashValue
                                                    normalizationProfile:rawBytes.normalizationProfile
                                                         frameHashesBlob:nil];
  return [self upsertFileEntryWithStaged:rawBytes.staged
                              generation:generation
                           fingerprintId:rawFingerprintId
                   rawContentFingerprintId:0
                       unscannableReason:videoUnscannableReason
                               isSymlink:NO];
}

- (NSInteger)upsertHashedFile:(DBHashedFile *)hashed generation:(NSInteger)generation
{
  NSInteger fingerprintId = [self getOrCreateFingerprintWithHashValue:hashed.hashValue
                                                 normalizationProfile:hashed.normalizationProfile
                                                      frameHashesBlob:hashed.frameHashesBlob];
  return [self upsertFileEntryWithStaged:hashed.staged
                              generation:generation
                           fingerprintId:fingerprintId
                   rawContentFingerprintId:0
                       unscannableReason:nil
                               isSymlink:NO];
}

- (NSInteger)countIndexedFilesWithSize:(int64_t)sizeBytes
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  const char *sql =
      "SELECT COUNT(*) FROM file_entry WHERE size = ? AND (duration_ms IS NULL OR duration_ms = 0)";
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, sizeBytes);
  NSInteger count = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    count = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return count;
}

- (NSArray<DBSizeBucketPendingEntry *> *)listSizeBucketPendingEntriesWithSizeBytes:(int64_t)sizeBytes
                                                                        generation:(NSInteger)generation
                                                                excludeFileEntryId:(NSInteger)excludeFileEntryId
{
  NSMutableArray<DBSizeBucketPendingEntry *> *rows = [NSMutableArray array];
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  const char *sql =
      "SELECT id, root_id, uri_or_path, display_name, size, mtime_ns, last_seen_generation "
      "FROM file_entry "
      "WHERE size = ? "
      "AND last_seen_generation = ? "
      "AND fingerprint_id IS NULL "
      "AND unscannable_reason IS NULL "
      "AND is_symlink = 0 "
      "AND (duration_ms IS NULL OR duration_ms = 0) "
      "AND id != ? "
      "ORDER BY id ASC";
  if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
    return rows;
  }
  sqlite3_bind_int64(stmt, 1, sizeBytes);
  sqlite3_bind_int(stmt, 2, (int)generation);
  sqlite3_bind_int64(stmt, 3, (sqlite3_int64)excludeFileEntryId);
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    DBSizeBucketPendingEntry *entry =
        [[DBSizeBucketPendingEntry alloc] initWithFileEntryId:sqlite3_column_int(stmt, 0)
                                                       rootId:sqlite3_column_int(stmt, 1)
                                                    uriOrPath:[NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 2)]
                                                  displayName:[NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 3)]
                                                    sizeBytes:sqlite3_column_int64(stmt, 4)
                                                      mtimeNs:sqlite3_column_int64(stmt, 5)
                                                   generation:sqlite3_column_int(stmt, 6)];
    [rows addObject:entry];
  }
  sqlite3_finalize(stmt);
  return rows;
}

- (NSInteger)countVideosWithinDurationGate:(int64_t)durationMs
{
  if (durationMs <= 0) {
    return 0;
  }
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "SELECT duration_ms FROM file_entry WHERE duration_ms IS NOT NULL AND duration_ms > 0",
      -1,
      &stmt,
      NULL);
  NSInteger count = 0;
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    int64_t other = sqlite3_column_int64(stmt, 0);
    if ([DBVideoContentMatcher passesDurationGateWithDurationA:durationMs durationB:other]) {
      count++;
    }
  }
  sqlite3_finalize(stmt);
  return count;
}

- (NSInteger)purgeEntriesNotSeenInGeneration:(NSInteger)rootId generation:(NSInteger)generation
{
  sqlite3 *db = _database.db;
  sqlite3_stmt *stmt = NULL;
  const char *deleteAliases =
      "DELETE FROM file_path WHERE file_entry_id IN ("
      "SELECT id FROM file_entry WHERE root_id = ? AND last_seen_generation < ?)";
  sqlite3_prepare_v2(db, deleteAliases, -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, rootId);
  sqlite3_bind_int64(stmt, 2, generation);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);

  sqlite3_prepare_v2(
      db, "DELETE FROM file_entry WHERE root_id = ? AND last_seen_generation < ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, rootId);
  sqlite3_bind_int64(stmt, 2, generation);
  sqlite3_step(stmt);
  int changes = sqlite3_changes(db);
  sqlite3_finalize(stmt);
  return changes;
}

- (NSInteger)fileEntryCount
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM file_entry", -1, &stmt, NULL);
  NSInteger count = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    count = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return count;
}

#pragma mark - Private

- (NSInteger)metaIntForKey:(NSString *)key
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT value FROM meta WHERE key = ?", -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, key.UTF8String, -1, SQLITE_TRANSIENT);
  NSInteger value = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    value = [[NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 0)] integerValue];
  }
  sqlite3_finalize(stmt);
  return value;
}

- (NSInteger)getOrCreateFingerprintWithHashValue:(NSString *)hashValue
                              normalizationProfile:(NSString *)profile
                                   frameHashesBlob:(NSData *)frameHashesBlob
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db,
      "SELECT id FROM fingerprint WHERE hash_algo = ? AND hash_value = ? AND normalization_profile = ?",
      -1,
      &stmt,
      NULL);
  sqlite3_bind_text(stmt, 1, DBCatalogHashAlgoSha256.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 2, hashValue.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 3, profile.UTF8String, -1, SQLITE_TRANSIENT);
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    NSInteger fpId = sqlite3_column_int64(stmt, 0);
    sqlite3_finalize(stmt);
    return fpId;
  }
  sqlite3_finalize(stmt);

  if (frameHashesBlob != nil) {
    sqlite3_prepare_v2(
        db,
        "INSERT INTO fingerprint (hash_algo, hash_value, normalization_profile, computed_at, "
        "frame_hashes_blob) VALUES (?, ?, ?, ?, ?)",
        -1,
        &stmt,
        NULL);
  } else {
    sqlite3_prepare_v2(
        db,
        "INSERT INTO fingerprint (hash_algo, hash_value, normalization_profile, computed_at) "
        "VALUES (?, ?, ?, ?)",
        -1,
        &stmt,
        NULL);
  }
  sqlite3_bind_text(stmt, 1, DBCatalogHashAlgoSha256.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 2, hashValue.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 3, profile.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 4, (sqlite3_int64)(NSDate.date.timeIntervalSince1970 * 1000));
  if (frameHashesBlob != nil) {
    sqlite3_bind_blob(stmt, 5, frameHashesBlob.bytes, (int)frameHashesBlob.length, SQLITE_TRANSIENT);
  }
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return (NSInteger)sqlite3_last_insert_rowid(db);
}

- (NSInteger)upsertFileEntryWithStaged:(DBStagedFile *)staged
                              generation:(NSInteger)generation
                           fingerprintId:(NSInteger)fingerprintId
                   rawContentFingerprintId:(NSInteger)rawContentFingerprintId
                       unscannableReason:(NSString *)unscannableReason
                               isSymlink:(BOOL)isSymlink
{
  NSNumber *inode = staged.inode;
  NSNumber *deviceId = staged.deviceId;
  if (inode != nil && deviceId != nil) {
    NSInteger existing = [self fileEntryIdForInode:inode.longLongValue deviceId:deviceId.longLongValue];
    if (existing > 0) {
      NSString *uri = [self uriOrPathForStaged:staged];
      NSString *primary = [self primaryUriForFileEntryId:existing];
      if (primary != nil && ![primary isEqualToString:uri]) {
        [self insertPathAlias:uri fileEntryId:existing];
      }
      [self updateFileEntryId:existing
                       staged:staged
                   generation:generation
                fingerprintId:fingerprintId
        rawContentFingerprintId:rawContentFingerprintId
            unscannableReason:unscannableReason
                    isSymlink:isSymlink];
      return existing;
    }
  }

  NSInteger rootId = staged.discovered.scanRootId;
  NSString *uri = [self uriOrPathForStaged:staged];
  NSInteger existingByUri = [self fileEntryIdForRootId:rootId uriOrPath:uri];
  if (existingByUri > 0) {
    [self updateFileEntryId:existingByUri
                     staged:staged
                 generation:generation
              fingerprintId:fingerprintId
      rawContentFingerprintId:rawContentFingerprintId
          unscannableReason:unscannableReason
                  isSymlink:isSymlink];
    return existingByUri;
  }
  return [self insertFileEntryWithStaged:staged
                              generation:generation
                           fingerprintId:fingerprintId
                   rawContentFingerprintId:rawContentFingerprintId
                       unscannableReason:unscannableReason
                               isSymlink:isSymlink];
}

- (NSInteger)insertFileEntryWithStaged:(DBStagedFile *)staged
                            generation:(NSInteger)generation
                         fingerprintId:(NSInteger)fingerprintId
                 rawContentFingerprintId:(NSInteger)rawContentFingerprintId
                     unscannableReason:(NSString *)unscannableReason
                             isSymlink:(BOOL)isSymlink
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  const char *sql =
      "INSERT INTO file_entry (root_id, uri_or_path, display_name, size, mtime_ns, "
      "fingerprint_id, last_seen_generation, is_symlink, unscannable_reason, inode, device_id, "
      "duration_ms, video_width, video_height, raw_content_fingerprint_id) "
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  DBDiscoveredEntry *discovered = staged.discovered;
  sqlite3_bind_int64(stmt, 1, discovered.scanRootId);
  sqlite3_bind_text(stmt, 2, [self uriOrPathForStaged:staged].UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_text(stmt, 3, discovered.displayName.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 4, staged.sizeBytes);
  sqlite3_bind_int64(stmt, 5, staged.mtimeNs);
  if (fingerprintId > 0) {
    sqlite3_bind_int64(stmt, 6, fingerprintId);
  } else {
    sqlite3_bind_null(stmt, 6);
  }
  sqlite3_bind_int64(stmt, 7, generation);
  sqlite3_bind_int(stmt, 8, isSymlink ? 1 : 0);
  if (unscannableReason) {
    sqlite3_bind_text(stmt, 9, unscannableReason.UTF8String, -1, SQLITE_TRANSIENT);
  } else {
    sqlite3_bind_null(stmt, 9);
  }
  if (staged.inode) {
    sqlite3_bind_int64(stmt, 10, staged.inode.longLongValue);
  } else {
    sqlite3_bind_null(stmt, 10);
  }
  if (staged.deviceId) {
    sqlite3_bind_int64(stmt, 11, staged.deviceId.longLongValue);
  } else {
    sqlite3_bind_null(stmt, 11);
  }
  [self bindVideoMetadataOnStatement:stmt staged:staged startingAtIndex:12];
  if (rawContentFingerprintId > 0) {
    sqlite3_bind_int64(stmt, 15, rawContentFingerprintId);
  } else {
    sqlite3_bind_null(stmt, 15);
  }
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return (NSInteger)sqlite3_last_insert_rowid(db);
}

- (void)updateFileEntryId:(NSInteger)fileEntryId
                   staged:(DBStagedFile *)staged
               generation:(NSInteger)generation
            fingerprintId:(NSInteger)fingerprintId
    rawContentFingerprintId:(NSInteger)rawContentFingerprintId
        unscannableReason:(NSString *)unscannableReason
                isSymlink:(BOOL)isSymlink
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  const char *sql =
      "UPDATE file_entry SET display_name = ?, size = ?, mtime_ns = ?, fingerprint_id = ?, "
      "last_seen_generation = ?, is_symlink = ?, unscannable_reason = ?, inode = ?, device_id = ?, "
      "duration_ms = ?, video_width = ?, video_height = ?, raw_content_fingerprint_id = ? "
      "WHERE id = ?";
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, staged.discovered.displayName.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 2, staged.sizeBytes);
  sqlite3_bind_int64(stmt, 3, staged.mtimeNs);
  if (fingerprintId > 0) {
    sqlite3_bind_int64(stmt, 4, fingerprintId);
  } else {
    sqlite3_bind_null(stmt, 4);
  }
  sqlite3_bind_int64(stmt, 5, generation);
  sqlite3_bind_int(stmt, 6, isSymlink ? 1 : 0);
  if (unscannableReason) {
    sqlite3_bind_text(stmt, 7, unscannableReason.UTF8String, -1, SQLITE_TRANSIENT);
  } else {
    sqlite3_bind_null(stmt, 7);
  }
  if (staged.inode) {
    sqlite3_bind_int64(stmt, 8, staged.inode.longLongValue);
  } else {
    sqlite3_bind_null(stmt, 8);
  }
  if (staged.deviceId) {
    sqlite3_bind_int64(stmt, 9, staged.deviceId.longLongValue);
  } else {
    sqlite3_bind_null(stmt, 9);
  }
  [self bindVideoMetadataOnStatement:stmt staged:staged startingAtIndex:10];
  if (rawContentFingerprintId > 0) {
    sqlite3_bind_int64(stmt, 13, rawContentFingerprintId);
  } else {
    sqlite3_bind_null(stmt, 13);
  }
  sqlite3_bind_int64(stmt, 14, fileEntryId);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
}

- (void)bindVideoMetadataOnStatement:(sqlite3_stmt *)stmt
                              staged:(DBStagedFile *)staged
                    startingAtIndex:(int)startIndex
{
  if (staged.durationMs > 0) {
    sqlite3_bind_int64(stmt, startIndex, staged.durationMs);
  } else {
    sqlite3_bind_null(stmt, startIndex);
  }
  if (staged.videoWidth > 0) {
    sqlite3_bind_int64(stmt, startIndex + 1, staged.videoWidth);
  } else {
    sqlite3_bind_null(stmt, startIndex + 1);
  }
  if (staged.videoHeight > 0) {
    sqlite3_bind_int64(stmt, startIndex + 2, staged.videoHeight);
  } else {
    sqlite3_bind_null(stmt, startIndex + 2);
  }
}

- (void)insertPathAlias:(NSString *)aliasPath fileEntryId:(NSInteger)fileEntryId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "INSERT OR IGNORE INTO file_path (file_entry_id, alias_path) VALUES (?, ?)", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, fileEntryId);
  sqlite3_bind_text(stmt, 2, aliasPath.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
}

- (NSInteger)fileEntryIdForInode:(int64_t)inode deviceId:(int64_t)deviceId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "SELECT id FROM file_entry WHERE inode = ? AND device_id = ? LIMIT 1", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, inode);
  sqlite3_bind_int64(stmt, 2, deviceId);
  NSInteger result = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    result = sqlite3_column_int64(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return result;
}

- (NSInteger)fileEntryIdForRootId:(NSInteger)rootId uriOrPath:(NSString *)uriOrPath
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(
      db, "SELECT id FROM file_entry WHERE root_id = ? AND uri_or_path = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, rootId);
  sqlite3_bind_text(stmt, 2, uriOrPath.UTF8String, -1, SQLITE_TRANSIENT);
  NSInteger result = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    result = sqlite3_column_int64(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return result;
}

- (NSString *)primaryUriForFileEntryId:(NSInteger)fileEntryId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, "SELECT uri_or_path FROM file_entry WHERE id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, fileEntryId);
  NSString *uri = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    uri = @(sqlite3_column_text(stmt, 0));
  }
  sqlite3_finalize(stmt);
  return uri;
}

- (NSString *)uriOrPathForStaged:(DBStagedFile *)staged
{
  if (staged.discovered.phAssetLocalIdentifier.length > 0) {
    return staged.discovered.phAssetLocalIdentifier;
  }
  return staged.discovered.contentURL.absoluteString;
}

@end
