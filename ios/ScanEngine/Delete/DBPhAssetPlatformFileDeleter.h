#import <Foundation/Foundation.h>

#import "DBPlatformFileDeleter.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Production iOS delete (M3-05): PHAsset via `PHAssetChangeRequest`, Mode A files via
 * `NSFileCoordinator` + security-scoped grant access.
 */
@interface DBPhAssetPlatformFileDeleter : NSObject <DBPlatformFileDeleter>
@end

NS_ASSUME_NONNULL_END
