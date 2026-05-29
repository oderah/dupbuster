#import "DBSha256Hasher.h"

#import <CommonCrypto/CommonDigest.h>

@implementation DBSha256Hasher

+ (NSString *)hexDigestOfData:(NSData *)data
{
  unsigned char digest[CC_SHA256_DIGEST_LENGTH];
  CC_SHA256(data.bytes, (CC_LONG)data.length, digest);
  return [self hexStringFromBytes:digest length:CC_SHA256_DIGEST_LENGTH];
}

+ (NSString *)hexDigestOfQuickSampleFirst:(NSData *)first last:(NSData *)last
{
  CC_SHA256_CTX context;
  CC_SHA256_Init(&context);
  CC_SHA256_Update(&context, first.bytes, (CC_LONG)first.length);
  CC_SHA256_Update(&context, last.bytes, (CC_LONG)last.length);
  unsigned char digest[CC_SHA256_DIGEST_LENGTH];
  CC_SHA256_Final(digest, &context);
  return [self hexStringFromBytes:digest length:CC_SHA256_DIGEST_LENGTH];
}

+ (DBStreamDigestOutcome)hexDigestOfStream:(NSInputStream *)stream
                                bufferSize:(NSUInteger)bufferSize
                                  deadline:(NSDate *)deadline
                                 outDigest:(NSString **)outDigest
{
  CC_SHA256_CTX context;
  CC_SHA256_Init(&context);

  uint8_t *buffer = (uint8_t *)malloc(bufferSize);
  if (buffer == NULL) {
    return DBStreamDigestOutcomeIoFailure;
  }

  [stream open];
  DBStreamDigestOutcome outcome = DBStreamDigestOutcomeOk;
  while (true) {
    if ([NSDate.date compare:deadline] == NSOrderedDescending) {
      outcome = DBStreamDigestOutcomeTimeout;
      break;
    }
    NSInteger read = [stream read:buffer maxLength:bufferSize];
    if (read < 0) {
      outcome = DBStreamDigestOutcomeIoFailure;
      break;
    }
    if (read == 0) {
      break;
    }
    CC_SHA256_Update(&context, buffer, (CC_LONG)read);
  }
  [stream close];
  free(buffer);

  if (outcome == DBStreamDigestOutcomeOk && outDigest != NULL) {
    unsigned char digest[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256_Final(digest, &context);
    *outDigest = [self hexStringFromBytes:digest length:CC_SHA256_DIGEST_LENGTH];
  }
  return outcome;
}

+ (NSString *)hexStringFromBytes:(const unsigned char *)bytes length:(NSUInteger)length
{
  NSMutableString *hex = [NSMutableString stringWithCapacity:length * 2];
  for (NSUInteger i = 0; i < length; i++) {
    [hex appendFormat:@"%02x", bytes[i]];
  }
  return hex;
}

@end
