#import "DBDeleteCoordinatorFactory.h"

#import "DBCatalogDatabase.h"
#import "DBDeleteCoordinator.h"
#import "DBIndexWriter.h"
#import "DBPendingPlatformFileDeleter.h"
#import "DBUriValidator.h"

@implementation DBDeleteCoordinatorFactory

+ (DBDeleteCoordinator *)createCoordinator
{
  DBCatalogDatabase *database = [DBCatalogDatabase sharedDatabase];
  DBIndexWriter *indexWriter = [[DBIndexWriter alloc] initWithDatabase:database];
  DBUriValidator *uriValidator = [[DBUriValidator alloc] init];
  DBPendingPlatformFileDeleter *deleter = [[DBPendingPlatformFileDeleter alloc] init];
  return [[DBDeleteCoordinator alloc] initWithIndexWriter:indexWriter
                                             uriValidator:uriValidator
                                       platformFileDeleter:deleter
                                                workQueue:nil];
}

@end
