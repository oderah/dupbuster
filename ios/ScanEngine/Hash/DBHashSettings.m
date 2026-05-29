#import "DBHashSettings.h"

@implementation DBHashSettings

+ (instancetype)defaultSettings
{
  DBHashSettings *settings = [[DBHashSettings alloc] init];
  settings.largeFilesOptIn = NO;
  return settings;
}

@end
