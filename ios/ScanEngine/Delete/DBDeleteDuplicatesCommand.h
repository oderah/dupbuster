#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBDeleteDuplicatesCommand : NSObject

@property (nonatomic, assign) NSInteger groupId;
@property (nonatomic, assign) NSInteger keeperFileEntryId;
@property (nonatomic, copy) NSArray<NSNumber *> *deleteFileEntryIds;

@end

@interface DBDeleteDuplicatesResult : NSObject

@property (nonatomic, assign) NSInteger deletedCount;
@property (nonatomic, assign) NSInteger failedCount;

@end

NS_ASSUME_NONNULL_END
