#import <Foundation/Foundation.h>

#import "DBIndexWriter.h"
#import "DBScanStartRequest.h"

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT NSString *const DBPlatformDiscoveryMarkerUri;

@interface DBScanRootResolver : NSObject

+ (DBScanRootResolverPlan *)resolveRequest:(DBScanStartRequest *)request
                               indexWriter:(DBIndexWriter *)indexWriter;

@end

NS_ASSUME_NONNULL_END
