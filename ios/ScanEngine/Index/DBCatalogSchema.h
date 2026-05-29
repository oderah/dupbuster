#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT const NSInteger DBCatalogSchemaCurrentVersion;
FOUNDATION_EXPORT NSString *const DBCatalogSchemaDatabaseName;
FOUNDATION_EXPORT NSString *const DBCatalogHashAlgoSha256;
FOUNDATION_EXPORT NSString *const DBCatalogMetaSchemaVersion;
FOUNDATION_EXPORT NSString *const DBCatalogMetaFullRescanRequired;

@interface DBCatalogSchema : NSObject

+ (NSArray<NSString *> *)createTableStatements;
+ (NSArray<NSString *> *)createIndexStatements;
+ (NSArray<NSString *> *)legacyV1CreateTableStatements;
+ (NSArray<NSString *> *)legacyV1CreateIndexStatements;

@end

NS_ASSUME_NONNULL_END
