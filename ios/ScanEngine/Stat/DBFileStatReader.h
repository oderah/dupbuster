#import <Foundation/Foundation.h>

#import "DBFileStatReading.h"

NS_ASSUME_NONNULL_BEGIN

/** Live stat via `lstat` / Photos.framework (injectable in tests). */
@interface DBFileStatReader : NSObject <DBFileStatReading>
@end

NS_ASSUME_NONNULL_END
