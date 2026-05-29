#import "DBScanRunStatus.h"

NSString *const DBScanRunStatusRunning = @"running";
NSString *const DBScanRunStatusPaused = @"paused";
NSString *const DBScanRunStatusCancelling = @"cancelling";
NSString *const DBScanRunStatusCancelled = @"cancelled";
NSString *const DBScanRunStatusComplete = @"complete";
NSString *const DBScanRunStatusError = @"error";

NSArray<NSString *> *DBScanRunStatusActiveValues(void)
{
  return @[
    DBScanRunStatusRunning,
    DBScanRunStatusPaused,
    DBScanRunStatusCancelling,
  ];
}

NSArray<NSString *> *DBScanRunStatusResumableValues(void)
{
  return @[DBScanRunStatusRunning, DBScanRunStatusPaused];
}
