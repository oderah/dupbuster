#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"

NS_ASSUME_NONNULL_BEGIN

/** Discovery row plus fresh stat for the hash pipeline (FR-SI-01). */
@interface DBStagedFile : NSObject

@property (nonatomic, strong) DBDiscoveredEntry *discovered;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, assign) int64_t mtimeNs;
@property (nonatomic, strong, nullable) NSNumber *inode;
@property (nonatomic, strong, nullable) NSNumber *deviceId;
@property (nonatomic, assign) BOOL isSymlink;
@property (nonatomic, copy) DBMediaTypeHint mediaTypeHint;

- (instancetype)initWithDiscovered:(DBDiscoveredEntry *)discovered
                         fileStat:(DBFileStat *)fileStat;

@end

NS_ASSUME_NONNULL_END
