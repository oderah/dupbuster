#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"
#import "DBPhAssetDiscoverySource.h"
#import "DBUriValidator.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^DBDiscoveryEntryHandler)(DBDiscoveredEntry *entry);
typedef BOOL (^DBDiscoveryCancelBlock)(void);

@interface DBDiscoveryResult : NSObject
@property (nonatomic, assign) NSInteger entriesEmitted;
@property (nonatomic, assign) NSInteger entriesDenied;
@property (nonatomic, assign) NSInteger directoriesVisited;
@property (nonatomic, assign) BOOL cancelled;
@end

/** Modes A (DocumentPicker folder) and B (PHAsset platform discovery). */
@interface DBDiscoveryEmitter : NSObject

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator;

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator
              phAssetDiscoverySource:(id<DBPhAssetDiscoverySource>)phAssetDiscoverySource;

/**
 * @param folderURL Security-scoped directory URL from DocumentPicker (file://).
 * @param grant Must be DBScanRootModeUserSelected; uriGrant prefixes folderURL.path.
 */
- (DBDiscoveryResult *)emitModeAWithFolderURL:(NSURL *)folderURL
                              scanRootGrant:(DBScanRootGrant *)grant
                                 scanRootId:(NSInteger)scanRootId
                                 generation:(NSInteger)generation
                                    handler:(DBDiscoveryEntryHandler)handler
                                  isCancelled:(nullable DBDiscoveryCancelBlock)isCancelled;

/**
 * Mode B — PHAsset fetch (full or limited-library identifiers) plus optional
 * security-scoped folder union from prior DocumentPicker grants.
 *
 * @param grant Must be `DBScanRootModePlatformDiscovery` (`uriGrant` is typically `*`).
 */
- (DBDiscoveryResult *)emitModeBWithScanRootGrant:(DBScanRootGrant *)grant
                                     scanRootId:(NSInteger)scanRootId
                                     generation:(NSInteger)generation
                    authorizedLocalIdentifiers:(nullable NSArray<NSString *> *)authorizedLocalIdentifiers
                  additionalScopedFolderURLs:(nullable NSArray<NSURL *> *)additionalScopedFolderURLs
                  additionalScopedGrants:(nullable NSArray<DBScanRootGrant *> *)additionalScopedGrants
                                       handler:(DBDiscoveryEntryHandler)handler
                                   isCancelled:(nullable DBDiscoveryCancelBlock)isCancelled;

@end

NS_ASSUME_NONNULL_END
