#import <Foundation/Foundation.h>

#import "DBPhAssetRecord.h"

NS_ASSUME_NONNULL_BEGIN

@protocol DBPhAssetDiscoverySource <NSObject>

/**
 * When @c authorizedLocalIdentifiers is non-nil, only those assets are returned
 * (limited photo library). When nil, fetches all assets the app can access.
 */
- (NSArray<DBPhAssetRecord *> *)fetchAssetRecordsWithAuthorizedLocalIdentifiers:
    (nullable NSArray<NSString *> *)authorizedLocalIdentifiers;

@end

NS_ASSUME_NONNULL_END
