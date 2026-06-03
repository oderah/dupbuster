#import <Foundation/Foundation.h>

#import "DBVideoFrameExtractor.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBImageBitmapExtractOutcome) {
  DBImageBitmapExtractOutcomeOk = 0,
  DBImageBitmapExtractOutcomeDecodeFailed = 1,
};

@protocol DBImageBitmapExtracting <NSObject>

- (DBImageBitmapExtractOutcome)decodeImageForURL:(NSURL *)fileURL
                                        deadline:(NSDate *)deadline
                                           frame:(DBGrayFrame *_Nullable *_Nullable)frame;

@end

NS_ASSUME_NONNULL_END
