#import "DBCheckpointStore.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBScanRunSnapshot.h"
#import "DBScanRunStatus.h"

NSString *const DBCheckpointStoreErrorDomain = @"com.dupbuster.scanengine.checkpoint";

@implementation DBCheckpointStore {
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

- (NSInteger)beginRunWithRootId:(NSInteger)rootId
                     generation:(NSInteger)generation
                           error:(NSError **)error
{
  if ([self hasConflictingActiveRunForRootId:rootId generation:generation excludeRunId:0]) {
    if (error) {
      *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                   code:DBCheckpointStoreErrorConflict
                               userInfo:@{
                                 @"rootId" : @(rootId),
                                 @"generation" : @(generation),
                               }];
    }
    return 0;
  }

  sqlite3_stmt *stmt = NULL;
  const char *sql =
      "INSERT INTO scan_run (root_id, generation, status, last_processed_id, started_at) "
      "VALUES (?, ?, ?, 0, ?)";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, rootId);
  sqlite3_bind_int64(stmt, 2, generation);
  sqlite3_bind_text(stmt, 3, DBScanRunStatusRunning.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 4, (sqlite3_int64)(NSDate.date.timeIntervalSince1970 * 1000));
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return (NSInteger)sqlite3_last_insert_rowid(db);
}

- (nullable DBScanRunSnapshot *)runWithId:(NSInteger)scanRunId
{
  sqlite3_stmt *stmt = NULL;
  const char *sql =
      "SELECT id, root_id, generation, status, last_processed_id, started_at, ended_at, "
      "teardown_reason FROM scan_run WHERE id = ?";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, scanRunId);
  DBScanRunSnapshot *snapshot = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    snapshot = [self snapshotFromStatement:stmt];
  }
  sqlite3_finalize(stmt);
  return snapshot;
}

- (nullable DBScanRunSnapshot *)findResumableRun
{
  NSArray<NSString *> *statuses = DBScanRunStatusResumableValues();
  NSString *placeholders = [self inClausePlaceholders:statuses.count];
  NSString *sql =
      [NSString stringWithFormat:
                    @"SELECT id, root_id, generation, status, last_processed_id, started_at, ended_at, "
                    @"teardown_reason FROM scan_run WHERE status IN (%@) ORDER BY id DESC LIMIT 1",
                    placeholders];
  return [self firstSnapshotForSQL:sql args:statuses];
}

- (nullable DBScanRunSnapshot *)findResumableRunForRootId:(NSInteger)rootId
{
  NSArray<NSString *> *statuses = DBScanRunStatusResumableValues();
  NSString *placeholders = [self inClausePlaceholders:statuses.count];
  NSString *sql =
      [NSString stringWithFormat:
                    @"SELECT id, root_id, generation, status, last_processed_id, started_at, ended_at, "
                    @"teardown_reason FROM scan_run WHERE status IN (%@) AND root_id = ? "
                    @"ORDER BY id DESC LIMIT 1",
                    placeholders];
  NSMutableArray *args = [statuses mutableCopy];
  [args addObject:@(rootId).stringValue];
  return [self firstSnapshotForSQL:sql args:args];
}

- (BOOL)hasConflictingActiveRunForRootId:(NSInteger)rootId
                              generation:(NSInteger)generation
                            excludeRunId:(NSInteger)excludeRunId
{
  NSArray<NSString *> *active = DBScanRunStatusActiveValues();
  NSString *placeholders = [self inClausePlaceholders:active.count];
  NSMutableString *sql =
      [NSMutableString stringWithFormat:@"SELECT id FROM scan_run WHERE root_id = ? AND generation = ? "
                                        @"AND status IN (%@)",
                                        placeholders];
  NSMutableArray<NSString *> *args = [NSMutableArray arrayWithObjects:@(rootId).stringValue,
                                                                       @(generation).stringValue,
                                                                       nil];
  [args addObjectsFromArray:active];
  if (excludeRunId > 0) {
    [sql appendString:@" AND id != ?"];
    [args addObject:@(excludeRunId).stringValue];
  }
  [sql appendString:@" LIMIT 1"];

  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql.UTF8String, -1, &stmt, NULL);
  for (NSInteger i = 0; i < args.count; i++) {
    sqlite3_bind_text(stmt, (int)i + 1, args[i].UTF8String, -1, SQLITE_TRANSIENT);
  }
  BOOL found = sqlite3_step(stmt) == SQLITE_ROW;
  sqlite3_finalize(stmt);
  return found;
}

- (BOOL)saveCheckpointForRunId:(NSInteger)scanRunId
               lastProcessedId:(int64_t)lastProcessedId
                         error:(NSError **)error
{
  DBScanRunSnapshot *run = [self runWithId:scanRunId];
  if (!run) {
    if (error) {
      *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                   code:DBCheckpointStoreErrorNotFound
                               userInfo:nil];
    }
    return NO;
  }
  if (![DBScanRunStatusResumableValues() containsObject:run.status]) {
    if (error) {
      *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                   code:DBCheckpointStoreErrorInvalidTransition
                               userInfo:nil];
    }
    return NO;
  }
  if (lastProcessedId < run.lastProcessedId) {
    if (error) {
      *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                   code:DBCheckpointStoreErrorMonotonicCheckpoint
                               userInfo:nil];
    }
    return NO;
  }

  sqlite3_stmt *stmt = NULL;
  const char *sql = "UPDATE scan_run SET last_processed_id = ? WHERE id = ?";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, lastProcessedId);
  sqlite3_bind_int64(stmt, 2, scanRunId);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return YES;
}

- (BOOL)pauseRunWithId:(NSInteger)scanRunId error:(NSError **)error
{
  return [self updateStatus:DBScanRunStatusPaused forRunId:scanRunId requireResumable:YES error:error];
}

- (nullable DBScanRunSnapshot *)resumeRunWithId:(NSInteger)scanRunId error:(NSError **)error
{
  if (![self updateStatus:DBScanRunStatusRunning forRunId:scanRunId requireResumable:YES error:error]) {
    return nil;
  }
  return [self runWithId:scanRunId];
}

- (BOOL)markCancellingRunWithId:(NSInteger)scanRunId error:(NSError **)error
{
  return [self updateStatus:DBScanRunStatusCancelling forRunId:scanRunId requireResumable:YES error:error];
}

- (BOOL)markCancelledRunWithId:(NSInteger)scanRunId
                    endedAtMs:(int64_t)endedAtMs
               teardownReason:(NSString *)teardownReason
                        error:(NSError **)error
{
  return [self updateTerminalStatus:DBScanRunStatusCancelled
                         forRunId:scanRunId
                        endedAtMs:endedAtMs
                   teardownReason:teardownReason
                            error:error];
}

- (BOOL)markCompleteRunWithId:(NSInteger)scanRunId
                    endedAtMs:(int64_t)endedAtMs
                        error:(NSError **)error
{
  return [self updateTerminalStatus:DBScanRunStatusComplete
                         forRunId:scanRunId
                        endedAtMs:endedAtMs
                   teardownReason:nil
                            error:error];
}

- (BOOL)markErrorRunWithId:(NSInteger)scanRunId
                 endedAtMs:(int64_t)endedAtMs
            teardownReason:(NSString *)teardownReason
                     error:(NSError **)error
{
  return [self updateTerminalStatus:DBScanRunStatusError
                         forRunId:scanRunId
                        endedAtMs:endedAtMs
                   teardownReason:teardownReason
                            error:error];
}

- (BOOL)abandonForRestartRunWithId:(NSInteger)scanRunId
                         endedAtMs:(int64_t)endedAtMs
                             error:(NSError **)error
{
  return [self markCancelledRunWithId:scanRunId
                            endedAtMs:endedAtMs
                       teardownReason:@"USER_RESTART"
                                error:error];
}

#pragma mark - Private

- (BOOL)updateStatus:(NSString *)status
           forRunId:(NSInteger)scanRunId
    requireResumable:(BOOL)requireResumable
               error:(NSError **)error
{
  DBScanRunSnapshot *run = [self runWithId:scanRunId];
  if (!run) {
    if (error) {
      *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                   code:DBCheckpointStoreErrorNotFound
                               userInfo:nil];
    }
    return NO;
  }
  if (requireResumable) {
    BOOL ok = [DBScanRunStatusResumableValues() containsObject:run.status] ||
              [run.status isEqualToString:DBScanRunStatusCancelling];
    if (!ok) {
      if (error) {
        *error = [NSError errorWithDomain:DBCheckpointStoreErrorDomain
                                     code:DBCheckpointStoreErrorInvalidTransition
                                 userInfo:nil];
      }
      return NO;
    }
  }

  sqlite3_stmt *stmt = NULL;
  const char *sql = "UPDATE scan_run SET status = ? WHERE id = ?";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, status.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 2, scanRunId);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return YES;
}

- (BOOL)updateTerminalStatus:(NSString *)status
                   forRunId:(NSInteger)scanRunId
                  endedAtMs:(int64_t)endedAtMs
             teardownReason:(NSString *)teardownReason
                      error:(NSError **)error
{
  (void)error;
  sqlite3_stmt *stmt = NULL;
  const char *sql = "UPDATE scan_run SET status = ?, ended_at = ?, teardown_reason = ? WHERE id = ?";
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql, -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, status.UTF8String, -1, SQLITE_TRANSIENT);
  sqlite3_bind_int64(stmt, 2, endedAtMs);
  if (teardownReason) {
    sqlite3_bind_text(stmt, 3, teardownReason.UTF8String, -1, SQLITE_TRANSIENT);
  } else {
    sqlite3_bind_null(stmt, 3);
  }
  sqlite3_bind_int64(stmt, 4, scanRunId);
  sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  return YES;
}

- (nullable DBScanRunSnapshot *)firstSnapshotForSQL:(NSString *)sql args:(NSArray *)args
{
  sqlite3_stmt *stmt = NULL;
  sqlite3 *db = _database.db;
  sqlite3_prepare_v2(db, sql.UTF8String, -1, &stmt, NULL);
  for (NSInteger i = 0; i < args.count; i++) {
    id value = args[i];
    if ([value isKindOfClass:[NSString class]]) {
      sqlite3_bind_text(stmt, (int)i + 1, [value UTF8String], -1, SQLITE_TRANSIENT);
    } else if ([value isKindOfClass:[NSNumber class]]) {
      sqlite3_bind_int64(stmt, (int)i + 1, [value longLongValue]);
    }
  }
  DBScanRunSnapshot *snapshot = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    snapshot = [self snapshotFromStatement:stmt];
  }
  sqlite3_finalize(stmt);
  return snapshot;
}

- (DBScanRunSnapshot *)snapshotFromStatement:(sqlite3_stmt *)stmt
{
  DBScanRunSnapshot *snapshot = [[DBScanRunSnapshot alloc] init];
  snapshot.scanRunId = sqlite3_column_int64(stmt, 0);
  if (sqlite3_column_type(stmt, 1) == SQLITE_NULL) {
    snapshot.rootId = nil;
  } else {
    snapshot.rootId = @(sqlite3_column_int64(stmt, 1));
  }
  snapshot.generation = sqlite3_column_int(stmt, 2);
  snapshot.status = [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 3)];
  snapshot.lastProcessedId = sqlite3_column_int64(stmt, 4);
  snapshot.startedAtMs = sqlite3_column_int64(stmt, 5);
  if (sqlite3_column_type(stmt, 6) == SQLITE_NULL) {
    snapshot.endedAtMs = nil;
  } else {
    snapshot.endedAtMs = @(sqlite3_column_int64(stmt, 6));
  }
  if (sqlite3_column_type(stmt, 7) == SQLITE_NULL) {
    snapshot.teardownReason = nil;
  } else {
    snapshot.teardownReason =
        [NSString stringWithUTF8String:(const char *)sqlite3_column_text(stmt, 7)];
  }
  return snapshot;
}

- (NSString *)inClausePlaceholders:(NSUInteger)count
{
  NSMutableArray<NSString *> *parts = [NSMutableArray arrayWithCapacity:count];
  for (NSUInteger i = 0; i < count; i++) {
    [parts addObject:@"?"];
  }
  return [parts componentsJoinedByString:@","];
}

@end
