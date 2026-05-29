#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Authoritative metadata from StatStage (`file_entry` size/mtime/inode/device_id). */
@interface DBFileStat : NSObject

@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, assign) int64_t mtimeNs;
@property (nonatomic, strong, nullable) NSNumber *inode;
@property (nonatomic, strong, nullable) NSNumber *deviceId;
@property (nonatomic, assign) BOOL isSymlink;

- (instancetype)initWithSizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs
                            inode:(nullable NSNumber *)inode
                         deviceId:(nullable NSNumber *)deviceId
                        isSymlink:(BOOL)isSymlink;

@end

NS_ASSUME_NONNULL_END
