#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Single egress point for opt-in crash SDK and analytics strings (FR-SE-02). */
@interface DBScanTelemetryEgress : NSObject

+ (BOOL)isEgressEnabled;
+ (void)setCrashAnalyticsOptIn:(BOOL)enabled;

+ (NSString *_Nullable)sanitizeCrashMessage:(NSString *_Nullable)message;
+ (NSString *_Nullable)sanitizeAnalyticsValue:(NSString *_Nullable)value;
+ (NSDictionary *)sanitizeCrashPayload:(NSDictionary *)payload;
+ (NSString *)sanitizeExceptionForTelemetry:(NSException *)exception;

/** Returns YES when opt-in is on and payload had allowlisted keys after filtering. */
+ (BOOL)recordCrashPayload:(NSDictionary *)payload;

#if DEBUG
+ (NSDictionary *)lastRecordedPayloadForTests;
#endif

@end

NS_ASSUME_NONNULL_END
