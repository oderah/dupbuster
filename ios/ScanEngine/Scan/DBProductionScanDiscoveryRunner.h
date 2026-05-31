#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"
#import "DBDiscoveryEmitter.h"
#import "DBScanStartRequest.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^DBScanDiscoveryEntryHandler)(DBDiscoveredEntry *entry);
typedef BOOL (^DBScanDiscoveryCancelBlock)(void);

@protocol DBScanDiscoveryRunning <NSObject>

- (DBDiscoveryResult *)discoverWithRequest:(DBScanStartRequest *)request
                                      plan:(DBScanRootResolverPlan *)plan
                                generation:(NSInteger)generation
                                   handler:(DBScanDiscoveryEntryHandler)handler
                               isCancelled:(DBScanDiscoveryCancelBlock)isCancelled;

@end

@interface DBProductionScanDiscoveryRunner : NSObject <DBScanDiscoveryRunning>

- (instancetype)initWithDiscoveryEmitter:(DBDiscoveryEmitter *)discoveryEmitter;

@end

NS_ASSUME_NONNULL_END
