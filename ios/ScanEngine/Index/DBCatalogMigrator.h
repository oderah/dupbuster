#import <Foundation/Foundation.h>

@class DBCatalogDatabase;

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT const NSInteger DBCatalogSchemaLegacyVersion;

@interface DBCatalogMigrator : NSObject

+ (BOOL)migrateDatabase:(DBCatalogDatabase *)database
            fromVersion:(NSInteger)fromVersion
              toVersion:(NSInteger)toVersion
                  error:(NSError * _Nullable * _Nullable)error;

@end

NS_ASSUME_NONNULL_END
