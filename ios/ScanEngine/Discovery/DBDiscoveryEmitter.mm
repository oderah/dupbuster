#import "DBDiscoveryEmitter.h"

#import "DBMediaTypeHintResolver.h"

static const NSInteger kDiscoveryBatchSize = 32;

@implementation DBDiscoveryResult
@end

@interface DBDiscoveryEmitter ()
@property (nonatomic, strong) DBUriValidator *uriValidator;
@end

@implementation DBDiscoveryEmitter

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator
{
  self = [super init];
  if (self) {
    _uriValidator = uriValidator;
  }
  return self;
}

- (DBDiscoveryResult *)emitModeAWithFolderURL:(NSURL *)folderURL
                              scanRootGrant:(DBScanRootGrant *)grant
                                 scanRootId:(NSInteger)scanRootId
                                 generation:(NSInteger)generation
                                    handler:(DBDiscoveryEntryHandler)handler
                                  isCancelled:(DBDiscoveryCancelBlock)isCancelled
{
  DBDiscoveryResult *result = [[DBDiscoveryResult alloc] init];
  if (grant.mode != DBScanRootModeUserSelected) {
    return result;
  }
  if (folderURL == nil || !folderURL.fileURL) {
    return result;
  }

  BOOL accessed = [folderURL startAccessingSecurityScopedResource];
  @try {
    [self enumerateDirectoryAtURL:folderURL
                            grant:grant
                       scanRootId:scanRootId
                       generation:generation
                          handler:handler
                      isCancelled:isCancelled
                           result:result];
  } @finally {
    if (accessed) {
      [folderURL stopAccessingSecurityScopedResource];
    }
  }
  return result;
}

- (void)enumerateDirectoryAtURL:(NSURL *)directoryURL
                          grant:(DBScanRootGrant *)grant
                     scanRootId:(NSInteger)scanRootId
                     generation:(NSInteger)generation
                        handler:(DBDiscoveryEntryHandler)handler
                    isCancelled:(DBDiscoveryCancelBlock)isCancelled
                         result:(DBDiscoveryResult *)result
{
  if (isCancelled != nil && isCancelled()) {
    result.cancelled = YES;
    return;
  }

  result.directoriesVisited += 1;

  NSError *listError = nil;
  NSArray<NSURL *> *children =
      [[NSFileManager defaultManager] contentsOfDirectoryAtURL:directoryURL
                                    includingPropertiesForKeys:@[
                                      NSURLIsDirectoryKey,
                                      NSURLIsRegularFileKey,
                                      NSURLFileSizeKey,
                                      NSURLContentModificationDateKey,
                                      NSURLTypeIdentifierKey,
                                    ]
                                                       options:
                                                           NSDirectoryEnumerationSkipsPackageDescendants
                                                         error:&listError];
  if (children == nil) {
    return;
  }

  NSInteger batchCount = 0;
  for (NSURL *childURL in children) {
    if (isCancelled != nil && isCancelled()) {
      result.cancelled = YES;
      return;
    }

    NSNumber *isDirectory = nil;
    [childURL getResourceValue:&isDirectory forKey:NSURLIsDirectoryKey error:nil];
    if (isDirectory.boolValue) {
      [self enumerateDirectoryAtURL:childURL
                              grant:grant
                         scanRootId:scanRootId
                         generation:generation
                            handler:handler
                        isCancelled:isCancelled
                             result:result];
      if (result.cancelled) {
        return;
      }
      continue;
    }

    NSNumber *isRegularFile = nil;
    [childURL getResourceValue:&isRegularFile forKey:NSURLIsRegularFileKey error:nil];
    if (isRegularFile != nil && !isRegularFile.boolValue) {
      continue;
    }

    DBUriValidationOutcome outcome =
        [self.uriValidator validateFileURL:childURL
                             scanRootGrant:grant
                               provenance:DBUriProvenanceDiscovery];
    if (outcome != DBUriValidationOutcomeAllowed) {
      result.entriesDenied += 1;
      continue;
    }

    NSNumber *fileSize = nil;
    NSDate *modified = nil;
    NSString *typeId = nil;
    [childURL getResourceValue:&fileSize forKey:NSURLFileSizeKey error:nil];
    [childURL getResourceValue:&modified forKey:NSURLContentModificationDateKey error:nil];
    [childURL getResourceValue:&typeId forKey:NSURLTypeIdentifierKey error:nil];

    DBMediaTypeHint hint = [DBMediaTypeHintResolver hintForUniformTypeIdentifier:typeId];
    if ([hint isEqualToString:DBMediaTypeHintOther]) {
      hint = [DBMediaTypeHintResolver hintForFileName:childURL.lastPathComponent];
    }

    int64_t mtimeNs = 0;
    if (modified != nil) {
      mtimeNs = (int64_t)(modified.timeIntervalSince1970 * 1e9);
    }

    DBDiscoveredEntry *entry =
        [[DBDiscoveredEntry alloc] initWithContentURL:childURL
                                          scanRootId:scanRootId
                                          generation:generation
                                         displayName:childURL.lastPathComponent
                                       mediaTypeHint:hint
                                           sizeBytes:fileSize.longLongValue
                                             mtimeNs:mtimeNs];
    handler(entry);
    result.entriesEmitted += 1;
    batchCount += 1;
    if (batchCount >= kDiscoveryBatchSize) {
      batchCount = 0;
      [NSThread sleepForTimeInterval:0];
    }
  }
}

@end
