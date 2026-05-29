#import "DBCatalogSchema.h"

const NSInteger DBCatalogSchemaCurrentVersion = 2;
NSString *const DBCatalogSchemaDatabaseName = @"dupbuster_catalog.db";
NSString *const DBCatalogHashAlgoSha256 = @"SHA256";
NSString *const DBCatalogMetaSchemaVersion = @"schema_version";
NSString *const DBCatalogMetaFullRescanRequired = @"full_rescan_required";

@implementation DBCatalogSchema

+ (NSArray<NSString *> *)createTableStatements
{
  return @[
    @"CREATE TABLE IF NOT EXISTS scan_root ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"uri_or_grant TEXT NOT NULL,"
    @"mode TEXT NOT NULL,"
    @"platform_reason TEXT,"
    @"created_at INTEGER NOT NULL)",
    @"CREATE TABLE IF NOT EXISTS fingerprint ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"hash_algo TEXT NOT NULL,"
    @"hash_value TEXT NOT NULL,"
    @"normalization_profile TEXT NOT NULL,"
    @"computed_at INTEGER NOT NULL,"
    @"frame_hashes_blob BLOB,"
    @"UNIQUE(hash_algo, hash_value, normalization_profile))",
    @"CREATE TABLE IF NOT EXISTS file_entry ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"root_id INTEGER NOT NULL REFERENCES scan_root(id),"
    @"uri_or_path TEXT NOT NULL,"
    @"display_name TEXT NOT NULL,"
    @"size INTEGER NOT NULL,"
    @"mtime_ns INTEGER NOT NULL,"
    @"fingerprint_id INTEGER REFERENCES fingerprint(id),"
    @"last_seen_generation INTEGER NOT NULL,"
    @"is_symlink INTEGER NOT NULL DEFAULT 0,"
    @"unscannable_reason TEXT,"
    @"inode INTEGER,"
    @"device_id INTEGER,"
    @"duration_ms INTEGER,"
    @"video_width INTEGER,"
    @"video_height INTEGER,"
    @"raw_content_fingerprint_id INTEGER REFERENCES fingerprint(id),"
    @"UNIQUE(root_id, uri_or_path))",
    @"CREATE TABLE IF NOT EXISTS file_path ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"file_entry_id INTEGER NOT NULL REFERENCES file_entry(id),"
    @"alias_path TEXT NOT NULL,"
    @"UNIQUE(file_entry_id, alias_path))",
    @"CREATE TABLE IF NOT EXISTS duplicate_group ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"fingerprint_id INTEGER NOT NULL REFERENCES fingerprint(id),"
    @"member_count INTEGER NOT NULL DEFAULT 0,"
    @"reclaimable_bytes_est INTEGER NOT NULL DEFAULT 0,"
    @"match_kind TEXT NOT NULL,"
    @"confidence_score REAL)",
    @"CREATE TABLE IF NOT EXISTS duplicate_member ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"group_id INTEGER NOT NULL REFERENCES duplicate_group(id),"
    @"file_entry_id INTEGER NOT NULL REFERENCES file_entry(id),"
    @"is_keeper INTEGER NOT NULL DEFAULT 0,"
    @"UNIQUE(group_id, file_entry_id))",
    @"CREATE TABLE IF NOT EXISTS scan_run ("
    @"id INTEGER PRIMARY KEY AUTOINCREMENT,"
    @"root_id INTEGER REFERENCES scan_root(id),"
    @"generation INTEGER NOT NULL,"
    @"status TEXT NOT NULL,"
    @"last_processed_id INTEGER NOT NULL DEFAULT 0,"
    @"started_at INTEGER NOT NULL,"
    @"ended_at INTEGER,"
    @"teardown_reason TEXT)",
    @"CREATE TABLE IF NOT EXISTS meta ("
    @"key TEXT PRIMARY KEY,"
    @"value TEXT NOT NULL)",
  ];
}

+ (NSArray<NSString *> *)createIndexStatements
{
  return @[
    @"CREATE INDEX IF NOT EXISTS idx_file_entry_size ON file_entry(size)",
    @"CREATE INDEX IF NOT EXISTS idx_file_entry_inode_device ON file_entry(inode, device_id)",
    @"CREATE INDEX IF NOT EXISTS idx_file_entry_generation ON file_entry(root_id, last_seen_generation)",
    @"CREATE INDEX IF NOT EXISTS idx_file_entry_fingerprint ON file_entry(fingerprint_id)",
  ];
}

@end
