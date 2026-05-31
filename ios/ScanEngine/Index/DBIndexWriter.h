#import <Foundation/Foundation.h>

#import "DBCatalogMeta.h"
#import "DBHashPipeline.h"

@class DBCatalogDatabase;
@class DBHashedFile;
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

- (NSInteger)nextGenerationForRootId:(NSInteger)rootId;

- (NSInteger)beginScanRunWithGeneration:(NSInteger)generation rootId:(NSInteger)rootId;

- (BOOL)updateScanRunCheckpoint:(NSInteger)scanRunId
                lastProcessedId:(int64_t)lastProcessedId
                          error:(NSError *_Nullable *_Nullable)error;

- (BOOL)completeScanRunWithId:(NSInteger)scanRunId error:(NSError *_Nullable *_Nullable)error;

- (NSInteger)upsertUnscannableWithReason:(NSString *)reason
                                  staged:(DBStagedFile *)staged
                              generation:(NSInteger)generation;

- (NSInteger)persistHashPipelineResult:(DBHashPipelineResult *)result
                                staged:(DBStagedFile *)staged
                            generation:(NSInteger)generation;

- (NSInteger)upsertHashedFile:(DBHashedFile *)hashed generation:(NSInteger)generation;

- (NSInteger)upsertVideoPartialHashed:(DBHashedFile *)rawBytes
                 videoUnscannableReason:(NSString *)videoUnscannableReason
                           generation:(NSInteger)generation;

- (NSInteger)countIndexedFilesWithSize:(int64_t)sizeBytes;
- (NSInteger)countVideosWithinDurationGate:(int64_t)durationMs;

- (NSInteger)purgeEntriesNotSeenInGeneration:(NSInteger)rootId generation:(NSInteger)generation;

- (NSInteger)fileEntryCount;

@end

NS_ASSUME_NONNULL_END
