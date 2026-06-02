#import <Foundation/Foundation.h>

#import "DBCatalogMeta.h"
#import "DBHashPipeline.h"

@class DBCatalogDatabase;

#import "DBUriValidator.h"
@class DBHashedFile;
@class DBSizeBucketPendingEntry;
@class DBDiscoveredEntry;
@class DBStagedFile;

NS_ASSUME_NONNULL_BEGIN

@interface DBIndexWriter : NSObject

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database;

- (DBCatalogMeta *)readCatalogMeta;

- (NSInteger)insertScanRootWithUriOrGrant:(NSString *)uriOrGrant
                                     mode:(NSString *)mode
                           platformReason:(nullable NSString *)platformReason;

- (nullable NSNumber *)findScanRootIdForUriOrGrant:(NSString *)uriOrGrant;

- (NSInteger)findOrInsertScanRootWithUriOrGrant:(NSString *)uriOrGrant mode:(NSString *)mode;

/** Lookup indexed row for resume skip (architecture §6.3). */
- (NSInteger)fileEntryIdForDiscoveredEntry:(DBDiscoveredEntry *)entry;

- (NSInteger)nextGenerationForRootId:(NSInteger)rootId;

- (NSInteger)beginScanRunWithGeneration:(NSInteger)generation rootId:(NSInteger)rootId;

- (BOOL)updateScanRunCheckpoint:(NSInteger)scanRunId
                lastProcessedId:(int64_t)lastProcessedId
                          error:(NSError *_Nullable *_Nullable)error;

- (BOOL)completeScanRunWithId:(NSInteger)scanRunId error:(NSError *_Nullable *_Nullable)error;

- (NSInteger)upsertUnscannableWithReason:(NSString *)reason
                                  staged:(DBStagedFile *)staged
                              generation:(NSInteger)generation;

/** AC-integrity-toctou-02: file vanished mid-hash; do not bump last_seen_generation. */
- (NSInteger)tombstoneDeletedMidHashWithStaged:(DBStagedFile *)staged
                              currentGeneration:(NSInteger)currentGeneration;

- (NSInteger)persistHashPipelineResult:(DBHashPipelineResult *)result
                                staged:(DBStagedFile *)staged
                            generation:(NSInteger)generation;

- (NSInteger)upsertHashedFile:(DBHashedFile *)hashed generation:(NSInteger)generation;

- (NSInteger)upsertVideoPartialHashed:(DBHashedFile *)rawBytes
                 videoUnscannableReason:(NSString *)videoUnscannableReason
                           generation:(NSInteger)generation;

- (NSInteger)countIndexedFilesWithSize:(int64_t)sizeBytes;

- (NSArray<DBSizeBucketPendingEntry *> *)listSizeBucketPendingEntriesWithSizeBytes:(int64_t)sizeBytes
                                                                        generation:(NSInteger)generation
                                                                excludeFileEntryId:(NSInteger)excludeFileEntryId;

- (NSInteger)countVideosWithinDurationGate:(int64_t)durationMs;

- (NSInteger)purgeEntriesNotSeenInGeneration:(NSInteger)rootId generation:(NSInteger)generation;

- (NSInteger)fileEntryCount;

- (nullable NSSet<NSNumber *> *)memberFileEntryIdsForGroupId:(NSInteger)groupId;

- (BOOL)loadDeleteTargetForFileEntryId:(NSInteger)fileEntryId
                             uriString:(NSString *_Nullable *_Nonnull)uriString
                                 grant:(DBScanRootGrant *_Nullable *_Nonnull)grant;

- (BOOL)applyDuplicateDeleteForGroupId:(NSInteger)groupId
                     keeperFileEntryId:(NSInteger)keeperFileEntryId
                  deletedFileEntryIds:(NSArray<NSNumber *> *)deletedFileEntryIds
                                error:(NSError *_Nullable *_Nullable)error;

@end

NS_ASSUME_NONNULL_END
