#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBHashSettings : NSObject

@property (nonatomic, assign) BOOL largeFilesOptIn;

+ (instancetype)defaultSettings;

@end

NS_ASSUME_NONNULL_END
