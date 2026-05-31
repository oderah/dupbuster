#import <Foundation/Foundation.h>

#import "DBScanProgressSnapshot.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^DBScanProgressBridgeEmitBlock)(NSDictionary *payload);

/** Throttled `onScanProgress` emitter for the scan orchestrator (M1-16). */
@interface DBScanProgressBridge : NSObject

- (instancetype)initWithEmitBlock:(DBScanProgressBridgeEmitBlock)emitBlock;

- (void)reportSnapshot:(DBScanProgressSnapshot *)snapshot atMs:(int64_t)atMs;
- (void)advanceToMs:(int64_t)atMs;
- (void)flushAtMs:(int64_t)atMs;
- (void)reset;

- (void)reportHashingProgressWithFilesProcessed:(NSInteger)filesProcessed
                               filesTotalKnown:(nullable NSNumber *)filesTotalKnown
                                   groupsFound:(NSInteger)groupsFound
                           reclaimableBytesEst:(int64_t)reclaimableBytesEst
                        isVideoFingerprintPass:(BOOL)isVideoFingerprintPass
                                          atMs:(int64_t)atMs;

@end

NS_ASSUME_NONNULL_END
