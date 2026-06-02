#import <Foundation/Foundation.h>

@class DBScanRootGrant;

NS_ASSUME_NONNULL_BEGIN

@protocol DBPlatformFileDeleter <NSObject>

- (BOOL)deleteURIString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant;

@end

/** Until M3-05 PHAsset delete — reports failure without mutating device storage. */
@interface DBPendingPlatformFileDeleter : NSObject <DBPlatformFileDeleter>
@end

NS_ASSUME_NONNULL_END
