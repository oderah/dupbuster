#import "DBSqliteSizeBucketIndex.h"

#import "DBDiscoveredEntry.h"
#import "DBIndexWriter.h"

@implementation DBSqliteSizeBucketIndex {
  DBIndexWriter *_indexWriter;
}

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
{
  self = [super init];
  if (self) {
    _indexWriter = indexWriter;
  }
  return self;
}

- (DBSizeBucketDisposition)registerSizeBytes:(int64_t)sizeBytes
                               mediaTypeHint:(NSString *)mediaTypeHint
                                     isEmpty:(BOOL)isEmpty
{
  if (isEmpty || [mediaTypeHint isEqualToString:DBMediaTypeHintVideo]) {
    return DBSizeBucketDispositionNeedsHash;
  }
  NSInteger existing = [_indexWriter countIndexedFilesWithSize:sizeBytes];
  return existing == 0 ? DBSizeBucketDispositionUniqueSkip : DBSizeBucketDispositionNeedsHash;
}

@end
