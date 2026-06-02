#import "DBDeleteCoordinator.h"

#import "DBGrouper.h"
#import "DBIndexWriter.h"
#import "DBPlatformFileDeleter.h"
#import "DBUriValidator.h"

static NSString *const kDeleteCoordinatorDomain = @"com.dupbuster.delete.coordinator";

@implementation DBDeleteCoordinator {
  DBIndexWriter *_indexWriter;
  DBUriValidator *_uriValidator;
  id<DBPlatformFileDeleter> _platformFileDeleter;
  dispatch_queue_t _workQueue;
}

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
                       uriValidator:(DBUriValidator *)uriValidator
                 platformFileDeleter:(id<DBPlatformFileDeleter>)platformFileDeleter
                          workQueue:(dispatch_queue_t)workQueue
{
  self = [super init];
  if (self) {
    _indexWriter = indexWriter;
    _uriValidator = uriValidator;
    _platformFileDeleter = platformFileDeleter;
    _workQueue = workQueue ?: dispatch_queue_create("com.dupbuster.delete", DISPATCH_QUEUE_SERIAL);
  }
  return self;
}

- (void)deleteDuplicates:(DBDeleteDuplicatesCommand *)command
              completion:(void (^)(DBDeleteDuplicatesResult *, NSError *))completion
{
  dispatch_async(_workQueue, ^{
    NSError *error = nil;
    DBDeleteDuplicatesResult *result = [self runDelete:command error:&error];
    dispatch_async(dispatch_get_main_queue(), ^{
      completion(result, error);
    });
  });
}

- (DBDeleteDuplicatesResult *)runDelete:(DBDeleteDuplicatesCommand *)command error:(NSError **)error
{
  if (![self validateCommand:command error:error]) {
    return nil;
  }

  NSInteger deletedCount = 0;
  NSInteger failedCount = 0;
  NSMutableArray<NSNumber *> *platformDeletedIds = [NSMutableArray array];

  for (NSNumber *fileEntryIdNumber in command.deleteFileEntryIds) {
    NSInteger fileEntryId = fileEntryIdNumber.integerValue;
    NSString *uriString = nil;
    DBScanRootGrant *grant = nil;
    if (![_indexWriter loadDeleteTargetForFileEntryId:fileEntryId
                                            uriString:&uriString
                                                grant:&grant]) {
      failedCount++;
      continue;
    }

    DBUriValidationOutcome outcome = [self validateUri:uriString grant:grant];
    if (outcome != DBUriValidationOutcomeAllowed) {
      failedCount++;
      continue;
    }

    if ([_platformFileDeleter deleteURIString:uriString scanRootGrant:grant]) {
      [platformDeletedIds addObject:fileEntryIdNumber];
      deletedCount++;
    } else {
      failedCount++;
    }
  }

  if (platformDeletedIds.count > 0) {
    [_indexWriter applyDuplicateDeleteForGroupId:command.groupId
                               keeperFileEntryId:command.keeperFileEntryId
                            deletedFileEntryIds:platformDeletedIds
                                          error:nil];
  }

  DBDeleteDuplicatesResult *result = [[DBDeleteDuplicatesResult alloc] init];
  result.deletedCount = deletedCount;
  result.failedCount = failedCount;
  return result;
}

- (DBUriValidationOutcome)validateUri:(NSString *)uriString grant:(DBScanRootGrant *)grant
{
  if ([uriString hasPrefix:@"file://"]) {
    NSURL *url = [NSURL URLWithString:uriString];
    if (url == nil) {
      return DBUriValidationOutcomePermissionDenied;
    }
    return [_uriValidator validateFileURL:url scanRootGrant:grant provenance:DBUriProvenanceDiscovery];
  }
  return [_uriValidator validateLocalIdentifier:uriString
                                scanRootGrant:grant
                                  provenance:DBUriProvenanceDiscovery];
}

- (BOOL)validateCommand:(DBDeleteDuplicatesCommand *)command error:(NSError **)error
{
  NSSet<NSNumber *> *members = [_indexWriter memberFileEntryIdsForGroupId:command.groupId];
  if (members == nil) {
    [self fail:@"Unknown duplicate group" code:10 error:error];
    return NO;
  }
  if (![members containsObject:@(command.keeperFileEntryId)]) {
    [self fail:@"keeper is not a member of group" code:11 error:error];
    return NO;
  }
  for (NSNumber *fileEntryId in command.deleteFileEntryIds) {
    if (![members containsObject:fileEntryId]) {
      [self fail:@"delete target is not a member of group" code:12 error:error];
      return NO;
    }
  }
  return YES;
}

- (void)fail:(NSString *)message code:(NSInteger)code error:(NSError **)error
{
  if (error != NULL) {
    *error = [NSError errorWithDomain:kDeleteCoordinatorDomain
                                 code:code
                             userInfo:@{NSLocalizedDescriptionKey : message}];
  }
}

@end
