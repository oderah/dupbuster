#import "DBScanOpenFileRegistry.h"

@interface DBRegisteredHandle : NSObject
@property (nonatomic, copy) dispatch_block_t closeBlock;
@end

@implementation DBRegisteredHandle
@end

@implementation DBScanOpenFileRegistry {
  NSMapTable<id, DBRegisteredHandle *> *_handles;
  NSLock *_lock;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _handles = [NSMapTable strongToStrongObjectsMapTable];
    _lock = [[NSLock alloc] init];
  }
  return self;
}

- (void)registerHandle:(id)handle closeBlock:(dispatch_block_t)closeBlock
{
  DBRegisteredHandle *registered = [[DBRegisteredHandle alloc] init];
  registered.closeBlock = closeBlock;
  [_lock lock];
  [_handles setObject:registered forKey:handle];
  [_lock unlock];
}

- (void)unregisterHandle:(id)handle
{
  [_lock lock];
  [_handles removeObjectForKey:handle];
  [_lock unlock];
}

- (void)closeAll
{
  [_lock lock];
  NSMapTable *snapshot = [_handles copy];
  [_handles removeAllObjects];
  [_lock unlock];

  for (DBRegisteredHandle *registered in snapshot.objectEnumerator) {
    if (registered.closeBlock != nil) {
      registered.closeBlock();
    }
  }
}

@end
