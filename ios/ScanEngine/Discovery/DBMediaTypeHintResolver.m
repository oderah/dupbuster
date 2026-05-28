#import "DBMediaTypeHintResolver.h"

@implementation DBMediaTypeHintResolver

+ (DBMediaTypeHint)hintForUniformTypeIdentifier:(NSString *)uti
{
  if (uti.length == 0) {
    return DBMediaTypeHintOther;
  }
  NSString *lower = uti.lowercaseString;
  if ([lower hasPrefix:@"public.text"] || [lower isEqualToString:@"public.plain-text"]) {
    return DBMediaTypeHintText;
  }
  if ([lower hasPrefix:@"public.image"] || [lower containsString:@"image"]) {
    return DBMediaTypeHintImage;
  }
  if ([lower hasPrefix:@"public.movie"] || [lower containsString:@"video"]) {
    return DBMediaTypeHintVideo;
  }
  if ([lower hasPrefix:@"public.audio"] || [lower containsString:@"audio"]) {
    return DBMediaTypeHintAudio;
  }
  if ([lower containsString:@"pdf"] || [lower containsString:@"document"]) {
    return DBMediaTypeHintDocument;
  }
  return DBMediaTypeHintOther;
}

+ (DBMediaTypeHint)hintForFileName:(NSString *)fileName
{
  NSString *ext = fileName.pathExtension.lowercaseString;
  if (ext.length == 0) {
    return DBMediaTypeHintOther;
  }
  static NSDictionary<NSString *, DBMediaTypeHint> *map;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    map = @{
      @"jpg" : DBMediaTypeHintImage,
      @"jpeg" : DBMediaTypeHintImage,
      @"png" : DBMediaTypeHintImage,
      @"heic" : DBMediaTypeHintImage,
      @"mp4" : DBMediaTypeHintVideo,
      @"mov" : DBMediaTypeHintVideo,
      @"m4v" : DBMediaTypeHintVideo,
      @"mp3" : DBMediaTypeHintAudio,
      @"m4a" : DBMediaTypeHintAudio,
      @"txt" : DBMediaTypeHintText,
      @"md" : DBMediaTypeHintText,
      @"pdf" : DBMediaTypeHintDocument,
      @"doc" : DBMediaTypeHintDocument,
      @"docx" : DBMediaTypeHintDocument,
    };
  });
  DBMediaTypeHint hint = map[ext];
  return hint ?: DBMediaTypeHintOther;
}

@end
