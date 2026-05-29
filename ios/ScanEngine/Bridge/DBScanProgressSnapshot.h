#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Bridge-safe progress metadata (architecture §6.5) — no paths, hashes, or bytes. */
@interface DBScanProgressSnapshot : NSObject

@property (nonatomic, assign) NSInteger filesProcessed;
@property (nonatomic, strong, nullable) NSNumber *filesTotalKnown;
@property (nonatomic, assign) NSInteger groupsFound;
@property (nonatomic, assign) int64_t reclaimableBytesEst;
@property (nonatomic, copy) NSString *phase;
@property (nonatomic, copy, nullable) NSString *contentKind;

- (instancetype)initWithFilesProcessed:(NSInteger)filesProcessed
                      filesTotalKnown:(NSNumber *_Nullable)filesTotalKnown
                          groupsFound:(NSInteger)groupsFound
                  reclaimableBytesEst:(int64_t)reclaimableBytesEst
                                phase:(NSString *)phase
                          contentKind:(NSString *_Nullable)contentKind;

@end

NS_ASSUME_NONNULL_END
