#import <XCTest/XCTest.h>
#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogSchema.h"
#import "DBIndexWriter.h"

@interface DBCatalogMigratorTests : XCTestCase
@end

@implementation DBCatalogMigratorTests

- (void)testOpenV1Database_migratesToV2AndSetsFullRescan
{
  NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:
                                         [NSString stringWithFormat:@"dupbuster_v1_%@.db", NSUUID.UUID.UUIDString]];
  sqlite3 *rawDb = NULL;
  XCTAssertEqual(SQLITE_OK, sqlite3_open(path.UTF8String, &rawDb));
  sqlite3_exec(rawDb, "PRAGMA foreign_keys=ON", NULL, NULL, NULL);
  for (NSString *sql in [DBCatalogSchema legacyV1CreateTableStatements]) {
    sqlite3_exec(rawDb, sql.UTF8String, NULL, NULL, NULL);
  }
  for (NSString *sql in [DBCatalogSchema legacyV1CreateIndexStatements]) {
    sqlite3_exec(rawDb, sql.UTF8String, NULL, NULL, NULL);
  }
  sqlite3_exec(
      rawDb,
      "INSERT OR REPLACE INTO meta (key, value) VALUES ('schema_version', '1')",
      NULL,
      NULL,
      NULL);
  sqlite3_exec(
      rawDb,
      "INSERT OR REPLACE INTO meta (key, value) VALUES ('full_rescan_required', '0')",
      NULL,
      NULL,
      NULL);
  sqlite3_close(rawDb);

  DBCatalogDatabase *database = [[DBCatalogDatabase alloc] initWithPath:path];
  NSError *error = nil;
  XCTAssertTrue([database openWithError:&error], @"%@", error);
  DBIndexWriter *writer = [[DBIndexWriter alloc] initWithDatabase:database];
  DBCatalogMeta *meta = [writer readCatalogMeta];
  XCTAssertEqual(meta.schemaVersion, DBCatalogSchemaCurrentVersion);
  XCTAssertTrue(meta.fullRescanRequired);

  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(database.db, "PRAGMA table_info(file_entry)", -1, &stmt, NULL);
  NSMutableSet<NSString *> *columns = [NSMutableSet set];
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    [columns addObject:@(sqlite3_column_text(stmt, 1))];
  }
  sqlite3_finalize(stmt);
  XCTAssertTrue([columns containsObject:@"duration_ms"]);
  XCTAssertTrue([columns containsObject:@"raw_content_fingerprint_id"]);

  [database close];
  [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
}

@end
