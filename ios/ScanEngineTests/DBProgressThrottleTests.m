#import <XCTest/XCTest.h>

#import "DBProgressThrottle.h"
#import "DBScanPhase.h"
#import "DBScanProgressContentKind.h"
#import "DBScanProgressSnapshot.h"

@interface DBProgressThrottleTests : XCTestCase
@end

@implementation DBProgressThrottleTests

- (void)testReport10kRapidUpdates_atMost4EventsPerSecond
{
  NSMutableArray<NSNumber *> *emitTimesMs = [NSMutableArray array];
  DBProgressThrottle *throttle =
      [[DBProgressThrottle alloc] initWithEmitBlock:^(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs) {
        (void)snapshot;
        [emitTimesMs addObject:@(emittedAtMs)];
      }];

  int64_t t = 0;
  for (NSInteger i = 0; i < 10000; i++) {
    [throttle reportSnapshot:[self progressWithFilesProcessed:i] atMs:t];
    [throttle advanceToMs:t];
    t += 1;
  }
  [throttle flushAtMs:t];

  XCTAssertGreaterThan(emitTimesMs.count, 0u);
  NSInteger maxPerSecond = [self maxEventsInAnyOneSecondWindow:emitTimesMs];
  XCTAssertLessThanOrEqual(maxPerSecond, DBProgressThrottleMaxEventsPerSecond);
}

- (void)testReport_coalescesToLatestWithinWindow
{
  NSMutableArray<NSNumber *> *processed = [NSMutableArray array];
  DBProgressThrottle *throttle =
      [[DBProgressThrottle alloc] initWithEmitBlock:^(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs) {
        (void)emittedAtMs;
        [processed addObject:@(snapshot.filesProcessed)];
      }];

  [throttle reportSnapshot:[self progressWithFilesProcessed:1] atMs:0];
  [throttle reportSnapshot:[self progressWithFilesProcessed:2] atMs:100];
  [throttle reportSnapshot:[self progressWithFilesProcessed:3] atMs:200];

  XCTAssertEqual(processed.count, 1u);
  XCTAssertEqual(processed[0].integerValue, 1);

  [throttle advanceToMs:250];

  XCTAssertEqual(processed.count, 2u);
  XCTAssertEqual(processed[1].integerValue, 3);
}

- (void)testReport_hashingVideoContent_coalescesContentKind
{
  NSMutableArray<NSString *> *kinds = [NSMutableArray array];
  DBProgressThrottle *throttle =
      [[DBProgressThrottle alloc] initWithEmitBlock:^(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs) {
        (void)emittedAtMs;
        [kinds addObject:snapshot.contentKind ?: @""];
      }];

  [throttle reportSnapshot:[self progressWithFilesProcessed:1
                                                      phase:DBScanPhaseHashing
                                                contentKind:DBScanProgressContentKindVideoContent]
                      atMs:0];
  [throttle reportSnapshot:[self progressWithFilesProcessed:2
                                                      phase:DBScanPhaseHashing
                                                contentKind:DBScanProgressContentKindNone]
                      atMs:100];
  [throttle advanceToMs:250];

  XCTAssertEqual(kinds.count, 2u);
  XCTAssertEqualObjects(kinds[0], DBScanProgressContentKindVideoContent);
  XCTAssertEqualObjects(kinds[1], DBScanProgressContentKindNone);
}

- (void)testFlush_emitsLatestPendingBeforeWindowElapses
{
  NSMutableArray<NSNumber *> *processed = [NSMutableArray array];
  DBProgressThrottle *throttle =
      [[DBProgressThrottle alloc] initWithEmitBlock:^(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs) {
        (void)emittedAtMs;
        [processed addObject:@(snapshot.filesProcessed)];
      }];

  [throttle reportSnapshot:[self progressWithFilesProcessed:1] atMs:0];
  [throttle reportSnapshot:[self progressWithFilesProcessed:9] atMs:50];
  [throttle flushAtMs:50];

  XCTAssertEqual(processed.count, 2u);
  XCTAssertEqual(processed[0].integerValue, 1);
  XCTAssertEqual(processed[1].integerValue, 9);
}

- (DBScanProgressSnapshot *)progressWithFilesProcessed:(NSInteger)filesProcessed
{
  return [self progressWithFilesProcessed:filesProcessed
                                    phase:DBScanPhaseDiscovering
                              contentKind:nil];
}

- (DBScanProgressSnapshot *)progressWithFilesProcessed:(NSInteger)filesProcessed
                                                 phase:(NSString *)phase
                                           contentKind:(NSString *)contentKind
{
  return [[DBScanProgressSnapshot alloc] initWithFilesProcessed:filesProcessed
                                                filesTotalKnown:@10000
                                                    groupsFound:0
                                            reclaimableBytesEst:0
                                                          phase:phase
                                                    contentKind:contentKind];
}

- (NSInteger)maxEventsInAnyOneSecondWindow:(NSArray<NSNumber *> *)emitTimesMs
{
  if (emitTimesMs.count == 0) {
    return 0;
  }
  NSArray<NSNumber *> *sorted =
      [emitTimesMs sortedArrayUsingComparator:^NSComparisonResult(NSNumber *a, NSNumber *b) {
        return [a compare:b];
      }];
  NSInteger maxInWindow = 0;
  NSInteger start = 0;
  for (NSInteger end = 0; end < (NSInteger)sorted.count; end++) {
    while (sorted[end].longLongValue - sorted[start].longLongValue >= 1000) {
      start++;
    }
    NSInteger count = end - start + 1;
    if (count > maxInWindow) {
      maxInWindow = count;
    }
  }
  return maxInWindow;
}

@end
