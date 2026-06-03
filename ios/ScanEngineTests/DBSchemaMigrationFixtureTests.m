#import <XCTest/XCTest.h>
#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogSchema.h"
#import "DBIndexWriter.h"

static NSString *DBFixtureRootPath(void)
{
  NSString *dir = [NSFileManager defaultManager].currentDirectoryPath;
  while (dir.length > 0) {
    NSString *candidate =
        [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1/manifest.json"];
    if ([[NSFileManager defaultManager] fileExistsAtPath:candidate]) {
      return [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1"];
    }
    dir = [dir stringByDeletingLastPathComponent];
  }
  XCTFail(@"Could not locate tests/fixtures/dupbuster/v1");
  return @"";
}

static NSDictionary *DBLoadFixture(NSString *relativePath)
{
  NSString *path = [DBFixtureRootPath() stringByAppendingPathComponent:relativePath];
  NSData *data = [NSData dataWithContentsOfFile:path];
  XCTAssertNotNil(data, @"missing fixture %@", relativePath);
  id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
  XCTAssertTrue([json isKindOfClass:[NSDictionary class]]);
  return json;
}

@interface DBSchemaMigrationFixtureTests : XCTestCase
@end

@implementation DBSchemaMigrationFixtureTests

- (void)testIndexSchemaMigration01_matchesFixtureExpect
{
  NSDictionary *fixture = DBLoadFixture(@"index-schema-migration-01.json");
  XCTAssertEqualObjects(fixture[@"id"], @"index-schema-migration-01");
  XCTAssertEqualObjects(fixture[@"platform"], @"both");

  NSDictionary *input = fixture[@"input"];
  NSDictionary *expect = fixture[@"expect"];
  XCTAssertEqual([input[@"fromSchemaVersion"] intValue], 1);
  XCTAssertEqual([input[@"toSchemaVersion"] intValue], 2);

  NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:
                                         [NSString stringWithFormat:@"dupbuster_fixture_%@.db",
                                                                    NSUUID.UUID.UUIDString]];
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
  sqlite3_exec(
      rawDb,
      "INSERT INTO duplicate_group (fingerprint_id, member_count, reclaimable_bytes_est, confidence_score) "
      "VALUES (1, 2, 50, 1.0)",
      NULL,
      NULL,
      NULL);
  sqlite3_close(rawDb);

  DBCatalogDatabase *database = [[DBCatalogDatabase alloc] initWithPath:path];
  NSError *error = nil;
  XCTAssertTrue([database openWithError:&error], @"%@", error);
  DBIndexWriter *writer = [[DBIndexWriter alloc] initWithDatabase:database];
  DBCatalogMeta *meta = [writer readCatalogMeta];

  XCTAssertEqual(meta.schemaVersion, [expect[@"schemaVersion"] intValue]);
  XCTAssertEqual(meta.fullRescanRequired, [expect[@"fullRescanRequired"] boolValue]);

  if ([expect[@"duplicateGroupsCleared"] boolValue]) {
    sqlite3_stmt *stmt = NULL;
    sqlite3_prepare_v2(database.db, "SELECT COUNT(*) FROM duplicate_group", -1, &stmt, NULL);
    XCTAssertEqual(SQLITE_ROW, sqlite3_step(stmt));
    XCTAssertEqual(0, sqlite3_column_int(stmt, 0));
    sqlite3_finalize(stmt);
  }

  for (NSString *qualified in expect[@"columnsAdded"]) {
    NSArray<NSString *> *parts = [qualified componentsSeparatedByString:@"."];
    XCTAssertEqual(parts.count, 2U);
    NSString *table = parts[0];
    NSString *column = parts[1];
    sqlite3_stmt *stmt = NULL;
    NSString *pragma = [NSString stringWithFormat:@"PRAGMA table_info(%@)", table];
    sqlite3_prepare_v2(database.db, pragma.UTF8String, -1, &stmt, NULL);
    BOOL found = NO;
    while (sqlite3_step(stmt) == SQLITE_ROW) {
      if ([column isEqualToString:@(sqlite3_column_text(stmt, 1))]) {
        found = YES;
        break;
      }
    }
    sqlite3_finalize(stmt);
    XCTAssertTrue(found, @"missing column %@", qualified);
  }

  [database close];
  [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
}

@end
