#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"
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

/**
 * Mode A — user-selected folder via DocumentPicker / security-scoped bookmark.
 * Mode B (PHAsset) lands in M1-05.
 */
@interface DBDiscoveryEmitter : NSObject

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator;

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

@end

NS_ASSUME_NONNULL_END
