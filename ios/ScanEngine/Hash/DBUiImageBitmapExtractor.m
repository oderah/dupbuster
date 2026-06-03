#import "DBUiImageBitmapExtractor.h"

#import <ImageIO/ImageIO.h>
#import <UIKit/UIKit.h>

#import "DBVideoConstants.h"
#import "DBVideoFrameExtractor.h"

@implementation DBUiImageBitmapExtractor

- (DBImageBitmapExtractOutcome)decodeImageForURL:(NSURL *)fileURL
                                        deadline:(NSDate *)deadline
                                           frame:(DBGrayFrame **)frame
{
  if ([NSDate.date compare:deadline] == NSOrderedDescending) {
    return DBImageBitmapExtractOutcomeDecodeFailed;
  }
  if (fileURL == nil) {
    return DBImageBitmapExtractOutcomeDecodeFailed;
  }

  @try {
    CGImageSourceRef source = CGImageSourceCreateWithURL((__bridge CFURLRef)fileURL, NULL);
    if (source == NULL) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }

    CFDictionaryRef properties =
        CGImageSourceCopyPropertiesAtIndex(source, 0, NULL);
    NSInteger orientation = 1;
    if (properties != NULL) {
      CFNumberRef orientationRef =
          CFDictionaryGetValue(properties, kCGImagePropertyOrientation);
      if (orientationRef != NULL) {
        CFNumberGetValue(orientationRef, kCFNumberNSIntegerType, &orientation);
      }
      CFRelease(properties);
    }

    NSDictionary *thumbnailOptions = @{
      (NSString *)kCGImageSourceCreateThumbnailFromImageAlways : @YES,
      (NSString *)kCGImageSourceCreateThumbnailWithTransform : @YES,
      (NSString *)kCGImageSourceThumbnailMaxPixelSize :
          @(MAX(DBVideoDHashMaxWidth, DBVideoDHashMaxHeight)),
    };
    CGImageRef imageRef =
        CGImageSourceCreateThumbnailAtIndex(source, 0, (__bridge CFDictionaryRef)thumbnailOptions);
    CFRelease(source);
    if (imageRef == NULL) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }

    UIImage *image = [UIImage imageWithCGImage:imageRef scale:1.0 orientation:(UIImageOrientation)orientation];
    CGImageRelease(imageRef);
    if (image == nil) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }

    CGSize targetSize = [self scaledSizeForWidth:image.size.width height:image.size.height];
    UIGraphicsBeginImageContextWithOptions(targetSize, YES, 1.0);
    [image drawInRect:CGRectMake(0, 0, targetSize.width, targetSize.height)];
    UIImage *scaled = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    if (scaled == nil) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }

    if ([NSDate.date compare:deadline] == NSOrderedDescending) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }

    DBGrayFrame *grayFrame = [self grayFrameFromImage:scaled];
    if (grayFrame == nil) {
      return DBImageBitmapExtractOutcomeDecodeFailed;
    }
    if (frame != NULL) {
      *frame = grayFrame;
    }
    return DBImageBitmapExtractOutcomeOk;
  } @catch (__unused NSException *exception) {
    return DBImageBitmapExtractOutcomeDecodeFailed;
  }
}

- (CGSize)scaledSizeForWidth:(CGFloat)width height:(CGFloat)height
{
  if (width <= 0 || height <= 0) {
    return CGSizeMake(1, 1);
  }
  CGFloat scale = MIN((CGFloat)DBVideoDHashMaxWidth / width,
                      (CGFloat)DBVideoDHashMaxHeight / height);
  scale = MIN(scale, 1.0);
  return CGSizeMake(MAX((CGFloat)1.0, floor(width * scale)), MAX((CGFloat)1.0, floor(height * scale)));
}

- (nullable DBGrayFrame *)grayFrameFromImage:(UIImage *)image
{
  CGImageRef cgImage = image.CGImage;
  if (cgImage == NULL) {
    return nil;
  }
  NSInteger width = CGImageGetWidth(cgImage);
  NSInteger height = CGImageGetHeight(cgImage);
  if (width <= 0 || height <= 0) {
    return nil;
  }

  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  if (colorSpace == NULL) {
    return nil;
  }
  size_t bytesPerRow = (size_t)width * 4;
  NSMutableData *pixelData = [NSMutableData dataWithLength:bytesPerRow * (size_t)height];
  CGContextRef context =
      CGBitmapContextCreate(pixelData.mutableBytes,
                            (size_t)width,
                            (size_t)height,
                            8,
                            bytesPerRow,
                            colorSpace,
                            kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
  CGColorSpaceRelease(colorSpace);
  if (context == NULL) {
    return nil;
  }
  CGContextDrawImage(context, CGRectMake(0, 0, width, height), cgImage);
  CGContextRelease(context);

  NSMutableData *gray = [NSMutableData dataWithLength:(NSUInteger)(width * height)];
  uint8_t *grayBytes = gray.mutableBytes;
  const uint8_t *pixels = pixelData.bytes;
  for (NSInteger index = 0; index < width * height; index++) {
    NSInteger offset = index * 4;
    uint8_t r = pixels[offset];
    uint8_t g = pixels[offset + 1];
    uint8_t b = pixels[offset + 2];
    grayBytes[index] = (uint8_t)((r * 77 + g * 150 + b * 29) >> 8);
  }

  DBGrayFrame *frame = [[DBGrayFrame alloc] init];
  frame.width = width;
  frame.height = height;
  frame.pixels = gray;
  return frame;
}

@end
