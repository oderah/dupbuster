#import "DBCatalogDatabase.h"

#import "DBCatalogMigrator.h"
#import "DBCatalogSchema.h"

static DBCatalogDatabase *_sharedDatabase;

@implementation DBCatalogDatabase {
  NSString *_path;
}

+ (instancetype)sharedDatabase
{
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    NSString *dir = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES).firstObject;
    NSString *path = [dir stringByAppendingPathComponent:DBCatalogSchemaDatabaseName];
    _sharedDatabase = [[DBCatalogDatabase alloc] initWithPath:path];
    NSError *error = nil;
    if (![_sharedDatabase openWithError:&error]) {
      NSLog(@"DupBuster catalog open failed: %@", error);
    }
  });
  return _sharedDatabase;
}

+ (instancetype)inMemoryDatabase
{
  return [[DBCatalogDatabase alloc] initWithPath:@":memory:"];
}

- (instancetype)initWithPath:(NSString *)path
{
  self = [super init];
  if (self) {
    _path = [path copy];
  }
  return self;
}

- (BOOL)openWithError:(NSError **)error
{
  if (_db != NULL) {
    return YES;
  }
  int rc = sqlite3_open(_path.UTF8String, &_db);
  if (rc != SQLITE_OK) {
    if (error) {
      *error = [NSError errorWithDomain:@"DBCatalogDatabase"
                                   code:rc
                               userInfo:@{NSLocalizedDescriptionKey : @(sqlite3_errmsg(_db)).stringValue}];
    }
    sqlite3_close(_db);
    _db = NULL;
    return NO;
  }
  char *errMsg = NULL;
  sqlite3_exec(_db, "PRAGMA foreign_keys=ON", NULL, NULL, &errMsg);
  if (errMsg) {
    sqlite3_free(errMsg);
  }
  if (![self ensureCurrentSchemaWithError:error]) {
    return NO;
  }
  return YES;
}

- (void)close
{
  if (_db) {
    sqlite3_close(_db);
    _db = NULL;
  }
}

- (BOOL)ensureCurrentSchemaWithError:(NSError **)error
{
  for (NSString *sql in [DBCatalogSchema createTableStatements]) {
    if (![self execSQL:sql error:error]) {
      return NO;
    }
  }
  for (NSString *sql in [DBCatalogSchema createIndexStatements]) {
    if (![self execSQL:sql error:error]) {
      return NO;
    }
  }
  NSInteger schemaVersion = [self readSchemaVersion];
  if (schemaVersion == 0) {
    NSString *version =
        [NSString stringWithFormat:@"%ld", (long)DBCatalogSchemaCurrentVersion];
    NSString *sql = [NSString stringWithFormat:
                                  @"INSERT OR REPLACE INTO meta (key, value) VALUES ('%@', '%@')",
                                  DBCatalogMetaSchemaVersion,
                                  version];
    if (![self execSQL:sql error:error]) {
      return NO;
    }
    if (![self execSQL:
                   @"INSERT OR REPLACE INTO meta (key, value) VALUES ('full_rescan_required', '0')"
                   error:error]) {
      return NO;
    }
    return YES;
  }
  if (schemaVersion < DBCatalogSchemaCurrentVersion) {
    return [DBCatalogMigrator migrateDatabase:self
                                  fromVersion:schemaVersion
                                    toVersion:DBCatalogSchemaCurrentVersion
                                        error:error];
  }
  return YES;
}

- (NSInteger)readSchemaVersion
{
  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(_db, "SELECT value FROM meta WHERE key = ?", -1, &stmt, NULL);
  sqlite3_bind_text(stmt, 1, DBCatalogMetaSchemaVersion.UTF8String, -1, SQLITE_TRANSIENT);
  NSInteger version = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    version = [@(sqlite3_column_text(stmt, 0)).stringValue integerValue];
  }
  sqlite3_finalize(stmt);
  return version;
}

- (BOOL)execSQL:(NSString *)sql error:(NSError **)error
{
  return [self execSQL:sql bind:nil error:error];
}

- (BOOL)execSQL:(NSString *)sql bind:(NSArray *)bindValues error:(NSError **)error
{
  sqlite3_stmt *stmt = NULL;
  int rc = sqlite3_prepare_v2(_db, sql.UTF8String, -1, &stmt, NULL);
  if (rc != SQLITE_OK) {
    if (error) {
      *error = [self errorForCode:rc];
    }
    return NO;
  }
  if (bindValues) {
    for (NSUInteger i = 0; i < bindValues.count; i++) {
      id value = bindValues[i];
      if ([value isKindOfClass:[NSString class]]) {
        sqlite3_bind_text(stmt, (int)i + 1, [(NSString *)value UTF8String], -1, SQLITE_TRANSIENT);
      } else if ([value isKindOfClass:[NSNumber class]]) {
        sqlite3_bind_int64(stmt, (int)i + 1, [(NSNumber *)value longLongValue]);
      }
    }
  }
  rc = sqlite3_step(stmt);
  sqlite3_finalize(stmt);
  if (rc != SQLITE_DONE && rc != SQLITE_ROW) {
    if (error) {
      *error = [self errorForCode:rc];
    }
    return NO;
  }
  return YES;
}

- (NSError *)errorForCode:(int)code
{
  return [NSError errorWithDomain:@"DBCatalogDatabase"
                             code:code
                         userInfo:@{NSLocalizedDescriptionKey : @(sqlite3_errmsg(_db)).stringValue}];
}

@end
