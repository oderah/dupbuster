#import <Foundation/Foundation.h>

@class DBScanRunSnapshot;

NS_ASSUME_NONNULL_BEGIN

/** Maps interrupted scan metadata to bridge-safe payloads (no paths/hashes). */
@interface DBResumableScanRunBridgeMapper : NSObject

+ (NSDictionary *)bridgePayloadForSnapshot:(DBScanRunSnapshot *)snapshot;

@end

NS_ASSUME_NONNULL_END
