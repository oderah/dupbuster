#import <Foundation/Foundation.h>

#import "DBImageBitmapExtractor.h"

@class DBStagedFile;
@class DBHashSettings;
@class DBImageFingerprint;

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBImageFingerprinterOutcome) {
  DBImageFingerprinterOutcomeSuccess = 0,
  DBImageFingerprinterOutcomeUnscannable = 1,
};

@interface DBImageFingerprinterResult : NSObject
@property (nonatomic, assign) DBImageFingerprinterOutcome outcome;
@property (nonatomic, copy, nullable) NSString *unscannableReason;
@property (nonatomic, strong, nullable) DBImageFingerprint *fingerprint;
@end

/** Computes IMAGE_CONTENT_V1 (dHash on decoded bitmap) for image files (FR-FP-09 / FR-FP-10). */
@interface DBImageFingerprinter : NSObject

- (instancetype)initWithBitmapExtractor:(id<DBImageBitmapExtracting>)bitmapExtractor;
- (DBImageFingerprinterResult *)fingerprintStaged:(DBStagedFile *)staged
                                         settings:(DBHashSettings *)settings;

@end

NS_ASSUME_NONNULL_END
