#import <Foundation/Foundation.h>

#import "DBScanStartRequest.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBScanStartRequestParser : NSObject

+ (DBScanStartRequest *)parseDictionary:(NSDictionary *)options;

+ (DBScanRootMode)modeFromBridgeValue:(NSString *)value error:(NSError *_Nullable *_Nullable)error;

@end

NS_ASSUME_NONNULL_END
