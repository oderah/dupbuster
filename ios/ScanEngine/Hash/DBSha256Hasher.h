#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBStreamDigestOutcome) {
  DBStreamDigestOutcomeOk = 0,
  DBStreamDigestOutcomeTimeout = 1,
  DBStreamDigestOutcomeIoFailure = 2,
};

@interface DBSha256Hasher : NSObject

+ (NSString *)hexDigestOfData:(NSData *)data;

+ (NSString *)hexDigestOfQuickSampleFirst:(NSData *)first last:(NSData *)last;

+ (DBStreamDigestOutcome)hexDigestOfStream:(NSInputStream *)stream
                                bufferSize:(NSUInteger)bufferSize
                                    deadline:(NSDate *)deadline
                                  outDigest:(NSString * _Nullable * _Nullable)outDigest;

@end

NS_ASSUME_NONNULL_END
