#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Plain-text normalization for `TEXT_NFC_LF` (architecture §4.3). */
@interface DBTextNormalizer : NSObject

/** Returns normalized UTF-8 bytes, or nil when input is not valid UTF-8. */
+ (nullable NSData *)normalizedUtf8FromRaw:(NSData *)raw;

@end

NS_ASSUME_NONNULL_END
