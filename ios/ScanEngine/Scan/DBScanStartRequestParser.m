#import "DBScanStartRequestParser.h"

NSString *const DBScanStartRequestParserErrorDomain = @"com.dupbuster.scanengine.scan.startRequest";

@implementation DBScanStartRequestParser

+ (DBScanStartRequest *)parseDictionary:(NSDictionary *)options
{
  id modeValue = options[@"mode"];
  if (![modeValue isKindOfClass:[NSString class]]) {
    @throw [NSException exceptionWithName:NSInvalidArgumentException
                                   reason:@"startScan requires mode"
                                 userInfo:nil];
  }

  NSError *modeError = nil;
  DBScanRootMode mode = [self modeFromBridgeValue:(NSString *)modeValue error:&modeError];
  if (modeError != nil) {
    @throw [NSException exceptionWithName:NSInvalidArgumentException
                                   reason:modeError.localizedDescription
                                 userInfo:nil];
  }

  NSMutableArray<DBScanRootInput *> *roots = [NSMutableArray array];
  id rootsValue = options[@"roots"];
  if (rootsValue != nil && rootsValue != [NSNull null]) {
    if (![rootsValue isKindOfClass:[NSArray class]]) {
      @throw [NSException exceptionWithName:NSInvalidArgumentException
                                     reason:@"startScan roots must be an array"
                                   userInfo:nil];
    }
    NSArray *rootsArray = (NSArray *)rootsValue;
    for (NSUInteger index = 0; index < rootsArray.count; index++) {
      id rootValue = rootsArray[index];
      if (![rootValue isKindOfClass:[NSDictionary class]]) {
        @throw [NSException exceptionWithName:NSInvalidArgumentException
                                       reason:[NSString stringWithFormat:@"roots[%lu] must be an object", (unsigned long)index]
                                     userInfo:nil];
      }
      NSDictionary *rootMap = (NSDictionary *)rootValue;
      id uriGrantValue = rootMap[@"uriGrant"];
      if (![uriGrantValue isKindOfClass:[NSString class]]) {
        @throw [NSException exceptionWithName:NSInvalidArgumentException
                                       reason:[NSString stringWithFormat:@"roots[%lu].uriGrant is required", (unsigned long)index]
                                     userInfo:nil];
      }
      DBScanRootInput *rootInput = [[DBScanRootInput alloc] init];
      rootInput.uriGrant = (NSString *)uriGrantValue;
      id scanRootIdValue = rootMap[@"scanRootId"];
      if (scanRootIdValue != nil && scanRootIdValue != [NSNull null]) {
        rootInput.scanRootId = @([(NSNumber *)scanRootIdValue integerValue]);
      }
      [roots addObject:rootInput];
    }
  }

  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = mode;
  request.roots = [roots copy];

  id resumeValue = options[@"resumeScanRunId"];
  if (resumeValue != nil && resumeValue != [NSNull null]) {
    request.resumeScanRunId = @([(NSNumber *)resumeValue integerValue]);
  }
  return request;
}

+ (DBScanRootMode)modeFromBridgeValue:(NSString *)value error:(NSError **)error
{
  if ([value isEqualToString:DBScanRootModeBridgeUserSelected]) {
    return DBScanRootModeUserSelected;
  }
  if ([value isEqualToString:DBScanRootModeBridgePlatformDiscovery]) {
    return DBScanRootModePlatformDiscovery;
  }
  if (error != nil) {
    *error = [NSError errorWithDomain:DBScanStartRequestParserErrorDomain
                                 code:1
                             userInfo:@{NSLocalizedDescriptionKey : [NSString stringWithFormat:@"Unknown scan mode: %@", value]}];
  }
  return DBScanRootModeUserSelected;
}

@end
