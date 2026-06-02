#import "DBGrantRevocationTracker.h"

#import "DBUnscannableReason.h"

@implementation DBGrantRevocationTracker {
  NSMutableSet<NSNumber *> *_successfulScanRootIds;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _successfulScanRootIds = [NSMutableSet set];
  }
  return self;
}

- (void)markSuccessfulAccessForScanRootId:(NSInteger)scanRootId
{
  [_successfulScanRootIds addObject:@(scanRootId)];
}

- (BOOL)isGrantRevocationForScanRootId:(NSInteger)scanRootId
                     unscannableReason:(NSString *)unscannableReason
{
  if (![unscannableReason isEqualToString:DBUnscannableReasonPermissionDenied]) {
    return NO;
  }
  return [_successfulScanRootIds containsObject:@(scanRootId)];
}

- (void)reset
{
  [_successfulScanRootIds removeAllObjects];
}

@end
