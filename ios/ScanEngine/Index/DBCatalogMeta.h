#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBCatalogMeta : NSObject

@property (nonatomic, assign) NSInteger schemaVersion;
@property (nonatomic, assign) BOOL fullRescanRequired;

@end

NS_ASSUME_NONNULL_END
