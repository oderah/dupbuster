#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"

NS_ASSUME_NONNULL_BEGIN

/** One PHAsset row from platform discovery (injectable in tests). */
@interface DBPhAssetRecord : NSObject

@property (nonatomic, copy) NSString *localIdentifier;
@property (nonatomic, copy) NSString *displayName;
@property (nonatomic, copy) DBMediaTypeHint mediaTypeHint;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, assign) int64_t mtimeNs;

- (instancetype)initWithLocalIdentifier:(NSString *)localIdentifier
                            displayName:(NSString *)displayName
                          mediaTypeHint:(DBMediaTypeHint)mediaTypeHint
                              sizeBytes:(int64_t)sizeBytes
                                mtimeNs:(int64_t)mtimeNs;

@end

NS_ASSUME_NONNULL_END
