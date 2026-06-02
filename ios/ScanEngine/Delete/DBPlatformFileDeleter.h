#import <Foundation/Foundation.h>

@class DBScanRootGrant;

NS_ASSUME_NONNULL_BEGIN

@protocol DBPlatformFileDeleter <NSObject>

- (BOOL)deleteURIString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant;

@end

/** Test / headless stub — reports failure without mutating device storage. */
@interface DBPendingPlatformFileDeleter : NSObject <DBPlatformFileDeleter>
@end

NS_ASSUME_NONNULL_END
