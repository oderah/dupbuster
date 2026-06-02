#import <Foundation/Foundation.h>

#import "DBDeleteDuplicatesCommand.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBDeleteDuplicatesCommandParser : NSObject

+ (nullable DBDeleteDuplicatesCommand *)parseCommand:(NSDictionary *)command
                                               error:(NSError *_Nullable *_Nullable)error;

@end

NS_ASSUME_NONNULL_END
