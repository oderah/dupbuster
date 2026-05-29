#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT NSString *const DBScanRunStatusRunning;
FOUNDATION_EXPORT NSString *const DBScanRunStatusPaused;
FOUNDATION_EXPORT NSString *const DBScanRunStatusCancelling;
FOUNDATION_EXPORT NSString *const DBScanRunStatusCancelled;
FOUNDATION_EXPORT NSString *const DBScanRunStatusComplete;
FOUNDATION_EXPORT NSString *const DBScanRunStatusError;

FOUNDATION_EXPORT NSArray<NSString *> *DBScanRunStatusActiveValues(void);
FOUNDATION_EXPORT NSArray<NSString *> *DBScanRunStatusResumableValues(void);

NS_ASSUME_NONNULL_END
