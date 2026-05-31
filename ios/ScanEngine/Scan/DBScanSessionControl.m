#import "DBScanSessionControl.h"

@implementation DBScanSessionControl

- (BOOL)isCancelled
{
  return self.cancelRequested;
}

- (void)awaitIfPaused
{
  while (self.paused && !self.cancelRequested) {
    [NSThread sleepForTimeInterval:0.02];
  }
}

@end
