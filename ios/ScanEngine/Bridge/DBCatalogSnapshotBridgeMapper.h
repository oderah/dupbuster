#import <Foundation/Foundation.h>

@class DBCatalogSnapshot;

NS_ASSUME_NONNULL_BEGIN

@interface DBCatalogSnapshotBridgeMapper : NSObject

+ (NSDictionary *)bridgePayloadForSnapshot:(DBCatalogSnapshot *)snapshot;
+ (void)assertBridgeSafePayload:(NSDictionary *)payload;

@end

NS_ASSUME_NONNULL_END
