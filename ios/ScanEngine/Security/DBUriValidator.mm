#import "DBUriValidator.h"

NSString *const DBUnscannableReasonPermissionDenied = @"PERMISSION_DENIED";

@implementation DBScanRootGrant

- (instancetype)initWithUriGrant:(NSString *)uriGrant mode:(DBScanRootMode)mode
{
  self = [super init];
  if (self) {
    _uriGrant = [uriGrant copy];
    _mode = mode;
  }
  return self;
}

@end

@implementation DBUriValidator

- (DBUriValidationOutcome)validateLocalIdentifier:(NSString *)localIdentifier
                                    scanRootGrant:(DBScanRootGrant *)grant
                                      provenance:(DBUriProvenance)provenance
{
  if (provenance == DBUriProvenanceUserSupplied) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if (localIdentifier.length == 0) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if ([self localIdentifierHasTraversal:localIdentifier]) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if (![self isLocalIdentifierFormatAllowed:localIdentifier]) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if (grant.mode == DBScanRootModeUserSelected &&
      ![self isLocalIdentifierWithinGrant:localIdentifier grant:grant]) {
    return DBUriValidationOutcomePermissionDenied;
  }
  return DBUriValidationOutcomeAllowed;
}

- (DBUriValidationOutcome)validateFileURL:(NSURL *)fileURL
                           scanRootGrant:(DBScanRootGrant *)grant
                             provenance:(DBUriProvenance)provenance
{
  if (provenance == DBUriProvenanceUserSupplied) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if (fileURL == nil || fileURL.fileURL == NO) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if ([self fileURLHasTraversal:fileURL]) {
    return DBUriValidationOutcomePermissionDenied;
  }
  if (grant.mode == DBScanRootModeUserSelected &&
      ![self isFileURLWithinScopedGrant:fileURL grant:grant]) {
    return DBUriValidationOutcomePermissionDenied;
  }
  return DBUriValidationOutcomeAllowed;
}

#pragma mark - PHAsset localIdentifier

- (BOOL)localIdentifierHasTraversal:(NSString *)localIdentifier
{
  NSArray<NSString *> *parts = [localIdentifier componentsSeparatedByString:@"/"];
  for (NSString *segment in parts) {
    if ([segment isEqualToString:@".."] || [segment containsString:@".."]) {
      return YES;
    }
  }
  return NO;
}

/** UUID segments from platform fetch (no pasted arbitrary strings). */
- (BOOL)isLocalIdentifierFormatAllowed:(NSString *)localIdentifier
{
  static NSRegularExpression *regex;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    NSError *error = nil;
    regex = [NSRegularExpression regularExpressionWithPattern:@"^[A-F0-9\\-]+(/[A-F0-9\\-]+)*$"
                                                      options:NSRegularExpressionCaseInsensitive
                                                        error:&error];
  });
  if (regex == nil) {
    return NO;
  }
  NSUInteger matches =
      [regex numberOfMatchesInString:localIdentifier
                             options:0
                               range:NSMakeRange(0, localIdentifier.length)];
  return matches == 1;
}

- (BOOL)isLocalIdentifierWithinGrant:(NSString *)localIdentifier grant:(DBScanRootGrant *)grant
{
  if (grant.uriGrant.length == 0) {
    return NO;
  }
  return [localIdentifier hasPrefix:grant.uriGrant] || [grant.uriGrant isEqualToString:@"*"];
}

#pragma mark - Security-scoped file URLs

- (BOOL)fileURLHasTraversal:(NSURL *)fileURL
{
  NSArray<NSString *> *parts = fileURL.pathComponents;
  for (NSString *segment in parts) {
    if ([segment isEqualToString:@".."] || [segment containsString:@".."]) {
      return YES;
    }
  }
  return NO;
}

- (BOOL)isFileURLWithinScopedGrant:(NSURL *)fileURL grant:(DBScanRootGrant *)grant
{
  if (grant.uriGrant.length == 0) {
    return NO;
  }
  NSURL *grantURL = [NSURL URLWithString:grant.uriGrant];
  if (grantURL == nil) {
    return [fileURL.path hasPrefix:grant.uriGrant];
  }
  NSString *grantPath = grantURL.path;
  if (grantPath.length == 0) {
    return NO;
  }
  return [fileURL.path hasPrefix:grantPath];
}

@end
