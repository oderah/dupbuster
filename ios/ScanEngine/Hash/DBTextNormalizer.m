#import "DBTextNormalizer.h"

@implementation DBTextNormalizer

+ (nullable NSData *)normalizedUtf8FromRaw:(NSData *)raw
{
  if (raw == nil) {
    return [NSData data];
  }
  NSData *withoutBom = [self stripUtf8BomFromData:raw];
  NSString *text = [[NSString alloc] initWithData:withoutBom encoding:NSUTF8StringEncoding];
  if (text == nil) {
    return nil;
  }
  NSString *nfc = [text precomposedStringWithCanonicalMapping];
  NSString *lfOnly = [nfc stringByReplacingOccurrencesOfString:@"\r\n" withString:@"\n"];
  return [lfOnly dataUsingEncoding:NSUTF8StringEncoding];
}

+ (NSData *)stripUtf8BomFromData:(NSData *)data
{
  const uint8_t *bytes = data.bytes;
  NSUInteger length = data.length;
  if (length >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
    return [data subdataWithRange:NSMakeRange(3, length - 3)];
  }
  return data;
}

@end
