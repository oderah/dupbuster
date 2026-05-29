#import "DBDHash.h"

#import "DBVideoConstants.h"

static const NSInteger kDHashCompareWidth = 9;
static const NSInteger kDHashCompareHeight = 8;

@implementation DBDHash

+ (uint64_t)hashFromGrayPixels:(NSData *)pixels width:(NSInteger)width height:(NSInteger)height
{
  NSData *bounded = [self downscaleGray:pixels width:width height:height targetW:DBVideoDHashMaxWidth targetH:DBVideoDHashMaxHeight];
  NSInteger boundedW = MIN(width, DBVideoDHashMaxWidth);
  NSInteger boundedH = MIN(height, DBVideoDHashMaxHeight);
  if (width > 0 && height > 0 && (width > DBVideoDHashMaxWidth || height > DBVideoDHashMaxHeight)) {
    double scale = MIN((double)DBVideoDHashMaxWidth / width, (double)DBVideoDHashMaxHeight / height);
    boundedW = MAX(1, (NSInteger)(width * scale));
    boundedH = MAX(1, (NSInteger)(height * scale));
  }
  NSData *grid = [self downscaleGray:bounded width:boundedW height:boundedH targetW:kDHashCompareWidth targetH:kDHashCompareHeight];
  return [self differenceHash:grid];
}

+ (NSInteger)hammingDistanceBetween:(uint64_t)left and:(uint64_t)right
{
  return __builtin_popcountll(left ^ right);
}

+ (uint64_t)differenceHash:(NSData *)gray
{
  const uint8_t *bytes = gray.bytes;
  uint64_t hash = 0;
  NSInteger bitIndex = 0;
  for (NSInteger row = 0; row < kDHashCompareHeight; row++) {
    NSInteger rowOffset = row * kDHashCompareWidth;
    for (NSInteger col = 0; col < kDHashCompareWidth - 1; col++) {
      uint8_t left = bytes[rowOffset + col];
      uint8_t right = bytes[rowOffset + col + 1];
      if (left < right) {
        hash |= (1ULL << bitIndex);
      }
      bitIndex++;
    }
  }
  return hash;
}

+ (NSData *)downscaleGray:(NSData *)pixels
                    width:(NSInteger)width
                   height:(NSInteger)height
                  targetW:(NSInteger)targetW
                  targetH:(NSInteger)targetH
{
  NSMutableData *out = [NSMutableData dataWithLength:(NSUInteger)(targetW * targetH)];
  uint8_t *outBytes = out.mutableBytes;
  const uint8_t *src = pixels.bytes;
  if (width <= 0 || height <= 0) {
    return out;
  }
  for (NSInteger y = 0; y < targetH; y++) {
    NSInteger srcY = y * height / targetH;
    for (NSInteger x = 0; x < targetW; x++) {
      NSInteger srcX = x * width / targetW;
      outBytes[y * targetW + x] = src[srcY * width + srcX];
    }
  }
  return out;
}

@end
