#import "DBScanBackgroundContinuation.h"

#import <BackgroundTasks/BackgroundTasks.h>
#import <UIKit/UIKit.h>

#import "DBScanBackgroundContinuationConstants.h"
#import "DBScanOrchestrator.h"

static const NSTimeInterval kBackgroundGracePollIntervalSec = 0.05;

@interface DBScanBackgroundContinuation ()
@property (nonatomic, weak) DBScanOrchestrator *orchestrator;
@property (nonatomic, assign) NSInteger trackedActiveScanRunId;
@property (nonatomic, assign) UIBackgroundTaskIdentifier backgroundGraceTaskId;
@property (nonatomic, assign) BOOL integrationRegistered;
@property (nonatomic, strong) dispatch_queue_t schedulerQueue;
@end

@implementation DBScanBackgroundContinuation

+ (instancetype)shared
{
  static DBScanBackgroundContinuation *instance;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    instance = [[DBScanBackgroundContinuation alloc] init];
  });
  return instance;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _trackedActiveScanRunId = 0;
    _backgroundGraceTaskId = UIBackgroundTaskInvalid;
    _schedulerQueue =
        dispatch_queue_create("com.dupbuster.scan.background-continuation", DISPATCH_QUEUE_SERIAL);
  }
  return self;
}

- (void)dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)registerApplicationIntegration
{
  if (self.integrationRegistered) {
    return;
  }
  self.integrationRegistered = YES;

  if (@available(iOS 13.0, *)) {
    [[BGTaskScheduler sharedScheduler]
        registerForTaskWithIdentifier:DBScanBackgroundContinuationTaskIdentifier
                           usingQueue:self.schedulerQueue
                        launchHandler:^(BGTask *task) {
                          [self handleBackgroundProcessingTask:task];
                        }];
  }

  NSNotificationCenter *center = NSNotificationCenter.defaultCenter;
  [center addObserver:self
             selector:@selector(handleApplicationDidEnterBackground)
                 name:UIApplicationDidEnterBackgroundNotification
               object:nil];
  [center addObserver:self
             selector:@selector(handleApplicationWillEnterForeground)
                 name:UIApplicationWillEnterForegroundNotification
               object:nil];
}

- (void)bindOrchestrator:(DBScanOrchestrator *)orchestrator
{
  self.orchestrator = orchestrator;
}

- (void)notifyActiveScanRunId:(NSInteger)scanRunId
{
  if (scanRunId <= 0) {
    return;
  }
  self.trackedActiveScanRunId = scanRunId;
}

- (void)notifyScanEnded
{
  self.trackedActiveScanRunId = 0;
  [self endBackgroundGraceTaskIfNeeded];
}

- (BOOL)scheduleContinuationTaskIfNeeded
{
  if (self.trackedActiveScanRunId <= 0) {
    return NO;
  }
  if (@available(iOS 13.0, *)) {
    BGProcessingTaskRequest *request =
        [[BGProcessingTaskRequest alloc] initWithIdentifier:DBScanBackgroundContinuationTaskIdentifier];
    request.requiresNetworkConnectivity = NO;
    request.requiresExternalPower = NO;
    NSError *submitError = nil;
    BOOL submitted = [[BGTaskScheduler sharedScheduler] submitTaskRequest:request error:&submitError];
    return submitted && submitError == nil;
  }
  return NO;
}

#pragma mark - UIApplication lifecycle

- (void)handleApplicationDidEnterBackground
{
  if (self.trackedActiveScanRunId <= 0) {
    return;
  }
  [self beginBackgroundGraceTaskIfNeeded];
  [self scheduleContinuationTaskIfNeeded];
}

- (void)handleApplicationWillEnterForeground
{
  [self endBackgroundGraceTaskIfNeeded];
}

#pragma mark - Background grace (in-flight continuation)

- (void)beginBackgroundGraceTaskIfNeeded
{
  if (self.backgroundGraceTaskId != UIBackgroundTaskInvalid) {
    return;
  }
  UIApplication *application = UIApplication.sharedApplication;
  __weak typeof(self) weakSelf = self;
  self.backgroundGraceTaskId =
      [application beginBackgroundTaskWithName:@"com.dupbuster.scan.background-grace"
                             expirationHandler:^{
                               [weakSelf handleBackgroundGraceExpired];
                             }];
}

- (void)endBackgroundGraceTaskIfNeeded
{
  if (self.backgroundGraceTaskId == UIBackgroundTaskInvalid) {
    return;
  }
  UIApplication *application = UIApplication.sharedApplication;
  [application endBackgroundTask:self.backgroundGraceTaskId];
  self.backgroundGraceTaskId = UIBackgroundTaskInvalid;
}

- (void)handleBackgroundGraceExpired
{
  NSInteger scanRunId = self.trackedActiveScanRunId;
  if (scanRunId > 0) {
    NSError *pauseError = nil;
    [self.orchestrator pauseScanWithId:scanRunId error:&pauseError];
  }
  [self endBackgroundGraceTaskIfNeeded];
}

#pragma mark - BGProcessingTask

- (void)handleBackgroundProcessingTask:(BGTask *)task API_AVAILABLE(ios(13.0))
{
  BGProcessingTask *processingTask = (BGProcessingTask *)task;
  __weak typeof(self) weakSelf = self;
  processingTask.expirationHandler = ^{
    [weakSelf handleProcessingTaskExpired];
  };

  NSInteger scanRunId = self.trackedActiveScanRunId;
  if (scanRunId <= 0) {
    scanRunId = [self.orchestrator activeScanRunId];
  }
  if (scanRunId <= 0) {
    [processingTask setTaskCompletedWithSuccess:YES];
    return;
  }

  dispatch_async(dispatch_get_global_queue(QOS_CLASS_UTILITY, 0), ^{
    [self waitForScanPipelineToFinishOrExpire];
    [processingTask setTaskCompletedWithSuccess:YES];
  });
}

- (void)waitForScanPipelineToFinishOrExpire
{
  while (self.trackedActiveScanRunId > 0) {
    [NSThread sleepForTimeInterval:kBackgroundGracePollIntervalSec];
  }
}

- (void)handleProcessingTaskExpired
{
  NSInteger scanRunId = self.trackedActiveScanRunId;
  if (scanRunId <= 0) {
    return;
  }
  NSError *pauseError = nil;
  [self.orchestrator pauseScanWithId:scanRunId error:&pauseError];
}

@end
