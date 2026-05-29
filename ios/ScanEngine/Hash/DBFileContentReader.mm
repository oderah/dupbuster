#import "DBFileContentReader.h"

@implementation DBFileContentReader

- (nullable NSInputStream *)openReadForFileURL:(NSURL *)fileURL
{
  if (fileURL == nil || !fileURL.isFileURL) {
    return nil;
  }
  return [NSInputStream inputStreamWithURL:fileURL];
}

- (nullable NSData *)readRangeForFileURL:(NSURL *)fileURL offset:(int64_t)offset length:(NSUInteger)length
{
  if (fileURL == nil || !fileURL.isFileURL || length == 0) {
    return [NSData data];
  }
  NSFileHandle *handle = [NSFileHandle fileHandleForReadingFromURL:fileURL error:nil];
  if (handle == nil) {
    return nil;
  }
  [handle seekToFileOffset:(unsigned long long)offset];
  NSData *data = [handle readDataOfLength:length];
  [handle closeFile];
  return data;
}

@end
