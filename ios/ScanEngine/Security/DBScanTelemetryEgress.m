#import "DBScanTelemetryEgress.h"

#import "DBRedactionFilter.h"

@implementation DBScanTelemetryEgress

+ (NSString *_Nullable)sanitizeCrashMessage:(NSString *_Nullable)message
{
  return [DBRedactionFilter applyNullable:message];
}

+ (NSString *_Nullable)sanitizeAnalyticsValue:(NSString *_Nullable)value
{
  return [DBRedactionFilter applyNullable:value];
}

+ (NSDictionary *)sanitizeCrashPayload:(NSDictionary *)payload
{
  return [DBRedactionFilter applyToCrashPayload:payload];
}

+ (NSString *)sanitizeExceptionForTelemetry:(NSException *)exception
{
  NSString *typeName = exception.name ?: @"Exception";
  NSString *message = [DBRedactionFilter applyNullable:exception.reason] ?: @"";
  if (message.length == 0) {
    return typeName;
  }
  return [NSString stringWithFormat:@"%@: %@", typeName, message];
}

@end
