#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Closed unscannable reason codes (architecture §5 / FR-UN-02). */
FOUNDATION_EXPORT NSString *const DBUnscannableReasonPermissionDenied;
FOUNDATION_EXPORT NSString *const DBUnscannableReasonLargeSkipped;
FOUNDATION_EXPORT NSString *const DBUnscannableReasonHashTimeout;
FOUNDATION_EXPORT NSString *const DBUnscannableReasonVideoDecodeFailed;
FOUNDATION_EXPORT NSString *const DBUnscannableReasonImageDecodeFailed;

NS_ASSUME_NONNULL_END
