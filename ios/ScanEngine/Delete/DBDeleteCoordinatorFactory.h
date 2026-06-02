#import <Foundation/Foundation.h>

@class DBDeleteCoordinator;

NS_ASSUME_NONNULL_BEGIN

@interface DBDeleteCoordinatorFactory : NSObject

+ (DBDeleteCoordinator *)createCoordinator;

@end

NS_ASSUME_NONNULL_END
