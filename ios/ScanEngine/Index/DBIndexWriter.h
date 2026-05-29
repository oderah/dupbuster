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

- (NSInteger)beginScanRunWithGeneration:(NSInteger)generation rootId:(NSInteger)rootId;

- (NSInteger)persistHashPipelineResult:(DBHashPipelineResult *)result
                                staged:(DBStagedFile *)staged
                            generation:(NSInteger)generation;

- (NSInteger)upsertHashedFile:(DBHashedFile *)hashed generation:(NSInteger)generation;

- (NSInteger)countIndexedFilesWithSize:(int64_t)sizeBytes;

- (NSInteger)purgeEntriesNotSeenInGeneration:(NSInteger)rootId generation:(NSInteger)generation;

- (NSInteger)fileEntryCount;

@end

NS_ASSUME_NONNULL_END
