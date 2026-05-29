#import <Foundation/Foundation.h>

@class DBStagedFile;
@class DBVideoFingerprint;
@class DBHashSettings;

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBVideoFingerprinterOutcome);

@interface DBVideoFingerprinterResult : NSObject
@property (nonatomic, assign) DBVideoFingerprinterOutcome outcome;
@property (nonatomic, copy, nullable) NSString *unscannableReason;
@property (nonatomic, strong, nullable) DBVideoFingerprint *fingerprint;
@end

@interface DBVideoFingerprinter : NSObject

- (instancetype)initWithFrameExtractor:(id)frameExtractor;
- (DBVideoFingerprinterResult *)fingerprintStaged:(DBStagedFile *)staged settings:(DBHashSettings *)settings;
+ (NSArray<NSNumber *> *)samplePositionsForDurationMs:(int64_t)durationMs;

@end

NS_ASSUME_NONNULL_END
