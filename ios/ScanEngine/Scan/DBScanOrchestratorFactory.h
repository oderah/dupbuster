#import <Foundation/Foundation.h>

#import "DBScanOrchestrator.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBScanOrchestratorFactory : NSObject

+ (DBScanOrchestrator *)createWithEmitProgress:(void (^)(NSDictionary *payload))emitProgress
                                     emitError:(void (^)(NSDictionary *payload))emitError;

@end

NS_ASSUME_NONNULL_END
