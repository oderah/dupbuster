#import "DBScanTelemetryEgress.h"

#import "DBRedactionFilter.h"
#import "DBTelemetryAllowedFields.h"

static BOOL sCrashAnalyticsOptIn = NO;
static NSDictionary *sLastRecordedPayload = nil;

@implementation DBScanTelemetryEgress

+ (BOOL)isEgressEnabled
{
  return sCrashAnalyticsOptIn;
}

+ (void)setCrashAnalyticsOptIn:(BOOL)enabled
{
  sCrashAnalyticsOptIn = enabled;
  if (!enabled) {
    sLastRecordedPayload = @{};
  }
}

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

+ (BOOL)recordCrashPayload:(NSDictionary *)payload
{
  if (!sCrashAnalyticsOptIn) {
    return NO;
  }
  NSDictionary *allowed = [DBTelemetryAllowedFields filterToAllowedPayload:payload];
  if (allowed.count == 0) {
    return NO;
  }
  sLastRecordedPayload = [self sanitizeCrashPayload:allowed];
  return YES;
}

#if DEBUG
+ (NSDictionary *)lastRecordedPayloadForTests
{
  return sLastRecordedPayload ?: @{};
}
#endif

@end
