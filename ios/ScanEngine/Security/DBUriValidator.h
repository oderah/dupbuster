#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Matches bridge / schema `scan_root.mode`. */
typedef NS_ENUM(NSInteger, DBScanRootMode) {
  DBScanRootModeUserSelected = 0,
  DBScanRootModePlatformDiscovery = 1,
};

typedef NS_ENUM(NSInteger, DBUriProvenance) {
  /** Active scan_root grant at scan start. */
  DBUriProvenanceGrantRoot = 0,
  /** Emitted by PHAsset / discovery for this run. */
  DBUriProvenanceDiscovery = 1,
  /** Untrusted pasted input — always fail-closed. */
  DBUriProvenanceUserSupplied = 2,
};

typedef NS_ENUM(NSInteger, DBUriValidationOutcome) {
  DBUriValidationOutcomeAllowed = 0,
  DBUriValidationOutcomePermissionDenied = 1,
};

/** Active scan_root grant context (architecture §8.1). */
@interface DBScanRootGrant : NSObject

@property (nonatomic, copy) NSString *uriGrant;
@property (nonatomic, assign) DBScanRootMode mode;

- (instancetype)initWithUriGrant:(NSString *)uriGrant mode:(DBScanRootMode)mode;

@end

/**
 * Mandatory gate before stat/open/hash (FR-SE-01).
 * PHAsset: localIdentifier from platform fetch only. Fail-closed → PERMISSION_DENIED.
 */
@interface DBUriValidator : NSObject

- (DBUriValidationOutcome)validateLocalIdentifier:(NSString *)localIdentifier
                                    scanRootGrant:(DBScanRootGrant *)grant
                                      provenance:(DBUriProvenance)provenance;

- (DBUriValidationOutcome)validateFileURL:(NSURL *)fileURL
                           scanRootGrant:(DBScanRootGrant *)grant
                             provenance:(DBUriProvenance)provenance;

@end

FOUNDATION_EXPORT NSString *const DBUnscannableReasonPermissionDenied;

NS_ASSUME_NONNULL_END
