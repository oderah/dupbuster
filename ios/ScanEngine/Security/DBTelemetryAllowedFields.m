#import "DBTelemetryAllowedFields.h"

@implementation DBTelemetryAllowedFields

+ (NSSet<NSString *> *)allowedKeys
{
  return [NSSet setWithArray:@[
    @"scan_run_id",
    @"phase",
    @"files_processed",
    @"groups_found",
    @"schema_version",
    @"teardown_reason",
    @"platform_api_level",
    @"exception_type",
  ]];
}

+ (NSSet<NSString *> *)bannedEgressKeys
{
  return [NSSet setWithArray:@[
    @"unscannable_reason",
    @"uri_or_path",
    @"display_name",
    @"path",
    @"paths",
    @"hash_value",
    @"hash_algo",
    @"frame_hashes_blob",
    @"normalization_profile",
    @"thumbnail_uri",
    @"thumbnailUri",
  ]];
}

+ (NSDictionary *)filterToAllowedPayload:(NSDictionary *)payload
{
  NSSet<NSString *> *allowed = [self allowedKeys];
  NSSet<NSString *> *banned = [self bannedEgressKeys];
  NSMutableDictionary *filtered = [NSMutableDictionary dictionary];
  for (NSString *key in payload) {
    if ([allowed containsObject:key] && ![banned containsObject:key]) {
      filtered[key] = payload[key];
    }
  }
  return filtered;
}

@end
