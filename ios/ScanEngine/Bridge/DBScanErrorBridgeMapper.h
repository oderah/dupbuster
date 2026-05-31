#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT NSSet<NSString *> *DBScanErrorBridgeAllowedKeys(void);

/** Maps native scan errors to the RN `onScanError` payload (architecture §6 / bridge law). */
@interface DBScanErrorBridgeMapper : NSObject

+ (NSDictionary *)bridgePayloadWithFileEntryId:(NSInteger)fileEntryId
                             unscannableReason:(NSString *)unscannableReason
                                     scanRunId:(nullable NSNumber *)scanRunId;

+ (void)assertBridgeSafePayload:(NSDictionary *)payload;

@end

NS_ASSUME_NONNULL_END
