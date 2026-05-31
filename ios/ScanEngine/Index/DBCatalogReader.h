#import <Foundation/Foundation.h>

@class DBCatalogDatabase;

NS_ASSUME_NONNULL_BEGIN

typedef NSString *DBCatalogMediaTypeHint NS_TYPED_ENUM;

@interface DBCatalogMember : NSObject
@property (nonatomic, assign) NSInteger fileEntryId;
@property (nonatomic, copy) NSString *displayName;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, assign) int64_t mtimeMs;
@property (nonatomic, assign) NSInteger pathLength;
@property (nonatomic, copy) DBCatalogMediaTypeHint mediaTypeHint;
@property (nonatomic, copy, nullable) NSString *thumbnailUri;
@end

@interface DBCatalogGroupSummary : NSObject
@property (nonatomic, assign) NSInteger groupId;
@property (nonatomic, copy) NSString *matchKind;
@property (nonatomic, assign) NSInteger memberCount;
@property (nonatomic, assign) int64_t reclaimableBytesEst;
@property (nonatomic, copy) NSArray<DBCatalogMember *> *thumbnails;
@end

@interface DBCatalogGroupDetail : NSObject
@property (nonatomic, assign) NSInteger groupId;
@property (nonatomic, copy) NSString *matchKind;
@property (nonatomic, assign) NSInteger memberCount;
@property (nonatomic, assign) int64_t reclaimableBytesEst;
@property (nonatomic, copy) NSArray<DBCatalogMember *> *members;
@end

@interface DBCatalogSnapshot : NSObject
@property (nonatomic, copy) NSArray<DBCatalogGroupSummary *> *duplicateGroups;
@property (nonatomic, copy) NSDictionary<NSString *, NSNumber *> *unscannableCounts;
@property (nonatomic, copy) NSDictionary<NSNumber *, DBCatalogGroupDetail *> *groupDetailsById;
@end

@interface DBCatalogReader : NSObject

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database NS_DESIGNATED_INITIALIZER;
- (instancetype)init NS_UNAVAILABLE;

- (DBCatalogSnapshot *)readSnapshot;

+ (DBCatalogMediaTypeHint)resolveMediaTypeHintForDisplayName:(NSString *)displayName
                                                  durationMs:(int64_t)durationMs;
+ (nullable NSString *)thumbnailUriForMediaTypeHint:(DBCatalogMediaTypeHint)mediaTypeHint
                                          uriOrPath:(NSString *)uriOrPath;

@end

NS_ASSUME_NONNULL_END
