#import <Foundation/Foundation.h>

#import "DBDeleteDuplicatesCommand.h"

@class DBIndexWriter;
@class DBUriValidator;
@protocol DBPlatformFileDeleter;

NS_ASSUME_NONNULL_BEGIN

@interface DBDeleteCoordinator : NSObject

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
                       uriValidator:(DBUriValidator *)uriValidator
                 platformFileDeleter:(id<DBPlatformFileDeleter>)platformFileDeleter
                          workQueue:(nullable dispatch_queue_t)workQueue;

- (void)deleteDuplicates:(DBDeleteDuplicatesCommand *)command
              completion:(void (^)(DBDeleteDuplicatesResult *result, NSError *_Nullable error))completion;

/** Synchronous path for unit tests. */
- (DBDeleteDuplicatesResult *)runDelete:(DBDeleteDuplicatesCommand *)command error:(NSError **)error;

@end

NS_ASSUME_NONNULL_END
