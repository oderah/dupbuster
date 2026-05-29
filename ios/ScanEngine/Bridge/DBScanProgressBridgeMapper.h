#import <Foundation/Foundation.h>

@class DBScanProgressSnapshot;

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT NSSet<NSString *> *DBScanProgressBridgeAllowedKeys(void);

@interface DBScanProgressBridgeMapper : NSObject

+ (NSDictionary *)bridgePayloadFromSnapshot:(DBScanProgressSnapshot *)snapshot;

+ (void)assertBridgeSafePayload:(NSDictionary *)payload;

@end

NS_ASSUME_NONNULL_END
