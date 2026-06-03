#import <Foundation/Foundation.h>

@class DBScanOrchestrator;

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS BGProcessingTask continuation hook (M4-03).
 * Foreground-primary: schedules continuation only for an in-flight active scan when the app backgrounds.
 */
@interface DBScanBackgroundContinuation : NSObject

+ (instancetype)shared;

/** Registers BGTaskScheduler handler and UIApplication lifecycle observers (idempotent). */
- (void)registerApplicationIntegration;

- (void)bindOrchestrator:(DBScanOrchestrator *)orchestrator;

- (void)notifyActiveScanRunId:(NSInteger)scanRunId;
- (void)notifyScanEnded;

/** Exposed for unit tests — 0 when no active scan is tracked. */
@property (nonatomic, readonly) NSInteger trackedActiveScanRunId;

/**
 * Submits a BGProcessingTaskRequest when an active scan is tracked.
 * Returns YES when submit was attempted and succeeded.
 */
- (BOOL)scheduleContinuationTaskIfNeeded;

@end

NS_ASSUME_NONNULL_END
