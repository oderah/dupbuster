#import "DBCatalogMigrator.h"

#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogSchema.h"
#import "DBMatchKind.h"

const NSInteger DBCatalogSchemaLegacyVersion = 1;

@implementation DBCatalogMigrator

+ (BOOL)migrateDatabase:(DBCatalogDatabase *)database
            fromVersion:(NSInteger)fromVersion
              toVersion:(NSInteger)toVersion
                  error:(NSError **)error
{
  if (fromVersion >= toVersion) {
    return YES;
  }
  NSInteger version = fromVersion;
  while (version < toVersion) {
    if (version == 1 && toVersion >= 2) {
      if (![self migrateV1ToV2OnDatabase:database error:error]) {
        return NO;
      }
      version = 2;
    } else {
      if (error) {
        *error = [NSError errorWithDomain:@"DBCatalogMigrator"
                                     code:1
                                 userInfo:@{
                                   NSLocalizedDescriptionKey :
                                       [NSString stringWithFormat:@"No migration from %ld to %ld",
                                                                  (long)version,
                                                                  (long)toVersion]
                                 }];
      }
      return NO;
    }
  }
  return YES;
}

+ (BOOL)migrateV1ToV2OnDatabase:(DBCatalogDatabase *)database error:(NSError **)error
{
  NSArray<NSString *> *statements = @[
    @"ALTER TABLE fingerprint ADD COLUMN frame_hashes_blob BLOB",
    @"ALTER TABLE file_entry ADD COLUMN duration_ms INTEGER",
    @"ALTER TABLE file_entry ADD COLUMN video_width INTEGER",
    @"ALTER TABLE file_entry ADD COLUMN video_height INTEGER",
    @"ALTER TABLE file_entry ADD COLUMN raw_content_fingerprint_id INTEGER REFERENCES fingerprint(id)",
    [NSString stringWithFormat:
                  @"ALTER TABLE duplicate_group ADD COLUMN match_kind TEXT NOT NULL DEFAULT '%@'",
                  DBMatchKindExactBytes],
    @"CREATE INDEX IF NOT EXISTS idx_file_entry_duration_ms ON file_entry(duration_ms)",
    @"DELETE FROM duplicate_member",
    @"DELETE FROM duplicate_group",
    [NSString stringWithFormat:
                  @"INSERT OR REPLACE INTO meta (key, value) VALUES ('%@', '%ld')",
                  DBCatalogMetaSchemaVersion,
                  (long)DBCatalogSchemaCurrentVersion],
    @"INSERT OR REPLACE INTO meta (key, value) VALUES ('full_rescan_required', '1')",
  ];
  for (NSString *sql in statements) {
    if (![database execSQL:sql error:error]) {
      return NO;
    }
  }
  return YES;
}

@end
