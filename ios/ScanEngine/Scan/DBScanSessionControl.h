#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Cooperative cancel / pause gate checked by the orchestrator pipeline. */
@interface DBScanSessionControl : NSObject

@property (nonatomic, assign, getter=isCancelRequested) BOOL cancelRequested;
@property (nonatomic, assign, getter=isPaused) BOOL paused;

- (BOOL)isCancelled;
- (void)awaitIfPaused;

@end

NS_ASSUME_NONNULL_END
