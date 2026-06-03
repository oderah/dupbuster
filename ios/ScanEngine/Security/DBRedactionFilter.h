#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Denylist redaction before crash SDK, opt-in analytics, and JS bridge error egress (FR-SE-02). */
@interface DBRedactionFilter : NSObject

FOUNDATION_EXPORT NSString *const DBRedactionPlaceholder;

+ (NSString *)apply:(NSString *)text;
+ (NSString *_Nullable)applyNullable:(NSString *_Nullable)text;

+ (BOOL)containsDeniedContent:(NSString *)text;

/** Pattern 7 — keys redacted entirely in crash / analytics payloads. */
+ (NSSet<NSString *> *)crashSensitiveKeys;

+ (NSDictionary *)applyToCrashPayload:(NSDictionary *)payload;

@end

NS_ASSUME_NONNULL_END
