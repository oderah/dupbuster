#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Closed allowlist for opt-in crash/analytics egress (architecture §8.2). */
@interface DBTelemetryAllowedFields : NSObject

+ (NSSet<NSString *> *)allowedKeys;
+ (NSSet<NSString *> *)bannedEgressKeys;
+ (NSDictionary *)filterToAllowedPayload:(NSDictionary *)payload;

@end

NS_ASSUME_NONNULL_END
