#import <Foundation/Foundation.h>

#import "DBImageBitmapExtractor.h"

NS_ASSUME_NONNULL_BEGIN

/** Production image decode via UIImage / ImageIO (≤320×180 gray frame for dHash). */
@interface DBUiImageBitmapExtractor : NSObject <DBImageBitmapExtracting>

@end

NS_ASSUME_NONNULL_END
