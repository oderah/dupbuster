#import "DBPlatformFileDeleter.h"

@implementation DBPendingPlatformFileDeleter

- (BOOL)deleteURIString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant
{
  (void)uriString;
  (void)grant;
  return NO;
}

@end
