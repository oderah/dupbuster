#import "DBSqliteDurationBucketIndex.h"

#import "DBDurationBucketIndex.h"
#import "DBIndexWriter.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBSqliteDurationBucketIndex : NSObject <DBDurationBucketIndexing>

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter;

@end

NS_ASSUME_NONNULL_END
