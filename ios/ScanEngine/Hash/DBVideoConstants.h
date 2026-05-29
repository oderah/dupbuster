#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT const int64_t DBVideoFingerprintTimeoutMs;
FOUNDATION_EXPORT const int64_t DBVideoFingerprintBudgetBytes;
FOUNDATION_EXPORT const int64_t DBVideoFingerprintBudgetLargeOptInBytes;
FOUNDATION_EXPORT const int64_t DBVideoMinDurationMultiFrameMs;
FOUNDATION_EXPORT const int DBVideoDHashMaxWidth;
FOUNDATION_EXPORT const int DBVideoDHashMaxHeight;
FOUNDATION_EXPORT const int DBVideoHammingThresholdDefault;
FOUNDATION_EXPORT const int DBVideoMinMatchingFramePairs;
FOUNDATION_EXPORT const int64_t DBVideoDurationGateMinMs;
FOUNDATION_EXPORT const double DBVideoDurationGateRatio;
FOUNDATION_EXPORT NSArray<NSNumber *> *DBVideoFrameSamplePositions(void);
FOUNDATION_EXPORT NSArray<NSNumber *> *DBVideoSingleFrameSamplePositions(void);

NS_ASSUME_NONNULL_END
