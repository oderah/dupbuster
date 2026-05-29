#import <Foundation/Foundation.h>

#import "DBSizeBucketIndex.h"

@class DBIndexWriter;

NS_ASSUME_NONNULL_BEGIN

@interface DBSqliteSizeBucketIndex : NSObject <DBSizeBucketIndexing>

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter;

@end

NS_ASSUME_NONNULL_END
