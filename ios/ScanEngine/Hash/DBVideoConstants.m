#import "DBVideoConstants.h"

const int64_t DBVideoFingerprintTimeoutMs = 90000;
const int64_t DBVideoFingerprintBudgetBytes = 500LL * 1024 * 1024;
const int64_t DBVideoFingerprintBudgetLargeOptInBytes = 2LL * 1024 * 1024 * 1024;
const int64_t DBVideoMinDurationMultiFrameMs = 3000;
const int DBVideoDHashMaxWidth = 320;
const int DBVideoDHashMaxHeight = 180;
const int DBVideoHammingThresholdDefault = 8;
const int DBVideoMinMatchingFramePairs = 3;
const int64_t DBVideoDurationGateMinMs = 1000;
const double DBVideoDurationGateRatio = 0.02;

NSArray<NSNumber *> *DBVideoFrameSamplePositions(void)
{
  return @[@0.02, @0.25, @0.50, @0.75, @0.98];
}

NSArray<NSNumber *> *DBVideoSingleFrameSamplePositions(void)
{
  return @[@0.50];
}
