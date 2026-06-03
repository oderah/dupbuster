#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Single egress point for opt-in crash SDK and analytics strings (FR-SE-02). */
@interface DBScanTelemetryEgress : NSObject

+ (NSString *_Nullable)sanitizeCrashMessage:(NSString *_Nullable)message;
+ (NSString *_Nullable)sanitizeAnalyticsValue:(NSString *_Nullable)value;
+ (NSDictionary *)sanitizeCrashPayload:(NSDictionary *)payload;
+ (NSString *)sanitizeExceptionForTelemetry:(NSException *)exception;

@end

NS_ASSUME_NONNULL_END
