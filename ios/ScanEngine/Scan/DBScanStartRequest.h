#import <Foundation/Foundation.h>

#import "DBUriValidator.h"

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT NSString *const DBScanRootModeBridgeUserSelected;
FOUNDATION_EXPORT NSString *const DBScanRootModeBridgePlatformDiscovery;

/** Parsed `startScan` options from the RN bridge (NativeScanEngine.ts). */
@interface DBScanRootInput : NSObject

@property (nonatomic, copy) NSString *uriGrant;
@property (nonatomic, strong, nullable) NSNumber *scanRootId;

@end

@interface DBScanStartRequest : NSObject

@property (nonatomic, assign) DBScanRootMode mode;
@property (nonatomic, copy) NSArray<DBScanRootInput *> *roots;
@property (nonatomic, strong, nullable) NSNumber *resumeScanRunId;
@property (nonatomic, assign) BOOL largeFilesOptIn;

@end

/** Active scan_root row + grant context for discovery/stat. */
@interface DBResolvedScanRoot : NSObject

@property (nonatomic, assign) NSInteger scanRootId;
@property (nonatomic, copy) NSString *uriGrant;
@property (nonatomic, assign) DBScanRootMode mode;

@end

@interface DBScanRootResolverPlan : NSObject

@property (nonatomic, assign) NSInteger primaryRootId;
@property (nonatomic, copy) NSArray<DBResolvedScanRoot *> *roots;
@property (nonatomic, strong, nullable) DBScanRootGrant *platformGrant;
@property (nonatomic, copy) NSArray<DBScanRootGrant *> *additionalScopedGrants;

@end

NS_ASSUME_NONNULL_END
