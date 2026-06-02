#import <Foundation/Foundation.h>

@class DBDiscoveredEntry;

NS_ASSUME_NONNULL_BEGIN

/** Catalog row skipped by size-bucket elimination; backfilled on size collision (FR-FP-02). */
@interface DBSizeBucketPendingEntry : NSObject

@property (nonatomic, readonly) NSInteger fileEntryId;
@property (nonatomic, readonly) NSInteger rootId;
@property (nonatomic, readonly, copy) NSString *uriOrPath;
@property (nonatomic, readonly, copy) NSString *displayName;
@property (nonatomic, readonly) int64_t sizeBytes;
@property (nonatomic, readonly) int64_t mtimeNs;
@property (nonatomic, readonly) NSInteger generation;

- (instancetype)initWithFileEntryId:(NSInteger)fileEntryId
                             rootId:(NSInteger)rootId
                          uriOrPath:(NSString *)uriOrPath
                        displayName:(NSString *)displayName
                          sizeBytes:(int64_t)sizeBytes
                            mtimeNs:(int64_t)mtimeNs
                         generation:(NSInteger)generation;

- (DBDiscoveredEntry *)toDiscoveredEntry;

@end

NS_ASSUME_NONNULL_END
