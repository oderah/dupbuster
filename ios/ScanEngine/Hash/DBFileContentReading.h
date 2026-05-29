#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol DBFileContentReading <NSObject>

- (nullable NSInputStream *)openReadForFileURL:(NSURL *)fileURL;

- (nullable NSData *)readRangeForFileURL:(NSURL *)fileURL offset:(int64_t)offset length:(NSUInteger)length;

@end

NS_ASSUME_NONNULL_END
