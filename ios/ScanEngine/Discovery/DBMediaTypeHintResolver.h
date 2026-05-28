#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBMediaTypeHintResolver : NSObject

+ (DBMediaTypeHint)hintForUniformTypeIdentifier:(nullable NSString *)uti;
+ (DBMediaTypeHint)hintForFileName:(NSString *)fileName;

@end

NS_ASSUME_NONNULL_END
