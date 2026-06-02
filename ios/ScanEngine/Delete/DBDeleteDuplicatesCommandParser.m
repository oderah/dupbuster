#import "DBDeleteDuplicatesCommandParser.h"

static NSString *const kDeleteParserDomain = @"com.dupbuster.delete.parser";

@implementation DBDeleteDuplicatesCommandParser

+ (DBDeleteDuplicatesCommand *)parseCommand:(NSDictionary *)command
                                      error:(NSError **)error
{
  NSNumber *groupId = command[@"groupId"];
  NSNumber *keeperId = command[@"keeperFileEntryId"];
  NSArray *deleteIds = command[@"deleteFileEntryIds"];

  if (![groupId isKindOfClass:[NSNumber class]]) {
    [self failWithCode:1 message:@"deleteDuplicates requires groupId" error:error];
    return nil;
  }
  if (![keeperId isKindOfClass:[NSNumber class]]) {
    [self failWithCode:2 message:@"deleteDuplicates requires keeperFileEntryId" error:error];
    return nil;
  }
  if (![deleteIds isKindOfClass:[NSArray class]] || deleteIds.count == 0) {
    [self failWithCode:3 message:@"deleteDuplicates requires deleteFileEntryIds" error:error];
    return nil;
  }

  NSMutableArray<NSNumber *> *parsedIds = [NSMutableArray array];
  NSMutableSet<NSNumber *> *seen = [NSMutableSet set];
  for (id value in deleteIds) {
    if (![value isKindOfClass:[NSNumber class]]) {
      [self failWithCode:4 message:@"deleteFileEntryIds must contain numbers" error:error];
      return nil;
    }
    NSNumber *fileEntryId = (NSNumber *)value;
    if ([fileEntryId isEqualToNumber:keeperId]) {
      [self failWithCode:5 message:@"keeper must not appear in deleteFileEntryIds" error:error];
      return nil;
    }
    if (![seen containsObject:fileEntryId]) {
      [seen addObject:fileEntryId];
      [parsedIds addObject:fileEntryId];
    }
  }

  DBDeleteDuplicatesCommand *parsed = [[DBDeleteDuplicatesCommand alloc] init];
  parsed.groupId = groupId.integerValue;
  parsed.keeperFileEntryId = keeperId.integerValue;
  parsed.deleteFileEntryIds = [parsedIds copy];
  return parsed;
}

+ (void)failWithCode:(NSInteger)code
             message:(NSString *)message
               error:(NSError **)error
{
  if (error != NULL) {
    *error = [NSError errorWithDomain:kDeleteParserDomain code:code userInfo:@{NSLocalizedDescriptionKey : message}];
  }
}

@end
