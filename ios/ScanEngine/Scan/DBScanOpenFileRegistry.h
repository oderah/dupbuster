#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Tracks open read handles so grant revocation can close in-flight FDs (architecture §6.4). */
@interface DBScanOpenFileRegistry : NSObject

- (void)registerHandle:(id)handle closeBlock:(dispatch_block_t)closeBlock;
- (void)unregisterHandle:(id)handle;
- (void)closeAll;

@end

NS_ASSUME_NONNULL_END
