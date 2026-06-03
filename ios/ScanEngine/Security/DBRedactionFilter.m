#import "DBRedactionFilter.h"

NSString *const DBRedactionPlaceholder = @"[REDACTED]";

@implementation DBRedactionFilter

+ (NSArray<NSRegularExpression *> *)denylistPatterns
{
  static NSArray<NSRegularExpression *> *patterns;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    NSArray<NSString *> *expressions = @[
        // Pattern 1 — full path segment (AC-security-redact-01)
        @"(?i)(/storage/[^\\s\"']+|/sdcard/[^\\s\"']+|/data/user/[^\\s\"']+|/var/mobile/[^\\s\"']+|/private/var/[^\\s\"']+)",
        @"(?i)content://[^\\s\"']+",
        @"(?i)file://[^\\s\"']+",
        @"(?i)(ph://|assets-library://)[^\\s\"']+",
        @"(?i)([A-Za-z]:\\\\Users\\\\[^\\s\"']+|/Users/[^\\s\"']+|/home/[^\\s\"']+)",
        @"(?i)([/\\\\][^\\s\"']+\\.(?:jpg|jpeg|png|gif|webp|heic|heif|bmp|tiff|tif|mp4|mov|m4v|avi|mkv|webm|mp3|m4a|wav|aac|flac|pdf|txt|doc|docx|xls|xlsx|ppt|pptx))",
    ];
    NSMutableArray<NSRegularExpression *> *compiled = [NSMutableArray array];
    for (NSString *expression in expressions) {
      NSError *error = nil;
      NSRegularExpression *regex =
          [NSRegularExpression regularExpressionWithPattern:expression
                                                    options:0
                                                      error:&error];
      NSAssert(regex != nil && error == nil, @"Invalid redaction pattern: %@", expression);
      [compiled addObject:regex];
    }
    patterns = [compiled copy];
  });
  return patterns;
}

+ (NSSet<NSString *> *)crashSensitiveKeys
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[
      @"uri_or_path",
      @"display_name",
      @"path",
      @"paths",
      @"thumbnailUri",
      @"thumbnail_uri",
      @"alias_path",
      @"alias_paths",
    ]];
  });
  return keys;
}

+ (NSString *)apply:(NSString *)text
{
  return [self applyNullable:text] ?: @"";
}

+ (NSString *_Nullable)applyNullable:(NSString *_Nullable)text
{
  if (text.length == 0) {
    return text;
  }
  NSString *result = text;
  for (NSRegularExpression *pattern in [self denylistPatterns]) {
    result = [pattern stringByReplacingMatchesInString:result
                                               options:0
                                                 range:NSMakeRange(0, result.length)
                                          withTemplate:DBRedactionPlaceholder];
  }
  return result;
}

+ (BOOL)containsDeniedContent:(NSString *)text
{
  if (text.length == 0) {
    return NO;
  }
  for (NSRegularExpression *pattern in [self denylistPatterns]) {
    if ([pattern firstMatchInString:text options:0 range:NSMakeRange(0, text.length)] != nil) {
      return YES;
    }
  }
  return NO;
}

+ (NSDictionary *)applyToCrashPayload:(NSDictionary *)payload
{
  return [self redactStructuredValue:payload] ?: @{};
}

+ (id)redactStructuredValue:(id)value
{
  if (value == nil || value == [NSNull null]) {
    return value;
  }
  if ([value isKindOfClass:[NSString class]]) {
    return [self applyNullable:(NSString *)value];
  }
  if ([value isKindOfClass:[NSDictionary class]]) {
    NSDictionary *dictionary = (NSDictionary *)value;
    NSMutableDictionary *redacted = [NSMutableDictionary dictionaryWithCapacity:dictionary.count];
    NSSet<NSString *> *sensitiveKeys = [self crashSensitiveKeys];
    [dictionary enumerateKeysAndObjectsUsingBlock:^(NSString *key, id nested, BOOL *stop) {
      if ([sensitiveKeys containsObject:key] && nested != nil && nested != [NSNull null]) {
        redacted[key] = DBRedactionPlaceholder;
      } else {
        redacted[key] = [self redactStructuredValue:nested] ?: [NSNull null];
      }
    }];
    return [redacted copy];
  }
  if ([value isKindOfClass:[NSArray class]]) {
    NSArray *array = (NSArray *)value;
    NSMutableArray *redacted = [NSMutableArray arrayWithCapacity:array.count];
    for (id item in array) {
      [redacted addObject:[self redactStructuredValue:item] ?: [NSNull null]];
    }
    return [redacted copy];
  }
  return value;
}

@end
