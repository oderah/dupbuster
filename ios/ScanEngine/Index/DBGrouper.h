#import <Foundation/Foundation.h>

@class DBCatalogDatabase;

NS_ASSUME_NONNULL_BEGIN

@interface DBGrouperRebuildResult : NSObject
@property (nonatomic, assign) NSInteger groupsCreated;
@property (nonatomic, assign) int64_t totalReclaimableBytesEst;
@end

/** Builds duplicate_group / duplicate_member from hashed catalog rows (M1-10). */
@interface DBGrouper : NSObject

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database;

- (DBGrouperRebuildResult *)rebuildDuplicateGroups;

- (NSInteger)duplicateGroupCount;
- (NSInteger)memberCountForGroupId:(NSInteger)groupId;
- (int64_t)reclaimableBytesForGroupId:(NSInteger)groupId;
- (nullable NSString *)matchKindForGroupId:(NSInteger)groupId;
- (NSInteger)firstDuplicateGroupId;

+ (NSString *)matchKindForNormalizationProfile:(NSString *)profile;
+ (int64_t)estimateReclaimableBytesForSizes:(NSArray<NSNumber *> *)sizes;

@end

NS_ASSUME_NONNULL_END
