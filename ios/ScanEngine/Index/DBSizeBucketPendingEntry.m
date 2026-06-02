#import "DBSizeBucketPendingEntry.h"

#import "DBDiscoveredEntry.h"
#import "DBMediaTypeHint.h"
#import "DBMediaTypeHintResolver.h"

@implementation DBSizeBucketPendingEntry

- (instancetype)initWithFileEntryId:(NSInteger)fileEntryId
                             rootId:(NSInteger)rootId
                          uriOrPath:(NSString *)uriOrPath
                        displayName:(NSString *)displayName
                          sizeBytes:(int64_t)sizeBytes
                            mtimeNs:(int64_t)mtimeNs
                         generation:(NSInteger)generation
{
  self = [super init];
  if (self) {
    _fileEntryId = fileEntryId;
    _rootId = rootId;
    _uriOrPath = [uriOrPath copy];
    _displayName = [displayName copy];
    _sizeBytes = sizeBytes;
    _mtimeNs = mtimeNs;
    _generation = generation;
  }
  return self;
}

- (DBDiscoveredEntry *)toDiscoveredEntry
{
  DBMediaTypeHint hint = [DBMediaTypeHintResolver hintForFileName:_displayName];
  if ([hint isEqualToString:DBMediaTypeHintOther]) {
    hint = DBMediaTypeHintDocument;
  }
  NSURL *uri = [NSURL URLWithString:_uriOrPath];
  return [[DBDiscoveredEntry alloc] initWithContentUri:uri
                                            scanRootId:_rootId
                                           generation:_generation
                                           displayName:_displayName
                                        mediaTypeHint:hint
                                            sizeBytes:_sizeBytes
                                              mtimeNs:_mtimeNs];
}

@end
