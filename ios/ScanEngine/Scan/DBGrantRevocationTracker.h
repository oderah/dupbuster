#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Detects grant-level permission loss mid-scan (FR-SI-02 / AC-integrity-perm-01). */
@interface DBGrantRevocationTracker : NSObject

- (void)markSuccessfulAccessForScanRootId:(NSInteger)scanRootId;
- (BOOL)isGrantRevocationForScanRootId:(NSInteger)scanRootId
                     unscannableReason:(NSString *)unscannableReason;
- (void)reset;

@end

NS_ASSUME_NONNULL_END
